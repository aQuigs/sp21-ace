package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.TallyCounter

/** Which index a flashcard asks for next: any of them as often as the next, or the worse answered more often. */
enum class CardsShown { RANDOM, PRIORITIZE_WORSE }

/**
 * How the flashcards are dealt, whatever the preset. [multiCardHands] shows stand and hit indices as hands of 3 or 4 cards about
 * half the time. It starts off, since published index tables give each number by hand total without saying how many cards make
 * it.
 */
data class FlashcardOptions(val cardsShown: CardsShown = CardsShown.RANDOM, val multiCardHands: Boolean = false)

/**
 * Every answer given to each of [game]'s indices. Keyed on the index itself, number and direction included, so the same fact
 * shares its record across presets while an edited number starts a record of its own.
 */
fun List<FlashcardAnswer>.tallyByIndex(game: Game): Map<PlayIndex, Tally> {
    val tallies = TallyCounter<PlayIndex>()
    for (answer in this) if (answer.game == game) tallies.add(answer.index, answer.isCorrect)
    return tallies.toMap()
}
