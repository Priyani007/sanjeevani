package com.sanjeevani

import com.sanjeevani.engine.Answer
import com.sanjeevani.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Android callback/state tests without native models, microphone, or real speaker playback. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceLifecycleTest {
    private fun set(vm: SanjeevaniViewModel, name: String, value: Any?) {
        SanjeevaniViewModel::class.java.getDeclaredField(name).apply { isAccessible=true }.set(vm,value)
    }
    private fun done(vm: SanjeevaniViewModel, id: String, success: Boolean) {
        SanjeevaniViewModel::class.java.getDeclaredMethod("finishSpeech",String::class.java,Boolean::class.javaPrimitiveType)
            .apply { isAccessible=true }.invoke(vm,id,success)
    }
    @Test fun listeningContinuationWaitsForMatchingSuccessfulSpeechCompletion() {
        val vm=SanjeevaniViewModel()
        var next=0
        set(vm,"utteranceId","current")
        set(vm,"completion",{ next++ })
        assertEquals(0,next)
        done(vm,"old",true); assertEquals(0,next)
        done(vm,"current",true); assertEquals(1,next)
        done(vm,"current",true); assertEquals(1,next)
    }
    @Test fun speechFailureDoesNotPretendToListen() {
        val vm=SanjeevaniViewModel()
        var next=0
        set(vm,"utteranceId","current"); set(vm,"completion",{ next++ })
        done(vm,"current",false)
        assertEquals(0,next)
        assertFalse(vm.isListening.value)
        assertTrue(vm.voiceStatus.value.contains("unavailable"))
    }
    @Test fun pauseInvalidatesPendingAudioWithoutLosingWorkflow() {
        val vm=SanjeevaniViewModel()
        vm.onEmergencySelected(EmergencyType.HEART_ATTACK)
        var next=0
        set(vm,"utteranceId","pending"); set(vm,"completion",{ next++ })
        vm.suspendSession(); done(vm,"pending",true)
        assertEquals(0,next)
        assertEquals(FSMState.RESPONSIVENESS_CHECK,vm.guidanceState.value.fsmState)
    }
    @Test fun tapFallbackUpdatesImmediatelyWithoutCameraAndRejectsStaleDoubleTap() {
        val vm=SanjeevaniViewModel()
        vm.onEmergencySelected(EmergencyType.HEART_ATTACK)
        val version=vm.guidanceState.value.stateVersion
        vm.answer(Answer.NO,version)
        assertEquals(FSMState.BREATHING_ASSESSMENT,vm.guidanceState.value.fsmState)
        vm.answer(Answer.NO,version)
        assertEquals(FSMState.BREATHING_ASSESSMENT,vm.guidanceState.value.fsmState)
        vm.answer(Answer.YES)
        assertEquals(FSMState.HEART_ATTACK_CONSCIOUS,vm.guidanceState.value.fsmState)
        assertFalse(vm.isListening.value)
    }
}
