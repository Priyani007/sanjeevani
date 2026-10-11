package com.sanjeevani.training

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

interface ModuleSkillStore {
    fun saveSession(session: ModuleTrainingSession)
    fun loadSessions(): List<ModuleTrainingSession>
    fun clearAllSessions()
}

class SharedPrefsModuleSkillStore(context: Context, moduleKey: String) : ModuleSkillStore {

    private val prefs = context.getSharedPreferences("module_skill_${moduleKey}", Context.MODE_PRIVATE)
    private val KEY   = "sessions"
    private val MAX   = 30

    override fun saveSession(session: ModuleTrainingSession) {
        val arr     = loadRaw()
        val updated = JSONArray()
        updated.put(session.toJson())
        var retained = 0
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optString("sessionId") == session.sessionId) continue
            if (retained++ >= MAX - 1) break
            updated.put(item)
        }
        // Assessment evidence is small and safety-relevant; commit before reporting success.
        prefs.edit().putString(KEY, updated.toString()).commit()
    }

    override fun loadSessions(): List<ModuleTrainingSession> {
        val arr = loadRaw()
        return (0 until arr.length()).mapNotNull { arr.getJSONObject(it).toSessionOrNull() }
    }

    override fun clearAllSessions() {
        prefs.edit().remove(KEY).commit()
    }

    private fun loadRaw(): JSONArray =
        try { JSONArray(prefs.getString(KEY, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
}

// ── JSON serialization ────────────────────────────────────────────────────────

private fun ModuleTrainingSession.toJson(): JSONObject = JSONObject().apply {
    put("sessionId",   sessionId)
    put("moduleType",  moduleType)
    put("startMs",     startMs)
    put("endMs",       endMs)
    put("isDemoData",  isDemoData)
    put("focusedSkillTag", focusedSkillTag ?: JSONObject.NULL)
    put("contentVersion", contentVersion)
    put("attempts",    JSONArray().also { arr -> attempts.forEach { arr.put(it.toJson()) } })
}

private fun ModuleTrainingAttempt.toJson(): JSONObject = JSONObject().apply {
    put("attemptId",        attemptId)
    put("sessionId",        sessionId)
    put("moduleType",       moduleType)
    put("skillTag",         skillTag)
    put("skillDescription", skillDescription)
    put("stepIndex",        stepIndex)
    put("questionId",       questionId)
    put("selectedOptionId", selectedOptionId ?: JSONObject.NULL)
    put("correctOptionId",  correctOptionId)
    put("knowledgeResult",  knowledgeResult.name)
    put("outcome",          outcome.name)
    put("feedbackShown",    feedbackShown)
    put("assessmentMethod", assessmentMethod.name)
    put("learnerResponse", learnerResponse ?: JSONObject.NULL)
    put("errorCategory", errorCategory ?: JSONObject.NULL)
    put("confidence", confidence ?: JSONObject.NULL)
    put("observationQuality", observationQuality.name)
    put("retryCount", retryCount)
    put("correctionStatus", correctionStatus.name)
    put("placementResult", placementResult.name)
    put("eventType", eventType)
    put("observationType", observationType)
    put("observedMeasurements", observedMeasurements ?: JSONObject.NULL)
    put("correctionDetected", correctionDetected)
    put("manuallyAdvanced", manuallyAdvanced)
    put("trackingModelVersion", trackingModelVersion ?: JSONObject.NULL)
    put("contentVersion", contentVersion)
    put("dataProvenance", dataProvenance)
    put("timestampMs",      timestampMs)
}

private fun JSONObject.toSessionOrNull(): ModuleTrainingSession? = try {
    ModuleTrainingSession(
        sessionId  = getString("sessionId"),
        moduleType = optString("moduleType", ""),
        startMs    = getLong("startMs"),
        endMs      = getLong("endMs"),
        isDemoData = optBoolean("isDemoData", false),
        focusedSkillTag = if (isNull("focusedSkillTag")) null else optString("focusedSkillTag"),
        contentVersion = optString("contentVersion", MODULE_CONTENT_VERSION),
        attempts   = run {
            val arr = optJSONArray("attempts") ?: JSONArray()
            (0 until arr.length()).mapNotNull { arr.getJSONObject(it).toAttemptOrNull() }
        }
    )
} catch (_: Exception) { null }

private fun JSONObject.toAttemptOrNull(): ModuleTrainingAttempt? = try {
    ModuleTrainingAttempt(
        attemptId        = getString("attemptId"),
        sessionId        = getString("sessionId"),
        moduleType       = optString("moduleType", ""),
        skillTag         = getString("skillTag"),
        skillDescription = getString("skillDescription"),
        stepIndex        = getInt("stepIndex"),
        questionId       = getString("questionId"),
        selectedOptionId = if (isNull("selectedOptionId")) null else getString("selectedOptionId"),
        correctOptionId  = getString("correctOptionId"),
        knowledgeResult  = KnowledgeResult.valueOf(getString("knowledgeResult")),
        outcome          = SkillAttemptOutcome.valueOf(getString("outcome")),
        feedbackShown    = optString("feedbackShown", ""),
        assessmentMethod = enumOrDefault(optString("assessmentMethod"), AssessmentMethod.BUTTON),
        learnerResponse  = if (isNull("learnerResponse")) null else optString("learnerResponse"),
        errorCategory    = if (isNull("errorCategory")) null else optString("errorCategory"),
        confidence       = if (isNull("confidence")) null else optDouble("confidence").toFloat(),
        observationQuality = enumOrDefault(optString("observationQuality"), ObservationQuality.NOT_APPLICABLE),
        retryCount       = optInt("retryCount", 0),
        correctionStatus = enumOrDefault(optString("correctionStatus"), CorrectionStatus.NOT_REQUIRED),
        placementResult  = enumOrDefault(optString("placementResult"), PlacementResult.NOT_ASSESSED),
        eventType        = optString("eventType", "assessment"),
        observationType  = optString("observationType", "knowledge_response"),
        observedMeasurements = if (isNull("observedMeasurements")) null else optString("observedMeasurements"),
        correctionDetected = optBoolean("correctionDetected", false),
        manuallyAdvanced = optBoolean("manuallyAdvanced", false),
        trackingModelVersion = if (isNull("trackingModelVersion")) null else optString("trackingModelVersion"),
        contentVersion   = optString("contentVersion", MODULE_CONTENT_VERSION),
        dataProvenance   = optString("dataProvenance", "learner_response"),
        timestampMs      = getLong("timestampMs")
    )
} catch (_: Exception) { null }

private inline fun <reified T : Enum<T>> enumOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
