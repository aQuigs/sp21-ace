package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.drill.CardDealer
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.TallyCounter
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration

// Half a deck, past which no shoe is dealt
private val FEWEST_LEFT = Decks(2)

// Two past the pad's ends, so they come up without taking over
private const val HIGHEST_TRUE_COUNT = 8

// With this few decks to deal, barring the last ones would deal them in turn, whatever was answered
private const val ALWAYS_DEALT = 2

// Half the 20 a Hi-Lo running count could reach on a deck, so a card never shows a count the cards dealt could hardly make
private const val HIGHEST_PER_DECK_DEALT = 10

/**
 * Deals the true count flashcards [options] ask for: decks left from the full shoe down to half a deck, or one when decks are
 * whole, and a running count that could have come up by then. Never a running count of 0, which is 0 with any decks left, and
 * never the same card twice in a row, nor the same decks while there are more than two. Under [CardsShown.PRIORITIZE_WORSE], the decks left come up in
 * inverse proportion to their accuracy with this shoe.
 */
class TrueCountDealer(private val options: TrueCountOptions) : CardDealer<TrueCountCard, Int, TrueCountAnswer> {
    private val shoe = Decks.whole(options.shoe)
    private val step = options.precision.step

    private val cardsByDecks: Map<Decks, List<TrueCountCard>> = (shoe.quarters downTo 1 step step.quarters).map(::Decks)
        .filter { it >= FEWEST_LEFT }
        .associateWith { left ->
            val highest = highestCount(left)
            (-highest..highest).filter { it != 0 }.map { TrueCountCard(options.shoe, left, options.rounding, it) }
        }

    // What is given as dealt may be up to half a step short of what was, so even a full shoe has a count
    private fun highestCount(left: Decks): Int {
        val mostDealtEighths = (shoe - left).quarters * 2 + step.quarters
        val fromDealt = HIGHEST_PER_DECK_DEALT * mostDealtEighths / (Decks.QUARTERS_PER_DECK * 2)
        return minOf(HIGHEST_TRUE_COUNT * left.quarters / Decks.QUARTERS_PER_DECK, fromDealt)
    }

    override fun deal(previous: TrueCountCard?, history: List<TrueCountAnswer>, random: Random): TrueCountCard {
        val candidates = cardsByDecks.keys.filter { cardsByDecks.size <= ALWAYS_DEALT || it != previous?.decksLeft }
        val left = options.cardsShown.pick(candidates, random) { tallyByDecks(history) }

        return cardsByDecks.getValue(left).filter { it != previous }.random(random)
    }

    override fun couldDeal(card: TrueCountCard): Boolean = card in cardsByDecks[card.decksLeft].orEmpty()

    override fun record(card: TrueCountCard, answer: Int, time: Duration?, answeredAt: Instant): TrueCountAnswer =
        TrueCountAnswer(answeredAt, card, answer, time)

    /** What was answered with [card]'s decks left, which is also what Prioritize worse weighs. */
    override fun tally(card: TrueCountCard, history: List<TrueCountAnswer>): Tally =
        tallyByDecks(history)[card.decksLeft] ?: Tally(correct = 0, incorrect = 0)

    private fun tallyByDecks(history: List<TrueCountAnswer>) = TallyCounter<Decks>().also { tallies ->
        for (answer in history) if (answer.card.shoe == options.shoe) tallies.add(answer.card.decksLeft, answer.isCorrect)
    }.toMap()
}
