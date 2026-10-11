package com.sanjeevani.engine

import android.graphics.Color
import com.sanjeevani.model.*

private enum class HeartAttackPhase {
    SIT_DOWN, LOOSEN_CLOTHING, ASPIRIN_PROMPT, MONITORING
}

private val PHASE_START_MS = mapOf(
    HeartAttackPhase.SIT_DOWN        to 0L,
    HeartAttackPhase.LOOSEN_CLOTHING to 10_000L,
    HeartAttackPhase.ASPIRIN_PROMPT  to 20_000L,
    HeartAttackPhase.MONITORING      to 35_000L
)

private val PHASE_VOICE = mapOf(
    HeartAttackPhase.SIT_DOWN        to "Sit or lie down. Do not walk.",
    HeartAttackPhase.LOOSEN_CLOTHING to "Loosen collar, belt, and chest clothing.",
    HeartAttackPhase.ASPIRIN_PROMPT  to "Give 300 milligrams aspirin to chew, if not allergic.",
    HeartAttackPhase.MONITORING      to "Help is on the way. Keep them calm. Do not leave them alone."
)

class HeartAttackModule : EmergencyModule {

    private var startMs = -1L
    private var currentPhase = HeartAttackPhase.SIT_DOWN
    private var lastSpokenPhase: HeartAttackPhase? = null

    override fun process(frame: PerceptionFrame, nowMs: Long): ModuleResult {
        if (startMs < 0) startMs = nowMs
        val elapsed = nowMs - startMs

        val newPhase = phaseForElapsed(elapsed)
        val phaseChanged = newPhase != currentPhase
        currentPhase = newPhase

        val voiceText = if (phaseChanged || lastSpokenPhase == null) {
            lastSpokenPhase = currentPhase
            PHASE_VOICE[currentPhase]
        } else null

        val progress = progressInPhase(elapsed)
        val overlay = AROverlaySpec(
            statusText = overlayText(currentPhase),
            statusColorGreen = currentPhase == HeartAttackPhase.MONITORING,
            guidanceText = PHASE_VOICE[currentPhase] ?: "",
            stepCardTitle = phaseTitle(currentPhase),
            stepCardInstruction = PHASE_VOICE[currentPhase] ?: "",
            stepCardStatus = if (currentPhase == HeartAttackPhase.MONITORING) {
                "✓ Keep monitoring"
            } else {
                "Call 1 1 2 now"
            },
            stepCardBgColor = if (currentPhase == HeartAttackPhase.MONITORING) {
                Color.rgb(34, 139, 34)
            } else {
                Color.rgb(194, 65, 12)
            },
            stepCardIcon = "🫀",
            emergencyType = EmergencyType.HEART_ATTACK,
            phaseProgress = progress
        )

        return ModuleResult(voiceText, overlay, false)
    }

    override fun reset() {
        startMs = -1L
        currentPhase = HeartAttackPhase.SIT_DOWN
        lastSpokenPhase = null
    }

    private fun phaseForElapsed(elapsedMs: Long): HeartAttackPhase {
        return PHASE_START_MS.entries
            .filter { it.value <= elapsedMs }
            .maxByOrNull { it.value }
            ?.key ?: HeartAttackPhase.SIT_DOWN
    }

    private fun progressInPhase(elapsedMs: Long): Float {
        val phases = HeartAttackPhase.values()
        val idx = phases.indexOf(currentPhase)
        val start = PHASE_START_MS[currentPhase] ?: 0L
        val end = if (idx + 1 < phases.size) PHASE_START_MS[phases[idx + 1]] ?: 60_000L else 60_000L
        val duration = (end - start).toFloat()
        return if (duration <= 0f) 1f else ((elapsedMs - start).toFloat() / duration).coerceIn(0f, 1f)
    }

    private fun overlayText(phase: HeartAttackPhase): String = when (phase) {
        HeartAttackPhase.SIT_DOWN        -> "Sit/lie down"
        HeartAttackPhase.LOOSEN_CLOTHING -> "Loosen clothing"
        HeartAttackPhase.ASPIRIN_PROMPT  -> "Give aspirin"
        HeartAttackPhase.MONITORING      -> "✓ Monitoring"
    }

    private fun phaseTitle(phase: HeartAttackPhase): String = when (phase) {
        HeartAttackPhase.SIT_DOWN        -> "1. Sit Down"
        HeartAttackPhase.LOOSEN_CLOTHING -> "2. Loosen Clothing"
        HeartAttackPhase.ASPIRIN_PROMPT  -> "3. Aspirin Check"
        HeartAttackPhase.MONITORING      -> "4. Monitor"
    }

}
