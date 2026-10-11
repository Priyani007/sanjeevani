package com.sanjeevani

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sanjeevani.training.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuizViewModel(app: Application) : AndroidViewModel(app) {

    private val store: QuizStore = SharedPrefsQuizStore(app)

    // ── Quiz session state ────────────────────────────────────────────────────

    private val _activeSession = MutableStateFlow<ActiveQuizSession?>(null)
    val activeSession: StateFlow<ActiveQuizSession?> = _activeSession.asStateFlow()

    // ── Learner profile (all attempts: real + demo) ───────────────────────────

    private val _profile = MutableStateFlow(QuizLearnerProfile())
    val profile: StateFlow<QuizLearnerProfile> = _profile.asStateFlow()

    private val _recommendations = MutableStateFlow<List<QuizRecommendation>>(emptyList())
    val recommendations: StateFlow<List<QuizRecommendation>> = _recommendations.asStateFlow()

    private val _isDemoDataLoaded = MutableStateFlow(false)
    val isDemoDataLoaded: StateFlow<Boolean> = _isDemoDataLoaded.asStateFlow()

    // ── Last completed attempt score ──────────────────────────────────────────

    private val _lastScore = MutableStateFlow<QuizAttemptScore?>(null)
    val lastScore: StateFlow<QuizAttemptScore?> = _lastScore.asStateFlow()

    init {
        refreshProfile()
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    fun startQuiz(category: QuizCategory, difficulty: QuizDifficulty) {
        val questions = QuizQuestionBank.questionsForSession(category, difficulty, maxCount = 7)
        _activeSession.value = ActiveQuizSession(
            category   = category,
            difficulty = difficulty,
            questions  = questions
        )
        _lastScore.value = null
    }

    fun selectOption(optionId: String) {
        val session = _activeSession.value ?: return
        if (session.isAnswerLocked) return
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(
            selectedOptionIds = if (optionId in current.selectedOptionIds)
                current.selectedOptionIds - optionId
            else
                current.selectedOptionIds + optionId
        )
    }

    /** Lock the current answer and reveal feedback. Returns false if nothing selected. */
    fun submitAnswer(): Boolean {
        val session = _activeSession.value ?: return false
        val question = session.currentQuestion ?: return false
        if (session.selectedOptionIds.isEmpty()) return false
        if (session.isAnswerLocked) return false

        val record = QuizAssessmentEngine.evaluateAnswer(
            question          = question,
            selectedOptionIds = session.selectedOptionIds,
            existingAnswers   = session.answers,
            isDemoData        = false
        ) ?: return false

        _activeSession.update { s ->
            s?.copy(
                answers          = s.answers + record,
                isAnswerLocked   = true
            )
        }
        return true
    }

    /** Advance to the next question, or finish the quiz. */
    fun nextQuestion() {
        val session = _activeSession.value ?: return
        val nextIndex = session.currentIndex + 1
        if (nextIndex >= session.questions.size) {
            finishQuiz()
        } else {
            _activeSession.update { s ->
                s?.copy(
                    currentIndex      = nextIndex,
                    selectedOptionIds = emptySet(),
                    isAnswerLocked    = false
                )
            }
        }
    }

    private fun finishQuiz() {
        val session = _activeSession.value ?: return
        val attempt = QuizAssessmentEngine.buildAttempt(session, isDemoData = false)
        store.saveAttempt(attempt)
        _lastScore.value = QuizAssessmentEngine.score(attempt)
        _activeSession.value = session.copy(currentIndex = session.questions.size) // mark complete
        refreshProfile()
    }

    fun dismissSession() {
        _activeSession.value = null
    }

    // ── Demo data ─────────────────────────────────────────────────────────────

    fun loadDemoData() {
        store.clearAllAttempts()
        QuizDemoData.generateAttempts().forEach { store.saveAttempt(it) }
        _isDemoDataLoaded.value = true
        refreshProfile()
    }

    fun clearAllAttempts() {
        store.clearAllAttempts()
        _isDemoDataLoaded.value = false
        refreshProfile()
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun refreshProfile() {
        val attempts = store.loadAllAttempts()
        val profile = QuizLearnerProfile(attempts)
        _profile.value = profile
        val demoMode = _isDemoDataLoaded.value || attempts.any { it.isDemoData }
        _recommendations.value = QuizRecommendationEngine.recommend(profile, demoMode = demoMode)
        if (attempts.any { it.isDemoData }) _isDemoDataLoaded.value = true
    }
}
