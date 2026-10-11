package com.sanjeevani.voice

import com.sanjeevani.model.ConfidenceLevel
import com.sanjeevani.model.FSMState
import com.sanjeevani.model.GuidanceState
import com.sanjeevani.model.RateStatus
import com.sanjeevani.model.SpatialAction
import java.util.Locale

/**
 * Deterministic, perception-grounded voice help. This class never changes the
 * emergency workflow and never invents medical guidance from an open-ended model.
 */
object ContextAwareVoiceCompanion {

    enum class Intent {
        REPEAT,
        WHAT_NEXT,
        CHECK_ACTION,
        HAND_LOCATION,
        COMPRESSION_SPEED,
        PATIENT_MOVED,
        PATIENT_BREATHING,
        CANNOT_CONTINUE,
        HELP_ARRIVED,
        EMOTIONAL_DISTRESS,
        UNKNOWN
    }

    data class Response(
        val intent: Intent,
        val text: String
    )

    fun resolve(
        transcript: String,
        guidance: GuidanceState,
        lastInstruction: String?
    ): Response {
        val intent = classify(transcript)
        val text = when (intent) {
            Intent.REPEAT -> lastInstruction
                ?: currentInstruction(guidance)
            Intent.WHAT_NEXT -> currentInstruction(guidance)
            Intent.CHECK_ACTION -> checkCurrentAction(guidance)
            Intent.HAND_LOCATION -> handLocation(guidance)
            Intent.COMPRESSION_SPEED -> compressionSpeed(guidance)
            Intent.PATIENT_MOVED ->
                "Movement can be a sign of response. Stop briefly, check if she is breathing normally, and tell me if she responds."
            Intent.PATIENT_BREATHING ->
                "If she is breathing normally, stop compressions. Keep her still, watch her breathing, and call 1 1 2 now."
            Intent.CANNOT_CONTINUE ->
                "Call for another person to take over. Continue until trained help arrives, she breathes normally, or you cannot physically continue."
            Intent.HELP_ARRIVED ->
                "Tell the responders what happened and follow their instructions. Let them take over now."
            Intent.EMOTIONAL_DISTRESS ->
                reassuranceWithOneAction(guidance)
            Intent.UNKNOWN ->
                "I didn't understand. Ask: what next, are my hands right, where do I press, or am I fast enough?"
        }
        return Response(intent, text)
    }

    fun classify(transcript: String): Intent {
        val phrase = transcript.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            containsAny(phrase, "ambulance is here", "ambulance arrived", "help is here", "paramedics are here") ->
                Intent.HELP_ARRIVED
            containsAny(phrase, "she is breathing", "she's breathing", "he is breathing", "he's breathing", "breathing normally") ->
                Intent.PATIENT_BREATHING
            containsAny(phrase, "she moved", "he moved", "she is moving", "he is moving", "woke up", "is awake") ->
                Intent.PATIENT_MOVED
            containsAny(phrase, "i am scared", "i'm scared", "i can't do this", "i cannot do this", "help me", "i don't know how") ->
                Intent.EMOTIONAL_DISTRESS
            containsAny(phrase, "can i stop", "should i stop", "i need to stop", "i can't continue", "i cannot continue", "too tired") ->
                Intent.CANNOT_CONTINUE
            containsAny(phrase, "repeat", "say that again", "what did you say", "again please") ->
                Intent.REPEAT
            containsAny(phrase, "how fast", "fast enough", "what pace", "what speed", "too fast", "too slow") ->
                Intent.COMPRESSION_SPEED
            containsAny(phrase, "where do i press", "where should i press", "where do my hands go", "where should my hands", "where is the marker") ->
                Intent.HAND_LOCATION
            containsAny(phrase, "is this right", "is this correct", "am i doing it right", "are my hands right", "hands correct", "does this look right") ->
                Intent.CHECK_ACTION
            containsAny(phrase, "what do i do", "what next", "now what", "what should i do", "tell me what to do") ->
                Intent.WHAT_NEXT
            else -> Intent.UNKNOWN
        }
    }

    private fun checkCurrentAction(guidance: GuidanceState): String {
        if (guidance.confidence.overall == ConfidenceLevel.LOW ||
            guidance.spatial.correctiveAction == SpatialAction.TRACKING_LOST
        ) {
            return "I can't see clearly enough to verify. Move the phone back so the patient and your hand are visible."
        }

        return when (guidance.fsmState) {
            FSMState.CPR_POSITIONING -> if (guidance.isPatientDetected) {
                "Yes, she appears to be on her back. Keep her on a firm, flat surface."
            } else {
                "Not yet. Roll her onto her back on a firm, flat surface, with one arm on each side."
            }
            FSMState.HAND_POSITIONING, FSMState.POSTURE_CHECK -> handCorrection(guidance)
            FSMState.COMPRESSION_ACTIVE -> when (guidance.temporal.rateStatus) {
                RateStatus.TOO_SLOW -> "Your rhythm is too slow. Speed up and match the pulsing marker."
                RateStatus.TOO_FAST -> "Your rhythm is too fast. Slow down slightly and keep it steady."
                RateStatus.GOOD -> "Your rhythm is good. Keep pressing and don't stop."
                RateStatus.INSUFFICIENT_DATA -> "Keep compressing. I need a few more compressions to measure your rhythm."
            }
            else -> currentInstruction(guidance)
        }
    }

    private fun handCorrection(guidance: GuidanceState): String = when (guidance.spatial.correctiveAction) {
        SpatialAction.CORRECT -> "Yes, your hand is centered correctly. Keep it on the blue circle."
        SpatialAction.MOVE_LEFT -> "Move slightly left toward the blue circle."
        SpatialAction.MOVE_RIGHT -> "Shift slightly right toward the blue circle."
        SpatialAction.MOVE_UP -> "Move slightly up toward the blue circle."
        SpatialAction.MOVE_DOWN -> "Move slightly down toward the blue circle."
        SpatialAction.TRACKING_LOST, SpatialAction.UNSURE ->
            "I can't verify your hand. Move the phone back and keep it visible."
    }

    private fun handLocation(guidance: GuidanceState): String {
        return if (guidance.spatial.sternumTarget == null) {
            "Place one hand in the center of the chest."
        } else {
            when (guidance.spatial.correctiveAction) {
                SpatialAction.CORRECT -> "Keep your hand exactly on the blue circle."
                else -> "Place one hand in the center of the chest and follow the blue circle. ${handCorrection(guidance)}"
            }
        }
    }

    private fun compressionSpeed(guidance: GuidanceState): String {
        val bpm = guidance.temporal.compressionRateBPM.toInt()
        return when {
            guidance.fsmState != FSMState.COMPRESSION_ACTIVE ->
                "When compressions begin, follow the pulsing marker at 100 to 120 compressions per minute."
            bpm <= 0 -> "Keep compressing. I need a few more presses before I can measure your rhythm."
            bpm < 90 -> "You're at about $bpm per minute. Go faster and match the pulsing marker."
            bpm > 130 -> "You're at about $bpm per minute. Slow down slightly and keep a steady rhythm."
            else -> "You're at about $bpm per minute. That's a good rhythm. Keep going."
        }
    }

    private fun currentInstruction(guidance: GuidanceState): String {
        val overlayInstruction = guidance.overlay.stepCardInstruction.takeIf { it.isNotBlank() }
        return when (guidance.fsmState) {
            FSMState.RESPONSIVENESS_CHECK ->
                "Tap her shoulders firmly and shout her name. Tell me whether she responds."
            FSMState.EMERGENCY_ESCALATION ->
                "Call 1 1 2 now. Put the phone on speaker and stay with her."
            FSMState.CPR_POSITIONING -> overlayInstruction
                ?: "Roll her onto her back on a firm, flat surface."
            FSMState.HAND_POSITIONING, FSMState.POSTURE_CHECK ->
                handCorrection(guidance)
            FSMState.COMPRESSION_ACTIVE ->
                "Keep pressing hard and fast. Match the pulsing marker at 100 to 120 compressions per minute."
            FSMState.STROKE_FAST_TEST -> overlayInstruction
                ?: "Follow the F A S T check shown on screen and report what you observe."
            FSMState.HEART_ATTACK_CONSCIOUS ->
                "Keep her still and calm, and call 1 1 2 now. Do not let her walk."
            FSMState.ALLERGIC_PROTOCOL -> overlayInstruction
                ?: "Follow the current allergic reaction step shown on screen."
            else -> "Tell me what is happening, or choose the emergency on screen."
        }
    }

    private fun reassuranceWithOneAction(guidance: GuidanceState): String {
        val action = when (guidance.fsmState) {
            FSMState.HAND_POSITIONING, FSMState.POSTURE_CHECK ->
                "Place one hand on the blue circle."
            FSMState.COMPRESSION_ACTIVE ->
                "Keep pressing and match the pulsing marker."
            FSMState.CPR_POSITIONING ->
                "Roll her onto her back on a firm, flat surface."
            else -> currentInstruction(guidance)
        }
        return "You can do this. I'm staying with you. $action"
    }

    private fun containsAny(value: String, vararg candidates: String): Boolean =
        candidates.any(value::contains)
}
