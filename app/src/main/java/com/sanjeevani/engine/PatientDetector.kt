package com.sanjeevani.engine

import com.sanjeevani.model.NormalizedLandmark
import kotlin.math.abs

private const val LEFT_SHOULDER = 11
private const val RIGHT_SHOULDER = 12
private const val LEFT_ELBOW = 13
private const val RIGHT_ELBOW = 14
private const val LEFT_WRIST = 15
private const val RIGHT_WRIST = 16
private const val LEFT_HIP = 23
private const val RIGHT_HIP = 24

// If one shoulder is this much more visible than the other, person is likely on their side.
// Side-lying: the bottom shoulder is partially occluded → lower visibility score.
private const val SIDE_LYING_VIS_ASYMMETRY = 0.25f
// Max allowed shoulder-to-shoulder world Y difference for lying FLAT (vs on side).
// On back: both shoulders press equally into ground → near-zero. On side: one is ~0.2m higher.
private const val MAX_SHOULDER_WORLD_Y_DIFF = 0.18f
// Arm landmarks are used as a direct side-lying signal. A patient on their back should
// have one arm on either side of the torso; a side-lying patient commonly has both arms
// projected onto the same side. The margin prevents jitter near the torso centre line.
// Match the skeleton renderer so any arm visibly drawn to the user also participates
// in posture validation.
private const val MIN_ARM_VISIBILITY = 0.40f
private const val ARM_SIDE_MARGIN = 0.02f

class PatientDetector {

    fun isPatientLying(
        pose: List<NormalizedLandmark>?,
        worldLandmarks: List<NormalizedLandmark>? = null
    ): Pair<Boolean, Float> {
        pose ?: return Pair(false, 0f)
        if (pose.size <= RIGHT_HIP) return Pair(false, 0f)

        val ls = pose[LEFT_SHOULDER]
        val rs = pose[RIGHT_SHOULDER]
        val lh = pose[LEFT_HIP]
        val rh = pose[RIGHT_HIP]

        val avgVis = (ls.visibility + rs.visibility + lh.visibility + rh.visibility) / 4f
        if (listOf(ls, rs, lh, rh).any { it.visibility < .65f || !it.x.isFinite() || !it.y.isFinite() }) return Pair(false, 0f)

        // If one shoulder is significantly less visible than the other, person is on their side.
        // This is a negative gate — if triggered, we know they are NOT lying flat.
        val visAsymmetry = abs(ls.visibility - rs.visibility)
        if (visAsymmetry > SIDE_LYING_VIS_ASYMMETRY) return Pair(false, 0f)

        // Reject side-lying when both reliably tracked arms are on the same side of the
        // torso. This test is mirror-safe: it only requires the two arm points to have
        // opposite signs relative to the body's centre line.
        val leftArm = bestVisibleArmPoint(pose[LEFT_WRIST], pose[LEFT_ELBOW])
        val rightArm = bestVisibleArmPoint(pose[RIGHT_WRIST], pose[RIGHT_ELBOW])
        // Do not confirm a safety-critical posture if the camera cannot verify both arms.
        if (leftArm == null || rightArm == null) return Pair(false, 0f)

        // Signed distance from shoulder-to-hip axis, rather than screen X:
        // a rotated body must not turn same-side arms into opposite-side arms.
        val sx = (ls.x + rs.x) / 2f
        val sy = (ls.y + rs.y) / 2f
        val dx = (lh.x + rh.x) / 2f - sx
        val dy = (lh.y + rh.y) / 2f - sy
        val length = kotlin.math.hypot(dx, dy)
        if (length < .1f) return false to 0f
        fun offset(arm: NormalizedLandmark) = ((arm.x-sx)*dy - (arm.y-sy)*dx)/length
        val leftOffset = offset(leftArm)
        val rightOffset = offset(rightArm)
        val armsOnOppositeSides =
            (leftOffset < -ARM_SIDE_MARGIN && rightOffset > ARM_SIDE_MARGIN) ||
                (rightOffset < -ARM_SIDE_MARGIN && leftOffset > ARM_SIDE_MARGIN)
        if (!armsOnOppositeSides) return Pair(false, 0f)

        // 2D screen-space detection (original iOS algorithm)
        val shoulderSpanX = abs(ls.x - rs.x)
        val shoulderSpanY = abs(ls.y - rs.y)
        val isHorizontal = shoulderSpanX > 0.12f && shoulderSpanY < shoulderSpanX * 0.9f
        val isHorizontalIOS = shoulderSpanX > (shoulderSpanY + 0.001f) * 0.7f

        // MediaPipe world Y is not a calibrated gravity/ground axis. Do not use
        // it to claim that shoulders touch a surface or that a patient is supine.
        val detected = isHorizontal && isHorizontalIOS
        val confidence = if (detected) avgVis else 0f
        return Pair(confidence >= .65f, confidence)
    }

    private fun bestVisibleArmPoint(
        wrist: NormalizedLandmark,
        elbow: NormalizedLandmark
    ): NormalizedLandmark? = when {
        wrist.visibility >= MIN_ARM_VISIBILITY -> wrist
        elbow.visibility >= MIN_ARM_VISIBILITY -> elbow
        else -> null
    }
}
