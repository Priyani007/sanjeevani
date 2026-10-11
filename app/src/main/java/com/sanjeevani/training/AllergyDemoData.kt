package com.sanjeevani.training

/**
 * Deterministic demo data for Allergic Reaction training.
 * 3 sessions showing progressive improvement:
 *  Session 1 (day -12): misses anaphylaxis severity, wrong positioning, wrong technique
 *  Session 2 (day -6):  recognition and call correct; positioning and technique still weak
 *  Session 3 (day -2):  all correct
 */
object AllergyDemoData {

    private const val MODULE = "ALLERGIC_REACTION"

    fun generateSessions(): List<ModuleTrainingSession> {
        val now   = System.currentTimeMillis()
        val day   = 86_400_000L
        val steps = AllergyTrainingContent.steps

        fun attempt(
            sessionId: String,
            step: ModuleTrainingStep,
            chooseCorrect: Boolean
        ): ModuleTrainingAttempt {
            val optId = if (chooseCorrect) step.correctOption.id
                        else step.options.first { !it.isCorrect }.id
            return ModuleSkillAssessmentEngine.evaluate(step, sessionId, optId)
                .copy(timestampMs = now - (steps.size - step.stepIndex) * 60_000L)
        }

        // ── Session 1: misses severity, wrong positioning, wrong technique ────
        val sid1 = "allergy_demo_session_1"
        val session1 = ModuleTrainingSession(
            sessionId  = sid1,
            moduleType = MODULE,
            startMs    = now - 12 * day,
            endMs      = now - 12 * day + 11 * 60_000L,
            isDemoData = true,
            attempts   = steps.map { step ->
                val correct = when (step.skillTag) {
                    AllergySkillTag.ANAPHYLAXIS_RECOGNITION -> false  // treats as mild
                    AllergySkillTag.CALL_112               -> true
                    AllergySkillTag.LAY_FLAT               -> false   // sits them up
                    AllergySkillTag.EPIPEN_LOCATION        -> false   // says upper arm
                    AllergySkillTag.EPIPEN_TECHNIQUE       -> false   // wrong technique
                    AllergySkillTag.POST_INJECTION         -> false   // cancels 112
                    AllergySkillTag.MONITORING             -> true
                    else                                   -> true
                }
                attempt(sid1, step, correct)
            }
        )

        // ── Session 2: recognition and call fixed; position and technique weak ─
        val sid2 = "allergy_demo_session_2"
        val session2 = ModuleTrainingSession(
            sessionId  = sid2,
            moduleType = MODULE,
            startMs    = now - 6 * day,
            endMs      = now - 6 * day + 9 * 60_000L,
            isDemoData = true,
            attempts   = steps.map { step ->
                val correct = when (step.skillTag) {
                    AllergySkillTag.ANAPHYLAXIS_RECOGNITION -> true   // improved
                    AllergySkillTag.CALL_112               -> true
                    AllergySkillTag.LAY_FLAT               -> false   // still wrong
                    AllergySkillTag.EPIPEN_LOCATION        -> true    // improved
                    AllergySkillTag.EPIPEN_TECHNIQUE       -> false   // still wrong
                    AllergySkillTag.POST_INJECTION         -> true    // improved
                    AllergySkillTag.MONITORING             -> true
                    else                                   -> true
                }
                attempt(sid2, step, correct)
            }
        )

        // ── Session 3: all correct ─────────────────────────────────────────────
        val sid3 = "allergy_demo_session_3"
        val session3 = ModuleTrainingSession(
            sessionId  = sid3,
            moduleType = MODULE,
            startMs    = now - 2 * day,
            endMs      = now - 2 * day + 8 * 60_000L,
            isDemoData = true,
            attempts   = steps.map { step -> attempt(sid3, step, true) }
        )

        return listOf(session1, session2, session3)
    }
}
