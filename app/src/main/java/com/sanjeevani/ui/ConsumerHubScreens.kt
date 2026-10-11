package com.sanjeevani.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

// ── Practice Hub ─────────────────────────────────────────────────────────────

@Composable
fun PracticeHubScreen(
    cprRecommendationCount: Int,
    strokeRecommendationCount: Int,
    allergyRecommendationCount: Int,
    onPracticeCpr: () -> Unit,
    onPracticeStroke: () -> Unit,
    onPracticeAllergy: () -> Unit,
    onOpenQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(L_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Text("Focused Practice", color = L_PRI, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Repeat the original training skill — not a disconnected substitute. Recommendations come from your saved attempts.",
            color = L_SEC,
            lineHeight = 20.sp,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(4.dp))

        PracticeCard(
            icon = Icons.Rounded.Favorite,
            iconTint = L_RED,
            title = "CPR camera practice",
            count = cprRecommendationCount,
            detail = "Uses the live CPR camera and AR guidance.",
            onClick = onPracticeCpr
        )
        PracticeCard(
            icon = Icons.Rounded.Psychology,
            iconTint = L_ORANGE,
            title = "Stroke FAST practice",
            count = strokeRecommendationCount,
            detail = "Opens the matching camera-supported or decision step.",
            onClick = onPracticeStroke
        )
        PracticeCard(
            icon = Icons.Rounded.Vaccines,
            iconTint = L_ACCENT,
            title = "Allergy placement practice",
            count = allergyRecommendationCount,
            detail = "Uses the thigh reference plus clearly labelled trainer simulation.",
            onClick = onPracticeAllergy
        )

        OutlinedButton(
            onClick = onOpenQuiz,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = L_ACCENT),
            border = androidx.compose.foundation.BorderStroke(1.dp, L_ACCENT.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open knowledge quiz", fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PracticeCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    detail: String,
    onClick: () -> Unit
) {
    Surface(
        color = L_CARD,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = L_PRI, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (count > 0) "$count saved recommendation${if (count == 1) "" else "s"}" else "Foundation practice available",
                        color = L_ACCENT,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(detail, color = L_SEC, fontSize = 13.sp, lineHeight = 18.sp)
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = L_ACCENT),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Start focused practice", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ── Profile & Settings ───────────────────────────────────────────────────────

@Composable
fun ProfileSettingsScreen(
    isVoiceMuted: Boolean,
    onToggleVoice: () -> Unit,
    onOpenHistory: () -> Unit,
    onDeleteTrainingData: () -> Unit
) {
    val context = LocalContext.current
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(L_BG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Profile & Settings", color = L_PRI, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // Voice guidance
        Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(L_ACCENT.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isVoiceMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = L_ACCENT,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text("Voice guidance", color = L_PRI, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(if (isVoiceMuted) "Muted" else "On", color = L_SEC, fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = !isVoiceMuted,
                    onCheckedChange = { onToggleVoice() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = L_GREEN)
                )
            }
        }

        // Permissions card
        Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                PermissionRow(Icons.Rounded.Videocam, "Camera", cameraGranted)
                HorizontalDivider(color = L_SEP, thickness = 0.5.dp, modifier = Modifier.padding(start = 60.dp))
                PermissionRow(Icons.Rounded.Mic, "Microphone", micGranted)
            }
        }

        // History button
        Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(L_ACCENT.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = L_ACCENT, modifier = Modifier.size(18.dp))
                    }
                    Text("Training history", color = L_PRI, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                IconButton(onClick = onOpenHistory, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = L_SEC)
                }
            }
        }

        // Privacy card
        Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = L_SEC, modifier = Modifier.size(18.dp))
                    Text("Privacy", color = L_PRI, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Text(
                    "Training stores structured answers and measurements on this device. Raw camera frames and audio recordings are not saved.",
                    color = L_SEC,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                TextButton(
                    onClick = onDeleteTrainingData,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = L_RED, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete all training data", color = L_RED, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // About
        Surface(color = L_CARD, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = L_RED, modifier = Modifier.size(18.dp))
                    Text("Sanjeevani", color = L_PRI, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text(
                    "Not a diagnostic device or professional certification. In an emergency, call 1 1 2 and follow the dispatcher. Camera observations can be inconclusive.",
                    color = L_SEC,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, label: String, granted: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(L_ACCENT.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = L_ACCENT, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(label, color = L_PRI, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (granted) "Granted" else "Not granted", color = L_SEC, fontSize = 12.sp)
            }
        }
        Icon(
            if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = null,
            tint = if (granted) L_GREEN else L_RED,
            modifier = Modifier.size(20.dp)
        )
    }
}
