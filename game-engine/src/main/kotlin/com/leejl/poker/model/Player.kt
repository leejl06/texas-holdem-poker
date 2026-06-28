package com.leejl.poker.model

data class Player(
    val name: String,
    val chips: Int,
    val holeCards: List<Card> = emptyList(),
    val currentBet: Int = 0,
    val totalBetThisRound: Int = 0,
    val folded: Boolean = false,
    val isAllIn: Boolean = false,
    val isDealer: Boolean = false,
    val isSmallBlind: Boolean = false,
    val isBigBlind: Boolean = false
) {
    val isActive: Boolean get() = !folded && !isAllIn
    val isEliminated: Boolean get() = chips <= 0
}
