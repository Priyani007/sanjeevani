package com.sanjeevani

import com.sanjeevani.training.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ModuleSkillStoreTest {
    @Test
    fun `draft is upserted and detailed evidence survives recreation`() {
        val key = "test-${System.nanoTime()}"
        val firstStore = SharedPrefsModuleSkillStore(RuntimeEnvironment.getApplication(), key)
        val step = StrokeTrainingContent.steps.first()
        val wrong = ModuleSkillAssessmentEngine.evaluate(
            step, "durable-session", step.options.first { !it.isCorrect }.id,
            AssessmentMethod.VOICE, "option D", .78f, 0
        )
        firstStore.saveSession(ModuleTrainingSession(
            sessionId = "durable-session", moduleType = "STROKE_FAST", attempts = listOf(wrong)
        ))

        val corrected = ModuleSkillAssessmentEngine.evaluate(
            step, "durable-session", step.correctOption.id,
            AssessmentMethod.BUTTON, step.correctOption.id, null, 1
        )
        firstStore.saveSession(ModuleTrainingSession(
            sessionId = "durable-session", moduleType = "STROKE_FAST", attempts = listOf(wrong, corrected),
            focusedSkillTag = step.skillTag
        ))

        val recreated = SharedPrefsModuleSkillStore(RuntimeEnvironment.getApplication(), key).loadSessions()
        assertEquals(1, recreated.size)
        assertEquals(2, recreated.single().attempts.size)
        assertEquals(AssessmentMethod.VOICE, recreated.single().attempts.first().assessmentMethod)
        assertEquals("option D", recreated.single().attempts.first().learnerResponse)
        assertEquals(.78f, recreated.single().attempts.first().confidence!!, .001f)
        assertEquals(CorrectionStatus.CORRECTED, recreated.single().attempts.last().correctionStatus)
        assertEquals(step.skillTag, recreated.single().focusedSkillTag)
        firstStore.clearAllSessions()
    }

    @Test
    fun `placement provenance and directional result persist`() {
        val key = "placement-${System.nanoTime()}"
        val store = SharedPrefsModuleSkillStore(RuntimeEnvironment.getApplication(), key)
        val step = AllergyTrainingContent.steps.first { it.requiresPlacementSimulation }
        val event = ModuleTrainingAttempt(
            sessionId = "placement-session", moduleType = "ALLERGIC_REACTION",
            skillTag = step.skillTag, skillDescription = step.skillDescription,
            stepIndex = step.stepIndex, questionId = "${step.skillTag}.placement",
            selectedOptionId = step.correctOption.id, correctOptionId = step.correctOption.id,
            knowledgeResult = KnowledgeResult.CORRECT, outcome = SkillAttemptOutcome.NEEDS_PRACTICE,
            feedbackShown = AllergyPlacementAssessment.feedback(PlacementResult.TOO_MEDIAL),
            assessmentMethod = AssessmentMethod.SIMULATED_PLACEMENT,
            learnerResponse = "x=0.500,y=0.500", errorCategory = "too_medial", confidence = 1f,
            observationQuality = ObservationQuality.HIGH, correctionStatus = CorrectionStatus.RETRY_REQUIRED,
            placementResult = PlacementResult.TOO_MEDIAL, dataProvenance = "guided_diagram_simulation"
        )
        store.saveSession(ModuleTrainingSession(sessionId = event.sessionId, moduleType = event.moduleType, attempts = listOf(event)))
        val loaded = store.loadSessions().single().attempts.single()
        assertEquals(PlacementResult.TOO_MEDIAL, loaded.placementResult)
        assertEquals("guided_diagram_simulation", loaded.dataProvenance)
        assertEquals("too_medial", loaded.errorCategory)
        store.clearAllSessions()
    }
}
