package com.sanjeevani.engine

import com.sanjeevani.model.FSMState

enum class CPRStep { POSITION_CHECK, KNEEL_BESIDE, HAND_PLACEMENT, BODY_POSITION, COMPRESSIONS, MAINTAIN_RHYTHM }

/** Read-only projection. No timers or independent progression authority. */
object CPRWorkflow {
    fun stepFor(state: FSMState): CPRStep? = when (state) {
        FSMState.CPR_POSITIONING -> CPRStep.POSITION_CHECK
        FSMState.POSITION_CONFIRMED -> CPRStep.KNEEL_BESIDE
        FSMState.HAND_POSITIONING -> CPRStep.HAND_PLACEMENT
        FSMState.POSTURE_CHECK -> CPRStep.BODY_POSITION
        FSMState.COMPRESSION_ACTIVE -> CPRStep.COMPRESSIONS
        else -> null
    }
}
