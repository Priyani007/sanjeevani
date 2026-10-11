package com.sanjeevani.training

/**
 * Deterministic demo data for Stroke FAST training.
 * 3 sessions showing progressive improvement:
 *  Session 1 (day -14): misses face/arm/speech recognition, struggles with TIA
 *  Session 2 (day -7):  face and arm recognition correct; speech and TIA still weak
 *  Session 3 (day -2):  all correct
 */
object StrokeDemoData {

    private const val MODULE = "STROKE_FAST"

    fun generateSessions(): List<ModuleTrainingSession> {
        val now   = System.currentTimeMillis()
        val day   = 86_400_000L
        val steps = StrokeTrainingContent.steps

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

        // ── Session 1: FAST acronym OK, face/arm recognition wrong, others weak ──
        val sid1 = "stroke_demo_session_1"
        val session1 = ModuleTrainingSession(
            sessionId  = sid1,
            moduleType = MODULE,
            startMs    = now - 14 * day,
            endMs      = now - 14 * day + 12 * 60_000L,
            isDemoData = true,
            attempts   = steps.map { step ->
                val correct = when (step.skillTag) {
                    StrokeSkillTag.FAST_RECOGNITION  -> true
                    StrokeSkillTag.FACE_DROOPING     -> false   // misses it
                    StrokeSkillTag.ARM_WEAKNESS      -> false   // misses it
                    StrokeSkillTag.SPEECH_DIFFICULTY -> false   // misses it
                    StrokeSkillTag.TIME_TO_ACT       -> true
                    StrokeSkillTag.SAFE_POSITIONING  -> false   // wrong positioning
                    StrokeSkillTag.DO_NOT_GIVE_FOOD  -> false   // gives water
                    StrokeSkillTag.TIA_RECOGNITION   -> false   // dismisses TIA
                    else                             -> true
                }
                attempt(sid1, step, correct)
            }
        )

        // ── Session 2: face and arm now correct; speech, food, TIA still weak ──
        val sid2 = "stroke_demo_session_2"
        val session2 = ModuleTrainingSession(
            sessionId  = sid2,
            moduleType = MODULE,
            startMs    = now - 7 * day,
            endMs      = now - 7 * day + 10 * 60_000L,
            isDemoData = true,
            attempts   = steps.map { step ->
                val correct = when (step.skillTag) {
                    StrokeSkillTag.FAST_RECOGNITION  -> true
                    StrokeSkillTag.FACE_DROOPING     -> true    // improved
                    StrokeSkillTag.ARM_WEAKNESS      -> true    // improved
                    StrokeSkillTag.SPEECH_DIFFICULTY -> false   // still weak
                    StrokeSkillTag.TIME_TO_ACT       -> true
                    StrokeSkillTag.SAFE_POSITIONING  -> true    // improved
                    StrokeSkillTag.DO_NOT_GIVE_FOOD  -> false   // still wrong
                    StrokeSkillTag.TIA_RECOGNITION   -> false   // still dismisses
                    else                             -> true
                }
                attempt(sid2, step, correct)
            }
        )

        // ── Session 3: all correct ─────────────────────────────────────────────
        val sid3 = "stroke_demo_session_3"
        val session3 = ModuleTrainingSession(
            sessionId  = sid3,
            moduleType = MODULE,
            startMs    = now - 2 * day,
            endMs      = now - 2 * day + 9 * 60_000L,
            isDemoData = true,
            attempts   = steps.map { step -> attempt(sid3, step, true) }
        )

        return listOf(session1, session2, session3)
    }
}
