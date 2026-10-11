package com.sanjeevani.training

/**
 * Deterministic synthetic demo data showing 14-day quiz progression for a fictional learner.
 *
 * Narrative:
 *  Day 1  – Beginner Heart Attack: poor (3/6), misses atypical symptoms & escalation
 *  Day 4  – Beginner CPR: critical safety error on agonal breathing question
 *  Day 7  – Beginner Allergy: partial (4/6), misses positioning after EpiPen
 *  Day 10 – Intermediate Heart Attack: improved (5/6), still misses atypical presentation
 *  Day 13 – Intermediate CPR: better (5/7), agonal breathing now correct but handover gap
 *  Day 14 – Intermediate Allergy: (5/6), anaphylaxis position still wrong
 *
 * Persistent gaps surfaced: atypical_presentation (Heart Attack), anaphylaxis_position (Allergy)
 * Resolved gap: agonal_breathing (CPR) — wrong Day 4, correct Day 13
 * Critical safety error: agonal_breathing in Day-4 CPR attempt (isCriticalSafety = true)
 */
object QuizDemoData {

    private val BASE_MS = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000

    private fun dayMs(day: Int): Long = BASE_MS + day.toLong() * 24 * 60 * 60 * 1000

    fun generateAttempts(): List<QuizAttempt> = listOf(
        heartAttackBeginner(),
        cprBeginnerWithCriticalError(),
        allergyBeginner(),
        heartAttackIntermediate(),
        cprIntermediate(),
        allergyIntermediate()
    )

    // ─── Day 1: Beginner Heart Attack ────────────────────────────────────────

    private fun heartAttackBeginner(): QuizAttempt = QuizAttempt(
        attemptId  = "demo-ha-beginner-001",
        startMs    = dayMs(0),
        endMs      = dayMs(0) + 8 * 60_000,
        category   = QuizCategory.HEART_ATTACK,
        difficulty = QuizDifficulty.BEGINNER,
        isDemoData = true,
        answers    = listOf(
            // HA_001 classic symptoms — correct
            answer("HA_001", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.SYMPTOM_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION), false),
            // HA_002 atypical (jaw/nausea) — WRONG
            answer("HA_002", QuizCategory.HEART_ATTACK, setOf("A"), setOf("C"),
                listOf(QuizSkillTag.ATYPICAL_PRESENTATION), false),
            // HA_003 symptom minimisation — correct
            answer("HA_003", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.EMERGENCY_ESCALATION, QuizSkillTag.APPROPRIATE_ACTION), false),
            // HA_004 resolved symptoms — WRONG
            answer("HA_004", QuizCategory.HEART_ATTACK, setOf("C"), setOf("B"),
                listOf(QuizSkillTag.ATYPICAL_PRESENTATION, QuizSkillTag.DANGEROUS_DELAY), false),
            // HA_005 diabetes/silent — WRONG
            answer("HA_005", QuizCategory.HEART_ATTACK, setOf("A"), setOf("B"),
                listOf(QuizSkillTag.ATYPICAL_PRESENTATION), false),
            // HA_006 aspirin — correct
            answer("HA_006", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.APPROPRIATE_ACTION, QuizSkillTag.EMERGENCY_ESCALATION), false)
        )
    )

    // ─── Day 4: Beginner CPR (critical safety error on agonal breathing) ─────

    private fun cprBeginnerWithCriticalError(): QuizAttempt = QuizAttempt(
        attemptId  = "demo-cpr-beginner-004",
        startMs    = dayMs(3),
        endMs      = dayMs(3) + 10 * 60_000,
        category   = QuizCategory.CPR_CARDIAC_ARREST,
        difficulty = QuizDifficulty.BEGINNER,
        isDemoData = true,
        answers    = listOf(
            // CPR_001 assessment sequence — correct
            answer("CPR_001", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.CARDIAC_ARREST_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION), false),
            // CPR_002 agonal breathing — WRONG + CRITICAL SAFETY
            answer("CPR_002", QuizCategory.CPR_CARDIAC_ARREST, setOf("A"), setOf("B"),
                listOf(QuizSkillTag.AGONAL_BREATHING), true),
            // CPR_003 sole rescuer sequence — correct
            answer("CPR_003", QuizCategory.CPR_CARDIAC_ARREST, setOf("C"), setOf("C"),
                listOf(QuizSkillTag.CPR_DECISION, QuizSkillTag.EMERGENCY_ESCALATION), false),
            // CPR_004 AED integration — correct
            answer("CPR_004", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.AED_USE), false),
            // CPR_005 handover — WRONG
            answer("CPR_005", QuizCategory.CPR_CARDIAC_ARREST, setOf("A"), setOf("C"),
                listOf(QuizSkillTag.HANDOVER), false),
            // CPR_006 hand placement — correct
            answer("CPR_006", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.CPR_TECHNIQUE), false),
            // CPR_007 rescuer rotation — correct
            answer("CPR_007", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.CPR_QUALITY), false)
        )
    )

    // ─── Day 7: Beginner Allergy ──────────────────────────────────────────────

    private fun allergyBeginner(): QuizAttempt = QuizAttempt(
        attemptId  = "demo-allergy-beginner-007",
        startMs    = dayMs(6),
        endMs      = dayMs(6) + 9 * 60_000,
        category   = QuizCategory.ALLERGIC_REACTION,
        difficulty = QuizDifficulty.BEGINNER,
        isDemoData = true,
        answers    = listOf(
            // ALLERGY_001 hives monitoring — correct
            answer("ALLERGY_001", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.MONITORING, QuizSkillTag.ANAPHYLAXIS_RECOGNITION), false),
            // ALLERGY_002 anaphylaxis airway — correct (critical safety, answered correctly)
            answer("ALLERGY_002", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.ANAPHYLAXIS_RECOGNITION, QuizSkillTag.EPIPEN_DECISION), false),
            // ALLERGY_003 EpiPen site — correct
            answer("ALLERGY_003", QuizCategory.ALLERGIC_REACTION, setOf("C"), setOf("C"),
                listOf(QuizSkillTag.EPIPEN_TECHNIQUE), false),
            // ALLERGY_004 positioning — WRONG
            answer("ALLERGY_004", QuizCategory.ALLERGIC_REACTION, setOf("A"), setOf("B"),
                listOf(QuizSkillTag.ANAPHYLAXIS_POSITION), false),
            // ALLERGY_005 biphasic — correct
            answer("ALLERGY_005", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.BIPHASIC_REACTION, QuizSkillTag.MONITORING), false),
            // ALLERGY_006 trigger agnostic — WRONG
            answer("ALLERGY_006", QuizCategory.ALLERGIC_REACTION, setOf("A"), setOf("B"),
                listOf(QuizSkillTag.EPIPEN_DECISION, QuizSkillTag.DANGEROUS_DELAY), false)
        )
    )

    // ─── Day 10: Intermediate Heart Attack (improved, atypical still wrong) ──

    private fun heartAttackIntermediate(): QuizAttempt = QuizAttempt(
        attemptId  = "demo-ha-intermediate-010",
        startMs    = dayMs(9),
        endMs      = dayMs(9) + 7 * 60_000,
        category   = QuizCategory.HEART_ATTACK,
        difficulty = QuizDifficulty.INTERMEDIATE,
        isDemoData = true,
        answers    = listOf(
            answer("HA_001", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.SYMPTOM_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION), false),
            // HA_002 atypical — still WRONG (persistent gap)
            answer("HA_002", QuizCategory.HEART_ATTACK, setOf("B"), setOf("C"),
                listOf(QuizSkillTag.ATYPICAL_PRESENTATION), false),
            answer("HA_003", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.EMERGENCY_ESCALATION, QuizSkillTag.APPROPRIATE_ACTION), false),
            answer("HA_004", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.ATYPICAL_PRESENTATION, QuizSkillTag.DANGEROUS_DELAY), false),
            answer("HA_005", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.ATYPICAL_PRESENTATION), false),
            answer("HA_006", QuizCategory.HEART_ATTACK, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.APPROPRIATE_ACTION, QuizSkillTag.EMERGENCY_ESCALATION), false)
        )
    )

    // ─── Day 13: Intermediate CPR (agonal breathing resolved, handover gap) ──

    private fun cprIntermediate(): QuizAttempt = QuizAttempt(
        attemptId  = "demo-cpr-intermediate-013",
        startMs    = dayMs(12),
        endMs      = dayMs(12) + 11 * 60_000,
        category   = QuizCategory.CPR_CARDIAC_ARREST,
        difficulty = QuizDifficulty.INTERMEDIATE,
        isDemoData = true,
        answers    = listOf(
            answer("CPR_001", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.CARDIAC_ARREST_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION), false),
            // CPR_002 agonal breathing — NOW CORRECT (resolved)
            answer("CPR_002", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.AGONAL_BREATHING), false),
            answer("CPR_003", QuizCategory.CPR_CARDIAC_ARREST, setOf("C"), setOf("C"),
                listOf(QuizSkillTag.CPR_DECISION, QuizSkillTag.EMERGENCY_ESCALATION), false),
            answer("CPR_004", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.AED_USE), false),
            // CPR_005 handover — still WRONG
            answer("CPR_005", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("C"),
                listOf(QuizSkillTag.HANDOVER), false),
            answer("CPR_006", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.CPR_TECHNIQUE), false),
            answer("CPR_007", QuizCategory.CPR_CARDIAC_ARREST, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.CPR_QUALITY), false)
        )
    )

    // ─── Day 14: Intermediate Allergy (positioning still wrong) ──────────────

    private fun allergyIntermediate(): QuizAttempt = QuizAttempt(
        attemptId  = "demo-allergy-intermediate-014",
        startMs    = dayMs(13),
        endMs      = dayMs(13) + 8 * 60_000,
        category   = QuizCategory.ALLERGIC_REACTION,
        difficulty = QuizDifficulty.INTERMEDIATE,
        isDemoData = true,
        answers    = listOf(
            answer("ALLERGY_001", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.MONITORING, QuizSkillTag.ANAPHYLAXIS_RECOGNITION), false),
            answer("ALLERGY_002", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.ANAPHYLAXIS_RECOGNITION, QuizSkillTag.EPIPEN_DECISION), false),
            answer("ALLERGY_003", QuizCategory.ALLERGIC_REACTION, setOf("C"), setOf("C"),
                listOf(QuizSkillTag.EPIPEN_TECHNIQUE), false),
            // ALLERGY_004 positioning — still WRONG (persistent gap)
            answer("ALLERGY_004", QuizCategory.ALLERGIC_REACTION, setOf("C"), setOf("B"),
                listOf(QuizSkillTag.ANAPHYLAXIS_POSITION), false),
            answer("ALLERGY_005", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.BIPHASIC_REACTION, QuizSkillTag.MONITORING), false),
            answer("ALLERGY_006", QuizCategory.ALLERGIC_REACTION, setOf("B"), setOf("B"),
                listOf(QuizSkillTag.EPIPEN_DECISION, QuizSkillTag.DANGEROUS_DELAY), false)
        )
    )

    // ─── builder helper ───────────────────────────────────────────────────────

    private fun answer(
        questionId: String,
        category: QuizCategory,
        selected: Set<String>,
        correct: Set<String>,
        skillTags: List<String>,
        isCriticalSafetyError: Boolean
    ): QuizAnswerRecord = QuizAnswerRecord(
        questionId        = questionId,
        category          = category,
        selectedOptionIds = selected,
        correctOptionIds  = correct,
        isCorrect         = selected == correct,
        skillTags         = skillTags,
        isCriticalSafety  = isCriticalSafetyError && selected != correct,
        timestampMs       = System.currentTimeMillis(),
        isDemoData        = true
    )
}
