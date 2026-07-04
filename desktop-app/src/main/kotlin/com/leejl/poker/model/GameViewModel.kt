package com.leejl.poker.model

import androidx.compose.runtime.*
import com.leejl.poker.ai.AiStrategy
import com.leejl.poker.engine.GameManager

/**
 * ViewModel for the poker game.
 * Logs all actions + errors to stdout/stderr for real-time debugging.
 */
class GameViewModel(
    playerNames: List<String> = listOf(
        "You", "AI-Alice", "AI-Bob", "AI-Carol", "AI-Dave", "AI-Eve"
    ),
    startingChips: Int = 1500
) {
    val humanName = playerNames.first()
    val aiNames = playerNames.drop(1).toSet()

    private val manager = GameManager(playerNames, startingChips)

    var state by mutableStateOf(manager.startNewHand())
        private set

    var lastAction by mutableStateOf("")
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    val isPlayerTurn: Boolean
        get() = state.currentPlayer?.name == humanName && !state.isHandOver

    val isHandOver: Boolean get() = state.isHandOver

    val activePlayerCount: Int get() = state.players.count { !it.isEliminated }

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            System.err.println("[Poker] UNCAUGHT: ${e.message}")
            e.printStackTrace(System.err)
            lastError = "Fatal: ${e.message}"
        }
    }

    fun newHand() {
        lastError = null
        safeRun("newHand") {
            state = manager.startNewHand()
            lastAction = "New hand dealt"
            log("New hand — phase=${state.phase} pot=${state.pot}")
            runAiActions()
        }
    }

    fun act(action: PlayerAction) {
        lastError = null
        safeRun("act") {
            require(action.playerName == humanName) { "Can only act for human player: ${action.playerName}" }
            if (state.isHandOver) return@safeRun
            log("Player action: ${formatAction(action)}")
            state = manager.applyAction(action)
            lastAction = formatAction(action)
            if (!state.isHandOver) runAiActions()
        }
    }

    fun availableActions() = try { manager.availableActions() } catch (e: Exception) {
        System.err.println("[Poker] availableActions error: ${e.message}")
        lastError = "availableActions: ${e.message}"
        emptyList()
    }

    private fun runAiActions() {
        val aiStates = manager.runAiActions(aiNames)
        aiStates.forEach { state = it }
        if (aiStates.isNotEmpty()) {
            lastAction = state.lastAction
        }
        if (state.isHandOver) {
            lastAction = state.outcome?.let { o -> o.winnerNames.joinToString(", ") + " won!" } ?: "Hand over"
        }
        log("AI done — phase=${state.phase} turn=${state.currentPlayer?.name}")
    }

    private fun log(msg: String) {
        System.out.println("[Poker] $msg  |  cards=${state.communityCards.size} pot=${state.pot} bet=${state.currentBet}")
    }

    /** Run a block; on exception set [lastError] and print stack trace, but do NOT crash. */
    private inline fun safeRun(context: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            System.err.println("[Poker] ERROR in $context: ${e.message}")
            e.printStackTrace(System.err)
            lastError = "$context: ${e.message}"
        }
    }
}

private fun formatAction(action: PlayerAction): String = when (action) {
    is PlayerAction.Fold -> "${action.playerName} folds"
    is PlayerAction.Check -> "${action.playerName} checks"
    is PlayerAction.Call -> "${action.playerName} calls"
    is PlayerAction.Raise -> "${action.playerName} raises to $${action.amount}"
    is PlayerAction.AllIn -> "${action.playerName} goes ALL-IN!"
}
