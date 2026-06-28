package com.leejl.poker.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Main game area
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — game info
            TopBar(
                handNumber = state.handHistory.size,
                activeCount = viewModel.activePlayerCount,
                onNewGame = { viewModel.newHand() },
                modifier = Modifier.fillMaxWidth()
            )

            // Table area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                PokerTable(
                    state = state,
                    humanName = viewModel.humanName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom: Action Bar
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp)
                        .padding(bottom = 8.dp)
                )
            } else if (!viewModel.isHandOver) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Texas Hold'em",
            color = Color(0xFF76FF03),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Players: $activeCount", color = Color(0xFFAAAAAA), fontSize = 13.sp)
            Box(
                modifier = Modifier
                    .background(Color(0xFF2E7D32), RoundedCornerShape(6.dp))
                    .clickable(onClick = onNewGame)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("New Hand", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HandOverBar(message: String, onNewHand: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                .clickable(onClick = onNewHand)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Next Hand", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** The green felt poker table with player positions around it. */
@Composable
private fun PokerTable(
    state: GameState,
    humanName: String,
    modifier: Modifier = Modifier
) {
    val tableColor = Color(0xFF1B6B2E)
    val tableAccent = Color(0xFF0D4F1A)

    Box(modifier = modifier) {
        // Green felt table background
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = tableColor,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.3f, h * 0.3f)
            )
            // Inner oval
            drawRoundRect(
                color = tableAccent,
                topLeft = Offset(w * 0.03f, h * 0.04f),
                size = Size(w * 0.94f, h * 0.92f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.28f, h * 0.28f)
            )
        }

        // Player positions around the table (6 players)
        val players = state.players

        // Position coordinates (relative to center, 0..1 for fraction of width/height)
        // Human at bottom center
        val positions = listOf(
            0.5f to 0.92f,  // Bottom center (human)
            0.82f to 0.68f,  // Bottom right
            0.82f to 0.25f,  // Top right
            0.5f to 0.06f,   // Top center
            0.18f to 0.25f,  // Top left
            0.18f to 0.68f   // Bottom left
        )

        val outcome = state.outcome
        val winners = outcome?.winnerNames?.toSet() ?: emptySet()
        val handNames = outcome?.handNames ?: emptyMap()

        Box(modifier = Modifier.fillMaxSize()) {
            players.forEachIndexed { i, player ->
                val (x, y) = positions[i]
                val isCurrent = state.currentPlayerIndex == i
                val isHuman = player.name == humanName
                val showCards = isHuman || state.isHandOver

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(
                            align = Alignment.TopStart,
                            unbounded = true
                        )
                        .offset(
                            x = (x * 1000).dp / 10f,
                            y = (y * 1000).dp / 10f
                        )
                ) {
                    PlayerPanel(
                        player = player,
                        isCurrentPlayer = isCurrent,
                        isHuman = isHuman,
                        isWinner = player.name in winners,
                        showCards = showCards,
                        handRank = handNames[player.name],
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }

        // Community cards + pot in center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.communityCards.isNotEmpty()) {
                    CardFan(
                        cards = state.communityCards,
                        faceUp = true,
                        small = false
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Pot display
                Text(
                    "Pot: $${state.pot}",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Current bet
                if (state.currentBet > 0 && state.phase.ordinal > 0) {
                    Text(
                        "Current Bet: $${state.currentBet}",
                        color = Color(0xFFE0E0E0),
                        fontSize = 12.sp
                    )
                }

                // Round indicator
                Text(
                    state.phase.name,
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
        }

        // Hand history (compact scrollable)
        if (state.handHistory.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(160.dp)
                    .fillMaxHeight(0.5f)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column {
                    Text("History", color = Color(0xFF76FF03), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    state.handHistory.takeLast(20).forEach { line ->
                        Text(
                            line,
                            color = Color(0xFFCCCCCC),
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}
