package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.Round
import com.aquigs.sp21ace.domain.game.STARTING_BANKROLL
import com.aquigs.sp21ace.domain.game.Shoe
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class PlayStatsTest {
    private val now = Instant.parse("2026-09-20T12:00:00Z")

    private fun hand(net: Long, hoursAgo: Long = 0, grade: StrategyGrade = StrategyGrade.CORRECT) = PlayedHand(
        now.minus(Duration.ofHours(hoursAgo)),
        RuleSet.S17,
        when {
            net > 0 -> Outcome.WIN
            net < 0 -> Outcome.LOSE
            else -> Outcome.PUSH
        },
        net,
        grade,
    )

    @Test
    fun aSettledRoundsHandsArePlayedApartWithTheirResults() {
        // 8-8 vs 6 splits, each 8 draws a K to stand on, and the dealer's 6-K draws a 2 for 18
        val round = Round.deal(RuleSet.S17, 2_500, STARTING_BANKROLL, Shoe(cards("8c 6s 8d Kh Ks Kd 2h")))
        val settled = requireNotNull(round.play(Move.SPLIT)?.play(Move.STAND)?.play(Move.STAND))

        assertEquals(
            listOf(PlayedHand(now, RuleSet.S17, Outcome.PUSH, 0, StrategyGrade.NO_ACTION_REQUIRED)).let { it + it },
            settled.playedHands(now),
        )
    }

    @Test
    fun countsTheHandsAndWhatTheyWonAndLost() {
        val stats = listOf(hand(2_500), hand(0), hand(-5_000), hand(3_750), hand(-1_250)).playStats(Period.ALL_TIME, now)

        assertEquals(listOf(2, 1, 2), listOf(stats.won, stats.pushed, stats.lost))
        assertEquals(5, stats.hands)
        assertEquals(6_250, stats.amountWon)
        assertEquals(6_250, stats.amountLost)
        assertEquals(0, stats.profit)
        assertEquals(listOf(0L, 2_500, 2_500, -2_500, 1_250, 0), stats.profits)
    }

    @Test
    fun countsEachStrategyGradeWithNoneLeftOut() {
        val stats = listOf(hand(2_500, grade = StrategyGrade.INCORRECT), hand(-2_500, grade = StrategyGrade.INCORRECT), hand(0))
            .playStats(Period.ALL_TIME, now)

        assertEquals(
            mapOf(
                StrategyGrade.CORRECT to 1,
                StrategyGrade.CORRECT_WITH_HINTS to 0,
                StrategyGrade.INCORRECT to 2,
                StrategyGrade.NO_ACTION_REQUIRED to 0,
            ),
            stats.grades,
        )
    }

    @Test
    fun aPeriodCountsOnlyTheHandsWithinItAsAccuracyDoes() {
        val history = listOf(hand(2_500, hoursAgo = 30), hand(-500, hoursAgo = 2), hand(1_000, hoursAgo = -1))

        assertEquals(listOf(0L, -500), history.playStats(Period.TODAY, now).profits)
        assertEquals(listOf(0L, 2_500, 2_000), history.playStats(Period.WEEK, now).profits)
        // One stamped ahead of the clock counts only under All Time
        assertEquals(3, history.playStats(Period.ALL_TIME, now).hands)
    }

    @Test
    fun nothingPlayedIsAllZeroesFromTheStart() {
        val stats = emptyList<PlayedHand>().playStats(Period.TODAY, now)

        assertEquals(0, stats.hands)
        assertEquals(listOf(0L), stats.profits)
        assertEquals(StrategyGrade.entries.associateWith { 0 }, stats.grades)
    }
}
