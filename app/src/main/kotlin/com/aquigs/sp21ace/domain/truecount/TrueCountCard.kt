package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.drill.AnswerPad
import com.aquigs.sp21ace.domain.drill.DrillCard
import com.aquigs.sp21ace.domain.indices.CountRounding
import java.time.Instant
import kotlin.time.Duration

/** Some decks, counted in [quarters] of one so every division is exact. */
@JvmInline
value class Decks(val quarters: Int) : Comparable<Decks> {
    /** "6", "3.5" or "2.25". */
    val label: String
        get() = "${quarters / QUARTERS_PER_DECK}" + when (quarters % QUARTERS_PER_DECK) {
            0 -> ""
            1 -> ".25"
            2 -> ".5"
            else -> ".75"
        }

    override fun compareTo(other: Decks): Int = quarters.compareTo(other.quarters)

    operator fun minus(other: Decks): Decks = Decks(quarters - other.quarters)

    companion object {
        const val QUARTERS_PER_DECK = 4

        fun whole(decks: Int): Decks = Decks(decks * QUARTERS_PER_DECK)
    }
}

/** The true counts a card can be answered with, the ends standing for every count past them too. */
val TRUE_COUNT_PAD = AnswerPad(-6..6, openEnded = true)

/** A [runningCount] to convert with [decksLeft] of a shoe of [shoe] decks, the fraction going as [rounding] says. */
data class TrueCountCard(val shoe: Int, val decksLeft: Decks, val rounding: CountRounding, val runningCount: Int) : DrillCard {
    val decksDiscarded: Decks get() = Decks.whole(shoe) - decksLeft

    val trueCount: Int get() = rounding.divide(runningCount * Decks.QUARTERS_PER_DECK, decksLeft.quarters)

    override val answer: Int get() = TRUE_COUNT_PAD.answer(trueCount)
}

/** A true count answer as the history keeps it. [time] is how long the card was on screen before it, or null when interrupted. */
data class TrueCountAnswer(val answeredAt: Instant, val card: TrueCountCard, val answer: Int, val time: Duration?) {
    init {
        require(time == null || (!time.isNegative() && time.isFinite())) { "An answer time out of range: $time" }
        require(card.shoe in SHOE_SIZES) { "A shoe of ${card.shoe} decks" }
        require(card.decksLeft.quarters in 1..Decks.whole(card.shoe).quarters) { "${card.decksLeft.label} decks left of ${card.shoe}" }
    }

    val isCorrect: Boolean get() = answer == card.answer
}
