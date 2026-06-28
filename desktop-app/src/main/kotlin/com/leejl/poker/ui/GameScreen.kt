package com.leejl.poker.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leejl.poker.engine.GameState
import com.leejl.poker.model.GameViewModel
import com.leejl.poker.model.PlayerAction

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state
    val isPlayerTurn = viewModel.isPlayerTurn
    val currentPlayer = state.currentPlayer

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopBar(
                handNumber = state.handHistory.size,
                activeCount = viewModel.activePlayerCount,
                onNewGame = { viewModel.newHand() },
                modifier = Modifier.fillMaxWidth()
            )

            // Table area
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                PokerTable(
                    state = state,
                    humanName = viewModel.humanName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom: Action bar or hand-over
            if (viewModel.isHandOver) {
                HandOverBar(
                    state.outcome?.let { o ->
                        o.winnerNames.joinToString(", ") + " won!"
                    } ?: "Hand over",
                    onNewHand = { viewModel.newHand() },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isPlayerTurn) {
                val actions = viewModel.availableActions()
                val player = state.players.find { it.name == viewModel.humanName }
                ActionBar(
                    actions = actions,
                    playerChips = player?.chips ?: 0,
                    currentBet = state.currentBet,
                    onAction = { viewModel.act(it) },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
            } else if (!viewModel.isHandOver) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI players thinking...", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun TopBar(handNumber: Int, activeCount: Int, onNewGame: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(Color(0xFF1A1A1A)).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Texas Hold'em", color = Color(0xFF76FF03), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Players: $activeCount", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Box(
                modifier = Modifier.background(Color(0xFF2E7D32), RoundedCornerShape(6.dp))
                    .clickable(onClick = onNewGame).padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("New Hand", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HandOverBar(message: String, onNewHand: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(Color(0xFF1A1A1A)).padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier.background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                .clickable(onClick = onNewHand).padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Next Hand", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Green felt poker table with 6 player positions distributed around it. */
@Composable
private fun PokerTable(state: GameState, humanName: String, modifier: Modifier = Modifier) {
    val tableColor = Color(0xFF1B6B2E)
    val tableAccent = Color(0xFF0D4F1A)

    // 6 player positions as (horizontal_frac, vertical_frac) of the container
    val playerSlots = listOf(
        0.50f to 0.92f,  // 0 — Bottom center (human)
        0.85f to 0.68f,  // 1 — Bottom right
        0.85f to 0.18f,  // 2 — Top right
        0.50f to 0.03f,  // 3 — Top center
        0.15f to 0.18f,  // 4 — Top left
        0.15f to 0.68f   // 5 — Bottom left
    )

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        val panelW = 140.dp
        val panelH = 85.dp

        // Green felt background
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            val cw = size.width
            val ch = size.height
            val r = CornerRadius(ch * 0.28f, ch * 0.28f)
            drawRoundRect(tableColor, Offset.Zero, Size(cw, ch), r)
            drawRoundRect(tableAccent, Offset(cw * 0.03f, ch * 0.04f), Size(cw * 0.94f, ch * 0.92f), r)
        }

        // Player panels
        val outcome = state.outcome
        val winners = outcome?.winnerNames?.toSet() ?: emptySet()
        val handNames = outcome?.handNames ?: emptyMap()

        state.players.forEachIndexed { i, player ->
            val (xFrac, yFrac) = playerSlots[i]
            Box(
                modifier = Modifier
                    .offset(x = w * xFrac - panelW / 2, y = h * yFrac - panelH / 2)
                    .width(panelW)
            ) {
                PlayerPanel(
                    player = player,
                    isCurrentPlayer = state.currentPlayerIndex == i,
                    isHuman = player.name == humanName,
                    isWinner = player.name in winners,
                    showCards = player.name == humanName || state.isHandOver,
                    handRank = handNames[player.name]
                )
            }
        }

        // Center: community cards + pot
        Box(
            modifier = Modifier
                .offset(x = w * 0.5f - 100.dp, y = h * 0.42f)
                .widthIn(max = 200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.communityCards.isNotEmpty()) {
                    CardFan(cards = state.communityCards, faceUp = true)
                    Spacer(Modifier.height(6.dp))
                }
                Text("Pot: $${state.pot}", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (state.currentBet > 0 && state.phase.ordinal > 0) {
                    Text("Current Bet: $${state.currentBet}", color = Color(0xFFE0E0E0), fontSize = 12.sp)
                }
                Text(state.phase.name, color = Color(0xFFAAAAAA), fontSize = 12.sp)
            }
        }
    }

    // Hand history overlay (right edge)
    if (state.handHistory.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight(0.5f)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("History", color = Color(0xFF76FF03), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                state.handHistory.takeLast(20).forEach { line ->
                    Text(line, color = Color(0xFFCCCCCC), fontSize = 10.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}
