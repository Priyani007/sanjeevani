package com.sanjeevani.ui

import androidx.compose.foundation.background
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
fun ModuleResultScreen(
    outcome: SessionOutcome,
    comparison: OutcomeComparison,
    recommendations: List<PracticeRecommendation>,
    onPracticeAgain: () -> Unit,
    onReturnHome: () -> Unit
) {
    val bg = Color(0xFF080E1C)
    val cardBg = Color(0xFF121929)
    val teal = Color(0xFF00C4B4)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF7A8899)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${outcome.moduleType.displayName} Session Complete",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = "${outcome.durationSecs}s · ${outcome.passCount}/${outcome.totalCriteria} criteria met",
                fontSize = 13.sp,
                color = textSecondary
            )
        }

        // Improvement banner (only when there's a previous session to compare)
        if (comparison.previous != null) {
            val delta = comparison.passCountDelta
            val bannerColor = when {
                delta > 0 -> Color(0xFF1A3A2A)
                delta < 0 -> Color(0xFF3A1A1A)
                else -> Color(0xFF1A2A3A)
            }
            val bannerText = when {
                delta > 0 -> "+$delta criteria vs last session"
                delta < 0 -> "$delta criteria vs last session"
                else -> "Same result as last session"
            }
            Surface(
                color = bannerColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bannerText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (delta >= 0) Color(0xFF4EE0A0) else Color(0xFFFF6B6B),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Criteria list
        Text("Assessment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5A6580), letterSpacing = 1.2.sp)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                outcome.criteria.forEachIndexed { i, criterion ->
                    CriterionRow(criterion)
                    if (i < outcome.criteria.lastIndex) {
                        HorizontalDivider(color = Color(0xFF1E2840), thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }

        // Recommendations
        if (recommendations.isNotEmpty()) {
            Text("What to focus on next", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5A6580), letterSpacing = 1.2.sp)
            recommendations.forEach { rec ->
                RecommendationCard(rec, cardBg, teal)
            }
        }

        // Safety notice
        Text(
            text = "Training simulations do not replace certified first-aid courses. Always call 112 in a real emergency.",
            fontSize = 11.sp, lineHeight = 16.sp, color = Color(0xFF3A4560),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onReturnHome,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Home", fontSize = 14.sp)
            }
            Button(
                onClick = onPracticeAgain,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = teal),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Practice Again", fontSize = 14.sp, color = bg, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CriterionRow(criterion: CriterionRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = criterion.key.description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                lineHeight = 18.sp
            )
            if (criterion.evidence.isNotBlank()) {
                Text(
                    text = criterion.evidence.take(100),
                    fontSize = 11.sp,
                    color = Color(0xFF5A6580),
                    lineHeight = 15.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val (icon, color) = when (criterion.result) {
            CriterionResult.PASS -> "✓" to Color(0xFF4EE0A0)
            CriterionResult.NEEDS_PRACTICE -> "!" to Color(0xFFFF9B2E)
            CriterionResult.INCONCLUSIVE -> "?" to Color(0xFF7A8899)
            CriterionResult.NOT_ASSESSED -> "—" to Color(0xFF3A4560)
        }
        Text(text = icon, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun RecommendationCard(
    rec: PracticeRecommendation,
    cardBg: Color,
    teal: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("→", fontSize = 14.sp, color = teal, fontWeight = FontWeight.Bold)
                Text(rec.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            }
            Text(rec.rationale, fontSize = 12.sp, color = Color(0xFF7A8899), lineHeight = 17.sp)
        }
    }
}
