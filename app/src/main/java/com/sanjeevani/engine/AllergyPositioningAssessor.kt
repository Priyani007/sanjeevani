package com.sanjeevani.engine

import android.graphics.PointF
import com.sanjeevani.model.NormalizedLandmark
import com.sanjeevani.model.SpatialAction
import kotlin.math.abs
import kotlin.math.sqrt

private const val RIGHT_HIP_LM   = 24
private const val RIGHT_KNEE_LM  = 26
private const val RIGHT_ANKLE_LM = 28
private const val LEFT_HIP_LM    = 23
private const val LEFT_KNEE_LM   = 25
private const val LEFT_ANKLE_LM  = 27
private const val LEFT_WRIST_LM  = 15
private const val RIGHT_WRIST_LM = 16

private const val VIS_THRESHOLD          = 0.50f
private const val LEG_ELEV_FRACTION      = 0.08f  // ankle must be this fraction of leg-length above hip (screen Y)
private const val LEG_CONSISTENCY_FRAMES = 5       // consecutive frames required before committing to a result
private const val THIGH_CORRECT_DIST     = 0.10f   // normalized units — wrist within this = CORRECT
private const val THIGH_HYSTERESIS       = 0.03f
private const val WRIST_HOLD_MS          = 700L    // min time before issuing a new direction

enum class AllergyPositionResult { CORRECT, INCORRECT, INCONCLUSIVE }

data class AllergyWristState(
    val wristPosition: PointF?,
    val thighTarget: PointF?,
    val errorVector: PointF?,           // smoothed displacement (wrist − thigh); null when tracking lost
    val correctiveAction: SpatialAction,
    val result: AllergyPositionResult
)

/**
 * Camera-based assessments for Allergic Reaction physical training steps.
 *
 * Limitations stated explicitly (safety policy):
 *   - assessPatientLying wraps PatientDetector 2-D heuristics; cannot verify supine vs prone.
 *   - assessLegElevation is a screen-space Y comparison; not a calibrated 3-D angle.
 *     It is a guided-simulation aid, not validated clinical measurement.
 *   - assessWristToThigh is a positional proxy; not physically validated for injection safety.
 */
class AllergyPositioningAssessor {

    private val patientDetector = PatientDetector()

    // ── Leg elevation smoothing ───────────────────────────────────────────────
    private val smoothedLegElev = ExponentialMovingAverage(alpha = 0.25f)
    private var legOkStreak   = 0
    private var legFailStreak = 0

    // ── Wrist-to-thigh tracking ───────────────────────────────────────────────
    private val smoothedThighX = ExponentialMovingAverage(alpha = 0.20f)
    private val smoothedThighY = ExponentialMovingAverage(alpha = 0.20f)
    private val smoothedErrX   = ExponentialMovingAverage(alpha = 0.22f)
    private val smoothedErrY   = ExponentialMovingAverage(alpha = 0.22f)
    private var lastWristAction   = SpatialAction.UNSURE
    private var lastWristActionTs = 0L

    fun resetAll() {
        smoothedLegElev.reset(); legOkStreak = 0; legFailStreak = 0
        smoothedThighX.reset(); smoothedThighY.reset()
        smoothedErrX.reset(); smoothedErrY.reset()
        lastWristAction = SpatialAction.UNSURE; lastWristActionTs = 0L
    }

    // ── Patient lying-flat ────────────────────────────────────────────────────

    fun assessPatientLying(
        pose: List<NormalizedLandmark>?,
        worldLandmarks: List<NormalizedLandmark>? = null
    ): Pair<AllergyPositionResult, Float> {
        val (lying, conf) = patientDetector.isPatientLying(pose, worldLandmarks)
        return when {
            conf < 0.40f -> Pair(AllergyPositionResult.INCONCLUSIVE, conf)
            lying        -> Pair(AllergyPositionResult.CORRECT, conf)
            else         -> Pair(AllergyPositionResult.INCORRECT, conf)
        }
    }

    // ── Leg elevation ─────────────────────────────────────────────────────────

    /**
     * Checks whether the patient's legs appear elevated by comparing ankle Y to hip Y
     * in normalized screen coordinates. When the camera is held above a lying patient,
     * a raised ankle has a lower Y value (higher on screen) than the hip.
     *
     * This is a guided-simulation heuristic only — camera orientation and patient angle
     * both affect the reading. Never present this as a verified clinical measurement.
     */
    fun assessLegElevation(pose: List<NormalizedLandmark>?): AllergyPositionResult {
        pose ?: return AllergyPositionResult.INCONCLUSIVE
        val (hip, _, ankle) = bestVisibleLeg(pose) ?: return AllergyPositionResult.INCONCLUSIVE

        val legLen = abs(hip.y - ankle.y).coerceAtLeast(0.05f)
        // Positive = ankle is above hip on screen; negative = ankle is below hip.
        val elev = (hip.y - ankle.y) / legLen
        val smoothed = smoothedLegElev.update(elev)

        return when {
            smoothed > LEG_ELEV_FRACTION -> {
                legOkStreak++; legFailStreak = 0
                if (legOkStreak >= LEG_CONSISTENCY_FRAMES) AllergyPositionResult.CORRECT
                else AllergyPositionResult.INCONCLUSIVE
            }
            smoothed < -0.02f -> {
                legFailStreak++; legOkStreak = 0
                if (legFailStreak >= LEG_CONSISTENCY_FRAMES) AllergyPositionResult.INCORRECT
                else AllergyPositionResult.INCONCLUSIVE
            }
            else -> AllergyPositionResult.INCONCLUSIVE
        }
    }

    // ── Wrist-to-thigh (EpiPen placement simulation) ─────────────────────────

    /** EMA-smoothed right outer-thigh midpoint; null when landmarks below threshold. */
    fun estimateThighTarget(pose: List<NormalizedLandmark>?): PointF? {
        val raw = rawThighTarget(pose) ?: return null
        return PointF(
            smoothedThighX.update(raw.first),
            smoothedThighY.update(raw.second)
        )
    }

    /**
     * Returns the raw (unsmoothed) thigh x/y from NormalizedLandmark fields.
     * Used internally so computations work in JVM unit tests where PointF.x/.y are stubs.
     */
    internal fun rawThighTarget(pose: List<NormalizedLandmark>?): Pair<Float, Float>? {
        pose ?: return null
        val hip  = pose.getOrNull(RIGHT_HIP_LM)  ?: return null
        val knee = pose.getOrNull(RIGHT_KNEE_LM) ?: return null
        if (hip.visibility < VIS_THRESHOLD || knee.visibility < VIS_THRESHOLD) return null
        return Pair(
            smoothedThighX.update((hip.x + knee.x) / 2f),
            smoothedThighY.update((hip.y + knee.y) / 2f)
        )
    }

    /**
     * Assess whether the learner's wrist is positioned over the thigh target.
     * Mirrors SpatialReasoner's directional-correction pattern used for CPR hand placement.
     * Returns directional guidance and a result for the AR arrow and step card.
     *
     * All arithmetic uses NormalizedLandmark.x/.y directly (pure Kotlin fields) so the
     * directional logic is fully testable on the JVM without Android stubs.
     */
    fun assessWristToThigh(pose: List<NormalizedLandmark>?, nowMs: Long): AllergyWristState {
        val (thighX, thighY) = rawThighTarget(pose)
            ?: return AllergyWristState(null, null, null, SpatialAction.TRACKING_LOST, AllergyPositionResult.INCONCLUSIVE)
        val thighPoint = PointF(thighX, thighY)

        val lwLm = pose?.getOrNull(LEFT_WRIST_LM)?.takeIf  { it.visibility >= VIS_THRESHOLD }
        val rwLm = pose?.getOrNull(RIGHT_WRIST_LM)?.takeIf { it.visibility >= VIS_THRESHOLD }

        val (wristX, wristY) = when {
            lwLm != null && rwLm != null -> {
                val ldSq = (lwLm.x - thighX) * (lwLm.x - thighX) + (lwLm.y - thighY) * (lwLm.y - thighY)
                val rdSq = (rwLm.x - thighX) * (rwLm.x - thighX) + (rwLm.y - thighY) * (rwLm.y - thighY)
                if (ldSq <= rdSq) Pair(lwLm.x, lwLm.y) else Pair(rwLm.x, rwLm.y)
            }
            lwLm != null -> Pair(lwLm.x, lwLm.y)
            rwLm != null -> Pair(rwLm.x, rwLm.y)
            else -> return AllergyWristState(null, thighPoint, null, SpatialAction.TRACKING_LOST, AllergyPositionResult.INCONCLUSIVE)
        }
        val wristPoint = PointF(wristX, wristY)

        val rawDx = wristX - thighX
        val rawDy = wristY - thighY
        val sdx = smoothedErrX.update(rawDx)
        val sdy = smoothedErrY.update(rawDy)
        val rawMag = sqrt(rawDx * rawDx + rawDy * rawDy)

        // Break out of sticky CORRECT if wrist has drifted past threshold + hysteresis
        if (rawMag > THIGH_CORRECT_DIST + THIGH_HYSTERESIS && lastWristAction == SpatialAction.CORRECT) {
            lastWristAction = SpatialAction.UNSURE; lastWristActionTs = 0L
        }

        val sMag = sqrt(sdx * sdx + sdy * sdy)
        // Error vector = (wrist − thigh): positive dx → wrist right of thigh → MOVE_LEFT.
        // positive dy → wrist below thigh on screen (higher Y) → MOVE_UP.
        // Matches SpatialReasoner convention used for CPR hand placement.
        val desired = when {
            sMag < THIGH_CORRECT_DIST - THIGH_HYSTERESIS -> SpatialAction.CORRECT
            abs(sdx) >= abs(sdy) -> if (sdx > 0) SpatialAction.MOVE_LEFT  else SpatialAction.MOVE_RIGHT
            else                 -> if (sdy > 0) SpatialAction.MOVE_UP    else SpatialAction.MOVE_DOWN
        }
        if (desired != lastWristAction && nowMs - lastWristActionTs >= WRIST_HOLD_MS) {
            lastWristAction = desired; lastWristActionTs = nowMs
        }

        val result = when (lastWristAction) {
            SpatialAction.CORRECT -> AllergyPositionResult.CORRECT
            SpatialAction.TRACKING_LOST, SpatialAction.UNSURE -> AllergyPositionResult.INCONCLUSIVE
            else -> AllergyPositionResult.INCORRECT
        }
        return AllergyWristState(wristPoint, thighPoint, PointF(sdx, sdy), lastWristAction, result)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun bestVisibleLeg(
        pose: List<NormalizedLandmark>
    ): Triple<NormalizedLandmark, NormalizedLandmark, NormalizedLandmark>? {
        fun ok(v: Float) = v >= VIS_THRESHOLD
        val rh = pose.getOrNull(RIGHT_HIP_LM); val rk = pose.getOrNull(RIGHT_KNEE_LM); val ra = pose.getOrNull(RIGHT_ANKLE_LM)
        val lh = pose.getOrNull(LEFT_HIP_LM);  val lk = pose.getOrNull(LEFT_KNEE_LM);  val la = pose.getOrNull(LEFT_ANKLE_LM)
        val rightOk = rh != null && rk != null && ra != null && ok(rh.visibility) && ok(rk.visibility) && ok(ra.visibility)
        val leftOk  = lh != null && lk != null && la != null && ok(lh.visibility) && ok(lk.visibility) && ok(la.visibility)
        return when {
            rightOk -> Triple(rh!!, rk!!, ra!!)
            leftOk  -> Triple(lh!!, lk!!, la!!)
            else    -> null
        }
    }
}
