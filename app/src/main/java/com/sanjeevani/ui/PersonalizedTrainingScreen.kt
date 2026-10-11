package com.sanjeevani.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun PersonalizedTrainingScreen(
    recommendations: List<QuizRecommendation>,
    onStartPractice: (QuizCategory, QuizDifficulty) -> Unit,
    onBack: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("Personalized Practice Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("Based on your quiz results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (recommendations.isEmpty()) {
            EmptyRecommendationsCard()
        } else {
            Text(
                "Your practice plan targets gaps identified from your quiz attempts, " +
                    "prioritised by clinical importance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            recommendations.forEachIndexed { index, rec ->
                RecommendationCard(index + 1, rec, onStartPractice)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyRecommendationsCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("✅", fontSize = 36.sp)
            Text("No urgent gaps found", fontWeight = FontWeight.SemiBold)
            Text("Complete more quizzes to get personalised recommendations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecommendationCard(
    rank: Int,
    rec: QuizRecommendation,
    onStartPractice: (QuizCategory, QuizDifficulty) -> Unit
) {
    val isCritical = rec.reason == QuizRecommendationReason.CRITICAL_SAFETY_ERROR
    val borderColor = if (isCritical) Color(0xFFD32F2F) else Color.Transparent
    val headerColor = when (rec.reason) {
        QuizRecommendationReason.CRITICAL_SAFETY_ERROR -> Color(0xFFFFECEC)
        QuizRecommendationReason.REPEATED_SKILL_MISS   -> Color(0xFFFFF3E0)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header strip
            Surface(color = headerColor, shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isCritical) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "#$rank",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        rec.category.displayName,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCritical) Color(0xFFD32F2F)
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.weight(1f))
                    Text(rec.category.emoji, fontSize = 20.sp)
                }
            }

            // Body
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(rec.reasonText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(rec.evidence, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                rec.targetSkillTag?.let { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Target skill:", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                tag.replace('_', ' '),
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Suggested level: ${rec.suggestedDifficulty.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onStartPractice(rec.category, rec.suggestedDifficulty) }) {
                        Text("Start Practice")
                    }
                }
            }
        }
    }
}
