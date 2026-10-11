package com.sanjeevani

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.sanjeevani.training.*
import com.sanjeevani.model.TrainingSessionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class ModuleTrainingPhase { STEP, PLACEMENT, COMPLETE }

data class ModuleStepState(
    val step: ModuleTrainingStep,
    val selectedOptionId: String? = null,
    val locked: Boolean = false,
    val attempt: ModuleTrainingAttempt? = null,
    val retryCount: Int = 0,
    val placementX: Float? = null,
    val placementY: Float? = null,
    val cameraVerified: Boolean = false,
    val cameraStatus: String? = null
) {
    val canAdvance: Boolean get() = attempt?.outcome == SkillAttemptOutcome.CORRECT &&
        (!step.requiresPlacementSimulation || attempt.placementResult == PlacementResult.CORRECT)
}

data class ModuleTrainingSessionState(
    val moduleTypeName: String,
    val stepIndex: Int = 0,
    val phase: ModuleTrainingPhase = ModuleTrainingPhase.STEP,
    val stepState: ModuleStepState? = null,
    val completedAttempts: List<ModuleTrainingAttempt> = emptyList(),
    val sessionId: String = UUID.randomUUID().toString(),
    val startMs: Long = System.currentTimeMillis(),
    val focusedSkillTag: String? = null
) {
    private val allSteps get() = when (moduleTypeName) {
        "STROKE_FAST" -> StrokeTrainingContent.steps
        "ALLERGIC_REACTION" -> AllergyTrainingContent.steps
        else -> emptyList()
    }
    val sessionSteps get() = focusedSkillTag?.let { tag -> allSteps.filter { it.skillTag == tag } } ?: allSteps
    val totalSteps get() = sessionSteps.size
    val isFinished get() = phase == ModuleTrainingPhase.COMPLETE
    val accuracy: Float get() {
        val latest = completedAttempts.groupBy { it.skillTag }.values.mapNotNull { it.maxByOrNull { a -> a.timestampMs } }
        return if (latest.isEmpty()) 0f else latest.count { it.outcome == SkillAttemptOutcome.CORRECT }.toFloat() / latest.size
    }
}

data class ModuleVoiceState(
    val moduleTypeName: String? = null,
    val isListening: Boolean = false,
    val message: String = "Say option A, B, C, or D"
)

class ModuleTrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val strokeStore = SharedPrefsModuleSkillStore(application, "stroke")
    private val allergyStore = SharedPrefsModuleSkillStore(application, "allergy")
    private val _strokeSession = MutableStateFlow<ModuleTrainingSessionState?>(null)
    val strokeSession: StateFlow<ModuleTrainingSessionState?> = _strokeSession.asStateFlow()
    private val _allergySession = MutableStateFlow<ModuleTrainingSessionState?>(null)
    val allergySession: StateFlow<ModuleTrainingSessionState?> = _allergySession.asStateFlow()
    private val _strokeProfile = MutableStateFlow(ModuleTrainingProfile("STROKE_FAST"))
    val strokeProfile: StateFlow<ModuleTrainingProfile> = _strokeProfile.asStateFlow()
    private val _allergyProfile = MutableStateFlow(ModuleTrainingProfile("ALLERGIC_REACTION"))
    val allergyProfile: StateFlow<ModuleTrainingProfile> = _allergyProfile.asStateFlow()
    private val _strokeRecs = MutableStateFlow<List<ModuleFocusedRecommendation>>(emptyList())
    val strokeRecs: StateFlow<List<ModuleFocusedRecommendation>> = _strokeRecs.asStateFlow()
    private val _allergyRecs = MutableStateFlow<List<ModuleFocusedRecommendation>>(emptyList())
    val allergyRecs: StateFlow<List<ModuleFocusedRecommendation>> = _allergyRecs.asStateFlow()
    private val _isStrokeDemoLoaded = MutableStateFlow(false)
    val isStrokeDemoLoaded: StateFlow<Boolean> = _isStrokeDemoLoaded.asStateFlow()
    private val _isAllergyDemoLoaded = MutableStateFlow(false)
    val isAllergyDemoLoaded: StateFlow<Boolean> = _isAllergyDemoLoaded.asStateFlow()
    private val _voiceState = MutableStateFlow(ModuleVoiceState())
    val voiceState: StateFlow<ModuleVoiceState> = _voiceState.asStateFlow()
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechToken = 0L
    private var lastManualAdvanceMs = 0L

    init {
        refreshStrokeProfile(); refreshAllergyProfile()
        restoreDraft("STROKE_FAST"); restoreDraft("ALLERGIC_REACTION")
    }

    private fun stepsFor(module: String) = when (module) {
        "STROKE_FAST" -> StrokeTrainingContent.steps
        "ALLERGIC_REACTION" -> AllergyTrainingContent.steps
        else -> emptyList()
    }
    private fun storeFor(module: String) = when (module) {
        "STROKE_FAST" -> strokeStore
        "ALLERGIC_REACTION" -> allergyStore
        else -> null
    }
    private fun sessionFlow(module: String) = when (module) {
        "STROKE_FAST" -> _strokeSession
        "ALLERGIC_REACTION" -> _allergySession
        else -> null
    }

    fun startStrokeTraining() = startTraining("STROKE_FAST")
    fun startAllergyTraining() = startTraining("ALLERGIC_REACTION")
    fun startFocusedTraining(module: String, skillTag: String) = startTraining(module, skillTag, true)

    private fun startTraining(module: String, focusedSkillTag: String? = null, forceNew: Boolean = false) {
        val flow = sessionFlow(module) ?: return
        if (!forceNew && flow.value?.isFinished == false) return
        val first = (focusedSkillTag?.let { tag -> stepsFor(module).filter { it.skillTag == tag } } ?: stepsFor(module)).firstOrNull() ?: return
        flow.value = ModuleTrainingSessionState(moduleTypeName = module, stepState = ModuleStepState(first), focusedSkillTag = focusedSkillTag)
        persist(flow.value!!)
    }

    fun selectOption(module: String, optionId: String) {
        val flow = sessionFlow(module) ?: return
        val state = flow.value ?: return
        val step = state.stepState ?: return
        if (!step.locked && step.step.options.any { it.id == optionId }) flow.value = state.copy(stepState = step.copy(selectedOptionId = optionId))
    }

    fun submitAnswer(module: String, method: AssessmentMethod = AssessmentMethod.BUTTON, rawResponse: String? = null, confidence: Float? = null): Boolean {
        val flow = sessionFlow(module) ?: return false
        val state = flow.value ?: return false
        val ss = state.stepState ?: return false
        val selected = ss.selectedOptionId ?: return false
        if (ss.locked) return false
        var attempt = ModuleSkillAssessmentEngine.evaluate(ss.step, state.sessionId, selected, method, rawResponse ?: selected, confidence, ss.retryCount)
        val needsPlacement = attempt.outcome == SkillAttemptOutcome.CORRECT && ss.step.requiresPlacementSimulation
        if (needsPlacement) attempt = attempt.copy(
            outcome = SkillAttemptOutcome.INCONCLUSIVE,
            feedbackShown = "Knowledge answer correct. Complete the trainer-device placement simulation.",
            correctionStatus = CorrectionStatus.UNRESOLVED
        )
        flow.value = state.copy(
            phase = if (needsPlacement) ModuleTrainingPhase.PLACEMENT else ModuleTrainingPhase.STEP,
            stepState = ss.copy(locked = true, attempt = attempt),
            completedAttempts = state.completedAttempts + attempt
        )
        persist(flow.value!!); refresh(module)
        return true
    }

    fun retryStep(module: String) {
        val flow = sessionFlow(module) ?: return
        val state = flow.value ?: return
        val ss = state.stepState ?: return
        if (ss.canAdvance) return
        flow.value = state.copy(
            phase = ModuleTrainingPhase.STEP,
            stepState = ModuleStepState(
                ss.step,
                retryCount = ss.retryCount + 1,
                cameraVerified = ss.cameraVerified,
                cameraStatus = ss.cameraStatus
            )
        )
        _voiceState.value = ModuleVoiceState(module)
    }

    fun assessPlacement(module: String, x: Float, y: Float, confidence: Float = 1f) {
        val flow = sessionFlow(module) ?: return
        val state = flow.value ?: return
        val ss = state.stepState ?: return
        if (state.phase != ModuleTrainingPhase.PLACEMENT || !ss.step.requiresPlacementSimulation) return
        val result = AllergyPlacementAssessment.evaluate(x, y, confidence)
        val correct = result == PlacementResult.CORRECT
        val placementRetry = state.completedAttempts.count {
            it.stepIndex == ss.step.stepIndex && it.assessmentMethod == AssessmentMethod.SIMULATED_PLACEMENT
        }
        val attempt = ModuleTrainingAttempt(
            sessionId = state.sessionId, moduleType = module, skillTag = ss.step.skillTag,
            skillDescription = ss.step.skillDescription, stepIndex = ss.step.stepIndex,
            questionId = "${ss.step.skillTag}.placement", selectedOptionId = ss.selectedOptionId,
            correctOptionId = ss.step.correctOption.id, knowledgeResult = KnowledgeResult.CORRECT,
            outcome = if (correct) SkillAttemptOutcome.CORRECT else SkillAttemptOutcome.NEEDS_PRACTICE,
            feedbackShown = AllergyPlacementAssessment.feedback(result), assessmentMethod = AssessmentMethod.SIMULATED_PLACEMENT,
            learnerResponse = "x=${"%.3f".format(Locale.US, x)},y=${"%.3f".format(Locale.US, y)}",
            errorCategory = if (correct) null else result.name.lowercase(), confidence = confidence,
            observationQuality = if (confidence >= .8f) ObservationQuality.HIGH else ObservationQuality.MODERATE,
            retryCount = placementRetry,
            correctionStatus = if (correct && placementRetry > 0) CorrectionStatus.CORRECTED else if (correct) CorrectionStatus.NOT_REQUIRED else CorrectionStatus.RETRY_REQUIRED,
            placementResult = result,
            dataProvenance = "guided_diagram_simulation",
            observationType = "diagram_placement",
            observedMeasurements = "normalized_x=${"%.3f".format(Locale.US, x)},normalized_y=${"%.3f".format(Locale.US, y)}"
        )
        flow.value = state.copy(stepState = ss.copy(locked = true, attempt = attempt, placementX = x, placementY = y), completedAttempts = state.completedAttempts + attempt)
        persist(flow.value!!); refresh(module)
    }

    fun nextStep(module: String) {
        val flow = sessionFlow(module) ?: return
        val state = flow.value ?: return
        if (state.stepState?.canAdvance != true) return
        val next = state.stepIndex + 1
        if (next >= state.sessionSteps.size) {
            flow.value = state.copy(phase = ModuleTrainingPhase.COMPLETE)
            persist(flow.value!!, true); refresh(module)
        } else {
            flow.value = state.copy(stepIndex = next, phase = ModuleTrainingPhase.STEP, stepState = ModuleStepState(state.sessionSteps[next]))
            persist(flow.value!!)
        }
    }

    fun continueManually(module: String) {
        val now = System.currentTimeMillis()
        if (now - lastManualAdvanceMs < 750L) return
        val flow = sessionFlow(module) ?: return
        val state = flow.value ?: return
        val ss = state.stepState ?: return
        lastManualAdvanceMs = now
        stopVoiceAnswer()
        val manual = ModuleTrainingAttempt(
            sessionId = state.sessionId,
            moduleType = module,
            skillTag = ss.step.skillTag,
            skillDescription = ss.step.skillDescription,
            stepIndex = ss.step.stepIndex,
            questionId = "${ss.step.skillTag}.manual",
            selectedOptionId = ss.selectedOptionId,
            correctOptionId = ss.step.correctOption.id,
            knowledgeResult = KnowledgeResult.NOT_ATTEMPTED,
            outcome = SkillAttemptOutcome.INCONCLUSIVE,
            feedbackShown = "Advanced manually. This skill remains unconfirmed and is recommended for reassessment.",
            assessmentMethod = AssessmentMethod.MANUAL,
            learnerResponse = ss.selectedOptionId,
            errorCategory = "manually_advanced_unconfirmed",
            observationQuality = ObservationQuality.NOT_APPLICABLE,
            retryCount = ss.retryCount,
            correctionStatus = CorrectionStatus.UNRESOLVED,
            eventType = "manual_advance",
            observationType = if (ss.step.requiresPlacementSimulation) "guided_simulation_unconfirmed" else "knowledge_unconfirmed",
            manuallyAdvanced = true,
            dataProvenance = "learner_manual_override",
            timestampMs = now
        )
        val withEvent = state.copy(completedAttempts = state.completedAttempts + manual)
        val next = state.stepIndex + 1
        if (next >= state.sessionSteps.size) {
            flow.value = withEvent.copy(phase = ModuleTrainingPhase.COMPLETE, stepState = ss.copy(locked = true, attempt = manual))
            persist(flow.value!!, true)
        } else {
            flow.value = withEvent.copy(stepIndex = next, phase = ModuleTrainingPhase.STEP, stepState = ModuleStepState(state.sessionSteps[next]))
            persist(flow.value!!)
        }
        refresh(module)
    }

    fun recordSupportedCameraObservation(
        module: String,
        snapshot: TrainingSessionSnapshot,
        poseConfidence: Float,
        thighReferenceVisible: Boolean,
        patientLyingEstimate: Boolean = false
    ) {
        val flow = sessionFlow(module) ?: return
        val state = flow.value ?: return
        val ss = state.stepState ?: return
        val observation = when (ss.step.cameraRequirement) {
            ModuleCameraRequirement.FACE_REFERENCE_VISIBLE -> if (snapshot.strokeSignals.faceObservationFrames >= 5) {
                val ratio = snapshot.strokeSignals.faceAsymmetryScore
                Triple(
                    "face_mesh_mouth_symmetry",
                    if (ratio >= .5f) "possible_mouth_corner_asymmetry" else null,
                    "Face Mesh observed both mouth corners across ${snapshot.strokeSignals.faceObservationFrames} frames; score=${"%.2f".format(Locale.US, ratio)}. This cannot diagnose or rule out stroke."
                )
            } else return
            ModuleCameraRequirement.BOTH_ARMS_VISIBLE -> if (snapshot.strokeSignals.armObservationFrames >= 5) Triple(
                "pose_arm_height_comparison",
                if (snapshot.strokeSignals.armDriftDetected) "possible_arm_height_difference" else null,
                "Pose tracking observed both wrists and elbows across ${snapshot.strokeSignals.armObservationFrames} frames; height difference=${snapshot.strokeSignals.armDriftDetected}. This is a training observation, not a diagnosis."
            ) else return
            ModuleCameraRequirement.PATIENT_LYING_FLAT -> if (patientLyingEstimate && poseConfidence >= .65f) Triple(
                "pose_lying_position_estimate",
                null,
                "Pose tracking produced a stable lying-flat estimate. Confirm the real position and follow the dispatcher; the camera cannot verify the surface."
            ) else return
            ModuleCameraRequirement.THIGH_REFERENCE_VISIBLE -> if (thighReferenceVisible && poseConfidence >= .65f) Triple(
                "pose_thigh_reference",
                null,
                "Pose tracking found the hip-to-knee thigh reference. The injector itself is not tracked; placement remains a guided simulation."
            ) else return
            ModuleCameraRequirement.NONE -> return
        }
        val alreadyRecorded = state.completedAttempts.any {
                it.stepIndex == ss.step.stepIndex && it.eventType == "camera_observation" && it.observationType == observation.first
            }
        val verifiedStep = ss.copy(cameraVerified = true, cameraStatus = observation.third)
        if (alreadyRecorded) {
            if (!ss.cameraVerified) flow.value = state.copy(stepState = verifiedStep)
            return
        }

        val event = ModuleTrainingAttempt(
            sessionId = state.sessionId,
            moduleType = module,
            skillTag = ss.step.skillTag,
            skillDescription = ss.step.skillDescription,
            stepIndex = ss.step.stepIndex,
            questionId = "${ss.step.skillTag}.camera",
            selectedOptionId = null,
            correctOptionId = ss.step.correctOption.id,
            knowledgeResult = KnowledgeResult.NOT_ATTEMPTED,
            outcome = SkillAttemptOutcome.INCONCLUSIVE,
            feedbackShown = observation.third,
            assessmentMethod = AssessmentMethod.CAMERA_MEDIAPIPE,
            errorCategory = observation.second,
            confidence = poseConfidence.takeIf { module == "ALLERGIC_REACTION" || ss.step.stepIndex == 2 },
            observationQuality = if (poseConfidence >= .8f) ObservationQuality.HIGH else ObservationQuality.MODERATE,
            correctionStatus = CorrectionStatus.UNRESOLVED,
            eventType = "camera_observation",
            observationType = observation.first,
            observedMeasurements = observation.third,
            trackingModelVersion = if (observation.first.startsWith("face")) "MediaPipe FaceLandmarker/478" else "MediaPipe PoseLandmarker/33",
            dataProvenance = "live_camera_mediapipe"
        )
        flow.value = state.copy(stepState = verifiedStep, completedAttempts = state.completedAttempts + event)
        persist(flow.value!!)
        refresh(module)
    }

    fun dismissSession(module: String) { stopVoiceAnswer() }

    fun startVoiceAnswer(module: String) {
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _voiceState.value = ModuleVoiceState(module, false, "Microphone permission unavailable — tap an option instead."); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _voiceState.value = ModuleVoiceState(module, false, "Speech recognition unavailable — tap an option instead."); return
        }
        stopVoiceAnswer()
        val token = ++speechToken
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).also { recognizer ->
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { _voiceState.value = ModuleVoiceState(module, true, "Listening… say option A, B, C, or D") }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { if (token == speechToken) _voiceState.value = ModuleVoiceState(module, false, "Checking answer…") }
                override fun onError(error: Int) { if (token == speechToken) _voiceState.value = ModuleVoiceState(module, false, "I didn't catch that — try again or tap an option.") }
                override fun onResults(results: Bundle?) {
                    if (token != speechToken) return
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()?.takeIf { it >= 0f }
                    val step = sessionFlow(module)?.value?.stepState?.step
                    val option = step?.let { ModuleVoiceInterpreter.resolve(transcript, it.options) }
                    if (option == null || (confidence != null && confidence < .55f)) {
                        _voiceState.value = ModuleVoiceState(module, false, "Say option A, B, C, or D, or tap your answer."); return
                    }
                    selectOption(module, option)
                    submitAnswer(module, AssessmentMethod.VOICE, transcript, confidence)
                    _voiceState.value = ModuleVoiceState(module, false, "Heard: $transcript")
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            })
        }
    }

    fun stopVoiceAnswer() {
        speechToken++
        speechRecognizer?.cancel(); speechRecognizer?.destroy(); speechRecognizer = null
        if (_voiceState.value.isListening) _voiceState.value = _voiceState.value.copy(isListening = false)
    }

    fun loadStrokeDemoData() { StrokeDemoData.generateSessions().forEach(strokeStore::saveSession); _isStrokeDemoLoaded.value = true; refreshStrokeProfile() }
    fun loadAllergyDemoData() { AllergyDemoData.generateSessions().forEach(allergyStore::saveSession); _isAllergyDemoLoaded.value = true; refreshAllergyProfile() }
    fun clearStrokeSessions() { strokeStore.clearAllSessions(); _isStrokeDemoLoaded.value = false; _strokeSession.value = null; refreshStrokeProfile() }
    fun clearAllergySessions() { allergyStore.clearAllSessions(); _isAllergyDemoLoaded.value = false; _allergySession.value = null; refreshAllergyProfile() }

    private fun persist(state: ModuleTrainingSessionState, complete: Boolean = false) {
        storeFor(state.moduleTypeName)?.saveSession(ModuleTrainingSession(
            sessionId = state.sessionId, moduleType = state.moduleTypeName, startMs = state.startMs,
            endMs = if (complete) System.currentTimeMillis() else 0L, attempts = state.completedAttempts,
            focusedSkillTag = state.focusedSkillTag
        ))
    }

    private fun restoreDraft(module: String) {
        val saved = storeFor(module)?.loadSessions()?.firstOrNull { !it.isComplete && !it.isDemoData } ?: return
        val steps = saved.focusedSkillTag?.let { tag -> stepsFor(module).filter { it.skillTag == tag } } ?: stepsFor(module)
        val current = steps.indexOfFirst { step ->
            !isStepComplete(saved.attempts, step)
        }.let { if (it < 0) steps.lastIndex.coerceAtLeast(0) else it }
        val step = steps.getOrNull(current) ?: return
        val stepAttempts = saved.attempts.filter { it.stepIndex == step.stepIndex }
        val knowledge = stepAttempts.filter {
            it.assessmentMethod == AssessmentMethod.BUTTON || it.assessmentMethod == AssessmentMethod.VOICE
        }.maxByOrNull { it.timestampMs }
        val placement = stepAttempts.filter { it.assessmentMethod == AssessmentMethod.SIMULATED_PLACEMENT }
            .maxByOrNull { it.timestampMs }
        val camera = cameraObservationType(step.cameraRequirement)?.let { type ->
            stepAttempts.firstOrNull { it.eventType == "camera_observation" && it.observationType == type }
        }
        val knowledgeCorrect = knowledge?.knowledgeResult == KnowledgeResult.CORRECT
        val placementPending = step.requiresPlacementSimulation && placement?.placementResult != PlacementResult.CORRECT
        val restoredAttempt = placement ?: knowledge
        sessionFlow(module)?.value = ModuleTrainingSessionState(
            moduleTypeName = module, stepIndex = current,
            phase = if (knowledgeCorrect && placementPending) ModuleTrainingPhase.PLACEMENT else ModuleTrainingPhase.STEP,
            stepState = ModuleStepState(
                step = step,
                selectedOptionId = knowledge?.selectedOptionId,
                locked = knowledgeCorrect,
                attempt = restoredAttempt.takeIf { knowledgeCorrect },
                retryCount = stepAttempts.count {
                    it.outcome == SkillAttemptOutcome.INCORRECT || it.outcome == SkillAttemptOutcome.NEEDS_PRACTICE
                },
                cameraVerified = camera != null,
                cameraStatus = camera?.feedbackShown
            ),
            completedAttempts = saved.attempts,
            sessionId = saved.sessionId, startMs = saved.startMs, focusedSkillTag = saved.focusedSkillTag
        )
    }

    private fun isStepComplete(attempts: List<ModuleTrainingAttempt>, step: ModuleTrainingStep): Boolean {
        val stepAttempts = attempts.filter { it.stepIndex == step.stepIndex }
        if (stepAttempts.any { it.manuallyAdvanced }) return true
        val knowledgeCorrect = stepAttempts.any {
            (it.assessmentMethod == AssessmentMethod.BUTTON || it.assessmentMethod == AssessmentMethod.VOICE) &&
                it.knowledgeResult == KnowledgeResult.CORRECT
        }
        val placementComplete = !step.requiresPlacementSimulation || stepAttempts.any {
            it.assessmentMethod == AssessmentMethod.SIMULATED_PLACEMENT && it.placementResult == PlacementResult.CORRECT
        }
        val cameraComplete = step.cameraRequirement == ModuleCameraRequirement.NONE ||
            cameraObservationType(step.cameraRequirement)?.let { type ->
                stepAttempts.any { it.eventType == "camera_observation" && it.observationType == type }
            } == true
        return knowledgeCorrect && placementComplete && cameraComplete
    }

    private fun cameraObservationType(requirement: ModuleCameraRequirement): String? = when (requirement) {
        ModuleCameraRequirement.NONE -> null
        ModuleCameraRequirement.FACE_REFERENCE_VISIBLE -> "face_mesh_mouth_symmetry"
        ModuleCameraRequirement.BOTH_ARMS_VISIBLE -> "pose_arm_height_comparison"
        ModuleCameraRequirement.PATIENT_LYING_FLAT -> "pose_lying_position_estimate"
        ModuleCameraRequirement.THIGH_REFERENCE_VISIBLE -> "pose_thigh_reference"
    }

    private fun refresh(module: String) = when (module) { "STROKE_FAST" -> refreshStrokeProfile(); "ALLERGIC_REACTION" -> refreshAllergyProfile(); else -> Unit }
    private fun refreshStrokeProfile() {
        val sessions = strokeStore.loadSessions(); _isStrokeDemoLoaded.value = sessions.any { it.isDemoData }
        val profile = ModuleTrainingProfile("STROKE_FAST", sessions); _strokeProfile.value = profile
        _strokeRecs.value = ModuleSkillAssessmentEngine.recommend(profile, false)
    }
    private fun refreshAllergyProfile() {
        val sessions = allergyStore.loadSessions(); _isAllergyDemoLoaded.value = sessions.any { it.isDemoData }
        val profile = ModuleTrainingProfile("ALLERGIC_REACTION", sessions); _allergyProfile.value = profile
        _allergyRecs.value = ModuleSkillAssessmentEngine.recommend(profile, false)
    }

    override fun onCleared() { stopVoiceAnswer(); super.onCleared() }
}
