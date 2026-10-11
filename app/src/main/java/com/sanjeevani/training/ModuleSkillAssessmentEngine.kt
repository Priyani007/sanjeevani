package com.sanjeevani.training

/**
 * Maps knowledge answers to ModuleTrainingAttempt outcomes for Stroke and Allergy training.
 * These modules are knowledge-only — no camera assessment.
 */
object ModuleSkillAssessmentEngine {

    fun evaluate(
        step: ModuleTrainingStep,
        sessionId: String,
        selectedOptionId: String,
        method: AssessmentMethod = AssessmentMethod.BUTTON,
        learnerResponse: String? = selectedOptionId,
        confidence: Float? = null,
        retryCount: Int = 0
    ): ModuleTrainingAttempt {
        val correctId       = step.correctOption.id
        val selected        = step.options.firstOrNull { it.id == selectedOptionId }
        val isCorrect       = selected != null && selectedOptionId == correctId
        val knowledgeResult = when {
            selected == null -> KnowledgeResult.NOT_ATTEMPTED
            isCorrect -> KnowledgeResult.CORRECT
            else -> KnowledgeResult.INCORRECT
        }

        val outcome = if (selected == null) SkillAttemptOutcome.INCONCLUSIVE else if (isCorrect) SkillAttemptOutcome.CORRECT else {
            if (selected?.isSafe == false) SkillAttemptOutcome.INCORRECT
            else SkillAttemptOutcome.NEEDS_PRACTICE
        }

        return ModuleTrainingAttempt(
            sessionId        = sessionId,
            moduleType       = step.moduleType,
            skillTag         = step.skillTag,
            skillDescription = step.skillDescription,
            stepIndex        = step.stepIndex,
            questionId       = "${step.skillTag}.step${step.stepIndex}",
            selectedOptionId = selectedOptionId,
            correctOptionId  = correctId,
            knowledgeResult  = knowledgeResult,
            outcome          = outcome,
            feedbackShown    = selected?.feedback ?: "I could not match that response. Say option A, B, C, or D, or tap an option.",
            assessmentMethod = method,
            learnerResponse  = learnerResponse,
            errorCategory    = when {
                selected == null -> "unrecognised_response"
                isCorrect -> null
                selected.isSafe -> "knowledge_gap"
                else -> "unsafe_choice"
            },
            confidence       = confidence,
            observationQuality = when {
                method != AssessmentMethod.VOICE -> ObservationQuality.NOT_APPLICABLE
                confidence == null -> ObservationQuality.LOW
                confidence >= 0.8f -> ObservationQuality.HIGH
                confidence >= 0.55f -> ObservationQuality.MODERATE
                else -> ObservationQuality.LOW
            },
            retryCount       = retryCount,
            correctionStatus = when {
                isCorrect && retryCount > 0 -> CorrectionStatus.CORRECTED
                isCorrect -> CorrectionStatus.NOT_REQUIRED
                else -> CorrectionStatus.RETRY_REQUIRED
            }
        )
    }

    fun recommend(profile: ModuleTrainingProfile, demoMode: Boolean = false): List<ModuleFocusedRecommendation> {
        val stats = profile.skillStats(demoMode)
        val weak = stats.filter {
            it.latestOutcome == SkillAttemptOutcome.INCORRECT ||
                it.latestOutcome == SkillAttemptOutcome.NEEDS_PRACTICE ||
                it.latestOutcome == SkillAttemptOutcome.INCONCLUSIVE
        }.sortedBy { it.accuracy }

        val content = when (profile.moduleType) {
            "STROKE_FAST" -> StrokeTrainingContent.steps
            "ALLERGIC_REACTION" -> AllergyTrainingContent.steps
            else -> emptyList()
        }
        if (stats.isEmpty()) {
            val first = content.firstOrNull() ?: return emptyList()
            return listOf(
                ModuleFocusedRecommendation(
                    skillTag = first.skillTag,
                    skillDescription = first.skillDescription,
                    reason = "Start with this foundation skill before focused reassessment.",
                    evidence = "No real learner attempts have been recorded yet.",
                    priority = 40,
                    stepIndex = first.stepIndex
                )
            )
        }

        return weak.take(3).map { s ->
            val pct = (s.accuracy * 100).toInt()
            ModuleFocusedRecommendation(
                skillTag         = s.skillTag,
                skillDescription = s.skillDescription,
                reason           = when {
                    s.latestOutcome == SkillAttemptOutcome.INCONCLUSIVE -> "This skill was left unconfirmed or needs a clearer supported observation."
                    pct == 0 -> "This skill has not been answered correctly yet."
                    else     -> "Accuracy on '${s.skillDescription}' is $pct% — review the scenario."
                },
                evidence = "Attempted ${s.totalAttempts} time(s); correct ${s.correctAttempts}.",
                priority = when {
                    s.accuracy == 0f && s.totalAttempts > 0 -> 90
                    s.accuracy < 0.5f                       -> 70
                    else                                    -> 50
                },
                stepIndex = content.firstOrNull { it.skillTag == s.skillTag }?.stepIndex ?: 0
            )
        }.sortedByDescending { it.priority }
    }
}

object ModuleVoiceInterpreter {
    fun resolve(transcript: String, options: List<ModuleTrainingOption>): String? {
        val normalized = transcript.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val labels = mapOf(
            "A" to listOf("a", "option a", "answer a", "first option", "option one"),
            "B" to listOf("b", "option b", "answer b", "second option", "option two"),
            "C" to listOf("c", "option c", "answer c", "third option", "option three"),
            "D" to listOf("d", "option d", "answer d", "fourth option", "option four")
        )
        labels.forEach { (id, phrases) ->
            if (options.any { it.id == id } && phrases.any { phrase ->
                    normalized == phrase || (phrase.length > 1 && Regex("(^|\\s)${Regex.escape(phrase)}($|\\s)").containsMatchIn(normalized))
                }) return id
        }
        return null
    }
}

object AllergyPlacementAssessment {
    fun evaluate(x: Float, y: Float, confidence: Float = 1f): PlacementResult = when {
        confidence < 0.65f -> PlacementResult.INCONCLUSIVE
        y < 0.38f -> PlacementResult.TOO_HIGH
        y > 0.68f -> PlacementResult.TOO_LOW
        x < 0.62f -> PlacementResult.TOO_MEDIAL
        x > 0.88f -> PlacementResult.TOO_LATERAL
        else -> PlacementResult.CORRECT
    }

    fun feedback(result: PlacementResult): String = when (result) {
        PlacementResult.CORRECT -> "Correct: the trainer pen is on the outer middle thigh. Follow the specific device label in a real emergency."
        PlacementResult.TOO_HIGH -> "Move lower, toward the middle third of the thigh."
        PlacementResult.TOO_LOW -> "Move higher, toward the middle third of the thigh."
        PlacementResult.TOO_MEDIAL -> "Move outward, away from the inner thigh."
        PlacementResult.TOO_LATERAL -> "Move slightly inward onto the broad outer thigh muscle."
        PlacementResult.INCONCLUSIVE -> "Placement could not be assessed confidently. Try again on the diagram."
        PlacementResult.NOT_ASSESSED -> "Tap the diagram to practise trainer-device placement."
    }
}
