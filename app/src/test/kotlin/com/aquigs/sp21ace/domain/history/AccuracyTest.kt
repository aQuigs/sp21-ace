package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AccuracyTest {
    // 08:00 in New York, where the day began at 04:00 UTC
    private val now = Instant.parse("2026-09-17T12:00:00Z")
    private val newYork = Clock.fixed(now, ZoneId.of("America/New_York"))

    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val softSeventeenVsTen = TrainerHand(cards("As 6d"), card("Kh"))
    private val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))

    private fun answer(right: Boolean = true, at: Instant = now, hand: TrainerHand = sixteenVsAce, correctMove: Move = Move.HIT) =
        PracticeAnswer(at, RuleSet.S17, hand, if (right) correctMove else Move.entries.first { it != correctMove }, correctMove)

    private fun List<PracticeAnswer>.figures(period: Period = Period.ALL_TIME, hands: HandFilter = HandFilter.ALL) = accuracy(period, hands, newYork)

    private fun streaks(rights: String) = rights.map { answer(right = it == 'R') }

    @Test
    fun todayStartsAtMidnightInTheClocksTimeZone() {
        assertEquals(Instant.parse("2026-09-17T04:00:00Z"), Period.TODAY.start(newYork))
        assertEquals(Instant.parse("2026-09-17T00:00:00Z"), Period.TODAY.start(Clock.fixed(now, ZoneOffset.UTC)))
    }

    @Test
    fun weekAndMonthReachBackSevenAndThirtyDaysAndAllTimeHasNoStart() {
        assertEquals(Instant.parse("2026-09-10T12:00:00Z"), Period.WEEK.start(newYork))
        assertEquals(Instant.parse("2026-08-18T12:00:00Z"), Period.MONTH.start(newYork))
        assertNull(Period.ALL_TIME.start(newYork))
    }

    @Test
    fun aWeekAcrossTheEndOfDaylightSavingReachesBackSevenDaysOnTheLocalClock() {
        // 07:00 on 3 November in New York, two days after the clocks went back, so a week earlier was 07:00 summer time
        val clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneId.of("America/New_York"))

        assertEquals(Instant.parse("2026-10-27T11:00:00Z"), Period.WEEK.start(clock))
    }

    @Test
    fun aPeriodCountsAnswersFromItsStartOn() {
        for (period in listOf(Period.TODAY, Period.WEEK, Period.MONTH)) {
            val start = requireNotNull(period.start(newYork))
            val history = listOf(answer(right = false, at = start.minusMillis(1)), answer(at = start))

            assertEquals(period.name, Tally(correct = 1, incorrect = 0), history.figures(period).overall)
        }

        assertEquals(Tally(correct = 1, incorrect = 0), listOf(answer(at = Instant.EPOCH)).figures(Period.ALL_TIME).overall)
    }

    @Test
    fun anAnswerStampedAfterNowCountsOnlyUnderAllTime() {
        // As when the device's clock was set ahead, then put back
        val history = listOf(answer(at = now), answer(right = false, at = now.plusMillis(1)))

        for (period in listOf(Period.TODAY, Period.WEEK, Period.MONTH)) {
            assertEquals(period.name, Tally(correct = 1, incorrect = 0), history.figures(period).overall)
        }
        assertEquals(Tally(correct = 1, incorrect = 1), history.figures(Period.ALL_TIME).overall)
    }

    @Test
    fun eachTabCountsOnlyItsKindOfHand() {
        val history = listOf(
            answer(hand = sixteenVsAce),
            answer(right = false, hand = softSeventeenVsTen),
            answer(hand = eightsVsSix, correctMove = Move.SPLIT),
            answer(hand = eightsVsSix, correctMove = Move.SPLIT),
        )

        assertEquals(
            mapOf(
                HandFilter.HARD to Tally(correct = 1, incorrect = 0),
                HandFilter.SOFT to Tally(correct = 0, incorrect = 1),
                HandFilter.PAIRS to Tally(correct = 2, incorrect = 0),
                HandFilter.ALL to Tally(correct = 3, incorrect = 1),
            ),
            HandFilter.entries.associateWith { history.figures(hands = it).overall },
        )
    }

    @Test
    fun eachMoveTalliesTheAnswersToHandsThatCalledForItWhateverTheAnswerAndOverallAddsThemUp() {
        val figures = listOf(
            answer(correctMove = Move.HIT),
            answer(right = false, correctMove = Move.HIT),
            answer(hand = eightsVsSix, correctMove = Move.SPLIT),
            answer(right = false, hand = TrainerHand(cards("Kc 7d"), card("As")), correctMove = Move.SURRENDER),
        ).figures()

        assertEquals(
            mapOf(
                Move.HIT to Tally(correct = 1, incorrect = 1),
                Move.STAND to Tally(correct = 0, incorrect = 0),
                Move.DOUBLE to Tally(correct = 0, incorrect = 0),
                Move.SPLIT to Tally(correct = 1, incorrect = 0),
                Move.SURRENDER to Tally(correct = 0, incorrect = 1),
            ),
            figures.byMove,
        )
        assertEquals(Tally(correct = 2, incorrect = 2), figures.overall)
    }

    @Test
    fun noAnswersHaveNoAccuracyButAllWrongOnesHaveNone() {
        val figures = emptyList<PracticeAnswer>().figures()

        assertEquals(Move.entries.associateWith { Tally(correct = 0, incorrect = 0) }, figures.byMove)
        assertNull(figures.overall.accuracyPermille)
        assertEquals(0, Tally(correct = 0, incorrect = 3).accuracyPermille)
    }

    @Test
    fun accuracyRoundsDownSoOnlyNoneWrongReadsAsAHundredPercent() {
        assertEquals(999, Tally(correct = 1999, incorrect = 1).accuracyPermille)
        assertEquals(1000, Tally(correct = 2000, incorrect = 0).accuracyPermille)
        assertEquals(666, Tally(correct = 2, incorrect = 1).accuracyPermille)
        // 29% is a shade under 0.29 as a double, which rounding down in floating point would read as 28.9%
        assertEquals(290, Tally(correct = 29, incorrect = 71).accuracyPermille)
    }

    @Test
    fun theLongestStreakIsTheLongestRunOfRightAnswersWhereverItFallsInThePeriod() {
        assertEquals(3, streaks("RRWRRRWR").figures().longestStreak)
        assertEquals(4, streaks("RRWRRRR").figures().longestStreak)
        assertEquals(0, streaks("WW").figures().longestStreak)
        assertEquals(0, emptyList<PracticeAnswer>().figures().longestStreak)

        val threeRightYesterday = List(3) { answer(at = now.minus(Duration.ofDays(1))) }
        assertEquals(2, (threeRightYesterday + streaks("WRR")).figures(Period.TODAY).longestStreak)
    }

    @Test
    fun eachTabHasACardForEveryMoveItsHandsCallForAndNoOther() {
        val deck = spanishShoe(decks = 1)
        // The same card twice included, so the suited 7-7 bonus exception is there too
        val hands = deck.flatMap { first -> deck.map { listOf(first, it) } }.filterNot { it.isBlackjack() }
        val upcards = deck.filter { it.suit == Suit.SPADES }
        val called = RuleSet.entries.map(StrategyCharts::forRules).flatMap { chart ->
            hands.flatMap { hand -> upcards.map { upcard -> chartRow(hand).table to chart.firstMove(hand, upcard) } }
        }.groupBy({ it.first }, { it.second })

        for (tab in HandFilter.entries) {
            val moves = tab.table?.let { called.getValue(it) } ?: called.values.flatten()
            assertEquals(tab.name, moves.toSet(), tab.moves.toSet())
        }
    }
}
