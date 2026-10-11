package com.sanjeevani

import com.sanjeevani.training.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class QuizAssessmentTest {

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun makeQuestion(
        id: String = "Q_TEST",
        type: QuestionType = QuestionType.SINGLE_CHOICE,
        correctIds: Set<String> = setOf("B"),
        isCritical: Boolean = false,
        skillTags: List<String> = listOf(QuizSkillTag.SYMPTOM_RECOGNITION)
    ) = QuizQuestion(
        id = id,
        category = QuizCategory.HEART_ATTACK,
        difficulty = QuizDifficulty.BEGINNER,
        type = type,
        scenario = "Test scenario",
        questionText = "Test question?",
        options = listOf(
            QuizOption("A", "Wrong option", "Explanation A"),
            QuizOption("B", "Correct option", "Explanation B"),
            QuizOption("C", "Another wrong", "Explanation C")
        ),
        correctOptionIds = correctIds,
        skillTags = skillTags,
        misconceptionTested = "test",
        learningObjective = "test",
        isCriticalSafety = isCritical
    )

    private fun makeAttempt(
        category: QuizCategory = QuizCategory.HEART_ATTACK,
        answers: List<QuizAnswerRecord>,
        isDemoData: Boolean = false
    ) = QuizAttempt(
        attemptId = UUID.randomUUID().toString(),
        startMs   = System.currentTimeMillis() - 60_000,
        endMs     = System.currentTimeMillis(),
        category  = category,
        difficulty = QuizDifficulty.BEGINNER,
        answers   = answers,
        isDemoData = isDemoData
    )

    // ── Single-choice: correct answer ─────────────────────────────────────────

    @Test
    fun `evaluateAnswer correct single choice returns isCorrect true`() {
        val q = makeQuestion(correctIds = setOf("B"))
        val record = QuizAssessmentEngine.evaluateAnswer(q, setOf("B"), emptyList())
        assertNotNull(record)
        assertTrue(record!!.isCorrect)
        assertEquals(setOf("B"), record.selectedOptionIds)
    }

    // ── Single-choice: wrong answer ───────────────────────────────────────────

    @Test
    fun `evaluateAnswer wrong single choice returns isCorrect false`() {
        val q = makeQuestion(correctIds = setOf("B"))
        val record = QuizAssessmentEngine.evaluateAnswer(q, setOf("A"), emptyList())
        assertNotNull(record)
        assertFalse(record!!.isCorrect)
    }

    // ── Duplicate submission guard ────────────────────────────────────────────

    @Test
    fun `evaluateAnswer returns null on duplicate questionId`() {
        val q = makeQuestion(id = "DUP_001")
        val first = QuizAssessmentEngine.evaluateAnswer(q, setOf("B"), emptyList())!!
        val second = QuizAssessmentEngine.evaluateAnswer(q, setOf("A"), listOf(first))
        assertNull(second)
    }

    // ── Critical safety error: wrong answer sets isCriticalSafety ────────────

    @Test
    fun `evaluateAnswer sets isCriticalSafety on wrong critical question`() {
        val q = makeQuestion(correctIds = setOf("B"), isCritical = true)
        val record = QuizAssessmentEngine.evaluateAnswer(q, setOf("A"), emptyList())!!
        assertFalse(record.isCorrect)
        assertTrue(record.isCriticalSafety)
    }

    @Test
    fun `evaluateAnswer does not set isCriticalSafety on correct critical question`() {
        val q = makeQuestion(correctIds = setOf("B"), isCritical = true)
        val record = QuizAssessmentEngine.evaluateAnswer(q, setOf("B"), emptyList())!!
        assertTrue(record.isCorrect)
        assertFalse(record.isCriticalSafety)
    }

    // ── skillBreakdown aggregation ────────────────────────────────────────────

    @Test
    fun `skillBreakdown computes accuracy correctly`() {
        val tag = QuizSkillTag.SYMPTOM_RECOGNITION
        val answers = listOf(
            QuizAnswerRecord("Q1", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"), true,  listOf(tag), false, 1L),
            QuizAnswerRecord("Q2", QuizCategory.HEART_ATTACK, setOf("A"), setOf("B"), false, listOf(tag), false, 2L),
            QuizAnswerRecord("Q3", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"), true,  listOf(tag), false, 3L)
        )
        val attempt = makeAttempt(answers = answers)
        val breakdown = QuizAssessmentEngine.skillBreakdown(listOf(attempt))
        val result = breakdown[tag]
        assertNotNull(result)
        assertEquals(3, result!!.attempted)
        assertEquals(2, result.correct)
        assertEquals(2f / 3f, result.accuracy, 0.01f)
    }

    // ── unresolvedWeakSkills logic ────────────────────────────────────────────

    @Test
    fun `unresolvedWeakSkills returns skill missed 2+ times with last answer wrong`() {
        val tag = QuizSkillTag.AGONAL_BREATHING
        val a1 = QuizAnswerRecord("CPR_002", QuizCategory.CPR_CARDIAC_ARREST, setOf("A"), setOf("B"), false, listOf(tag), false, 100L)
        val a2 = QuizAnswerRecord("CPR_002b", QuizCategory.CPR_CARDIAC_ARREST, setOf("A"), setOf("B"), false, listOf(tag), false, 200L)
        val attempts = listOf(
            makeAttempt(QuizCategory.CPR_CARDIAC_ARREST, listOf(a1)),
            makeAttempt(QuizCategory.CPR_CARDIAC_ARREST, listOf(a2))
        )
        val weak = QuizAssessmentEngine.unresolvedWeakSkills(attempts)
        assertTrue(weak.contains(tag))
    }

    @Test
    fun `unresolvedWeakSkills does not return skill when last answer is correct`() {
        val tag = QuizSkillTag.AGONAL_BREATHING
        val wrong = QuizAnswerRecord("CPR_002", QuizCategory.CPR_CARDIAC_ARREST, setOf("A"), setOf("B"), false, listOf(tag), false, 100L)
        val correct = QuizAnswerRecord("CPR_002b", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"), true, listOf(tag), false, 200L)
        val attempts = listOf(
            makeAttempt(QuizCategory.CPR_CARDIAC_ARREST, listOf(wrong)),
            makeAttempt(QuizCategory.CPR_CARDIAC_ARREST, listOf(correct))
        )
        val weak = QuizAssessmentEngine.unresolvedWeakSkills(attempts)
        assertFalse(weak.contains(tag))
    }

    // ── Demo data isolation ───────────────────────────────────────────────────

    @Test
    fun `QuizLearnerProfile separates real and demo attempts`() {
        val realAttempt = makeAttempt(answers = emptyList(), isDemoData = false)
        val demoAttempt = makeAttempt(answers = emptyList(), isDemoData = true)
        val profile = QuizLearnerProfile(listOf(realAttempt, demoAttempt))
        assertEquals(1, profile.realAttempts.size)
        assertEquals(1, profile.demoAttempts.size)
    }

    @Test
    fun `overallAccuracy with demoMode false excludes demo data`() {
        val realAnswer  = QuizAnswerRecord("Q1", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"), true,  listOf(), false, 1L, isDemoData = false)
        val demoAnswer  = QuizAnswerRecord("Q2", QuizCategory.HEART_ATTACK, setOf("A"), setOf("B"), false, listOf(), false, 2L, isDemoData = true)
        val realAttempt = makeAttempt(answers = listOf(realAnswer), isDemoData = false)
        val demoAttempt = makeAttempt(answers = listOf(demoAnswer), isDemoData = true)
        val profile = QuizLearnerProfile(listOf(realAttempt, demoAttempt))
        // Real-only accuracy = 1/1 = 100%
        assertEquals(1.0f, profile.overallAccuracy(demoMode = false), 0.01f)
        // Demo-mode accuracy = (1+0)/2 = 50%
        assertEquals(0.5f, profile.overallAccuracy(demoMode = true), 0.01f)
    }

    // ── Recommendation priority order ─────────────────────────────────────────

    @Test
    fun `recommendation for critical error has higher priority than repeated skill miss`() {
        val attempts = QuizDemoData.generateAttempts()
        val profile = QuizLearnerProfile(attempts)
        val recs = QuizRecommendationEngine.recommend(profile, demoMode = true)
        assertTrue(recs.isNotEmpty())
        val hasCritical = recs.any { it.reason == QuizRecommendationReason.CRITICAL_SAFETY_ERROR }
        assertTrue("Expected at least one critical safety recommendation from demo data", hasCritical)
        // Critical must come before repeated miss
        val critIdx = recs.indexOfFirst { it.reason == QuizRecommendationReason.CRITICAL_SAFETY_ERROR }
        val missIdx = recs.indexOfFirst { it.reason == QuizRecommendationReason.REPEATED_SKILL_MISS }
        if (missIdx != -1) assertTrue(critIdx < missIdx)
    }

    // ── Demo data idempotency ─────────────────────────────────────────────────

    @Test
    fun `QuizDemoData generateAttempts returns 6 attempts each time`() {
        val first  = QuizDemoData.generateAttempts()
        val second = QuizDemoData.generateAttempts()
        assertEquals(6, first.size)
        assertEquals(6, second.size)
        assertEquals(first.map { it.attemptId }, second.map { it.attemptId })
    }

    @Test
    fun `QuizDemoData all attempts marked isDemoData`() {
        QuizDemoData.generateAttempts().forEach { attempt ->
            assertTrue("Attempt ${attempt.attemptId} should be demo", attempt.isDemoData)
            attempt.answers.forEach { answer ->
                assertTrue("Answer in ${attempt.attemptId} should be demo", answer.isDemoData)
            }
        }
    }

    // ── QuizAttempt computed properties ───────────────────────────────────────

    @Test
    fun `QuizAttempt accuracy computed correctly`() {
        val answers = listOf(
            QuizAnswerRecord("Q1", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"), true,  listOf(), false, 1L),
            QuizAnswerRecord("Q2", QuizCategory.HEART_ATTACK, setOf("A"), setOf("B"), false, listOf(), false, 2L)
        )
        val attempt = makeAttempt(answers = answers)
        assertEquals(0.5f, attempt.accuracy, 0.01f)
        assertEquals(1, attempt.correctCount)
    }

    @Test
    fun `QuizAttempt criticalSafetyErrors count only incorrect critical answers`() {
        val answers = listOf(
            QuizAnswerRecord("Q1", QuizCategory.HEART_ATTACK, setOf("A"), setOf("B"), false, listOf(), true,  1L),
            QuizAnswerRecord("Q2", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"), true,  listOf(), false, 2L)
        )
        val attempt = makeAttempt(answers = answers)
        assertEquals(1, attempt.criticalSafetyErrors)
    }
}
