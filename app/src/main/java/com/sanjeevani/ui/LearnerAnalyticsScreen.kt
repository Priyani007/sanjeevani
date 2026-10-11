package com.sanjeevani.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.training.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BG        = L_BG
private val CARD_BG   = L_CARD
private val TEAL      = L_ACCENT
private val GREEN     = L_GREEN
private val GREEN_BG  = L_GREEN.copy(alpha = 0.10f)
private val ORANGE    = L_ORANGE
private val ORANGE_BG = L_ORANGE.copy(alpha = 0.10f)
private val DIM       = Color(0xFFE5E5EA)
private val LABEL     = L_SEC
private val SEC       = L_SEC
private val WHITE     = L_PRI

@Composable
fun LearnerAnalyticsScreen(
    profile: LearnerProfile,
    recommendations: List<PracticeRecommendation>,
    isDemoData: Boolean,
    onLoadDemo: () -> Unit,
    onClearData: () -> Unit,
    onBack: () -> Unit,
    cprTrainingProfile: com.sanjeevani.training.CprTrainingProfile = com.sanjeevani.training.CprTrainingProfile(),
    onOpenCprFocus: (() -> Unit)? = null,
    strokeTrainingProfile: com.sanjeevani.training.ModuleTrainingProfile = com.sanjeevani.training.ModuleTrainingProfile("STROKE_FAST"),
    onOpenStrokeFocus: (() -> Unit)? = null,
    allergyTrainingProfile: com.sanjeevani.training.ModuleTrainingProfile = com.sanjeevani.training.ModuleTrainingProfile("ALLERGIC_REACTION"),
    onOpenAllergyFocus: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Navigation header ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", tint = TEAL, modifier = Modifier.size(28.dp))
            }
            Text("Learner Analytics", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WHITE,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.width(36.dp))
        }

        // ── Profile card ───────────────────────────────────────────────
        if (isDemoData) {
            DemoProfileCard()
        }

        // ── Overview stats ─────────────────────────────────────────────
        if (profile.totalSessions() > 0) {
            OverviewStatsRow(profile)
        }

        // ── Per-module analytics ───────────────────────────────────────
        val modulesWithSessions = TrainingModuleType.values()
            .filter { (profile.moduleHistory[it]?.size ?: 0) > 0 }

        if (modulesWithSessions.isEmpty()) {
            EmptyStateCard(onLoadDemo)
        } else {
            modulesWithSessions.forEach { module ->
                val sessions = (profile.moduleHistory[module] ?: emptyList())
                    .sortedBy { it.startMs }
                val repeatedWeak = profile.repeatedWeakCriteria(module)
                ModuleAnalyticsCard(module, sessions, repeatedWeak)
            }

            // ── Recommendations ────────────────────────────────────────
            if (recommendations.isNotEmpty()) {
                RecommendationsSection(recommendations)
            }
        }

        // ── CPR skill-level breakdown ──────────────────────────────────
        val cprStats = cprTrainingProfile.skillStats(demoMode = isDemoData)
        if (cprStats.isNotEmpty()) {
            CprSkillBreakdownSection(cprStats, onOpenCprFocus)
        }

        // ── Stroke skill-level breakdown ───────────────────────────────
        // Module demo records are never blended into real learner analytics.
        val strokeStats = strokeTrainingProfile.skillStats(demoMode = false)
        if (strokeStats.isNotEmpty()) {
            ModuleSkillBreakdownSection(
                title       = "STROKE SKILL BREAKDOWN",
                stats       = strokeStats,
                onOpenFocus = onOpenStrokeFocus
            )
        }

        // ── Allergy skill-level breakdown ──────────────────────────────
        val allergyStats = allergyTrainingProfile.skillStats(demoMode = false)
        if (allergyStats.isNotEmpty()) {
            ModuleSkillBreakdownSection(
                title       = "ALLERGY SKILL BREAKDOWN",
                stats       = allergyStats,
                onOpenFocus = onOpenAllergyFocus
            )
        }

        // ── Demo data controls ─────────────────────────────────────────
        DataControls(isDemoData, profile.totalSessions() > 0, onLoadDemo, onClearData)
    }
}

// ── Profile card ──────────────────────────────────────────────────────────

@Composable
private fun DemoProfileCard() {
    Surface(
        color = CARD_BG,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TEAL.copy(alpha = 0.12f))
            ) {
                Text("AK", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TEAL)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Aarav Kumar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WHITE)
                Surface(
                    color = TEAL.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Demo Learner",
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = TEAL, letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Overview stats row ────────────────────────────────────────────────────

@Composable
private fun OverviewStatsRow(profile: LearnerProfile) {
    val activeModules = TrainingModuleType.values().count { (profile.moduleHistory[it]?.size ?: 0) > 0 }
    val bestPassRate = TrainingModuleType.values().mapNotNull { module ->
        val sessions = profile.moduleHistory[module] ?: return@mapNotNull null
        if (sessions.isEmpty()) return@mapNotNull null
        val last = sessions.maxByOrNull { it.endMs } ?: return@mapNotNull null
        if (last.totalCriteria == 0) return@mapNotNull null
        last.passCount.toFloat() / last.totalCriteria.toFloat()
    }.maxOrNull() ?: 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(
            label = "Sessions",
            value = "${profile.totalSessions()}",
            color = TEAL,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Modules",
            value = "$activeModules / ${TrainingModuleType.values().size}",
            color = Color(0xFF6C9FFF),
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Best Score",
            value = "${(bestPassRate * 100).toInt()}%",
            color = if (bestPassRate >= 0.75f) GREEN else ORANGE,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = CARD_BG,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = LABEL, letterSpacing = 0.3.sp)
        }
    }
}

// ── Module analytics card ─────────────────────────────────────────────────

@Composable
private fun ModuleAnalyticsCard(
    module: TrainingModuleType,
    sessions: List<SessionOutcome>,
    repeatedWeak: List<CriterionKey>
) {
    val moduleIcon = when (module) {
        TrainingModuleType.CPR -> Icons.Rounded.Favorite to L_RED
        TrainingModuleType.STROKE_FAST -> Icons.Rounded.Psychology to L_ORANGE
        TrainingModuleType.ALLERGIC_REACTION -> Icons.Rounded.Vaccines to L_ACCENT
    }
    val daysFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    val lastSession = sessions.maxByOrNull { it.endMs }
    val daysSince = lastSession?.let {
        val diff = System.currentTimeMillis() - it.endMs
        (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    Surface(
        color = CARD_BG,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Module header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(moduleIcon.second.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(moduleIcon.first, contentDescription = null, tint = moduleIcon.second, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(module.displayName, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = WHITE)
                        Text(
                            "${sessions.size} sessions" + (daysSince?.let { " · ${if (it == 0) "today" else "${it}d ago"}" } ?: ""),
                            fontSize = 11.sp, color = LABEL
                        )
                    }
                }
                // Latest session badge
                lastSession?.let { s ->
                    val passRate = if (s.totalCriteria > 0) s.passCount.toFloat() / s.totalCriteria else 0f
                    Surface(
                        color = if (passRate >= 0.99f) GREEN_BG else ORANGE_BG,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${s.passCount}/${s.totalCriteria}",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (passRate >= 0.99f) GREEN else ORANGE,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Session trend
            SectionLabel("Session Trend")
            SessionTrendRow(sessions, daysFmt)

            // Criterion heat map
            SectionLabel("Skill Breakdown  (oldest → newest)")
            CriterionHeatMap(
                criteria = AssessmentEngine.criteriaFor(module),
                sessions = sessions,
                repeatedWeak = repeatedWeak,
                dateFmt = daysFmt
            )

            // Persistent gap warning
            if (repeatedWeak.isNotEmpty()) {
                PersistentGapBanner(repeatedWeak)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = LABEL, letterSpacing = 0.8.sp
    )
}

// ── Session trend (coloured dots) ─────────────────────────────────────────

@Composable
private fun SessionTrendRow(sessions: List<SessionOutcome>, dateFmt: SimpleDateFormat) {
    val display = sessions.takeLast(6)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        display.forEachIndexed { idx, s ->
            val (dotColor, bgColor) = when (s.overallResult) {
                CriterionResult.PASS -> GREEN to GREEN_BG
                CriterionResult.NEEDS_PRACTICE -> ORANGE to ORANGE_BG
                else -> L_TER to L_FILL
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.5.dp, dotColor.copy(alpha = 0.7f), CircleShape)
                ) {
                    val dotIcon = when (s.overallResult) {
                        CriterionResult.PASS -> Icons.Rounded.Check
                        CriterionResult.NEEDS_PRACTICE -> Icons.Rounded.PriorityHigh
                        else -> Icons.Rounded.QuestionMark
                    }
                    Icon(dotIcon, contentDescription = null, tint = dotColor, modifier = Modifier.size(14.dp))
                }
                Text(
                    dateFmt.format(Date(s.endMs)),
                    fontSize = 8.sp, color = LABEL
                )
            }
            // Connector line (not after last)
            if (idx < display.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(1.dp)
                        .align(Alignment.CenterVertically)
                        .background(L_SEP)
                )
            }
        }

        // Legend
        Spacer(Modifier.weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.End
        ) {
            LegendItem(GREEN, "Pass")
            LegendItem(ORANGE, "Needs practice")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, fontSize = 9.sp, color = LABEL)
    }
}

// ── Criterion heat map ────────────────────────────────────────────────────

@Composable
private fun CriterionHeatMap(
    criteria: List<CriterionKey>,
    sessions: List<SessionOutcome>,
    repeatedWeak: List<CriterionKey>,
    dateFmt: SimpleDateFormat
) {
    val display = sessions.takeLast(6)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // Column date headers
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            display.forEach { s ->
                Box(
                    modifier = Modifier.size(width = 34.dp, height = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dateFmt.format(Date(s.endMs)).take(6),
                        fontSize = 8.sp, color = LABEL
                    )
                }
            }
        }

        HorizontalDivider(color = L_SEP, thickness = 0.5.dp)

        // Criterion rows
        criteria.forEach { key ->
            val isWeak = key in repeatedWeak
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Criterion name (truncated)
                Row(
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWeak) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = ORANGE, modifier = Modifier.size(10.dp))
                    }
                    Text(
                        key.description,
                        fontSize = 10.sp,
                        color = if (isWeak) ORANGE else SEC,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Session cells
                display.forEach { session ->
                    val result = session.criteria.find { it.key == key }?.result
                    val (cellBg, borderColor) = when (result) {
                        CriterionResult.PASS          -> GREEN_BG to GREEN.copy(alpha = 0.6f)
                        CriterionResult.NEEDS_PRACTICE -> ORANGE_BG to ORANGE.copy(alpha = 0.6f)
                        CriterionResult.INCONCLUSIVE  -> L_FILL to L_TER
                        else                           -> L_FILL to L_SEP
                    }
                    val cellIcon = when (result) {
                        CriterionResult.PASS          -> Icons.Rounded.Check
                        CriterionResult.NEEDS_PRACTICE -> Icons.Rounded.Close
                        CriterionResult.INCONCLUSIVE  -> Icons.Rounded.QuestionMark
                        else                           -> null
                    }
                    val iconColor = when (result) {
                        CriterionResult.PASS          -> GREEN
                        CriterionResult.NEEDS_PRACTICE -> ORANGE
                        CriterionResult.INCONCLUSIVE  -> L_TER
                        else                           -> L_TER
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp, 26.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(cellBg)
                            .border(0.5.dp, borderColor, RoundedCornerShape(5.dp))
                    ) {
                        if (cellIcon != null) Icon(cellIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

// ── Persistent gap banner ─────────────────────────────────────────────────

@Composable
private fun PersistentGapBanner(repeatedWeak: List<CriterionKey>) {
    Surface(
        color = ORANGE_BG,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ORANGE.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = ORANGE, modifier = Modifier.size(14.dp))
                Text(
                    "Persistent gaps — failed in last 2 sessions",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ORANGE
                )
            }
            repeatedWeak.forEach { key ->
                Text(
                    "  · ${key.description}",
                    fontSize = 11.sp, color = ORANGE.copy(alpha = 0.8f), lineHeight = 16.sp
                )
            }
        }
    }
}

// ── Recommendations section ───────────────────────────────────────────────

@Composable
private fun RecommendationsSection(recommendations: List<PracticeRecommendation>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "WHAT TO FOCUS ON NEXT",
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = LABEL, letterSpacing = 1.2.sp
        )
        recommendations.take(3).forEach { rec ->
            Surface(
                color = CARD_BG,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, TEAL.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = TEAL, modifier = Modifier.size(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            rec.title, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = WHITE, lineHeight = 18.sp
                        )
                        Text(rec.rationale, fontSize = 11.sp, color = SEC, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────

@Composable
private fun EmptyStateCard(onLoadDemo: () -> Unit) {
    Surface(
        color = CARD_BG,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Rounded.BarChart, contentDescription = null, tint = TEAL, modifier = Modifier.size(48.dp))
            Text(
                "No training data yet",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WHITE
            )
            Text(
                "Complete a module to see your analytics,\nor load demo data to explore the feature.",
                fontSize = 13.sp, color = SEC, lineHeight = 20.sp
            )
            Button(
                onClick = onLoadDemo,
                colors = ButtonDefaults.buttonColors(containerColor = TEAL),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Load Demo Data (Aarav Kumar)",
                    color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
            }
        }
    }
}

// ── Data controls ─────────────────────────────────────────────────────────

@Composable
private fun DataControls(
    isDemoData: Boolean,
    hasSessions: Boolean,
    onLoadDemo: () -> Unit,
    onClearData: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(color = L_SEP)
        Text(
            "Data",
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = LABEL, letterSpacing = 0.8.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isDemoData) {
                OutlinedButton(
                    onClick = onLoadDemo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TEAL)
                ) {
                    Text("Load Demo", fontSize = 13.sp)
                }
            }
            if (hasSessions) {
                OutlinedButton(
                    onClick = onClearData,
                    modifier = if (!isDemoData) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SEC)
                ) {
                    Text(
                        if (isDemoData) "Clear Demo Data" else "Clear All Data",
                        fontSize = 13.sp
                    )
                }
            }
        }
        Text(
            "Training data is stored on this device only. It does not constitute medical certification.",
            fontSize = 10.sp, color = L_TER, lineHeight = 15.sp
        )
    }
}

// ── Module (Stroke / Allergy) skill-level breakdown ──────────────────────────

@Composable
private fun ModuleSkillBreakdownSection(
    title: String,
    stats: List<com.sanjeevani.training.ModuleSkillStats>,
    onOpenFocus: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = LABEL, letterSpacing = 1.2.sp
            )
            if (onOpenFocus != null) {
                TextButton(onClick = onOpenFocus, contentPadding = PaddingValues(0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Details", fontSize = 11.sp, color = TEAL)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TEAL, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        stats.take(5).forEach { s ->
            val pct = (s.accuracy * 100).toInt()
            val accent = when {
                pct >= 80 -> GREEN
                pct >= 50 -> ORANGE
                else      -> L_RED
            }
            Surface(
                color = CARD_BG, shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.skillDescription, fontSize = 12.sp, color = WHITE,
                            fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("${s.totalAttempts} attempt(s)", fontSize = 10.sp, color = LABEL)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.12f)) {
                        Text(
                            "$pct%",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── CPR skill-level breakdown ─────────────────────────────────────────────

@Composable
private fun CprSkillBreakdownSection(
    stats: List<com.sanjeevani.training.CprSkillStats>,
    onOpenFocus: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "CPR SKILL BREAKDOWN",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = LABEL, letterSpacing = 1.2.sp
            )
            if (onOpenFocus != null) {
                TextButton(onClick = onOpenFocus, contentPadding = PaddingValues(0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Details", fontSize = 11.sp, color = TEAL)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TEAL, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        stats.take(5).forEach { s ->
            val pct = (s.accuracy * 100).toInt()
            val accent = when {
                pct >= 80 -> GREEN
                pct >= 50 -> ORANGE
                else      -> L_RED
            }
            Surface(
                color = CARD_BG, shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.skillDescription, fontSize = 12.sp, color = WHITE,
                            fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("${s.totalAttempts} attempt(s)", fontSize = 10.sp, color = LABEL)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.12f)) {
                        Text(
                            "$pct%",
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
