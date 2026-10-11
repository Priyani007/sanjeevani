package com.sanjeevani.training

import java.util.UUID

// ── Stroke FAST skill tags ────────────────────────────────────────────────────

object StrokeSkillTag {
    const val FAST_RECOGNITION    = "stroke.fast_recognition"
    const val FACE_DROOPING       = "stroke.face_drooping"
    const val ARM_WEAKNESS        = "stroke.arm_weakness"
    const val SPEECH_DIFFICULTY   = "stroke.speech_difficulty"
    const val TIME_TO_ACT         = "stroke.time_to_act"
    const val SAFE_POSITIONING    = "stroke.safe_positioning"
    const val DO_NOT_GIVE_FOOD    = "stroke.do_not_give_food"
    const val TIA_RECOGNITION     = "stroke.tia_recognition"
}

// ── Allergic reaction skill tags ──────────────────────────────────────────────

object AllergySkillTag {
    const val ANAPHYLAXIS_RECOGNITION = "allergy.anaphylaxis_recognition"
    const val CALL_112               = "allergy.call_112"
    const val LAY_FLAT               = "allergy.lay_flat"
    const val EPIPEN_LOCATION        = "allergy.epipen_location"
    const val EPIPEN_TECHNIQUE       = "allergy.epipen_technique"
    const val POST_INJECTION         = "allergy.post_injection"
    const val MONITORING             = "allergy.monitoring"
}

enum class AssessmentMethod { BUTTON, VOICE, SIMULATED_PLACEMENT, CAMERA_MEDIAPIPE, MANUAL }
enum class ObservationQuality { NOT_APPLICABLE, HIGH, MODERATE, LOW }
enum class CorrectionStatus { NOT_REQUIRED, RETRY_REQUIRED, CORRECTED, UNRESOLVED }
enum class PlacementResult {
    CORRECT, TOO_HIGH, TOO_LOW, TOO_MEDIAL, TOO_LATERAL, INCONCLUSIVE, NOT_ASSESSED
}

const val MODULE_CONTENT_VERSION = "2026.10"

// ── A single skill attempt within a module training session ───────────────────

data class ModuleTrainingAttempt(
    val attemptId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val moduleType: String,              // TrainingModuleType.name
    val skillTag: String,
    val skillDescription: String,
    val stepIndex: Int,
    val questionId: String,
    val selectedOptionId: String?,
    val correctOptionId: String,
    val knowledgeResult: KnowledgeResult,
    val outcome: SkillAttemptOutcome,
    val feedbackShown: String,
    val assessmentMethod: AssessmentMethod = AssessmentMethod.BUTTON,
    val learnerResponse: String? = selectedOptionId,
    val errorCategory: String? = null,
    val confidence: Float? = null,
    val observationQuality: ObservationQuality = ObservationQuality.NOT_APPLICABLE,
    val retryCount: Int = 0,
    val correctionStatus: CorrectionStatus = CorrectionStatus.NOT_REQUIRED,
    val placementResult: PlacementResult = PlacementResult.NOT_ASSESSED,
    val eventType: String = "assessment",
    val observationType: String = "knowledge_response",
    val observedMeasurements: String? = null,
    val correctionDetected: Boolean = correctionStatus == CorrectionStatus.CORRECTED,
    val manuallyAdvanced: Boolean = false,
    val trackingModelVersion: String? = null,
    val contentVersion: String = MODULE_CONTENT_VERSION,
    val dataProvenance: String = "learner_response",
    val timestampMs: Long = System.currentTimeMillis()
)

// ── A complete module training session ────────────────────────────────────────

data class ModuleTrainingSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val moduleType: String,
    val startMs: Long = System.currentTimeMillis(),
    val endMs: Long = 0L,
    val attempts: List<ModuleTrainingAttempt> = emptyList(),
    val isDemoData: Boolean = false,
    val focusedSkillTag: String? = null,
    val contentVersion: String = MODULE_CONTENT_VERSION
) {
    val isComplete: Boolean get() = endMs > 0L
    val totalAttempts: Int get() = attempts.size
    val correctAttempts: Int get() = attempts.count { it.outcome == SkillAttemptOutcome.CORRECT }
    val accuracy: Float get() = if (attempts.isEmpty()) 0f else correctAttempts.toFloat() / attempts.size

    fun attemptsFor(skillTag: String): List<ModuleTrainingAttempt> =
        attempts.filter { it.skillTag == skillTag }

    fun outcomeFor(skillTag: String): SkillAttemptOutcome? =
        attemptsFor(skillTag).maxByOrNull { it.timestampMs }?.outcome
}

// ── Aggregated skill stats across sessions ────────────────────────────────────

data class ModuleSkillStats(
    val skillTag: String,
    val skillDescription: String,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val incorrectAttempts: Int,
    val inconclusiveAttempts: Int,
    val manualAdvances: Int,
    val correctedAttempts: Int,
    val accuracy: Float,
    val latestOutcome: SkillAttemptOutcome?,
    val hasImprovedInLatestSession: Boolean
)

// ── Module training learner profile ──────────────────────────────────────────

data class ModuleTrainingProfile(
    val moduleType: String,
    val sessions: List<ModuleTrainingSession> = emptyList()
) {
    val realSessions: List<ModuleTrainingSession> get() = sessions.filter { !it.isDemoData }
    val demoSessions: List<ModuleTrainingSession> get() = sessions.filter { it.isDemoData }

    fun sessionsToUse(demoMode: Boolean) = if (demoMode) demoSessions else realSessions

    fun skillStats(demoMode: Boolean = false): List<ModuleSkillStats> {
        val useSessions = sessionsToUse(demoMode)
        val allAttempts = useSessions.flatMap { it.attempts }
        val tags = allAttempts.map { it.skillTag }.distinct()
        return tags.map { tag ->
            val tagAttempts = allAttempts.filter { it.skillTag == tag }
            val scoredAttempts = tagAttempts.filter {
                it.outcome == SkillAttemptOutcome.CORRECT ||
                    it.outcome == SkillAttemptOutcome.INCORRECT ||
                    it.outcome == SkillAttemptOutcome.NEEDS_PRACTICE
            }
            val orderedTagAttempts = tagAttempts.sortedByDescending { it.timestampMs }
            val latestOutcome = orderedTagAttempts.firstOrNull()?.outcome
            val prevOutcome = orderedTagAttempts.drop(1).firstOrNull()?.outcome
            val improved = latestOutcome == SkillAttemptOutcome.CORRECT &&
                prevOutcome != null && prevOutcome != SkillAttemptOutcome.CORRECT

            ModuleSkillStats(
                skillTag         = tag,
                skillDescription = tagAttempts.firstOrNull()?.skillDescription ?: tag,
                totalAttempts    = tagAttempts.size,
                correctAttempts  = tagAttempts.count { it.outcome == SkillAttemptOutcome.CORRECT },
                incorrectAttempts = tagAttempts.count {
                    it.outcome == SkillAttemptOutcome.INCORRECT || it.outcome == SkillAttemptOutcome.NEEDS_PRACTICE
                },
                inconclusiveAttempts = tagAttempts.count { it.outcome == SkillAttemptOutcome.INCONCLUSIVE },
                manualAdvances = tagAttempts.count { it.manuallyAdvanced },
                correctedAttempts = tagAttempts.count { it.correctionDetected },
                accuracy         = if (scoredAttempts.isEmpty()) 0f else
                    scoredAttempts.count { it.outcome == SkillAttemptOutcome.CORRECT }.toFloat() / scoredAttempts.size,
                latestOutcome    = latestOutcome,
                hasImprovedInLatestSession = improved
            )
        }
    }

    fun weakSkills(demoMode: Boolean = false): List<String> =
        skillStats(demoMode)
            .filter {
                it.latestOutcome == SkillAttemptOutcome.INCORRECT ||
                    it.latestOutcome == SkillAttemptOutcome.NEEDS_PRACTICE ||
                    it.latestOutcome == SkillAttemptOutcome.INCONCLUSIVE
            }
            .sortedBy { it.accuracy }
            .map { it.skillTag }

    fun totalSessionCount(demoMode: Boolean = false) = sessionsToUse(demoMode).count { it.isComplete }
}

// ── Focused practice recommendation for a module ─────────────────────────────

data class ModuleFocusedRecommendation(
    val skillTag: String,
    val skillDescription: String,
    val reason: String,
    val evidence: String,
    val priority: Int,
    val stepIndex: Int,
    val isResolved: Boolean = false
)
