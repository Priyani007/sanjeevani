package com.sanjeevani.ui

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
import com.sanjeevani.training.*

@Composable
fun QuizResultScreen(
    session: ActiveQuizSession,
    score: QuizAttemptScore,
    recommendations: List<QuizRecommendation>,
    onStartPersonalizedPractice: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ResultHeader(session, score)
        ScoreSummaryCard(score)

        if (score.criticalSafetyErrors > 0) {
            CriticalSafetyErrorBanner(score)
        }

        if (score.missedSkillTags.isNotEmpty()) {
            SkillBreakdownCard(score)
        }

        if (recommendations.isNotEmpty()) {
            PersonalizedPracticeCallout(recommendations, onStartPersonalizedPractice)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Try Again") }
            FilledTonalButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text("Quiz Home") }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultHeader(session: ActiveQuizSession, score: QuizAttemptScore) {
    val emoji = when {
        score.criticalSafetyErrors > 0 -> "⚠️"
        score.accuracy >= 0.85f        -> "🎉"
        score.accuracy >= 0.6f         -> "📈"
        else                           -> "💪"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "${session.category.displayName} — ${session.difficulty.label}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            resultHeadline(score),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun resultHeadline(score: QuizAttemptScore): String = when {
    score.criticalSafetyErrors > 0 -> "Review the critical safety questions below."
    score.accuracy >= 0.85f        -> "Strong performance — keep it up!"
    score.accuracy >= 0.6f         -> "Good effort — a few areas to revisit."
    else                           -> "More practice on this topic will help."
}

@Composable
private fun ScoreSummaryCard(score: QuizAttemptScore) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreItem("${score.correctAnswers}/${score.totalQuestions}", "Correct")
            VerticalDivider(modifier = Modifier.height(40.dp))
            ScoreItem("${(score.accuracy * 100).toInt()}%", "Accuracy")
            VerticalDivider(modifier = Modifier.height(40.dp))
            ScoreItem("${score.criticalSafetyErrors}", "Critical Errors",
                valueColor = if (score.criticalSafetyErrors > 0) Color(0xFFD32F2F) else null)
        }
    }
}

@Composable
private fun ScoreItem(value: String, label: String, valueColor: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CriticalSafetyErrorBanner(score: QuizAttemptScore) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFECEC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "⚠️ Critical Safety Error${if (score.criticalSafetyErrors > 1) "s" else ""}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
            Text(
                "You answered ${score.criticalSafetyErrors} critical safety question${if (score.criticalSafetyErrors > 1) "s" else ""} incorrectly. " +
                    "In a real emergency, this type of mistake can have serious consequences. " +
                    "Personalized practice below targets these gaps.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB71C1C)
            )
        }
    }
}

@Composable
private fun SkillBreakdownCard(score: QuizAttemptScore) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Skills to Review", fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall)
            score.missedSkillTags.take(5).forEach { tag ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary)
                    Text(
                        tag.replace('_', ' ').replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizedPracticeCallout(
    recommendations: List<QuizRecommendation>,
    onStartPersonalizedPractice: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎯 Personalized Practice Ready",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "${recommendations.size} recommendation${if (recommendations.size != 1) "s" else ""} based on your results.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(onClick = onStartPersonalizedPractice, modifier = Modifier.fillMaxWidth()) {
                Text("View Personalized Practice Plan")
            }
        }
    }
}
