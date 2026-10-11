package com.sanjeevani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.training.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionHistoryScreen(
    profile: LearnerProfile,
    onBack: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(L_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", tint = L_ACCENT, modifier = Modifier.size(28.dp))
            }
            Text(
                "Training History",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = L_PRI,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.width(36.dp))
        }

        val total = profile.totalSessions()
        if (total == 0) {
            Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = L_TER, modifier = Modifier.size(48.dp))
                    Text("No sessions yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = L_PRI)
                    Text(
                        "Complete a training module to see your history here.",
                        fontSize = 14.sp, color = L_SEC, lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Text("$total sessions recorded", fontSize = 13.sp, color = L_SEC, modifier = Modifier.padding(horizontal = 4.dp))

            TrainingModuleType.values().forEach { module ->
                val sessions = profile.moduleHistory[module] ?: emptyList()
                if (sessions.isNotEmpty()) {
                    ModuleHistorySection(module, sessions, fmt)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ModuleHistorySection(
    module: TrainingModuleType,
    sessions: List<SessionOutcome>,
    fmt: SimpleDateFormat
) {
    val (moduleIcon, iconTint) = when (module) {
        TrainingModuleType.CPR -> Icons.Rounded.Favorite to L_RED
        TrainingModuleType.STROKE_FAST -> Icons.Rounded.Psychology to L_ORANGE
        TrainingModuleType.ALLERGIC_REACTION -> Icons.Rounded.Vaccines to L_ACCENT
    }
    val sortedSessions = sessions.sortedByDescending { it.endMs }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(moduleIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            Text(
                module.displayName.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = L_SEC,
                letterSpacing = 0.8.sp
            )
        }

        Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                sortedSessions.take(5).forEachIndexed { index, session ->
                    SessionHistoryRow(session, fmt)
                    if (index < minOf(sortedSessions.size, 5) - 1) {
                        HorizontalDivider(color = L_SEP, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        if (sortedSessions.size > 5) {
            Text(
                "+${sortedSessions.size - 5} more sessions",
                fontSize = 12.sp,
                color = L_ACCENT,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SessionHistoryRow(session: SessionOutcome, fmt: SimpleDateFormat) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(
                fmt.format(Date(session.endMs)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = L_PRI
            )
            Text(
                "${session.durationSecs}s  ·  ${session.passCount}/${session.totalCriteria} criteria",
                fontSize = 12.sp,
                color = L_SEC
            )
        }
        ResultBadge(session.overallResult)
    }
}

@Composable
private fun ResultBadge(result: CriterionResult) {
    val (label, color) = when (result) {
        CriterionResult.PASS -> "PASS" to L_GREEN
        CriterionResult.NEEDS_PRACTICE -> "PRACTICE" to L_ORANGE
        CriterionResult.INCONCLUSIVE -> "REVIEW" to L_SEC
        CriterionResult.NOT_ASSESSED -> "PARTIAL" to L_TER
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
