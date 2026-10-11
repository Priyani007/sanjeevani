package com.sanjeevani.engine

import com.sanjeevani.model.*

data class FSMTransitionResult(val newState: FSMState, val voiceText: String?, val requiresUserConfirmation: Boolean = false)
data class TransitionRecord(val timestamp: Long, val from: FSMState, val to: FSMState, val reason: String, val version: Long)
enum class Answer { YES, NO, UNCERTAIN, REPEAT, QUESTION, UNKNOWN, READY, CHANGED, HELP_ARRIVED }

/** Sole authority for progression. Camera never determines responsiveness or breathing. */
class EmergencyFSM {
    private var state = FSMState.IDLE
    private var type = EmergencyType.UNKNOWN
    var version = 0L
        private set
    val history = mutableListOf<TransitionRecord>()
    val completed = mutableSetOf<FSMState>()
    var responding: Boolean? = null
        private set
    var breathingNormally: Boolean? = null
        private set
    var lastAnswer: Answer? = null
        private set
    private var stableSince: Long? = null
    private var lastObservation = -1L
    fun getState() = state
    fun getConfirmedEmergencyType() = type
    fun instruction(): String = when (state) {
        FSMState.IDLE, FSMState.SCENE_ASSESSMENT, FSMState.TRIAGE_DETECTION ->
            "Make sure it is safe to approach. I'm here to help. Call one one two or ask someone nearby to call. Tell me what's happening."
        FSMState.RESPONSIVENESS_CHECK ->
            "Call one one two on speaker now. Gently tap their shoulders and speak loudly. Are they responding?"
        FSMState.BREATHING_ASSESSMENT ->
            "Check for normal breathing for no more than ten seconds. Gasping is not normal breathing. Are they breathing normally?"
        FSMState.CPR_POSITIONING ->
            "They need CPR. Put them on their back. Ask someone to bring an AED. A firm surface is best, but do not delay CPR to move them off a bed or wait for the camera."
        FSMState.POSITION_CONFIRMED ->
            "Position confirmed. Kneel beside them. Tap Ready when set."
        FSMState.HAND_POSITIONING ->
            "Place the heel of one hand in the centre of the chest, on the lower half of the breastbone. Put your other hand on top. The marker is only an estimate."
        FSMState.POSTURE_CHECK ->
            "Arms straight, shoulders above hands. Tap Ready."
        FSMState.COMPRESSION_ACTIVE ->
            "Begin compressions!"
        FSMState.HEART_ATTACK_CONSCIOUS ->
            if (responding == true) "Keep them comfortable and still. Call one one two. Watch their breathing and tell me if they become unresponsive."
            else "They are breathing normally. Follow the dispatcher about positioning them safely. Watch their breathing. Tell me if their condition changes."
        FSMState.CPR_SUCCESS -> "Let the emergency team take over. Follow their instructions."
        FSMState.CPR_PAUSE -> "Follow the emergency dispatcher. Tap resume when you want app guidance."
        FSMState.ALLERGIC_PROTOCOL ->
            "Anaphylaxis protocol. Call one one two now. Follow the on-screen guidance and voice instructions."
        FSMState.STROKE_FAST_TEST ->
            "Follow the on-screen FAST assessment. Watch their face, arms, and speech carefully. Call one one two now — do not delay."
        else -> "Call one one two now. Follow the emergency dispatcher's instructions."
    }
    fun expectsAnswer() = state in setOf(FSMState.IDLE, FSMState.SCENE_ASSESSMENT,
        FSMState.TRIAGE_DETECTION, FSMState.RESPONSIVENESS_CHECK, FSMState.BREATHING_ASSESSMENT,
        FSMState.POSITION_CONFIRMED, FSMState.POSTURE_CHECK)

    fun setUserSelectedEmergency(selected: EmergencyType, nowMs: Long) {
        if (state !in setOf(FSMState.IDLE, FSMState.SCENE_ASSESSMENT, FSMState.TRIAGE_DETECTION)) return
        type = selected
        when (selected) {
            EmergencyType.UNKNOWN -> transition(FSMState.TRIAGE_DETECTION, nowMs, "selection fallback")
            EmergencyType.ALLERGIC_REACTION -> transition(FSMState.ALLERGIC_PROTOCOL, nowMs,
                "allergic reaction selected; begin anaphylaxis protocol")
            EmergencyType.FAST_STROKE -> transition(FSMState.STROKE_FAST_TEST, nowMs,
                "stroke selected; begin FAST assessment")
            else -> transition(FSMState.RESPONSIVENESS_CHECK, nowMs,
                "reported concern: $selected; diagnosis not inferred")
        }
    }

    fun resetForNewSession(nowMs: Long) {
        val prev = state
        state = FSMState.IDLE
        type = EmergencyType.UNKNOWN
        version++
        responding = null; breathingNormally = null
        lastAnswer = null; stableSince = null; lastObservation = -1L
        completed.clear()
        history.add(TransitionRecord(nowMs, prev, FSMState.IDLE,
            "user navigated back; reset for new session", version))
    }

    fun answer(answer: Answer, nowMs: Long, expectedVersion: Long = version): Boolean {
        if (expectedVersion != version) return false
        lastAnswer = answer
        if (answer == Answer.HELP_ARRIVED) return transition(FSMState.CPR_SUCCESS, nowMs, "user: emergency team taking over")
        if (answer == Answer.CHANGED && state in setOf(FSMState.HEART_ATTACK_CONSCIOUS, FSMState.COMPRESSION_ACTIVE)) {
            responding = null; breathingNormally = null
            return transition(FSMState.RESPONSIVENESS_CHECK, nowMs, "user reports changed condition; reassess")
        }
        return when (state) {
            FSMState.RESPONSIVENESS_CHECK -> when (answer) {
                Answer.YES -> { responding = true; transition(FSMState.HEART_ATTACK_CONSCIOUS, nowMs, "user reports response") }
                Answer.NO -> { responding = false; transition(FSMState.BREATHING_ASSESSMENT, nowMs, "user reports no response") }
                else -> false
            }
            FSMState.BREATHING_ASSESSMENT -> when (answer) {
                Answer.YES -> { breathingNormally = true; transition(FSMState.HEART_ATTACK_CONSCIOUS, nowMs, "user reports normal breathing") }
                Answer.NO -> if (responding == false) { breathingNormally = false; transition(FSMState.CPR_POSITIONING, nowMs, "unresponsive and not breathing normally reported") } else false
                else -> false
            }
            FSMState.CPR_POSITIONING -> if (answer == Answer.READY) transition(FSMState.HAND_POSITIONING, nowMs, "user confirms back on firm surface; camera unverified") else false
            FSMState.POSITION_CONFIRMED -> if (answer == Answer.READY) transition(FSMState.HAND_POSITIONING, nowMs, "user confirms surface and kneeling") else false
            FSMState.HAND_POSITIONING -> if (answer == Answer.READY) transition(FSMState.POSTURE_CHECK, nowMs, "user confirms hand placement; camera unverified") else false
            FSMState.POSTURE_CHECK -> if (answer == Answer.READY && responding == false && breathingNormally == false)
                transition(FSMState.COMPRESSION_ACTIVE, nowMs, "user ready; CPR entry criteria satisfied") else false
            else -> false
        }
    }

    fun process(guidance: GuidanceState, nowMs: Long): FSMTransitionResult {
        if (nowMs <= lastObservation) return FSMTransitionResult(state, null)
        if (lastObservation >= 0 && nowMs - lastObservation > 300) stableSince = null
        lastObservation = nowMs
        val accepted = when (state) {
            FSMState.CPR_POSITIONING -> guidance.isPatientDetected && guidance.confidence.patientConfidence >= .65f
            FSMState.HAND_POSITIONING -> guidance.spatial.correctiveAction == SpatialAction.CORRECT &&
                guidance.confidence.poseConfidence >= .65f && guidance.confidence.handsConfidence >= .65f
            else -> { stableSince = null; return FSMTransitionResult(state, null) }
        }
        if (!accepted) stableSince = null
        else {
            val start = stableSince ?: nowMs.also { stableSince = it }
            if (nowMs - start >= 1000) {
                val next = if (state == FSMState.CPR_POSITIONING) FSMState.POSITION_CONFIRMED else FSMState.POSTURE_CHECK
                transition(next, nowMs, "fresh high-confidence geometry stable one second; estimate only")
                return FSMTransitionResult(state, instruction())
            }
        }
        return FSMTransitionResult(state, null)
    }
    fun confirmNoResponse(nowMs: Long) { answer(Answer.NO, nowMs) }
    fun onPatientResponsive(nowMs: Long) { answer(Answer.YES, nowMs) }
    private fun transition(next: FSMState, nowMs: Long, reason: String): Boolean {
        if (next == state) return false
        val previous = state
        completed.add(previous)
        state = next; version++; stableSince = null; lastObservation = -1
        history.add(TransitionRecord(nowMs, previous, next, reason, version))
        return true
    }
}
