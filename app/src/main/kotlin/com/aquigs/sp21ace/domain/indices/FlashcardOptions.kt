package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.TallyCounter

/**
 * How the flashcards are dealt, whatever the preset. [noIndexCards] throws in the odd hand that has no index. [multiCardHands]
 * starts off, since published index tables give each number by hand total without saying how many cards make it.
 */
data class FlashcardOptions(
    val cardsShown: CardsShown = CardsShown.RANDOM,
    val noIndexCards: Boolean = false,
    val multiCardHands: Boolean = false,
)

/**
 * Every answer given to each of [game]'s facts. An index's number and direction are part of it, so the same fact shares its record
 * across presets while an edited number starts a record of its own.
 */
fun List<FlashcardAnswer>.tallyByFact(game: Game): Map<IndexFact, Tally> {
    val tallies = TallyCounter<IndexFact>()
    for (answer in this) if (answer.game == game) tallies.add(answer.fact, answer.isCorrect)
    return tallies.toMap()
}
