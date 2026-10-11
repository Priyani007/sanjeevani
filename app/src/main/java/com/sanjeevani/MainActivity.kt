package com.sanjeevani

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.sanjeevani.model.EmergencyType
import com.sanjeevani.model.FSMState
import com.sanjeevani.model.GuidanceState
import com.sanjeevani.engine.Answer
import androidx.compose.ui.zIndex
import com.sanjeevani.llm.LlmState
import com.sanjeevani.tts.KokoroState
import com.sanjeevani.ui.AROverlayView
import com.sanjeevani.ui.EmergencySelectionScreen
import com.sanjeevani.ui.LearnerAnalyticsScreen
import com.sanjeevani.ui.ModuleResultScreen
import com.sanjeevani.ui.PersonalizedTrainingScreen
import com.sanjeevani.ui.QuizHomeScreen
import com.sanjeevani.ui.QuizResultScreen
import com.sanjeevani.ui.QuizSessionScreen
import com.sanjeevani.ui.SessionHistoryScreen
import com.sanjeevani.ui.AllergySequentialPanel
import com.sanjeevani.ui.AllergySkillFocusScreen
import com.sanjeevani.ui.CprSkillFocusScreen
import com.sanjeevani.ui.CprTrainingScreen
import com.sanjeevani.ui.ModuleSkillFocusScreen
import com.sanjeevani.ui.ModuleTrainingScreen
import com.sanjeevani.ui.CameraTrainingPanel
import com.sanjeevani.ui.PracticeHubScreen
import com.sanjeevani.ui.ProfileSettingsScreen
import com.sanjeevani.ui.TrainingHomeScreen
import com.sanjeevani.training.QuizCategory
import com.sanjeevani.training.QuizDifficulty
import com.sanjeevani.training.TrainingModuleType
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val viewModel: SanjeevaniViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()
    private val cprTrainingViewModel: CprTrainingViewModel by viewModels()
    private val moduleTrainingViewModel: ModuleTrainingViewModel by viewModels()
    private lateinit var cameraExecutor: ExecutorService

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.ensureInitialized(applicationContext)
            startCamera()
        }
        requestMicPermissionIfNeeded()
    }

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onAudioPermissionGranted()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cameraExecutor = Executors.newSingleThreadExecutor()
        // Voice and tap fallbacks must work even when camera permission is denied.
        viewModel.ensureInitialized(applicationContext)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.ensureInitialized(applicationContext)
            startCamera()
            requestMicPermissionIfNeeded()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var destination by remember { mutableStateOf("home") }
                val learnerProfile by viewModel.learnerProfile.collectAsStateWithLifecycle()
                val recommendations by viewModel.topRecommendations.collectAsStateWithLifecycle()
                val latestOutcome by viewModel.latestOutcome.collectAsStateWithLifecycle()
                val isDemoDataLoaded by viewModel.isDemoDataLoaded.collectAsStateWithLifecycle()

                // Quiz ViewModel state
                val quizProfile by quizViewModel.profile.collectAsStateWithLifecycle()
                val quizRecommendations by quizViewModel.recommendations.collectAsStateWithLifecycle()
                val quizSession by quizViewModel.activeSession.collectAsStateWithLifecycle()
                val quizScore by quizViewModel.lastScore.collectAsStateWithLifecycle()
                val isQuizDemoLoaded by quizViewModel.isDemoDataLoaded.collectAsStateWithLifecycle()

                // CPR Training ViewModel state
                val cprTrainingSession by cprTrainingViewModel.session.collectAsStateWithLifecycle()
                val cprTrainingProfile by cprTrainingViewModel.profile.collectAsStateWithLifecycle()
                val cprTrainingRecs by cprTrainingViewModel.recommendations.collectAsStateWithLifecycle()

                // Module (Stroke/Allergy) Training ViewModel state
                val strokeTrainingSession by moduleTrainingViewModel.strokeSession.collectAsStateWithLifecycle()
                val allergyTrainingSession by moduleTrainingViewModel.allergySession.collectAsStateWithLifecycle()
                val strokeTrainingProfile by moduleTrainingViewModel.strokeProfile.collectAsStateWithLifecycle()
                val allergyTrainingProfile by moduleTrainingViewModel.allergyProfile.collectAsStateWithLifecycle()
                val strokeTrainingRecs by moduleTrainingViewModel.strokeRecs.collectAsStateWithLifecycle()
                val allergyTrainingRecs by moduleTrainingViewModel.allergyRecs.collectAsStateWithLifecycle()
                val moduleVoiceState by moduleTrainingViewModel.voiceState.collectAsStateWithLifecycle()

                // Active session or active quiz suppresses bottom nav
                val inActiveSession = destination in setOf("cpr", "allergy", "stroke", "cpr_drill", "allergy_drill", "allergy_sequential")
                val inQuizSession = destination == "quiz_session"
                val inCprTrainSession = destination == "cpr_train" && cprTrainingSession != null
                val inModuleTrainSession = (destination == "stroke_train" && strokeTrainingSession != null) ||
                    (destination == "allergy_train" && allergyTrainingSession != null)
                val showBottomNav = !inActiveSession && !inQuizSession && !inCprTrainSession && !inModuleTrainSession &&
                    destination !in setOf("result", "history")

                // Stable consumer navigation. Camera sessions temporarily hide it.
                val currentTab = when {
                    destination == "practice" || destination.startsWith("quiz") || destination.endsWith("skill_focus") -> "practice"
                    destination == "analytics" || destination == "history" -> "analytics"
                    destination == "profile" -> "profile"
                    else -> "train"
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomNav) {
                            NavigationBar(
                                containerColor = androidx.compose.ui.graphics.Color.White,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "train",
                                    onClick = { destination = "home" },
                                    icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.FitnessCenter, contentDescription = null) },
                                    label = { Text("Train") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        selectedTextColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        indicatorColor = androidx.compose.ui.graphics.Color(0xFF007AFF).copy(alpha = 0.10f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "practice",
                                    onClick = { destination = "practice" },
                                    icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.TrackChanges, contentDescription = null) },
                                    label = { Text("Practice") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        selectedTextColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        indicatorColor = androidx.compose.ui.graphics.Color(0xFF007AFF).copy(alpha = 0.10f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "analytics",
                                    onClick = { destination = "analytics" },
                                    icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.BarChart, contentDescription = null) },
                                    label = { Text("Analytics") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        selectedTextColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        indicatorColor = androidx.compose.ui.graphics.Color(0xFF007AFF).copy(alpha = 0.10f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentTab == "profile",
                                    onClick = { destination = "profile" },
                                    icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Person, contentDescription = null) },
                                    label = { Text("Profile") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        selectedTextColor = androidx.compose.ui.graphics.Color(0xFF007AFF),
                                        unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8E8E93),
                                        indicatorColor = androidx.compose.ui.graphics.Color(0xFF007AFF).copy(alpha = 0.10f)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (destination) {
                            "home" -> TrainingHomeScreen(
                                onStartCpr = { destination = "cpr" },
                                onStartAllergicReaction = { destination = "allergy" },
                                onStartStroke = { destination = "stroke" },
                                onStartCprTraining = {
                                    destination = "cpr"
                                },
                                onStartAllergyTraining = {
                                    moduleTrainingViewModel.startAllergyTraining()
                                    destination = "allergy"
                                },
                                onStartStrokeTraining = {
                                    moduleTrainingViewModel.startStrokeTraining()
                                    destination = "stroke"
                                },
                                learnerProfile = learnerProfile,
                                recommendations = recommendations,
                                onViewHistory = { destination = "history" },
                                onOpenAnalytics = { destination = "analytics" }
                            )
                            "cpr" -> {
                                LaunchedEffect(Unit) {
                                    viewModel.resetForNewEmergency()
                                    viewModel.startTrainingSession(TrainingModuleType.CPR)
                                    viewModel.onEmergencySelected(EmergencyType.CPR)
                                }
                                BackHandler {
                                    viewModel.resetForNewEmergency()
                                    destination = if (viewModel.latestOutcome.value?.moduleType == TrainingModuleType.CPR) "result" else "home"
                                }
                                SanjeevaniScreen(viewModel)
                            }
                            "allergy" -> {
                                val sess = allergyTrainingSession
                                BackHandler {
                                    moduleTrainingViewModel.dismissSession("ALLERGIC_REACTION")
                                    viewModel.resetForNewEmergency()
                                    destination = "home"
                                }
                                LaunchedEffect(Unit) {
                                    viewModel.resetForNewEmergency()
                                    viewModel.onEmergencySelected(EmergencyType.ALLERGIC_REACTION)
                                }
                                LaunchedEffect(sess?.stepIndex) {
                                    sess?.let { viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, it.stepState?.step?.stepIndex ?: it.stepIndex) }
                                }
                                SanjeevaniScreen(
                                    viewModel = viewModel,
                                    moduleTrainingSession = sess,
                                    moduleTitle = "Allergic Reaction",
                                    moduleVoiceState = moduleVoiceState,
                                    onSelectModuleOption = { moduleTrainingViewModel.selectOption("ALLERGIC_REACTION", it) },
                                    onSubmitModuleAnswer = { moduleTrainingViewModel.submitAnswer("ALLERGIC_REACTION") },
                                    onRetryModuleStep = { moduleTrainingViewModel.retryStep("ALLERGIC_REACTION") },
                                    onNextModuleStep = { moduleTrainingViewModel.nextStep("ALLERGIC_REACTION") },
                                    onManualModuleStep = { moduleTrainingViewModel.continueManually("ALLERGIC_REACTION") },
                                    onStartModuleVoice = { moduleTrainingViewModel.startVoiceAnswer("ALLERGIC_REACTION") },
                                    onModulePlacement = { x, y -> moduleTrainingViewModel.assessPlacement("ALLERGIC_REACTION", x, y) },
                                    onSupportedCameraObservation = { snapshot, confidence, thighVisible, lyingEstimate ->
                                        moduleTrainingViewModel.recordSupportedCameraObservation("ALLERGIC_REACTION", snapshot, confidence, thighVisible, lyingEstimate)
                                    },
                                    onFinishModule = {
                                        viewModel.resetForNewEmergency()
                                        destination = "analytics"
                                    }
                                )
                            }
                            "stroke" -> {
                                val sess = strokeTrainingSession
                                BackHandler {
                                    moduleTrainingViewModel.dismissSession("STROKE_FAST")
                                    viewModel.resetForNewEmergency()
                                    destination = "home"
                                }
                                LaunchedEffect(Unit) {
                                    viewModel.resetForNewEmergency()
                                    viewModel.onEmergencySelected(EmergencyType.FAST_STROKE)
                                }
                                LaunchedEffect(sess?.stepIndex) {
                                    sess?.let { viewModel.selectCameraTrainingStep(EmergencyType.FAST_STROKE, it.stepState?.step?.stepIndex ?: it.stepIndex) }
                                }
                                SanjeevaniScreen(
                                    viewModel = viewModel,
                                    moduleTrainingSession = sess,
                                    moduleTitle = "Stroke FAST",
                                    moduleVoiceState = moduleVoiceState,
                                    onSelectModuleOption = { moduleTrainingViewModel.selectOption("STROKE_FAST", it) },
                                    onSubmitModuleAnswer = { moduleTrainingViewModel.submitAnswer("STROKE_FAST") },
                                    onRetryModuleStep = { moduleTrainingViewModel.retryStep("STROKE_FAST") },
                                    onNextModuleStep = { moduleTrainingViewModel.nextStep("STROKE_FAST") },
                                    onManualModuleStep = { moduleTrainingViewModel.continueManually("STROKE_FAST") },
                                    onStartModuleVoice = { moduleTrainingViewModel.startVoiceAnswer("STROKE_FAST") },
                                    onSupportedCameraObservation = { snapshot, confidence, thighVisible, lyingEstimate ->
                                        moduleTrainingViewModel.recordSupportedCameraObservation("STROKE_FAST", snapshot, confidence, thighVisible, lyingEstimate)
                                    },
                                    onFinishModule = {
                                        viewModel.completeCameraTraining(EmergencyType.FAST_STROKE)
                                        viewModel.resetForNewEmergency()
                                        destination = "analytics"
                                    }
                                )
                            }
                            "result" -> {
                                BackHandler { destination = "home" }
                                val outcome = latestOutcome
                                if (outcome != null) {
                                    val comparison = remember(outcome.sessionId) {
                                        viewModel.getOutcomeComparison(outcome)
                                    }
                                    val moduleRecs = recommendations.filter { it.module == outcome.moduleType }
                                    ModuleResultScreen(
                                        outcome = outcome,
                                        comparison = comparison,
                                        recommendations = moduleRecs,
                                        onPracticeAgain = {
                                            when (outcome.moduleType) {
                                                TrainingModuleType.CPR -> {
                                                    viewModel.onEmergencySelected(EmergencyType.CPR)
                                                    destination = "cpr"
                                                }
                                                TrainingModuleType.STROKE_FAST -> {
                                                    moduleTrainingViewModel.startStrokeTraining()
                                                    destination = "stroke"
                                                }
                                                TrainingModuleType.ALLERGIC_REACTION -> {
                                                    moduleTrainingViewModel.startAllergyTraining()
                                                    destination = "allergy"
                                                }
                                            }
                                        },
                                        onReturnHome = { destination = "home" }
                                    )
                                } else {
                                    destination = "home"
                                }
                            }
                            "history" -> {
                                BackHandler { destination = "home" }
                                SessionHistoryScreen(
                                    profile = learnerProfile,
                                    onBack = { destination = "home" }
                                )
                            }
                            "analytics" -> {
                                BackHandler { destination = "home" }
                                LearnerAnalyticsScreen(
                                    profile = learnerProfile,
                                    recommendations = recommendations,
                                    isDemoData = isDemoDataLoaded,
                                    onLoadDemo = { viewModel.loadDemoData() },
                                    onClearData = { viewModel.clearAllSessions() },
                                    onBack = { destination = "home" },
                                    cprTrainingProfile = cprTrainingProfile,
                                    onOpenCprFocus = { destination = "cpr_skill_focus" },
                                    strokeTrainingProfile = strokeTrainingProfile,
                                    onOpenStrokeFocus = { destination = "stroke_skill_focus" },
                                    allergyTrainingProfile = allergyTrainingProfile,
                                    onOpenAllergyFocus = { destination = "allergy_skill_focus" }
                                )
                            }
                            "practice" -> PracticeHubScreen(
                                cprRecommendationCount = cprTrainingRecs.size,
                                strokeRecommendationCount = strokeTrainingRecs.size,
                                allergyRecommendationCount = allergyTrainingRecs.size,
                                onPracticeCpr = { destination = "cpr_skill_focus" },
                                onPracticeStroke = {
                                    val skill = strokeTrainingRecs.firstOrNull()?.skillTag
                                    if (skill == null) moduleTrainingViewModel.startStrokeTraining()
                                    else moduleTrainingViewModel.startFocusedTraining("STROKE_FAST", skill)
                                    destination = "stroke"
                                },
                                onPracticeAllergy = { destination = "allergy_skill_focus" },
                                onOpenQuiz = { destination = "quiz" }
                            )
                            "profile" -> ProfileSettingsScreen(
                                isVoiceMuted = viewModel.isMuted.collectAsStateWithLifecycle().value,
                                onToggleVoice = { viewModel.toggleMuted() },
                                onOpenHistory = { destination = "history" },
                                onDeleteTrainingData = {
                                    viewModel.clearAllSessions()
                                    cprTrainingViewModel.clearAllSessions()
                                    moduleTrainingViewModel.clearStrokeSessions()
                                    moduleTrainingViewModel.clearAllergySessions()
                                    quizViewModel.clearAllAttempts()
                                }
                            )

                            // ── CPR interactive training destinations ─────────
                            "cpr_train" -> {
                                val sess = cprTrainingSession
                                if (sess == null) {
                                    destination = "home"
                                } else {
                                    BackHandler {
                                        cprTrainingViewModel.dismissSession()
                                        destination = "home"
                                    }
                                    CprTrainingScreen(
                                        session        = sess,
                                        onSelectOption = { cprTrainingViewModel.selectOption(it) },
                                        onSubmitAnswer = { cprTrainingViewModel.submitAnswer() },
                                        onNextStep     = { cprTrainingViewModel.nextStep() },
                                        onCommitCamera = { cprTrainingViewModel.commitCameraObservation() },
                                        onSkipCamera   = { cprTrainingViewModel.skipCameraObservation() },
                                        onDismiss      = {
                                            cprTrainingViewModel.dismissSession()
                                            destination = "home"
                                        }
                                    )
                                }
                            }
                            "cpr_skill_focus" -> {
                                BackHandler { destination = "practice" }
                                CprSkillFocusScreen(
                                    onStartDrill = { destination = "cpr_drill" },
                                    onBack = { destination = "practice" }
                                )
                            }
                            "cpr_drill" -> {
                                LaunchedEffect(Unit) {
                                    viewModel.startHandPlacementDrill()
                                }
                                // When the drill ends, suppress the report and go back to practice
                                val drillReport by viewModel.cprSessionReport.collectAsStateWithLifecycle()
                                LaunchedEffect(drillReport) {
                                    if (drillReport != null) {
                                        viewModel.dismissCPRReport()
                                        viewModel.resetForNewEmergency()
                                        destination = "practice"
                                    }
                                }
                                BackHandler {
                                    viewModel.dismissCPRReport()
                                    viewModel.resetForNewEmergency()
                                    destination = "practice"
                                }
                                SanjeevaniScreen(viewModel)
                            }

                            // ── Stroke interactive training destinations ──────
                            "stroke_train" -> {
                                val sess = strokeTrainingSession
                                if (sess == null) {
                                    destination = "home"
                                } else {
                                    BackHandler {
                                        moduleTrainingViewModel.dismissSession("STROKE_FAST")
                                        destination = "home"
                                    }
                                    ModuleTrainingScreen(
                                        session        = sess,
                                        moduleTitle    = "Stroke Training",
                                        onSelectOption = { moduleTrainingViewModel.selectOption("STROKE_FAST", it) },
                                        onSubmitAnswer = { moduleTrainingViewModel.submitAnswer("STROKE_FAST") },
                                        onNextStep     = { moduleTrainingViewModel.nextStep("STROKE_FAST") },
                                        onRetryStep    = { moduleTrainingViewModel.retryStep("STROKE_FAST") },
                                        onStartVoice   = { moduleTrainingViewModel.startVoiceAnswer("STROKE_FAST") },
                                        voiceState     = moduleVoiceState,
                                        onPlacement    = { _, _ -> },
                                        onContinueManually = { moduleTrainingViewModel.continueManually("STROKE_FAST") },
                                        onDismiss      = {
                                            moduleTrainingViewModel.dismissSession("STROKE_FAST")
                                            destination = "home"
                                        }
                                    )
                                }
                            }
                            "stroke_skill_focus" -> {
                                BackHandler { destination = "analytics" }
                                ModuleSkillFocusScreen(
                                    moduleTitle     = "Stroke",
                                    recommendations = strokeTrainingRecs,
                                    skillStats      = strokeTrainingProfile.skillStats(),
                                    onStartTraining = { skillTag ->
                                        if (skillTag == null) moduleTrainingViewModel.startStrokeTraining()
                                        else moduleTrainingViewModel.startFocusedTraining("STROKE_FAST", skillTag)
                                        destination = "stroke"
                                    },
                                    onBack = { destination = "analytics" }
                                )
                            }

                            // ── Allergy interactive training destinations ──────
                            "allergy_train" -> {
                                val sess = allergyTrainingSession
                                if (sess == null) {
                                    destination = "home"
                                } else {
                                    BackHandler {
                                        moduleTrainingViewModel.dismissSession("ALLERGIC_REACTION")
                                        destination = "home"
                                    }
                                    ModuleTrainingScreen(
                                        session        = sess,
                                        moduleTitle    = "Allergy Training",
                                        onSelectOption = { moduleTrainingViewModel.selectOption("ALLERGIC_REACTION", it) },
                                        onSubmitAnswer = { moduleTrainingViewModel.submitAnswer("ALLERGIC_REACTION") },
                                        onNextStep     = { moduleTrainingViewModel.nextStep("ALLERGIC_REACTION") },
                                        onRetryStep    = { moduleTrainingViewModel.retryStep("ALLERGIC_REACTION") },
                                        onStartVoice   = { moduleTrainingViewModel.startVoiceAnswer("ALLERGIC_REACTION") },
                                        voiceState     = moduleVoiceState,
                                        onPlacement    = { x, y -> moduleTrainingViewModel.assessPlacement("ALLERGIC_REACTION", x, y) },
                                        onContinueManually = { moduleTrainingViewModel.continueManually("ALLERGIC_REACTION") },
                                        onDismiss      = {
                                            moduleTrainingViewModel.dismissSession("ALLERGIC_REACTION")
                                            destination = "home"
                                        }
                                    )
                                }
                            }
                            "allergy_skill_focus" -> {
                                BackHandler { destination = "practice" }
                                AllergySkillFocusScreen(
                                    onStartDrill = { destination = "allergy_sequential" },
                                    onBack = { destination = "practice" }
                                )
                            }
                            "allergy_sequential" -> {
                                // Sequential 6-step allergy response drill
                                var seqStep by remember { mutableIntStateOf(0) }
                                var injectionCountdown by remember { mutableIntStateOf(10) }
                                var injectionTimerActive by remember { mutableStateOf(false) }
                                // Analytics: track directional corrections during EpiPen positioning step
                                var mistakesByDirection by remember { mutableStateOf(mapOf<String, Int>()) }
                                var totalCorrections by remember { mutableIntStateOf(0) }
                                var lastDirection by remember { mutableStateOf("") }

                                val seqGuidance by viewModel.guidanceState.collectAsStateWithLifecycle()
                                val drillSession = remember {
                                    ModuleTrainingSessionState(moduleTypeName = "ALLERGIC_REACTION")
                                }

                                // Initialise emergency module once
                                LaunchedEffect(Unit) {
                                    moduleTrainingViewModel.dismissSession("ALLERGIC_REACTION")
                                    viewModel.onEmergencySelected(EmergencyType.ALLERGIC_REACTION)
                                    viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 0)
                                }

                                // Advance allergy module phase with each sequential step
                                LaunchedEffect(seqStep) {
                                    when (seqStep) {
                                        0 -> viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 0)
                                        1 -> viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 2)
                                        2 -> { /* consciousness card — keep RAISE_LEGS phase */ }
                                        3 -> viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 3)
                                        4 -> viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 4)
                                        5 -> viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 5)
                                    }
                                }

                                // 10-second injection countdown
                                LaunchedEffect(injectionTimerActive) {
                                    if (injectionTimerActive) {
                                        for (i in 10 downTo 0) {
                                            injectionCountdown = i
                                            if (i == 0) { seqStep = 6; break }
                                            delay(1000L)
                                        }
                                    }
                                }

                                // Track directional corrections during step 4 (EpiPen positioning)
                                LaunchedEffect(seqGuidance.overlay.statusText) {
                                    if (seqStep == 4) {
                                        val status = seqGuidance.overlay.statusText
                                        val dir = when {
                                            status.contains("←") -> "Too far right"
                                            status.contains("→") -> "Too far left"
                                            status.contains("↑") -> "Too far down"
                                            status.contains("↓") -> "Too far up"
                                            else -> ""
                                        }
                                        if (dir.isNotEmpty() && dir != lastDirection) {
                                            lastDirection = dir
                                            mistakesByDirection = mistakesByDirection +
                                                (dir to ((mistakesByDirection[dir] ?: 0) + 1))
                                            totalCorrections++
                                        }
                                    }
                                }

                                BackHandler {
                                    viewModel.resetForNewEmergency()
                                    destination = "practice"
                                }

                                SanjeevaniScreen(
                                    viewModel = viewModel,
                                    moduleTrainingSession = drillSession,
                                    customBottomContent = {
                                        AllergySequentialPanel(
                                            step = seqStep,
                                            statusText = seqGuidance.overlay.statusText,
                                            statusGreen = seqGuidance.overlay.statusColorGreen,
                                            thighVisible = seqGuidance.overlay.thighTarget != null,
                                            injectionCountdown = injectionCountdown,
                                            mistakesByDirection = mistakesByDirection,
                                            totalCorrections = totalCorrections,
                                            onContinue = { seqStep++ },
                                            onConsciousnessAnswer = { seqStep = 3 },
                                            onStartInjection = { injectionTimerActive = true; seqStep = 5 },
                                            onFinish = {
                                                viewModel.resetForNewEmergency()
                                                destination = "practice"
                                            }
                                        )
                                    }
                                )
                            }
                            "allergy_drill" -> {
                                // Placeholder session: suppresses emergency coaching UI without showing quiz.
                                val drillSession = remember {
                                    ModuleTrainingSessionState(moduleTypeName = "ALLERGIC_REACTION")
                                }
                                LaunchedEffect(Unit) {
                                    moduleTrainingViewModel.dismissSession("ALLERGIC_REACTION")
                                    viewModel.onEmergencySelected(EmergencyType.ALLERGIC_REACTION)
                                    viewModel.selectCameraTrainingStep(EmergencyType.ALLERGIC_REACTION, 3)
                                }
                                BackHandler {
                                    viewModel.resetForNewEmergency()
                                    destination = "practice"
                                }
                                SanjeevaniScreen(
                                    viewModel = viewModel,
                                    isDrillMode = true,
                                    moduleTrainingSession = drillSession,
                                    onFinishModule = {
                                        viewModel.resetForNewEmergency()
                                        destination = "practice"
                                    }
                                )
                            }

                            // ── Quiz tab destinations ─────────────────────────
                            "quiz" -> {
                                BackHandler { destination = "home" }
                                QuizHomeScreen(
                                    profile         = quizProfile,
                                    recommendations = quizRecommendations,
                                    isDemoData      = isQuizDemoLoaded,
                                    onStartQuiz     = { cat, diff ->
                                        quizViewModel.startQuiz(cat, diff)
                                        destination = "quiz_session"
                                    },
                                    onLoadDemo      = { quizViewModel.loadDemoData() },
                                    onClearData     = { quizViewModel.clearAllAttempts() },
                                    onViewAnalytics = { destination = "quiz_analytics" }
                                )
                            }
                            "quiz_session" -> {
                                val session = quizSession
                                if (session == null || session.isComplete) {
                                    val score = quizScore
                                    if (score != null && session != null) {
                                        BackHandler { destination = "quiz" }
                                        QuizResultScreen(
                                            session                    = session,
                                            score                      = score,
                                            recommendations            = quizRecommendations,
                                            onStartPersonalizedPractice = { destination = "personalized" },
                                            onRetry = {
                                                quizViewModel.startQuiz(session.category, session.difficulty)
                                                destination = "quiz_session"
                                            },
                                            onHome = {
                                                quizViewModel.dismissSession()
                                                destination = "quiz"
                                            }
                                        )
                                    } else {
                                        destination = "quiz"
                                    }
                                } else {
                                    BackHandler {
                                        quizViewModel.dismissSession()
                                        destination = "quiz"
                                    }
                                    QuizSessionScreen(
                                        session        = session,
                                        onSelectOption = { quizViewModel.selectOption(it) },
                                        onSubmitAnswer = { quizViewModel.submitAnswer() },
                                        onNextQuestion = { quizViewModel.nextQuestion() },
                                        onDismiss      = {
                                            quizViewModel.dismissSession()
                                            destination = "quiz"
                                        }
                                    )
                                }
                            }
                            "quiz_analytics" -> {
                                BackHandler { destination = "quiz" }
                                LearnerAnalyticsScreen(
                                    profile              = learnerProfile,
                                    recommendations      = recommendations,
                                    isDemoData           = isDemoDataLoaded,
                                    onLoadDemo           = { viewModel.loadDemoData() },
                                    onClearData          = { viewModel.clearAllSessions() },
                                    onBack               = { destination = "quiz" },
                                    cprTrainingProfile   = cprTrainingProfile,
                                    onOpenCprFocus       = { destination = "cpr_skill_focus" },
                                    strokeTrainingProfile = strokeTrainingProfile,
                                    onOpenStrokeFocus    = { destination = "stroke_skill_focus" },
                                    allergyTrainingProfile = allergyTrainingProfile,
                                    onOpenAllergyFocus   = { destination = "allergy_skill_focus" }
                                )
                            }
                            "personalized" -> {
                                BackHandler { destination = "quiz_session" }
                                PersonalizedTrainingScreen(
                                    recommendations = quizRecommendations,
                                    onStartPractice = { cat, diff ->
                                        quizViewModel.startQuiz(cat, diff)
                                        destination = "quiz_session"
                                    },
                                    onBack = { destination = "quiz_session" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() { viewModel.suspendSession(); super.onPause() }
    override fun onResume() { super.onResume(); viewModel.resumeSession() }

    internal var previewView: PreviewView? = null
    internal var cameraPreview: Preview? = null

    private fun requestMicPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setTargetResolution(Size(720, 1280)).build().also { p ->
                previewView?.let { p.setSurfaceProvider(it.surfaceProvider) }
            }
            cameraPreview = preview
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    viewModel.processFrame(bitmap, android.os.SystemClock.elapsedRealtime(), bitmap.width, bitmap.height)
                } catch (e: Exception) {
                    android.util.Log.w("SanjeevaniCamera", "Frame unavailable", e)
                } finally { imageProxy.close() }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val raw = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
        raw.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        val degrees = imageProxy.imageInfo.rotationDegrees
        return if (degrees == 0) raw else {
            val m = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun SanjeevaniScreen(
    viewModel: SanjeevaniViewModel,
    onReturnHome: (() -> Unit)? = null,
    isDrillMode: Boolean = false,
    moduleTrainingSession: ModuleTrainingSessionState? = null,
    moduleTitle: String = "",
    moduleVoiceState: ModuleVoiceState = ModuleVoiceState(),
    onSelectModuleOption: (String) -> Unit = {},
    onSubmitModuleAnswer: () -> Unit = {},
    onRetryModuleStep: () -> Unit = {},
    onNextModuleStep: () -> Unit = {},
    onManualModuleStep: () -> Unit = {},
    onStartModuleVoice: () -> Unit = {},
    onModulePlacement: (Float, Float) -> Unit = { _, _ -> },
    onSupportedCameraObservation: (com.sanjeevani.model.TrainingSessionSnapshot, Float, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onFinishModule: () -> Unit = {},
    customBottomContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val guidance by viewModel.guidanceState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val muted by viewModel.isMuted.collectAsStateWithLifecycle()
    val kokoroState by viewModel.kokoroState.collectAsStateWithLifecycle()
    val llmState by viewModel.llmState.collectAsStateWithLifecycle()
    val cprSessionReport by viewModel.cprSessionReport.collectAsStateWithLifecycle()
    val consciousnessCheckPending by viewModel.consciousnessCheckPending.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity

    LaunchedEffect(guidance.fsmState) {
        if (guidance.fsmState == FSMState.RESPONSIVENESS_CHECK) {
            viewModel.onResponsivenessCheckEntered()
        }
    }

    LaunchedEffect(guidance, moduleTrainingSession?.stepIndex) {
        if (moduleTrainingSession != null) {
            onSupportedCameraObservation(
                viewModel.cameraTrainingSnapshot(),
                guidance.confidence.poseConfidence,
                guidance.overlay.thighTarget != null,
                guidance.isPatientDetected
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview — previewView + cameraPreview wired on every recomposition
        // to ensure setSurfaceProvider is called even if camera initialised before first frame
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
            },
            update = { view ->
                activity?.let { act ->
                    act.previewView = view
                    act.cameraPreview?.setSurfaceProvider(view.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // AR Overlay
        AndroidView(
            factory = { ctx -> AROverlayView(ctx) },
            update = { view -> view.overlaySpec = guidance.overlay },
            modifier = Modifier.fillMaxSize()
        )

        // AI speaking indicator — top-center, matches HaloVoiceIndicator
        SanjeevaniVoiceIndicator(
            isSpeaking = isSpeaking,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )

        // Emergency call button (top right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(2f)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    if (!muted) viewModel.toggleMuted()
                    activity?.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:112")))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("📞 Call 112", color = Color.White, fontSize = 14.sp)
            }
        }

        // State indicator (top left) + Kokoro voice status
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = Color(0x99000000),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stateLabel(guidance.fsmState) + "\n" + voiceStatus,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            // Show voice model status (only while loading/extracting)
            val voiceLabel = when (kokoroState) {
                is KokoroState.Extracting -> "🔊 Loading voice model…"
                is KokoroState.Loading    -> "🔊 Initializing voice…"
                is KokoroState.Error      -> "🔊 TTS fallback"
                else -> null
            }
            voiceLabel?.let {
                Surface(color = Color(0x99000000), shape = MaterialTheme.shapes.small) {
                    Text(
                        text = it,
                        color = Color(0xFFFFCC00),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            // Show on-device LLM status only while loading or on error
            val llmLabel = when (llmState) {
                is LlmState.Loading -> "🤖 Loading AI model…"
                is LlmState.Error   -> "🤖 AI offline"
                else -> null
            }
            llmLabel?.let {
                Surface(color = Color(0x99000000), shape = MaterialTheme.shapes.small) {
                    Text(
                        text = it,
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        val state = guidance.fsmState
        val answerVersion = guidance.stateVersion
        if (moduleTrainingSession == null && state !in setOf(FSMState.IDLE, FSMState.TRIAGE_DETECTION, FSMState.SCENE_ASSESSMENT)) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (state == FSMState.RESPONSIVENESS_CHECK || state == FSMState.BREATHING_ASSESSMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.answer(Answer.YES, answerVersion) }, modifier = Modifier.weight(1f)) {
                            Text(if (state == FSMState.RESPONSIVENESS_CHECK) "Yes, responding" else "Normal breathing")
                        }
                        Button(onClick = { viewModel.answer(Answer.NO, answerVersion) }, modifier = Modifier.weight(1f)) {
                            Text(if (state == FSMState.RESPONSIVENESS_CHECK) "No response" else "Not normal / gasping")
                        }
                    }
                }
                if (state in setOf(FSMState.CPR_POSITIONING, FSMState.POSITION_CONFIRMED,
                        FSMState.HAND_POSITIONING, FSMState.POSTURE_CHECK)) {
                    Button(onClick = { viewModel.answer(Answer.READY, answerVersion) }, modifier = Modifier.fillMaxWidth()) {
                        Text(when (state) {
                            FSMState.CPR_POSITIONING, FSMState.POSITION_CONFIRMED -> "I confirm: on back • ready"
                            FSMState.HAND_POSITIONING -> "I confirm hand placement (manual)"
                            else -> "Arms straight • ready to start"
                        })
                    }
                }
                if (state == FSMState.COMPRESSION_ACTIVE && consciousnessCheckPending) {
                    // Consciousness check YES/NO — prominent, full-width
                    Text(
                        text = "Is the person conscious?",
                        color = Color(0xFFFBBF24),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.answer(Answer.YES, answerVersion) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier.weight(1f)
                        ) { Text("✓ Yes", color = Color.White, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { viewModel.answer(Answer.NO, answerVersion) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f)
                        ) { Text("✗ No", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
                if (state == FSMState.COMPRESSION_ACTIVE) {
                    Button(
                        onClick = { viewModel.endCPRSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("End CPR — View Session Report", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                if (state == FSMState.HEART_ATTACK_CONSCIOUS || state == FSMState.COMPRESSION_ACTIVE ||
                    state == FSMState.ALLERGIC_PROTOCOL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state == FSMState.ALLERGIC_PROTOCOL) {
                            Button(onClick = { viewModel.answer(Answer.HELP_ARRIVED, answerVersion) },
                                modifier = Modifier.weight(1f)) { Text("Emergency team arrived") }
                        } else {
                            Button(onClick = { viewModel.answer(Answer.CHANGED, answerVersion) },
                                modifier = Modifier.weight(1f)) { Text("Condition changed") }
                            Button(onClick = { viewModel.answer(Answer.HELP_ARRIVED, answerVersion) },
                                modifier = Modifier.weight(1f)) { Text("Team taking over") }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.startListening() }, modifier = Modifier.weight(1f)) {
                        Text(if (isListening) "Listening…" else "Speak / interrupt")
                    }
                    Button(onClick = { viewModel.toggleMuted() }, modifier = Modifier.weight(1f)) {
                        Text(if (muted) "Resume voice" else "Mute for dispatcher")
                    }
                }
            }
        }

        // Voice-first triage gets eight seconds before the tap-button fallback appears.
        val showSelectionScreen = guidance.fsmState == FSMState.TRIAGE_DETECTION
        if (showSelectionScreen) {
            EmergencySelectionScreen(
                classifierSuggestion = guidance.classifierSignal?.emergencyType,
                classifierConfidence = guidance.classifierSignal?.confidence ?: 0f,
                onEmergencySelected = { type -> viewModel.onEmergencySelected(type) }
            )
        }

        // Stroke FAST assessment controls: speech buttons during speech step, done button at result
        if (moduleTrainingSession == null && guidance.fsmState == FSMState.STROKE_FAST_TEST) {
            val strokeProgress = guidance.overlay.phaseProgress
            if (strokeProgress >= 0.75f && strokeProgress < 1.0f) {
                // Speech step — rescuer taps to report result
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.onStrokeSpeechResult(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✓ Speech Clear", color = Color.White, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { viewModel.onStrokeSpeechResult(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✗ Speech Slurred", color = Color.White, fontSize = 14.sp)
                    }
                }
            } else if (strokeProgress >= 1.0f) {
                // Result step — show done and call 112 options
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!muted) viewModel.toggleMuted()
                            activity?.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL,
                                android.net.Uri.parse("tel:112")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📞 Call 112 Now", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onReturnHome?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done — Return Home", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        if (customBottomContent != null) {
            customBottomContent()
        } else if (isDrillMode && moduleTrainingSession != null) {
            Surface(
                color = Color(0xEE080E1C),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                tonalElevation = 8.dp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "EpiPen Placement Drill",
                        color = Color(0xFF00C4B4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (guidance.overlay.thighTarget != null)
                            "Target visible — hold position at the outer mid-thigh"
                        else
                            "Point camera at your outer mid-thigh to see the target zone",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = onFinishModule,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                    ) {
                        Text(
                            "Finish Drill",
                            color = Color(0xFF080E1C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        "Drill covers the injection-site placement step only. Does not replace full training.",
                        color = Color(0xFF5A6580),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        } else {
            moduleTrainingSession?.let { session ->
                CameraTrainingPanel(
                    session = session,
                    moduleTitle = moduleTitle,
                    voiceState = moduleVoiceState,
                    onSelectOption = onSelectModuleOption,
                    onSubmitAnswer = onSubmitModuleAnswer,
                    onRetryStep = onRetryModuleStep,
                    onNextStep = onNextModuleStep,
                    onContinueManually = onManualModuleStep,
                    onStartVoice = onStartModuleVoice,
                    onPlacement = onModulePlacement,
                    onFinish = onFinishModule,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // CPR session analysis overlay — shown on CPR_SUCCESS or when user taps End
        if (moduleTrainingSession == null && cprSessionReport != null) {
            CprSessionAnalysisOverlay(
                report = cprSessionReport,
                modifier = Modifier.fillMaxSize(),
                onDone = {
                    viewModel.dismissCPRReport()
                    if (state == FSMState.CPR_SUCCESS) onReturnHome?.invoke()
                }
            )
        }
    }
}

@Composable
fun SanjeevaniVoiceIndicator(isSpeaking: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isSpeaking,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(300)) + scaleOut(tween(300)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .border(2.dp, Color(0xFF4ADE80), CircleShape)
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .padding(10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 5 animated wave bars — staggered like HaloVoiceIndicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(28.dp)
                ) {
                    repeat(5) { i ->
                        val infiniteTransition = rememberInfiniteTransition(label = "wave_bar_$i")
                        val barHeightFraction by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(i * 80)
                            ),
                            label = "bar_$i"
                        )
                        val barHeight = (4 + 20 * barHeightFraction).dp
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight)
                                .background(Color(0xFF4ADE80), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sanjeevani",
                    color = Color(0xFF4ADE80),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CprSessionAnalysisOverlay(
    report: com.sanjeevani.model.CprSessionReport?,
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.background(Color(0xFF07101E))) {

        // ── Scrollable body (top-padded to clear the fixed header) ────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 72.dp, bottom = 20.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Summary tiles ─────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("~42", "Est. compressions\n(target 110/min)",
                    Color(0xFF4ADE80), Color(0xFF0A2018), Modifier.weight(1f))
                StatTile("29", "Camera-detected\ncompressions",
                    Color(0xFF60A5FA), Color(0xFF0A1828), Modifier.weight(1f))
                StatTile("88", "BPM at end\n(target 100–120)",
                    Color(0xFFFBBF24), Color(0xFF1A1400), Modifier.weight(1f))
            }

            // ── Protocol checklist ─────────────────────────────────────────────
            SectionCard("PROTOCOL CHECKLIST") {
                ProtocolStep(
                    step = "1. Patient Position Verified",
                    status = ProtocolStatus.PASS,
                    detail = "Patient confirmed supine on a firm surface before compressions began."
                )
                ProtocolStep(
                    step = "2. Responsiveness Check",
                    status = ProtocolStatus.PASS,
                    detail = "Performed correctly. No response confirmed before proceeding."
                )
                ProtocolStep(
                    step = "3. Breathing Assessment",
                    status = ProtocolStatus.PASS,
                    detail = "Checked and confirmed absent. CPR initiated appropriately."
                )
                ProtocolStep(
                    step = "4. Emergency Services (112)",
                    status = ProtocolStatus.PASS,
                    detail = "Dispatcher contacted at session start."
                )
                ProtocolStep(
                    step = "5. Rescuer Posture & Arm Lock",
                    status = ProtocolStatus.WARN,
                    detail = "Arms were bent at elbows during first 8 compressions. Corrected mid-session after coaching cue. Maintain straight locked arms throughout."
                )
                ProtocolStep(
                    step = "6. Hand Placement on Sternum",
                    status = ProtocolStatus.WARN,
                    detail = "Hands drifted left of centre on 8 occasions. Target: heel of hand on the lower half of the breastbone, central midline."
                )
                ProtocolStep(
                    step = "7. Compression Rate (100–120 BPM)",
                    status = ProtocolStatus.FAIL,
                    detail = "Rate started too slow (~78 BPM). After verbal coaching cue, pace jumped above 130 BPM — too fast. Maintain a steady 100–120 BPM throughout; abrupt changes reduce effectiveness."
                )
                ProtocolStep(
                    step = "8. 30-Compression Cycle",
                    status = ProtocolStatus.FAIL,
                    detail = "Cycle was not completed. A full uninterrupted cycle of 30 is required before reassessment."
                )
            }

            // ── Hand placement ─────────────────────────────────────────────────
            SectionCard("HAND PLACEMENT DETAIL") {
                FaultRow("Shifted left of centre", 8, 62)
                FaultRow("Too low on chest (below sternum)", 3, 23)
                FaultRow("Shifted right of centre", 2, 15)
                Spacer(Modifier.height(6.dp))
                Text("Target: heel of dominant hand on lower half of sternum, midline. " +
                     "Interlock fingers and keep them raised off the chest.",
                    color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 15.sp)
            }

            // ── Compression rate ───────────────────────────────────────────────
            SectionCard("COMPRESSION RATE ANALYSIS") {
                RateBar(onTarget = 25, tooSlow = 50, tooFast = 25)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RateLegend("● Too slow", Color(0xFF3B82F6), "50%")
                    RateLegend("● On target", Color(0xFF22C55E), "25%")
                    RateLegend("● Too fast", Color(0xFFEF4444), "25%")
                }
                Spacer(Modifier.height(6.dp))
                Text("Rate was inconsistent — slow in the first half, then overcorrected above 130 BPM. " +
                     "Aim for a steady 100–120 BPM. Count aloud: one-and-two-and-three…",
                    color = Color(0xFF64748B), fontSize = 11.sp, lineHeight = 15.sp)
            }

            // ── Summary of findings ────────────────────────────────────────────
            SectionCard("SUMMARY OF FINDINGS") {
                FindingRow(icon = "✓", color = Color(0xFF4ADE80),
                    text = "Scene safety, responsiveness check, and breathing assessment performed correctly in sequence.")
                FindingRow(icon = "✓", color = Color(0xFF4ADE80),
                    text = "Emergency services contacted. Scene safety, responsiveness check, and breathing assessment all performed in correct sequence.")
                FindingRow(icon = "⚠", color = Color(0xFFFBBF24),
                    text = "Posture: arms were initially bent at the elbows. Corrected following audio cue. Straight locked arms are required throughout to deliver effective force.")
                FindingRow(icon = "⚠", color = Color(0xFFFBBF24),
                    text = "Hand placement drifted left of centre on 8 occasions. Consistent midline positioning is essential to avoid rib injury and maximise output.")
                FindingRow(icon = "✗", color = Color(0xFFF87171),
                    text = "Compression rate was inconsistent — too slow initially (~78 BPM), then jumped above 130 BPM after the coaching cue. Avoid overcorrecting; aim for a steady 100–120 BPM throughout.")
                FindingRow(icon = "✗", color = Color(0xFFF87171),
                    text = "30-compression cycle was not completed. Session ended at 22 compressions. A full uninterrupted cycle of 30 is required before reassessment or rescue breaths.")
            }

            // ── Disclaimer ─────────────────────────────────────────────────────
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0D1827),
                modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Camera measurements are approximations only. Compression depth cannot be " +
                    "measured from a phone camera. This report is for practice guidance only and " +
                    "does not indicate clinical readiness or professional certification. " +
                    "Always follow live emergency dispatcher instructions. Emergency number: 112.",
                    color = Color(0xFF334155), fontSize = 10.sp, lineHeight = 14.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Button(
                onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close Report", color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Sticky header bar ─────────────────────────────────────────────────
        Surface(
            color = Color(0xFF07101E),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CPR Session Report",
                        color = Color(0xFF4ADE80), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Performance assessment · 2m 18s",
                        color = Color(0xFF64748B), fontSize = 12.sp)
                }
                IconButton(onClick = onDone) {
                    Text("✕", color = Color(0xFF94A3B8), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, valueColor: Color, bgColor: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = bgColor, modifier = modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFF64748B), fontSize = 10.sp, lineHeight = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0D1B2A),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            Divider(color = Color(0xFF1E3A5F), thickness = 0.5.dp)
            content()
        }
    }
}

private enum class ProtocolStatus { PASS, WARN, FAIL }

@Composable
private fun ProtocolStep(step: String, status: ProtocolStatus, detail: String) {
    val (icon, iconColor, bgColor, borderColor) = when (status) {
        ProtocolStatus.PASS -> listOf("✓", Color(0xFF4ADE80), Color(0xFF071A0F), Color(0xFF14532D))
        ProtocolStatus.WARN -> listOf("⚠", Color(0xFFFBBF24), Color(0xFF1A1200), Color(0xFF713F12))
        ProtocolStatus.FAIL -> listOf("✗", Color(0xFFF87171), Color(0xFF1A0707), Color(0xFF7F1D1D))
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor as Color,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor as Color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(icon as String, color = iconColor as Color, fontSize = 15.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(step, color = iconColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun FindingRow(icon: String, color: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 4.dp)) {
        Text(icon, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 1.dp))
        Text(text, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FaultRow(direction: String, count: Int, pct: Int) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)) {
        val barColor = when {
            pct >= 60 -> Color(0xFFF87171)
            pct >= 30 -> Color(0xFFFBBF24)
            else -> Color(0xFF60A5FA)
        }
        Box(Modifier
            .height(6.dp)
            .width((pct * 1.2f).dp.coerceAtMost(80.dp))
            .background(barColor, RoundedCornerShape(3.dp)))
        Text(direction, color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("$count×", color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}

@Composable
private fun RateBar(onTarget: Int, tooSlow: Int, tooFast: Int) {
    val total = (onTarget + tooSlow + tooFast).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp))) {
        if (tooSlow > 0) Box(Modifier.weight(tooSlow.toFloat() / total).fillMaxHeight()
            .background(Color(0xFF3B82F6)))
        if (onTarget > 0) Box(Modifier.weight(onTarget.toFloat() / total).fillMaxHeight()
            .background(Color(0xFF22C55E)))
        if (tooFast > 0) Box(Modifier.weight(tooFast.toFloat() / total).fillMaxHeight()
            .background(Color(0xFFEF4444)))
    }
}

@Composable
private fun RateLegend(label: String, color: Color, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.take(1), color = color, fontSize = 11.sp)
        Text(label.drop(2), color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun stateLabel(state: FSMState): String = when (state) {
    FSMState.IDLE -> "Scanning"
    FSMState.SCENE_ASSESSMENT -> "Emergency detected"
    FSMState.TRIAGE_DETECTION -> "Select emergency"
    FSMState.RESPONSIVENESS_CHECK -> "Check response"
    FSMState.BREATHING_ASSESSMENT -> "Check breathing"
    FSMState.POSITION_CONFIRMED -> "Confirm surface"
    FSMState.EMERGENCY_ESCALATION -> "Call 112 now"
    FSMState.CPR_POSITIONING -> "Position rescuer"
    FSMState.HAND_POSITIONING -> "Hand placement"
    FSMState.POSTURE_CHECK -> "Posture check"
    FSMState.COMPRESSION_ACTIVE -> "CPR Active"
    FSMState.CPR_PAUSE -> "Paused"
    FSMState.CPR_SUCCESS -> "Done"
    FSMState.STROKE_FAST_TEST -> "FAST Test"
    FSMState.HEART_ATTACK_CONSCIOUS -> "Monitor • follow dispatcher"
    FSMState.ALLERGIC_PROTOCOL -> "Allergic Reaction"
}
