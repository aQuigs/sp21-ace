package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.playedHands
import com.aquigs.sp21ace.serializedAndBack
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class TableTest {
    private val random = Random(1)

    // Dealt in order: player, upcard, player, hole, then the draws
    private fun table(deal: String, bankroll: Long = STARTING_BANKROLL) = Table(bankroll, Shoe(cards(deal)))

    private fun Table.betting(vararg chips: Long): Table = chips.fold(this) { table, chip -> requireNotNull(table.addChip(chip)) }

    @Test
    fun chipsMoveFromTheBankrollToTheBetAndBackWhenItsCleared() {
        val bet = table("9c 6s 7d Kh").betting(2_500, 500)

        assertEquals(STARTING_BANKROLL - 3_000, bet.bankroll)
        assertEquals(3_000, bet.bet)
        assertEquals(STARTING_BANKROLL, bet.chips)
        assertEquals(Table(STARTING_BANKROLL, bet.shoe), bet.clearBet())
    }

    @Test
    fun aChipTheBankrollCantCoverStaysOffTheBet() {
        assertNull(table("9c 6s 7d Kh", bankroll = 1_000).addChip(2_500))
    }

    @Test
    fun dealsTheBetButNothingWithoutOne() {
        assertNull(table("9c 6s 7d Kh").deal(RuleSet.S17, random))

        val dealt = requireNotNull(table("9c 6s 7d Kh").betting(2_500).deal(RuleSet.S17, random))
        val round = requireNotNull(dealt.round)
        assertEquals(cards("9c 7d"), round.hands.single().cards)
        assertEquals(2_500, round.bet)
        assertEquals(STARTING_BANKROLL - 2_500, dealt.available)
        assertEquals(0, dealt.bet)
    }

    @Test
    fun dealsFromAFreshShuffleOnceTheCutCardIsOut() {
        val used = Shoe.shuffled(Random(2)).copy(dealt = 216)
        val dealt = requireNotNull(Table(STARTING_BANKROLL, used).betting(500).deal(RuleSet.S17, random)).round

        assertEquals(4, requireNotNull(dealt).shoe.dealt)
        assertNotEquals(used.cards, dealt.shoe.cards)
    }

    @Test
    fun chipsRidingOnAnUnsettledRoundStillCount() {
        val doubled = requireNotNull(table("5c 6s 6d Kh 2c").betting(2_500).deal(RuleSet.S17, random)?.play(Move.DOUBLE))

        assertEquals(STARTING_BANKROLL - 5_000, doubled.available)
        assertEquals(STARTING_BANKROLL, doubled.chips)
    }

    @Test
    fun aSettledRoundShowsEachHandsResultThenBetsTheSameAgain() {
        // 8-8 splits against a 7 with K, the first hand drawing 3 and K to 21 and the second Q to 18, which beat the dealer's 17
        val split = requireNotNull(table("8c 7s 8d Kh 3h Ks Qd").betting(2_500).deal(RuleSet.S17, random))
        val settled = requireNotNull(split.play(Move.SPLIT)?.play(Move.HIT)?.play(Move.STAND))
        assertEquals(cards("8c 3h Ks"), settled.shownHand?.cards)
        assertEquals(STARTING_BANKROLL + 5_000, settled.chips)

        val second = requireNotNull(settled.next())
        assertEquals(cards("8d Qd"), second.shownHand?.cards)

        val again = requireNotNull(second.next())
        assertNull(again.round)
        assertEquals(2_500, again.bet)
        assertEquals(STARTING_BANKROLL + 2_500, again.bankroll)
        assertEquals(split.round?.shoe, again.shoe.copy(dealt = 4))
    }

    @Test
    fun theDealersDrawsTurnOverOneAtATimeAndTheResultAndPayoutWaitForTheLast() {
        // 16 stands against a 6 and K, which draws a Q and busts
        val stood = requireNotNull(table("Kc 6s 6d Kh Qs").betting(2_500).deal(RuleSet.S17, random)?.play(Move.STAND))
        assertEquals(2, stood.dealerCardsShown)
        assertNull(stood.shownResult)
        assertEquals(STARTING_BANKROLL - 2_500, stood.available)
        assertNull(stood.next())

        val revealed = requireNotNull(stood.revealDealerCard())
        assertEquals(3, revealed.dealerCardsShown)
        assertEquals(Outcome.WIN, revealed.shownResult?.outcome)
        assertEquals(STARTING_BANKROLL + 2_500, revealed.available)
        assertNull(revealed.revealDealerCard())
    }

    @Test
    fun whileTheDealerPlaysASplitRoundShowsTheLastHandPlayedThenStepsFromTheFirst() {
        // 8-8 splits against a 6 and K: 21 on the first hand, 18 on the second, and the dealer draws a 5 to 21
        val split = requireNotNull(table("8c 6s 8d Kh 3h Ks Qd 5c").betting(2_500).deal(RuleSet.S17, random))
        val stood = requireNotNull(split.play(Move.SPLIT)?.play(Move.HIT)?.play(Move.STAND))
        assertEquals(1, stood.shownIndex)
        assertFalse(stood.hasNextResult)

        val revealed = requireNotNull(stood.revealDealerCard())
        assertEquals(0, revealed.shownIndex)
        assertEquals(Outcome.WIN, revealed.shownResult?.outcome)
        assertTrue(revealed.hasNextResult)
        assertEquals(Outcome.LOSE, revealed.next()?.shownResult?.outcome)
    }

    @Test
    fun aHintShowsTheCorrectMoveUntilAMoveIsMade() {
        // 12 vs 2 hits, and the 3 it draws makes 15, a decision of its own
        val dealt = requireNotNull(table("Kc 2s 2d Kh 3s").betting(2_500).deal(RuleSet.S17, random))
        assertNull(dealt.hint)

        val hinted = requireNotNull(dealt.showHint())
        assertEquals(Move.HIT, hinted.hint)
        assertNull(hinted.showHint())

        val played = requireNotNull(hinted.play(Move.HIT))
        assertNull(played.hint)
        assertEquals(Move.STAND, played.showHint()?.hint)
    }

    @Test
    fun eachMoveIsGradedOnItsHandAndHelpCountsAsBlackjackAceCountsIt() {
        // 16 vs 6 stands, and the Q to come busts whoever draws it
        val dealt = requireNotNull(table("Kc 6s 6d Kh Qs").betting(2_500).deal(RuleSet.S17, random))
        fun grade(table: Table?) = requireNotNull(table?.round).playedHands(Instant.EPOCH).single().grade

        assertEquals(StrategyGrade.CORRECT, grade(dealt.play(Move.STAND)))
        assertEquals(StrategyGrade.INCORRECT, grade(dealt.play(Move.HIT)))
        assertEquals(StrategyGrade.CORRECT_WITH_HINTS, grade(dealt.showHint()?.play(Move.STAND)))
        // Backing out of a move the warning questioned
        assertEquals(StrategyGrade.CORRECT_WITH_HINTS, grade(dealt.heedWarning()?.play(Move.STAND)))
        assertEquals(StrategyGrade.INCORRECT, grade(dealt.showHint()?.play(Move.HIT)))
        // A blackjack settles at the deal, with nothing to decide
        assertEquals(StrategyGrade.NO_ACTION_REQUIRED, grade(table("Ac 6s Kd 9h").betting(2_500).deal(RuleSet.S17, random)))
    }

    @Test
    fun aSplitsDecisionStaysWithTheFirstOfItsHands() {
        // K-Q vs 6 stands, so the split is wrong. The first hand draws a 9 to stand on, and the second an A, making 21 by itself
        val dealt = requireNotNull(table("Kc 6s Qd Kh 9s Ad 5h").betting(2_500).deal(RuleSet.S17, random))
        val played = requireNotNull(dealt.play(Move.SPLIT)?.play(Move.STAND))

        assertEquals(
            listOf(StrategyGrade.INCORRECT, StrategyGrade.NO_ACTION_REQUIRED),
            requireNotNull(played.round).playedHands(Instant.EPOCH).map { it.grade },
        )
    }

    @Test
    fun theHintGoesWithTheMoveButTheHelpStaysWithTheHand() {
        // 12 vs 2 hits, and the 3 it draws makes 15, a decision of its own
        val dealt = requireNotNull(table("Kc 2s 2d Kh 3s Qs").betting(2_500).deal(RuleSet.S17, random))
        val helped = requireNotNull(dealt.showHint()?.play(Move.HIT))

        assertFalse(helped.hinted)
        assertTrue(requireNotNull(helped.round?.activeHand).strategy.helped)
    }

    @Test
    fun noHelpOnceTheRoundIsSettled() {
        val settled = requireNotNull(table("Kc 6s 6d Kh Qs").betting(2_500).deal(RuleSet.S17, random)?.play(Move.STAND))

        assertNull(settled.showHint())
        assertNull(settled.heedWarning())
    }

    @Test
    fun aTableMidRoundComesBackFromItsSavedState() {
        val doubled = requireNotNull(table("5c 6s 6d Kh 2c").betting(2_500).deal(RuleSet.S17, random)?.play(Move.DOUBLE))

        assertEquals(doubled, doubled.serializedAndBack())
    }

    @Test
    fun theSameBetAgainOnlyIfTheBankrollCoversIt() {
        val lost = requireNotNull(table("Kc 7s 6d Kh", bankroll = 2_500).betting(2_500).deal(RuleSet.S17, random)?.play(Move.STAND))

        assertEquals(Table(0, lost.round!!.shoe), lost.next())
    }

    @Test
    fun aTopUpAddsToTheBankrollBetweenRoundsOrDuringOne() {
        assertEquals(STARTING_BANKROLL + 50_000, table("9c 6s 7d Kh").topUp(50_000).bankroll)

        val dealt = requireNotNull(table("9c 6s 7d Kh").betting(2_500).deal(RuleSet.S17, random))
        assertEquals(STARTING_BANKROLL - 2_500 + 10_000, dealt.topUp(10_000).available)
    }
}
