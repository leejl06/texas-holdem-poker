package com.leejl.poker.ai

import com.leejl.poker.engine.HandEvaluator
import com.leejl.poker.engine.RoundPhase
import com.leejl.poker.model.*

/**
 * Simple AI strategy for Texas Hold'em.
 * Evaluates hand strength and uses basic thresholds to decide actions.
 */
object AiStrategy {

    fun chooseAction(
        holeCards: List<Card>,
        communityCards: List<Card>,
        currentBet: Int,
        playerBet: Int,
        chips: Int,
        pot: Int,
        phase: RoundPhase,
        actions: List<PlayerAction>
    ): PlayerAction {
        val allCards = holeCards + communityCards
        val handResult = if (communityCards.isEmpty()) null
        else HandEvaluator.bestHand(allCards)
        val handStrength = handResult?.rank?.ordinal ?: 0

        val toCall = currentBet - playerBet
        val potOdds = if (toCall > 0) pot.toFloat() / toCall else Float.MAX_VALUE

        // Pre-flop strategy (only hole cards)
        if (phase == RoundPhase.PREFLOP) {
            val score = scorePreFlop(holeCards[0], holeCards[1])
            return when {
                // Premium hands: raise
                score >= 1.0f -> pick(actions, { it is PlayerAction.Raise }, { it is PlayerAction.Call })
                // Playable: call or check
                score >= 0.5f && toCall <= chips * 0.15f -> pick(actions, { it is PlayerAction.Call }, { it is PlayerAction.Check })
                // Marginal: limp if cheap
                score >= 0.3f && toCall <= chips * 0.06f -> pick(actions, { it is PlayerAction.Call }, { it is PlayerAction.Check })
                // Trash
                else -> pick(actions, { it is PlayerAction.Check }, { it is PlayerAction.Call }, { it is PlayerAction.Fold })
            }
        }

        // Post-flop strategy
        return when {
            // Premium made hands
            handStrength >= HandRank.STRAIGHT.ordinal -> {
                if (potOdds >= 2.0f && actions.any { it is PlayerAction.Raise })
                    pick(actions, { it is PlayerAction.Raise }, { it is PlayerAction.Call })
                else pick(actions, { it is PlayerAction.Call }, { it is PlayerAction.Check })
            }
            // Good made hands: top pair, overpair
            handStrength >= HandRank.ONE_PAIR.ordinal && handResult!!.kickers[0] >= 10 -> {
                if (potOdds >= 3.0f)
                    pick(actions, { it is PlayerAction.Call }, { it is PlayerAction.Check }, { it is PlayerAction.Raise })
                else pick(actions, { it is PlayerAction.Check }, { it is PlayerAction.Call }, { it is PlayerAction.Fold })
            }
            // Weak pair or draw
            handStrength >= HandRank.ONE_PAIR.ordinal -> {
                if (potOdds >= 5.0f)
                    pick(actions, { it is PlayerAction.Call }, { it is PlayerAction.Check })
                else pick(actions, { it is PlayerAction.Check }, { it is PlayerAction.Call }, { it is PlayerAction.Fold })
            }
            // High card: bluff occasionally
            chips > toCall * 4 && rng.nextFloat() < 0.12f && toCall == 0 ->
                pick(actions, { it is PlayerAction.Raise }, { it is PlayerAction.Check })
            // Fold to aggression
            toCall > chips * 0.25f -> pick(actions, { it is PlayerAction.Check }, { it is PlayerAction.Call }, { it is PlayerAction.Fold })
            // Free check
            toCall == 0 -> pick(actions, { it is PlayerAction.Check }, { it is PlayerAction.Fold })
            // Cheap call
            else -> pick(actions, { it is PlayerAction.Call }, { it is PlayerAction.Check }, { it is PlayerAction.Fold })
        }
    }

    /** Pick the first action whose type matches one of the type-checkers, in order. */
    private fun pick(
        actions: List<PlayerAction>,
        vararg checks: (PlayerAction) -> Boolean
    ): PlayerAction {
        for (check in checks) {
            val found = actions.find(check)
            if (found != null) return found
        }
        return PlayerAction.Fold("")
    }

    /** Score preflop hand: 0 (trash) to ~1.2 (premium). */
    private fun scorePreFlop(c1: Card, c2: Card): Float {
        val pair = c1.rank == c2.rank
        val suited = c1.suit == c2.suit
        val high = maxOf(c1.rank.value, c2.rank.value)
        val low = minOf(c1.rank.value, c2.rank.value)
        val gap = high - low

        return when {
            pair && high >= 12 -> 1.2f                          // AA, KK
            pair && high >= 10 -> 1.1f                          // QQ, JJ
            pair && high >= 8 -> 0.9f                           // 88, 99
            pair && high >= 6 -> 0.6f                           // 66, 77
            pair -> 0.4f                                        // Low pair
            suited && high >= 13 && low >= 11 -> 1.1f           // AKs
            high >= 13 && low >= 11 -> 1.0f                     // AKo
            suited && high >= 13 && low >= 9 -> 0.8f            // AQs, AJs
            high >= 13 && low >= 9 -> 0.7f                      // AQo, AJo
            suited && gap <= 2 && high >= 10 -> 0.6f            // KQs, QJs, JTs
            gap <= 1 && high >= 10 -> 0.5f                      // KQo, QJo, JTo
            high >= 13 -> 0.4f                                  // Ace-x (weak)
            gap <= 2 -> 0.3f                                    // Connectors
            else -> 0.1f                                        // Trash
        }
    }

    private val rng = kotlin.random.Random
}
