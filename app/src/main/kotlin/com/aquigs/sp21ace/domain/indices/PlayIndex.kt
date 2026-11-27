package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.totalOf
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

    /** What the hand adds up to, a pair included. */
    internal val playedTotal: HandTotal get() = if (kind == HandKind.PAIR) totalOf(listOf(value, value)) else total

    internal val pairCard: Upcard get() = Upcard.ofPoints(value)

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

/**
 * The plays an index on a hand of this kind can name. Only a pair splits, and no basic strategy chart or index table surrenders a
 * soft hand, which can always take a card without busting. A pair can be played as its total too, as in doubling a pair of 5s or
 * surrendering a pair of 8s.
 */
val HandKind.plays: List<DeviationPlay>
    get() = when (this) {
        HandKind.HARD -> listOf(DeviationPlay.HIT, DeviationPlay.STAND, DeviationPlay.DOUBLE, DeviationPlay.SURRENDER)
        HandKind.SOFT -> listOf(DeviationPlay.HIT, DeviationPlay.STAND, DeviationPlay.DOUBLE)
        HandKind.PAIR -> DeviationPlay.entries
    }

/** The numbers an index can be set to, past the furthest any published Hi-Lo index reaches. */
val INDEX_NUMBERS: IntRange = -10..10

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
            require(play in hand.kind.plays) { "No $play index on ${hand.label}" }
        }
    }

    /** Whether this asks about [other]'s total against its upcard, whichever play either names. A pair can be played as its total. */
    fun asksSameHandAs(other: OnHand): Boolean = this is OnHand && hand.playedTotal == other.hand.playedTotal && upcard == other.upcard

    /** Insurance is offered only against a dealer ace, and asks for no hand. */
    data object Insurance : Deviation {
        override val upcard: Upcard get() = Upcard.ACE
    }
}

/** Which side of its number an index makes its play on. Published charts print both forms, such as "hit at −1 or lower". */
enum class IndexDirection { AT_OR_ABOVE, AT_OR_BELOW }

/** What a flashcard asks for and its answer: the [threshold] of the play on [deviation], or null where there is no such index. */
sealed interface IndexFact : Serializable {
    val deviation: Deviation
    val direction: IndexDirection
    val threshold: Int?
}

/** That no index is defined for the [deviation]'s hand against its upcard, asked for as the index it passes for would be. */
data class NoIndex(override val deviation: Deviation.OnHand, override val direction: IndexDirection = IndexDirection.AT_OR_ABOVE) : IndexFact {
    override val threshold: Int? get() = null
}

/**
 * An index: make the [deviation]'s play at a true count of [threshold] or higher, or at
 * [threshold] or lower for [IndexDirection.AT_OR_BELOW], and play basic strategy on the other side. The same deviation with
 * another threshold or direction, as under other rules or after an edit, is another fact.
 */
data class PlayIndex(
    override val deviation: Deviation,
    override val threshold: Int,
    override val direction: IndexDirection = IndexDirection.AT_OR_ABOVE,
) : IndexFact

/** An index in a preset, which is drilled while [enabled]. */
data class IndexEntry(val index: PlayIndex, val enabled: Boolean = true)
