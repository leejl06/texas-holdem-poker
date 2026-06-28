package com.leejl.poker.model

import kotlin.random.Random

/** A standard 52-card deck that can be shuffled and dealt from. */
class Deck(private val rng: Random = Random) {
    private val cards = mutableListOf<Card>()
    private var dealtIndex = 0

    init { reset() }

    fun reset() {
        cards.clear()
        for (suit in Suit.all()) for (rank in Rank.all()) cards.add(Card(suit, rank))
        dealtIndex = 0
    }

    fun shuffle() {
        val remaining = cards.subList(dealtIndex, cards.size)
        remaining.shuffle(rng)
    }

    /** The number of cards remaining in the deck. */
    val remainingCount get() = cards.size - dealtIndex

    /** Deal one card from the top of the deck. */
    fun deal(): Card {
        require(dealtIndex < cards.size) { "No cards left in deck" }
        return cards[dealtIndex++]
    }

    /** Deal [n] cards. */
    fun deal(n: Int): List<Card> = (1..n).map { deal() }
}
