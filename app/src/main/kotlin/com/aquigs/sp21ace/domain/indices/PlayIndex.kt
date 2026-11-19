package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard
import java.io.Serializable

/** The game an index is for. Spanish 21 joins with its own indices. */
enum class Game(val title: String) { BLACKJACK("Blackjack") }

/**
 * The [values] each kind of hand can have: a hard or soft total two cards make without a pair, since two cards make a hard 4
 * only as 2-2, a hard 20 only as two tens and a soft 12 only as A-A, or a pair's card value, an ace 1 as
 * [com.aquigs.sp21ace.domain.cards.Rank.value] counts it. So every index can be dealt as two cards.
 */
enum class HandKind(val values: IntRange) {
    HARD(5..19),
    SOFT(13..20),
    PAIR(1..10),
}

/** A player's hand as an index names it: a hard or soft total, or a pair. */
data class IndexHand(val kind: HandKind, val value: Int) : Serializable {
    init {
        require(value in kind.values) { "No $kind hand of $value" }
    }

    /** The hand as the strategy charts key their rows: "16" for hard 16, "A-7" for soft 18, "8-8" and "A-A" for pairs. */
    val label: String
        get() = when (kind) {
            HandKind.HARD -> "$value"
            HandKind.SOFT -> "A-${value - 11}"
            HandKind.PAIR -> cardLabel(value).let { "$it-$it" }
        }

    companion object {
        /** The hand [label] names, as [IndexHand.label] writes it. */
        fun parse(label: String): IndexHand {
            val cards = label.split('-')
            return when {
                cards.size == 1 -> IndexHand(HandKind.HARD, requireNotNull(label.toIntOrNull()) { "Unknown hand: $label" })
                cards.size != 2 -> throw IllegalArgumentException("Unknown hand: $label")
                cards[0] == cards[1] -> IndexHand(HandKind.PAIR, cardValue(cards[0]))
                cards[0] == "A" -> IndexHand(HandKind.SOFT, 11 + cardValue(cards[1]))
                else -> throw IllegalArgumentException("Unknown hand: $label")
            }.also { require(it.label == label) { "Unknown hand: $label" } }
        }

        private fun cardLabel(value: Int) = if (value == 1) "A" else "$value"

        private fun cardValue(label: String) = if (label == "A") 1 else requireNotNull(label.toIntOrNull()) { "Unknown card: $label" }
    }
}

/** The play an index names, which a negative index is often a hit: the play to switch to as the count falls. */
enum class DeviationPlay { HIT, STAND, DOUBLE, SPLIT, SURRENDER }

/** The question an index answers: when to make a play basic strategy doesn't, a hand's play against an upcard, or insurance. */
sealed interface Deviation : Serializable {
    val upcard: Upcard

    data class OnHand(val hand: IndexHand, override val upcard: Upcard, val play: DeviationPlay) : Deviation {
        init {
            require(play != DeviationPlay.SPLIT || hand.kind == HandKind.PAIR) { "Only a pair splits: ${hand.label}" }
        }
    }

    /** Insurance is offered only against a dealer ace, and asks for no hand. */
    data object Insurance : Deviation {
        override val upcard: Upcard get() = Upcard.ACE

        // Restored state comes back as this one object rather than a copy
        private fun readResolve(): Any = Insurance
    }
}

/** Which side of its number an index makes its play on. Published charts print both forms, such as "hit at −1 or lower". */
enum class IndexDirection { AT_OR_ABOVE, AT_OR_BELOW }

/**
 * An index, the fact a flashcard asks for: make the [deviation]'s play at a true count of [threshold] or higher, or at
 * [threshold] or lower for [IndexDirection.AT_OR_BELOW], and play basic strategy on the other side. The same deviation with
 * another threshold or direction, as under other rules or after an edit, is another fact.
 */
data class PlayIndex(
    val deviation: Deviation,
    val threshold: Int,
    val direction: IndexDirection = IndexDirection.AT_OR_ABOVE,
) : Serializable

/** An index in a preset, which is drilled while [enabled]. */
data class IndexEntry(val index: PlayIndex, val enabled: Boolean = true)
