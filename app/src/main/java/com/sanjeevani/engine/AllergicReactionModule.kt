package com.sanjeevani.engine

import android.graphics.PointF
import com.sanjeevani.model.*

private val PHASE_VOICE = mapOf(
    AllergicPhase.LAY_FLAT      to "Lay them flat on the ground. Do not let them stand up.",
    AllergicPhase.RAISE_LEGS    to "Raise their legs above heart level. This helps blood flow to the brain.",
    AllergicPhase.SAFE_POSITION to "Keep them in the safest position for their breathing and follow the dispatcher.",
    AllergicPhase.EPIPEN_READY  to "Use a trainer only for practice. The marker shows the outer middle thigh target.",
    AllergicPhase.EPIPEN_INJECT to "For a real prescribed device, follow its label and the dispatcher. Never use a live injector for practice.",
    AllergicPhase.MONITORING    to "Monitor breathing and stay with them. Help is on the way. Call 1 1 2 now if not already done."
)

class AllergicReactionModule : EmergencyModule {

    private val assessor = AllergyPositioningAssessor()

    private var currentPhase = AllergicPhase.LAY_FLAT
    private var trainingStepIndex = 0
    private var lastSpokenPhase: AllergicPhase? = null

    // Latest assessment results — read by SanjeevaniEngine for step-card status
    var currentAssessmentStatus: String = "Camera observation unavailable"
        private set
    var currentAssessmentGreen: Boolean = false
        private set

    override fun process(frame: PerceptionFrame, nowMs: Long): ModuleResult {
        val voiceText = if (lastSpokenPhase != currentPhase) {
            lastSpokenPhase = currentPhase
            PHASE_VOICE[currentPhase]
        } else null

        val progress = currentPhase.ordinal.toFloat() / (AllergicPhase.values().size - 1)
        val pose = frame.poseLandmarks

        val (overlay, green, statusLine) = buildOverlay(pose, progress, nowMs)

        currentAssessmentGreen = green
        currentAssessmentStatus = statusLine

        return ModuleResult(voiceText, overlay, currentPhase == AllergicPhase.MONITORING)
    }

    override fun reset() {
        currentPhase = AllergicPhase.LAY_FLAT
        trainingStepIndex = 0
        lastSpokenPhase = null
        assessor.resetAll()
        currentAssessmentStatus = "Camera observation unavailable"
        currentAssessmentGreen = false
    }

    /** Training phases follow assessed learner steps; elapsed time never advances them. */
    fun selectTrainingStep(stepIndex: Int) {
        trainingStepIndex = stepIndex
        val next = when (stepIndex) {
            2    -> AllergicPhase.RAISE_LEGS
            3    -> AllergicPhase.EPIPEN_READY
            4    -> AllergicPhase.EPIPEN_INJECT
            5, 6 -> AllergicPhase.MONITORING
            else -> AllergicPhase.LAY_FLAT
        }
        if (next != currentPhase) {
            currentPhase = next
            lastSpokenPhase = null
            assessor.resetAll()
        }
    }

    val currentPhaseOrdinal: Int get() = currentPhase.ordinal
    val currentTrainingStepIndex: Int get() = trainingStepIndex

    // ── AR overlay per phase ──────────────────────────────────────────────────

    private data class OverlayBundle(
        val spec: AROverlaySpec,
        val green: Boolean,
        val statusLine: String
    )

    private fun buildOverlay(
        pose: List<NormalizedLandmark>?,
        progress: Float,
        nowMs: Long
    ): OverlayBundle {
        return when (currentPhase) {
            AllergicPhase.LAY_FLAT      -> buildLayFlatOverlay(pose, progress)
            AllergicPhase.RAISE_LEGS    -> buildRaiseLegsOverlay(pose, progress)
            AllergicPhase.SAFE_POSITION -> buildSafePositionOverlay(pose, progress)
            AllergicPhase.EPIPEN_READY  -> buildEpiPenReadyOverlay(pose, progress)
            AllergicPhase.EPIPEN_INJECT -> buildEpiPenInjectOverlay(pose, progress, nowMs)
            AllergicPhase.MONITORING    -> buildMonitoringOverlay(pose, progress)
        }
    }

    private fun buildLayFlatOverlay(pose: List<NormalizedLandmark>?, progress: Float): OverlayBundle {
        val (result, conf) = assessor.assessPatientLying(pose)
        val green = result == AllergyPositionResult.CORRECT
        val status = when (result) {
            AllergyPositionResult.CORRECT     -> "✓ Position estimate stable • confirm flat surface"
            AllergyPositionResult.INCORRECT   -> "Not lying flat detected • guide them down"
            AllergyPositionResult.INCONCLUSIVE -> if (conf < 0.30f)
                "Show full body including shoulders and hips"
            else
                "Waiting for stable position estimate"
        }
        val spec = AROverlaySpec(
            statusText = "Lay flat",
            guidanceText = PHASE_VOICE[AllergicPhase.LAY_FLAT] ?: "",
            statusColorGreen = green,
            emergencyType = EmergencyType.ALLERGIC_REACTION,
            phaseProgress = progress,
            thighTarget = assessor.estimateThighTarget(pose)
        )
        return OverlayBundle(spec, green, status)
    }

    private fun buildRaiseLegsOverlay(pose: List<NormalizedLandmark>?, progress: Float): OverlayBundle {
        val result = assessor.assessLegElevation(pose)
        val green = result == AllergyPositionResult.CORRECT
        val status = when (result) {
            AllergyPositionResult.CORRECT     -> "✓ Leg elevation estimate detected (simulation)"
            AllergyPositionResult.INCORRECT   -> "Legs appear flat • raise legs above heart level"
            AllergyPositionResult.INCONCLUSIVE -> "Show hip, knee, and ankle to camera"
        }

        // Upward arrow from ankle toward target when legs appear too low
        var arrowFrom: PointF? = null
        var arrowTo:   PointF? = null
        if (result == AllergyPositionResult.INCORRECT && pose != null) {
            val ankle = pose.getOrNull(28)?.takeIf { it.visibility >= 0.50f }
                ?: pose.getOrNull(27)?.takeIf { it.visibility >= 0.50f }
            val hip = pose.getOrNull(24)?.takeIf { it.visibility >= 0.50f }
                ?: pose.getOrNull(23)?.takeIf { it.visibility >= 0.50f }
            if (ankle != null) {
                arrowFrom = PointF(ankle.x, ankle.y)
                // Target: ankle should be at or above hip level; screen Y decreases upward
                val targetY = if (hip != null) (hip.y - 0.06f).coerceAtLeast(0.05f)
                              else            (ankle.y - 0.18f).coerceAtLeast(0.05f)
                arrowTo = PointF(ankle.x, targetY)
            }
        }

        val spec = AROverlaySpec(
            statusText = "Raise legs",
            guidanceText = PHASE_VOICE[AllergicPhase.RAISE_LEGS] ?: "",
            statusColorGreen = green,
            emergencyType = EmergencyType.ALLERGIC_REACTION,
            phaseProgress = progress,
            thighTarget = assessor.estimateThighTarget(pose),
            arrowFrom = arrowFrom,
            arrowTo = arrowTo
        )
        return OverlayBundle(spec, green, status)
    }

    private fun buildSafePositionOverlay(pose: List<NormalizedLandmark>?, progress: Float): OverlayBundle {
        val spec = AROverlaySpec(
            statusText = "✓ Safe position",
            guidanceText = PHASE_VOICE[AllergicPhase.SAFE_POSITION] ?: "",
            statusColorGreen = true,
            emergencyType = EmergencyType.ALLERGIC_REACTION,
            phaseProgress = progress,
            thighTarget = assessor.estimateThighTarget(pose)
        )
        return OverlayBundle(spec, true, "✓ Safe position maintained • follow dispatcher")
    }

    private fun buildEpiPenReadyOverlay(pose: List<NormalizedLandmark>?, progress: Float): OverlayBundle {
        val thigh = assessor.estimateThighTarget(pose)
        val thighVisible = thigh != null
        val status = if (thighVisible) "Thigh reference visible • trainer simulation only"
                     else "Move right hip and knee into camera frame"
        val spec = AROverlaySpec(
            statusText = "EpiPen ready",
            guidanceText = PHASE_VOICE[AllergicPhase.EPIPEN_READY] ?: "",
            statusColorGreen = thighVisible,
            emergencyType = EmergencyType.ALLERGIC_REACTION,
            phaseProgress = progress,
            showEpiPenMarker = true,
            thighTarget = thigh
        )
        return OverlayBundle(spec, thighVisible, status)
    }

    private fun buildEpiPenInjectOverlay(
        pose: List<NormalizedLandmark>?,
        progress: Float,
        nowMs: Long
    ): OverlayBundle {
        val wristState = assessor.assessWristToThigh(pose, nowMs)
        val green = wristState.result == AllergyPositionResult.CORRECT
        val status = when (wristState.correctiveAction) {
            SpatialAction.CORRECT        -> "✓ Wrist on target • hold steady"
            SpatialAction.MOVE_LEFT      -> "← Move left toward thigh"
            SpatialAction.MOVE_RIGHT     -> "Move right toward thigh →"
            SpatialAction.MOVE_UP        -> "↑ Move up toward thigh"
            SpatialAction.MOVE_DOWN      -> "↓ Move down toward thigh"
            SpatialAction.TRACKING_LOST  -> "Move hip, knee, and wrist into camera frame"
            else                         -> "Align wrist over the thigh marker"
        }
        val showArrow = wristState.correctiveAction !in setOf(SpatialAction.CORRECT, SpatialAction.TRACKING_LOST, SpatialAction.UNSURE)
        val spec = AROverlaySpec(
            statusText = "Inject EpiPen",
            guidanceText = PHASE_VOICE[AllergicPhase.EPIPEN_INJECT] ?: "",
            statusColorGreen = green,
            emergencyType = EmergencyType.ALLERGIC_REACTION,
            phaseProgress = progress,
            showEpiPenMarker = true,
            thighTarget = wristState.thighTarget,
            arrowFrom = wristState.wristPosition.takeIf { showArrow },
            arrowTo   = wristState.thighTarget.takeIf { showArrow }
        )
        return OverlayBundle(spec, green, status)
    }

    private fun buildMonitoringOverlay(pose: List<NormalizedLandmark>?, progress: Float): OverlayBundle {
        val spec = AROverlaySpec(
            statusText = "Monitoring",
            guidanceText = PHASE_VOICE[AllergicPhase.MONITORING] ?: "",
            statusColorGreen = true,
            emergencyType = EmergencyType.ALLERGIC_REACTION,
            phaseProgress = progress,
            thighTarget = assessor.estimateThighTarget(pose)
        )
        return OverlayBundle(spec, true, "✓ Monitoring • help is on the way")
    }

    // Right outer thigh midpoint: between right hip (24) and right knee (26).
    // Kept for compatibility; new code uses assessor.estimateThighTarget().
    private fun rightThighOf(pose: List<NormalizedLandmark>): PointF? {
        if (pose.size < 27) return null
        val hip = pose[24]; val knee = pose[26]
        if (hip.visibility < 0.4f || knee.visibility < 0.4f) return null
        return PointF((hip.x + knee.x) / 2f, (hip.y + knee.y) / 2f)
    }
}
