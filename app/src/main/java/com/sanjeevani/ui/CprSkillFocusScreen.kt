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

private val BG      = Color(0xFF080E1C)
private val CARD_BG = Color(0xFF121929)
private val TEAL    = Color(0xFF00C4B4)
private val ORANGE  = Color(0xFFFF9B2E)
private val RED     = Color(0xFFE53935)
private val LABEL   = Color(0xFF5A6580)
private val WHITE   = Color.White

@Composable
fun CprSkillFocusScreen(
    onStartDrill: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
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
                Text("← Back", fontSize = 14.sp, color = TEAL)
            }
            Text("Focused Practice", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WHITE)
            Spacer(Modifier.width(60.dp))
        }

        // Session insight card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1A0A00),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, ORANGE.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠", fontSize = 18.sp, color = ORANGE)
                    Text("From your last session", fontSize = 12.sp,
                        color = ORANGE, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp)
                }
                Text(
                    "Hand Placement on Chest — 8 drift errors detected",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WHITE
                )
                Text(
                    "Your hands drifted left of centre on 8 occasions during chest compressions. " +
                    "Consistent midline hand placement is essential to deliver effective compressions " +
                    "and avoid rib injury.",
                    fontSize = 12.sp, color = Color(0xFFB0BEC5), lineHeight = 18.sp
                )
            }
        }

        // What this drill covers
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CARD_BG,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("What this drill covers",
                    fontSize = 12.sp, color = LABEL, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp)
                DrillPoint("🎯", "Heel of hand on the lower half of the sternum, midline")
                DrillPoint("👐", "Fingers interlocked, raised off the chest")
                DrillPoint("📱", "Live camera overlay shows your hand position relative to the target")
                DrillPoint("🔔", "Real-time audio cues when placement drifts")
            }
        }

        // Target anatomy note
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0A1420),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E3A5F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CORRECT POSITION", fontSize = 10.sp, color = TEAL,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "Place the heel of your dominant hand on the centre of the chest — " +
                    "on the lower half of the breastbone. The app will show a target circle " +
                    "on screen. Align your hand to it.",
                    fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 17.sp
                )
            }
        }

        // Start button
        Button(
            onClick = onStartDrill,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TEAL)
        ) {
            Text("Start Hand Placement Drill", fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = Color(0xFF080E1C))
        }

        Text(
            "This drill starts at the hand positioning step only. It does not replace a full CPR " +
            "training session. Training records do not indicate professional certification.",
            fontSize = 10.sp, color = LABEL.copy(alpha = 0.7f), lineHeight = 15.sp
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DrillPoint(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 14.sp)
        Text(text, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 18.sp,
            modifier = Modifier.weight(1f))
    }
}
