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

private val SF_BG       = Color(0xFFF2F2F7)
private val SF_CARD     = Color(0xFFFFFFFF)
private val SF_ACCENT   = Color(0xFF007AFF)
private val SF_ORANGE   = Color(0xFFFF9500)
private val SF_LABEL    = Color(0xFF000000)
private val SF_SEC      = Color(0xFF3C3C43).copy(alpha = 0.6f)
private val SF_TER      = Color(0xFF3C3C43).copy(alpha = 0.3f)
private val SF_SEP      = Color(0xFFC6C6C8)

@Composable
fun AllergySkillFocusScreen(
    onStartDrill: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SF_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // Nav bar
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", tint = SF_ACCENT, modifier = Modifier.size(28.dp))
            }
            Text(
                "Focused Practice",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = SF_LABEL,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.width(36.dp))
        }

        // Session insight card
        Surface(
            color = SF_CARD,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SF_ORANGE.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = SF_ORANGE, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("From your last session", fontSize = 12.sp, color = SF_ORANGE, fontWeight = FontWeight.Medium)
                    Text(
                        "EpiPen Injection Site — thigh placement needs practice",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SF_LABEL
                    )
                    Text(
                        "The EpiPen must be injected into the outer mid-thigh for fastest absorption. Incorrect placement can delay epinephrine onset during a severe allergic reaction.",
                        fontSize = 13.sp,
                        color = SF_SEC,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // What this drill covers
        Surface(
            color = SF_CARD,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    "WHAT THIS DRILL COVERS",
                    fontSize = 11.sp,
                    color = SF_SEC,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                DrillStep(Icons.Rounded.PersonPin,    SF_ACCENT, "Verify simulated patient is lying flat on a firm surface")
                StepDivider()
                DrillStep(Icons.Rounded.ArrowUpward,  SF_ACCENT, "Raise legs above heart level — arrow guides you if too low")
                StepDivider()
                DrillStep(Icons.Rounded.RecordVoiceOver, SF_ACCENT, "Consciousness check — tap YES or NO to continue")
                StepDivider()
                DrillStep(Icons.Rounded.GpsFixed,     SF_ACCENT, "Camera overlay highlights the outer mid-thigh target zone")
                StepDivider()
                DrillStep(Icons.Rounded.TouchApp,     SF_ACCENT, "Wrist tracking with directional corrections to the injection site")
                StepDivider()
                DrillStep(Icons.Rounded.Timer,        SF_ACCENT, "10-second simulated injection hold with countdown timer")
            }
        }

        // Anatomy note
        Surface(
            color = SF_CARD,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SF_ACCENT.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GpsFixed, contentDescription = null, tint = SF_ACCENT, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CORRECT SITE", fontSize = 11.sp, color = SF_ACCENT, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    Text(
                        "Outer mid-thigh — either thigh, through clothing if necessary. The camera will show a pulsing target zone. Position your wrist there before simulating injection.",
                        fontSize = 13.sp,
                        color = SF_SEC,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Start button
        Button(
            onClick = onStartDrill,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SF_ACCENT),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Start Full Allergy Response Drill",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Text(
            "This drill covers simulated allergy response training steps only. It does not replace full clinical training or professional guidance. Training records do not indicate professional certification.",
            fontSize = 11.sp,
            color = SF_TER,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DrillStep(icon: ImageVector, iconTint: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF1C1C1E), lineHeight = 19.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StepDivider() {
    HorizontalDivider(
        color = SF_SEP,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 36.dp)
    )
}
