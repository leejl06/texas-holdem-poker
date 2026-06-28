package com.leejl.poker.engine

import com.leejl.poker.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandEvaluatorTest {

    @Test fun `royal flush is highest`() {
        val h = listOf(c(14,'s'), c(13,'s'), c(12,'s'), c(11,'s'), c(10,'s'))
        assertEquals(HandRank.ROYAL_FLUSH, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `straight flush`() {
        val h = listOf(c(9,'h'), c(8,'h'), c(7,'h'), c(6,'h'), c(5,'h'))
        assertEquals(HandRank.STRAIGHT_FLUSH, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `four of a kind`() {
        val h = listOf(c(13,'s'), c(13,'h'), c(13,'d'), c(13,'c'), c(3,'s'))
        assertEquals(HandRank.FOUR_OF_A_KIND, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `full house`() {
        val h = listOf(c(14,'s'), c(14,'h'), c(14,'d'), c(12,'s'), c(12,'h'))
        assertEquals(HandRank.FULL_HOUSE, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `flush`() {
        val h = listOf(c(14,'c'), c(11,'c'), c(9,'c'), c(6,'c'), c(3,'c'))
        assertEquals(HandRank.FLUSH, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `straight`() {
        val h = listOf(c(9,'s'), c(8,'h'), c(7,'d'), c(6,'c'), c(5,'s'))
        assertEquals(HandRank.STRAIGHT, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `wheel straight Ace-5`() {
        val h = listOf(c(14,'s'), c(2,'h'), c(3,'d'), c(4,'c'), c(5,'s'))
        assertEquals(HandRank.STRAIGHT, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `three of a kind`() {
        val h = listOf(c(7,'s'), c(7,'h'), c(7,'d'), c(13,'s'), c(2,'h'))
        assertEquals(HandRank.THREE_OF_A_KIND, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `two pair`() {
        val h = listOf(c(11,'s'), c(11,'h'), c(5,'d'), c(5,'c'), c(14,'s'))
        assertEquals(HandRank.TWO_PAIR, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `one pair`() {
        val h = listOf(c(10,'s'), c(10,'h'), c(13,'d'), c(7,'s'), c(2,'h'))
        assertEquals(HandRank.ONE_PAIR, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `high card`() {
        val h = listOf(c(14,'s'), c(12,'h'), c(10,'d'), c(7,'c'), c(3,'s'))
        assertEquals(HandRank.HIGH_CARD, HandEvaluator.evaluate(h).rank)
    }

    @Test fun `best 7-card picks royal flush combo`() {
        val seven = listOf(c(14,'s'), c(14,'h'), c(14,'d'), c(13,'s'), c(12,'s'), c(11,'s'), c(10,'s'))
        assertEquals(HandRank.ROYAL_FLUSH, HandEvaluator.bestHand(seven).rank)
    }

    @Test fun `pair beats high card`() {
        val pair = listOf(c(2,'s'), c(2,'h'), c(9,'d'), c(5,'c'), c(13,'s'))
        val high = listOf(c(14,'s'), c(12,'h'), c(10,'d'), c(8,'c'), c(6,'s'))
        assertTrue(HandEvaluator.evaluate(pair) > HandEvaluator.evaluate(high))
    }

    @Test fun `determineWinner picks the best of two 7-card hands`() {
        // Pair hand (7 cards): 4♠ 4♥ 9♦ K♠ 3♦ Q♣ 7♠ — no straight/flush, best is pair
        val pair = listOf(c(4,'s'), c(4,'h'), c(9,'d'), c(13,'s'), c(3,'d'), c(12,'c'), c(7,'s'))
        // High card hand (7 cards): A♠ K♥ Q♦ J♣ 9♠ 8♦ 3♣ — no pair, best is A-high
        val high = listOf(c(14,'s'), c(13,'h'), c(12,'d'), c(11,'c'), c(9,'s'), c(8,'d'), c(3,'c'))
        val (idx, result) = HandEvaluator.determineWinner(listOf(pair, high))
        assertEquals(0, idx, "Pair hand should win")
        assertEquals(HandRank.ONE_PAIR, result.rank)
    }
}

private fun c(r: Int, suit: Char): Card {
    val s = when (suit) { 's' -> Suit.SPADES; 'h' -> Suit.HEARTS; 'd' -> Suit.DIAMONDS; else -> Suit.CLUBS }
    return Card(s, Rank.all().first { it.value == r })
}
