package com.sanjeevani

import android.animation.ValueAnimator
import android.os.SystemClock
import com.sanjeevani.engine.*
import com.sanjeevani.model.*
import com.sanjeevani.ui.*
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VisionIntegrationTest {
    private fun pose(): List<NormalizedLandmark> {
        val p = MutableList(33) { NormalizedLandmark(.5f,.5f) }
        p[11]=NormalizedLandmark(.3f,.25f); p[12]=NormalizedLandmark(.7f,.25f)
        p[23]=NormalizedLandmark(.35f,.75f); p[24]=NormalizedLandmark(.65f,.75f)
        p[15]=NormalizedLandmark(.2f,.6f); p[16]=NormalizedLandmark(.8f,.6f)
        return p
    }
    private fun hand(x: Float, y: Float) = List(21) { NormalizedLandmark(x,y) }
    @Test fun screenSpaceDirectionsMatchSignedError() {
        listOf(Triple(.7f,.425f,SpatialAction.MOVE_LEFT), Triple(.3f,.425f,SpatialAction.MOVE_RIGHT),
            Triple(.5f,.7f,SpatialAction.MOVE_UP), Triple(.5f,.2f,SpatialAction.MOVE_DOWN),
            Triple(.5f,.425f,SpatialAction.CORRECT)).forEach { (x,y,expected) ->
            val s=SpatialReasoner().compute(pose(),hand(x,y),null,1000)
            assertEquals(expected,s.correctiveAction)
        }
    }
    @Test fun emptyHandOrMissingPoseCannotReuseLastTarget() {
        val r=SpatialReasoner()
        assertNotNull(r.compute(pose(),hand(.5f,.425f),null,1000).sternumTarget)
        // Brief pose gap (within STERNUM_HOLD_MS): target is held to suppress single-frame jitter.
        assertNotNull(r.compute(null,hand(.5f,.425f),null,1100).sternumTarget)
        // Extended pose loss (beyond STERNUM_HOLD_MS 600ms): target is cleared; no indefinite reuse.
        assertNull(r.compute(null,hand(.5f,.425f),null,1700).sternumTarget)
        assertEquals(SpatialAction.TRACKING_LOST,r.compute(pose(),emptyList(),null,1800).correctiveAction)
    }
    @Test fun patientWristsCannotSubstituteForRescuerHands() {
        assertEquals(SpatialAction.TRACKING_LOST,SpatialReasoner().compute(pose(),null,null,1000).correctiveAction)
    }
    @Test fun lowVisibilityBlocksTarget() {
        val p=pose().toMutableList(); p[23]=p[23].copy(visibility=.2f)
        assertNull(SpatialReasoner().compute(p,hand(.5f,.425f),null,1000).sternumTarget)
    }
    @Test fun movementOutOfTargetClearsCorrectImmediately() {
        val r=SpatialReasoner()
        assertEquals(SpatialAction.CORRECT,r.compute(pose(),hand(.5f,.425f),null,1000).correctiveAction)
        assertNotEquals(SpatialAction.CORRECT,r.compute(pose(),hand(.8f,.425f),null,1010).correctiveAction)
    }
    @Test fun sameSideArmsAndOcclusionRejectPositionEstimate() {
        val p=pose().toMutableList()
        assertTrue(PatientDetector().isPatientLying(p).first)
        p[16]=NormalizedLandmark(.1f,.6f)
        assertFalse(PatientDetector().isPatientLying(p).first)
        p[11]=p[11].copy(visibility=.1f)
        assertFalse(PatientDetector().isPatientLying(p).first)
    }
    @Test fun fittedOverlayHandlesPortraitLandscapeAndMirroredInputCoordinates() {
        assertEquals(50f,OverlayCoordinates.fit(0f,0f,100,200,200,200).first,.001f)
        assertEquals(150f,OverlayCoordinates.fit(1f,1f,100,200,200,200).first,.001f)
        assertEquals(50f,OverlayCoordinates.fit(0f,0f,200,100,200,200).second,.001f)
        // Mirroring changes x before fit; the back-camera path itself is unmirrored.
        val x=OverlayCoordinates.fit(.2f,.4f,100,200,200,200).first
        val mirror=OverlayCoordinates.fit(.8f,.4f,100,200,200,200).first
        assertEquals(200f,x+mirror,.001f)
    }
    @Test fun confirmationAnimationIsEventDrivenAndPacingUsesConfiguredCadence() {
        val v=AROverlayView(RuntimeEnvironment.getApplication())
        val field=AROverlayView::class.java.getDeclaredField("stepCardAnimator").apply { isAccessible=true }
        val accepted=AROverlaySpec(state=FSMState.POSITION_CONFIRMED,confirmationEvent=3,stepCardTitle="1. Position Check")
        v.overlaySpec=accepted
        val animator=field.get(v)
        assertNotNull(animator)
        v.overlaySpec=accepted
        assertSame(animator,field.get(v))
        v.overlaySpec=AROverlaySpec(state=FSMState.COMPRESSION_ACTIVE,pacingBpm=110)
        val sphereField=AROverlayView::class.java.getDeclaredField("spherePulseAnimator").apply { isAccessible=true }
        val sphere=sphereField.get(v) as ValueAnimator
        assertEquals(30000L/110,sphere.duration)
        assertEquals(ValueAnimator.REVERSE,sphere.repeatMode)
        v.overlaySpec=AROverlaySpec()
        assertNull(sphereField.get(v))
    }
    private fun tick(ms: Long=100): Long {
        ShadowSystemClock.advanceBy(Duration.ofMillis(ms))
        return SystemClock.elapsedRealtime()
    }
    @Test fun controlledEndToEndJourneyDoesNotReopenPositionOnTrackingLoss() {
        val e=SanjeevaniEngine()
        e.setUserSelectedEmergency(EmergencyType.HEART_ATTACK)
        assertEquals(FSMState.RESPONSIVENESS_CHECK,e.snapshot().fsmState)
        e.answer(Answer.NO); assertEquals(FSMState.BREATHING_ASSESSMENT,e.snapshot().fsmState)
        e.answer(Answer.NO); assertEquals(FSMState.CPR_POSITIONING,e.snapshot().fsmState)
        repeat(12) {
            val t=tick()
            e.process(PerceptionFrame(t,pose(),null,null,720,1280))
        }
        assertEquals(FSMState.POSITION_CONFIRMED,e.getFSMState())
        e.answer(Answer.READY); e.snapshot()
        repeat(12) {
            val t=tick()
            e.process(PerceptionFrame(t,pose(),hand(.5f,.425f),null,720,1280))
        }
        assertEquals(FSMState.POSTURE_CHECK,e.getFSMState())
        e.answer(Answer.READY)
        val started=e.snapshot()
        assertEquals(FSMState.COMPRESSION_ACTIVE,started.fsmState)
        // No entry speech — count starts after 500 ms delay; first snapshot returns null voice
        assertNull(e.snapshot().voiceText)
        repeat(40) {
            val t=tick()
            val g=e.process(PerceptionFrame(t,null,null,null,720,1280))
            assertEquals(FSMState.COMPRESSION_ACTIVE,g.fsmState)
            assertEquals(0,g.temporal.compressionCount)
            assertFalse(g.temporal.trackingReliable)
            assertFalse(g.voiceText?.contains("on their back")==true)
        }
    }
    @Test fun staleObservationsNeverAcceptPosition() {
        val e=SanjeevaniEngine()
        e.setUserSelectedEmergency(EmergencyType.CPR); e.answer(Answer.NO); e.answer(Answer.NO); e.snapshot()
        val old=tick()
        repeat(30) {
            val t=tick()
            e.process(PerceptionFrame(t,pose(),null,null,720,1280,poseTimestamp=old))
        }
        assertEquals(FSMState.CPR_POSITIONING,e.getFSMState())
    }
    private fun reachCompressionActive(e: SanjeevaniEngine) {
        e.setUserSelectedEmergency(EmergencyType.HEART_ATTACK)
        e.answer(Answer.NO); e.answer(Answer.NO)
        repeat(12) { e.process(PerceptionFrame(tick(), pose(), null, null, 720, 1280)) }
        e.answer(Answer.READY); e.snapshot()
        repeat(12) { e.process(PerceptionFrame(tick(), pose(), hand(.5f, .425f), null, 720, 1280)) }
        e.answer(Answer.READY); e.snapshot()
    }
    @Test fun responseCheckYesAnswerContinuesCompressions() {
        val e = SanjeevaniEngine()
        reachCompressionActive(e)
        assertEquals(FSMState.COMPRESSION_ACTIVE, e.getFSMState())
        // Simulate the ViewModel counting coroutine triggering the consciousness check
        e.markConsciousnessCheckPending()
        assertTrue("Consciousness check should be pending", e.isPendingConsciousnessCheck)
        e.answer(Answer.YES)
        // YES keeps CPR going — encouragement voice is handled by ViewModel counting coroutine
        assertEquals(FSMState.COMPRESSION_ACTIVE, e.getFSMState())
        assertFalse("Consciousness check should be cleared after YES", e.isPendingConsciousnessCheck)
    }
    @Test fun responseCheckNoAnswerContinuesCompressions() {
        val e = SanjeevaniEngine()
        reachCompressionActive(e)
        assertEquals(FSMState.COMPRESSION_ACTIVE, e.getFSMState())
        // Simulate the ViewModel counting coroutine triggering the consciousness check
        e.markConsciousnessCheckPending()
        assertTrue("Consciousness check should be pending", e.isPendingConsciousnessCheck)
        e.answer(Answer.NO)
        // NO keeps CPR going — encouragement voice is handled by ViewModel counting coroutine
        assertEquals(FSMState.COMPRESSION_ACTIVE, e.getFSMState())
        assertFalse("Consciousness check should be cleared after NO", e.isPendingConsciousnessCheck)
    }
}
