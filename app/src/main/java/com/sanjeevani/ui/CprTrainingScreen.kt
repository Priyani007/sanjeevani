package com.sanjeevani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.CprStepState
import com.sanjeevani.CprTrainingPhase
import com.sanjeevani.CprTrainingSessionState
import com.sanjeevani.training.*

private val BG        = Color(0xFF080E1C)
private val CARD_BG   = Color(0xFF121929)
private val TEAL      = Color(0xFF00C4B4)
private val GREEN     = Color(0xFF4EE0A0)
private val GREEN_BG  = Color(0xFF0A2018)
private val RED       = Color(0xFFE53935)
private val RED_BG    = Color(0xFF2A0808)
private val ORANGE    = Color(0xFFFF9B2E)
private val ORANGE_BG = Color(0xFF2A1600)
private val DIM       = Color(0xFF1A2438)
private val LABEL     = Color(0xFF5A6580)
private val SEC       = Color(0xFF7A8899)
private val WHITE     = Color.White

@Composable
fun CprTrainingScreen(
    session: CprTrainingSessionState,
    onSelectOption: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextStep: () -> Unit,
    onCommitCamera: () -> Unit,
    onSkipCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    if (session.isFinished) {
        CprTrainingCompleteScreen(session = session, onDismiss = onDismiss)
        return
    }

    val stepState = session.stepState ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
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
                Text("✕ Exit", fontSize = 13.sp, color = LABEL)
            }
            Text("CPR Training", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WHITE)
            Text("${session.stepIndex + 1} / ${session.totalSteps}",
                fontSize = 13.sp, color = LABEL)
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { (session.stepIndex + 1).toFloat() / session.totalSteps },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = TEAL,
            trackColor = DIM
        )

        val step = stepState.step

        // Step title pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = TEAL.copy(alpha = 0.12f)
        ) {
            Text(
                "Step ${step.stepIndex + 1}: ${step.stepTitle}",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = TEAL, letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Scenario card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CARD_BG,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Scenario", fontSize = 11.sp, color = TEAL, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp)
                Text(step.scenarioContext, fontSize = 14.sp, color = SEC, lineHeight = 20.sp)
            }
        }

        // Question
        Text(step.questionText, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = WHITE, lineHeight = 22.sp)

        // Camera phase overlay
        if (session.phase == CprTrainingPhase.AWAITING_CAMERA) {
            CameraPhaseCard(
                instruction  = step.cameraAssessmentInstruction,
                spatialNote  = session.latestSpatialAction?.let { CameraHint.fromAction(it) },
                onCommit     = onCommitCamera,
                onSkip       = onSkipCamera
            )
        } else {
            // Options
            step.options.forEach { option ->
                OptionCard(
                    option     = option,
                    selected   = stepState.selectedOptionId == option.id,
                    locked     = stepState.locked,
                    attempt    = stepState.attempt,
                    onClick    = { if (!stepState.locked) onSelectOption(option.id) }
                )
            }

            // Feedback after lock
            if (stepState.locked) {
                val attempt = stepState.attempt
                if (attempt != null) {
                    FeedbackCard(attempt)
                }
                // Next / Submit button
                Button(
                    onClick = onNextStep,
                    modifier = Modifier.fillMaxWidth(),
                    colors  = ButtonDefaults.buttonColors(containerColor = TEAL)
                ) {
                    Text(
                        if (session.stepIndex + 1 >= session.totalSteps) "See Results" else "Next Step →",
                        fontWeight = FontWeight.Bold, color = Color(0xFF080E1C)
                    )
                }
            } else {
                // Submit button
                Button(
                    onClick  = onSubmitAnswer,
                    enabled  = stepState.selectedOptionId != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = TEAL)
                ) {
                    Text(
                        if (step.useCameraForAssessment) "Submit & Show Camera" else "Submit Answer",
                        fontWeight = FontWeight.Bold,
                        color = if (stepState.selectedOptionId != null) Color(0xFF080E1C) else LABEL
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionCard(
    option: CprTrainingOption,
    selected: Boolean,
    locked: Boolean,
    attempt: CprSkillAttempt?,
    onClick: () -> Unit
) {
    val (bg, border, textColor) = when {
        locked && attempt != null && option.id == attempt.correctOptionId ->
            Triple(GREEN_BG, GREEN, WHITE)
        locked && attempt != null && option.id == attempt.selectedOptionId && !attempt.knowledgeResult.let { it == KnowledgeResult.CORRECT } ->
            Triple(RED_BG, RED, WHITE)
        selected && !locked ->
            Triple(TEAL.copy(alpha = 0.12f), TEAL, WHITE)
        else ->
            Triple(CARD_BG, DIM, SEC)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked) { onClick() }
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = border.copy(alpha = 0.2f)
        ) {
            Text(
                option.id,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = border
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(option.text, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
            if (locked && (option.id == attempt?.selectedOptionId || option.id == attempt?.correctOptionId)) {
                Text(option.feedback, fontSize = 12.sp, color = SEC, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun FeedbackCard(attempt: CprSkillAttempt) {
    val isCorrect = attempt.outcome == SkillAttemptOutcome.CORRECT
    val isNeeds   = attempt.outcome == SkillAttemptOutcome.NEEDS_PRACTICE
    val (bg, accent, icon) = when {
        isCorrect -> Triple(GREEN_BG, GREEN, "✓")
        isNeeds   -> Triple(ORANGE_BG, ORANGE, "⚠")
        else      -> Triple(RED_BG, RED, "✗")
    }
    Surface(shape = RoundedCornerShape(12.dp), color = bg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(icon, fontSize = 16.sp, color = accent)
                Text(
                    when {
                        isCorrect -> "Correct"
                        isNeeds   -> "Practise needed"
                        else      -> "Incorrect"
                    },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent
                )
            }
            if (attempt.cameraObservationNote.isNotEmpty()) {
                Text(attempt.cameraObservationNote, fontSize = 12.sp, color = SEC, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun CameraPhaseCard(
    instruction: String,
    spatialNote: String?,
    onCommit: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(shape = RoundedCornerShape(14.dp), color = CARD_BG, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📷 Camera Assessment", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TEAL)
            Text(instruction, fontSize = 13.sp, color = SEC, lineHeight = 19.sp)

            if (spatialNote != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = TEAL.copy(alpha = 0.08f)) {
                    Text(spatialNote, fontSize = 12.sp, color = TEAL,
                        modifier = Modifier.padding(10.dp), lineHeight = 17.sp)
                }
            }

            Text(
                "Note: camera feedback is a visual aid only. Practise on a manikin for physical accuracy.",
                fontSize = 11.sp, color = LABEL, lineHeight = 16.sp
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text("Skip Camera", fontSize = 13.sp, color = LABEL)
                }
                Button(
                    onClick = onCommit, modifier = Modifier.weight(1f),
                    colors  = ButtonDefaults.buttonColors(containerColor = TEAL)
                ) {
                    Text("Record Observation", fontSize = 13.sp, color = Color(0xFF080E1C),
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Converts SpatialAction display hint text for the camera phase card
private object CameraHint {
    fun fromAction(action: com.sanjeevani.model.SpatialAction): String = when (action) {
        com.sanjeevani.model.SpatialAction.CORRECT       -> "Hands appear on target. Hold still and record."
        com.sanjeevani.model.SpatialAction.MOVE_DOWN     -> "Hands appear too high — move slightly toward the abdomen."
        com.sanjeevani.model.SpatialAction.MOVE_UP       -> "Hands appear too low — move slightly toward the shoulders."
        com.sanjeevani.model.SpatialAction.MOVE_LEFT     -> "Hands appear off-centre to the left — move right."
        com.sanjeevani.model.SpatialAction.MOVE_RIGHT    -> "Hands appear off-centre to the right — move left."
        com.sanjeevani.model.SpatialAction.TRACKING_LOST -> "Camera cannot detect hands clearly. Check lighting and camera angle."
        com.sanjeevani.model.SpatialAction.UNSURE        -> "Observation inconclusive. Adjust position and hold still."
    }
}

// ── Completion screen ──────────────────────────────────────────────────────

@Composable
private fun CprTrainingCompleteScreen(
    session: CprTrainingSessionState,
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
            .background(BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(emoji, fontSize = 56.sp)
        Text(headline, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WHITE)
        Text(
            "$pct% of steps answered correctly",
            fontSize = 15.sp, color = TEAL
        )

        // Per-skill summary
        Surface(shape = RoundedCornerShape(14.dp), color = CARD_BG, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Step Summary", fontSize = 12.sp, color = TEAL, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp)
                session.completedAttempts.forEach { attempt ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(attempt.skillDescription, fontSize = 13.sp, color = SEC,
                            modifier = Modifier.weight(1f), maxLines = 1)
                        OutcomeChip(attempt.outcome)
                    }
                }
            }
        }

        Text(
            "This session records your progress. Check the Analytics tab for skill-level trends and " +
                "personalised practice recommendations.",
            fontSize = 12.sp, color = LABEL, lineHeight = 17.sp
        )
        Text(
            "Training records do not indicate professional certification or clinical competency.",
            fontSize = 11.sp, color = LABEL.copy(alpha = 0.7f), lineHeight = 16.sp
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = TEAL)
        ) {
            Text("Done", fontWeight = FontWeight.Bold, color = Color(0xFF080E1C))
        }
    }
}

@Composable
private fun OutcomeChip(outcome: SkillAttemptOutcome) {
    val (text, color) = when (outcome) {
        SkillAttemptOutcome.CORRECT         -> "✓" to GREEN
        SkillAttemptOutcome.NEEDS_PRACTICE  -> "⚠" to ORANGE
        SkillAttemptOutcome.INCORRECT       -> "✗" to RED
        SkillAttemptOutcome.INCONCLUSIVE    -> "?" to LABEL
        SkillAttemptOutcome.NOT_ASSESSED    -> "–" to LABEL
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(text, fontSize = 13.sp, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}
