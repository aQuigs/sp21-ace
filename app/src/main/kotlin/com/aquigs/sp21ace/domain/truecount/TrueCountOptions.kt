package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.indices.CountRounding

/** The shoes a true count drill can be set to, in decks. */
val SHOE_SIZES: List<Int> = listOf(1, 2, 4, 6, 8)

/** How finely a card gives the decks: in steps of [step]. */
enum class DeckPrecision(val step: Decks) {
    WHOLE(Decks(4)),
    HALF(Decks(2)),
    QUARTER(Decks(1)),
}

/** Whether a card gives the decks left to divide by, or those in the discard tray to subtract from the shoe first. */
enum class DecksShown { LEFT, DISCARDED }

/** What the true count flashcards ask. [rounding] is the drill's own, whatever the index presets use. */
data class TrueCountOptions(
    val shoe: Int = 6,
    val precision: DeckPrecision = DeckPrecision.HALF,
    val rounding: CountRounding = CountRounding.DEFAULT,
    val decksShown: DecksShown = DecksShown.LEFT,
    val cardsShown: CardsShown = CardsShown.RANDOM,
)
