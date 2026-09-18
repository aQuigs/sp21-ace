package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.HandFilter
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move

enum class HandsDealt { RANDOM, PRIORITIZE_WORSE }

/** A kind of hand and the move it calls for, which Customize Hands can switch off. */
data class HandType(val table: ChartTable, val move: Move)

/** Every switch, the same whatever the rules, as in Blackjack Ace: each kind of hand with every move it calls for under any rule set. */
val HAND_TYPES: List<HandType> = HandFilter.entries.flatMap { filter -> filter.table?.let { table -> filter.moves.map { HandType(table, it) } }.orEmpty() }

/** How the trainer deals. A type is dealt unless switched off, so every switch starts on. */
data class HandCustomization(val handsDealt: HandsDealt = HandsDealt.RANDOM, val switchedOff: Set<HandType> = emptySet())
