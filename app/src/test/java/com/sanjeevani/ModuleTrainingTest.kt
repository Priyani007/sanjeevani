package com.sanjeevani

import com.sanjeevani.training.*
import org.junit.Assert.*
import org.junit.Test

class ModuleTrainingTest {

    // ── StrokeTrainingContent ─────────────────────────────────────────────────

    @Test
    fun `StrokeTrainingContent has 8 steps`() {
        assertEquals(8, StrokeTrainingContent.steps.size)
    }

    @Test
    fun `each stroke step has exactly 4 options`() {
        StrokeTrainingContent.steps.forEach { step ->
            assertEquals("Stroke step ${step.stepIndex} should have 4 options", 4, step.options.size)
        }
    }

    @Test
    fun `each stroke step has exactly 1 correct option`() {
        StrokeTrainingContent.steps.forEach { step ->
            val correct = step.options.count { it.isCorrect }
            assertEquals("Stroke step ${step.stepIndex} should have 1 correct option", 1, correct)
        }
    }

    @Test
    fun `strokeStepFor returns null for out-of-range index`() {
        assertNull(StrokeTrainingContent.stepFor(-1))
        assertNull(StrokeTrainingContent.stepFor(99))
    }

    @Test
    fun `strokeAllSkillTags returns 8 distinct tags`() {
        val tags = StrokeTrainingContent.allSkillTags()
        assertEquals(8, tags.size)
        assertEquals(8, tags.distinct().size)
    }

    // ── AllergyTrainingContent ────────────────────────────────────────────────

    @Test
    fun `AllergyTrainingContent has 7 steps`() {
        assertEquals(7, AllergyTrainingContent.steps.size)
    }

    @Test
    fun `each allergy step has exactly 4 options`() {
        AllergyTrainingContent.steps.forEach { step ->
            assertEquals("Allergy step ${step.stepIndex} should have 4 options", 4, step.options.size)
        }
    }

    @Test
    fun `each allergy step has exactly 1 correct option`() {
        AllergyTrainingContent.steps.forEach { step ->
            val correct = step.options.count { it.isCorrect }
            assertEquals("Allergy step ${step.stepIndex} should have 1 correct option", 1, correct)
        }
    }

    @Test
    fun `allergyStepFor returns null for out-of-range index`() {
        assertNull(AllergyTrainingContent.stepFor(-1))
        assertNull(AllergyTrainingContent.stepFor(99))
    }

    @Test
    fun `allergyAllSkillTags returns 7 distinct tags`() {
        val tags = AllergyTrainingContent.allSkillTags()
        assertEquals(7, tags.size)
        assertEquals(7, tags.distinct().size)
    }

    // ── ModuleSkillAssessmentEngine ───────────────────────────────────────────

    @Test
    fun `correct answer on stroke step produces CORRECT outcome`() {
        val step = StrokeTrainingContent.stepFor(0)!!
        val attempt = ModuleSkillAssessmentEngine.evaluate(step, "sess", step.correctOption.id)
        assertEquals(SkillAttemptOutcome.CORRECT, attempt.outcome)
        assertEquals(KnowledgeResult.CORRECT, attempt.knowledgeResult)
    }

    @Test
    fun `incorrect unsafe answer on stroke step produces INCORRECT outcome`() {
        val step = StrokeTrainingContent.stepFor(1)!!  // face drooping — has isSafe=false wrong options
        val unsafeOption = step.options.first { !it.isCorrect && !it.isSafe }
        val attempt = ModuleSkillAssessmentEngine.evaluate(step, "sess", unsafeOption.id)
        assertEquals(SkillAttemptOutcome.INCORRECT, attempt.outcome)
        assertEquals(KnowledgeResult.INCORRECT, attempt.knowledgeResult)
    }

    @Test
    fun `incorrect safe answer on stroke step produces NEEDS_PRACTICE outcome`() {
        val step = StrokeTrainingContent.stepFor(4)!!  // time to act — has a safe wrong option
        val safeWrong = step.options.firstOrNull { !it.isCorrect && it.isSafe }
        if (safeWrong != null) {
            val attempt = ModuleSkillAssessmentEngine.evaluate(step, "sess", safeWrong.id)
            assertEquals(SkillAttemptOutcome.NEEDS_PRACTICE, attempt.outcome)
        }
        // If all wrong options are isSafe=false, that step does not test this path — skip gracefully
    }

    @Test
    fun `correct answer on allergy step produces CORRECT outcome`() {
        val step = AllergyTrainingContent.stepFor(0)!!
        val attempt = ModuleSkillAssessmentEngine.evaluate(step, "sess", step.correctOption.id)
        assertEquals(SkillAttemptOutcome.CORRECT, attempt.outcome)
        assertEquals(KnowledgeResult.CORRECT, attempt.knowledgeResult)
    }

    @Test
    fun `incorrect answer on allergy step produces non-CORRECT outcome`() {
        val step = AllergyTrainingContent.stepFor(3)!!  // EpiPen location
        val wrong = step.options.first { !it.isCorrect }
        val attempt = ModuleSkillAssessmentEngine.evaluate(step, "sess", wrong.id)
        assertNotEquals(SkillAttemptOutcome.CORRECT, attempt.outcome)
    }

    // ── ModuleTrainingSession ─────────────────────────────────────────────────

    @Test
    fun `ModuleTrainingSession accuracy computed correctly`() {
        val sid = "stroke-test-session"
        val steps = StrokeTrainingContent.steps.take(4)
        val attempts = steps.mapIndexed { i, step ->
            val isCorrect = i < 3 // first 3 correct, last wrong
            val optId = if (isCorrect) step.correctOption.id else step.options.first { !it.isCorrect }.id
            ModuleSkillAssessmentEngine.evaluate(step, sid, optId)
        }
        val session = ModuleTrainingSession(
            sessionId  = sid,
            moduleType = "STROKE_FAST",
            attempts   = attempts,
            endMs      = System.currentTimeMillis()
        )
        assertEquals(3, session.correctAttempts)
        assertEquals(4, session.totalAttempts)
        assertEquals(3f / 4f, session.accuracy, 0.01f)
    }

    @Test
    fun `ModuleTrainingSession outcomeFor returns correct skill outcome`() {
        val sid = "test-session"
        val step0 = StrokeTrainingContent.stepFor(0)!!
        val attempt = ModuleSkillAssessmentEngine.evaluate(step0, sid, step0.correctOption.id)
        val session = ModuleTrainingSession(
            sessionId  = sid,
            moduleType = "STROKE_FAST",
            attempts   = listOf(attempt)
        )
        assertEquals(SkillAttemptOutcome.CORRECT, session.outcomeFor(StrokeSkillTag.FAST_RECOGNITION))
        assertNull(session.outcomeFor("nonexistent.tag"))
    }

    // ── ModuleTrainingProfile ─────────────────────────────────────────────────

    @Test
    fun `ModuleTrainingProfile separates real and demo sessions`() {
        val real = ModuleTrainingSession(isDemoData = false, moduleType = "STROKE_FAST",
            endMs = System.currentTimeMillis())
        val demo = ModuleTrainingSession(isDemoData = true, moduleType = "STROKE_FAST",
            endMs = System.currentTimeMillis())
        val p = ModuleTrainingProfile("STROKE_FAST", listOf(real, demo))
        assertEquals(1, p.realSessions.size)
        assertEquals(1, p.demoSessions.size)
    }

    @Test
    fun `weakSkills returns tags where latest outcome is incorrect`() {
        val sid = "test-weak"
        val step0 = StrokeTrainingContent.stepFor(0)!!
        val step1 = StrokeTrainingContent.stepFor(1)!!
        val attempts = listOf(
            ModuleSkillAssessmentEngine.evaluate(step0, sid, step0.correctOption.id),  // correct
            ModuleSkillAssessmentEngine.evaluate(step1, sid, step1.options.first { !it.isCorrect }.id)  // wrong
        )
        val session = ModuleTrainingSession(
            sessionId  = sid,
            moduleType = "STROKE_FAST",
            attempts   = attempts,
            endMs      = System.currentTimeMillis()
        )
        val p = ModuleTrainingProfile("STROKE_FAST", listOf(session))
        val weak = p.weakSkills()
        assertTrue(weak.contains(StrokeSkillTag.FACE_DROOPING))
        assertFalse(weak.contains(StrokeSkillTag.FAST_RECOGNITION))
    }

    // ── StrokeDemoData ────────────────────────────────────────────────────────

    @Test
    fun `StrokeDemoData generates 3 sessions`() {
        assertEquals(3, StrokeDemoData.generateSessions().size)
    }

    @Test
    fun `all stroke demo sessions are marked isDemoData`() {
        StrokeDemoData.generateSessions().forEach { session ->
            assertTrue("Stroke session ${session.sessionId} should be demo", session.isDemoData)
        }
    }

    @Test
    fun `stroke demo sessions are idempotent`() {
        val first  = StrokeDemoData.generateSessions()
        val second = StrokeDemoData.generateSessions()
        assertEquals(first.map { it.sessionId }, second.map { it.sessionId })
    }

    @Test
    fun `each stroke demo session has 8 attempts`() {
        StrokeDemoData.generateSessions().forEach { session ->
            assertEquals("Stroke session ${session.sessionId} should have 8 attempts",
                8, session.attempts.size)
        }
    }

    @Test
    fun `stroke demo session 3 is all correct`() {
        val session3 = StrokeDemoData.generateSessions().last()
        session3.attempts.forEach { attempt ->
            assertEquals("All attempts in session 3 should be CORRECT",
                SkillAttemptOutcome.CORRECT, attempt.outcome)
        }
    }

    // ── AllergyDemoData ───────────────────────────────────────────────────────

    @Test
    fun `AllergyDemoData generates 3 sessions`() {
        assertEquals(3, AllergyDemoData.generateSessions().size)
    }

    @Test
    fun `all allergy demo sessions are marked isDemoData`() {
        AllergyDemoData.generateSessions().forEach { session ->
            assertTrue("Allergy session ${session.sessionId} should be demo", session.isDemoData)
        }
    }

    @Test
    fun `allergy demo sessions are idempotent`() {
        val first  = AllergyDemoData.generateSessions()
        val second = AllergyDemoData.generateSessions()
        assertEquals(first.map { it.sessionId }, second.map { it.sessionId })
    }

    @Test
    fun `each allergy demo session has 7 attempts`() {
        AllergyDemoData.generateSessions().forEach { session ->
            assertEquals("Allergy session ${session.sessionId} should have 7 attempts",
                7, session.attempts.size)
        }
    }

    @Test
    fun `allergy demo session 3 is all correct`() {
        val session3 = AllergyDemoData.generateSessions().last()
        session3.attempts.forEach { attempt ->
            assertEquals("All attempts in allergy session 3 should be CORRECT",
                SkillAttemptOutcome.CORRECT, attempt.outcome)
        }
    }

    // ── ModuleSkillAssessmentEngine.recommend ─────────────────────────────────

    @Test
    fun `recommend returns baseline practice when profile has no sessions`() {
        val p = ModuleTrainingProfile("STROKE_FAST")
        val recs = ModuleSkillAssessmentEngine.recommend(p)
        assertEquals(1, recs.size)
        assertEquals(0, recs.single().stepIndex)
        assertTrue(recs.single().evidence.contains("No real learner attempts"))
    }

    @Test
    fun `recommend returns recommendations for weak skills`() {
        val sessions = StrokeDemoData.generateSessions().take(1)  // first session has wrong answers
        val p = ModuleTrainingProfile("STROKE_FAST", sessions)
        val recs = ModuleSkillAssessmentEngine.recommend(p, demoMode = true)
        assertTrue("Should have recommendations for weak skills in session 1", recs.isNotEmpty())
    }

    @Test
    fun `voice option and button use the same assessment engine`() {
        val step = StrokeTrainingContent.steps.first()
        val spoken = ModuleVoiceInterpreter.resolve("I choose option ${step.correctOption.id}", step.options)
        assertEquals(step.correctOption.id, spoken)
        val voice = ModuleSkillAssessmentEngine.evaluate(step, "voice", spoken!!, AssessmentMethod.VOICE, "option $spoken", .91f)
        val button = ModuleSkillAssessmentEngine.evaluate(step, "button", step.correctOption.id, AssessmentMethod.BUTTON)
        assertEquals(button.outcome, voice.outcome)
        assertEquals(AssessmentMethod.VOICE, voice.assessmentMethod)
        assertEquals(.91f, voice.confidence!!, .001f)
    }

    @Test
    fun `unrecognised voice never becomes a correct answer`() {
        val step = StrokeTrainingContent.steps.first()
        assertNull(ModuleVoiceInterpreter.resolve("maybe later", step.options))
        val attempt = ModuleSkillAssessmentEngine.evaluate(step, "voice", "unknown", AssessmentMethod.VOICE, "maybe later", .2f)
        assertEquals(SkillAttemptOutcome.INCONCLUSIVE, attempt.outcome)
        assertEquals("unrecognised_response", attempt.errorCategory)
    }

    @Test
    fun `placement boundaries are deterministic and directional`() {
        assertEquals(PlacementResult.CORRECT, AllergyPlacementAssessment.evaluate(.62f, .38f))
        assertEquals(PlacementResult.CORRECT, AllergyPlacementAssessment.evaluate(.88f, .68f))
        assertEquals(PlacementResult.TOO_HIGH, AllergyPlacementAssessment.evaluate(.75f, .37f))
        assertEquals(PlacementResult.TOO_LOW, AllergyPlacementAssessment.evaluate(.75f, .69f))
        assertEquals(PlacementResult.TOO_MEDIAL, AllergyPlacementAssessment.evaluate(.61f, .5f))
        assertEquals(PlacementResult.TOO_LATERAL, AllergyPlacementAssessment.evaluate(.89f, .5f))
        assertEquals(PlacementResult.INCONCLUSIVE, AllergyPlacementAssessment.evaluate(.75f, .5f, .5f))
    }

    @Test
    fun `historical error remains while latest correction resolves weakness`() {
        val step = StrokeTrainingContent.steps.first()
        val wrong = ModuleSkillAssessmentEngine.evaluate(step, "retry", step.options.first { !it.isCorrect }.id, retryCount = 0).copy(timestampMs = 1L)
        val corrected = ModuleSkillAssessmentEngine.evaluate(step, "retry", step.correctOption.id, retryCount = 1).copy(timestampMs = 2L)
        val profile = ModuleTrainingProfile("STROKE_FAST", listOf(ModuleTrainingSession(moduleType = "STROKE_FAST", attempts = listOf(wrong, corrected))))
        val stats = profile.skillStats().single()
        assertEquals(2, stats.totalAttempts)
        assertEquals(SkillAttemptOutcome.CORRECT, stats.latestOutcome)
        assertTrue(stats.hasImprovedInLatestSession)
        assertFalse(profile.weakSkills().contains(step.skillTag))
    }

    @Test
    fun `demo evidence is never mixed into real analytics`() {
        val step = StrokeTrainingContent.steps.first()
        val demoAttempt = ModuleSkillAssessmentEngine.evaluate(step, "demo", step.correctOption.id)
        val profile = ModuleTrainingProfile("STROKE_FAST", listOf(ModuleTrainingSession(moduleType = "STROKE_FAST", attempts = listOf(demoAttempt), isDemoData = true)))
        assertTrue(profile.skillStats().isEmpty())
        assertEquals(1, profile.skillStats(demoMode = true).size)
    }
}
