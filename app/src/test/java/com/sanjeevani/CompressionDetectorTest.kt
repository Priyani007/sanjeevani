package com.sanjeevani

import com.sanjeevani.engine.CompressionDetector
import com.sanjeevani.model.RateStatus
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/** Synthetic motion only, not evidence of clinical accuracy. */
class CompressionDetectorTest {
    private fun feed(d: CompressionDetector, bpm: Int, duration: Long = 10000, duplicates: Boolean = false) {
        for (t in 1000L..1000L+duration step 20) {
            val y = .09f*sin(2*PI*bpm*(t-1000)/60000).toFloat()
            d.addSample(y, t)
            if (duplicates) d.addSample(y, t)
        }
    }
    @Test fun stationaryAndSmallNoiseDoNotCount() {
        val d = CompressionDetector()
        for(t in 1000L..11000L step 20) d.addSample(.5f+.002f*sin(t.toFloat()), t)
        assertEquals(0, d.getTemporalState(11000).compressionCount)
    }
    @Test fun controlledCadencesAreMeasured() {
        for (bpm in listOf(80,110,140)) {
            val d = CompressionDetector(); feed(d,bpm)
            val s = d.getTemporalState(11000)
            assertTrue("count $bpm: ${s.compressionCount}", abs(s.compressionCount-bpm/6f) <= 2)
            assertEquals(bpm.toFloat(),s.compressionRateBPM,3f)
            assertTrue(s.trackingReliable)
            assertEquals(when(bpm) { 80 -> RateStatus.TOO_SLOW; 140 -> RateStatus.TOO_FAST; else -> RateStatus.GOOD }, s.rateStatus)
        }
    }
    @Test fun duplicatesAndOutOfOrderSamplesDoNotCountAgain() {
        val a=CompressionDetector(); val b=CompressionDetector()
        feed(a,110); feed(b,110,duplicates=true)
        b.addSample(10f,500)
        assertEquals(a.getTemporalState(11000).compressionCount,b.getTemporalState(11000).compressionCount)
    }
    @Test fun trackingLossPreservesCountButInvalidatesRateAndCycle() {
        val d=CompressionDetector(); feed(d,110)
        val count=d.getTemporalState(11000).compressionCount
        d.trackingLost(); d.addSample(.8f,12000)
        val s=d.getTemporalState(12000)
        assertEquals(count,s.compressionCount); assertFalse(s.trackingReliable)
        assertEquals(RateStatus.INSUFFICIENT_DATA,s.rateStatus)
    }
    @Test fun elapsedTimeAloneNeverCountsAndStaleRateExpires() {
        val d=CompressionDetector(); feed(d,110)
        val count=d.getTemporalState(11000).compressionCount
        assertEquals(count,d.getTemporalState(30000).compressionCount)
        assertFalse(d.getTemporalState(30000).trackingReliable)
    }
    @Test fun resetClearsSessionMetrics() {
        val d=CompressionDetector(); feed(d,110); d.reset()
        assertEquals(0,d.getTemporalState(12000).compressionCount)
    }
}
