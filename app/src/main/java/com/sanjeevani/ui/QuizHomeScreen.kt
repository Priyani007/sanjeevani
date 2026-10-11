package com.sanjeevani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.training.*

@Composable
fun QuizHomeScreen(
    profile: QuizLearnerProfile,
    recommendations: List<QuizRecommendation>,
    isDemoData: Boolean,
    onStartQuiz: (QuizCategory, QuizDifficulty) -> Unit,
    onLoadDemo: () -> Unit,
    onClearData: () -> Unit,
    onViewAnalytics: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuizHeader(profile, isDemoData, onViewAnalytics)

        if (profile.attempts.isEmpty()) {
            QuizEmptyState(onLoadDemo)
        } else {
            if (isDemoData) DemoBanner()
            RecommendationCallout(recommendations, onStartQuiz)
            QuizStatsRow(profile, isDemoData)
        }

        Text(
            "Choose a Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        QuizCategory.values().forEach { category ->
            CategoryCard(
                category    = category,
                profile     = profile,
                isDemoData  = isDemoData,
                onStartQuiz = onStartQuiz
            )
        }

        if (profile.attempts.isNotEmpty()) {
            DataControlRow(isDemoData, onLoadDemo, onClearData)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuizHeader(
    profile: QuizLearnerProfile,
    isDemoData: Boolean,
    onViewAnalytics: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Quiz Practice", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Test your emergency knowledge",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (profile.attempts.isNotEmpty()) {
            FilledTonalButton(onClick = onViewAnalytics) { Text("Analytics") }
        }
    }
}

@Composable
private fun DemoBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("👤", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Showing demo data — Aarav Kumar (14-day practice history)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun QuizEmptyState(onLoadDemo: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", fontSize = 40.sp)
            Text("No quiz attempts yet", fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Start a quiz below, or load a sample learner profile to see how analytics work.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onLoadDemo) { Text("Load Demo Profile") }
        }
    }
}

@Composable
private fun RecommendationCallout(
    recommendations: List<QuizRecommendation>,
    onStartQuiz: (QuizCategory, QuizDifficulty) -> Unit
) {
    val top = recommendations.firstOrNull() ?: return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (top.reason) {
            QuizRecommendationReason.CRITICAL_SAFETY_ERROR -> Color(0xFFFFECEC)
            QuizRecommendationReason.REPEATED_SKILL_MISS   -> Color(0xFFFFF3E0)
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null,
                    tint = when (top.reason) {
                        QuizRecommendationReason.CRITICAL_SAFETY_ERROR -> Color(0xFFD32F2F)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Recommended next", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(top.reasonText, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onStartQuiz(top.category, top.suggestedDifficulty) }) {
                Text("Start ${top.category.displayName} (${top.suggestedDifficulty.label})")
            }
        }
    }
}

@Composable
private fun QuizStatsRow(profile: QuizLearnerProfile, isDemoData: Boolean) {
    val attempts = if (isDemoData) profile.attempts else profile.realAttempts
    val accuracy = profile.overallAccuracy(isDemoData)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip(Modifier.weight(1f), "${attempts.size}", "Attempts")
        StatChip(Modifier.weight(1f), "${(accuracy * 100).toInt()}%", "Accuracy")
        StatChip(Modifier.weight(1f), "${profile.categoriesPractised(isDemoData).size}/3", "Topics")
    }
}

@Composable
private fun StatChip(modifier: Modifier, value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryCard(
    category: QuizCategory,
    profile: QuizLearnerProfile,
    isDemoData: Boolean,
    onStartQuiz: (QuizCategory, QuizDifficulty) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val accuracy = profile.categoryAccuracy(category, isDemoData)
    val attempts = profile.attemptsForCategory(category, isDemoData)
    val hasCritical = profile.hasUnresolvedCriticalSafetyError(isDemoData) &&
        attempts.any { it.criticalSafetyErrors > 0 }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (hasCritical) 2.dp else 0.dp,
                color = if (hasCritical) Color(0xFFD32F2F) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.displayName, fontWeight = FontWeight.SemiBold)
                    if (attempts.isNotEmpty()) {
                        Text(
                            "${attempts.size} attempt${if (attempts.size != 1) "s" else ""} · ${(accuracy * 100).toInt()}% accuracy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Not started", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (hasCritical) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(Modifier.width(4.dp))
                }
                Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizDifficulty.values().forEach { difficulty ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(difficulty.label, fontWeight = FontWeight.Medium)
                                Text(
                                    difficultyDescription(difficulty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onStartQuiz(category, difficulty) }
                            ) { Text("Start") }
                        }
                    }
                }
            }
        }
    }
}

private fun difficultyDescription(d: QuizDifficulty): String = when (d) {
    QuizDifficulty.BEGINNER     -> "Core recognition & first steps"
    QuizDifficulty.INTERMEDIATE -> "Atypical presentations & sequences"
    QuizDifficulty.ADVANCED     -> "Edge cases & critical decisions"
}

@Composable
private fun DataControlRow(isDemoData: Boolean, onLoadDemo: () -> Unit, onClearData: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isDemoData) {
            OutlinedButton(onClick = onLoadDemo, modifier = Modifier.weight(1f)) { Text("Load Demo") }
        }
        OutlinedButton(
            onClick = onClearData,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) { Text("Clear Data") }
    }
}
