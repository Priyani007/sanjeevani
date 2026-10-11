package com.sanjeevani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.ModuleStepState
import com.sanjeevani.ModuleTrainingPhase
import com.sanjeevani.ModuleTrainingSessionState
import com.sanjeevani.training.*

private val MOD_BG       = Color(0xFF080E1C)
private val MOD_CARD_BG  = Color(0xFF121929)
private val MOD_TEAL     = Color(0xFF00C4B4)
private val MOD_GREEN    = Color(0xFF4EE0A0)
private val MOD_GREEN_BG = Color(0xFF0A2018)
private val MOD_RED      = Color(0xFFE53935)
private val MOD_RED_BG   = Color(0xFF2A0808)
private val MOD_ORANGE   = Color(0xFFFF9B2E)
private val MOD_ORANGE_BG = Color(0xFF2A1600)
private val MOD_DIM      = Color(0xFF1A2438)
private val MOD_LABEL    = Color(0xFF5A6580)
private val MOD_SEC      = Color(0xFF7A8899)
private val MOD_WHITE    = Color.White

@Composable
fun ModuleTrainingScreen(
    session: ModuleTrainingSessionState,
    moduleTitle: String,
    onSelectOption: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextStep: () -> Unit,
    onRetryStep: () -> Unit,
    onStartVoice: () -> Unit,
    voiceState: com.sanjeevani.ModuleVoiceState,
    onPlacement: (Float, Float) -> Unit,
    onContinueManually: () -> Unit,
    onDismiss: () -> Unit
) {
    if (session.isFinished) {
        ModuleTrainingCompleteScreen(
            session     = session,
            moduleTitle = moduleTitle,
            onDismiss   = onDismiss
        )
        return
    }

    val stepState = session.stepState ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MOD_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) {
                Text("✕ Exit", fontSize = 13.sp, color = MOD_LABEL)
            }
            Text(moduleTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MOD_WHITE)
            Text("${session.stepIndex + 1} / ${session.totalSteps}",
                fontSize = 13.sp, color = MOD_LABEL)
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { (session.stepIndex + 1).toFloat() / session.totalSteps },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color    = MOD_TEAL,
            trackColor = MOD_DIM
        )

        val step = stepState.step

        // Step title pill
        Surface(shape = RoundedCornerShape(8.dp), color = MOD_TEAL.copy(alpha = 0.12f)) {
            Text(
                "Step ${step.stepIndex + 1}: ${step.stepTitle}",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MOD_TEAL, letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Scenario card
        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = MOD_CARD_BG,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Scenario", fontSize = 11.sp, color = MOD_TEAL, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp)
                Text(step.scenarioContext, fontSize = 14.sp, color = MOD_SEC, lineHeight = 20.sp)
            }
        }

        // Question
        Text(step.questionText, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MOD_WHITE, lineHeight = 22.sp)

        // Options
        step.options.forEach { option ->
            ModuleOptionCard(
                option   = option,
                selected = stepState.selectedOptionId == option.id,
                locked   = stepState.locked,
                attempt  = stepState.attempt,
                onClick  = { if (!stepState.locked) onSelectOption(option.id) }
            )
        }

        if (!stepState.locked) {
            OutlinedButton(
                onClick = onStartVoice,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MOD_WHITE)
            ) {
                Text(if (voiceState.isListening) "● Listening…" else "🎙 Say option A, B, C, or D", fontWeight = FontWeight.SemiBold)
            }
            Text(voiceState.message, fontSize = 12.sp, color = if (voiceState.isListening) MOD_GREEN else MOD_SEC)
        }

        if (session.phase == ModuleTrainingPhase.PLACEMENT) {
            PlacementSimulation(
                x = stepState.placementX,
                y = stepState.placementY,
                result = stepState.attempt?.placementResult ?: PlacementResult.NOT_ASSESSED,
                onPlacement = onPlacement
            )
        }

        // Feedback + navigation after assessment
        if (stepState.locked) {
            stepState.attempt?.let { ModuleFeedbackCard(it) }
            if (stepState.canAdvance) {
                Button(onClick = onNextStep, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = MOD_TEAL)) {
                    Text(if (session.stepIndex + 1 >= session.totalSteps) "See Results" else "Next Step →", fontWeight = FontWeight.Bold, color = Color(0xFF080E1C))
                }
            } else if (session.phase != ModuleTrainingPhase.PLACEMENT) {
                Button(onClick = onRetryStep, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = MOD_ORANGE)) {
                    Text("Try Again", fontWeight = FontWeight.Bold, color = Color(0xFF080E1C))
                }
            }
        } else {
            Button(
                onClick  = onSubmitAnswer,
                enabled  = stepState.selectedOptionId != null,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = MOD_TEAL)
            ) {
                Text(
                    "Submit Answer",
                    fontWeight = FontWeight.Bold,
                    color = if (stepState.selectedOptionId != null) Color(0xFF080E1C) else MOD_LABEL
                )
            }
        }

        OutlinedButton(
            onClick = onContinueManually,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MOD_ORANGE)
        ) {
            Text("Continue Manually — mark unconfirmed")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModuleOptionCard(
    option: ModuleTrainingOption,
    selected: Boolean,
    locked: Boolean,
    attempt: ModuleTrainingAttempt?,
    onClick: () -> Unit
) {
    val (bg, border, textColor) = when {
        locked && attempt != null && option.id == attempt.correctOptionId ->
            Triple(MOD_GREEN_BG, MOD_GREEN, MOD_WHITE)
        locked && attempt != null && option.id == attempt.selectedOptionId &&
            attempt.knowledgeResult != KnowledgeResult.CORRECT ->
            Triple(MOD_RED_BG, MOD_RED, MOD_WHITE)
        selected && !locked ->
            Triple(MOD_TEAL.copy(alpha = 0.12f), MOD_TEAL, MOD_WHITE)
        else ->
            Triple(MOD_CARD_BG, MOD_DIM, MOD_SEC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Option ${option.id}: ${option.text}"
                role = Role.Button
                this.selected = selected
            }
            .clickable(enabled = !locked) { onClick() }
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(6.dp), color = border.copy(alpha = 0.2f)) {
            Text(
                option.id,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = border
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(option.text, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
            if (locked && (option.id == attempt?.selectedOptionId || option.id == attempt?.correctOptionId)) {
                Text(option.feedback, fontSize = 12.sp, color = MOD_SEC, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun ModuleFeedbackCard(attempt: ModuleTrainingAttempt) {
    val isCorrect = attempt.outcome == SkillAttemptOutcome.CORRECT
    val isNeeds   = attempt.outcome == SkillAttemptOutcome.NEEDS_PRACTICE
    val (bg, accent, icon) = when {
        isCorrect -> Triple(MOD_GREEN_BG, MOD_GREEN, "✓")
        isNeeds   -> Triple(MOD_ORANGE_BG, MOD_ORANGE, "⚠")
        else      -> Triple(MOD_RED_BG, MOD_RED, "✗")
    }
    Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icon, fontSize = 16.sp, color = accent)
                Text(when { isCorrect -> "Correct"; isNeeds -> "Practise needed"; else -> "Try again" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
            }
            Text(attempt.feedbackShown, fontSize = 13.sp, color = MOD_WHITE, lineHeight = 19.sp)
            Text("Assessed by ${attempt.assessmentMethod.name.lowercase().replace('_', ' ')} · attempt ${attempt.retryCount + 1}", fontSize = 10.sp, color = MOD_LABEL)
        }
    }
}

@Composable
private fun PlacementSimulation(
    x: Float?,
    y: Float?,
    result: PlacementResult,
    onPlacement: (Float, Float) -> Unit
) {
    Surface(shape = RoundedCornerShape(14.dp), color = MOD_CARD_BG, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GUIDED SIMULATION — NOT A REAL INJECTION", color = MOD_ORANGE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Tap the outer middle thigh where a trainer auto-injector would contact this diagram. Never use a live injector for practice.", color = MOD_WHITE, fontSize = 13.sp, lineHeight = 19.sp)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .semantics { contentDescription = "Trainer auto-injector placement diagram. Tap the outer middle thigh." }
                    .pointerInput(Unit) {
                        detectTapGestures { point -> onPlacement(point.x / size.width, point.y / size.height) }
                    }
            ) {
                val skin = Color(0xFFD8A27F)
                drawRoundRect(skin, topLeft = androidx.compose.ui.geometry.Offset(size.width * .28f, size.height * .08f), size = androidx.compose.ui.geometry.Size(size.width * .44f, size.height * .84f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(45f, 45f))
                drawLine(MOD_LABEL, androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .12f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .88f), strokeWidth = 2f)
                drawRoundRect(MOD_TEAL.copy(alpha = .15f), topLeft = androidx.compose.ui.geometry.Offset(size.width * .62f, size.height * .38f), size = androidx.compose.ui.geometry.Size(size.width * .26f, size.height * .30f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f), style = Stroke(width = 3f))
                if (x != null && y != null) {
                    val color = if (result == PlacementResult.CORRECT) MOD_GREEN else MOD_RED
                    drawCircle(color, 16f, androidx.compose.ui.geometry.Offset(size.width * x, size.height * y))
                    drawCircle(Color.White, 7f, androidx.compose.ui.geometry.Offset(size.width * x, size.height * y))
                }
            }
            Text(AllergyPlacementAssessment.feedback(result), color = if (result == PlacementResult.CORRECT) MOD_GREEN else MOD_ORANGE, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Completion screen ──────────────────────────────────────────────────────────

@Composable
private fun ModuleTrainingCompleteScreen(
    session: ModuleTrainingSessionState,
    moduleTitle: String,
    onDismiss: () -> Unit
) {
    val pct = (session.accuracy * 100).toInt()
    val (emoji, headline) = when {
        pct >= 85 -> "🌟" to "Excellent session"
        pct >= 60 -> "👍" to "Good progress"
        else      -> "📚" to "Keep practising"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MOD_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(emoji, fontSize = 56.sp)
        Text(headline, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MOD_WHITE)
        Text("$pct% of steps answered correctly", fontSize = 15.sp, color = MOD_TEAL)

        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = MOD_CARD_BG,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Step Summary", fontSize = 12.sp, color = MOD_TEAL,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                session.completedAttempts.forEach { attempt ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(attempt.skillDescription, fontSize = 13.sp, color = MOD_SEC,
                            modifier = Modifier.weight(1f), maxLines = 1)
                        ModuleOutcomeChip(attempt.outcome)
                    }
                }
            }
        }

        Text(
            "This session records your progress. Check the Analytics tab for skill-level trends and " +
                "personalised practice recommendations.",
            fontSize = 12.sp, color = MOD_LABEL, lineHeight = 17.sp
        )
        Text(
            "Training records do not indicate professional certification or clinical competency.",
            fontSize = 11.sp, color = MOD_LABEL.copy(alpha = 0.7f), lineHeight = 16.sp
        )

        Button(
            onClick  = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = MOD_TEAL)
        ) {
            Text("Done", fontWeight = FontWeight.Bold, color = Color(0xFF080E1C))
        }
    }
}

@Composable
private fun ModuleOutcomeChip(outcome: SkillAttemptOutcome) {
    val (text, color) = when (outcome) {
        SkillAttemptOutcome.CORRECT        -> "✓" to MOD_GREEN
        SkillAttemptOutcome.NEEDS_PRACTICE -> "⚠" to MOD_ORANGE
        SkillAttemptOutcome.INCORRECT      -> "✗" to MOD_RED
        SkillAttemptOutcome.INCONCLUSIVE   -> "?" to MOD_LABEL
        SkillAttemptOutcome.NOT_ASSESSED   -> "–" to MOD_LABEL
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(text, fontSize = 13.sp, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

// ── Skill focus screen for Stroke and Allergy ─────────────────────────────────

@Composable
fun ModuleSkillFocusScreen(
    moduleTitle: String,
    recommendations: List<ModuleFocusedRecommendation>,
    skillStats: List<ModuleSkillStats>,
    onStartTraining: (String?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MOD_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Back", fontSize = 14.sp, color = MOD_TEAL)
            }
            Text("$moduleTitle Focus", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MOD_WHITE)
            Spacer(Modifier.width(60.dp))
        }

        if (recommendations.isEmpty() && skillStats.isEmpty()) {
            ModuleNoDataCard(moduleTitle) { onStartTraining(null) }
        } else {
            if (recommendations.isNotEmpty()) {
                Text("Practice Recommendations", fontSize = 12.sp, color = MOD_LABEL,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                recommendations.forEachIndexed { i, rec ->
                    ModuleFocusRecommendationCard(rank = i + 1, rec = rec, onStart = { onStartTraining(rec.skillTag) })
                }
            }
            if (skillStats.isNotEmpty()) {
                Text("Per-Skill Breakdown", fontSize = 12.sp, color = MOD_LABEL,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                skillStats.forEach { stats -> ModuleSkillStatCard(stats) }
            }
        }

        Text(
            "Training records do not indicate professional certification or clinical competency.",
            fontSize = 11.sp, color = MOD_LABEL.copy(alpha = 0.7f), lineHeight = 16.sp
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModuleFocusRecommendationCard(
    rank: Int,
    rec: ModuleFocusedRecommendation,
    onStart: () -> Unit
) {
    val accent = if (rec.priority >= 80) MOD_RED else if (rec.priority >= 60) MOD_ORANGE else MOD_TEAL
    Surface(shape = RoundedCornerShape(14.dp), color = MOD_CARD_BG, modifier = Modifier.fillMaxWidth()) {
        Column {
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(50), color = accent) {
                        Text("#$rank", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MOD_WHITE, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                    Text(rec.skillDescription, fontWeight = FontWeight.SemiBold, color = accent,
                        modifier = Modifier.weight(1f))
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(rec.reason, fontSize = 13.sp, color = MOD_WHITE, lineHeight = 19.sp)
                Text(rec.evidence, fontSize = 11.sp, color = MOD_SEC, lineHeight = 16.sp)
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MOD_TEAL)
                ) {
                    Text("Practise This Skill", fontWeight = FontWeight.Bold, color = Color(0xFF080E1C))
                }
            }
        }
    }
}

@Composable
private fun ModuleSkillStatCard(stats: ModuleSkillStats) {
    val pct = (stats.accuracy * 100).toInt()
    val accent = when {
        pct >= 80 -> MOD_GREEN
        pct >= 50 -> MOD_ORANGE
        else      -> MOD_RED
    }
    Surface(shape = RoundedCornerShape(12.dp), color = MOD_CARD_BG, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stats.skillDescription, fontSize = 13.sp, color = MOD_WHITE,
                    fontWeight = FontWeight.SemiBold)
                Text("${stats.totalAttempts} attempt(s) · ${stats.correctAttempts} correct",
                    fontSize = 11.sp, color = MOD_LABEL)
                if (stats.manualAdvances > 0 || stats.inconclusiveAttempts > 0 || stats.correctedAttempts > 0) {
                    Text(
                        "${stats.correctedAttempts} corrected · ${stats.inconclusiveAttempts} inconclusive · ${stats.manualAdvances} manual",
                        fontSize = 10.sp, color = MOD_LABEL
                    )
                }
                stats.latestOutcome?.let { outcome ->
                    val (label, color) = when (outcome) {
                        SkillAttemptOutcome.CORRECT        -> "Latest: correct" to MOD_GREEN
                        SkillAttemptOutcome.NEEDS_PRACTICE -> "Latest: needs practice" to MOD_ORANGE
                        SkillAttemptOutcome.INCORRECT      -> "Latest: incorrect" to MOD_RED
                        SkillAttemptOutcome.INCONCLUSIVE   -> "Latest: unconfirmed" to MOD_LABEL
                        SkillAttemptOutcome.NOT_ASSESSED   -> "Latest: not assessed" to MOD_LABEL
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.12f)) {
                        Text(label, fontSize = 10.sp, color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.12f)) {
                Text("$pct%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ModuleNoDataCard(moduleTitle: String, onStart: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MOD_CARD_BG, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📋", fontSize = 36.sp)
            Text("No $moduleTitle training data yet", fontWeight = FontWeight.SemiBold, color = MOD_WHITE)
            Text("Complete a $moduleTitle training session to see per-skill feedback and recommendations.",
                fontSize = 12.sp, color = MOD_LABEL, lineHeight = 17.sp)
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MOD_TEAL)
            ) {
                Text("Start $moduleTitle Training", fontWeight = FontWeight.Bold, color = Color(0xFF080E1C))
            }
        }
    }
}

/**
 * Compact assessment surface used over the live CameraX/AR experience.
 * Knowledge choices supplement the camera; they never replace or fabricate a visual result.
 */
@Composable
fun CameraTrainingPanel(
    session: ModuleTrainingSessionState,
    moduleTitle: String,
    voiceState: com.sanjeevani.ModuleVoiceState,
    onSelectOption: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onRetryStep: () -> Unit,
    onNextStep: () -> Unit,
    onContinueManually: () -> Unit,
    onStartVoice: () -> Unit,
    onPlacement: (Float, Float) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(session.sessionId, session.stepIndex) { mutableStateOf(true) }
    Surface(
        color = Color(0xEE080E1C),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        if (session.isFinished) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("✓ $moduleTitle saved", color = MOD_GREEN, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Assessed answers and any manual advances are stored in your skill history.", color = MOD_SEC, fontSize = 13.sp)
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Finish Training") }
            }
            return@Surface
        }

        val ss = session.stepState ?: return@Surface
        if (!expanded) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("$moduleTitle • ${session.stepIndex + 1}/${session.totalSteps}", color = MOD_TEAL, fontWeight = FontWeight.Bold)
                    Text(ss.step.stepTitle, color = MOD_WHITE, fontSize = 13.sp, maxLines = 1)
                }
                TextButton(onClick = { expanded = true }) { Text("Show choices") }
            }
            return@Surface
        }
        Column(
            Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(moduleTitle, color = MOD_TEAL, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${session.stepIndex + 1} / ${session.totalSteps}", color = MOD_WHITE, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = false }, contentPadding = PaddingValues(start = 12.dp)) {
                    Text("Hide", color = MOD_SEC)
                }
            }
            LinearProgressIndicator(
                progress = { (session.stepIndex + 1).toFloat() / session.totalSteps.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = MOD_TEAL, trackColor = MOD_DIM
            )
            Text(ss.step.stepTitle, color = MOD_WHITE, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(ss.step.questionText, color = MOD_WHITE, fontSize = 14.sp, lineHeight = 19.sp)

            if (ss.step.cameraRequirement != ModuleCameraRequirement.NONE) {
                Surface(
                    color = if (ss.cameraVerified) MOD_GREEN_BG else MOD_ORANGE_BG,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (ss.cameraVerified) "✓ Camera check ready" else "◉ Waiting for camera check",
                            color = if (ss.cameraVerified) MOD_GREEN else MOD_ORANGE,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            ss.cameraStatus ?: when (ss.step.cameraRequirement) {
                                ModuleCameraRequirement.FACE_REFERENCE_VISIBLE -> "Frame the person's face and ask them to smile. The camera verifies visibility only—not stroke."
                                ModuleCameraRequirement.BOTH_ARMS_VISIBLE -> "Ask them to raise both arms and keep both wrists and elbows visible."
                                ModuleCameraRequirement.PATIENT_LYING_FLAT -> "Show shoulders, hips, and arms while they lie on their back. Confirm the real surface yourself."
                                ModuleCameraRequirement.THIGH_REFERENCE_VISIBLE -> "Show the hip-to-knee area so AR can anchor the outer mid-thigh trainer target."
                                ModuleCameraRequirement.NONE -> ""
                            },
                            color = MOD_SEC,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            ss.step.options.forEach { option ->
                ModuleOptionCard(
                    option = option,
                    selected = ss.selectedOptionId == option.id,
                    locked = ss.locked,
                    attempt = ss.attempt,
                    onClick = { onSelectOption(option.id) }
                )
            }

            if (!ss.locked) {
                OutlinedButton(onClick = onStartVoice, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (voiceState.isListening) "● Listening…" else "🎙 Say option A, B, C, or D")
                }
                Button(
                    onClick = onSubmitAnswer,
                    enabled = ss.selectedOptionId != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Submit Answer") }
            } else {
                ss.attempt?.let { ModuleFeedbackCard(it) }
                when {
                    ss.canAdvance -> Button(onClick = onNextStep, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (session.stepIndex + 1 >= session.totalSteps) "Save Results" else "Next Step →")
                    }
                    ss.attempt?.knowledgeResult == KnowledgeResult.CORRECT && !ss.cameraVerified &&
                        ss.step.cameraRequirement != ModuleCameraRequirement.NONE -> Text(
                            "Answer confirmed. Complete the camera check above, or continue manually.",
                            color = MOD_ORANGE,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    session.phase != ModuleTrainingPhase.PLACEMENT -> Button(onClick = onRetryStep, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Try Again")
                    }
                }
            }

            if (session.phase == ModuleTrainingPhase.PLACEMENT) {
                PlacementSimulation(ss.placementX, ss.placementY, ss.attempt?.placementResult ?: PlacementResult.NOT_ASSESSED, onPlacement)
            }

            OutlinedButton(
                onClick = onContinueManually,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MOD_ORANGE)
            ) {
                Text("Continue Manually — mark unconfirmed", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
