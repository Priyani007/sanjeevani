package com.sanjeevani.training

enum class TrainingModuleType(val displayName: String) {
    CPR("CPR"),
    STROKE_FAST("Stroke FAST"),
    ALLERGIC_REACTION("Allergic Reaction")
}

enum class CriterionResult { PASS, NEEDS_PRACTICE, INCONCLUSIVE, NOT_ASSESSED }

data class CriterionKey(
    val module: TrainingModuleType,
    val key: String,
    val description: String
)

data class CriterionRecord(
    val key: CriterionKey,
    val result: CriterionResult,
    val evidence: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

data class SessionOutcome(
    val sessionId: String,
    val moduleType: TrainingModuleType,
    val startMs: Long,
    val endMs: Long,
    val criteria: List<CriterionRecord>,
    val notes: String = ""
) {
    val overallResult: CriterionResult get() = when {
        criteria.isEmpty() -> CriterionResult.NOT_ASSESSED
        criteria.any { it.result == CriterionResult.NEEDS_PRACTICE } -> CriterionResult.NEEDS_PRACTICE
        criteria.all { it.result == CriterionResult.PASS } -> CriterionResult.PASS
        else -> CriterionResult.INCONCLUSIVE
    }
    val passCount: Int get() = criteria.count { it.result == CriterionResult.PASS }
    val totalCriteria: Int get() = criteria.size
    val durationSecs: Int get() = ((endMs - startMs) / 1000).toInt()
}

data class LearnerProfile(
    val moduleHistory: Map<TrainingModuleType, List<SessionOutcome>> = emptyMap()
) {
    fun lastSession(module: TrainingModuleType): SessionOutcome? =
        moduleHistory[module]?.maxByOrNull { it.endMs }

    fun sessionCount(module: TrainingModuleType): Int =
        moduleHistory[module]?.size ?: 0

    fun repeatedWeakCriteria(module: TrainingModuleType): List<CriterionKey> {
        val sessions = moduleHistory[module] ?: return emptyList()
        if (sessions.size < 2) return emptyList()
        val lastTwo = sessions.sortedByDescending { it.endMs }.take(2)
        return lastTwo[0].criteria
            .filter { it.result == CriterionResult.NEEDS_PRACTICE }
            .map { it.key }
            .filter { key -> lastTwo[1].criteria.any { it.key == key && it.result == CriterionResult.NEEDS_PRACTICE } }
    }

    fun daysSinceLastPractice(module: TrainingModuleType): Int? {
        val last = lastSession(module) ?: return null
        val diff = System.currentTimeMillis() - last.endMs
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    fun totalSessions(): Int = moduleHistory.values.sumOf { it.size }
}

data class PracticeRecommendation(
    val module: TrainingModuleType,
    val criterionKey: String?,
    val title: String,
    val rationale: String,
    val evidence: String,
    val priority: Int = 0
)

data class OutcomeComparison(
    val current: SessionOutcome,
    val previous: SessionOutcome?,
    val newlyPassed: List<CriterionKey>,
    val stillNeedsPractice: List<CriterionKey>,
    val unchangedPass: List<CriterionKey>
) {
    val hasImprovement: Boolean get() = newlyPassed.isNotEmpty()
    val passCountDelta: Int get() = current.passCount - (previous?.passCount ?: 0)
}
