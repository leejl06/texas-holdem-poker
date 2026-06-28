package com.leejl.poker.model

/** Actions a player can take on their turn. */
sealed interface PlayerAction : NamedAction {
    data class Fold(override val playerName: String) : PlayerAction, NamedAction
    data class Check(override val playerName: String) : PlayerAction, NamedAction
    data class Call(override val playerName: String) : PlayerAction, NamedAction
    data class Raise(override val playerName: String, val amount: Int) : PlayerAction, NamedAction
    data class AllIn(override val playerName: String, val amount: Int) : PlayerAction, NamedAction
}

/** Convenience: every action type knows the player name. */
interface NamedAction { val playerName: String }
