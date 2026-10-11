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

private val ALLERGY_BG    = Color(0xFF080E1C)
private val ALLERGY_CARD  = Color(0xFF121929)
private val ALLERGY_TEAL  = Color(0xFF00C4B4)
private val ALLERGY_AMBER = Color(0xFFFBBF24)
private val ALLERGY_LABEL = Color(0xFF5A6580)
private val ALLERGY_WHITE = Color.White

@Composable
fun AllergySkillFocusScreen(
    onStartDrill: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ALLERGY_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Back", fontSize = 14.sp, color = ALLERGY_TEAL)
            }
            Text("Focused Practice", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ALLERGY_WHITE)
            Spacer(Modifier.width(60.dp))
        }

        // Session insight card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1A1000),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, ALLERGY_AMBER.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠", fontSize = 18.sp, color = ALLERGY_AMBER)
                    Text("From your last session", fontSize = 12.sp,
                        color = ALLERGY_AMBER, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp)
                }
                Text(
                    "EpiPen Injection Site — thigh placement needs practice",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ALLERGY_WHITE
                )
                Text(
                    "The EpiPen must be injected into the outer mid-thigh for fastest absorption. " +
                    "Incorrect placement can delay the onset of epinephrine and reduce effectiveness " +
                    "during a severe allergic reaction.",
                    fontSize = 12.sp, color = Color(0xFFB0BEC5), lineHeight = 18.sp
                )
            }
        }

        // What this drill covers
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ALLERGY_CARD,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What this drill covers (6 steps)",
                    fontSize = 12.sp, color = ALLERGY_LABEL, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp)
                AllergyDrillPoint("🛌", "Verify simulated patient is lying flat on a firm surface")
                AllergyDrillPoint("⬆️", "Raise legs above heart level — arrow guides you if too low")
                AllergyDrillPoint("🧠", "Consciousness check — tap YES or NO to continue")
                AllergyDrillPoint("🎯", "Camera overlay highlights the outer mid-thigh target zone")
                AllergyDrillPoint("💉", "Wrist tracking with directional corrections to the injection site")
                AllergyDrillPoint("⏱", "10-second simulated injection hold with countdown timer")
            }
        }

        // Anatomy note
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0A1420),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E3A5F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CORRECT SITE", fontSize = 10.sp, color = ALLERGY_TEAL,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "Outer mid-thigh — either thigh, through clothing if necessary. " +
                    "The app camera will show a pulsing target zone. Position your wrist there before simulating injection.",
                    fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 17.sp
                )
            }
        }

        // Start button
        Button(
            onClick = onStartDrill,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ALLERGY_TEAL)
        ) {
            Text("Start Full Allergy Response Drill", fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = Color(0xFF080E1C))
        }

        Text(
            "This drill covers simulated allergy response training steps only. It does not replace full " +
            "clinical training or professional guidance. Training records do not indicate professional certification.",
            fontSize = 10.sp, color = ALLERGY_LABEL.copy(alpha = 0.7f), lineHeight = 15.sp
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AllergyDrillPoint(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 14.sp)
        Text(text, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 18.sp,
            modifier = Modifier.weight(1f))
    }
}
