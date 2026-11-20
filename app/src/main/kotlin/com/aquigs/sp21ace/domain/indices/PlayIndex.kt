package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.pairRow
import com.aquigs.sp21ace.domain.strategy.row
import java.io.Serializable

/** The game an index is for. Spanish 21 joins with its own indices. */
enum class Game { BLACKJACK }

/**
 * The totals two cards make without a pair, so every index can be dealt as two cards: hard 4, hard 20 and soft 12 are only ever
 * pairs. A pair's value is its card's, an ace 1.
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

    /** The strategy chart row basic strategy plays the hand from. */
    val row: ChartRow get() = if (kind == HandKind.PAIR) pairRow(pairCard) else total.row

    /** The hand as the strategy charts key their rows: "16" for hard 16, "A-7" for soft 18, "8-8" and "A-A" for pairs. */
    val label: String get() = row.hand

    internal val total: HandTotal get() = HandTotal(value, soft = kind == HandKind.SOFT)

    internal val pairCard: Upcard get() = if (value == 1) Upcard.ACE else Upcard.fromLabel("$value")

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

        private fun cardValue(label: String) = if (label == "A") 1 else requireNotNull(label.toIntOrNull()) { "Unknown card: $label" }
    }
}

/** HIT is for indices that leave a stand or double as the count falls, as published charts print "hit at −1 or lower". */
enum class DeviationPlay { HIT, STAND, DOUBLE, SPLIT, SURRENDER }

/** The question an index answers: when to make a play basic strategy doesn't, a hand's play against an upcard, or insurance. */
sealed interface Deviation : Serializable {
    val upcard: Upcard

    companion object {
        internal const val INSURANCE = "INSURANCE"
        private const val NO_HAND = "-"

        /** The deviation named by its play, hand label and upcard label, with insurance as "INSURANCE", "-" and "A". */
        internal fun parse(play: String, hand: String, upcard: String): Deviation = if (play == INSURANCE) {
            require(hand == NO_HAND && upcard == Upcard.ACE.label) { "Insurance has no hand and a dealer ace: $hand vs $upcard" }
            Insurance
        } else {
            OnHand(IndexHand.parse(hand), Upcard.fromLabel(upcard), DeviationPlay.valueOf(play))
        }
    }

    data class OnHand(val hand: IndexHand, override val upcard: Upcard, val play: DeviationPlay) : Deviation {
        init {
            require(play != DeviationPlay.SPLIT || hand.kind == HandKind.PAIR) { "Only a pair splits: ${hand.label}" }
        }
    }

    /** Insurance is offered only against a dealer ace, and asks for no hand. */
    data object Insurance : Deviation {
        override val upcard: Upcard get() = Upcard.ACE
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
