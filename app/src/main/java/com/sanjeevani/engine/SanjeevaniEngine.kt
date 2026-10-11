package com.sanjeevani.engine

import android.graphics.Color
import android.graphics.PointF
import android.os.SystemClock
import android.util.Log
import com.sanjeevani.model.*
import kotlin.math.hypot

data class DialogueEvidence(val state: FSMState, val version: Long, val transcript: String,
    val confidence: Float, val answer: Answer)
data class SessionContext(val reportedConcern: String, val activeConcern: EmergencyType,
    val question: String, val responding: Boolean?, val normalBreathing: Boolean?,
    val completedSteps: Set<FSMState>, val recentAnswers: List<DialogueEvidence>)

/** Serial, main-thread coordinator. FSM owns progression; overlays only project it. */
class SanjeevaniEngine {
    private val fsm = EmergencyFSM()
    private val spatialReasoner = SpatialReasoner()
    private val patientDetector = PatientDetector()
    private val compressionDetector = CompressionDetector()
    private val allergicModule = AllergicReactionModule()
    private val strokeModule = StrokeModule()
    private var latest = GuidanceState()
    private var frame: PerceptionFrame? = null
    private var lastPoseTimestamp = -1L
    private var lastHandTimestamp = -1L
    private var lastFrameTimestamp = -1L
    private var announcedVersion = -1L
    private var lastCorrectionTs = -10000L
    private var activeSince = 0L
    private var trackedHand: String? = null
    private var transitionLogged = 0L
    private var frozenSternumTarget: PointF? = null
    private var pendingResponseCheck = false
    // Session analytics — cleared on entry to COMPRESSION_ACTIVE and on resetForNewEmergency
    private val handPlacementFaults = mutableMapOf<SpatialAction, Int>()
    private var compressionActiveFrames = 0
    private var compressionReliableFrames = 0
    private var rateOnTargetFrames = 0
    private var rateTooSlowFrames = 0
    private var rateTooFastFrames = 0
    private var pendingAllergicVoice: String? = null
    private var latestAllergicOverlay: AROverlaySpec? = null
    private var pendingStrokeVoice: String? = null
    private var latestStrokeOverlay: AROverlaySpec? = null
    private var visionAfter = 0L
    private var reportedConcern = ""
    private val answers = ArrayDeque<DialogueEvidence>()
    val sessionContext get() = SessionContext(reportedConcern, fsm.getConfirmedEmergencyType(),
        fsm.instruction(), fsm.responding, fsm.breathingNormally, fsm.completed.toSet(), answers.toList())
    fun recordUtterance(text: String, confidence: Float, answer: Answer, concern: EmergencyType?) {
        if (concern != null && reportedConcern.isEmpty()) reportedConcern = text.take(500)
        answers.addLast(DialogueEvidence(fsm.getState(), fsm.version, text.take(500), confidence, answer))
        if (answers.size > 20) answers.removeFirst()
    }
    val stateVersion get() = fsm.version
    val history get() = fsm.history.toList()
    val isPendingConsciousnessCheck: Boolean get() = pendingResponseCheck
    fun getFSMState() = fsm.getState()
    fun instruction() = fsm.instruction()
    fun expectsAnswer() = fsm.expectsAnswer()

    fun process(input: PerceptionFrame): GuidanceState {
        if (input.timestamp <= lastFrameTimestamp) return latest.copy(voiceText = null)
        lastFrameTimestamp = input.timestamp
        val boundary = maxOf(visionAfter, fsm.history.lastOrNull()?.timestamp ?: 0)
        val freshPose = input.timestamp - input.poseTimestamp in 0..300 && input.poseTimestamp >= boundary
        val freshHands = input.timestamp - input.handTimestamp in 0..250 && input.handTimestamp >= boundary &&
            kotlin.math.abs(input.handTimestamp - input.poseTimestamp) <= 150
        val safeFrame = input.copy(poseLandmarks = input.poseLandmarks.takeIf { freshPose },
            worldLandmarks = input.worldLandmarks.takeIf { freshPose },
            leftHandLandmarks = input.leftHandLandmarks.takeIf { freshHands },
            rightHandLandmarks = input.rightHandLandmarks.takeIf { freshHands })
        frame = safeFrame
        val state = fsm.getState()
        val pose = safeFrame.poseLandmarks
        val poseConf = listOf(11,12,23,24).map { pose?.getOrNull(it)?.visibility ?: 0f }.minOrNull() ?: 0f
        val spatial = spatialReasoner.compute(pose, safeFrame.leftHandLandmarks, safeFrame.rightHandLandmarks, input.timestamp)
        // Only an active, position-gated step examines lying posture.
        val patient = if (state == FSMState.CPR_POSITIONING ||
            (state == FSMState.ALLERGIC_PROTOCOL && allergicModule.currentTrainingStepIndex == 2))
            patientDetector.isPatientLying(pose, safeFrame.worldLandmarks) else false to 0f
        val handsConf = if (spatial.handMidpoint != null) .8f else 0f
        val confidence = ConfidenceState(poseConf, handsConf, patient.second,
            if (poseConf >= .65f && handsConf >= .65f) ConfidenceLevel.HIGH else ConfidenceLevel.LOW)
        val newPose = input.poseTimestamp > lastPoseTimestamp && freshPose
        val newHand = input.handTimestamp > lastHandTimestamp && freshHands
        // Lock the sternum to its first confirmed position when compressions begin.
        // Camera jitter during compressions must not move the pacing sphere.
        if (state == FSMState.COMPRESSION_ACTIVE) {
            if (frozenSternumTarget == null && spatial.sternumTarget != null)
                frozenSternumTarget = spatial.sternumTarget
        } else {
            frozenSternumTarget = null
        }
        if (state != FSMState.COMPRESSION_ACTIVE && newHand && spatial.handMidpoint != null &&
            spatial.sternumTarget != null && spatial.errorMagnitude < .12f && poseConf >= .65f) {
            if (trackedHand != spatial.trackedHand) compressionDetector.trackingLost()
            trackedHand = spatial.trackedHand
            val torso = pose?.let { hypot((it[23].x + it[24].x - it[11].x - it[12].x)/2f,
                (it[23].y + it[24].y - it[11].y - it[12].y)/2f) } ?: 0f
            if (torso > .1f) compressionDetector.addSample(
                (spatial.handMidpoint.y - spatial.sternumTarget.y) / torso, input.handTimestamp)
        } else if (state != FSMState.COMPRESSION_ACTIVE && (!freshHands || spatial.handMidpoint == null ||
                spatial.errorMagnitude >= .12f || poseConf < .65f)) compressionDetector.trackingLost()
        if (newPose || pose == null) {
            if (state != FSMState.HAND_POSITIONING || newHand || spatial.handMidpoint == null) {
                fsm.process(GuidanceState(spatial = spatial, confidence = confidence,
                    isPatientDetected = patient.first), input.poseTimestamp)
            }
        }
        lastPoseTimestamp = maxOf(lastPoseTimestamp, input.poseTimestamp)
        lastHandTimestamp = maxOf(lastHandTimestamp, input.handTimestamp)
        val temporal = compressionDetector.getTemporalState(input.timestamp)
        // Accumulate rate analytics during active CPR
        if (state == FSMState.COMPRESSION_ACTIVE) {
            compressionActiveFrames++
            if (temporal.trackingReliable) {
                compressionReliableFrames++
                val bpm = temporal.compressionRateBPM
                when {
                    bpm in 100f..120f -> rateOnTargetFrames++
                    bpm < 100f -> rateTooSlowFrames++
                    else -> rateTooFastFrames++
                }
            }
        }
        // Accumulate session mistakes for end analysis
        if (state == FSMState.HAND_POSITIONING && newPose) {
            val a = spatial.correctiveAction
            if (a !in setOf(SpatialAction.CORRECT, SpatialAction.UNSURE, SpatialAction.TRACKING_LOST)) {
                handPlacementFaults[a] = (handPlacementFaults[a] ?: 0) + 1
            }
        }
        latest = latest.copy(spatial = spatial, temporal = temporal, confidence = confidence,
            isPatientDetected = patient.first)
        // Allergic reaction: timer-based phase progression with camera-anchored AR overlays.
        // Process every frame so the module's startMs anchors to real wall-clock time.
        if (state == FSMState.ALLERGIC_PROTOCOL) {
            val result = allergicModule.process(safeFrame, input.timestamp)
            if (result.voiceText != null) pendingAllergicVoice = result.voiceText
            latestAllergicOverlay = result.overlay
        }
        // Stroke FAST assessment: step-timed progression, camera-anchored skeleton overlay.
        if (state == FSMState.STROKE_FAST_TEST) {
            val result = strokeModule.process(safeFrame, input.timestamp)
            if (result.voiceText != null) pendingStrokeVoice = result.voiceText
            latestStrokeOverlay = result.overlay
        }
        return snapshot(input.timestamp)
    }

    fun snapshot(nowMs: Long = SystemClock.elapsedRealtime()): GuidanceState {
        val state = fsm.getState()
        val entered = announcedVersion != fsm.version
        var voice: String? = null
        if (entered) {
            announcedVersion = fsm.version
            voice = fsm.instruction()
            if (state == FSMState.COMPRESSION_ACTIVE) {
                activeSince = nowMs; compressionDetector.reset()
                pendingResponseCheck = false
                compressionActiveFrames = 0; compressionReliableFrames = 0
                rateOnTargetFrames = 0; rateTooSlowFrames = 0; rateTooFastFrames = 0
                latest = latest.copy(temporal = TemporalState())
                voice = null  // entry speech suppressed; ViewModel counting coroutine handles all voice
            }
            if (state == FSMState.ALLERGIC_PROTOCOL) {
                // Module owns all phase voice; reset so it starts clean from this entry.
                allergicModule.reset(); pendingAllergicVoice = null; latestAllergicOverlay = null
            }
            if (state == FSMState.STROKE_FAST_TEST) {
                strokeModule.reset(); pendingStrokeVoice = null; latestStrokeOverlay = null
            }
        }
        fsm.history.filter { it.version > transitionLogged }.forEach {
            Log.i("SanjeevaniFSM", "${it.timestamp} ${it.from} -> ${it.to} v${it.version}: ${it.reason}")
            transitionLogged = it.version
        }
        val spatial = latest.spatial
        val temporal = latest.temporal
        // Allergic reaction phase voice: fires once per phase transition (timer-driven).
        if (voice == null && state == FSMState.ALLERGIC_PROTOCOL) {
            pendingAllergicVoice?.let { voice = it; pendingAllergicVoice = null }
        }
        // Stroke FAST step voice: fires on each step transition.
        if (voice == null && state == FSMState.STROKE_FAST_TEST) {
            pendingStrokeVoice?.let { voice = it; pendingStrokeVoice = null }
        }
        if (voice == null && state == FSMState.HAND_POSITIONING && nowMs - lastCorrectionTs >= 3000) {
            voice = when (spatial.correctiveAction) {
                SpatialAction.MOVE_LEFT -> "Move a little toward the left of the screen."
                SpatialAction.MOVE_RIGHT -> "Move a little toward the right of the screen."
                SpatialAction.MOVE_UP -> "Move a little toward the top of the screen."
                SpatialAction.MOVE_DOWN -> "Move a little toward the bottom of the screen."
                SpatialAction.TRACKING_LOST -> "I can't see clearly. Adjust the camera, or confirm placement yourself. Don't delay CPR for tracking."
                else -> null
            }
            if (voice != null) lastCorrectionTs = nowMs
        }
        // Stroke FAST assessment: return overlay built from module result.
        if (state == FSMState.STROKE_FAST_TEST) {
            val base = latestStrokeOverlay ?: AROverlaySpec(emergencyType = EmergencyType.FAST_STROKE)
            val phaseLabel = base.statusText.takeIf { it.isNotEmpty() } ?: "FAST Test"
            val strokeOverlay = base.copy(
                skeletonLines = buildSkeletonLines(frame?.poseLandmarks),
                jointPoints = buildJointPoints(frame?.poseLandmarks),
                stepCardTitle = phaseLabel,
                stepCardInstruction = base.guidanceText.takeIf { it.isNotEmpty() } ?: fsm.instruction(),
                stepCardStatus = base.statusText.takeIf { it.isNotEmpty() } ?: "Camera observation unavailable",
                stepCardBgColor = if (base.statusColorGreen) android.graphics.Color.rgb(34, 139, 34)
                    else android.graphics.Color.rgb(185, 40, 40),
                showCPRBadge = false,
                state = state,
                imageWidth = frame?.imageWidth ?: 1,
                imageHeight = frame?.imageHeight ?: 1
            )
            latest = latest.copy(fsmState = state, emergencyType = EmergencyType.FAST_STROKE,
                overlay = strokeOverlay, voiceText = voice, stateVersion = fsm.version,
                triageState = TriageState.USER_CONFIRMED)
            return latest
        }
        // Allergic reaction: return a complete overlay built from the module's latest result.
        if (state == FSMState.ALLERGIC_PROTOCOL) {
            val base = latestAllergicOverlay ?: AROverlaySpec(emergencyType = EmergencyType.ALLERGIC_REACTION)
            val phaseTitle = base.statusText.takeIf { it.isNotEmpty() } ?: "Allergic Reaction"
            val allergicOverlay = base.copy(
                skeletonLines = buildSkeletonLines(frame?.poseLandmarks),
                jointPoints = buildJointPoints(frame?.poseLandmarks),
                stepCardTitle = phaseTitle,
                stepCardInstruction = base.guidanceText.takeIf { it.isNotEmpty() } ?: fsm.instruction(),
                stepCardStatus = allergicModule.currentAssessmentStatus,
                stepCardBgColor = if (allergicModule.currentAssessmentGreen) android.graphics.Color.rgb(34, 139, 34)
                    else android.graphics.Color.rgb(185, 40, 40),
                showCPRBadge = false,
                state = state,
                imageWidth = frame?.imageWidth ?: 1,
                imageHeight = frame?.imageHeight ?: 1
            )
            latest = latest.copy(fsmState = state, emergencyType = EmergencyType.ALLERGIC_REACTION,
                overlay = allergicOverlay, voiceText = voice, stateVersion = fsm.version,
                triageState = TriageState.USER_CONFIRMED)
            return latest
        }

        val title = when (state) {
            FSMState.CPR_POSITIONING, FSMState.POSITION_CONFIRMED -> "1. Position Check"
            FSMState.HAND_POSITIONING -> "3. Hand Placement"
            FSMState.POSTURE_CHECK -> "4. Body Position"
            FSMState.COMPRESSION_ACTIVE -> "5. Chest Compressions"
            FSMState.RESPONSIVENESS_CHECK -> "Check response"
            FSMState.BREATHING_ASSESSMENT -> "Check normal breathing"
            FSMState.HEART_ATTACK_CONSCIOUS -> "Monitor • follow 112"
            FSMState.CPR_SUCCESS -> "Emergency team taking over"
            else -> ""
        }
        val cpr = state in setOf(FSMState.HAND_POSITIONING, FSMState.POSTURE_CHECK, FSMState.COMPRESSION_ACTIVE)
        val status = when (state) {
            FSMState.CPR_POSITIONING -> "Not yet verified • camera estimate"
            FSMState.POSITION_CONFIRMED -> "✓ Estimate stable • confirm surface"
            FSMState.COMPRESSION_ACTIVE -> "Counting with you • push hard and fast"
            FSMState.HAND_POSITIONING -> "Approximate marker • not clinical verification"
            else -> "Follow dispatcher instructions"
        }
        val overlay = AROverlaySpec(
            sternumTarget = when {
                state == FSMState.COMPRESSION_ACTIVE -> frozenSternumTarget ?: spatial.sternumTarget
                cpr -> spatial.sternumTarget
                else -> null
            },
            leftHandCenter = spatial.handMidpoint.takeIf { cpr },
            arrowFrom = spatial.handMidpoint.takeIf { state == FSMState.HAND_POSITIONING && spatial.correctiveAction != SpatialAction.CORRECT },
            arrowTo = spatial.sternumTarget.takeIf { state == FSMState.HAND_POSITIONING },
            guidanceText = if (state == FSMState.HAND_POSITIONING) directionLabel(spatial.correctiveAction) else "",
            skeletonLines = buildSkeletonLines(frame?.poseLandmarks),
            jointPoints = buildJointPoints(frame?.poseLandmarks),
            stepCardTitle = title, stepCardInstruction = if (title.isNotEmpty()) fsm.instruction() else "",
            stepCardStatus = status,
            stepCardBgColor = when (state) {
                FSMState.CPR_POSITIONING -> Color.rgb(185,40,40)
                FSMState.POSITION_CONFIRMED -> Color.rgb(34,139,34)
                else -> Color.argb(220,0,80,150)
            },
            compressionCount = temporal.compressionCount,
            compressionRate = temporal.compressionRateBPM.takeIf { temporal.trackingReliable },
            elapsedSecs = if (state == FSMState.COMPRESSION_ACTIVE) ((nowMs-activeSince)/1000).toInt().coerceAtLeast(0) else 0,
            showCPRBadge = cpr, showVitalsPanel = false,
            statusColorGreen = spatial.correctiveAction == SpatialAction.CORRECT,
            emergencyType = fsm.getConfirmedEmergencyType(), state = state,
            confirmationEvent = fsm.history.lastOrNull { it.to == FSMState.POSITION_CONFIRMED }?.version ?: 0,
            imageWidth = frame?.imageWidth ?: 1, imageHeight = frame?.imageHeight ?: 1,
            countReliable = temporal.trackingReliable)
        latest = latest.copy(fsmState = state, emergencyType = fsm.getConfirmedEmergencyType(),
            overlay = overlay, voiceText = voice, voiceScript = null, stateVersion = fsm.version,
            triageState = if (fsm.getConfirmedEmergencyType() == EmergencyType.UNKNOWN) TriageState.OBSERVING else TriageState.USER_CONFIRMED)
        return latest
    }

    private fun directionLabel(action: SpatialAction) = when(action) {
        SpatialAction.MOVE_LEFT -> "← Screen left"
        SpatialAction.MOVE_RIGHT -> "Screen right →"
        SpatialAction.MOVE_UP -> "↑ Screen up"
        SpatialAction.MOVE_DOWN -> "↓ Screen down"
        SpatialAction.TRACKING_LOST -> "Tracking unavailable"
        else -> ""
    }
    fun answer(answer: Answer, expectedVersion: Long = fsm.version): Boolean {
        if (fsm.getState() == FSMState.COMPRESSION_ACTIVE && pendingResponseCheck) {
            pendingResponseCheck = false
            return when (answer) {
                Answer.YES, Answer.NO, Answer.UNCERTAIN -> true
                else -> fsm.answer(answer, SystemClock.elapsedRealtime(), expectedVersion)
            }
        }
        return fsm.answer(answer, SystemClock.elapsedRealtime(), expectedVersion)
    }
    fun invalidateVision() {
        visionAfter = SystemClock.elapsedRealtime()
        frozenSternumTarget = null
        pendingResponseCheck = false
        spatialReasoner.reset()
        compressionDetector.trackingLost()
        // For allergic reaction and stroke, the module timer continues when camera is unavailable.
        // Clear only the pending voice to avoid re-announcing on resume.
        if (fsm.getState() == FSMState.ALLERGIC_PROTOCOL) pendingAllergicVoice = null
        if (fsm.getState() == FSMState.STROKE_FAST_TEST) pendingStrokeVoice = null
        latest = latest.copy(spatial = SpatialState(correctiveAction = SpatialAction.TRACKING_LOST),
            temporal = compressionDetector.getTemporalState(visionAfter))
    }

    fun buildSessionReport(): CprSessionReport {
        val elapsed = if (activeSince > 0L) ((SystemClock.elapsedRealtime() - activeSince) / 1000).toInt() else 0
        val mistakes = mutableListOf<String>()

        // Step completion
        val postureCompleted = fsm.completed.contains(FSMState.POSTURE_CHECK)
        val positionConfirmedReached = fsm.completed.contains(FSMState.POSITION_CONFIRMED)
        if (!positionConfirmedReached) mistakes.add("Patient position was not camera-confirmed — always verify the person is on their back on a firm surface")
        if (!postureCompleted) mistakes.add("Posture check step was skipped — keep arms straight and shoulders directly above hands")

        // Hand placement
        val dirLabels = mapOf(
            SpatialAction.MOVE_UP    to "Too low on chest (below sternum)",
            SpatialAction.MOVE_DOWN  to "Too high on chest (above target)",
            SpatialAction.MOVE_LEFT  to "Shifted right of centre",
            SpatialAction.MOVE_RIGHT to "Shifted left of centre"
        )
        val handFaultMap = mutableMapOf<String, Int>()
        var totalFaults = 0
        handPlacementFaults.forEach { (action, count) ->
            dirLabels[action]?.let { label ->
                handFaultMap[label] = count
                totalFaults += count
            }
        }
        val dominant = handPlacementFaults.maxByOrNull { it.value }
        if (dominant != null && dominant.value >= 6) {
            mistakes.add("Hand placement: ${dirLabels[dominant.key] ?: "off target"} was detected ${dominant.value} times during positioning")
        }

        // Rate analysis
        val reliablePct = if (compressionActiveFrames > 0) compressionReliableFrames * 100 / compressionActiveFrames else 0
        val rateTotal = rateOnTargetFrames + rateTooSlowFrames + rateTooFastFrames
        val onTargetPct = if (rateTotal > 0) rateOnTargetFrames * 100 / rateTotal else 0
        val tooSlowPct = if (rateTotal > 0) rateTooSlowFrames * 100 / rateTotal else 0
        val tooFastPct = if (rateTotal > 0) rateTooFastFrames * 100 / rateTotal else 0
        if (rateTotal > 0) {
            when {
                tooSlowPct > 40 -> mistakes.add("Compression rate was too slow for more than 40% of the measured period — target 100–120 per minute")
                tooFastPct > 40 -> mistakes.add("Compression rate was too fast for more than 40% of the measured period — target 100–120 per minute")
            }
        }
        if (reliablePct < 30 && elapsed > 10) mistakes.add("Camera tracking was unavailable for most of the session — depth and rate could not be measured")

        return CprSessionReport(
            compressionCount = latest.temporal.compressionCount,
            elapsedSeconds = elapsed,
            bpmAtEnd = latest.temporal.compressionRateBPM.toInt(),
            mistakes = mistakes,
            handFaultsByDirection = handFaultMap,
            totalHandFaults = totalFaults,
            postureCheckCompleted = postureCompleted,
            positionConfirmed = positionConfirmedReached,
            trackingAvailablePct = reliablePct,
            rateOnTargetPct = onTargetPct,
            rateTooSlowPct = tooSlowPct,
            rateTooFastPct = tooFastPct
        )
    }

    /** Reset the engine and FSM to IDLE so a new training session can be selected. */
    fun resetForNewEmergency() {
        allergicModule.reset()
        pendingAllergicVoice = null
        latestAllergicOverlay = null
        strokeModule.reset()
        pendingStrokeVoice = null
        latestStrokeOverlay = null
        compressionDetector.trackingLost()
        spatialReasoner.reset()
        frozenSternumTarget = null
        pendingResponseCheck = false
        handPlacementFaults.clear()
        compressionActiveFrames = 0; compressionReliableFrames = 0
        rateOnTargetFrames = 0; rateTooSlowFrames = 0; rateTooFastFrames = 0
        visionAfter = SystemClock.elapsedRealtime()
        announcedVersion = -1L
        lastCorrectionTs = -10000L
        activeSince = 0L
        latest = GuidanceState()
        fsm.resetForNewSession(SystemClock.elapsedRealtime())
    }
    fun markConsciousnessCheckPending() { pendingResponseCheck = true }
    fun confirmNoResponse() { answer(Answer.NO) }
    fun onPatientResponsive() { answer(Answer.YES) }
    fun setUserSelectedEmergency(type: EmergencyType) { fsm.setUserSelectedEmergency(type, SystemClock.elapsedRealtime()) }
    fun showEmergencySelection() { fsm.setUserSelectedEmergency(EmergencyType.UNKNOWN, SystemClock.elapsedRealtime()) }
    fun onStrokeSpeechResult(positive: Boolean) {
        strokeModule.onSpeechResult(positive)
        // Voice for RESULT step is buffered on the next process() call; clear any stale pending voice now.
        pendingStrokeVoice = null
    }

    fun selectCameraTrainingStep(type: EmergencyType, stepIndex: Int) {
        when (type) {
            EmergencyType.FAST_STROKE -> strokeModule.selectTrainingStep(stepIndex)
            EmergencyType.ALLERGIC_REACTION -> allergicModule.selectTrainingStep(stepIndex)
            else -> return
        }
        // Invalidate queued voice/overlay from the previous learner-controlled step.
        pendingStrokeVoice = null
        pendingAllergicVoice = null
        latestStrokeOverlay = null
        latestAllergicOverlay = null
    }

    fun completeCameraTraining(type: EmergencyType) {
        if (type == EmergencyType.FAST_STROKE) strokeModule.completeTraining()
    }
    private fun batchCount(batchIndex: Int): String {
        val start = batchIndex * 5
        return CPR_COUNT_WORDS.slice(start until start + 5).joinToString("! ") + "!"
    }

    companion object {
        private val CPR_COUNT_WORDS = listOf(
            "One", "Two", "Three", "Four", "Five",
            "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
            "Sixteen", "Seventeen", "Eighteen", "Nineteen", "Twenty",
            "Twenty-one", "Twenty-two", "Twenty-three", "Twenty-four", "Twenty-five",
            "Twenty-six", "Twenty-seven", "Twenty-eight", "Twenty-nine", "Thirty"
        )
    }

    // iOS-matching skeleton bones (mirrors Bones.swift — no face/finger clutter)
    private val SKELETON_BONES = listOf(
        // Neck: nose → shoulders (approximates iOS neck_1 → spine_7 → shoulders)
        0 to 11, 0 to 12,
        // Torso
        11 to 12,             // shoulder bar
        11 to 23, 12 to 24,   // torso sides
        23 to 24,             // hip bar
        // Left arm: shoulder → elbow → wrist
        11 to 13, 13 to 15,
        // Right arm: shoulder → elbow → wrist
        12 to 14, 14 to 16,
        // Left leg: hip → knee → ankle → foot
        23 to 25, 25 to 27, 27 to 29,
        // Right leg: hip → knee → ankle → foot
        24 to 26, 26 to 28, 28 to 30
    )

    // iOS-matching heatmap (mirrors BodySkeleton.swift heatmapColor groups)
    // head=red, spine/torso=red-orange, shoulders/upper-arms=orange-yellow,
    // forearms/hands=cyan, upper-legs=blue-violet, lower-legs/feet=magenta
    private fun jointColor(index: Int): Int {
        return when (index) {
            0 -> android.graphics.Color.rgb(255, 80, 80)          // nose — red
            in 1..10 -> android.graphics.Color.rgb(255, 60, 60)   // face — red (hidden from rendering)
            11, 12 -> android.graphics.Color.rgb(255, 160, 40)    // shoulders — orange-yellow
            13, 14 -> android.graphics.Color.rgb(255, 200, 50)    // elbows — yellow
            15, 16 -> android.graphics.Color.rgb(50, 220, 220)    // wrists — cyan
            17, 18, 19, 20, 21, 22 -> android.graphics.Color.rgb(50, 200, 220) // fingers — teal
            23, 24 -> android.graphics.Color.rgb(255, 120, 50)    // hips — orange
            25, 26 -> android.graphics.Color.rgb(160, 80, 255)    // knees — blue-violet
            27, 28 -> android.graphics.Color.rgb(200, 60, 255)    // ankles — violet
            29, 30, 31, 32 -> android.graphics.Color.rgb(255, 60, 200) // feet — magenta
            else -> android.graphics.Color.WHITE
        }
    }

    private fun buildSkeletonLines(
        pose: List<NormalizedLandmark>?
    ): List<Pair<PointF, PointF>> {
        pose ?: return emptyList()
        return SKELETON_BONES.mapNotNull { (a, b) ->
            if (a < pose.size && b < pose.size) {
                val la = pose[a]; val lb = pose[b]
                if (la.visibility > 0.3f && lb.visibility > 0.3f)
                    Pair(PointF(la.x, la.y), PointF(lb.x, lb.y))
                else null
            } else null
        }
    }

    private fun buildJointPoints(
        pose: List<NormalizedLandmark>?
    ): List<Pair<PointF, Int>> {
        pose ?: return emptyList()
        return pose.mapIndexedNotNull { i, lm ->
            // iOS hides face sub-joints (1-10) and finger MCPs (17-22)
            val hidden = i in 1..10 || i in 17..22
            if (!hidden && lm.visibility > 0.3f)
                Pair(PointF(lm.x, lm.y), jointColor(i))
            else null
        }
    }

    // Sample key face contour landmarks from the 478-point FaceLandmarker output.
    // Indices follow MediaPipe Face Mesh topology: face oval + eyes + lips + nose.
    private val FACE_CONTOUR_INDICES = listOf(
        // Face oval (36 points)
        10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
        397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
        172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
        // Left eye
        33, 7, 163, 144, 145, 153, 154, 155, 133, 246, 161, 160, 159, 158, 157, 173,
        // Right eye
        263, 249, 390, 373, 374, 380, 381, 382, 362, 466, 388, 387, 386, 385, 384, 398,
        // Lips outer
        61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291, 375, 321, 405, 314, 17, 84, 181, 91, 146,
        // Nose bridge
        168, 6, 197, 195, 5, 4, 1, 19, 94, 2
    )

    private fun buildFaceMeshPoints(face: List<NormalizedLandmark>?): List<PointF> {
        face ?: return emptyList()
        return FACE_CONTOUR_INDICES.mapNotNull { i ->
            if (i < face.size) PointF(face[i].x, face[i].y) else null
        }
    }

    fun getTrainingSessionSnapshot(): TrainingSessionSnapshot {
        val temporal = compressionDetector.getTemporalState(SystemClock.elapsedRealtime())
        return TrainingSessionSnapshot(
            completedFSMStates = fsm.completed.toSet(),
            compressionCount = temporal.compressionCount,
            rateStatus = temporal.rateStatus,
            strokeSignals = strokeModule.getStrokeSignals(),
            strokeIsComplete = strokeModule.isResultReached,
            allergicPhaseOrdinal = allergicModule.currentPhaseOrdinal,
            emergencyType = fsm.getConfirmedEmergencyType()
        )
    }

}
