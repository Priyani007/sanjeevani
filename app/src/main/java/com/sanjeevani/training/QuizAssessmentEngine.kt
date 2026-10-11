package com.sanjeevani.training

import java.util.UUID

object QuizAssessmentEngine {

    /** Evaluate a single answer submission. Returns null if already answered (duplicate guard). */
    fun evaluateAnswer(
        question: QuizQuestion,
        selectedOptionIds: Set<String>,
        existingAnswers: List<QuizAnswerRecord>,
        isDemoData: Boolean = false
    ): QuizAnswerRecord? {
        // Duplicate submission guard
        if (existingAnswers.any { it.questionId == question.id }) return null

        val isCorrect = when (question.type) {
            QuestionType.SINGLE_CHOICE -> selectedOptionIds == question.correctOptionIds
            QuestionType.MULTI_CHOICE  -> selectedOptionIds == question.correctOptionIds
        }

        return QuizAnswerRecord(
            questionId = question.id,
            category = question.category,
            selectedOptionIds = selectedOptionIds,
            correctOptionIds = question.correctOptionIds,
            isCorrect = isCorrect,
            skillTags = question.skillTags,
            isCriticalSafety = question.isCriticalSafety && !isCorrect,
            timestampMs = System.currentTimeMillis(),
            isDemoData = isDemoData
        )
    }

    /** Build a completed QuizAttempt from a finished session. */
    fun buildAttempt(session: ActiveQuizSession, isDemoData: Boolean = false): QuizAttempt =
        QuizAttempt(
            attemptId = session.attemptId,
            startMs = session.startMs,
            endMs = System.currentTimeMillis(),
            category = session.category,
            difficulty = session.difficulty,
            answers = session.answers,
            isDemoData = isDemoData
        )

    /** Score a completed attempt. */
    fun score(attempt: QuizAttempt): QuizAttemptScore {
        val total = attempt.answers.size
        val correct = attempt.correctCount
        val criticalErrors = attempt.criticalSafetyErrors
        val missedSkills = attempt.answers
            .filter { !it.isCorrect }
            .flatMap { it.skillTags }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }
        return QuizAttemptScore(
            totalQuestions = total,
            correctAnswers = correct,
            criticalSafetyErrors = criticalErrors,
            accuracy = attempt.accuracy,
            missedSkillTags = missedSkills
        )
    }

    /** Aggregate skill accuracy across multiple attempts. */
    fun skillBreakdown(attempts: List<QuizAttempt>): Map<String, SkillResult> {
        val allAnswers = attempts.flatMap { it.answers }
        val tags = allAnswers.flatMap { it.skillTags }.toSet()
        return tags.associateWith { tag ->
            val tagged = allAnswers.filter { tag in it.skillTags }
            val correct = tagged.count { it.isCorrect }
            SkillResult(
                skillTag = tag,
                attempted = tagged.size,
                correct = correct,
                accuracy = if (tagged.isEmpty()) 0f else correct.toFloat() / tagged.size,
                hasCriticalError = tagged.any { !it.isCorrect && it.isCriticalSafety }
            )
        }
    }

    /** Identify skills that have been missed two or more times without a subsequent correct answer. */
    fun unresolvedWeakSkills(attempts: List<QuizAttempt>): List<String> {
        val sortedAttempts = attempts.sortedBy { it.startMs }
        val allAnswers = sortedAttempts.flatMap { it.answers }
        val tags = allAnswers.flatMap { it.skillTags }.toSet()
        return tags.filter { tag ->
            val tagged = allAnswers.filter { tag in it.skillTags }
            // Skill is unresolved if the most recent tagged answer is incorrect
            val lastTagged = tagged.maxByOrNull { it.timestampMs }
            val missCount = tagged.count { !it.isCorrect }
            missCount >= 2 && lastTagged?.isCorrect == false
        }
    }
}

data class QuizAttemptScore(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val criticalSafetyErrors: Int,
    val accuracy: Float,
    val missedSkillTags: List<String>
)

data class SkillResult(
    val skillTag: String,
    val attempted: Int,
    val correct: Int,
    val accuracy: Float,
    val hasCriticalError: Boolean
)
