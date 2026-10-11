package com.sanjeevani.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Apple-inspired light palette ─────────────────────────────────────────────
private val BG          = Color(0xFFF2F2F7)   // iOS system grouped background
private val CARD        = Color(0xFFFFFFFF)
private val ACCENT      = Color(0xFF007AFF)   // iOS blue
private val GREEN       = Color(0xFF34C759)   // iOS green
private val ORANGE      = Color(0xFFFF9500)   // iOS orange
private val RED         = Color(0xFFFF3B30)   // iOS red
private val LABEL_PRI   = Color(0xFF000000)
private val LABEL_SEC   = Color(0xFF3C3C43).copy(alpha = 0.6f)
private val LABEL_TER   = Color(0xFF3C3C43).copy(alpha = 0.3f)
private val SEPARATOR   = Color(0xFFC6C6C8)
private val FILL_SEC    = Color(0xFFE5E5EA)

private val STEP_TITLES = listOf(
    "Patient Position",
    "Leg Elevation",
    "Consciousness Check",
    "Injection Site",
    "EpiPen Positioning",
    "Simulated Injection",
    "Drill Complete"
)

private val STEP_SUBTITLES = listOf(
    "Step 1 of 6",
    "Step 2 of 6",
    "Step 3 of 6",
    "Step 4 of 6",
    "Step 5 of 6",
    "Step 6 of 6",
    ""
)

private val STEP_INSTRUCTIONS = listOf(
    "Verify the simulated patient is lying flat on a firm surface. The camera overlay confirms position.",
    "Raise the patient's legs above heart level. An arrow guides you if the legs appear too low.",
    "Tap the patient's shoulder and call their name loudly.",
    "Locate the outer mid-thigh injection zone. The pulsing marker shows the correct area.",
    "Hold your wrist over the target zone. Arrows guide you to the correct position. Hold steady to confirm.",
    "Hold the trainer EpiPen firmly at 90° against the outer thigh for 10 seconds.",
    ""
)

private val STEP_ICONS: List<ImageVector> = listOf(
    Icons.Rounded.PersonPin,
    Icons.Rounded.ArrowUpward,
    Icons.Rounded.RecordVoiceOver,
    Icons.Rounded.GpsFixed,
    Icons.Rounded.TouchApp,
    Icons.Rounded.Timer,
    Icons.Rounded.CheckCircle
)

@Composable
fun BoxScope.AllergySequentialPanel(
    step: Int,
    statusText: String,
    statusGreen: Boolean,
    thighVisible: Boolean,
    injectionCountdown: Int,
    mistakesByDirection: Map<String, Int>,
    totalCorrections: Int,
    onContinue: () -> Unit,
    onConsciousnessAnswer: (Boolean) -> Unit,
    onStartInjection: () -> Unit,
    onFinish: () -> Unit,
) {
    when (step) {
        2 -> ConsciousnessCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onAnswer = onConsciousnessAnswer
        )
        5 -> InjectionCountdown(
            countdown = injectionCountdown,
            modifier = Modifier.align(Alignment.Center)
        )
        6 -> SummaryCard(
            mistakesByDirection = mistakesByDirection,
            totalCorrections = totalCorrections,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onFinish = onFinish
        )
        else -> CameraStepPanel(
            step = step,
            statusText = statusText,
            statusGreen = statusGreen,
            thighVisible = thighVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            onAction = if (step == 4) onStartInjection else onContinue,
            onSkip = if (step == 4) onStartInjection else onContinue
        )
    }
}

@Composable
private fun CameraStepPanel(
    step: Int,
    statusText: String,
    statusGreen: Boolean,
    thighVisible: Boolean,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val canProceed = when (step) {
        0 -> statusGreen
        1 -> statusGreen
        3 -> thighVisible
        4 -> statusGreen
        else -> true
    }
    val buttonLabel = when {
        step == 4 -> if (statusGreen) "Simulate Injection" else "Waiting for position…"
        canProceed -> "Continue"
        else -> "Waiting for camera…"
    }

    Surface(
        color = CARD,
        shadowElevation = 24.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (statusGreen) GREEN.copy(alpha = 0.12f) else ACCENT.copy(alpha = 0.10f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = STEP_ICONS.getOrElse(step) { Icons.Rounded.Circle },
                        contentDescription = null,
                        tint = if (statusGreen) GREEN else ACCENT,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        STEP_SUBTITLES.getOrElse(step) { "" },
                        color = LABEL_SEC,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        STEP_TITLES.getOrElse(step) { "" },
                        color = LABEL_PRI,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Instruction text
            Text(
                STEP_INSTRUCTIONS.getOrElse(step) { "" },
                color = LABEL_SEC,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Status pill
            if (statusText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (statusGreen) GREEN.copy(alpha = 0.08f) else ORANGE.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (statusGreen) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (statusGreen) GREEN else ORANGE,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        statusText,
                        color = if (statusGreen) GREEN else ORANGE,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Primary action button
            Button(
                onClick = onAction,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ACCENT,
                    disabledContainerColor = FILL_SEC
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    buttonLabel,
                    color = if (canProceed) Color.White else LABEL_TER,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // Skip/continue link for testing
            if (onSkip != null && !canProceed) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 0.dp)
                ) {
                    Text(
                        "Continue",
                        color = ACCENT,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                "Simulation only • Call 1 1 2 in a real emergency",
                color = LABEL_TER,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConsciousnessCard(
    modifier: Modifier = Modifier,
    onAnswer: (Boolean) -> Unit
) {
    Surface(
        color = CARD,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 16.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(ACCENT.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.RecordVoiceOver,
                    contentDescription = null,
                    tint = ACCENT,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Step 3 of 6",
                    color = LABEL_SEC,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Consciousness Check",
                    color = LABEL_PRI,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Tap the patient's shoulder and call their name loudly.\nIs the patient responding?",
                    color = LABEL_SEC,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = SEPARATOR, thickness = 0.5.dp)

            // Safety note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ORANGE.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = ORANGE,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                )
                Text(
                    "In a real emergency: if unresponsive and not breathing normally, call 1 1 2 immediately and begin CPR.",
                    color = LABEL_SEC,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            // YES / NO buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RED),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RED.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Rounded.PersonOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Unresponsive", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GREEN),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Responding", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InjectionCountdown(
    countdown: Int,
    modifier: Modifier = Modifier
) {
    val trackColor = FILL_SEC
    val arcColor by animateColorAsState(
        targetValue = when {
            countdown > 5 -> ACCENT
            countdown > 2 -> ORANGE
            else -> GREEN
        },
        animationSpec = tween(400),
        label = "arc_color"
    )
    val fraction by animateFloatAsState(
        targetValue = countdown / 10f,
        animationSpec = tween(900),
        label = "arc_fraction"
    )

    Surface(
        color = CARD,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 20.dp,
        modifier = modifier.padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Step 6 of 6",
                color = LABEL_SEC,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Simulated Injection",
                color = LABEL_PRI,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        countdown.toString(),
                        color = arcColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("sec", color = LABEL_SEC, fontSize = 12.sp)
                }
            }

            Text(
                "Hold trainer EpiPen firmly at 90° to the outer thigh",
                color = LABEL_SEC,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ORANGE.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = ORANGE, modifier = Modifier.size(14.dp))
                Text(
                    "Trainer simulation only — never use a live injector for practice.",
                    color = LABEL_SEC,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    mistakesByDirection: Map<String, Int>,
    totalCorrections: Int,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    Surface(
        color = CARD,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 16.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(GREEN.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = GREEN,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        "Drill Complete",
                        color = LABEL_PRI,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "All 6 steps completed",
                        color = LABEL_SEC,
                        fontSize = 13.sp
                    )
                }
            }

            HorizontalDivider(color = SEPARATOR, thickness = 0.5.dp)

            // Positioning summary section
            Text(
                "POSITIONING SUMMARY",
                color = LABEL_SEC,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )

            if (totalCorrections == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GREEN.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = GREEN, modifier = Modifier.size(18.dp))
                    Text(
                        "No positioning corrections needed",
                        color = GREEN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ORANGE.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = ORANGE, modifier = Modifier.size(18.dp))
                        Text(
                            "$totalCorrections directional correction${if (totalCorrections != 1) "s" else ""} during EpiPen positioning",
                            color = ORANGE,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    mistakesByDirection.forEach { (dir, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .background(LABEL_SEC, CircleShape)
                                )
                                Text(dir, color = LABEL_PRI, fontSize = 13.sp)
                            }
                            Text("${count}×", color = LABEL_SEC, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            HorizontalDivider(color = SEPARATOR, thickness = 0.5.dp)

            Text(
                "This drill covers simulated allergy response training steps only. It does not replace full clinical training. Performance in this drill does not indicate professional certification or clinical readiness. Emergency: 1 1 2.",
                color = LABEL_TER,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Done",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
