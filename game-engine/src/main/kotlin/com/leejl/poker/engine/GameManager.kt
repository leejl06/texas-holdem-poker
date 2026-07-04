package com.leejl.poker.engine

import com.leejl.poker.model.*
import com.leejl.poker.ai.AiStrategy

import kotlin.math.min

/** Betting phases in a single hand of Texas Hold'em. */
enum class RoundPhase {
    PREFLOP, FLOP, TURN, RIVER, SHOWDOWN;

    val communityCardsCount: Int get() = when (this) {
        PREFLOP -> 0; FLOP -> 3; TURN -> 4; RIVER -> 5; SHOWDOWN -> 5
    }
}

/** Outcome of a single hand. */
data class HandOutcome(
    val winnerNames: List<String>,
    val handNames: Map<String, String>,   // player -> hand rank display
    val winAmounts: Map<String, Int>,     // player -> chips won this hand
    val communityCards: List<Card>,
    val playersAtShowdown: List<String>
)

/** Full game state that the UI can observe. */
data class GameState(
    val players: List<Player>,
    val communityCards: List<Card>,
    val pot: Int,
    val sidePots: List<SidePot>,
    val currentPlayerIndex: Int,
    val dealerIndex: Int,
    val phase: RoundPhase,
    val lastAction: String,
    val handHistory: List<String>,
    val winner: String?,
    val outcome: HandOutcome?,
    val playerActionsThisRound: Int,
    val minRaise: Int,
    val currentBet: Int,
    val smallBlind: Int,
    val bigBlind: Int
) {
    val activePlayers get() = players.filter { it.isActive }
    val playersInHand get() = players.filter { !it.folded }
    val currentPlayer: Player? get() = players.getOrNull(currentPlayerIndex)
    val isHandOver: Boolean get() = winner != null
}

data class SidePot(val amount: Int, val eligiblePlayerNames: List<String>)

/**
 * Manages the full lifecycle of a Texas Hold'em hand.
 * Pure state machine — no side effects.
 */
class GameManager(
    playerNames: List<String>,
    startingChips: Int = 1500,
    val smallBlind: Int = 10,
    val bigBlind: Int = 20
) {
    private val rng = kotlin.random.Random
    private val deck = Deck(rng)
    private val _initialPlayerNames: List<String> = playerNames
    private val _startingChips: Int = startingChips
    private var _players: MutableList<Player> = _initialPlayerNames.map { Player(name = it, chips = _startingChips) }.toMutableList()
    private var _communityCards = mutableListOf<Card>()
    private var _pot = 0
    private var _sidePots = listOf<SidePot>()
    private var _currentPlayerIndex = 0
    private var _dealerIndex = 0
    private var _phase = RoundPhase.PREFLOP
    private var _lastAction = ""
    private val _handHistory = mutableListOf<String>()
    private var _winner: String? = null
    private var _outcome: HandOutcome? = null
    private var _playerActionsThisRound = 0
    private var _minRaise = bigBlind
    private var _currentBet = 0

    // Snapshots for tracking bets per round
    private var _betTracker = RoundBetTracker()

    fun state(): GameState = GameState(
        players = _players.toList(),
        communityCards = _communityCards.toList(),
        pot = _pot,
        sidePots = _sidePots,
        currentPlayerIndex = _currentPlayerIndex,
        dealerIndex = _dealerIndex,
        phase = _phase,
        lastAction = _lastAction,
        handHistory = _handHistory.toList(),
        winner = _winner,
        outcome = _outcome,
        playerActionsThisRound = _playerActionsThisRound,
        minRaise = _minRaise,
        currentBet = _currentBet,
        smallBlind = smallBlind,
        bigBlind = bigBlind
    )

    // ── Public API ──────────────────────────────────────────────

    /** Start a new hand. Returns the initial state. */
    fun startNewHand(): GameState {
        _winner = null
        _outcome = null
        _handHistory.clear()
        _communityCards.clear()
        _pot = 0
        _sidePots = emptyList()
        _phase = RoundPhase.PREFLOP
        _betTracker = RoundBetTracker()
        _minRaise = bigBlind
        _currentBet = 0

        // Remove eliminated players
        _players = _players.filter { !it.isEliminated }.toMutableList()
        if (_players.size < 2) {
        _players = _initialPlayerNames.map { Player(name = it, chips = _startingChips) }.toMutableList()
        log("--- New Game! All players reset ---")
    }

        // Advance dealer
        _dealerIndex = advanceIndex(_dealerIndex, 1)
        deck.reset()
        deck.shuffle()

        // Reset player states for new hand
        _players = _players.map { it.copy(
            holeCards = emptyList(), currentBet = 0, totalBetThisRound = 0,
            folded = false, isAllIn = false,
            isDealer = false, isSmallBlind = false, isBigBlind = false
        ) }.toMutableList()

        // Mark positions
        _players = _players.mapIndexed { i, p ->
            p.copy(
                isDealer = i == _dealerIndex,
                isSmallBlind = i == advanceIndex(_dealerIndex, 1),
                isBigBlind = i == advanceIndex(_dealerIndex, 2)
            )
        }.toMutableList()

        // Deal hole cards
        _players = _players.map { it.copy(holeCards = deck.deal(2)) }.toMutableList()

        // Post blinds
        val sbIdx = advanceIndex(_dealerIndex, 1)
        val bbIdx = advanceIndex(_dealerIndex, 2)
        postBlind(sbIdx, smallBlind)
        postBlind(bbIdx, bigBlind)
        _currentBet = bigBlind

        // First to act preflop is after big blind (UTG)
        _currentPlayerIndex = advanceIndex(bbIdx, 1)

        log("--- New Hand ---")
        log("${_players[_dealerIndex].name} is dealer (SB $smallBlind / BB $bigBlind)")
        log("${_players[sbIdx].name} posts SB $$smallBlind")
        log("${_players[bbIdx].name} posts BB $$bigBlind")

        _playerActionsThisRound = 0
        return state()
    }

    /** Apply a player action, advance state. Returns the new state. */
    fun applyAction(action: PlayerAction): GameState {
        val idx = _players.indexOfFirst { it.name == action.playerName }
        if (idx == -1) return state()
        val player = _players[idx]

        when (action) {
            is PlayerAction.Fold -> {
                _players[idx] = player.copy(folded = true)
                log("${player.name} folds")
                checkHandOver()
            }
            is PlayerAction.Check -> {
                _players[idx] = player.copy(currentBet = _currentBet)
                log("${player.name} checks")
            }
            is PlayerAction.Call -> {
                val callAmount = _currentBet - player.currentBet
                val actual = min(callAmount, player.chips)
                _players[idx] = player.copy(
                    chips = player.chips - actual,
                    currentBet = player.currentBet + actual,
                    totalBetThisRound = player.totalBetThisRound + actual,
                    isAllIn = player.chips - actual <= 0
                )
                _pot += actual
                log("${player.name} calls $$actual")
            }
            is PlayerAction.Raise -> {
                val totalNeeded = _currentBet - player.currentBet + action.amount
                val actual = min(totalNeeded, player.chips)
                _currentBet = player.currentBet + actual
                _minRaise = _minRaise.coerceAtLeast(action.amount)
                _players[idx] = player.copy(
                    chips = player.chips - actual,
                    currentBet = player.currentBet + actual,
                    totalBetThisRound = player.totalBetThisRound + actual,
                    isAllIn = player.chips - actual <= 0
                )
                _pot += actual
                // Reset action count — everyone needs to respond to the raise
                _betTracker = _betTracker.raiseReset()
                log("${player.name} raises to $${_currentBet} ($${_minRaise} min)")
            }
            is PlayerAction.AllIn -> {
                val allInAmount = player.chips
                _players[idx] = player.copy(
                    chips = 0,
                    currentBet = player.currentBet + allInAmount,
                    totalBetThisRound = player.totalBetThisRound + allInAmount,
                    isAllIn = true
                )
                _pot += allInAmount
                if (player.currentBet + allInAmount > _currentBet) {
                    _currentBet = player.currentBet + allInAmount
                    _betTracker = _betTracker.raiseReset()
                }
                log("${player.name} goes ALL-IN $$allInAmount")
            }
        }

        _betTracker = _betTracker.recordAction(idx)
        _playerActionsThisRound++

        if (_winner != null) return finalizeHand()

        // Advance to next active player
        advanceToNextPlayer()
        return state()
    }

    /** Available actions for the current player. */
    fun availableActions(): List<PlayerAction> {
        val player = _players.getOrNull(_currentPlayerIndex) ?: return emptyList()
        if (!player.isActive) return emptyList()
        val toCall = _currentBet - player.currentBet
        val actions = mutableListOf<PlayerAction>()

        if (toCall == 0) {
            actions.add(PlayerAction.Check(player.name))
        } else {
            actions.add(PlayerAction.Fold(player.name))
            actions.add(PlayerAction.Call(player.name))
        }

        // Raise: need at least minRaise above current bet
        val minTotal = _currentBet + _minRaise
        val raisePossible = player.chips > toCall
        if (raisePossible) {
            if (player.chips >= minTotal - player.currentBet) {
                actions.add(PlayerAction.Raise(player.name, _minRaise))
            }
            // All-in is always an option if we have chips
            if (player.chips > 0) {
                actions.add(PlayerAction.AllIn(player.name, player.chips))
            }
        }

        return actions
    }

    /** Run AI actions for all remaining AI players (for the current hand). */
    fun runAiActions(aiNames: Set<String>): List<GameState> {
        val states = mutableListOf<GameState>()
        var aiStep = 0
        while (_winner == null && aiStep < 200) {
            val player = _players.getOrNull(_currentPlayerIndex) ?: break
            if (!player.isActive) { advanceToNextPlayer(); continue }
            if (player.name !in aiNames) break // human's turn

            val actions = availableActions()
            val action = AiStrategy.chooseAction(
                holeCards = player.holeCards,
                communityCards = _communityCards,
                currentBet = _currentBet,
                playerBet = player.currentBet,
                chips = player.chips,
                pot = _pot,
                phase = _phase,
                actions = actions
            )
            states.add(applyAction(action))
            aiStep++
        }
        return states
    }

    // ── Private helpers ─────────────────────────────────────────

    private fun advanceIndex(from: Int, steps: Int): Int {
        var i = from
        var remaining = steps
        while (remaining > 0) {
            i = (i + 1) % _players.size
            remaining--
        }
        return i
    }

    private fun postBlind(idx: Int, amount: Int) {
        val player = _players[idx]
        val actual = min(amount, player.chips)
        _players[idx] = player.copy(
            chips = player.chips - actual,
            currentBet = actual,
            totalBetThisRound = actual,
            isAllIn = player.chips - actual <= 0
        )
        _pot += actual
    }

    private fun log(msg: String) { _handHistory.add(msg) }

    /** Advance index to the next active (non-folded, non-all-in) player. */
    private fun advanceToNextPlayer() {
        if (_winner != null) return
        var count = 0
        do {
            _currentPlayerIndex = (_currentPlayerIndex + 1) % _players.size
            count++
        } while (!_players[_currentPlayerIndex].isActive && count <= _players.size)

        // Check if betting round is complete
        if (isBettingRoundComplete()) {
            advancePhase()
        }
    }

    /** Betting round is complete when all active players have acted and matched the current bet. */
    private fun isBettingRoundComplete(): Boolean {
        val active = _players.indices.filter { _players[it].isActive }
        if (active.isEmpty()) return true
        val allActed = active.all { _betTracker.hasActed(it) }
        if (!allActed) return false
        val allBetMatch = active.all { _players[it].currentBet == _currentBet || _players[it].isAllIn }
        return allBetMatch
    }

    private fun advancePhase() {
        // Reset bets for the round
        _players = _players.map { it.copy(currentBet = 0, totalBetThisRound = 0) }.toMutableList()
        _currentBet = 0
        _betTracker = RoundBetTracker()

        _phase = when (_phase) {
            RoundPhase.PREFLOP -> {
                _communityCards.addAll(deck.deal(3))
                log("--- Flop: ${_communityCards.takeLast(3).joinToString(" ")} ---")
                RoundPhase.FLOP
            }
            RoundPhase.FLOP -> {
                _communityCards.add(deck.deal())
                log("--- Turn: [${_communityCards.last()}] ---")
                RoundPhase.TURN
            }
            RoundPhase.TURN -> {
                _communityCards.add(deck.deal())
                log("--- River: [${_communityCards.last()}] ---")
                RoundPhase.RIVER
            }
            RoundPhase.RIVER -> {
                _phase = RoundPhase.SHOWDOWN
                return showdown()
            }
            RoundPhase.SHOWDOWN -> return
        }

        // First to act after flop is first active player after dealer
        _currentPlayerIndex = advanceIndex(_dealerIndex, 1)
        // Skip folded/all-in players (check current first, do-while skips it)
        var skipCount = 0
        while (!_players[_currentPlayerIndex].isActive && skipCount < _players.size) {
            _currentPlayerIndex = (_currentPlayerIndex + 1) % _players.size
            skipCount++
        }

        // If all players are all-in, auto-advance to the next phase
        if (!_players[_currentPlayerIndex].isActive && isBettingRoundComplete()) {
            advancePhase()
        }
    }

    private fun checkHandOver() {
        val inHand = _players.filter { !it.folded && !it.isEliminated }
        if (inHand.size == 1) {
            _winner = inHand[0].name
            log("${inHand[0].name} wins (all others folded)")
        }
    }

    private fun showdown() {
        val contenders = _players.filter { !it.folded }
        if (contenders.size == 1) {
            _winner = contenders[0].name
            log("${contenders[0].name} wins (uncontested)")
            return
        }

        // Calculate side pots
        val allInPlayers = contenders.filter { it.isAllIn }.sortedBy { it.totalBetThisRound }
        var alreadyAllocated = 0
        val pots = mutableListOf<SidePot>()

        for (ai in allInPlayers) {
            val atThisLevel = ai.totalBetThisRound - alreadyAllocated
            if (atThisLevel <= 0) continue
            val contributors = contenders.filter { it.totalBetThisRound >= ai.totalBetThisRound }
            val amount = contributors.size * atThisLevel
            pots.add(SidePot(amount, contributors.map { it.name }))
            alreadyAllocated += atThisLevel
        }

        // Main pot for the remainder
        val mainContributors = contenders.filter { it.totalBetThisRound > alreadyAllocated }
        if (mainContributors.isNotEmpty()) {
            val remainAmount = mainContributors.sumOf { it.totalBetThisRound - alreadyAllocated }
            pots.add(SidePot(_pot - pots.sumOf { it.amount }, contenders.filter { it.totalBetThisRound >= alreadyAllocated + (if (mainContributors.isNotEmpty()) 1 else 0) }.map { it.name }))
        } else {
            // All chips accounted for in side pots — add remaining pot difference
            val allocated = pots.sumOf { it.amount }
            if (allocated < _pot) {
                pots.add(SidePot(_pot - allocated, contenders.map { it.name }))
            }
        }

        _sidePots = pots

        // Determine winners for each pot
        val playerHands = contenders.associate { it.name to (it.holeCards + _communityCards) }
        val winners = mutableListOf<String>()
        val winAmounts = mutableMapOf<String, Int>()
        val handNames = mutableMapOf<String, String>()

        for (pot in _sidePots) {
            val eligible = pot.eligiblePlayerNames
            if (eligible.isEmpty()) continue
            val eligibleHands = eligible.mapNotNull { n ->
                playerHands[n]?.let { n to HandEvaluator.bestHand(it) }
            }
            if (eligibleHands.isEmpty()) continue
            val best = eligibleHands.maxBy { it.second }
            val potWinners = eligibleHands.filter { it.second == best.second }.map { it.first }
            val share = pot.amount / potWinners.size
            val remainder = pot.amount % potWinners.size
            potWinners.forEachIndexed { i, name ->
                winAmounts[name] = (winAmounts[name] ?: 0) + share + if (i == 0) remainder else 0
                handNames[name] = best.second.rank.display
            }
            winners.addAll(potWinners)
        }

        _winner = winners.joinToString(", ")
        _outcome = HandOutcome(
            winnerNames = winners.distinct(),
            handNames = handNames,
            winAmounts = winAmounts,
            communityCards = _communityCards.toList(),
            playersAtShowdown = contenders.map { it.name }
        )

        log("--- Showdown ---")
        contenders.forEach { p ->
            val result = playerHands[p.name]?.let { HandEvaluator.bestHand(it) }
            log("${p.name}: ${p.holeCards.joinToString(" ")} → ${result?.rank?.display ?: "?"}")
        }
        winAmounts.forEach { (name, amount) ->
            log("$name wins $$amount${handNames[name]?.let { " ($it)" } ?: ""}")
        }
    }

    private fun finalizeHand(): GameState {
        _outcome?.let { outcome ->
            _players = _players.map { p ->
                val won = outcome.winAmounts[p.name] ?: 0
                p.copy(chips = p.chips + won)
            }.toMutableList()
        }
        return state()
    }
}

/** Tracks which players have acted in the current betting round. */
private data class RoundBetTracker(
    private val acted: Set<Int> = emptySet()
) {
    fun hasActed(index: Int) = index in acted
    fun recordAction(index: Int) = copy(acted = acted + index)
    fun raiseReset() = copy(acted = emptySet()) // After a raise, everyone must act again
}
