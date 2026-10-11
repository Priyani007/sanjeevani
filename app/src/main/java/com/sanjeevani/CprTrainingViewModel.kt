package com.sanjeevani

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanjeevani.model.SpatialAction
import com.sanjeevani.training.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Represents the state of one active CPR training step
data class CprStepState(
    val step: CprTrainingStep,
    val selectedOptionId: String? = null,
    val locked: Boolean = false,           // true after Submit
    val attempt: CprSkillAttempt? = null   // set when locked
)

// Phase of the training session
enum class CprTrainingPhase { STEP, AWAITING_CAMERA, COMPLETE }

data class CprTrainingSessionState(
    val stepIndex: Int          = 0,
    val phase: CprTrainingPhase = CprTrainingPhase.STEP,
    val stepState: CprStepState? = null,
    val latestSpatialAction: SpatialAction? = null,
    val completedAttempts: List<CprSkillAttempt> = emptyList(),
    val sessionId: String = java.util.UUID.randomUUID().toString()
) {
    val totalSteps: Int get() = CprTrainingContent.steps.size
    val isFinished: Boolean get() = phase == CprTrainingPhase.COMPLETE
    val currentStep: CprTrainingStep? get() = CprTrainingContent.stepFor(stepIndex)
    val accuracy: Float get() {
        val assessed = completedAttempts.filter { it.outcome != SkillAttemptOutcome.NOT_ASSESSED }
        return if (assessed.isEmpty()) 0f
        else assessed.count { it.outcome == SkillAttemptOutcome.CORRECT }.toFloat() / assessed.size
    }
}

class CprTrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SharedPrefsCprSkillStore(application)

    private val _session   = MutableStateFlow<CprTrainingSessionState?>(null)
    val session: StateFlow<CprTrainingSessionState?> = _session.asStateFlow()

    private val _profile   = MutableStateFlow(CprTrainingProfile())
    val profile: StateFlow<CprTrainingProfile> = _profile.asStateFlow()

    private val _recommendations = MutableStateFlow<List<CprFocusedRecommendation>>(emptyList())
    val recommendations: StateFlow<List<CprFocusedRecommendation>> = _recommendations.asStateFlow()

    private val _isDemoLoaded = MutableStateFlow(false)
    val isDemoLoaded: StateFlow<Boolean> = _isDemoLoaded.asStateFlow()

    init { refreshProfile() }

    // ── Session lifecycle ──────────────────────────────────────────────────

    fun startTraining() {
        val firstStep = CprTrainingContent.stepFor(0) ?: return
        _session.value = CprTrainingSessionState(
            stepIndex = 0,
            phase     = CprTrainingPhase.STEP,
            stepState = CprStepState(firstStep)
        )
    }

    fun selectOption(optionId: String) {
        val s = _session.value ?: return
        val ss = s.stepState ?: return
        if (ss.locked) return
        _session.value = s.copy(stepState = ss.copy(selectedOptionId = optionId))
    }

    // Returns true if the answer was submitted, false if nothing selected
    fun submitAnswer(): Boolean {
        val s  = _session.value ?: return false
        val ss = s.stepState ?: return false
        val selectedId = ss.selectedOptionId ?: return false
        if (ss.locked) return false

        val step  = ss.step
        val phase = if (step.useCameraForAssessment) CprTrainingPhase.AWAITING_CAMERA
                    else CprTrainingPhase.STEP

        // If no camera, evaluate immediately; camera steps evaluated after camera phase
        val attempt = if (!step.useCameraForAssessment) {
            CprSkillAssessmentEngine.evaluate(step, s.sessionId, selectedId, null)
        } else null

        _session.value = s.copy(
            phase     = phase,
            stepState = ss.copy(locked = true, attempt = attempt)
        )
        return true
    }

    // Called from camera pipeline with latest SpatialAction while on a camera step
    fun onSpatialAction(action: SpatialAction) {
        val s = _session.value ?: return
        if (s.phase != CprTrainingPhase.AWAITING_CAMERA) return
        _session.value = s.copy(latestSpatialAction = action)
    }

    // Commit camera observation and build the attempt for camera steps
    fun commitCameraObservation() {
        val s  = _session.value ?: return
        val ss = s.stepState ?: return
        if (s.phase != CprTrainingPhase.AWAITING_CAMERA) return
        val selectedId = ss.selectedOptionId ?: return

        val attempt = CprSkillAssessmentEngine.evaluate(
            step            = ss.step,
            sessionId       = s.sessionId,
            selectedOptionId = selectedId,
            spatialAction   = s.latestSpatialAction
        )
        _session.value = s.copy(
            phase     = CprTrainingPhase.STEP,
            stepState = ss.copy(attempt = attempt)
        )
    }

    // Skip camera observation (camera unavailable or learner opts out)
    fun skipCameraObservation() {
        val s  = _session.value ?: return
        val ss = s.stepState ?: return
        if (s.phase != CprTrainingPhase.AWAITING_CAMERA) return
        val selectedId = ss.selectedOptionId ?: return

        val attempt = CprSkillAssessmentEngine.evaluate(
            step            = ss.step,
            sessionId       = s.sessionId,
            selectedOptionId = selectedId,
            spatialAction   = null
        )
        _session.value = s.copy(
            phase     = CprTrainingPhase.STEP,
            stepState = ss.copy(attempt = attempt)
        )
    }

    fun nextStep() {
        val s  = _session.value ?: return
        val ss = s.stepState ?: return
        val attempt = ss.attempt ?: return

        val updated = s.completedAttempts + attempt
        val nextIdx = s.stepIndex + 1

        if (nextIdx >= CprTrainingContent.steps.size) {
            // Session complete — save
            val finishedSession = CprSkillSession(
                sessionId = s.sessionId,
                startMs   = System.currentTimeMillis() - (updated.size * 45_000L),
                endMs     = System.currentTimeMillis(),
                attempts  = updated,
                isDemoData = false
            )
            viewModelScope.launch {
                store.saveSession(finishedSession)
                refreshProfile()
            }
            _session.value = s.copy(
                completedAttempts = updated,
                phase             = CprTrainingPhase.COMPLETE
            )
        } else {
            val nextStep = CprTrainingContent.stepFor(nextIdx)!!
            _session.value = s.copy(
                stepIndex         = nextIdx,
                phase             = CprTrainingPhase.STEP,
                stepState         = CprStepState(nextStep),
                latestSpatialAction = null,
                completedAttempts = updated
            )
        }
    }

    fun dismissSession() { _session.value = null }

    // ── Demo data ──────────────────────────────────────────────────────────

    fun loadDemoData() {
        viewModelScope.launch {
            val demo = CprDemoData.generateSessions()
            demo.forEach { store.saveSession(it) }
            _isDemoLoaded.value = true
            refreshProfile()
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            store.clearAllSessions()
            _isDemoLoaded.value = false
            refreshProfile()
        }
    }

    // ── Profile refresh ────────────────────────────────────────────────────

    private fun refreshProfile() {
        val sessions = store.loadSessions()
        val p = CprTrainingProfile(sessions)
        _profile.value = p
        _recommendations.value = CprSkillAssessmentEngine.recommend(p, demoMode = _isDemoLoaded.value)
    }
}
