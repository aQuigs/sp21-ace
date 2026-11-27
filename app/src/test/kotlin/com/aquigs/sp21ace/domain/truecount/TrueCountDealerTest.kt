package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.drill.DrillRun
import com.aquigs.sp21ace.domain.drill.Streak
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.indices.CountRounding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class TrueCountDealerTest {
    private val random = Random(21)
    private val at = Instant.ofEpochMilli(1_789_000_000_000)

    private fun dealt(options: TrueCountOptions, cards: Int = 4_000, history: List<TrueCountAnswer> = emptyList()): List<TrueCountCard> {
        val dealer = TrueCountDealer(options)
        return generateSequence(dealer.deal(null, history, random)) { dealer.deal(it, history, random) }.take(cards).toList()
    }

    private val everyOptions = SHOE_SIZES.flatMap { shoe -> DeckPrecision.entries.map { TrueCountOptions(shoe, it) } }

    @Test
    fun dealsEveryStepFromAFullShoeToHalfADeckOrOneWhole() {
        fun labels(options: TrueCountOptions) = dealt(options).map { it.decksLeft }.distinct().sortedDescending().map { it.label }

        assertEquals(listOf("2", "1.5", "1", "0.5"), labels(TrueCountOptions(shoe = 2)))
        assertEquals(listOf("6", "5", "4", "3", "2", "1"), labels(TrueCountOptions(precision = DeckPrecision.WHOLE)))
        assertEquals(listOf("1", "0.75", "0.5"), labels(TrueCountOptions(shoe = 1, precision = DeckPrecision.QUARTER)))
        assertEquals(listOf("1"), labels(TrueCountOptions(shoe = 1, precision = DeckPrecision.WHOLE)))
    }

    @Test
    fun theRunningCountIsNeverZeroNorPastWhatTheCardsDealtCouldMake() {
        for (options in everyOptions) {
            for (card in dealt(options, 2_000)) {
                val dealtAtMost = (card.decksDiscarded.quarters + options.precision.step.quarters / 2.0) / 4

                assertNotEquals("$card", 0, card.runningCount)
                assertTrue("$card", abs(card.runningCount) <= 10 * dealtAtMost)
                assertTrue("$card", abs(card.runningCount) * 4 <= 8 * card.decksLeft.quarters)
            }
        }
    }

    @Test
    fun everyAnswerOnThePadComesUpUnderEveryRounding() {
        for (rounding in CountRounding.entries) {
            assertEquals(TRUE_COUNT_PAD.numbers.toSet(), dealt(TrueCountOptions(rounding = rounding)).map { it.answer }.toSet())
        }
    }

    @Test
    fun noCardComesTwiceInARowNorTheSameDecksWhileThereAreMoreThanTwo() {
        for (options in everyOptions) {
            val cards = dealt(options, 500)
            val few = cards.distinctBy { it.decksLeft }.size <= 2

            cards.zipWithNext { previous, next ->
                assertNotEquals(previous, next)
                if (!few) assertNotEquals(previous.decksLeft, next.decksLeft)
            }
        }
    }

    @Test
    fun withOnlyTwoDecksToDealTheyDontComeInTurnAndTheWorseStillComesMoreOften() {
        val options = TrueCountOptions(shoe = 2, precision = DeckPrecision.WHOLE)
        assertTrue(dealt(options, 500).zipWithNext().any { (previous, next) -> previous.decksLeft == next.decksLeft })

        val card = TrueCountCard(2, Decks.whole(1), CountRounding.FLOOR, 3)
        val wrong = List(20) { TrueCountAnswer(at, card, 1, null) }
        val right = List(20) { TrueCountAnswer(at, card.copy(decksLeft = Decks.whole(2)), 1, null) }
        val counts = dealt(options.copy(cardsShown = CardsShown.PRIORITIZE_WORSE), 2_000, wrong + right).groupingBy { it.decksLeft }.eachCount()

        assertTrue("$counts", counts.getValue(Decks.whole(1)) > 3 * counts.getValue(Decks.whole(2)))
    }

    @Test
    fun dealsOnlyCardsItCouldDealAndNoneOfAnotherShoePrecisionOrRounding() {
        val options = TrueCountOptions()
        val dealer = TrueCountDealer(options)

        assertTrue(dealt(options, 500).all(dealer::couldDeal))
        assertFalse(dealer.couldDeal(TrueCountCard(8, Decks(14), CountRounding.FLOOR, 7)))
        assertFalse(dealer.couldDeal(TrueCountCard(6, Decks(13), CountRounding.FLOOR, 7)))
        assertFalse(dealer.couldDeal(TrueCountCard(6, Decks(14), CountRounding.ROUND, 7)))
        assertFalse(dealer.couldDeal(TrueCountCard(6, Decks(14), CountRounding.FLOOR, 0)))
        assertFalse(dealer.couldDeal(TrueCountCard(6, Decks(14), CountRounding.FLOOR, 40)))
    }

    @Test
    fun changingWhatTheCardShowsKeepsItAndChangingTheRoundingRedealsIt() {
        val run = DrillRun(TrueCountCard(6, Decks(14), CountRounding.FLOOR, 7), streak = Streak(3))

        assertEquals(run, run.dealtFrom(TrueCountDealer(TrueCountOptions(decksShown = DecksShown.DISCARDED))))

        val redealt = run.dealtFrom(TrueCountDealer(TrueCountOptions(rounding = CountRounding.TRUNCATE)))
        assertEquals(CountRounding.TRUNCATE to Streak(3), redealt.card?.rounding to redealt.streak)
    }

    @Test
    fun recordsTheCardAsAsked() {
        val card = TrueCountCard(6, Decks(14), CountRounding.FLOOR, 7)
        val dealer = TrueCountDealer(TrueCountOptions())
        val answered = requireNotNull(DrillRun(card).answer(card, 1, dealer, 900.milliseconds, at))

        assertEquals(TrueCountAnswer(at, card, 1, 900.milliseconds), answered.answer)
        assertFalse(answered.answer.isCorrect)
        assertNotEquals(card, answered.run.card)
    }

    @Test
    fun theGradeTalliesEveryAnswerWithItsDecksLeftOfItsShoeThisOneIncluded() {
        val card = TrueCountCard(6, Decks(14), CountRounding.FLOOR, 7)
        fun earlier(of: TrueCountCard, number: Int = of.answer) = TrueCountAnswer(at, of, number, null)
        val history = listOf(
            earlier(card.copy(runningCount = -3)),
            earlier(card, 5),
            // Other decks left and another shoe don't count
            earlier(card.copy(decksLeft = Decks(8))),
            earlier(card.copy(shoe = 8)),
        )
        fun tally(history: List<TrueCountAnswer>?) =
            DrillRun(card).answer(card, 2, TrueCountDealer(TrueCountOptions()), null, at, history)?.run?.lastGrade?.tally

        assertEquals(Tally(correct = 2, incorrect = 1), tally(history))
        assertNull(tally(null))
    }

    @Test
    fun prioritizeWorseDealsTheDecksAnsweredWorseMoreOftenCountingOnlyThisShoe() {
        fun answers(shoe: Int, decks: Decks, right: Boolean) = List(20) {
            val card = TrueCountCard(shoe, decks, CountRounding.FLOOR, 3)
            TrueCountAnswer(at, card, if (right) card.answer else card.answer + 1, null)
        }

        val options = TrueCountOptions(shoe = 4, precision = DeckPrecision.WHOLE, cardsShown = CardsShown.PRIORITIZE_WORSE)
        // Always right with 1, 2 and 4 decks left of 4, always wrong with 3, and another shoe the other way around
        val history = listOf(1, 2, 4).flatMap { answers(4, Decks.whole(it), right = true) } + answers(4, Decks.whole(3), right = false) +
            answers(6, Decks.whole(1), right = false)
        val counts = dealt(options, 4_000, history).groupingBy { it.decksLeft }.eachCount()

        // Never twice in a row, so at most every other card
        assertTrue("$counts", counts.getValue(Decks.whole(3)) > 1_600)
        assertTrue("$counts", counts.getValue(Decks.whole(1)) < 1_000)
    }
}
