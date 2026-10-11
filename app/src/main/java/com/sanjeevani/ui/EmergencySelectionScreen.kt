package com.sanjeevani.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanjeevani.model.EmergencyType

private data class EmergencyCard(
    val type: EmergencyType,
    val emoji: String,
    val name: String,
    val description: String,
    val color: Color
)

private val EMERGENCY_CARDS = listOf(
    EmergencyCard(EmergencyType.CPR, "❤️", "Collapsed", "Not breathing or unresponsive", Color(0xFFff6b6b)),
    EmergencyCard(EmergencyType.FAST_STROKE, "🧠", "Stroke", "Face droop, arm weakness, slurred speech", Color(0xFFa78bfa)),
    EmergencyCard(EmergencyType.HEART_ATTACK, "🫀", "Heart Attack", "Chest pain, still conscious", Color(0xFFf97316)),
    EmergencyCard(EmergencyType.ALLERGIC_REACTION, "💉", "Allergic Reaction", "Severe allergy / EpiPen needed", Color(0xFF4ecca8))
)

@Composable
fun EmergencySelectionScreen(
    classifierSuggestion: EmergencyType?,
    classifierConfidence: Float,
    onEmergencySelected: (EmergencyType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xD9000000)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 112.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "WHAT IS HAPPENING?",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            if (classifierSuggestion != null && classifierConfidence > 0.4f) {
                val suggestionCard = EMERGENCY_CARDS.find { it.type == classifierSuggestion }
                if (suggestionCard != null) {
                    Surface(
                        color = Color(0x33FFFFFF),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Camera detects: ${suggestionCard.emoji} ${suggestionCard.name}",
                                color = suggestionCard.color,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = classifierConfidence,
                                modifier = Modifier.fillMaxWidth(),
                                color = suggestionCard.color,
                                trackColor = Color(0x33FFFFFF)
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(EMERGENCY_CARDS) { card ->
                    val isHighlighted = card.type == classifierSuggestion && classifierConfidence > 0.4f
                    EmergencyCardItem(
                        card = card,
                        isHighlighted = isHighlighted,
                        onClick = { onEmergencySelected(card.type) }
                    )
                }
            }

            Text(
                text = "Report your concern • this app cannot diagnose. Adult guidance prototype; follow 112.",
                color = Color(0xAAFFFFFF),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmergencyCardItem(
    card: EmergencyCard,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isHighlighted) card.color.copy(alpha = 0.25f) else Color(0x22FFFFFF)
        ),
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) card.color else Color(0x55FFFFFF)
        ),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = card.emoji, fontSize = 36.sp)
            Text(
                text = card.name,
                color = card.color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = card.description,
                color = Color(0xCCFFFFFF),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
