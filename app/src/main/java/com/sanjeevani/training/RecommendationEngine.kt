package com.sanjeevani.training

object RecommendationEngine {

    fun recommend(profile: LearnerProfile, targetModule: TrainingModuleType? = null): List<PracticeRecommendation> {
        val recs = mutableListOf<PracticeRecommendation>()
        val modules = if (targetModule != null) listOf(targetModule) else TrainingModuleType.values().toList()

        for (module in modules) {
            val sessions = profile.moduleHistory[module]

            if (sessions.isNullOrEmpty()) {
                recs.add(PracticeRecommendation(
                    module = module,
                    criterionKey = null,
                    title = "Start ${module.displayName} training",
                    rationale = "No sessions recorded yet for ${module.displayName}.",
                    evidence = "First session",
                    priority = 10
                ))
                continue
            }

            val lastSession = sessions.maxByOrNull { it.endMs } ?: continue
            val daysSince = profile.daysSinceLastPractice(module) ?: continue

            // Recommend refresher if more than 7 days since last practice
            if (daysSince >= 7) {
                recs.add(PracticeRecommendation(
                    module = module,
                    criterionKey = null,
                    title = "Refresher due: ${module.displayName}",
                    rationale = "$daysSince days since your last ${module.displayName} practice. Skills decay without regular repetition.",
                    evidence = "Frequent short practice retains skills better than infrequent long sessions (ILCOR EIT guidance)",
                    priority = 8
                ))
            }

            // Highlight criteria that failed in the last session
            val failedInLast = lastSession.criteria.filter { it.result == CriterionResult.NEEDS_PRACTICE }
            for (criterion in failedInLast.take(2)) {
                recs.add(PracticeRecommendation(
                    module = module,
                    criterionKey = criterion.key.key,
                    title = "Practice: ${criterion.key.description}",
                    rationale = "Your last ${module.displayName} session did not complete this step.",
                    evidence = criterion.evidence.take(120),
                    priority = 9
                ))
            }

            // Highlight criteria that failed in last two sessions (persistent gap)
            val repeatedWeak = profile.repeatedWeakCriteria(module)
            for (key in repeatedWeak.take(1)) {
                recs.add(PracticeRecommendation(
                    module = module,
                    criterionKey = key.key,
                    title = "Repeated gap: ${key.description}",
                    rationale = "This step was missed in your last two ${module.displayName} sessions. Focused repetition helps.",
                    evidence = "Deliberate practice on specific weak criteria improves retention",
                    priority = 10
                ))
            }
        }

        // Deduplicate by (module, criterionKey) and return top 3 by priority desc
        return recs
            .sortedByDescending { it.priority }
            .distinctBy { "${it.module.name}:${it.criterionKey ?: "general"}" }
            .take(3)
    }

    fun buildProfile(store: TrainingStore): LearnerProfile =
        LearnerProfile(store.loadAllSessions())
}
