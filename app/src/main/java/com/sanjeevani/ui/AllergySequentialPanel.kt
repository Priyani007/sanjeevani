package com.sanjeevani.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SEQ_BG    = Color(0xEE080E1C)
private val SEQ_TEAL  = Color(0xFF00C4B4)
private val SEQ_AMBER = Color(0xFFFBBF24)
private val SEQ_GREEN = Color(0xFF4ADE80)
private val SEQ_RED   = Color(0xFFE53935)
private val SEQ_LABEL = Color(0xFF5A6580)
private val SEQ_WHITE = Color.White
private val SEQ_TEXT  = Color(0xFFCBD5E1)
private val SEQ_BORDER = Color(0xFF1E3A5F)

private val STEP_TITLES = listOf(
    "Step 1 of 6 — Patient Position",
    "Step 2 of 6 — Leg Elevation",
    "Step 3 of 6 — Consciousness Check",
    "Step 4 of 6 — Injection Site",
    "Step 5 of 6 — EpiPen Positioning",
    "Step 6 of 6 — Simulated Injection",
    "Training Complete"
)

private val STEP_INSTRUCTIONS = listOf(
    "Verify the simulated patient is lying flat on a firm surface. The camera overlay confirms the position estimate.",
    "Raise the patient's legs above heart level. An arrow appears when legs appear too low — guide them upward.",
    "Assess consciousness before proceeding.",
    "Locate the outer mid-thigh injection zone. The pulsing target marker shows the correct area.",
    "Hold your wrist over the target zone. Arrows guide you to the correct position. Hold steady to confirm.",
    "Hold the trainer EpiPen firmly at 90° against the outer thigh for 10 seconds.",
    ""
)

/**
 * Step-by-step overlay panel for the sequential allergy response drill.
 *
 * Steps 0, 1, 3, 4 → camera panel at bottom
 * Step 2           → consciousness question card (centred, full-width)
 * Step 5           → injection countdown overlay (centred)
 * Step 6           → summary card (centred, scrollable)
 */
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
                .padding(horizontal = 20.dp),
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
                .padding(horizontal = 20.dp),
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
        step == 4 -> if (statusGreen) "Simulate Injection" else "Hold on target…"
        canProceed -> "Continue"
        else -> "Waiting for camera…"
    }

    Surface(
        color = SEQ_BG,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                STEP_TITLES.getOrElse(step) { "" },
                color = SEQ_TEAL,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                STEP_INSTRUCTIONS.getOrElse(step) { "" },
                color = SEQ_TEXT,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            if (statusText.isNotEmpty()) {
                Surface(
                    color = if (statusGreen) Color(0xFF0A1F0F) else Color(0xFF1A1000),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (statusGreen) SEQ_GREEN.copy(alpha = 0.5f) else SEQ_AMBER.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        statusText,
                        color = if (statusGreen) SEQ_GREEN else SEQ_AMBER,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        lineHeight = 17.sp
                    )
                }
            }
            Button(
                onClick = onAction,
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SEQ_TEAL,
                    disabledContainerColor = Color(0xFF1A3A38)
                )
            ) {
                Text(
                    buttonLabel,
                    color = if (canProceed) Color(0xFF080E1C) else SEQ_LABEL,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                "Simulation only. Follow 1 1 2 in a real emergency.",
                color = SEQ_LABEL,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
            if (onSkip != null && !canProceed) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    Text(
                        "Continue →",
                        color = SEQ_LABEL,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsciousnessCard(
    modifier: Modifier = Modifier,
    onAnswer: (Boolean) -> Unit
) {
    Surface(
        color = Color(0xF2080E1C),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SEQ_BORDER),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Step 3 of 6",
                color = SEQ_TEAL,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Consciousness Check",
                color = SEQ_WHITE,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Tap the patient's shoulder and call their name loudly.\n\nIs the patient responding?",
                color = SEQ_TEXT,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Surface(
                color = Color(0xFF0A1420),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, SEQ_BORDER),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "In a real emergency: if unresponsive and not breathing normally, call 1 1 2 immediately and begin CPR. Do not delay emergency assistance.",
                    color = SEQ_LABEL,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A0A0A))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unresponsive", color = SEQ_RED, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A2A1A))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Responding", color = SEQ_GREEN, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
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
    val countColor by animateColorAsState(
        targetValue = when {
            countdown > 5 -> SEQ_TEAL
            countdown > 2 -> SEQ_AMBER
            else -> SEQ_GREEN
        },
        label = "countdown_color"
    )
    val fraction = countdown / 10f

    Column(
        modifier = modifier
            .background(Color(0xCC080E1C), RoundedCornerShape(20.dp))
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Step 6 of 6",
            color = SEQ_TEAL,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Simulated Injection",
            color = SEQ_WHITE,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val strokeWidth = 10.dp.toPx()
                drawArc(
                    color = Color(0xFF1E3A5F),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = countColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                countdown.toString(),
                color = countColor,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "Hold trainer EpiPen firmly at 90° to the outer thigh",
            color = SEQ_TEXT,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Text(
            "Trainer simulation only — never use a live injector for practice.",
            color = SEQ_LABEL,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
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
        color = Color(0xF5080E1C),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SEQ_BORDER),
        modifier = modifier
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("✓", color = SEQ_GREEN, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Column {
                    Text(
                        "Drill Complete",
                        color = SEQ_WHITE,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("All 6 steps completed", color = SEQ_LABEL, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = SEQ_BORDER, thickness = 0.5.dp)

            Text(
                "POSITIONING SUMMARY",
                color = SEQ_LABEL,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (totalCorrections == 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓", color = SEQ_GREEN, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "No positioning corrections needed",
                        color = SEQ_TEXT,
                        fontSize = 13.sp
                    )
                }
            } else {
                Surface(
                    color = Color(0xFF1A1000),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, SEQ_AMBER.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "$totalCorrections directional correction${if (totalCorrections != 1) "s" else ""} during EpiPen positioning",
                            color = SEQ_AMBER,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        mistakesByDirection.forEach { (dir, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• $dir", color = SEQ_TEXT, fontSize = 12.sp)
                                Text("${count}×", color = SEQ_LABEL, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = SEQ_BORDER, thickness = 0.5.dp)

            Text(
                "This drill covers simulated allergy response training steps only. It does not replace full clinical training. Performance in this drill does not indicate professional certification or clinical readiness. Follow emergency dispatcher instructions in a real emergency. Emergency: 1 1 2.",
                color = SEQ_LABEL,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SEQ_TEAL)
            ) {
                Text(
                    "Done",
                    color = Color(0xFF080E1C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
