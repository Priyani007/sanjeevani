package com.sanjeevani

import com.sanjeevani.model.FSMState
import com.sanjeevani.model.RateStatus
import com.sanjeevani.model.StrokeSignals
import com.sanjeevani.training.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TrainingIntelligenceTest {

    // ─── AssessmentEngine — CPR ───────────────────────────────────────────────

    @Test
    fun cprFullCompletion_allPass() {
        val states = setOf(
            FSMState.RESPONSIVENESS_CHECK, FSMState.CPR_POSITIONING,
            FSMState.HAND_POSITIONING, FSMState.COMPRESSION_ACTIVE
        )
        val criteria = AssessmentEngine.assessCPR(states, 35, RateStatus.GOOD, System.currentTimeMillis())
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.CPR_CALL_112 }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.CPR_COMPRESSIONS_STARTED }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.CPR_COMPRESSION_RATE }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.CPR_THIRTY_CYCLES }?.result)
    }

    @Test
    fun cprNoCompressions_needsPractice() {
        val criteria = AssessmentEngine.assessCPR(emptySet(), 0, RateStatus.INSUFFICIENT_DATA, System.currentTimeMillis())
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.CPR_COMPRESSIONS_STARTED }?.result)
        assertEquals(CriterionResult.NOT_ASSESSED, criteria.find { it.key == AssessmentEngine.CPR_THIRTY_CYCLES }?.result)
    }

    @Test
    fun cprTooSlowRate_needsPractice() {
        val criteria = AssessmentEngine.assessCPR(setOf(FSMState.COMPRESSION_ACTIVE), 10, RateStatus.TOO_SLOW, System.currentTimeMillis())
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.CPR_COMPRESSION_RATE }?.result)
    }

    @Test
    fun cprTooFewCycles_needsPractice() {
        val criteria = AssessmentEngine.assessCPR(setOf(FSMState.COMPRESSION_ACTIVE), 15, RateStatus.GOOD, System.currentTimeMillis())
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.CPR_THIRTY_CYCLES }?.result)
    }

    @Test
    fun cprExactlyThirtyCycles_pass() {
        val criteria = AssessmentEngine.assessCPR(setOf(FSMState.COMPRESSION_ACTIVE), 30, RateStatus.GOOD, System.currentTimeMillis())
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.CPR_THIRTY_CYCLES }?.result)
    }

    // ─── AssessmentEngine — Stroke ────────────────────────────────────────────

    @Test
    fun strokeComplete_speechPrompted_allPass() {
        val signals = StrokeSignals(0.5f, true, true, 2)
        val criteria = AssessmentEngine.assessStroke(signals, true, System.currentTimeMillis())
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.STROKE_FACE }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.STROKE_SPEECH }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.STROKE_EMERGENCY_CALL }?.result)
    }

    @Test
    fun strokeComplete_speechNotPrompted_needsPractice() {
        val signals = StrokeSignals(0.2f, false, false, 0)
        val criteria = AssessmentEngine.assessStroke(signals, true, System.currentTimeMillis())
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.STROKE_SPEECH }?.result)
        // RESULT reached → emergency call PASS
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.STROKE_EMERGENCY_CALL }?.result)
    }

    @Test
    fun strokeIncomplete_notAssessed() {
        val signals = StrokeSignals(0f, false, false, 0)
        val criteria = AssessmentEngine.assessStroke(signals, false, System.currentTimeMillis())
        assertEquals(CriterionResult.NOT_ASSESSED, criteria.find { it.key == AssessmentEngine.STROKE_FACE }?.result)
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.STROKE_EMERGENCY_CALL }?.result)
    }

    // ─── AssessmentEngine — Allergic reaction ─────────────────────────────────

    @Test
    fun allergicFullCompletion_allPass() {
        val criteria = AssessmentEngine.assessAllergicReaction(5, System.currentTimeMillis())
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.ALLERGY_LAY_FLAT }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.ALLERGY_EPIPEN_PREP }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.ALLERGY_EPIPEN_INJECT }?.result)
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.ALLERGY_MONITORING }?.result)
    }

    @Test
    fun allergicOnlyLayFlat_epipenStepsNeedPractice() {
        val criteria = AssessmentEngine.assessAllergicReaction(0, System.currentTimeMillis())
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.ALLERGY_LAY_FLAT }?.result)
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.ALLERGY_EPIPEN_INJECT }?.result)
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.ALLERGY_MONITORING }?.result)
    }

    @Test
    fun allergicEpiPenInjected_monitoringNeedsPractice() {
        val criteria = AssessmentEngine.assessAllergicReaction(4, System.currentTimeMillis())
        assertEquals(CriterionResult.PASS, criteria.find { it.key == AssessmentEngine.ALLERGY_EPIPEN_INJECT }?.result)
        assertEquals(CriterionResult.NEEDS_PRACTICE, criteria.find { it.key == AssessmentEngine.ALLERGY_MONITORING }?.result)
    }

    // ─── SessionOutcome derived fields ────────────────────────────────────────

    @Test
    fun sessionOutcome_overallResult_allPass() {
        val criteria = listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.PASS)
        )
        val outcome = makeOutcome(TrainingModuleType.CPR, criteria)
        assertEquals(CriterionResult.PASS, outcome.overallResult)
    }

    @Test
    fun sessionOutcome_overallResult_anyNeedsPractice() {
        val criteria = listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE)
        )
        val outcome = makeOutcome(TrainingModuleType.CPR, criteria)
        assertEquals(CriterionResult.NEEDS_PRACTICE, outcome.overallResult)
    }

    @Test
    fun sessionOutcome_passCount() {
        val criteria = listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSION_RATE, CriterionResult.INCONCLUSIVE)
        )
        val outcome = makeOutcome(TrainingModuleType.CPR, criteria)
        assertEquals(1, outcome.passCount)
        assertEquals(3, outcome.totalCriteria)
    }

    // ─── OutcomeComparison ────────────────────────────────────────────────────

    @Test
    fun comparison_newlyPassedDetected() {
        val prev = makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.NEEDS_PRACTICE),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.PASS)
        ))
        val curr = makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.PASS)
        ))
        val comp = AssessmentEngine.compare(prev, curr)
        assertTrue(comp.hasImprovement)
        assertEquals(1, comp.newlyPassed.size)
        assertEquals(AssessmentEngine.CPR_CALL_112, comp.newlyPassed[0])
        assertEquals(1, comp.passCountDelta)
    }

    @Test
    fun comparison_noImprovement() {
        val prev = makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS)
        ))
        val curr = makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS)
        ))
        val comp = AssessmentEngine.compare(prev, curr)
        assertFalse(comp.hasImprovement)
        assertEquals(0, comp.passCountDelta)
    }

    @Test
    fun comparison_noPreviousSession() {
        val curr = makeOutcome(TrainingModuleType.STROKE_FAST, listOf(
            CriterionRecord(AssessmentEngine.STROKE_FACE, CriterionResult.PASS)
        ))
        val comp = AssessmentEngine.compare(null, curr)
        assertNull(comp.previous)
        assertEquals(1, comp.unchangedPass.size + comp.newlyPassed.size + comp.stillNeedsPractice.size)
    }

    // ─── InMemoryTrainingStore ────────────────────────────────────────────────

    @Test
    fun store_saveAndLoad() {
        val store = InMemoryTrainingStore()
        val outcome = makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS)
        ))
        store.saveSession(outcome)
        val loaded = store.loadSessions(TrainingModuleType.CPR)
        assertEquals(1, loaded.size)
        assertEquals(outcome.sessionId, loaded[0].sessionId)
    }

    @Test
    fun store_separateModules() {
        val store = InMemoryTrainingStore()
        store.saveSession(makeOutcome(TrainingModuleType.CPR, emptyList()))
        store.saveSession(makeOutcome(TrainingModuleType.STROKE_FAST, emptyList()))
        assertEquals(1, store.loadSessions(TrainingModuleType.CPR).size)
        assertEquals(1, store.loadSessions(TrainingModuleType.STROKE_FAST).size)
        assertEquals(0, store.loadSessions(TrainingModuleType.ALLERGIC_REACTION).size)
    }

    @Test
    fun store_loadAllSessions() {
        val store = InMemoryTrainingStore()
        store.saveSession(makeOutcome(TrainingModuleType.CPR, emptyList()))
        store.saveSession(makeOutcome(TrainingModuleType.ALLERGIC_REACTION, emptyList()))
        val all = store.loadAllSessions()
        assertEquals(2, all.values.sumOf { it.size })
    }

    // ─── LearnerProfile ───────────────────────────────────────────────────────

    @Test
    fun profile_sessionCount() {
        val store = InMemoryTrainingStore()
        store.saveSession(makeOutcome(TrainingModuleType.CPR, emptyList()))
        store.saveSession(makeOutcome(TrainingModuleType.CPR, emptyList()))
        val profile = LearnerProfile(store.loadAllSessions())
        assertEquals(2, profile.sessionCount(TrainingModuleType.CPR))
        assertEquals(0, profile.sessionCount(TrainingModuleType.STROKE_FAST))
    }

    @Test
    fun profile_lastSession() {
        val store = InMemoryTrainingStore()
        val older = makeOutcome(TrainingModuleType.CPR, emptyList(), endMs = 1000L)
        val newer = makeOutcome(TrainingModuleType.CPR, emptyList(), endMs = 2000L)
        store.saveSession(older)
        store.saveSession(newer)
        val profile = LearnerProfile(store.loadAllSessions())
        assertEquals(newer.sessionId, profile.lastSession(TrainingModuleType.CPR)?.sessionId)
    }

    @Test
    fun profile_repeatedWeakCriteria_detectedAcrossTwoSessions() {
        val store = InMemoryTrainingStore()
        val weakCriteria1 = listOf(
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE, timestampMs = 1000L)
        )
        val weakCriteria2 = listOf(
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE, timestampMs = 2000L)
        )
        store.saveSession(makeOutcome(TrainingModuleType.CPR, weakCriteria1, endMs = 1000L))
        store.saveSession(makeOutcome(TrainingModuleType.CPR, weakCriteria2, endMs = 2000L))
        val profile = LearnerProfile(store.loadAllSessions())
        val repeated = profile.repeatedWeakCriteria(TrainingModuleType.CPR)
        assertEquals(1, repeated.size)
        assertEquals(AssessmentEngine.CPR_COMPRESSIONS_STARTED, repeated[0])
    }

    @Test
    fun profile_repeatedWeakCriteria_emptyIfOnlyOneSession() {
        val store = InMemoryTrainingStore()
        store.saveSession(makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE)
        )))
        val profile = LearnerProfile(store.loadAllSessions())
        assertTrue(profile.repeatedWeakCriteria(TrainingModuleType.CPR).isEmpty())
    }

    // ─── RecommendationEngine ─────────────────────────────────────────────────

    @Test
    fun recommendations_noSessions_suggestsFirstSession() {
        val profile = LearnerProfile()
        val recs = RecommendationEngine.recommend(profile, TrainingModuleType.CPR)
        assertTrue(recs.isNotEmpty())
        assertTrue(recs.any { it.criterionKey == null })
    }

    @Test
    fun recommendations_failedCriteria_suggestsPractice() {
        val store = InMemoryTrainingStore()
        store.saveSession(makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE)
        ), endMs = System.currentTimeMillis()))
        val profile = LearnerProfile(store.loadAllSessions())
        val recs = RecommendationEngine.recommend(profile, TrainingModuleType.CPR)
        assertTrue(recs.any { it.criterionKey == AssessmentEngine.CPR_COMPRESSIONS_STARTED.key })
    }

    @Test
    fun recommendations_atMostThreeReturned() {
        val store = InMemoryTrainingStore()
        // Session with many failing criteria
        store.saveSession(makeOutcome(TrainingModuleType.CPR, listOf(
            CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE),
            CriterionRecord(AssessmentEngine.CPR_COMPRESSION_RATE, CriterionResult.NEEDS_PRACTICE),
            CriterionRecord(AssessmentEngine.CPR_THIRTY_CYCLES, CriterionResult.NEEDS_PRACTICE),
            CriterionRecord(AssessmentEngine.CPR_HAND_PLACEMENT, CriterionResult.NEEDS_PRACTICE)
        )))
        val profile = LearnerProfile(store.loadAllSessions())
        val recs = RecommendationEngine.recommend(profile)
        assertTrue(recs.size <= 3)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun makeOutcome(
        module: TrainingModuleType,
        criteria: List<CriterionRecord>,
        startMs: Long = 0L,
        endMs: Long = 60_000L
    ) = SessionOutcome(
        sessionId = UUID.randomUUID().toString(),
        moduleType = module,
        startMs = startMs,
        endMs = endMs,
        criteria = criteria
    )
}
