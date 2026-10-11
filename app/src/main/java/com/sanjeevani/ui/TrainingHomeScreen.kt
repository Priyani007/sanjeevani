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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.training.LearnerProfile
import com.sanjeevani.training.PracticeRecommendation

// ── Apple light palette (shared across all home screens) ─────────────────────
internal val L_BG      = Color(0xFFF2F2F7)
internal val L_CARD    = Color(0xFFFFFFFF)
internal val L_ACCENT  = Color(0xFF007AFF)
internal val L_GREEN   = Color(0xFF34C759)
internal val L_ORANGE  = Color(0xFFFF9500)
internal val L_RED     = Color(0xFFFF3B30)
internal val L_PRI     = Color(0xFF000000)
internal val L_SEC     = Color(0xFF3C3C43).copy(alpha = 0.6f)
internal val L_TER     = Color(0xFF3C3C43).copy(alpha = 0.3f)
internal val L_SEP     = Color(0xFFC6C6C8)
internal val L_FILL    = Color(0xFFE5E5EA)

private data class TrainingModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val available: Boolean
)

private val modules = listOf(
    TrainingModule("cpr",      "CPR",               "Cardiopulmonary Resuscitation",    Icons.Rounded.Favorite,       L_RED,    true),
    TrainingModule("allergy",  "Allergic Reaction", "Anaphylaxis & EpiPen Protocol",    Icons.Rounded.Vaccines,       L_ACCENT, true),
    TrainingModule("stroke",   "Stroke",            "FAST Recognition & Response",      Icons.Rounded.Psychology,     L_ORANGE, true),
    TrainingModule("aed",      "AED",               "Automated External Defibrillator", Icons.Rounded.ElectricBolt,   L_SEC,    false),
    TrainingModule("choking",  "Choking",           "First Aid for Choking",            Icons.Rounded.Air,            L_SEC,    false),
    TrainingModule("recovery", "Recovery Position", "Safe Lateral Position",            Icons.Rounded.AirlineSeatReclineNormal, L_SEC, false),
)

@Composable
fun TrainingHomeScreen(
    onStartCpr: () -> Unit,
    onStartAllergicReaction: () -> Unit,
    onStartStroke: () -> Unit,
    onStartCprTraining: () -> Unit = onStartCpr,
    onStartAllergyTraining: () -> Unit = onStartAllergicReaction,
    onStartStrokeTraining: () -> Unit = onStartStroke,
    learnerProfile: LearnerProfile = LearnerProfile(),
    recommendations: List<PracticeRecommendation> = emptyList(),
    onViewHistory: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(L_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        AppHeader(learnerProfile, onViewHistory, onOpenAnalytics)

        if (learnerProfile.totalSessions() == 0) {
            DemoCallToAction(onOpenAnalytics)
        }

        if (recommendations.isNotEmpty()) {
            SectionLabel("Recommended Next")
            RecommendationStrip(recommendations)
        }

        SectionLabel("Training Modules")
        ModuleGrid(onStartCprTraining, onStartAllergyTraining, onStartStrokeTraining)
        DisclaimerText()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AppHeader(
    profile: LearnerProfile,
    onViewHistory: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(L_RED.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = L_RED, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    "Sanjeevani",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = L_PRI,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    "AI-Powered Emergency Skills",
                    fontSize = 12.sp,
                    color = L_ACCENT,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (profile.totalSessions() > 0) {
                TextButton(
                    onClick = onViewHistory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${profile.totalSessions()} sessions", fontSize = 12.sp, color = L_SEC)
                }
            }
            TextButton(
                onClick = onOpenAnalytics,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Analytics", fontSize = 12.sp, color = L_ACCENT, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DemoCallToAction(onOpenAnalytics: () -> Unit) {
    Surface(
        color = L_CARD,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Insights, contentDescription = null, tint = L_ACCENT, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("See analytics in action", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = L_PRI)
                    Text("Load demo data to explore the learner profile", fontSize = 11.sp, color = L_SEC, lineHeight = 16.sp)
                }
            }
            TextButton(
                onClick = onOpenAnalytics,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Try Demo", fontSize = 12.sp, color = L_ACCENT, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = L_ACCENT, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = L_SEC,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ModuleGrid(
    onStartCpr: () -> Unit,
    onStartAllergicReaction: () -> Unit,
    onStartStroke: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modules.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { module ->
                    val action: (() -> Unit)? = when (module.id) {
                        "cpr"     -> onStartCpr
                        "allergy" -> onStartAllergicReaction
                        "stroke"  -> onStartStroke
                        else      -> null
                    }
                    TrainingCard(module = module, onStart = action, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrainingCard(
    module: TrainingModule,
    onStart: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = L_CARD,
        shadowElevation = if (module.available) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (module.available) module.iconTint.copy(alpha = 0.12f) else L_FILL,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    module.icon,
                    contentDescription = null,
                    tint = if (module.available) module.iconTint else L_TER,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    module.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (module.available) L_PRI else L_TER
                )
                Text(
                    module.subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = if (module.available) L_SEC else L_TER
                )
            }

            if (module.available && onStart != null) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = L_ACCENT),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Start", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(L_FILL, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Coming Soon", fontSize = 12.sp, color = L_TER, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun RecommendationStrip(recommendations: List<PracticeRecommendation>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recommendations.take(2).forEach { rec ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = L_CARD,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = L_ACCENT, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(rec.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = L_PRI, lineHeight = 17.sp)
                        Text(rec.rationale, fontSize = 11.sp, color = L_SEC, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerText() {
    Text(
        "Training simulations do not replace certified first-aid courses. Always call 1 1 2 in a real emergency.",
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = L_TER,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
