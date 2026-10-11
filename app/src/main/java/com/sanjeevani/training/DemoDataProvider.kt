package com.sanjeevani.training

import java.util.UUID

object DemoDataProvider {

    private val DAY = 24 * 60 * 60 * 1000L

    fun generateSessions(now: Long = System.currentTimeMillis()): List<SessionOutcome> =
        cprSessions(now) + allergicSessions(now)

    private fun cprSessions(now: Long): List<SessionOutcome> = listOf(
        // Session 1 (14 days ago): Froze — only called 112, never reached positioning
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.CPR,
            startMs = now - 14 * DAY,
            endMs = now - 14 * DAY + 120_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS, "Responsiveness check step completed"),
                CriterionRecord(AssessmentEngine.CPR_POSITIONING, CriterionResult.NEEDS_PRACTICE, "Did not reach patient positioning step"),
                CriterionRecord(AssessmentEngine.CPR_HAND_PLACEMENT, CriterionResult.NEEDS_PRACTICE, "Hand placement step not reached"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.NEEDS_PRACTICE, "Compression phase not reached"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSION_RATE, CriterionResult.NOT_ASSESSED, "No compressions to assess"),
                CriterionRecord(AssessmentEngine.CPR_THIRTY_CYCLES, CriterionResult.NOT_ASSESSED, "No compressions to assess"),
            )
        ),
        // Session 2 (10 days ago): Reached compressions but rate too slow, only 8 cycles
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.CPR,
            startMs = now - 10 * DAY,
            endMs = now - 10 * DAY + 155_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS, "Responsiveness check step completed"),
                CriterionRecord(AssessmentEngine.CPR_POSITIONING, CriterionResult.PASS, "Position confirmed on flat surface"),
                CriterionRecord(AssessmentEngine.CPR_HAND_PLACEMENT, CriterionResult.NEEDS_PRACTICE, "Hand placement not confirmed — used manual fallback"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.PASS, "Compression phase reached"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSION_RATE, CriterionResult.NEEDS_PRACTICE, "Camera estimate: TOO_SLOW (100–120 BPM target; monocular depth not validated)"),
                CriterionRecord(AssessmentEngine.CPR_THIRTY_CYCLES, CriterionResult.NEEDS_PRACTICE, "8 estimated cycles (camera estimate only)"),
            )
        ),
        // Session 3 (7 days ago): Rate now good, hands placed, but only 22 cycles
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.CPR,
            startMs = now - 7 * DAY,
            endMs = now - 7 * DAY + 185_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS, "Responsiveness check step completed"),
                CriterionRecord(AssessmentEngine.CPR_POSITIONING, CriterionResult.PASS, "Position confirmed"),
                CriterionRecord(AssessmentEngine.CPR_HAND_PLACEMENT, CriterionResult.PASS, "Hand placement confirmed"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.PASS, "Compression phase reached"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSION_RATE, CriterionResult.PASS, "Camera estimate: GOOD (100–120 BPM target; monocular depth not validated)"),
                CriterionRecord(AssessmentEngine.CPR_THIRTY_CYCLES, CriterionResult.NEEDS_PRACTICE, "22 estimated cycles (camera estimate only)"),
            )
        ),
        // Session 4 (3 days ago): Full pass — all criteria met
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.CPR,
            startMs = now - 3 * DAY,
            endMs = now - 3 * DAY + 200_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.CPR_CALL_112, CriterionResult.PASS, "Responsiveness check step completed"),
                CriterionRecord(AssessmentEngine.CPR_POSITIONING, CriterionResult.PASS, "Position confirmed"),
                CriterionRecord(AssessmentEngine.CPR_HAND_PLACEMENT, CriterionResult.PASS, "Hand placement confirmed"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSIONS_STARTED, CriterionResult.PASS, "Compression phase reached"),
                CriterionRecord(AssessmentEngine.CPR_COMPRESSION_RATE, CriterionResult.PASS, "Camera estimate: GOOD (100–120 BPM target; monocular depth not validated)"),
                CriterionRecord(AssessmentEngine.CPR_THIRTY_CYCLES, CriterionResult.PASS, "31 estimated cycles (camera estimate only)"),
            )
        ),
    )

    private fun allergicSessions(now: Long): List<SessionOutcome> = listOf(
        // Session 1 (12 days ago): Only lay flat — exited before EpiPen steps
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.ALLERGIC_REACTION,
            startMs = now - 12 * DAY,
            endMs = now - 12 * DAY + 90_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.ALLERGY_LAY_FLAT, CriterionResult.PASS, "LAY_FLAT phase completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_PREP, CriterionResult.NEEDS_PRACTICE, "EpiPen preparation step not reached"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_INJECT, CriterionResult.NEEDS_PRACTICE, "EpiPen injection step not reached"),
                CriterionRecord(AssessmentEngine.ALLERGY_MONITORING, CriterionResult.NEEDS_PRACTICE, "Monitoring phase not reached"),
            )
        ),
        // Session 2 (8 days ago): Prepared EpiPen but did not inject
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.ALLERGIC_REACTION,
            startMs = now - 8 * DAY,
            endMs = now - 8 * DAY + 120_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.ALLERGY_LAY_FLAT, CriterionResult.PASS, "LAY_FLAT phase completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_PREP, CriterionResult.PASS, "EpiPen preparation completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_INJECT, CriterionResult.NEEDS_PRACTICE, "Injection step not completed: ordinal reached = 2"),
                CriterionRecord(AssessmentEngine.ALLERGY_MONITORING, CriterionResult.NEEDS_PRACTICE, "Monitoring phase not reached"),
            )
        ),
        // Session 3 (5 days ago): Injected EpiPen but skipped monitoring
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.ALLERGIC_REACTION,
            startMs = now - 5 * DAY,
            endMs = now - 5 * DAY + 155_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.ALLERGY_LAY_FLAT, CriterionResult.PASS, "LAY_FLAT phase completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_PREP, CriterionResult.PASS, "EpiPen preparation completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_INJECT, CriterionResult.PASS, "EpiPen injected in right outer thigh"),
                CriterionRecord(AssessmentEngine.ALLERGY_MONITORING, CriterionResult.NEEDS_PRACTICE, "Session ended before monitoring phase completed"),
            )
        ),
        // Session 4 (1 day ago): Full pass — entire protocol completed
        SessionOutcome(
            sessionId = UUID.randomUUID().toString(),
            moduleType = TrainingModuleType.ALLERGIC_REACTION,
            startMs = now - 1 * DAY,
            endMs = now - 1 * DAY + 175_000,
            criteria = listOf(
                CriterionRecord(AssessmentEngine.ALLERGY_LAY_FLAT, CriterionResult.PASS, "LAY_FLAT phase completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_PREP, CriterionResult.PASS, "EpiPen preparation completed"),
                CriterionRecord(AssessmentEngine.ALLERGY_EPIPEN_INJECT, CriterionResult.PASS, "EpiPen injected in right outer thigh"),
                CriterionRecord(AssessmentEngine.ALLERGY_MONITORING, CriterionResult.PASS, "Monitoring continued until emergency team arrived"),
            )
        ),
    )
}
