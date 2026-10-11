package com.sanjeevani

import com.sanjeevani.model.SpatialAction
import com.sanjeevani.training.*
import org.junit.Assert.*
import org.junit.Test

class CprTrainingTest {

    // ── CprTrainingContent ────────────────────────────────────────────────────

    @Test
    fun `CprTrainingContent has 7 steps`() {
        assertEquals(7, CprTrainingContent.steps.size)
    }

    @Test
    fun `each step has exactly 4 options`() {
        CprTrainingContent.steps.forEach { step ->
            assertEquals("Step ${step.stepIndex} should have 4 options", 4, step.options.size)
        }
    }

    @Test
    fun `each step has exactly 1 correct option`() {
        CprTrainingContent.steps.forEach { step ->
            val correct = step.options.count { it.isCorrect }
            assertEquals("Step ${step.stepIndex} should have 1 correct option", 1, correct)
        }
    }

    @Test
    fun `only step 4 uses camera assessment`() {
        val cameraSteps = CprTrainingContent.steps.filter { it.useCameraForAssessment }
        assertEquals(1, cameraSteps.size)
        assertEquals(4, cameraSteps.first().stepIndex)
    }

    @Test
    fun `stepFor returns null for out-of-range index`() {
        assertNull(CprTrainingContent.stepFor(-1))
        assertNull(CprTrainingContent.stepFor(99))
    }

    @Test
    fun `allSkillTags returns 7 distinct tags`() {
        val tags = CprTrainingContent.allSkillTags()
        assertEquals(7, tags.size)
        assertEquals(7, tags.distinct().size)
    }

    // ── CprSkillAssessmentEngine ───────────────────────────────────────────────

    @Test
    fun `correct knowledge answer on non-camera step produces CORRECT outcome`() {
        val step = CprTrainingContent.stepFor(0)!! // scene safety — no camera
        val correctId = step.correctOption.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "test-session", correctId, null)
        assertEquals(SkillAttemptOutcome.CORRECT, attempt.outcome)
        assertEquals(KnowledgeResult.CORRECT, attempt.knowledgeResult)
        assertEquals(HandPlacementResult.NOT_OBSERVED, attempt.handPlacementResult)
    }

    @Test
    fun `incorrect knowledge answer produces INCORRECT outcome`() {
        val step = CprTrainingContent.stepFor(0)!!
        val wrongId = step.options.first { !it.isCorrect }.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "test-session", wrongId, null)
        assertEquals(SkillAttemptOutcome.INCORRECT, attempt.outcome)
        assertEquals(KnowledgeResult.INCORRECT, attempt.knowledgeResult)
    }

    @Test
    fun `camera step with CORRECT spatial action and correct knowledge produces CORRECT outcome`() {
        val step = CprTrainingContent.stepFor(4)!! // hand placement — camera step
        val correctId = step.correctOption.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "sess", correctId, SpatialAction.CORRECT)
        assertEquals(SkillAttemptOutcome.CORRECT, attempt.outcome)
        assertEquals(HandPlacementResult.CORRECT, attempt.handPlacementResult)
    }

    @Test
    fun `camera step with MOVE_DOWN produces TOO_HIGH placement result`() {
        val step = CprTrainingContent.stepFor(4)!!
        val correctId = step.correctOption.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "sess", correctId, SpatialAction.MOVE_DOWN)
        assertEquals(HandPlacementResult.TOO_HIGH, attempt.handPlacementResult)
        assertEquals(SkillAttemptOutcome.NEEDS_PRACTICE, attempt.outcome)
    }

    @Test
    fun `camera step with MOVE_UP produces TOO_LOW placement result`() {
        val step = CprTrainingContent.stepFor(4)!!
        val correctId = step.correctOption.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "sess", correctId, SpatialAction.MOVE_UP)
        assertEquals(HandPlacementResult.TOO_LOW, attempt.handPlacementResult)
    }

    @Test
    fun `camera step with TRACKING_LOST produces INCONCLUSIVE outcome`() {
        val step = CprTrainingContent.stepFor(4)!!
        val correctId = step.correctOption.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "sess", correctId, SpatialAction.TRACKING_LOST)
        assertEquals(HandPlacementResult.UNABLE_TO_ASSESS, attempt.handPlacementResult)
        assertEquals(SkillAttemptOutcome.INCONCLUSIVE, attempt.outcome)
    }

    @Test
    fun `incorrect knowledge on camera step overrides camera result to INCORRECT`() {
        val step = CprTrainingContent.stepFor(4)!!
        val wrongId = step.options.first { !it.isCorrect }.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "sess", wrongId, SpatialAction.CORRECT)
        assertEquals(KnowledgeResult.INCORRECT, attempt.knowledgeResult)
        assertEquals(SkillAttemptOutcome.INCORRECT, attempt.outcome)
    }

    @Test
    fun `camera step with null spatial action (skipped) and correct knowledge produces CORRECT`() {
        val step = CprTrainingContent.stepFor(4)!!
        val correctId = step.correctOption.id
        val attempt = CprSkillAssessmentEngine.evaluate(step, "sess", correctId, null)
        assertEquals(HandPlacementResult.NOT_OBSERVED, attempt.handPlacementResult)
        assertEquals(SkillAttemptOutcome.CORRECT, attempt.outcome)
    }

    // ── CprSkillSession computed properties ───────────────────────────────────

    @Test
    fun `CprSkillSession accuracy computed correctly`() {
        val sid = "test-session"
        val attempts = listOf(
            makeAttempt(sid, 0, CprSkillTag.SCENE_SAFETY, SkillAttemptOutcome.CORRECT),
            makeAttempt(sid, 1, CprSkillTag.ARREST_RECOGNITION, SkillAttemptOutcome.INCORRECT),
            makeAttempt(sid, 2, CprSkillTag.EMERGENCY_ESCALATION, SkillAttemptOutcome.CORRECT)
        )
        val session = CprSkillSession(sessionId = sid, attempts = attempts, endMs = System.currentTimeMillis())
        assertEquals(2, session.correctAttempts)
        assertEquals(3, session.totalAttempts)
        assertEquals(2f / 3f, session.accuracy, 0.01f)
    }

    @Test
    fun `CprSkillSession outcomeFor returns latest attempt outcome for tag`() {
        val sid = "test-session"
        val t = System.currentTimeMillis()
        val attempts = listOf(
            makeAttempt(sid, 0, CprSkillTag.SCENE_SAFETY, SkillAttemptOutcome.INCORRECT, ts = t - 1000),
            makeAttempt(sid, 0, CprSkillTag.SCENE_SAFETY, SkillAttemptOutcome.CORRECT,   ts = t)
        )
        val session = CprSkillSession(sessionId = sid, attempts = attempts)
        assertEquals(SkillAttemptOutcome.CORRECT, session.outcomeFor(CprSkillTag.SCENE_SAFETY))
    }

    // ── CprTrainingProfile ────────────────────────────────────────────────────

    @Test
    fun `CprTrainingProfile separates real and demo sessions`() {
        val real = CprSkillSession(isDemoData = false, endMs = System.currentTimeMillis())
        val demo = CprSkillSession(isDemoData = true,  endMs = System.currentTimeMillis())
        val p = CprTrainingProfile(listOf(real, demo))
        assertEquals(1, p.realSessions.size)
        assertEquals(1, p.demoSessions.size)
    }

    @Test
    fun `weakSkills returns tags where latest outcome is incorrect`() {
        val sid = "s1"
        val attempts = listOf(
            makeAttempt(sid, 0, CprSkillTag.SCENE_SAFETY, SkillAttemptOutcome.CORRECT),
            makeAttempt(sid, 6, CprSkillTag.COMPRESSION_RATE, SkillAttemptOutcome.INCORRECT)
        )
        val session = CprSkillSession(sessionId = sid, attempts = attempts,
            endMs = System.currentTimeMillis())
        val p = CprTrainingProfile(listOf(session))
        val weak = p.weakSkills()
        assertTrue(weak.contains(CprSkillTag.COMPRESSION_RATE))
        assertFalse(weak.contains(CprSkillTag.SCENE_SAFETY))
    }

    // ── CprDemoData ───────────────────────────────────────────────────────────

    @Test
    fun `CprDemoData generates 3 sessions`() {
        assertEquals(3, CprDemoData.generateSessions().size)
    }

    @Test
    fun `all demo sessions marked isDemoData`() {
        CprDemoData.generateSessions().forEach { session ->
            assertTrue("Session ${session.sessionId} should be demo", session.isDemoData)
        }
    }

    @Test
    fun `demo sessions are idempotent`() {
        val first  = CprDemoData.generateSessions()
        val second = CprDemoData.generateSessions()
        assertEquals(first.map { it.sessionId }, second.map { it.sessionId })
    }

    @Test
    fun `each demo session has 7 attempts (one per step)`() {
        CprDemoData.generateSessions().forEach { session ->
            assertEquals("Session ${session.sessionId} should have 7 attempts",
                7, session.attempts.size)
        }
    }

    @Test
    fun `demo session 3 shows improvement in hand placement`() {
        val sessions = CprDemoData.generateSessions()
        val last = sessions.last()
        val handAttempt = last.attempts.firstOrNull {
            it.skillTag == CprSkillTag.HAND_PLACEMENT_REGION }
        assertNotNull(handAttempt)
        assertEquals(HandPlacementResult.CORRECT, handAttempt!!.handPlacementResult)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeAttempt(
        sessionId: String,
        stepIndex: Int,
        skillTag: String,
        outcome: SkillAttemptOutcome,
        ts: Long = System.currentTimeMillis()
    ) = CprSkillAttempt(
        sessionId             = sessionId,
        skillTag              = skillTag,
        skillDescription      = skillTag,
        stepIndex             = stepIndex,
        questionId            = "$skillTag.$stepIndex",
        selectedOptionId      = "C",
        correctOptionId       = "C",
        knowledgeResult       = if (outcome == SkillAttemptOutcome.CORRECT) KnowledgeResult.CORRECT
                                else KnowledgeResult.INCORRECT,
        outcome               = outcome,
        feedbackShown         = "",
        timestampMs           = ts
    )
}
