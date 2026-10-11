package com.sanjeevani.training

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

interface CprSkillStore {
    fun saveSession(session: CprSkillSession)
    fun loadSessions(): List<CprSkillSession>
    fun clearAllSessions()
}

class SharedPrefsCprSkillStore(context: Context) : CprSkillStore {

    private val prefs = context.getSharedPreferences("cpr_skill_sessions", Context.MODE_PRIVATE)
    private val KEY   = "sessions"
    private val MAX   = 30

    override fun saveSession(session: CprSkillSession) {
        val arr = loadRaw()
        val updated = JSONArray()
        updated.put(session.toJson())
        for (i in 0 until minOf(arr.length(), MAX - 1)) updated.put(arr.get(i))
        prefs.edit().putString(KEY, updated.toString()).apply()
    }

    override fun loadSessions(): List<CprSkillSession> {
        val arr = loadRaw()
        return (0 until arr.length()).mapNotNull { arr.getJSONObject(it).toSessionOrNull() }
    }

    override fun clearAllSessions() {
        prefs.edit().remove(KEY).apply()
    }

    private fun loadRaw(): JSONArray =
        try { JSONArray(prefs.getString(KEY, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
}

// ── JSON serialization ────────────────────────────────────────────────────

private fun CprSkillSession.toJson(): JSONObject = JSONObject().apply {
    put("sessionId",  sessionId)
    put("startMs",    startMs)
    put("endMs",      endMs)
    put("isDemoData", isDemoData)
    put("attempts",   JSONArray().also { arr -> attempts.forEach { arr.put(it.toJson()) } })
}

private fun CprSkillAttempt.toJson(): JSONObject = JSONObject().apply {
    put("attemptId",              attemptId)
    put("sessionId",              sessionId)
    put("skillTag",               skillTag)
    put("skillDescription",       skillDescription)
    put("stepIndex",              stepIndex)
    put("questionId",             questionId)
    put("selectedOptionId",       selectedOptionId ?: JSONObject.NULL)
    put("correctOptionId",        correctOptionId)
    put("knowledgeResult",        knowledgeResult.name)
    put("handPlacementResult",    handPlacementResult.name)
    put("cameraObservationNote",  cameraObservationNote)
    put("outcome",                outcome.name)
    put("feedbackShown",          feedbackShown)
    put("learnerCorrected",       learnerCorrected)
    put("reassessmentOutcome",    reassessmentOutcome?.name ?: JSONObject.NULL)
    put("timestampMs",            timestampMs)
    put("protocolVersion",        protocolVersion)
}

private fun JSONObject.toSessionOrNull(): CprSkillSession? = try {
    CprSkillSession(
        sessionId  = getString("sessionId"),
        startMs    = getLong("startMs"),
        endMs      = getLong("endMs"),
        isDemoData = optBoolean("isDemoData", false),
        attempts   = run {
            val arr = optJSONArray("attempts") ?: JSONArray()
            (0 until arr.length()).mapNotNull { arr.getJSONObject(it).toAttemptOrNull() }
        }
    )
} catch (_: Exception) { null }

private fun JSONObject.toAttemptOrNull(): CprSkillAttempt? = try {
    CprSkillAttempt(
        attemptId             = getString("attemptId"),
        sessionId             = getString("sessionId"),
        skillTag              = getString("skillTag"),
        skillDescription      = getString("skillDescription"),
        stepIndex             = getInt("stepIndex"),
        questionId            = getString("questionId"),
        selectedOptionId      = if (isNull("selectedOptionId")) null else getString("selectedOptionId"),
        correctOptionId       = getString("correctOptionId"),
        knowledgeResult       = KnowledgeResult.valueOf(getString("knowledgeResult")),
        handPlacementResult   = HandPlacementResult.valueOf(optString("handPlacementResult", HandPlacementResult.NOT_OBSERVED.name)),
        cameraObservationNote = optString("cameraObservationNote", ""),
        outcome               = SkillAttemptOutcome.valueOf(getString("outcome")),
        feedbackShown         = optString("feedbackShown", ""),
        learnerCorrected      = optBoolean("learnerCorrected", false),
        reassessmentOutcome   = optString("reassessmentOutcome", "").takeIf { it.isNotEmpty() }?.let {
            runCatching { SkillAttemptOutcome.valueOf(it) }.getOrNull()
        },
        timestampMs           = getLong("timestampMs"),
        protocolVersion       = optString("protocolVersion", "RCUK-BLS-2021")
    )
} catch (_: Exception) { null }
