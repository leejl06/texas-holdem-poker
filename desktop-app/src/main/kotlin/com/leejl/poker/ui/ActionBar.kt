package com.leejl.poker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leejl.poker.model.PlayerAction

@Composable
fun ActionBar(
    actions: List<PlayerAction>,
    playerChips: Int,
    currentBet: Int,
    onAction: (PlayerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasRaise = actions.any { it is PlayerAction.Raise }
    val minRaise = if (hasRaise) {
        actions.filterIsInstance<PlayerAction.Raise>().firstOrNull()?.amount ?: 20
    } else 0
    val toCall = actions.any { it is PlayerAction.Call }
    val maxRaise = playerChips - (if (toCall) currentBet else 0)

    var raiseAmount by remember { mutableStateOf(minRaise.coerceAtLeast(20)) }
    var showSlider by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (action in actions) {
                    Box(Modifier.weight(1f)) {
                        PokerActionButton(action, onClick = { onAction(action) })
                    }
                }
            }

            if (showSlider && hasRaise) {
                Spacer(Modifier.height(8.dp))
                val clamped = raiseAmount.coerceIn(minRaise, maxRaise.coerceAtLeast(minRaise))
                Text("Raise: $$clamped", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = clamped.toFloat(),
                    onValueChange = { raiseAmount = it.toInt().coerceIn(minRaise, maxRaise.coerceAtLeast(minRaise)) },
                    valueRange = minRaise.toFloat()..maxRaise.toFloat().coerceAtLeast(minRaise.toFloat()),
                    modifier = Modifier.fillMaxWidth(0.5f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF7043),
                        activeTrackColor = Color(0xFFFF7043),
                        inactiveTrackColor = Color(0xFF666666)
                    )
                )
                Box(Modifier.fillMaxWidth(0.5f)) {
                    PokerActionButton(
                        PlayerAction.Raise(
                            actions.filterIsInstance<PlayerAction.Raise>().first().playerName,
                            clamped
                        ),
                        onClick = {
                            onAction(PlayerAction.Raise(
                                actions.filterIsInstance<PlayerAction.Raise>().first().playerName,
                                clamped
                            ))
                            showSlider = false
                        }
                    )
                }
            } else if (hasRaise) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF333333), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF666666), RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showSlider = true }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Custom Raise", color = Color(0xFFFF7043), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PokerActionButton(action: PlayerAction, onClick: () -> Unit) {
    val (bg, displayLabel) = when (action) {
        is PlayerAction.Fold -> Color(0xFFC62828) to "Fold"
        is PlayerAction.Check -> Color(0xFF2E7D32) to "Check"
        is PlayerAction.Call -> Color(0xFF2E7D32) to "Call"
        is PlayerAction.Raise -> Color(0xFFEF6C00) to "Raise $${action.amount}"
        is PlayerAction.AllIn -> Color(0xFFD32F2F) to "All-In $${action.amount}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.dp, bg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(displayLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
