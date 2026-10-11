package com.sanjeevani.engine

import com.sanjeevani.model.RateStatus
import com.sanjeevani.model.TemporalState

/** Counts observed hand-motion cycles, not validated chest compressions or depth.
 * Caller supplies torso-relative displacement only with visible, consistently tracked hands.
 */
class CompressionDetector(
    private val minimumExcursion: Float = .035f,
    private val minBpm: Float = 100f,
    private val maxBpm: Float = 120f
) {
    private var lastTs = -1L
    private var filtered: Float? = null
    private var trough = 0f
    private var peak = 0f
    private var rising = false
    private var lastCycle = -1L
    private var cycleStart = -1L
    private val events = ArrayDeque<Long>()
    private var count = 0
    private var rate = RateStatus.INSUFFICIENT_DATA

    fun addSample(wristY: Float, timestampMs: Long) {
        if (!wristY.isFinite() || timestampMs <= lastTs) return
        if (lastTs >= 0 && timestampMs - lastTs > 250) trackingLost()
        lastTs = timestampMs
        val previous = filtered
        val y = previous?.let { .4f*wristY + .6f*it } ?: wristY
        filtered = y
        if (previous == null) { trough = y; peak = y; cycleStart = timestampMs; return }
        if (!rising) {
            if (y < trough) { trough = y; cycleStart = timestampMs }
            if (y - trough >= minimumExcursion) { rising = true; peak = y }
        } else {
            peak = maxOf(peak, y)
            if (peak - y >= minimumExcursion) {
                val duration = timestampMs - cycleStart
                if (duration in 250..1500 && (lastCycle < 0 || timestampMs - lastCycle >= 300)) {
                    events.addLast(timestampMs); count++; lastCycle = timestampMs
                    while (events.size > 12) events.removeFirst()
                }
                rising = false; trough = y; cycleStart = timestampMs
            }
        }
    }
    fun trackingLost() {
        filtered = null; rising = false; events.clear(); lastCycle = -1; rate = RateStatus.INSUFFICIENT_DATA
    }
    fun getTemporalState(nowMs: Long): TemporalState {
        while (events.isNotEmpty() && nowMs - events.first() > 6000) events.removeFirst()
        val intervals = events.zipWithNext { a, b -> b-a }
        val mean = intervals.average()
        val consistent = intervals.size >= 3 && mean > 0 &&
            intervals.all { kotlin.math.abs(it-mean) / mean <= .25 }
        val reliable = lastTs >= 0 && nowMs-lastTs in 0..250 && consistent &&
            nowMs - events.last() <= 1500
        val bpm = if (reliable) (events.size-1)*60000f/(events.last()-events.first()) else 0f
        rate = when {
            !reliable -> RateStatus.INSUFFICIENT_DATA
            rate == RateStatus.TOO_SLOW && bpm < minBpm+2 -> RateStatus.TOO_SLOW
            rate == RateStatus.TOO_FAST && bpm > maxBpm-2 -> RateStatus.TOO_FAST
            bpm < minBpm -> RateStatus.TOO_SLOW
            bpm > maxBpm -> RateStatus.TOO_FAST
            else -> RateStatus.GOOD
        }
        return TemporalState(compressionRateBPM = bpm, rateStatus = rate,
            compressionCount = count, trackingReliable = reliable)
    }
    fun reset() { trackingLost(); lastTs = -1; count = 0 }
}
