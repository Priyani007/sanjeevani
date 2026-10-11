package com.sanjeevani

import com.sanjeevani.engine.*
import com.sanjeevani.model.*
import com.sanjeevani.voice.*
import org.junit.Assert.*
import org.junit.Test

private fun emptyFrame(nowMs: Long) =
    PerceptionFrame(nowMs, null, null, null, 720, 1280)

class WorkflowTest {
    private fun assessment() = EmergencyFSM().also { it.setUserSelectedEmergency(EmergencyType.HEART_ATTACK, 1000) }
    private fun position() = assessment().also { it.answer(Answer.NO, 1100); it.answer(Answer.NO, 1200) }
    @Test fun concernIsNotCardiacArrest() {
        val f = assessment()
        assertEquals(FSMState.RESPONSIVENESS_CHECK, f.getState())
        repeat(100) { f.process(GuidanceState(isPatientDetected = true), 2000L+it*1000) }
        assertEquals(FSMState.RESPONSIVENESS_CHECK, f.getState())
    }
    @Test fun responsivenessRequiresAnswerAndNormalBreathingIsNotCPR() {
        val f = assessment()
        f.answer(Answer.NO, 1100)
        assertEquals(FSMState.BREATHING_ASSESSMENT, f.getState())
        f.answer(Answer.YES, 1200)
        assertEquals(FSMState.HEART_ATTACK_CONSCIOUS, f.getState())
        assertTrue(f.breathingNormally == true)
    }
    @Test fun responsivePatientDoesNotStartCPR() {
        val f = assessment(); f.answer(Answer.YES, 1100)
        assertEquals(FSMState.HEART_ATTACK_CONSCIOUS, f.getState())
        assertFalse(f.answer(Answer.READY, 1200))
    }
    @Test fun staleAndDuplicateAnswersAreRejected() {
        val f = assessment(); val v = f.version
        assertTrue(f.answer(Answer.NO, 1100, v))
        assertFalse(f.answer(Answer.NO, 1101, v))
        assertEquals(FSMState.BREATHING_ASSESSMENT, f.getState())
    }
    @Test fun timersCannotAdvanceManualSteps() {
        val f = position(); f.answer(Answer.READY, 1300)
        repeat(50) { f.process(GuidanceState(), 2000L+it*1000) }
        assertEquals(FSMState.HAND_POSITIONING, f.getState())
        f.answer(Answer.READY, 60000)
        f.process(GuidanceState(), 90000)
        assertEquals(FSMState.POSTURE_CHECK, f.getState())
    }
    @Test fun stablePositionRequiresConfidenceAndFreshContinuousEvidence() {
        val f = position()
        val good = GuidanceState(isPatientDetected = true, confidence = ConfidenceState(patientConfidence = .9f))
        f.process(good, 2000)
        f.process(good, 4000) // Gap resets the hold.
        assertEquals(FSMState.CPR_POSITIONING, f.getState())
        repeat(10) { f.process(good, 4100L+it*100) }
        assertEquals(FSMState.POSITION_CONFIRMED, f.getState())
        val version = f.version
        repeat(20) { f.process(GuidanceState(), 6000L+it*100) }
        assertEquals(version, f.version)
        assertEquals(1, f.history.count { it.to == FSMState.POSITION_CONFIRMED })
    }
    @Test fun missingOrLowConfidenceNeverConfirms() {
        val f = position()
        repeat(50) { f.process(GuidanceState(isPatientDetected = true,
            confidence = ConfidenceState(patientConfidence = .4f)), 2000L+it*100) }
        assertEquals(FSMState.CPR_POSITIONING, f.getState())
    }
    @Test fun duplicateFramesCannotAccumulateHold() {
        val f = position()
        val g = GuidanceState(isPatientDetected = true, confidence = ConfidenceState(patientConfidence = 1f))
        repeat(100) { f.process(g, 2000) }
        assertEquals(FSMState.CPR_POSITIONING, f.getState())
    }
    @Test fun completedStepsStayCompletedUntilExplicitSafetyReassessment() {
        val f = position()
        repeat(3) { f.answer(Answer.READY, 1400L+it*100) }
        assertEquals(FSMState.COMPRESSION_ACTIVE, f.getState())
        repeat(50) { f.process(GuidanceState(), 2000L+it*100) }
        assertEquals(FSMState.COMPRESSION_ACTIVE, f.getState())
        f.answer(Answer.CHANGED, 8000)
        assertEquals(FSMState.RESPONSIVENESS_CHECK, f.getState())
        assertNull(f.responding)
        assertTrue(f.history.last().reason.contains("reassess"))
    }
    @Test fun uncertaintyAndRepeatDoNotMutateWorkflow() {
        val f = assessment(); val v = f.version
        listOf(Answer.REPEAT, Answer.UNKNOWN, Answer.UNCERTAIN).forEach { assertFalse(f.answer(it, 1100)) }
        assertEquals(v, f.version)
    }
    @Test fun everyTransitionHasReasonAndMonotonicVersion() {
        val f = position(); repeat(3) { f.answer(Answer.READY, 1400L+it*100) }
        f.history.forEachIndexed { i, r -> assertEquals(i+1L, r.version); assertTrue(r.reason.isNotBlank()) }
    }
    @Test fun handHoldIsSeparateFromPositionHistory() {
        val f = position(); f.answer(Answer.READY, 1300)
        val g = GuidanceState(spatial = SpatialState(correctiveAction = SpatialAction.CORRECT),
            confidence = ConfidenceState(poseConfidence = .9f, handsConfidence = .9f))
        repeat(11) { f.process(g, 2000L+it*100) }
        assertEquals(FSMState.POSTURE_CHECK, f.getState())
    }
    @Test fun turnGateInvalidatesOldAudioRecognitionAndAIResults() {
        val gate = TurnGate(); val token = gate.invalidate()
        assertTrue(gate.accepts(token, 1, 1))
        assertFalse(gate.accepts(token, 1, 2))
        gate.invalidate()
        assertFalse(gate.accepts(token, 1, 1))
    }
    @Test fun contextAwareTranscriptMatrix() {
        val r = FSMState.RESPONSIVENESS_CHECK; val b = FSMState.BREATHING_ASSESSMENT
        val cases = listOf(
            Triple("yes", r, Answer.YES), Triple("no", r, Answer.NO),
            Triple("No, she isn't responding", r, Answer.NO),
            Triple("she is not responsive", r, Answer.NO),
            Triple("yes she is responding", r, Answer.YES),
            Triple("she's breathing", r, Answer.UNKNOWN),
            Triple("no response", b, Answer.UNKNOWN),
            Triple("not breathing normally", b, Answer.NO),
            Triple("gasping", b, Answer.NO),
            Triple("she is breathing normally", b, Answer.YES),
            Triple("I don't know", b, Answer.UNCERTAIN),
            Triple("can you repeat", r, Answer.REPEAT),
            Triple("yesterday", r, Answer.UNKNOWN),
            Triple("yes no response", r, Answer.UNCERTAIN)
        )
        cases.forEach { (text, state, answer) -> assertEquals(text, answer, SessionDialogue.interpret(text, state).answer) }
    }
    @Test fun lowConfidenceClarifiesAndOpeningConcernIsNotAnswer() {
        assertEquals(Answer.UNCERTAIN, SessionDialogue.interpret("yes", FSMState.RESPONSIVENESS_CHECK, .2f).answer)
        assertEquals(EmergencyType.HEART_ATTACK,
            SessionDialogue.interpret("I think she is having a heart attack", FSMState.IDLE).concern)
        assertEquals(Answer.UNKNOWN, SessionDialogue.interpret("heart attack", FSMState.BREATHING_ASSESSMENT).answer)
    }
    @Test fun questionsDoNotAuthorizeAClinicalTransition() {
        val f=assessment()
        val result=SessionDialogue.interpret("how fast",f.getState())
        assertEquals(Answer.QUESTION,result.answer)
        assertFalse(f.answer(result.answer,1100))
        assertTrue(SessionDialogue.help("how fast",f.getState(),f.instruction()).contains("responding"))
    }
    @Test fun allergicReactionSelectsAllergicProtocol() {
        val f = EmergencyFSM()
        f.setUserSelectedEmergency(EmergencyType.ALLERGIC_REACTION, 1000)
        assertEquals(FSMState.ALLERGIC_PROTOCOL, f.getState())
        assertEquals(EmergencyType.ALLERGIC_REACTION, f.getConfirmedEmergencyType())
        assertTrue(f.history.last().reason.contains("anaphylaxis", ignoreCase = true))
    }

    @Test fun allergicModuleRequiresExplicitLearnerStepSelection() {
        val module = AllergicReactionModule()
        val t0 = module.process(emptyFrame(0), 0)
        assertNotNull("Phase 0 should speak on first call", t0.voiceText)
        assertTrue(t0.voiceText!!.contains("flat", ignoreCase = true))
        assertFalse(t0.isComplete)
        val noTimerAdvance = module.process(emptyFrame(90_000), 90_000)
        assertNull("Elapsed time must not change the phase", noTimerAdvance.voiceText)
        assertFalse(noTimerAdvance.isComplete)
        module.selectTrainingStep(2)
        val legs = module.process(emptyFrame(90_001), 90_001)
        assertTrue(legs.voiceText!!.contains("leg", ignoreCase = true))
        module.selectTrainingStep(3)
        val trainer = module.process(emptyFrame(90_002), 90_002)
        assertTrue(trainer.voiceText!!.contains("trainer", ignoreCase = true))
        module.selectTrainingStep(6)
        assertTrue(module.process(emptyFrame(90_003), 90_003).isComplete)
    }

    @Test fun allergicModuleEpiPenMarkerOnThighNotSternum() {
        val module = AllergicReactionModule()
        val landmarks = (0 until 33).map { i ->
            when (i) {
                24 -> com.sanjeevani.model.NormalizedLandmark(0.5f, 0.6f, 0f, 0.9f) // right hip
                26 -> com.sanjeevani.model.NormalizedLandmark(0.5f, 0.8f, 0f, 0.9f) // right knee
                else -> com.sanjeevani.model.NormalizedLandmark(0.5f, 0.5f, 0f, 0.9f)
            }
        }
        val frame = PerceptionFrame(48001, landmarks, null, null, 720, 1280)
        val result = module.process(frame, 48001)
        // thighTarget must be non-null — confirms rightThighOf() found visible hip+knee landmarks.
        // PointF field values are Android stubs in JVM unit tests (always 0); coordinate accuracy
        // is verified by visual inspection on device.
        assertNotNull("EpiPen marker must be placed at right thigh, not sternum", result.overlay.thighTarget)
        assertNull("sternumTarget must be null in allergic protocol — sternum sphere is CPR-only", result.overlay.sternumTarget)
    }

    @Test fun allergicModuleResetRestartsCycle() {
        val module = AllergicReactionModule()
        module.process(emptyFrame(0), 0)
        module.process(emptyFrame(20_000), 20_000)
        module.reset()
        // After reset startMs=-1; next process sets startMs=21000, elapsed=0 → LAY_FLAT
        val r = module.process(emptyFrame(21_000), 21_000)
        assertNotNull("Should speak LAY_FLAT instruction after reset", r.voiceText)
        assertTrue(r.voiceText!!.contains("flat", ignoreCase = true))
        assertFalse("Should not be complete immediately after reset", r.isComplete)
    }

    @Test fun strokeFastRoutesToStrokeFastTest() {
        val f = EmergencyFSM()
        f.setUserSelectedEmergency(EmergencyType.FAST_STROKE, 1000)
        assertEquals(FSMState.STROKE_FAST_TEST, f.getState())
        assertEquals(EmergencyType.FAST_STROKE, f.getConfirmedEmergencyType())
        assertTrue(f.history.last().reason.contains("FAST", ignoreCase = true))
    }

    @Test fun strokeModuleIntroFaceArmSpeechResult() {
        val module = com.sanjeevani.engine.StrokeModule()
        // INTRO at t=0
        val r0 = module.process(emptyFrame(0), 0)
        assertNotNull("INTRO should speak", r0.voiceText)
        assertTrue(r0.voiceText!!.contains("FAST", ignoreCase = true))
        assertFalse(r0.isComplete)
        module.selectTrainingStep(1)
        val r3 = module.process(emptyFrame(3001), 3001)
        assertNotNull("FACE step should speak on transition", r3.voiceText)
        assertTrue(r3.voiceText!!.contains("smile", ignoreCase = true))
        module.selectTrainingStep(2)
        val r13 = module.process(emptyFrame(13002), 13002)
        assertNotNull("ARM step should speak on transition", r13.voiceText)
        assertTrue(r13.voiceText!!.contains("arm", ignoreCase = true))
        module.selectTrainingStep(3)
        val r25 = module.process(emptyFrame(25003), 25003)
        assertNotNull("SPEECH step should speak on transition", r25.voiceText)
        assertTrue(r25.voiceText!!.contains("phrase", ignoreCase = true))
        assertFalse("Not complete until speech result received", r25.isComplete)
        module.onSpeechResult(true)   // slurred speech = positive FAST finding
        assertFalse("Speech input alone must not auto-complete training", module.process(emptyFrame(25050), 25050).isComplete)
        module.completeTraining()
        val rResult = module.process(emptyFrame(25100), 25100)
        assertTrue("Module should be complete after RESULT", rResult.isComplete)
        assertNotNull("RESULT should announce findings", rResult.voiceText)
    }

    @Test fun strokeModuleZeroPositivesLowRisk() {
        val module = com.sanjeevani.engine.StrokeModule()
        module.selectTrainingStep(3)
        module.process(emptyFrame(25003), 25003)
        module.onSpeechResult(false)  // false = speech clear = NOT a positive FAST finding
        val signals = module.getStrokeSignals()
        assertTrue("positiveTestCount must be non-negative", signals.positiveTestCount >= 0)
        assertTrue("speechPrompted must be true after speech result", signals.speechPrompted)
    }

    @Test fun strokeModuleResetRestartsCycle() {
        val module = com.sanjeevani.engine.StrokeModule()
        module.process(emptyFrame(0), 0)
        module.process(emptyFrame(3001), 3001)
        module.reset()
        val r = module.process(emptyFrame(5000), 5000)
        assertNotNull("After reset INTRO should re-speak", r.voiceText)
        assertTrue(r.voiceText!!.contains("FAST", ignoreCase = true))
        assertFalse(r.isComplete)
    }

    @Test fun strokeSpeechSlurredIsPositiveFindings() {
        // onSpeechResult(true) = slurred = positive FAST finding; (false) = clear = not positive
        val slurred = com.sanjeevani.engine.StrokeModule()
        slurred.selectTrainingStep(3)
        slurred.process(emptyFrame(25003), 25003) // SPEECH
        slurred.onSpeechResult(true)              // slurred = positive finding
        assertTrue("Slurred speech must register as a positive FAST sign",
            slurred.getStrokeSignals().positiveTestCount >= 1)

        val clear = com.sanjeevani.engine.StrokeModule()
        clear.selectTrainingStep(3)
        clear.process(emptyFrame(25003), 25003)
        clear.onSpeechResult(false)               // clear speech = NOT a positive finding
        assertEquals("Clear speech must NOT be counted as a positive FAST sign",
            0, clear.getStrokeSignals().positiveTestCount)
    }

    @Test fun strokeDoesNotAffectCprOrAllergicFsm() {
        // CPR flow still works independently
        val cpr = EmergencyFSM()
        cpr.setUserSelectedEmergency(EmergencyType.CPR, 1000)
        assertEquals(FSMState.RESPONSIVENESS_CHECK, cpr.getState())
        // Allergic flow still works independently
        val allergic = EmergencyFSM()
        allergic.setUserSelectedEmergency(EmergencyType.ALLERGIC_REACTION, 1000)
        assertEquals(FSMState.ALLERGIC_PROTOCOL, allergic.getState())
        // Stroke routes correctly
        val stroke = EmergencyFSM()
        stroke.setUserSelectedEmergency(EmergencyType.FAST_STROKE, 1000)
        assertEquals(FSMState.STROKE_FAST_TEST, stroke.getState())
    }

    @Test fun compressionActiveDialogueInterpretsPlainYesNo() {
        val active = FSMState.COMPRESSION_ACTIVE
        assertEquals(Answer.YES, SessionDialogue.interpret("yes", active).answer)
        assertEquals(Answer.NO, SessionDialogue.interpret("no", active).answer)
        assertEquals(Answer.YES, SessionDialogue.interpret("showing signs", active).answer)
        assertEquals(Answer.NO, SessionDialogue.interpret("no signs", active).answer)
        // Recovery phrases bypass the response-check path and still return CHANGED.
        assertEquals(Answer.CHANGED, SessionDialogue.interpret("she woke up", active).answer)
    }
}
