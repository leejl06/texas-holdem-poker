package com.leejl.poker.model

import androidx.compose.runtime.*
import com.leejl.poker.ai.AiStrategy
import com.leejl.poker.engine.GameManager
import com.leejl.poker.engine.GameState

/**
 * ViewModel for the poker game.
 * Owns the [GameManager] and exposes reactive [GameState] via Compose [State].
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

    val isPlayerTurn: Boolean
        get() = state.currentPlayer?.name == humanName && !state.isHandOver

    val isHandOver: Boolean get() = state.isHandOver

    val activePlayerCount: Int get() = state.players.count { !it.isEliminated }

    fun newHand() {
        state = manager.startNewHand()
        lastAction = "New hand dealt"
        runAiActions()
    }

    fun act(action: PlayerAction) {
        require(action.playerName == humanName) { "Can only act for human player" }
        if (state.isHandOver) return
        state = manager.applyAction(action)
        lastAction = formatAction(action)
        if (!state.isHandOver) runAiActions()
    }

    fun availableActions() = manager.availableActions()

    /** Run AI actions synchronously (fast — typically < 10 iterations per round). */
    private fun runAiActions() {
        val aiStates = manager.runAiActions(aiNames)
        aiStates.forEach { state = it }
        if (aiStates.isNotEmpty()) {
            lastAction = state.lastAction
        }
        if (state.isHandOver) {
            lastAction = state.outcome?.let { outcome ->
                outcome.winnerNames.joinToString(", ") + " won!"
            } ?: "Hand over"
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
