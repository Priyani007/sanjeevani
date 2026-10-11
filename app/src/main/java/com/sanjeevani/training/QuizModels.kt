package com.sanjeevani.training

import java.util.UUID

// ── Enums ────────────────────────────────────────────────────────────────────

enum class QuizCategory(val displayName: String, val emoji: String) {
    HEART_ATTACK("Heart Attack", "❤️‍🔥"),
    CPR_CARDIAC_ARREST("CPR & Cardiac Arrest", "🫀"),
    ALLERGIC_REACTION("Allergic Reactions", "💉")
}

enum class QuizDifficulty(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced")
}

enum class QuestionType { SINGLE_CHOICE, MULTI_CHOICE }

// ── Skill tags (used for mistake classification & recommendations) ─────────────

object QuizSkillTag {
    const val EMERGENCY_ESCALATION       = "emergency_escalation"
    const val SYMPTOM_RECOGNITION        = "symptom_recognition"
    const val ATYPICAL_PRESENTATION      = "atypical_presentation"
    const val CPR_DECISION               = "cpr_decision"
    const val CPR_TECHNIQUE              = "cpr_technique"
    const val AGONAL_BREATHING           = "agonal_breathing"
    const val CARDIAC_ARREST_RECOGNITION = "cardiac_arrest_recognition"
    const val AED_USE                    = "aed_use"
    const val HANDOVER                   = "handover"
    const val CPR_QUALITY                = "cpr_quality"
    const val ANAPHYLAXIS_RECOGNITION    = "anaphylaxis_recognition"
    const val EPIPEN_TECHNIQUE           = "epipen_technique"
    const val EPIPEN_DECISION            = "epipen_decision"
    const val ANAPHYLAXIS_POSITION       = "anaphylaxis_position"
    const val BIPHASIC_REACTION          = "biphasic_reaction"
    const val APPROPRIATE_ACTION         = "appropriate_action"
    const val MONITORING                 = "monitoring"
    const val DANGEROUS_DELAY            = "dangerous_delay"
}

// ── Question models ───────────────────────────────────────────────────────────

data class QuizOption(
    val id: String,          // "A", "B", "C", "D"
    val text: String,
    val explanation: String, // shown in feedback regardless of correctness
    val isSafeChoice: Boolean = true   // false = choosing this is an unsafe action
)

data class QuizQuestion(
    val id: String,
    val category: QuizCategory,
    val difficulty: QuizDifficulty,
    val type: QuestionType,
    val scenario: String,
    val questionText: String,
    val options: List<QuizOption>,
    val correctOptionIds: Set<String>,
    val skillTags: List<String>,
    val misconceptionTested: String,
    val learningObjective: String,
    val isCriticalSafety: Boolean = false,
    val contentVersion: String = "v1.0-2025"
)

// ── Attempt and answer records ────────────────────────────────────────────────

data class QuizAnswerRecord(
    val questionId: String,
    val category: QuizCategory,
    val selectedOptionIds: Set<String>,
    val correctOptionIds: Set<String>,
    val isCorrect: Boolean,
    val skillTags: List<String>,
    val isCriticalSafety: Boolean,
    val timestampMs: Long,
    val isDemoData: Boolean = false
)

data class QuizAttempt(
    val attemptId: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val category: QuizCategory,
    val difficulty: QuizDifficulty,
    val answers: List<QuizAnswerRecord>,
    val isDemoData: Boolean = false
) {
    val totalQuestions: Int get() = answers.size
    val correctCount: Int get() = answers.count { it.isCorrect }
    val criticalSafetyErrors: Int get() = answers.count { !it.isCorrect && it.isCriticalSafety }
    val accuracy: Float get() = if (answers.isEmpty()) 0f else correctCount.toFloat() / answers.size
}

// ── Quiz session state (in-memory, drives UI) ─────────────────────────────────

data class ActiveQuizSession(
    val attemptId: String = UUID.randomUUID().toString(),
    val category: QuizCategory,
    val difficulty: QuizDifficulty,
    val questions: List<QuizQuestion>,
    val currentIndex: Int = 0,
    val answers: List<QuizAnswerRecord> = emptyList(),
    val selectedOptionIds: Set<String> = emptySet(),
    val isAnswerLocked: Boolean = false,
    val startMs: Long = System.currentTimeMillis()
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= questions.size
    val progress: Float get() = if (questions.isEmpty()) 0f else (currentIndex.toFloat() / questions.size)
}

// ── Analytics profile ─────────────────────────────────────────────────────────

data class QuizLearnerProfile(
    val attempts: List<QuizAttempt> = emptyList()
) {
    val realAttempts: List<QuizAttempt> get() = attempts.filter { !it.isDemoData }
    val demoAttempts: List<QuizAttempt> get() = attempts.filter { it.isDemoData }

    fun totalAttempts(demoMode: Boolean = false): Int =
        if (demoMode) attempts.size else realAttempts.size

    fun totalQuestionsAnswered(demoMode: Boolean = false): Int =
        (if (demoMode) attempts else realAttempts).sumOf { it.answers.size }

    fun overallAccuracy(demoMode: Boolean = false): Float {
        val relevant = if (demoMode) attempts else realAttempts
        val total = relevant.sumOf { it.answers.size }
        val correct = relevant.sumOf { it.correctCount }
        return if (total == 0) 0f else correct.toFloat() / total
    }

    fun categoryAccuracy(category: QuizCategory, demoMode: Boolean = false): Float {
        val relevant = (if (demoMode) attempts else realAttempts).filter { it.category == category }
        val answers = relevant.flatMap { it.answers }
        return if (answers.isEmpty()) 0f else answers.count { it.isCorrect }.toFloat() / answers.size
    }

    fun skillAccuracy(skillTag: String, demoMode: Boolean = false): Float {
        val answers = (if (demoMode) attempts else realAttempts)
            .flatMap { it.answers }
            .filter { skillTag in it.skillTags }
        return if (answers.isEmpty()) 0f else answers.count { it.isCorrect }.toFloat() / answers.size
    }

    fun repeatedMissCount(skillTag: String, demoMode: Boolean = false): Int =
        (if (demoMode) attempts else realAttempts)
            .flatMap { it.answers }
            .count { !it.isCorrect && skillTag in it.skillTags }

    fun hasUnresolvedCriticalSafetyError(demoMode: Boolean = false): Boolean =
        (if (demoMode) attempts else realAttempts).any { it.criticalSafetyErrors > 0 }

    fun categoriesPractised(demoMode: Boolean = false): Set<QuizCategory> =
        (if (demoMode) attempts else realAttempts).map { it.category }.toSet()

    fun lastActivityMs(demoMode: Boolean = false): Long? =
        (if (demoMode) attempts else realAttempts).maxOfOrNull { it.endMs }

    fun attemptsForCategory(category: QuizCategory, demoMode: Boolean = false): List<QuizAttempt> =
        (if (demoMode) attempts else realAttempts).filter { it.category == category }
            .sortedBy { it.startMs }
}

// ── Recommendation model ──────────────────────────────────────────────────────

enum class QuizRecommendationReason {
    CRITICAL_SAFETY_ERROR,
    REPEATED_SKILL_MISS,
    RECENT_FAILURE_NOT_REASSESSED,
    LOW_ACCURACY,
    NOT_YET_PRACTISED
}

data class QuizRecommendation(
    val recommendationId: String = UUID.randomUUID().toString(),
    val targetSkillTag: String?,
    val category: QuizCategory,
    val reason: QuizRecommendationReason,
    val reasonText: String,
    val evidence: String,
    val suggestedDifficulty: QuizDifficulty,
    val priority: Int,
    val createdMs: Long = System.currentTimeMillis()
)
