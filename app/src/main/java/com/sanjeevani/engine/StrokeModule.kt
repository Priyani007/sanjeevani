package com.sanjeevani.engine

import android.graphics.PointF
import com.sanjeevani.model.*

// FAST stroke assessment: Face, Arms, Speech, Time
// States: INTRO → FACE → ARM → SPEECH → RESULT

private enum class FastStep { INTRO, FACE, ARM, SPEECH, RESULT }

class StrokeModule : EmergencyModule {
    private var currentStep = FastStep.INTRO
    private var lastSpokenStep: FastStep? = null

    private var facePositiveCount = 0
    private var faceObservationFrames = 0
    private var maxFaceAsymmetryRatio = 0f
    private var armDriftDetected = false
    private var armObservationFrames = 0
    private var armDriftPositiveCount = 0
    private var speechPositive = false
    private var speechInputReceived = false

    override fun process(frame: PerceptionFrame, nowMs: Long): ModuleResult {
        when (currentStep) {
            FastStep.FACE -> assessFaceAsymmetry(frame)
            FastStep.ARM -> assessArmDrift(frame)
            else -> Unit
        }

        val stepChanged = currentStep != lastSpokenStep
        val voiceText = if (stepChanged) {
            lastSpokenStep = currentStep
            voiceForStep(currentStep)
        } else null

        val positiveCount = countPositives()
        val overlay = buildOverlay(frame, positiveCount)

        val isComplete = currentStep == FastStep.RESULT

        return ModuleResult(voiceText, overlay, isComplete)
    }

    fun onSpeechResult(positive: Boolean) {
        if (currentStep == FastStep.SPEECH) {
            speechPositive = positive
            speechInputReceived = true
        }
    }

    /** Training steps are learner-driven. Time and camera frames never advance the protocol. */
    fun selectTrainingStep(stepIndex: Int) {
        val next = when (stepIndex) {
            1 -> FastStep.FACE
            2 -> FastStep.ARM
            3 -> FastStep.SPEECH
            else -> FastStep.INTRO
        }
        if (next != currentStep) {
            currentStep = next
            lastSpokenStep = null
        }
    }

    fun completeTraining() {
        currentStep = FastStep.RESULT
        lastSpokenStep = null
    }

    override fun reset() {
        currentStep = FastStep.INTRO
        lastSpokenStep = null
        facePositiveCount = 0
        faceObservationFrames = 0
        maxFaceAsymmetryRatio = 0f
        armDriftDetected = false
        armObservationFrames = 0
        armDriftPositiveCount = 0
        speechPositive = false
        speechInputReceived = false
    }

    private fun assessFaceAsymmetry(frame: PerceptionFrame) {
        // Face Mesh mouth corners (61, 291), normalized by eye-to-eye width (33, 263).
        // This is a training observation, not a stroke diagnosis.
        val face = frame.faceLandmarks ?: return
        if (face.size <= 291) return
        val faceWidth = kotlin.math.abs(face[33].x - face[263].x)
        if (faceWidth < 0.08f) return
        faceObservationFrames++
        val asymmetryRatio = kotlin.math.abs(face[61].y - face[291].y) / faceWidth
        maxFaceAsymmetryRatio = maxOf(maxFaceAsymmetryRatio, asymmetryRatio)
        if (asymmetryRatio > 0.08f) facePositiveCount++
    }

    private fun assessArmDrift(frame: PerceptionFrame) {
        val pose = frame.poseLandmarks ?: return
        if (pose.size <= 16) return
        val le = pose[13]; val re = pose[14]
        val lw = pose[15]; val rw = pose[16]
        if (le.visibility < 0.4f || re.visibility < 0.4f) return
        if (lw.visibility < 0.4f || rw.visibility < 0.4f) return
        armObservationFrames++
        val lDroop = if (lw.visibility > 0.4f) lw.y - le.y else 0f
        val rDroop = if (rw.visibility > 0.4f) rw.y - re.y else 0f
        if (kotlin.math.abs(lDroop - rDroop) > 0.12f) armDriftPositiveCount++
        // Require temporal agreement; one noisy frame must never become a finding.
        armDriftDetected = armDriftPositiveCount >= 5
    }

    private fun countPositives(): Int {
        var n = 0
        if (facePositiveCount > 5) n++
        if (armDriftDetected) n++
        if (speechInputReceived && speechPositive) n++
        return n
    }

    private fun voiceForStep(step: FastStep): String = when (step) {
        FastStep.INTRO   -> "I will now guide you through the FAST stroke test. Watch their face, arms, and speech."
        FastStep.FACE    -> "Ask them to smile. Look for drooping or unevenness on one side of their face."
        FastStep.ARM     -> "Ask them to raise both arms. Watch if one arm drifts downward."
        FastStep.SPEECH  -> "Ask them to repeat a simple phrase. Tap the button below: speech clear or slurred?"
        FastStep.RESULT  -> {
            val n = countPositives()
            when {
                n >= 1 -> "A FAST warning sign was reported or observed. Call one one two now. The camera cannot diagnose stroke. Note when symptoms began and follow the dispatcher."
                else   -> "The camera did not confirm a FAST sign. This does not rule out stroke. Call one one two for any new FAST symptom and follow the dispatcher."
            }
        }
    }

    private fun buildOverlay(frame: PerceptionFrame, positiveCount: Int): AROverlaySpec {
        val pose = frame.poseLandmarks
        val lsy = pose?.getOrNull(11)?.y ?: 0f
        val rsy = pose?.getOrNull(12)?.y ?: 0f
        val isHighRisk = positiveCount >= 2

        val statusText = when (currentStep) {
            FastStep.INTRO   -> "FAST Test"
            FastStep.FACE    -> if (faceObservationFrames >= 5) "Face reference visible • not a diagnosis" else "Move face into camera frame"
            FastStep.ARM     -> if (armObservationFrames >= 5) "Both arms visible • compare together" else "Show both arms to the camera"
            FastStep.SPEECH  -> if (speechInputReceived) "Speech response recorded" else "Speech cannot be camera-assessed"
            FastStep.RESULT  -> if (isHighRisk) "⚠ FAST sign observed" else "Assessment unconfirmed"
        }

        return AROverlaySpec(
            statusText = statusText,
            statusColorGreen = when (currentStep) {
                FastStep.FACE -> faceObservationFrames >= 5
                FastStep.ARM -> armObservationFrames >= 5
                FastStep.SPEECH -> speechInputReceived
                else -> false
            },
            emergencyType = EmergencyType.FAST_STROKE,
            phaseProgress = stepProgress(currentStep),
            leftShoulderY = lsy,
            rightShoulderY = rsy,
            guidanceText = voiceForStep(currentStep)
        )
    }

    private fun stepProgress(step: FastStep): Float {
        return when (step) {
            FastStep.INTRO   -> 0.1f
            FastStep.FACE    -> 0.35f
            FastStep.ARM     -> 0.6f
            FastStep.SPEECH  -> 0.8f
            FastStep.RESULT  -> 1.0f
        }
    }

    fun getStrokeSignals(): StrokeSignals = StrokeSignals(
        faceAsymmetryScore = maxFaceAsymmetryRatio,
        armDriftDetected = armDriftDetected,
        speechPrompted = speechInputReceived,
        positiveTestCount = countPositives(),
        faceObservationFrames = faceObservationFrames,
        armObservationFrames = armObservationFrames
    )

    val isResultReached: Boolean get() = currentStep == FastStep.RESULT
}
