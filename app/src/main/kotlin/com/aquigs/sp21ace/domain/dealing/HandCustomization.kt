package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.HandFilter
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move

enum class HandsDealt { RANDOM, PRIORITIZE_WORSE }

/** A kind of hand and the move it calls for, which Customize Hands can switch off. */
data class HandType(val table: ChartTable, val move: Move)

/** A kind of hand's switches, the same whatever the rules, as in Blackjack Ace: one for every move it calls for under any rule set. All isn't a kind of hand, so it has none. */
val HandFilter.types: List<HandType> get() = table?.let { table -> moves.map { HandType(table, it) } }.orEmpty()

/** Every switch, kind by kind. */
val HAND_TYPES: List<HandType> = HandFilter.entries.flatMap { it.types }

/**
 * How the trainer deals. A type is dealt unless switched off, so every switch starts on, as do the ones for hands of 3 or more
 * cards, for card-count hands and for bonus hands.
 */
data class HandCustomization(
    val handsDealt: HandsDealt = HandsDealt.RANDOM,
    val switchedOff: Set<HandType> = emptySet(),
    val multiCardHands: Boolean = true,
    val cardCountHands: Boolean = true,
    val bonusHands: Boolean = true,
)
