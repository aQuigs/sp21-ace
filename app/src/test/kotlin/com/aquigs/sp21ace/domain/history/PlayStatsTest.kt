package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.Round
import com.aquigs.sp21ace.domain.game.STARTING_BANKROLL
import com.aquigs.sp21ace.domain.game.Shoe
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

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
        // 8-8 vs 6 splits, each 8 draws a K to stand on, as the chart does, and the dealer's 6-K draws a 2 for 18
        val round = Round.deal(RuleSet.S17, 2_500, STARTING_BANKROLL, Shoe(cards("8c 6s 8d Kh Ks Kd 2h")))
        val settled = requireNotNull(round.play(Move.SPLIT)?.play(Move.STAND)?.nextHand()?.play(Move.STAND))

        assertEquals(
            listOf(PlayedHand(now, RuleSet.S17, Outcome.PUSH, 0, StrategyGrade.CORRECT)).let { it + it },
            settled.playedHands(now),
        )
    }

    @Test
    fun aTablesRoundIsPlayedOnceAsItSettlesByAMoveOrOnTheDeal() {
        // 16 vs 6 stands, and the dealer's 6-K draws a Q to bust
        val betting = requireNotNull(Table(STARTING_BANKROLL, Shoe(cards("Kc 6s 6d Kh Qs"))).addChip(2_500))
        val dealt = requireNotNull(betting.deal(RuleSet.S17, Random(1)))
        val settled = requireNotNull(dealt.play(Move.STAND))
        val revealed = requireNotNull(settled.revealDealerCard())

        assertEquals(emptyList<PlayedHand>(), dealt.playedHandsSince(betting, now))
        assertEquals(listOf(Outcome.WIN), settled.playedHandsSince(dealt, now).map { it.outcome })
        // Nothing after the settlement plays it again
        assertEquals(emptyList<PlayedHand>(), revealed.playedHandsSince(settled, now))
        assertEquals(emptyList<PlayedHand>(), settled.topUp(10_000).playedHandsSince(settled, now))
        assertEquals(emptyList<PlayedHand>(), requireNotNull(revealed.next()).playedHandsSince(revealed, now))

        // A blackjack settles on the deal
        val blackjack = requireNotNull(Table(STARTING_BANKROLL, Shoe(cards("Ac 6s Kd 9h"))).addChip(2_500))
        val paid = requireNotNull(blackjack.deal(RuleSet.S17, Random(1))).playedHandsSince(blackjack, now)
        assertEquals(listOf(StrategyGrade.NO_ACTION_REQUIRED), paid.map { it.grade })
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
    fun countsEachStrategyGrade() {
        val stats = listOf(hand(2_500, grade = StrategyGrade.INCORRECT), hand(-2_500, grade = StrategyGrade.INCORRECT), hand(0))
            .playStats(Period.ALL_TIME, now)

        assertEquals(mapOf(StrategyGrade.INCORRECT to 2, StrategyGrade.CORRECT to 1), stats.grades)
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
        assertEquals(emptyMap<StrategyGrade, Int>(), stats.grades)
    }
}
