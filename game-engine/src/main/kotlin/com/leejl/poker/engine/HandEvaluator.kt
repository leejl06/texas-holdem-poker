package com.leejl.poker.engine

import com.leejl.poker.model.*

object HandEvaluator {

    /** Evaluate the best 5-card hand from exactly 5 cards. */
    fun evaluate(cards: List<Card>): HandResult {
        require(cards.size == 5) { "Must evaluate exactly 5 cards, got ${cards.size}" }
        val sorted = cards.sortedByDescending { it.rank.value }
        val ranks = sorted.map { it.rank.value }
        val suits = sorted.map { it.suit }
        val isFlush = suits.distinct().size == 1

        // Detect straight: check 5 consecutive ranks (A-2-3-4-5 counts as 5-high)
        val isStraight = run {
            val unique = ranks.distinct()
            if (unique.size < 5) return@run false
            if (unique[0] - unique[4] == 4) return@run true
            // Wheel: A-2-3-4-5  (ranks would be 14,5,4,3,2)
            unique == listOf(14, 5, 4, 3, 2)
        }

        // For wheel we need to adjust the rank list
        val adjRanks = if (isStraight && ranks == listOf(14, 5, 4, 3, 2)) {
            listOf(5, 4, 3, 2, 1) // Ace counts as 1 for wheel
        } else ranks

        // Count rank occurrences
        val counts = adjRanks.groupingBy { it }.eachCount()
        val groups = counts.entries.sortedByDescending { it.value * 100 + it.key }

        return when {
            isFlush && isStraight && adjRanks[0] == 14 -> // A-K-Q-J-10 of same suit
                HandResult(HandRank.ROYAL_FLUSH, adjRanks)
            isFlush && isStraight ->
                HandResult(HandRank.STRAIGHT_FLUSH, adjRanks)
            groups[0].value == 4 && groups.size == 2 ->
                HandResult(HandRank.FOUR_OF_A_KIND, listOf(groups[0].key, groups[1].key))
            groups[0].value == 3 && groups.size == 2 && groups[1].value == 2 ->
                HandResult(HandRank.FULL_HOUSE, listOf(groups[0].key, groups[1].key))
            isFlush ->
                HandResult(HandRank.FLUSH, adjRanks)
            isStraight ->
                HandResult(HandRank.STRAIGHT, adjRanks)
            groups[0].value == 3 ->
                HandResult(HandRank.THREE_OF_A_KIND, groups.map { it.key })
            groups[0].value == 2 && groups.size == 3 && groups[1].value == 2 ->
                HandResult(HandRank.TWO_PAIR, groups.map { it.key })
            groups[0].value == 2 ->
                HandResult(HandRank.ONE_PAIR, groups.map { it.key })
            else ->
                HandResult(HandRank.HIGH_CARD, adjRanks)
        }
    }

    /** From 7 cards (2 hole + 5 community), find the best 5-card hand. */
    fun bestHand(sevenCards: List<Card>): HandResult {
        require(sevenCards.size == 7) { "Expected 5-7 cards, got ${sevenCards.size}" }
        return sevenCards.combinations(5).maxOf { evaluate(it) }
    }

    /** Compare two hands (7 cards each) and return the winner index + result. */
    fun determineWinner(hands: List<List<Card>>): Pair<Int, HandResult> {
        val results = hands.map { bestHand(it) }
        val best = results.max()
        return results.indexOf(best) to best
    }

    /** Generate all combinations of [k] elements from the list. */
    private fun <T> List<T>.combinations(k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (k > size) return emptyList()
        val result = mutableListOf<List<T>>()
        fun combine(start: Int, chosen: MutableList<T>) {
            if (chosen.size == k) { result.add(chosen.toList()); return }
            for (i in start until size) {
                chosen.add(this[i])
                combine(i + 1, chosen)
                chosen.removeLast()
            }
        }
        combine(0, mutableListOf())
        return result
    }
}
