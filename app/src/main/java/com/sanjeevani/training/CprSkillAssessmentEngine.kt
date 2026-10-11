package com.sanjeevani.training

import com.sanjeevani.model.SpatialAction

/**
 * Maps knowledge answers and camera observations to CprSkillAttempt outcomes.
 *
 * SpatialAction semantics (from SpatialReasoner.kt):
 *   CORRECT         → hand is on the lower-half sternum target
 *   MOVE_DOWN       → hand is too high on the body (toward shoulders)  → TOO_HIGH
 *   MOVE_UP         → hand is too low on the body (toward abdomen)     → TOO_LOW
 *   MOVE_LEFT/RIGHT → lateral error
 *   TRACKING_LOST   → unable to assess
 *   UNSURE          → inconclusive
 */
object CprSkillAssessmentEngine {

    /**
     * Build a CprSkillAttempt from:
     *  - the training step definition
     *  - the learner's selected option id
     *  - the latest SpatialAction from the camera (null = no camera step)
     */
    fun evaluate(
        step: CprTrainingStep,
        sessionId: String,
        selectedOptionId: String,
        spatialAction: SpatialAction?
    ): CprSkillAttempt {
        val correctId   = step.correctOption.id
        val selected    = step.options.firstOrNull { it.id == selectedOptionId }
        val isCorrect   = selectedOptionId == correctId
        val knowledgeResult = if (isCorrect) KnowledgeResult.CORRECT else KnowledgeResult.INCORRECT

        val (handResult, camNote) = if (step.useCameraForAssessment && spatialAction != null) {
            spatialActionToPlacement(spatialAction)
        } else {
            HandPlacementResult.NOT_OBSERVED to ""
        }

        val outcome = computeOutcome(
            knowledgeResult = knowledgeResult,
            handResult      = handResult,
            usesCamera      = step.useCameraForAssessment
        )

        val feedbackText = buildFeedback(
            selected   = selected,
            isCorrect  = isCorrect,
            handResult = handResult,
            camNote    = camNote,
            usesCamera = step.useCameraForAssessment
        )

        return CprSkillAttempt(
            sessionId             = sessionId,
            skillTag              = step.skillTag,
            skillDescription      = step.skillDescription,
            stepIndex             = step.stepIndex,
            questionId            = "${step.skillTag}.step${step.stepIndex}",
            selectedOptionId      = selectedOptionId,
            correctOptionId       = correctId,
            knowledgeResult       = knowledgeResult,
            handPlacementResult   = handResult,
            cameraObservationNote = camNote,
            outcome               = outcome,
            feedbackShown         = feedbackText
        )
    }

    private fun spatialActionToPlacement(action: SpatialAction): Pair<HandPlacementResult, String> =
        when (action) {
            SpatialAction.CORRECT      -> HandPlacementResult.CORRECT      to "Camera: hands appear over sternum target area."
            SpatialAction.MOVE_DOWN    -> HandPlacementResult.TOO_HIGH      to "Camera: hands appear above the sternum target (too high toward shoulders). Try moving down."
            SpatialAction.MOVE_UP      -> HandPlacementResult.TOO_LOW       to "Camera: hands appear below the sternum target (too low toward abdomen). Try moving up."
            SpatialAction.MOVE_LEFT    -> HandPlacementResult.TOO_FAR_LEFT  to "Camera: hands appear to the left of centre. Try moving right."
            SpatialAction.MOVE_RIGHT   -> HandPlacementResult.TOO_FAR_RIGHT to "Camera: hands appear to the right of centre. Try moving left."
            SpatialAction.TRACKING_LOST -> HandPlacementResult.UNABLE_TO_ASSESS to "Camera: unable to detect hands clearly. Ensure your hands are visible and well-lit."
            SpatialAction.UNSURE       -> HandPlacementResult.INCONCLUSIVE  to "Camera: observation inconclusive. Consider practising on a manikin for physical feedback."
        }

    private fun computeOutcome(
        knowledgeResult: KnowledgeResult,
        handResult: HandPlacementResult,
        usesCamera: Boolean
    ): SkillAttemptOutcome {
        if (knowledgeResult == KnowledgeResult.INCORRECT) return SkillAttemptOutcome.INCORRECT

        if (!usesCamera || handResult == HandPlacementResult.NOT_OBSERVED) {
            return SkillAttemptOutcome.CORRECT
        }

        return when (handResult) {
            HandPlacementResult.CORRECT            -> SkillAttemptOutcome.CORRECT
            HandPlacementResult.UNABLE_TO_ASSESS   -> SkillAttemptOutcome.INCONCLUSIVE
            HandPlacementResult.INCONCLUSIVE       -> SkillAttemptOutcome.INCONCLUSIVE
            else                                   -> SkillAttemptOutcome.NEEDS_PRACTICE
        }
    }

    private fun buildFeedback(
        selected: CprTrainingOption?,
        isCorrect: Boolean,
        handResult: HandPlacementResult,
        camNote: String,
        usesCamera: Boolean
    ): String {
        val base = selected?.feedback ?: (if (isCorrect) "Correct." else "Incorrect.")
        return if (usesCamera && camNote.isNotEmpty()) "$base\n\n$camNote" else base
    }

    /**
     * Produce a CprFocusedRecommendation for each weak skill in the profile.
     */
    fun recommend(profile: CprTrainingProfile, demoMode: Boolean = false): List<CprFocusedRecommendation> {
        val stats  = profile.skillStats(demoMode)
        val weak   = stats.filter {
            it.latestOutcome == SkillAttemptOutcome.INCORRECT ||
            it.latestOutcome == SkillAttemptOutcome.NEEDS_PRACTICE
        }.sortedBy { it.accuracy }

        return weak.take(3).map { s ->
            val reason = buildRecommendationReason(s)
            val evidence = buildEvidence(s)
            CprFocusedRecommendation(
                skillTag        = s.skillTag,
                skillDescription = s.skillDescription,
                reason          = reason,
                evidence        = evidence,
                priority        = if (s.accuracy == 0f && s.totalAttempts > 0) 90
                                  else if (s.accuracy < 0.5f) 70
                                  else 50
            )
        }.sortedByDescending { it.priority }
    }

    private fun buildRecommendationReason(stats: CprSkillStats): String {
        val pct = (stats.accuracy * 100).toInt()
        return when {
            stats.mostFrequentPlacementError != null && stats.mostFrequentPlacementError != HandPlacementResult.CORRECT ->
                "Hand placement needs work: ${stats.mostFrequentPlacementError.name.replace('_', ' ').lowercase()} observed most often."
            pct == 0 -> "This skill has not been answered correctly yet."
            else -> "Accuracy on '${stats.skillDescription}' is $pct% — review the technique."
        }
    }

    private fun buildEvidence(stats: CprSkillStats): String =
        "Attempted ${stats.totalAttempts} time(s); correct ${stats.correctAttempts}. " +
            "Source: RCUK BLS 2021 / ERC Guidelines 2021."
}
