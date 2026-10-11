package com.sanjeevani.ui

import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.training.*

@Composable
fun QuizSessionScreen(
    session: ActiveQuizSession,
    onSelectOption: (String) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()
    val question = session.currentQuestion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuizSessionHeader(session, onDismiss)
        QuizProgressBar(session)

        if (question != null) {
            ScenarioCard(question.scenario)
            QuestionText(question.questionText, question.isCriticalSafety)
            OptionsList(
                question   = question,
                session    = session,
                onSelect   = onSelectOption
            )
            if (session.isAnswerLocked) {
                FeedbackPanel(question, session.answers.lastOrNull())
                Button(
                    onClick  = onNextQuestion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isLast = session.currentIndex == session.questions.size - 1
                    Text(if (isLast) "See Results" else "Next Question →")
                }
            } else {
                Button(
                    onClick  = onSubmitAnswer,
                    enabled  = session.selectedOptionIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit Answer") }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuizSessionHeader(session: ActiveQuizSession, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(session.category.displayName, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Text(session.difficulty.label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Quit quiz")
        }
    }
}

@Composable
private fun QuizProgressBar(session: ActiveQuizSession) {
    val displayIndex = (session.currentIndex + 1).coerceAtMost(session.questions.size)
    val total = session.questions.size
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Question $displayIndex of $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { displayIndex.toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScenarioCard(scenario: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Scenario", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(scenario, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun QuestionText(text: String, isCritical: Boolean) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isCritical) {
            Text("⚠️", fontSize = 18.sp)
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OptionsList(
    question: QuizQuestion,
    session: ActiveQuizSession,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        question.options.forEach { option ->
            OptionItem(
                option   = option,
                question = question,
                session  = session,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun OptionItem(
    option: QuizOption,
    question: QuizQuestion,
    session: ActiveQuizSession,
    onSelect: (String) -> Unit
) {
    val isSelected = option.id in session.selectedOptionIds
    val isCorrect  = option.id in question.correctOptionIds
    val locked     = session.isAnswerLocked

    val borderColor = when {
        locked && isCorrect                 -> Color(0xFF2E7D32)
        locked && isSelected && !isCorrect  -> Color(0xFFD32F2F)
        isSelected                          -> MaterialTheme.colorScheme.primary
        else                                -> MaterialTheme.colorScheme.outline
    }
    val surfaceColor = when {
        locked && isCorrect                 -> Color(0xFFE8F5E9)
        locked && isSelected && !isCorrect  -> Color(0xFFFFEBEE)
        isSelected                          -> MaterialTheme.colorScheme.primaryContainer
        else                                -> MaterialTheme.colorScheme.surface
    }

    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = !locked) { onSelect(option.id) }
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                option.id,
                fontWeight = FontWeight.Bold,
                color = borderColor,
                modifier = Modifier.width(20.dp)
            )
            Text(option.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (locked && isCorrect) Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            if (locked && isSelected && !isCorrect) Text("✗", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeedbackPanel(question: QuizQuestion, lastAnswer: QuizAnswerRecord?) {
    val isCorrect = lastAnswer?.isCorrect ?: false
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (isCorrect) "✓ Correct!" else "✗ Incorrect",
                fontWeight = FontWeight.Bold,
                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFE65100)
            )
            if (question.isCriticalSafety && !isCorrect) {
                Text(
                    "⚠️ This is a critical safety question. In a real emergency, this mistake could cause serious harm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }
            val selectedOption = question.options.find { it.id in (lastAnswer?.selectedOptionIds ?: emptySet()) }
            selectedOption?.explanation?.let { exp ->
                Text(exp, style = MaterialTheme.typography.bodySmall)
            }
            if (!isCorrect) {
                val correctOption = question.options.find { it.id in question.correctOptionIds }
                correctOption?.let { co ->
                    Text(
                        "Correct answer (${co.id}): ${co.explanation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
            Text(
                "Learning objective: ${question.learningObjective}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
