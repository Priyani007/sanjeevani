package com.sanjeevani.engine

import android.graphics.PointF
import com.sanjeevani.model.NormalizedLandmark
import com.sanjeevani.model.SpatialAction
import com.sanjeevani.model.SpatialState
import kotlin.math.abs
import kotlin.math.sqrt

// MediaPipe Pose landmark indices
private const val LEFT_SHOULDER = 11
private const val RIGHT_SHOULDER = 12
private const val LEFT_HIP = 23
private const val RIGHT_HIP = 24

// MediaPipe Hand landmark indices (palm keypoints for center estimation)
private const val INDEX_FINGER_MCP = 5
private const val MIDDLE_FINGER_MCP = 9
private const val RING_FINGER_MCP = 13
private const val PINKY_MCP = 17

private const val STERNUM_TORSO_RATIO = 0.45f   // Approximate screen-space chest target; not clinical validation
private const val CORRECTION_THRESHOLD = 0.09f  // Wider zone — tolerates camera wobble during compressions
private const val HYSTERESIS = 0.03f            // Larger dead-band so CORRECT state is sticky
private const val MIN_ACTION_CHANGE_MS = 800L   // Longer hold before issuing a new direction

// Sternum stability constants
// Keep last known sternum position this long after landmarks drop below threshold.
// Eliminates jumps from single-frame occlusion events during CPR hand movements.
private const val STERNUM_HOLD_MS = 600L
// If the sternum landmark estimate jumps more than this in one frame, it's a bad
// inference frame (motion blur, compression artifact, wrong person detected).
// Hold the current smoothed value instead of letting the spike through.
private const val OUTLIER_THRESHOLD = 0.12f

class SpatialReasoner {

    private var selectedHand: String? = null
    private var lastAction = SpatialAction.UNSURE
    private var lastActionChangeTs = 0L
    private val smoothedErrorX = ExponentialMovingAverage(alpha = 0.2f)
    private val smoothedErrorY = ExponentialMovingAverage(alpha = 0.2f)
    private val smoothedSternumX = ExponentialMovingAverage(alpha = 0.18f)
    private val smoothedSternumY = ExponentialMovingAverage(alpha = 0.18f)
    private var lastSternumX: Float? = null
    private var lastSternumY: Float? = null
    private var lastGoodSternumTs: Long = 0L   // timestamp of last valid landmark-based estimate

    fun reset() {
        smoothedSternumX.reset()
        smoothedSternumY.reset()
        smoothedErrorX.reset()
        smoothedErrorY.reset()
        lastSternumX = null
        lastSternumY = null
        lastGoodSternumTs = 0L
        lastAction = SpatialAction.UNSURE
        lastActionChangeTs = 0L
        selectedHand = null
    }

    fun compute(
        poseLandmarks: List<NormalizedLandmark>?,
        leftHand: List<NormalizedLandmark>?,
        rightHand: List<NormalizedLandmark>?,
        nowMs: Long
    ): SpatialState {
        val target = estimateSternumTarget(poseLandmarks, nowMs)
        if (target == null) {
            // Truly lost (landmark gap exceeded STERNUM_HOLD_MS): clear hand state too.
            selectedHand = null
            smoothedErrorX.reset()
            smoothedErrorY.reset()
            lastAction = SpatialAction.UNSURE
            lastActionChangeTs = 0L
            return SpatialState(correctiveAction = SpatialAction.TRACKING_LOST)
        }

        // Single-hand mode: if both hands happen to be visible, track whichever
        // palm is closer to the sternum instead of averaging or requiring stacking.
        val handCenter = estimateSingleHandCenter(leftHand, rightHand, target)

        if (handCenter == null) return SpatialState(
            sternumTarget = target,
            correctiveAction = SpatialAction.TRACKING_LOST
        )

        val rawDx = handCenter.x - target.x
        val rawDy = handCenter.y - target.y
        val smoothDx = smoothedErrorX.update(rawDx)
        val smoothDy = smoothedErrorY.update(rawDy)
        val mag = sqrt(smoothDx * smoothDx + smoothDy * smoothDy)

        val rawMag = sqrt(rawDx * rawDx + rawDy * rawDy)
        val action = if (rawMag > CORRECTION_THRESHOLD + HYSTERESIS && lastAction == SpatialAction.CORRECT) {
            lastAction = SpatialAction.UNSURE
            lastActionChangeTs = 0L
            computeAction(rawDx, rawDy, rawMag, nowMs)
        } else computeAction(smoothDx, smoothDy, mag, nowMs)

        return SpatialState(
            sternumTarget = target,
            handMidpoint = handCenter,
            errorVector = PointF(smoothDx, smoothDy),
            errorMagnitude = mag,
            correctiveAction = if (action == SpatialAction.CORRECT && rawMag > CORRECTION_THRESHOLD + HYSTERESIS)
                SpatialAction.UNSURE else action,
            trackedHand = selectedHand
        )
    }

    private fun estimateSternumTarget(pose: List<NormalizedLandmark>?, nowMs: Long): PointF? {
        if (pose == null || pose.size <= RIGHT_HIP) return holdOrNull(nowMs)
        val ls = pose[LEFT_SHOULDER]
        val rs = pose[RIGHT_SHOULDER]
        val lh = pose[LEFT_HIP]
        val rh = pose[RIGHT_HIP]

        if (listOf(ls, rs, lh, rh).any { it.visibility < .65f || !it.x.isFinite() || !it.y.isFinite() })
            return holdOrNull(nowMs)

        val shoulderMidX = (ls.x + rs.x) / 2f
        val shoulderMidY = (ls.y + rs.y) / 2f
        val hipMidX = (lh.x + rh.x) / 2f
        val hipMidY = (lh.y + rh.y) / 2f
        val rawX = shoulderMidX + (hipMidX - shoulderMidX) * STERNUM_TORSO_RATIO
        val rawY = shoulderMidY + (hipMidY - shoulderMidY) * STERNUM_TORSO_RATIO

        // Outlier rejection: if the new estimate jumps more than OUTLIER_THRESHOLD from the
        // current smoothed position in a single frame, it is almost certainly a bad inference
        // frame (motion blur, JPEG artefact, wrong pose detected). Hold the current value.
        val curX = smoothedSternumX.current
        val curY = smoothedSternumY.current
        if (curX != null && curY != null) {
            val jump = kotlin.math.hypot(rawX - curX, rawY - curY)
            if (jump > OUTLIER_THRESHOLD) {
                lastGoodSternumTs = nowMs   // reset expiry — we still have a good held position
                return PointF(curX, curY)
            }
        }

        // Adaptive EMA alpha: track camera/body motion quickly, suppress jitter when still.
        val moveDist = if (curX != null && curY != null)
            kotlin.math.hypot(rawX - curX, rawY - curY) else 0f
        val adaptAlpha = when {
            moveDist > 0.05f -> 0.28f   // fast movement — follow quickly
            moveDist > 0.015f -> 0.18f  // moderate
            else -> 0.10f               // nearly stationary — heavily smooth jitter
        }
        smoothedSternumX.alpha = adaptAlpha
        smoothedSternumY.alpha = adaptAlpha

        val sx = smoothedSternumX.update(rawX)
        val sy = smoothedSternumY.update(rawY)
        lastSternumX = sx
        lastSternumY = sy
        lastGoodSternumTs = nowMs
        return PointF(sx, sy)
    }

    /** Keep last known sternum for STERNUM_HOLD_MS after landmarks drop below threshold.
     *  Prevents jumps from single-frame occlusion during CPR hand movement. */
    private fun holdOrNull(nowMs: Long): PointF? {
        val x = lastSternumX ?: return null
        val y = lastSternumY ?: return null
        if (nowMs - lastGoodSternumTs > STERNUM_HOLD_MS) {
            // Prolonged tracking loss — clear fully so next acquisition starts fresh.
            lastSternumX = null
            lastSternumY = null
            smoothedSternumX.reset()
            smoothedSternumY.reset()
            return null
        }
        return PointF(x, y)
    }

    private fun estimateSingleHandCenter(
        left: List<NormalizedLandmark>?,
        right: List<NormalizedLandmark>?,
        target: PointF
    ): PointF? {
        val lc = left?.let { palmCenter(it) }
        val rc = right?.let { palmCenter(it) }
        if (selectedHand == null) selectedHand = when {
            lc != null && rc != null -> if (distanceSquared(lc, target) <= distanceSquared(rc, target)) "left" else "right"
            lc != null -> "left"
            rc != null -> "right"
            else -> null
        }
        val result = if (selectedHand == "left") lc else rc
        if (result == null) { selectedHand = null; smoothedErrorX.reset(); smoothedErrorY.reset() }
        return result
    }

    private fun palmCenter(hand: List<NormalizedLandmark>): PointF? {
        if (hand.size <= PINKY_MCP || hand.any { !it.x.isFinite() || !it.y.isFinite() || it.visibility < .65f }) return null
        val x = (hand[INDEX_FINGER_MCP].x + hand[MIDDLE_FINGER_MCP].x +
                 hand[RING_FINGER_MCP].x + hand[PINKY_MCP].x) / 4f
        val y = (hand[INDEX_FINGER_MCP].y + hand[MIDDLE_FINGER_MCP].y +
                 hand[RING_FINGER_MCP].y + hand[PINKY_MCP].y) / 4f
        return PointF(x, y)
    }

    private fun estimateWristCenter(pose: List<NormalizedLandmark>?, target: PointF): PointF? {
        pose ?: return null
        if (pose.size <= 16) return null
        val lw = pose[15]  // LEFT_WRIST
        val rw = pose[16]  // RIGHT_WRIST
        val left = PointF(lw.x, lw.y).takeIf { lw.visibility >= 0.4f }
        val right = PointF(rw.x, rw.y).takeIf { rw.visibility >= 0.4f }
        return when {
            left != null && right != null -> if (distanceSquared(left, target) <= distanceSquared(right, target)) left else right
            left != null -> left
            else -> right
        }
    }

    private fun distanceSquared(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun computeAction(dx: Float, dy: Float, mag: Float, nowMs: Long): SpatialAction {
        val newAction = when {
            mag < CORRECTION_THRESHOLD - HYSTERESIS -> SpatialAction.CORRECT
            abs(dx) > abs(dy) -> if (dx > 0) SpatialAction.MOVE_LEFT else SpatialAction.MOVE_RIGHT
            else -> if (dy > 0) SpatialAction.MOVE_UP else SpatialAction.MOVE_DOWN
        }
        if (newAction == lastAction) return lastAction
        if (nowMs - lastActionChangeTs < MIN_ACTION_CHANGE_MS) return lastAction
        lastAction = newAction
        lastActionChangeTs = nowMs
        return newAction
    }
}

class ExponentialMovingAverage(var alpha: Float) {
    private var value: Float? = null
    val current: Float? get() = value
    fun update(newValue: Float): Float {
        value = value?.let { alpha * newValue + (1 - alpha) * it } ?: newValue
        return value!!
    }
    fun reset() { value = null }
}
