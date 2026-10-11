package com.sanjeevani.training

import java.util.UUID

/**
 * Deterministic demo sessions showing a realistic CPR training arc.
 * All sessions are marked isDemoData=true so they are isolated from real data.
 */
object CprDemoData {

    fun generateSessions(): List<CprSkillSession> = listOf(session1(), session2(), session3())

    // ── Session 1 (Day 1): First attempt — weak on hand placement + compression rate ──
    private fun session1(): CprSkillSession {
        val sid = "demo-cpr-session-001"
        val t0  = System.currentTimeMillis() - 12L * 86_400_000L
        return CprSkillSession(
            sessionId  = sid,
            startMs    = t0,
            endMs      = t0 + 420_000L,
            isDemoData = true,
            attempts   = listOf(
                attempt(sid, 0, CprSkillTag.SCENE_SAFETY,          "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 30_000),
                attempt(sid, 1, CprSkillTag.ARREST_RECOGNITION,    "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 75_000),
                attempt(sid, 2, CprSkillTag.EMERGENCY_ESCALATION,  "A", "B", KnowledgeResult.INCORRECT, SkillAttemptOutcome.INCORRECT,      t0 + 120_000),
                attempt(sid, 3, CprSkillTag.PATIENT_POSITIONING,   "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 165_000),
                attempt(sid, 4, CprSkillTag.HAND_PLACEMENT_REGION, "B", "B", KnowledgeResult.CORRECT,   SkillAttemptOutcome.NEEDS_PRACTICE, t0 + 225_000,
                    HandPlacementResult.TOO_HIGH, "Camera: hands appear above the sternum target."),
                attempt(sid, 5, CprSkillTag.BODY_POSITION,         "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 300_000),
                attempt(sid, 6, CprSkillTag.COMPRESSION_RATE,      "A", "B", KnowledgeResult.INCORRECT, SkillAttemptOutcome.INCORRECT,      t0 + 360_000)
            )
        )
    }

    // ── Session 2 (Day 5): Improved — escalation now correct, placement still off ──
    private fun session2(): CprSkillSession {
        val sid = "demo-cpr-session-002"
        val t0  = System.currentTimeMillis() - 7L * 86_400_000L
        return CprSkillSession(
            sessionId  = sid,
            startMs    = t0,
            endMs      = t0 + 380_000L,
            isDemoData = true,
            attempts   = listOf(
                attempt(sid, 0, CprSkillTag.SCENE_SAFETY,          "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 25_000),
                attempt(sid, 1, CprSkillTag.ARREST_RECOGNITION,    "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 65_000),
                attempt(sid, 2, CprSkillTag.EMERGENCY_ESCALATION,  "B", "B", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 110_000),
                attempt(sid, 3, CprSkillTag.PATIENT_POSITIONING,   "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 155_000),
                attempt(sid, 4, CprSkillTag.HAND_PLACEMENT_REGION, "B", "B", KnowledgeResult.CORRECT,   SkillAttemptOutcome.NEEDS_PRACTICE, t0 + 215_000,
                    HandPlacementResult.TOO_HIGH, "Camera: hands appear slightly above the sternum target."),
                attempt(sid, 5, CprSkillTag.BODY_POSITION,         "C", "C", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 285_000),
                attempt(sid, 6, CprSkillTag.COMPRESSION_RATE,      "B", "B", KnowledgeResult.CORRECT,   SkillAttemptOutcome.CORRECT,        t0 + 345_000)
            )
        )
    }

    // ── Session 3 (Day 10): Hand placement now correct ─────────────────────────────
    private fun session3(): CprSkillSession {
        val sid = "demo-cpr-session-003"
        val t0  = System.currentTimeMillis() - 2L * 86_400_000L
        return CprSkillSession(
            sessionId  = sid,
            startMs    = t0,
            endMs      = t0 + 360_000L,
            isDemoData = true,
            attempts   = listOf(
                attempt(sid, 0, CprSkillTag.SCENE_SAFETY,          "C", "C", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 20_000),
                attempt(sid, 1, CprSkillTag.ARREST_RECOGNITION,    "C", "C", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 60_000),
                attempt(sid, 2, CprSkillTag.EMERGENCY_ESCALATION,  "B", "B", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 100_000),
                attempt(sid, 3, CprSkillTag.PATIENT_POSITIONING,   "C", "C", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 140_000),
                attempt(sid, 4, CprSkillTag.HAND_PLACEMENT_REGION, "B", "B", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 200_000,
                    HandPlacementResult.CORRECT, "Camera: hands appear over sternum target area."),
                attempt(sid, 5, CprSkillTag.BODY_POSITION,         "C", "C", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 265_000),
                attempt(sid, 6, CprSkillTag.COMPRESSION_RATE,      "B", "B", KnowledgeResult.CORRECT, SkillAttemptOutcome.CORRECT,  t0 + 320_000)
            )
        )
    }

    private fun attempt(
        sessionId: String,
        stepIndex: Int,
        skillTag: String,
        selectedId: String,
        correctId: String,
        knowledgeResult: KnowledgeResult,
        outcome: SkillAttemptOutcome,
        ts: Long,
        handResult: HandPlacementResult = HandPlacementResult.NOT_OBSERVED,
        camNote: String = ""
    ): CprSkillAttempt {
        val step = CprTrainingContent.stepFor(stepIndex)
        return CprSkillAttempt(
            attemptId             = "demo-${skillTag.replace('.', '-')}-s${sessionId.takeLast(3)}-$stepIndex",
            sessionId             = sessionId,
            skillTag              = skillTag,
            skillDescription      = step?.skillDescription ?: skillTag,
            stepIndex             = stepIndex,
            questionId            = "$skillTag.step$stepIndex",
            selectedOptionId      = selectedId,
            correctOptionId       = correctId,
            knowledgeResult       = knowledgeResult,
            handPlacementResult   = handResult,
            cameraObservationNote = camNote,
            outcome               = outcome,
            feedbackShown         = step?.options?.firstOrNull { it.id == selectedId }?.feedback ?: "",
            timestampMs           = ts
        )
    }
}
