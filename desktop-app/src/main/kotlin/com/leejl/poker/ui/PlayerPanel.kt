package com.leejl.poker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leejl.poker.model.Player

private val activeBg = Color(0xFF1B5E20).copy(alpha = 0.7f)
private val inactiveBg = Color(0xFF333333).copy(alpha = 0.5f)
private val foldBg = Color(0xFF555555).copy(alpha = 0.4f)
private val highlightBorder = Color(0xFFFFD700)

@Composable
fun PlayerPanel(
    player: Player,
    isCurrentPlayer: Boolean = false,
    isHuman: Boolean = false,
    isWinner: Boolean = false,
    showCards: Boolean = false,
    handRank: String? = null,
    modifier: Modifier = Modifier
) {
    val bg = if (player.folded) foldBg
    else if (isWinner) Color(0xFF1B5E20).copy(alpha = 0.9f)
    else if (isCurrentPlayer) activeBg.copy(alpha = 0.8f)
    else activeBg

    val border = if (isCurrentPlayer) highlightBorder else Color.Transparent
    val bw = if (isCurrentPlayer) 2.dp else 1.dp

    Box(
        modifier = modifier
            .widthIn(min = 100.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .border(bw, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Name + chip indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (player.isDealer) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) { Text("D", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(4.dp))
                }
                if (player.isSmallBlind) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4FC3F7)),
                        contentAlignment = Alignment.Center
                    ) { Text("SB", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(4.dp))
                }
                if (player.isBigBlind) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF7043)),
                        contentAlignment = Alignment.Center
                    ) { Text("BB", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    player.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isHuman) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                "$${player.chips}",
                color = Color(0xFF76FF03),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Current bet
            if (player.currentBet > 0) {
                Text(
                    "Bet: $${player.currentBet}",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp
                )
            }

            // Folded/all-in status
            if (player.folded) {
                Text("FOLDED", color = Color(0xFF9E9E9E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (player.isAllIn) {
                Text("ALL IN", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Hole cards (face up only if hand is over or AI)
            if (player.holeCards.isNotEmpty() && !player.folded) {
                Spacer(Modifier.height(2.dp))
                CardFan(
                    cards = player.holeCards,
                    faceUp = showCards || (!isHuman && player.holeCards.isNotEmpty()),
                    small = true
                )
            }

            // Hand rank at showdown
            handRank?.let {
                if (it.isNotEmpty()) {
                    Text(it, color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}
