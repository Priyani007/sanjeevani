package com.sanjeevani.training

import java.util.UUID

// ── CPR training skill tags (stable identifiers) ──────────────────────────────

object CprSkillTag {
    const val SCENE_SAFETY          = "cpr.scene_safety"
    const val ARREST_RECOGNITION    = "cpr.arrest_recognition"
    const val EMERGENCY_ESCALATION  = "cpr.emergency_escalation"
    const val PATIENT_POSITIONING   = "cpr.patient_positioning"
    const val HAND_PLACEMENT_REGION = "cpr.hand_placement_region"
    const val BODY_POSITION         = "cpr.body_position"
    const val COMPRESSION_RATE      = "cpr.compression_rate"
}

// ── Hand placement assessment categories ─────────────────────────────────────

enum class HandPlacementResult {
    CORRECT,               // spatial reasoner reports CORRECT
    TOO_HIGH,              // hand above sternum target (MOVE_DOWN instruction)
    TOO_LOW,               // hand below sternum target (MOVE_UP instruction)
    TOO_FAR_LEFT,          // lateral error left
    TOO_FAR_RIGHT,         // lateral error right
    UNABLE_TO_ASSESS,      // tracking lost or insufficient confidence
    INCONCLUSIVE,          // UNSURE from spatial reasoner
    NOT_OBSERVED           // camera not used for this attempt (knowledge-only)
}

// ── Knowledge-based answer result ─────────────────────────────────────────────

enum class KnowledgeResult { CORRECT, INCORRECT, NOT_ATTEMPTED }

// ── Combined skill attempt outcome ────────────────────────────────────────────

enum class SkillAttemptOutcome {
    CORRECT,          // knowledge correct (and placement correct if applicable)
    INCORRECT,        // knowledge incorrect or placement incorrect
    NEEDS_PRACTICE,   // answered but showed a misconception
    INCONCLUSIVE,     // camera-only step with insufficient tracking
    NOT_ASSESSED      // step was skipped
}

// ── A single skill attempt within a CPR training session ─────────────────────

data class CprSkillAttempt(
    val attemptId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val skillTag: String,
    val skillDescription: String,
    val stepIndex: Int,
    // Knowledge question fields
    val questionId: String,
    val selectedOptionId: String?,
    val correctOptionId: String,
    val knowledgeResult: KnowledgeResult,
    // Camera observation fields (may be NOT_OBSERVED for pure knowledge steps)
    val handPlacementResult: HandPlacementResult = HandPlacementResult.NOT_OBSERVED,
    val cameraObservationNote: String = "",
    // Overall outcome
    val outcome: SkillAttemptOutcome,
    val feedbackShown: String,
    val learnerCorrected: Boolean = false,
    val reassessmentOutcome: SkillAttemptOutcome? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val protocolVersion: String = "RCUK-BLS-2021"
)

// ── A complete CPR training session ──────────────────────────────────────────

data class CprSkillSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val startMs: Long = System.currentTimeMillis(),
    val endMs: Long = 0L,
    val attempts: List<CprSkillAttempt> = emptyList(),
    val isDemoData: Boolean = false
) {
    val isComplete: Boolean get() = endMs > 0L
    val totalAttempts: Int get() = attempts.size
    val correctAttempts: Int get() = attempts.count { it.outcome == SkillAttemptOutcome.CORRECT }
    val accuracy: Float get() = if (attempts.isEmpty()) 0f else correctAttempts.toFloat() / attempts.size

    fun attemptsFor(skillTag: String): List<CprSkillAttempt> =
        attempts.filter { it.skillTag == skillTag }

    fun outcomeFor(skillTag: String): SkillAttemptOutcome? =
        attemptsFor(skillTag).maxByOrNull { it.timestampMs }?.outcome
}

// ── Aggregated skill stats across sessions ────────────────────────────────────

data class CprSkillStats(
    val skillTag: String,
    val skillDescription: String,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val incorrectAttempts: Int,
    val inconclusiveAttempts: Int,
    val accuracy: Float,
    val mostFrequentPlacementError: HandPlacementResult?,
    val latestOutcome: SkillAttemptOutcome?,
    val hasImprovedInLatestSession: Boolean
)

// ── CPR training learner profile ──────────────────────────────────────────────

data class CprTrainingProfile(
    val sessions: List<CprSkillSession> = emptyList()
) {
    val realSessions: List<CprSkillSession> get() = sessions.filter { !it.isDemoData }
    val demoSessions: List<CprSkillSession> get() = sessions.filter { it.isDemoData }

    fun sessionsToUse(demoMode: Boolean) = if (demoMode) sessions else realSessions

    fun skillStats(demoMode: Boolean = false): List<CprSkillStats> {
        val useSessions = sessionsToUse(demoMode)
        val allAttempts = useSessions.flatMap { it.attempts }
        val tags = allAttempts.map { it.skillTag }.distinct()
        return tags.map { tag ->
            val tagAttempts = allAttempts.filter { it.skillTag == tag }
            val latestSession = useSessions.maxByOrNull { it.startMs }
            val prevSession = useSessions.sortedByDescending { it.startMs }.drop(1).firstOrNull()
            val latestOutcome = latestSession?.outcomeFor(tag)
            val prevOutcome = prevSession?.outcomeFor(tag)
            val improved = latestOutcome == SkillAttemptOutcome.CORRECT &&
                prevOutcome != null && prevOutcome != SkillAttemptOutcome.CORRECT

            val placementAttempts = tagAttempts.filter {
                it.handPlacementResult != HandPlacementResult.NOT_OBSERVED &&
                    it.handPlacementResult != HandPlacementResult.UNABLE_TO_ASSESS
            }
            val mostFrequentError = if (placementAttempts.isNotEmpty())
                placementAttempts
                    .filter { it.handPlacementResult != HandPlacementResult.CORRECT }
                    .groupingBy { it.handPlacementResult }
                    .eachCount()
                    .maxByOrNull { it.value }?.key
            else null

            CprSkillStats(
                skillTag          = tag,
                skillDescription  = tagAttempts.firstOrNull()?.skillDescription ?: tag,
                totalAttempts     = tagAttempts.size,
                correctAttempts   = tagAttempts.count { it.outcome == SkillAttemptOutcome.CORRECT },
                incorrectAttempts = tagAttempts.count { it.outcome == SkillAttemptOutcome.INCORRECT || it.outcome == SkillAttemptOutcome.NEEDS_PRACTICE },
                inconclusiveAttempts = tagAttempts.count { it.outcome == SkillAttemptOutcome.INCONCLUSIVE },
                accuracy          = if (tagAttempts.isEmpty()) 0f else
                    tagAttempts.count { it.outcome == SkillAttemptOutcome.CORRECT }.toFloat() / tagAttempts.size,
                mostFrequentPlacementError = mostFrequentError,
                latestOutcome     = latestOutcome,
                hasImprovedInLatestSession = improved
            )
        }
    }

    fun weakSkills(demoMode: Boolean = false): List<String> =
        skillStats(demoMode)
            .filter { it.latestOutcome == SkillAttemptOutcome.INCORRECT ||
                it.latestOutcome == SkillAttemptOutcome.NEEDS_PRACTICE }
            .sortedBy { it.accuracy }
            .map { it.skillTag }

    fun totalSessionCount(demoMode: Boolean = false) = sessionsToUse(demoMode).size
}

// ── Recommendation for focused CPR practice ───────────────────────────────────

data class CprFocusedRecommendation(
    val skillTag: String,
    val skillDescription: String,
    val reason: String,
    val evidence: String,
    val priority: Int   // higher = more urgent
)
