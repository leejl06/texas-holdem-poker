package com.leejl.poker.model

/** All 10 standard poker hand rankings, ordered weakest → strongest. */
enum class HandRank(val display: String) {
    HIGH_CARD("High Card"),
    ONE_PAIR("One Pair"),
    TWO_PAIR("Two Pair"),
    THREE_OF_A_KIND("Three of a Kind"),
    STRAIGHT("Straight"),
    FLUSH("Flush"),
    FULL_HOUSE("Full House"),
    FOUR_OF_A_KIND("Four of a Kind"),
    STRAIGHT_FLUSH("Straight Flush"),
    ROYAL_FLUSH("Royal Flush")
}

/**
 * The result of evaluating a 5-card hand.
 * First compared by [rank], then by [kickers] (highest first).
 */
data class HandResult(
    val rank: HandRank,
    val kickers: List<Int>      // descending; strength for tie-breaking
) : Comparable<HandResult> {
    override fun compareTo(other: HandResult): Int {
        val r = rank.compareTo(other.rank)
        if (r != 0) return r
        for ((a, b) in kickers.zip(other.kickers)) {
            val d = a.compareTo(b)
            if (d != 0) return d
        }
        return 0
    }
}
