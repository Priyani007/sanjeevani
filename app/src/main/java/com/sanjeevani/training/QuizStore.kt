package com.sanjeevani.training

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

interface QuizStore {
    fun saveAttempt(attempt: QuizAttempt)
    fun loadAttempts(category: QuizCategory): List<QuizAttempt>
    fun loadAllAttempts(): List<QuizAttempt>
    fun clearAllAttempts()
}

class SharedPrefsQuizStore(context: Context) : QuizStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sanjeevani_quiz", Context.MODE_PRIVATE)

    override fun saveAttempt(attempt: QuizAttempt) {
        val key = keyFor(attempt.category)
        val existing = loadRaw(key).toMutableList()
        existing.add(attempt.toJson())
        // Keep last 50 per category
        val trimmed = if (existing.size > 50) existing.takeLast(50) else existing
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    override fun loadAttempts(category: QuizCategory): List<QuizAttempt> =
        loadRaw(keyFor(category)).mapNotNull { it.toAttemptOrNull() }

    override fun loadAllAttempts(): List<QuizAttempt> =
        QuizCategory.values().flatMap { loadAttempts(it) }.sortedBy { it.startMs }

    override fun clearAllAttempts() {
        val editor = prefs.edit()
        QuizCategory.values().forEach { editor.remove(keyFor(it)) }
        editor.apply()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun keyFor(category: QuizCategory) = "quiz_attempts_${category.name}"

    private fun loadRaw(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }
}

// ── JSON serialisation helpers ────────────────────────────────────────────────

private fun QuizAttempt.toJson(): JSONObject = JSONObject().apply {
    put("attemptId", attemptId)
    put("startMs", startMs)
    put("endMs", endMs)
    put("category", category.name)
    put("difficulty", difficulty.name)
    put("isDemoData", isDemoData)
    put("answers", JSONArray().also { arr -> answers.forEach { arr.put(it.toJson()) } })
}

private fun QuizAnswerRecord.toJson(): JSONObject = JSONObject().apply {
    put("questionId", questionId)
    put("category", category.name)
    put("selectedOptionIds", JSONArray(selectedOptionIds.toList()))
    put("correctOptionIds", JSONArray(correctOptionIds.toList()))
    put("isCorrect", isCorrect)
    put("skillTags", JSONArray(skillTags))
    put("isCriticalSafety", isCriticalSafety)
    put("timestampMs", timestampMs)
    put("isDemoData", isDemoData)
}

private fun JSONObject.toAttemptOrNull(): QuizAttempt? = try {
    val answersArr = getJSONArray("answers")
    val answers = (0 until answersArr.length()).mapNotNull {
        answersArr.getJSONObject(it).toAnswerOrNull()
    }
    QuizAttempt(
        attemptId  = getString("attemptId"),
        startMs    = getLong("startMs"),
        endMs      = getLong("endMs"),
        category   = QuizCategory.valueOf(getString("category")),
        difficulty = QuizDifficulty.valueOf(getString("difficulty")),
        answers    = answers,
        isDemoData = optBoolean("isDemoData", false)
    )
} catch (_: Exception) { null }

private fun JSONObject.toAnswerOrNull(): QuizAnswerRecord? = try {
    fun JSONArray.toStringSet(): Set<String> =
        (0 until length()).map { getString(it) }.toSet()
    fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    QuizAnswerRecord(
        questionId       = getString("questionId"),
        category         = QuizCategory.valueOf(getString("category")),
        selectedOptionIds = getJSONArray("selectedOptionIds").toStringSet(),
        correctOptionIds  = getJSONArray("correctOptionIds").toStringSet(),
        isCorrect        = getBoolean("isCorrect"),
        skillTags        = getJSONArray("skillTags").toStringList(),
        isCriticalSafety = getBoolean("isCriticalSafety"),
        timestampMs      = getLong("timestampMs"),
        isDemoData       = optBoolean("isDemoData", false)
    )
} catch (_: Exception) { null }
