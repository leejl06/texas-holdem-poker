package com.leejl.poker.model

/** Standard 52-card suit. */
enum class Suit(val symbol: String) {
    SPADES("♠"),
    HEARTS("♥"),
    DIAMONDS("♦"),
    CLUBS("♣");

    companion object { fun all() = entries.toList() }
}

/** Standard 52-card rank (2–14, Ace-high). */
enum class Rank(val value: Int, val display: String) {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"),
    SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"),
    TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");

    companion object { fun all() = entries.toList() }
}

data class Card(val suit: Suit, val rank: Rank) : Comparable<Card> {
    override fun compareTo(other: Card) = this.rank.value.compareTo(other.rank.value)
    override fun toString() = "${rank.display}${suit.symbol}"
}
