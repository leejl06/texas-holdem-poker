package com.leejl.poker

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.leejl.poker.model.GameViewModel
import com.leejl.poker.ui.GameScreen
import com.leejl.poker.ui.theme.PokerTheme

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1100.dp, 760.dp),
        position = WindowPosition(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Texas Hold'em Poker",
        state = windowState
    ) {
        val viewModel = remember { GameViewModel() }

        PokerTheme {
            GameScreen(viewModel)
        }
    }
}
