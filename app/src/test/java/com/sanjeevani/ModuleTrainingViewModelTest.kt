package com.sanjeevani

import com.sanjeevani.training.*
import com.sanjeevani.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ModuleTrainingViewModelTest {
    @Test
    fun `incorrect answer cannot advance and correction preserves both attempts`() {
        val app = RuntimeEnvironment.getApplication()
        val vm = ModuleTrainingViewModel(app)
        vm.clearStrokeSessions()
        vm.startStrokeTraining()
        val initial = vm.strokeSession.value!!
        val wrong = initial.stepState!!.step.options.first { !it.isCorrect }
        vm.selectOption("STROKE_FAST", wrong.id)
        assertTrue(vm.submitAnswer("STROKE_FAST"))
        vm.nextStep("STROKE_FAST")
        assertEquals(0, vm.strokeSession.value!!.stepIndex)
        assertFalse(vm.strokeSession.value!!.stepState!!.canAdvance)

        vm.retryStep("STROKE_FAST")
        val correct = vm.strokeSession.value!!.stepState!!.step.correctOption.id
        vm.selectOption("STROKE_FAST", correct)
        vm.submitAnswer("STROKE_FAST")
        assertTrue(vm.strokeSession.value!!.stepState!!.canAdvance)
        assertEquals(2, vm.strokeSession.value!!.completedAttempts.size)
        assertEquals(CorrectionStatus.CORRECTED, vm.strokeSession.value!!.completedAttempts.last().correctionStatus)
        vm.nextStep("STROKE_FAST")
        assertEquals(1, vm.strokeSession.value!!.stepIndex)
        vm.clearStrokeSessions()
    }

    @Test
    fun `unfinished assessment restores after view model recreation`() {
        val app = RuntimeEnvironment.getApplication()
        val vm = ModuleTrainingViewModel(app)
        vm.clearAllergySessions()
        vm.startAllergyTraining()
        val wrong = vm.allergySession.value!!.stepState!!.step.options.first { !it.isCorrect }
        vm.selectOption("ALLERGIC_REACTION", wrong.id)
        vm.submitAnswer("ALLERGIC_REACTION")

        val recreated = ModuleTrainingViewModel(app)
        val restored = recreated.allergySession.value!!
        assertEquals(vm.allergySession.value!!.sessionId, restored.sessionId)
        assertEquals(0, restored.stepIndex)
        assertEquals(1, restored.completedAttempts.size)
        assertFalse(restored.stepState!!.locked)
        recreated.clearAllergySessions()
    }

    @Test
    fun `focused practice opens the recommended skill directly`() {
        val app = RuntimeEnvironment.getApplication()
        val vm = ModuleTrainingViewModel(app)
        vm.clearStrokeSessions()
        vm.startFocusedTraining("STROKE_FAST", StrokeSkillTag.SPEECH_DIFFICULTY)
        val state = vm.strokeSession.value!!
        assertEquals(StrokeSkillTag.SPEECH_DIFFICULTY, state.stepState!!.step.skillTag)
        assertEquals(1, state.totalSteps)
        assertEquals(StrokeSkillTag.SPEECH_DIFFICULTY, state.focusedSkillTag)
        vm.clearStrokeSessions()
    }

    @Test
    fun `placement simulation blocks progress until accepted zone`() {
        val app = RuntimeEnvironment.getApplication()
        val vm = ModuleTrainingViewModel(app)
        vm.clearAllergySessions()
        vm.startFocusedTraining("ALLERGIC_REACTION", AllergySkillTag.EPIPEN_LOCATION)
        val correct = vm.allergySession.value!!.stepState!!.step.correctOption.id
        vm.selectOption("ALLERGIC_REACTION", correct)
        vm.submitAnswer("ALLERGIC_REACTION")
        assertEquals(ModuleTrainingPhase.PLACEMENT, vm.allergySession.value!!.phase)
        assertFalse(vm.allergySession.value!!.stepState!!.canAdvance)
        vm.assessPlacement("ALLERGIC_REACTION", .5f, .5f)
        assertEquals(PlacementResult.TOO_MEDIAL, vm.allergySession.value!!.stepState!!.attempt!!.placementResult)
        assertFalse(vm.allergySession.value!!.stepState!!.canAdvance)
        vm.assessPlacement("ALLERGIC_REACTION", .75f, .5f)
        assertTrue(vm.allergySession.value!!.stepState!!.canAdvance)
        assertEquals(CorrectionStatus.CORRECTED, vm.allergySession.value!!.stepState!!.attempt!!.correctionStatus)
        vm.clearAllergySessions()
    }

    @Test
    fun `manual progression records unconfirmed and never awards success`() {
        val app = RuntimeEnvironment.getApplication()
        val vm = ModuleTrainingViewModel(app)
        vm.clearStrokeSessions()
        vm.startFocusedTraining("STROKE_FAST", StrokeSkillTag.TIME_TO_ACT)
        vm.continueManually("STROKE_FAST")
        val state = vm.strokeSession.value!!
        assertTrue(state.isFinished)
        val event = state.completedAttempts.single()
        assertTrue(event.manuallyAdvanced)
        assertEquals(AssessmentMethod.MANUAL, event.assessmentMethod)
        assertEquals(SkillAttemptOutcome.INCONCLUSIVE, event.outcome)
        assertNotEquals(CorrectionStatus.CORRECTED, event.correctionStatus)
        vm.clearStrokeSessions()
    }

    @Test
    fun `supported camera observation is deduplicated and knowledge remains authoritative`() {
        val app = RuntimeEnvironment.getApplication()
        val vm = ModuleTrainingViewModel(app)
        vm.clearStrokeSessions()
        vm.startFocusedTraining("STROKE_FAST", StrokeSkillTag.FACE_DROOPING)
        val snapshot = TrainingSessionSnapshot(
            completedFSMStates = emptySet(), compressionCount = 0,
            rateStatus = RateStatus.INSUFFICIENT_DATA,
            strokeSignals = StrokeSignals(.2f, false, false, 0, faceObservationFrames = 6),
            strokeIsComplete = false, allergicPhaseOrdinal = 0,
            emergencyType = EmergencyType.FAST_STROKE
        )
        repeat(5) { vm.recordSupportedCameraObservation("STROKE_FAST", snapshot, .9f, false) }
        assertEquals(1, vm.strokeSession.value!!.completedAttempts.size)
        assertEquals(AssessmentMethod.CAMERA_MEDIAPIPE, vm.strokeSession.value!!.completedAttempts.single().assessmentMethod)
        assertEquals(SkillAttemptOutcome.INCONCLUSIVE, vm.strokeSession.value!!.completedAttempts.single().outcome)
        val correct = vm.strokeSession.value!!.stepState!!.step.correctOption.id
        vm.selectOption("STROKE_FAST", correct)
        vm.submitAnswer("STROKE_FAST")
        assertEquals(SkillAttemptOutcome.CORRECT, vm.strokeSession.value!!.completedAttempts.last().outcome)
        vm.clearStrokeSessions()
    }
}
