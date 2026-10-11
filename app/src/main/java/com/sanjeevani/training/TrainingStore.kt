package com.sanjeevani.training

import android.content.Context
import android.content.SharedPreferences

interface TrainingStore {
    fun saveSession(outcome: SessionOutcome)
    fun loadSessions(module: TrainingModuleType): List<SessionOutcome>
    fun loadAllSessions(): Map<TrainingModuleType, List<SessionOutcome>>
    fun clearAllSessions()
}

class InMemoryTrainingStore : TrainingStore {
    private val data = mutableMapOf<TrainingModuleType, MutableList<SessionOutcome>>()

    override fun saveSession(outcome: SessionOutcome) {
        data.getOrPut(outcome.moduleType) { mutableListOf() }.add(outcome)
    }

    override fun loadSessions(module: TrainingModuleType): List<SessionOutcome> =
        data[module]?.toList() ?: emptyList()

    override fun loadAllSessions(): Map<TrainingModuleType, List<SessionOutcome>> =
        data.mapValues { it.value.toList() }

    override fun clearAllSessions() { data.clear() }
}

class SharedPreferencesTrainingStore(context: Context) : TrainingStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sanjeevani_training", Context.MODE_PRIVATE)

    override fun saveSession(outcome: SessionOutcome) {
        val key = prefKey(outcome.moduleType)
        val existing = loadSessions(outcome.moduleType).toMutableList()
        existing.add(outcome)
        // Keep last 20 sessions per module
        val trimmed = if (existing.size > 20) existing.takeLast(20) else existing
        prefs.edit().putString(key, encodeSessions(trimmed)).apply()
    }

    override fun loadSessions(module: TrainingModuleType): List<SessionOutcome> {
        val raw = prefs.getString(prefKey(module), null) ?: return emptyList()
        return try { decodeSessions(raw, module) } catch (_: Exception) { emptyList() }
    }

    override fun loadAllSessions(): Map<TrainingModuleType, List<SessionOutcome>> =
        TrainingModuleType.values().associateWith { loadSessions(it) }

    override fun clearAllSessions() {
        TrainingModuleType.values().forEach { prefs.edit().remove(prefKey(it)).apply() }
    }

    private fun prefKey(module: TrainingModuleType) = "sessions_${module.name}"

    // Simple pipe-delimited encoding — no external JSON library needed
    // Session line: id|module|startMs|endMs|notes
    // Criterion line: key|result|evidence
    // Sessions separated by "==="; criteria within a session by "---"
    private fun encodeSessions(sessions: List<SessionOutcome>): String =
        sessions.joinToString("===") { s ->
            val header = listOf(s.sessionId, s.moduleType.name, s.startMs, s.endMs, s.notes.replace("|", " "))
                .joinToString("|")
            val criteria = s.criteria.joinToString("---") { c ->
                listOf(c.key.key, c.result.name, c.evidence.replace("|", " ").replace("---", " "), c.timestampMs)
                    .joinToString("|")
            }
            "$header\n$criteria"
        }

    private fun decodeSessions(raw: String, module: TrainingModuleType): List<SessionOutcome> {
        if (raw.isBlank()) return emptyList()
        return raw.split("===").mapNotNull { block ->
            try {
                val lines = block.trim().split("\n", limit = 2)
                val parts = lines[0].split("|")
                val id = parts[0]
                val moduleType = TrainingModuleType.valueOf(parts[1])
                val startMs = parts[2].toLong()
                val endMs = parts[3].toLong()
                val notes = if (parts.size > 4) parts[4] else ""
                val criteria = if (lines.size > 1 && lines[1].isNotBlank()) {
                    lines[1].split("---").mapNotNull { cLine ->
                        try {
                            val cp = cLine.split("|")
                            val criterionKey = findCriterionKey(moduleType, cp[0]) ?: return@mapNotNull null
                            val result = CriterionResult.valueOf(cp[1])
                            val evidence = if (cp.size > 2) cp[2] else ""
                            val ts = if (cp.size > 3) cp[3].toLong() else endMs
                            CriterionRecord(criterionKey, result, evidence, ts)
                        } catch (_: Exception) { null }
                    }
                } else emptyList()
                SessionOutcome(id, moduleType, startMs, endMs, criteria, notes)
            } catch (_: Exception) { null }
        }
    }

    private fun findCriterionKey(module: TrainingModuleType, key: String): CriterionKey? {
        val allKeys = when (module) {
            TrainingModuleType.CPR -> listOf(
                AssessmentEngine.CPR_CALL_112, AssessmentEngine.CPR_POSITIONING,
                AssessmentEngine.CPR_HAND_PLACEMENT, AssessmentEngine.CPR_COMPRESSIONS_STARTED,
                AssessmentEngine.CPR_COMPRESSION_RATE, AssessmentEngine.CPR_THIRTY_CYCLES
            )
            TrainingModuleType.STROKE_FAST -> listOf(
                AssessmentEngine.STROKE_FACE, AssessmentEngine.STROKE_ARM,
                AssessmentEngine.STROKE_SPEECH, AssessmentEngine.STROKE_EMERGENCY_CALL
            )
            TrainingModuleType.ALLERGIC_REACTION -> listOf(
                AssessmentEngine.ALLERGY_LAY_FLAT, AssessmentEngine.ALLERGY_EPIPEN_PREP,
                AssessmentEngine.ALLERGY_EPIPEN_INJECT, AssessmentEngine.ALLERGY_MONITORING
            )
        }
        return allKeys.find { it.key == key }
    }
}
