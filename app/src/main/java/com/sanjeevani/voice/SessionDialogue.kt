package com.sanjeevani.voice

import com.sanjeevani.engine.Answer
import com.sanjeevani.model.EmergencyType
import com.sanjeevani.model.FSMState

data class Interpretation(val answer: Answer = Answer.UNKNOWN, val concern: EmergencyType? = null)

/** Conservative final-transcript parser. A phrase from a different question is not a yes/no. */
object SessionDialogue {
    private val NO_RESPONSE_KEYWORDS = setOf(
        "no response", "not responding", "unconscious", "no response found",
        "not conscious", "unresponsive", "he's not responding", "she's not responding"
    )
    fun interpret(raw: String, state: FSMState, confidence: Float = -1f): Interpretation {
        val text = raw.lowercase().replace('’', '\'').replace(Regex("[^a-z' ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        fun result(a: Answer) = Interpretation(answer = a)
        fun has(vararg phrases: String) = phrases.any { phrase ->
            Regex("(^| )" + Regex.escape(phrase) + "($| )").containsMatchIn(text)
        }
        if (confidence in 0f..<.55f) return result(Answer.UNCERTAIN)
        if (has("repeat", "say that again", "what next")) return result(Answer.REPEAT)
        if (has("how fast", "where do i press", "where do my hands go", "am i doing it right",
                "are my hands right", "i'm scared", "i am scared", "i can't do this"))
            return result(Answer.QUESTION)
        if (has("don't know", "do not know", "not sure", "maybe", "can't tell")) return result(Answer.UNCERTAIN)
        if (has("paramedics are taking over", "emergency team is here")) return result(Answer.HELP_ARRIVED)
        val deterioration = state == FSMState.HEART_ATTACK_CONSCIOUS &&
            (has("became unresponsive", "stopped breathing", "not breathing", "isn't responding") ||
                NO_RESPONSE_KEYWORDS.any { has(it) })
        val recovery = state == FSMState.COMPRESSION_ACTIVE &&
            has("woke up", "now responding", "she is responding", "he is responding")
        if (deterioration || recovery || has("condition changed"))
            return result(Answer.CHANGED)
        if (state in setOf(FSMState.IDLE, FSMState.SCENE_ASSESSMENT, FSMState.TRIAGE_DETECTION)) {
            val concern = when {
                has("heart attack", "chest pain", "chest hurts") -> EmergencyType.HEART_ATTACK
                has("not breathing", "collapsed", "cardiac arrest", "unconscious", "unresponsive") -> EmergencyType.CPR
                has("stroke", "face droop", "slurred speech") -> EmergencyType.FAST_STROKE
                has("allergic reaction", "anaphylaxis", "epipen") -> EmergencyType.ALLERGIC_REACTION
                else -> null
            }
            return Interpretation(concern = concern)
        }
        val plainYes = text in setOf("yes", "yeah", "yes she is", "yes he is", "yes they are")
        val plainNo = text in setOf("no", "no she isn't", "no he isn't", "no they aren't")
        if (state == FSMState.RESPONSIVENESS_CHECK) {
            val negative = plainNo || NO_RESPONSE_KEYWORDS.any { has(it) } ||
                has("isn't responding", "is not responding", "not responsive", "no she is not responding")
            val positive = plainYes || text == "responsive" || has("she moved", "he moved", "he's awake", "she's awake",
                "she is responding", "he is responding", "they are responding", "she's responding", "he's responding")
            if ((negative && (positive || has("yes"))) || (positive && has("no", "not"))) return result(Answer.UNCERTAIN)
            return result(if (negative) Answer.NO else if (positive) Answer.YES else Answer.UNKNOWN)
        }
        if (state == FSMState.BREATHING_ASSESSMENT) {
            val negative = plainNo || has("not breathing", "isn't breathing", "not breathing normally", "gasping", "only gasps")
            val positive = plainYes || has("she is breathing normally", "he is breathing normally", "normal breathing")
            if ((negative && (positive || has("yes"))) || (positive && has("no", "not"))) return result(Answer.UNCERTAIN)
            return result(if (negative) Answer.NO else if (positive) Answer.YES else Answer.UNKNOWN)
        }
        if (state == FSMState.COMPRESSION_ACTIVE) {
            if (plainYes || has("showing signs", "they responded", "responding now"))
                return result(Answer.YES)
            if (plainNo || has("no signs", "not showing", "nothing yet"))
                return result(Answer.NO)
        }
        if (text in setOf("ready", "i am ready", "i'm ready", "done"))
            return result(Answer.READY)
        return result(Answer.UNKNOWN)
    }

    fun help(raw: String, state: FSMState, instruction: String): String {
        val text = raw.lowercase()
        val active = state == FSMState.COMPRESSION_ACTIVE
        val placement = state in setOf(FSMState.HAND_POSITIONING, FSMState.POSTURE_CHECK, FSMState.COMPRESSION_ACTIVE)
        return when {
            text.contains("how fast") && active ->
                "Aim for one hundred to one hundred twenty per minute. Follow the pacing cue. The camera does not measure depth."
            (text.contains("where") || text.contains("hands right") || text.contains("doing it right")) && placement ->
                "The marker is approximate, not medical verification. Use the heel of your hand on the lower half of the breastbone, with your other hand on top. Follow the dispatcher."
            else -> "Stay with me. Follow the dispatcher first. " + instruction
        }
    }
}

/** Every asynchronous turn captures both epoch and workflow version. */
class TurnGate {
    private var epoch = 0L
    fun invalidate(): Long = ++epoch
    fun accepts(token: Long, capturedVersion: Long, currentVersion: Long) =
        token == epoch && capturedVersion == currentVersion
}
