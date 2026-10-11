package com.sanjeevani.training

import com.sanjeevani.model.FSMState
import com.sanjeevani.model.RateStatus
import com.sanjeevani.model.StrokeSignals

object AssessmentEngine {

    // CPR criteria
    val CPR_CALL_112 = CriterionKey(TrainingModuleType.CPR, "call_112", "Called emergency services (112)")
    val CPR_POSITIONING = CriterionKey(TrainingModuleType.CPR, "positioning", "Patient positioned on firm surface")
    val CPR_HAND_PLACEMENT = CriterionKey(TrainingModuleType.CPR, "hand_placement", "Hands centred on lower breastbone")
    val CPR_COMPRESSIONS_STARTED = CriterionKey(TrainingModuleType.CPR, "compressions_started", "Began chest compressions")
    val CPR_COMPRESSION_RATE = CriterionKey(TrainingModuleType.CPR, "compression_rate", "Compression rate 100–120 BPM")
    val CPR_THIRTY_CYCLES = CriterionKey(TrainingModuleType.CPR, "thirty_cycles", "Completed 30+ compression cycles")

    // Stroke FAST criteria
    val STROKE_FACE = CriterionKey(TrainingModuleType.STROKE_FAST, "face_assessed", "Face drooping assessed (F)")
    val STROKE_ARM = CriterionKey(TrainingModuleType.STROKE_FAST, "arm_assessed", "Arm weakness assessed (A)")
    val STROKE_SPEECH = CriterionKey(TrainingModuleType.STROKE_FAST, "speech_assessed", "Speech difficulty assessed (S)")
    val STROKE_EMERGENCY_CALL = CriterionKey(TrainingModuleType.STROKE_FAST, "emergency_call", "Recognised need to call 112 (T)")

    // Allergic reaction criteria
    val ALLERGY_LAY_FLAT = CriterionKey(TrainingModuleType.ALLERGIC_REACTION, "lay_flat", "Lay flat — do not allow standing up")
    val ALLERGY_EPIPEN_PREP = CriterionKey(TrainingModuleType.ALLERGIC_REACTION, "epipen_prep", "EpiPen auto-injector prepared")
    val ALLERGY_EPIPEN_INJECT = CriterionKey(TrainingModuleType.ALLERGIC_REACTION, "epipen_inject", "EpiPen injected to right outer thigh")
    val ALLERGY_MONITORING = CriterionKey(TrainingModuleType.ALLERGIC_REACTION, "monitoring", "Post-injection monitoring completed")

    fun assessCPR(
        completedStates: Set<FSMState>,
        compressionCount: Int,
        rateStatus: RateStatus,
        sessionEndMs: Long
    ): List<CriterionRecord> = listOf(
        CriterionRecord(
            CPR_CALL_112,
            if (FSMState.RESPONSIVENESS_CHECK in completedStates) CriterionResult.PASS else CriterionResult.NOT_ASSESSED,
            "Emergency call instructed at RESPONSIVENESS_CHECK step",
            sessionEndMs
        ),
        CriterionRecord(
            CPR_POSITIONING,
            when {
                FSMState.CPR_POSITIONING in completedStates -> CriterionResult.PASS
                else -> CriterionResult.NOT_ASSESSED
            },
            "User confirmed patient on back on firm surface",
            sessionEndMs
        ),
        CriterionRecord(
            CPR_HAND_PLACEMENT,
            when {
                FSMState.HAND_POSITIONING in completedStates -> CriterionResult.PASS
                else -> CriterionResult.NOT_ASSESSED
            },
            "User confirmed hand placement on lower breastbone",
            sessionEndMs
        ),
        CriterionRecord(
            CPR_COMPRESSIONS_STARTED,
            when {
                FSMState.COMPRESSION_ACTIVE in completedStates -> CriterionResult.PASS
                else -> CriterionResult.NEEDS_PRACTICE
            },
            if (FSMState.COMPRESSION_ACTIVE in completedStates)
                "Reached active compression phase" else "Session ended before compressions began",
            sessionEndMs
        ),
        CriterionRecord(
            CPR_COMPRESSION_RATE,
            when (rateStatus) {
                RateStatus.GOOD -> CriterionResult.PASS
                RateStatus.TOO_SLOW, RateStatus.TOO_FAST -> CriterionResult.NEEDS_PRACTICE
                RateStatus.INSUFFICIENT_DATA -> CriterionResult.INCONCLUSIVE
            },
            "Camera rate estimate: $rateStatus (100–120 BPM target; monocular depth not validated)",
            sessionEndMs
        ),
        CriterionRecord(
            CPR_THIRTY_CYCLES,
            when {
                compressionCount >= 30 -> CriterionResult.PASS
                compressionCount > 0 -> CriterionResult.NEEDS_PRACTICE
                else -> CriterionResult.NOT_ASSESSED
            },
            "$compressionCount estimated compression cycles (camera estimate only)",
            sessionEndMs
        )
    )

    fun assessStroke(
        signals: StrokeSignals,
        isComplete: Boolean,
        sessionEndMs: Long
    ): List<CriterionRecord> = listOf(
        CriterionRecord(
            STROKE_FACE,
            if (isComplete) CriterionResult.PASS else CriterionResult.NOT_ASSESSED,
            "Face asymmetry score from AR overlay: ${String.format("%.2f", signals.faceAsymmetryScore)}",
            sessionEndMs
        ),
        CriterionRecord(
            STROKE_ARM,
            if (isComplete) CriterionResult.PASS else CriterionResult.NOT_ASSESSED,
            "Arm drift observed: ${signals.armDriftDetected}",
            sessionEndMs
        ),
        CriterionRecord(
            STROKE_SPEECH,
            when {
                !isComplete -> CriterionResult.NOT_ASSESSED
                signals.speechPrompted -> CriterionResult.PASS
                else -> CriterionResult.NEEDS_PRACTICE
            },
            if (signals.speechPrompted) "Speech test prompted and assessed" else "Speech test not reached or timed out",
            sessionEndMs
        ),
        CriterionRecord(
            STROKE_EMERGENCY_CALL,
            if (isComplete) CriterionResult.PASS else CriterionResult.NEEDS_PRACTICE,
            "FAST result: ${signals.positiveTestCount} positive sign(s); 112 call instructed at RESULT",
            sessionEndMs
        )
    )

    // AllergicPhase ordinal: LAY_FLAT=0, RAISE_LEGS=1, SAFE_POSITION=2,
    //                         EPIPEN_READY=3, EPIPEN_INJECT=4, MONITORING=5
    fun assessAllergicReaction(
        completedPhaseOrdinal: Int,
        sessionEndMs: Long
    ): List<CriterionRecord> = listOf(
        CriterionRecord(
            ALLERGY_LAY_FLAT,
            if (completedPhaseOrdinal >= 0) CriterionResult.PASS else CriterionResult.NOT_ASSESSED,
            "Lay-flat phase instructed",
            sessionEndMs
        ),
        CriterionRecord(
            ALLERGY_EPIPEN_PREP,
            if (completedPhaseOrdinal >= 3) CriterionResult.PASS else CriterionResult.NEEDS_PRACTICE,
            "EpiPen preparation phase: ordinal reached = $completedPhaseOrdinal",
            sessionEndMs
        ),
        CriterionRecord(
            ALLERGY_EPIPEN_INJECT,
            if (completedPhaseOrdinal >= 4) CriterionResult.PASS else CriterionResult.NEEDS_PRACTICE,
            "EpiPen injection to right outer thigh: ordinal reached = $completedPhaseOrdinal",
            sessionEndMs
        ),
        CriterionRecord(
            ALLERGY_MONITORING,
            if (completedPhaseOrdinal >= 5) CriterionResult.PASS else CriterionResult.NEEDS_PRACTICE,
            "Post-injection monitoring phase: ordinal reached = $completedPhaseOrdinal",
            sessionEndMs
        )
    )

    fun compare(previous: SessionOutcome?, current: SessionOutcome): OutcomeComparison {
        val prevMap = previous?.criteria?.associateBy { it.key } ?: emptyMap()
        val newlyPassed = current.criteria.filter { c ->
            c.result == CriterionResult.PASS && (prevMap[c.key]?.result ?: CriterionResult.NOT_ASSESSED) != CriterionResult.PASS
        }.map { it.key }
        val stillFailing = current.criteria.filter { c ->
            c.result == CriterionResult.NEEDS_PRACTICE && prevMap[c.key]?.result == CriterionResult.NEEDS_PRACTICE
        }.map { it.key }
        val unchangedPass = current.criteria.filter { c ->
            c.result == CriterionResult.PASS && prevMap[c.key]?.result == CriterionResult.PASS
        }.map { it.key }
        return OutcomeComparison(current, previous, newlyPassed, stillFailing, unchangedPass)
    }

    fun criteriaFor(module: TrainingModuleType): List<CriterionKey> = when (module) {
        TrainingModuleType.CPR -> listOf(
            CPR_CALL_112, CPR_POSITIONING, CPR_HAND_PLACEMENT,
            CPR_COMPRESSIONS_STARTED, CPR_COMPRESSION_RATE, CPR_THIRTY_CYCLES
        )
        TrainingModuleType.STROKE_FAST -> listOf(
            STROKE_FACE, STROKE_ARM, STROKE_SPEECH, STROKE_EMERGENCY_CALL
        )
        TrainingModuleType.ALLERGIC_REACTION -> listOf(
            ALLERGY_LAY_FLAT, ALLERGY_EPIPEN_PREP, ALLERGY_EPIPEN_INJECT, ALLERGY_MONITORING
        )
    }
}
