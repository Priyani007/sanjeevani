package com.sanjeevani.engine

import com.sanjeevani.model.*

private const val BUFFER_SIZE = 90  // 3s at 30fps
private const val L_SHOULDER = 11
private const val R_SHOULDER = 12
private const val L_ELBOW = 13
private const val R_ELBOW = 14
private const val L_WRIST = 15
private const val R_WRIST = 16
private const val L_HIP = 23
private const val R_HIP = 24
private const val NOSE = 0

class EmergencyClassifier {

    private val poseBuffer = ArrayDeque<List<NormalizedLandmark>>(BUFFER_SIZE)

    fun addFrame(pose: List<NormalizedLandmark>?) {
        pose ?: return
        if (poseBuffer.size >= BUFFER_SIZE) poseBuffer.removeFirst()
        poseBuffer.addLast(pose)
    }

    fun classify(): ClassifierSignal {
        if (poseBuffer.size < 15) {
            return ClassifierSignal(EmergencyType.UNKNOWN, 0f, emptyList(), false)
        }

        val latest = poseBuffer.last()

        val cprScore = scoreCPR(latest)
        val heartScore = scoreHeartAttack(latest)
        val strokeScore = scoreStroke(latest)

        val best = listOf(
            EmergencyType.CPR to cprScore,
            EmergencyType.HEART_ATTACK to heartScore,
            EmergencyType.FAST_STROKE to strokeScore
        ).maxByOrNull { it.second }!!

        if (best.second < 0.35f) {
            return ClassifierSignal(EmergencyType.UNKNOWN, best.second, emptyList(), false)
        }

        val signals = when (best.first) {
            EmergencyType.CPR -> buildCPRSignals(latest)
            EmergencyType.HEART_ATTACK -> buildHeartSignals(latest)
            EmergencyType.FAST_STROKE -> buildStrokeSignals(latest)
            else -> emptyList()
        }

        return ClassifierSignal(
            emergencyType = best.first,
            confidence = best.second,
            visualSignals = signals,
            suggestedByCamera = true
        )
    }

    // CPR: patient is lying flat (shoulder span horizontal > vertical / 0.7)
    private fun scoreCPR(pose: List<NormalizedLandmark>): Float {
        if (pose.size <= R_HIP) return 0f
        val ls = pose[L_SHOULDER]; val rs = pose[R_SHOULDER]
        val lh = pose[L_HIP]; val rh = pose[R_HIP]
        if (ls.visibility < 0.4f || rs.visibility < 0.4f) return 0f

        val shoulderSpanX = kotlin.math.abs(rs.x - ls.x)
        val shoulderSpanY = kotlin.math.abs(rs.y - ls.y)
        val hipSpanX = kotlin.math.abs(rh.x - lh.x)

        val lyingFlat = shoulderSpanX > 0.12f && shoulderSpanX > shoulderSpanY / 0.7f

        // Also average over buffer: patient consistently horizontal
        val bufferLyingCount = poseBuffer.count { p ->
            if (p.size > R_SHOULDER) {
                val bls = p[L_SHOULDER]; val brs = p[R_SHOULDER]
                val bsx = kotlin.math.abs(brs.x - bls.x)
                val bsy = kotlin.math.abs(brs.y - bls.y)
                bsx > 0.12f && bsx > bsy / 0.7f
            } else false
        }
        val bufferScore = bufferLyingCount.toFloat() / poseBuffer.size

        return if (lyingFlat) (0.5f + bufferScore * 0.5f) else bufferScore * 0.4f
    }

    // Heart attack: person upright but hunched, hand near chest, little movement
    private fun scoreHeartAttack(pose: List<NormalizedLandmark>): Float {
        if (pose.size <= R_WRIST) return 0f
        val ls = pose[L_SHOULDER]; val rs = pose[R_SHOULDER]
        val lw = pose[L_WRIST]; val rw = pose[R_WRIST]
        if (ls.visibility < 0.4f || rs.visibility < 0.4f) return 0f

        // Upright: shoulders roughly at similar Y
        val shoulderLevelDiff = kotlin.math.abs(ls.y - rs.y)
        val isUpright = shoulderLevelDiff < 0.15f

        // Hand near chest: wrist Y close to shoulder Y
        val chestY = (ls.y + rs.y) / 2f
        val lWristNearChest = lw.visibility > 0.4f && kotlin.math.abs(lw.y - chestY) < 0.2f
        val rWristNearChest = rw.visibility > 0.4f && kotlin.math.abs(rw.y - chestY) < 0.2f

        if (!isUpright) return 0f

        // Low movement in buffer: person mostly still
        val movementScore = computeMovementStillness()

        var score = movementScore * 0.4f
        if (lWristNearChest || rWristNearChest) score += 0.35f
        if (lWristNearChest && rWristNearChest) score += 0.15f
        return score.coerceIn(0f, 1f)
    }

    // Stroke: asymmetric shoulder/arm posture; one arm drooping
    private fun scoreStroke(pose: List<NormalizedLandmark>): Float {
        if (pose.size <= R_ELBOW) return 0f
        val ls = pose[L_SHOULDER]; val rs = pose[R_SHOULDER]
        val le = pose[L_ELBOW]; val re = pose[R_ELBOW]
        val lw = pose[L_WRIST]; val rw = pose[R_WRIST]
        if (ls.visibility < 0.4f || rs.visibility < 0.4f) return 0f

        // Shoulder height asymmetry
        val shoulderAsymmetry = kotlin.math.abs(ls.y - rs.y)

        // Arm droop: one wrist significantly lower than elbow on same side
        val lArmDroop = if (le.visibility > 0.4f && lw.visibility > 0.4f)
            (lw.y - le.y) else 0f
        val rArmDroop = if (re.visibility > 0.4f && rw.visibility > 0.4f)
            (rw.y - re.y) else 0f
        val armAsymmetry = kotlin.math.abs(lArmDroop - rArmDroop)

        var score = 0f
        if (shoulderAsymmetry > 0.08f) score += 0.3f
        if (armAsymmetry > 0.1f) score += 0.4f
        // Buffer: persistent asymmetry
        val persistentAsymmetry = poseBuffer.count { p ->
            if (p.size > R_SHOULDER) {
                kotlin.math.abs(p[L_SHOULDER].y - p[R_SHOULDER].y) > 0.08f
            } else false
        }.toFloat() / poseBuffer.size
        score += persistentAsymmetry * 0.3f
        return score.coerceIn(0f, 1f)
    }

    private fun computeMovementStillness(): Float {
        if (poseBuffer.size < 10) return 0.5f
        val recent = poseBuffer.takeLast(10)
        var totalDelta = 0f
        for (i in 1 until recent.size) {
            val prev = recent[i - 1]; val curr = recent[i]
            if (curr.size > R_SHOULDER && prev.size > R_SHOULDER) {
                totalDelta += kotlin.math.abs(curr[L_SHOULDER].x - prev[L_SHOULDER].x)
                totalDelta += kotlin.math.abs(curr[L_SHOULDER].y - prev[L_SHOULDER].y)
            }
        }
        val avgDelta = totalDelta / (recent.size - 1)
        // Low movement → high stillness score
        return (1f - (avgDelta * 20f)).coerceIn(0f, 1f)
    }

    private fun buildCPRSignals(pose: List<NormalizedLandmark>): List<String> {
        val signals = mutableListOf<String>()
        if (pose.size > R_SHOULDER) {
            val ls = pose[L_SHOULDER]; val rs = pose[R_SHOULDER]
            if (kotlin.math.abs(rs.x - ls.x) > 0.12f) signals.add("patient lying flat")
        }
        return signals
    }

    private fun buildHeartSignals(pose: List<NormalizedLandmark>): List<String> {
        val signals = mutableListOf<String>()
        signals.add("person upright and still")
        if (pose.size > R_WRIST) {
            val ls = pose[L_SHOULDER]; val rs = pose[R_SHOULDER]
            val lw = pose[L_WRIST]; val rw = pose[R_WRIST]
            val chestY = (ls.y + rs.y) / 2f
            if (lw.visibility > 0.4f && kotlin.math.abs(lw.y - chestY) < 0.2f) signals.add("hand near chest")
            if (rw.visibility > 0.4f && kotlin.math.abs(rw.y - chestY) < 0.2f) signals.add("hand near chest")
        }
        return signals.distinct()
    }

    private fun buildStrokeSignals(pose: List<NormalizedLandmark>): List<String> {
        val signals = mutableListOf<String>()
        if (pose.size > R_SHOULDER) {
            val asymmetry = kotlin.math.abs(pose[L_SHOULDER].y - pose[R_SHOULDER].y)
            if (asymmetry > 0.08f) signals.add("shoulder asymmetry detected")
        }
        if (pose.size > R_ELBOW) {
            val le = pose[L_ELBOW]; val re = pose[R_ELBOW]
            val lw = pose[L_WRIST]; val rw = pose[R_WRIST]
            val lDroop = if (le.visibility > 0.4f && lw.visibility > 0.4f) lw.y - le.y else 0f
            val rDroop = if (re.visibility > 0.4f && rw.visibility > 0.4f) rw.y - re.y else 0f
            if (kotlin.math.abs(lDroop - rDroop) > 0.1f) signals.add("arm drift detected")
        }
        return signals
    }

    fun reset() = poseBuffer.clear()
}
