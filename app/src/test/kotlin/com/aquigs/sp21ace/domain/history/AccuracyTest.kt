package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.afterDoublingRow
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.correctMoveAfterDoubling
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AccuracyTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")

    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val softSeventeenVsTen = TrainerHand(cards("As 6d"), card("Kh"))
    private val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))

    private fun answer(right: Boolean = true, at: Instant = now, hand: TrainerHand = sixteenVsAce, correctMove: Move = Move.HIT, rules: RuleSet = RuleSet.S17) =
        PracticeAnswer(at, rules, hand, if (right) correctMove else Move.entries.first { it != correctMove }, correctMove)

    private fun List<PracticeAnswer>.figures(period: Period = Period.ALL_TIME, hands: HandFilter = HandFilter.ALL, at: Instant = now) =
        accuracy(period, hands, at)

    private fun streaks(rights: String) = rights.map { answer(right = it == 'R') }

    private fun square(table: ChartTable, hand: String, upcard: Upcard) = ChartSquare(ChartRow(table, hand), upcard)

    private val sixteenVsAceSquare = square(ChartTable.HARD, "16", Upcard.ACE)

    @Test
    fun todayWeekAndMonthReachBack24HoursSevenDaysAnd28DaysAndAllTimeHasNoStart() {
        assertEquals(Instant.parse("2026-09-16T12:00:00Z"), Period.TODAY.start(now))
        assertEquals(Instant.parse("2026-09-10T12:00:00Z"), Period.WEEK.start(now))
        assertEquals(Instant.parse("2026-08-20T12:00:00Z"), Period.MONTH.start(now))
        assertNull(Period.ALL_TIME.start(now))
    }

    @Test
    fun aPeriodCountsAnswersNewerThanWhereItReachesBackTo() {
        for (period in listOf(Period.TODAY, Period.WEEK, Period.MONTH)) {
            val start = requireNotNull(period.start(now))
            val history = listOf(answer(right = false, at = start), answer(at = start.plusMillis(1)))

            assertEquals(period.name, Tally(correct = 1, incorrect = 0), history.figures(period).overall)
        }

        assertEquals(Tally(correct = 1, incorrect = 0), listOf(answer(at = Instant.EPOCH)).figures(Period.ALL_TIME).overall)
    }

    @Test
    fun anAnswerStaysInTodayPastMidnightAndLeavesIt24HoursOn() {
        val lateAtNight = Instant.parse("2026-09-17T23:00:00Z")
        val history = listOf(answer(at = lateAtNight))

        assertEquals(1, history.figures(Period.TODAY, at = Instant.parse("2026-09-18T08:00:00Z")).overall.total)
        assertEquals(0, history.figures(Period.TODAY, at = lateAtNight.plus(Duration.ofHours(24))).overall.total)
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
            // Doubled hard 16 vs A, and soft 18 vs 4 with redoubling
            answer(right = false, hand = TrainerHand(cards("5c 6d 5h"), card("As"), doubles = 1), correctMove = Move.RESCUE),
            answer(hand = TrainerHand(cards("As 5d 2c"), card("4h"), doubles = 1), correctMove = Move.REDOUBLE, rules = RuleSet.H17_REDOUBLE),
        )

        assertEquals(
            mapOf(
                HandFilter.HARD to Tally(correct = 1, incorrect = 0),
                HandFilter.SOFT to Tally(correct = 0, incorrect = 1),
                HandFilter.PAIRS to Tally(correct = 2, incorrect = 0),
                HandFilter.AFTER_DOUBLE_HARD to Tally(correct = 0, incorrect = 1),
                HandFilter.AFTER_DOUBLE_SOFT to Tally(correct = 1, incorrect = 0),
                HandFilter.ALL to Tally(correct = 4, incorrect = 2),
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
                Move.REDOUBLE to Tally(correct = 0, incorrect = 0),
                Move.RESCUE to Tally(correct = 0, incorrect = 0),
            ),
            figures.byMove,
        )
        assertEquals(Tally(correct = 2, incorrect = 2), figures.overall)
    }

    @Test
    fun eachSquareTalliesTheAnswersToEveryHandReadFromItAgainstItsUpcard() {
        val figures = listOf(
            answer(hand = sixteenVsAce),
            answer(right = false, hand = TrainerHand(cards("Kd 6h"), card("Ac"))),
            answer(hand = TrainerHand(cards("9c 4d 3s"), card("Ad"))),
            answer(hand = TrainerHand(cards("9c 7d"), card("Qs"))),
            answer(right = false, hand = softSeventeenVsTen),
        ).figures()

        assertEquals(
            mapOf(
                sixteenVsAceSquare to Tally(correct = 2, incorrect = 1),
                square(ChartTable.HARD, "16", Upcard.TEN) to Tally(correct = 1, incorrect = 0),
                square(ChartTable.SOFT, "A-6", Upcard.TEN) to Tally(correct = 0, incorrect = 1),
            ),
            figures.bySquare,
        )
    }

    @Test
    fun theSquaresCountOnlyThePeriodsAnswersToTheTabsKindOfHand() {
        val history = listOf(
            answer(at = now.minus(Duration.ofDays(3))),
            answer(right = false),
            answer(hand = softSeventeenVsTen),
            answer(hand = eightsVsSix, correctMove = Move.SPLIT),
        )

        assertEquals(mapOf(sixteenVsAceSquare to Tally(correct = 0, incorrect = 1)), history.figures(Period.TODAY, HandFilter.HARD).bySquare)
        assertEquals(mapOf(sixteenVsAceSquare to Tally(correct = 1, incorrect = 1)), history.figures(Period.WEEK, HandFilter.HARD).bySquare)
        assertEquals(mapOf(square(ChartTable.SOFT, "A-6", Upcard.TEN) to Tally(correct = 1, incorrect = 0)), history.figures(Period.WEEK, HandFilter.SOFT).bySquare)
    }

    @Test
    fun pairsLandInThePairsGridRatherThanUnderTheirTotal() {
        val history = listOf(
            answer(hand = eightsVsSix, correctMove = Move.SPLIT),
            answer(right = false, hand = TrainerHand(cards("Kc Qd"), card("6h")), correctMove = Move.STAND),
            answer(hand = TrainerHand(cards("Ah Ac"), card("6s")), correctMove = Move.SPLIT),
        )

        assertEquals(
            mapOf(
                square(ChartTable.PAIRS, "8-8", Upcard.SIX) to Tally(correct = 1, incorrect = 0),
                square(ChartTable.PAIRS, "10-10", Upcard.SIX) to Tally(correct = 0, incorrect = 1),
                square(ChartTable.PAIRS, "A-A", Upcard.SIX) to Tally(correct = 1, incorrect = 0),
            ),
            history.figures(hands = HandFilter.PAIRS).bySquare,
        )
        assertEquals(emptyMap<ChartSquare, Tally>(), history.figures(hands = HandFilter.HARD).bySquare)
        assertEquals(emptyMap<ChartSquare, Tally>(), history.figures(hands = HandFilter.SOFT).bySquare)
    }

    @Test
    fun aDoubledHandLandsInTheAfterDoublingSquaresRatherThanUnderItsTotal() {
        val fourteenVsNine = TrainerHand(cards("5c 6d 3h"), card("9s"))
        val history = listOf(
            answer(right = false, hand = fourteenVsNine.copy(doubles = 1), correctMove = Move.RESCUE),
            answer(hand = TrainerHand(cards("As 5d 2c"), card("4h"), doubles = 1), correctMove = Move.REDOUBLE, rules = RuleSet.H17_REDOUBLE),
            answer(hand = fourteenVsNine, correctMove = Move.HIT),
        )

        assertEquals(
            mapOf(
                square(ChartTable.AFTER_DOUBLE_HARD, "14", Upcard.NINE) to Tally(correct = 0, incorrect = 1),
                square(ChartTable.AFTER_DOUBLE_SOFT, "A-7", Upcard.FOUR) to Tally(correct = 1, incorrect = 0),
                square(ChartTable.HARD, "14", Upcard.NINE) to Tally(correct = 1, incorrect = 0),
            ),
            history.figures().bySquare,
        )
        assertEquals(mapOf(square(ChartTable.HARD, "14", Upcard.NINE) to Tally(correct = 1, incorrect = 0)), history.figures(hands = HandFilter.HARD).bySquare)
        assertEquals(emptyMap<ChartSquare, Tally>(), history.figures(hands = HandFilter.SOFT).bySquare)
        assertEquals(Tally(correct = 0, incorrect = 1), history.figures(hands = HandFilter.AFTER_DOUBLE_HARD).overall)
        assertEquals(Tally(correct = 1, incorrect = 0), history.figures(hands = HandFilter.AFTER_DOUBLE_SOFT).byMove.getValue(Move.REDOUBLE))
    }

    @Test
    fun aRedoubleAndARescueHaveCardsOfTheirOwnApartFromADoubleAndASurrender() {
        val history = listOf(
            answer(hand = TrainerHand(cards("5c 6d"), card("5s")), correctMove = Move.DOUBLE),
            answer(right = false, hand = TrainerHand(cards("As 5d 2c"), card("4h"), doubles = 1), correctMove = Move.REDOUBLE, rules = RuleSet.H17_REDOUBLE),
            answer(hand = TrainerHand(cards("5c 6d 3h"), card("9s"), doubles = 1), correctMove = Move.RESCUE),
            // A stand is a stand, doubled or not
            answer(hand = TrainerHand(cards("5c 6d 9h"), card("4s"), doubles = 1), correctMove = Move.STAND),
            answer(hand = TrainerHand(cards("Kc 8d"), card("6h")), correctMove = Move.STAND),
        )

        val byMove = history.figures().byMove
        assertEquals(Tally(correct = 1, incorrect = 0), byMove.getValue(Move.DOUBLE))
        assertEquals(Tally(correct = 0, incorrect = 1), byMove.getValue(Move.REDOUBLE))
        assertEquals(Tally(correct = 1, incorrect = 0), byMove.getValue(Move.RESCUE))
        assertEquals(Tally(correct = 0, incorrect = 0), byMove.getValue(Move.SURRENDER))
        assertEquals(Tally(correct = 2, incorrect = 0), byMove.getValue(Move.STAND))
    }

    @Test
    fun anAnswerCountsInItsSquareWhateverRulesGradedIt() {
        // Hard 16 vs A is a hit when the dealer stands on soft 17, and a surrender when the dealer hits
        val history = listOf(answer(), answer(rules = RuleSet.H17, correctMove = Move.SURRENDER))

        assertEquals(mapOf(sixteenVsAceSquare to Tally(correct = 2, incorrect = 0)), history.figures().bySquare)
    }

    @Test
    fun aCounterTalliesEachKeysRightAndWrongAnswersAndHoldsOnlyTheKeysAdded() {
        val counter = TallyCounter<String>()
        counter.add("a", isCorrect = true)
        counter.add("b", isCorrect = false)
        counter.add("a", isCorrect = false)
        counter.add("a", isCorrect = true)

        assertEquals(mapOf("a" to Tally(correct = 2, incorrect = 1), "b" to Tally(correct = 0, incorrect = 1)), counter.toMap())
        assertEquals(emptyMap<String, Tally>(), TallyCounter<String>().toMap())
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
    fun theLongestStreakRunsOverEveryAnswerWhateverThePeriodAndTab() {
        assertEquals(3, streaks("RRWRRRWR").figures().longestStreak)
        assertEquals(4, streaks("RRWRRRR").figures().longestStreak)
        assertEquals(0, streaks("WW").figures().longestStreak)
        assertEquals(0, emptyList<PracticeAnswer>().figures().longestStreak)

        // Three right last month, then a wrong soft hand and two right hard ones today
        val history = List(3) { answer(at = now.minus(Duration.ofDays(40))) } + answer(right = false, hand = softSeventeenVsTen) + streaks("RR")
        assertEquals(3, history.figures(Period.TODAY, HandFilter.HARD).longestStreak)
    }

    @Test
    fun eachTabHasACardForEveryMoveItsHandsCallForInBlackjackAcesOrder() {
        val deck = spanishShoe(decks = 1)
        // The same card twice included, so the suited 7-7 bonus exception is there too
        val hands = deck.flatMap { first -> deck.map { listOf(first, it) } }.filterNot { it.isBlackjack() }
        val upcards = deck.filter { it.suit == Suit.SPADES }
        // Every total a doubled hand can have, from hard 6 and soft 13 up
        val doubled = (6..20).map { HandTotal(it, soft = false) } + (13..20).map { HandTotal(it, soft = true) }
        val called = RuleSet.entries.map(StrategyCharts::forRules).flatMap { chart ->
            hands.flatMap { hand -> upcards.map { upcard -> chartRow(hand).table to chart.correctMove(hand, upcard) } } +
                doubled.flatMap { total -> upcards.map { total.afterDoublingRow.table to chart.correctMoveAfterDoubling(total, it.upcard) } }
        }.groupBy({ it.first }, { it.second })

        for (tab in HandFilter.entries) {
            val moves = (tab.table?.let { called.getValue(it) } ?: called.values.flatten()).toSet()
            val order = listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.REDOUBLE, Move.STAND, Move.SURRENDER, Move.RESCUE)
            assertEquals(tab.name, order.filter { it in moves }, tab.moves)
        }
    }
}
