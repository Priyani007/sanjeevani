package com.sanjeevani.training

object QuizRecommendationEngine {

    fun recommend(profile: QuizLearnerProfile, demoMode: Boolean = false): List<QuizRecommendation> {
        val attempts = if (demoMode) profile.attempts else profile.realAttempts
        if (attempts.isEmpty()) return firstTimeRecommendations()

        val recs = mutableListOf<QuizRecommendation>()

        // Priority 1: Unresolved critical-safety errors
        val criticalAttempts = attempts.filter { it.criticalSafetyErrors > 0 }
        for (attempt in criticalAttempts.distinctBy { it.category }) {
            val criticalAnswers = attempt.answers.filter { !it.isCorrect && it.isCriticalSafety }
            val skillTag = criticalAnswers.firstOrNull()?.skillTags?.firstOrNull()
            recs.add(QuizRecommendation(
                targetSkillTag = skillTag,
                category = attempt.category,
                reason = QuizRecommendationReason.CRITICAL_SAFETY_ERROR,
                reasonText = "You made a critical safety error in ${attempt.category.displayName}.",
                evidence = "Critical safety questions test decisions where a wrong choice in a real situation could cause serious harm.",
                suggestedDifficulty = attempt.difficulty,
                priority = 100
            ))
        }

        // Priority 2: Repeated weak skills (missed in ≥2 separate attempts)
        val unresolvedSkills = QuizAssessmentEngine.unresolvedWeakSkills(attempts)
        for (skillTag in unresolvedSkills) {
            val category = attempts.flatMap { it.answers }
                .filter { skillTag in it.skillTags && !it.isCorrect }
                .groupBy { it.category }
                .maxByOrNull { it.value.size }?.key ?: continue
            recs.add(QuizRecommendation(
                targetSkillTag = skillTag,
                category = category,
                reason = QuizRecommendationReason.REPEATED_SKILL_MISS,
                reasonText = "You have missed \"${skillTagLabel(skillTag)}\" more than once without a correct answer.",
                evidence = "Repeated mistakes on the same skill suggest a persistent gap that targeted practice can address.",
                suggestedDifficulty = QuizDifficulty.INTERMEDIATE,
                priority = 80
            ))
        }

        // Priority 3: Recent failures not yet reassessed
        val lastAttemptByCategory = attempts.groupBy { it.category }
            .mapValues { (_, v) -> v.maxByOrNull { it.endMs }!! }
        for ((category, lastAttempt) in lastAttemptByCategory) {
            if (lastAttempt.accuracy < 0.7f) {
                val alreadyCovered = recs.any { it.category == category }
                if (!alreadyCovered) {
                    recs.add(QuizRecommendation(
                        targetSkillTag = null,
                        category = category,
                        reason = QuizRecommendationReason.RECENT_FAILURE_NOT_REASSESSED,
                        reasonText = "Your most recent ${category.displayName} quiz scored ${(lastAttempt.accuracy * 100).toInt()}%.",
                        evidence = "A score below 70% suggests this topic needs more practice.",
                        suggestedDifficulty = lastAttempt.difficulty,
                        priority = 60
                    ))
                }
            }
        }

        // Priority 4: Categories not yet practised
        val practised = attempts.map { it.category }.toSet()
        for (category in QuizCategory.values()) {
            if (category !in practised) {
                recs.add(QuizRecommendation(
                    targetSkillTag = null,
                    category = category,
                    reason = QuizRecommendationReason.NOT_YET_PRACTISED,
                    reasonText = "You haven't tried ${category.displayName} yet.",
                    evidence = "A well-rounded learner practises all three emergency categories.",
                    suggestedDifficulty = QuizDifficulty.BEGINNER,
                    priority = 40
                ))
            }
        }

        return recs.sortedByDescending { it.priority }.take(3)
    }

    private fun firstTimeRecommendations(): List<QuizRecommendation> =
        QuizCategory.values().take(3).map { category ->
            QuizRecommendation(
                targetSkillTag = null,
                category = category,
                reason = QuizRecommendationReason.NOT_YET_PRACTISED,
                reasonText = "Start with ${category.displayName} to build a baseline.",
                evidence = "No quiz attempts recorded yet.",
                suggestedDifficulty = QuizDifficulty.BEGINNER,
                priority = 40
            )
        }

    private fun skillTagLabel(tag: String): String = when (tag) {
        QuizSkillTag.EMERGENCY_ESCALATION       -> "Emergency escalation"
        QuizSkillTag.SYMPTOM_RECOGNITION        -> "Symptom recognition"
        QuizSkillTag.ATYPICAL_PRESENTATION      -> "Atypical presentation recognition"
        QuizSkillTag.CPR_DECISION               -> "CPR decision-making"
        QuizSkillTag.CPR_TECHNIQUE              -> "CPR technique"
        QuizSkillTag.AGONAL_BREATHING           -> "Agonal breathing recognition"
        QuizSkillTag.CARDIAC_ARREST_RECOGNITION -> "Cardiac arrest recognition"
        QuizSkillTag.AED_USE                    -> "AED use"
        QuizSkillTag.ANAPHYLAXIS_RECOGNITION    -> "Anaphylaxis recognition"
        QuizSkillTag.EPIPEN_TECHNIQUE           -> "EpiPen technique"
        QuizSkillTag.EPIPEN_DECISION            -> "EpiPen decision"
        QuizSkillTag.ANAPHYLAXIS_POSITION       -> "Anaphylaxis positioning"
        QuizSkillTag.BIPHASIC_REACTION          -> "Biphasic reaction awareness"
        QuizSkillTag.DANGEROUS_DELAY            -> "Avoiding dangerous delay"
        else -> tag.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
    }
}
