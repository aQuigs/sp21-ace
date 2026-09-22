package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.TableRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TableStrategyTest {
    private val bet = 2_500L

    /** A round waiting on [hand], the last of [hands] hands, against a dealer showing [upcard]. */
    private fun move(
        hand: String,
        upcard: String,
        ruleSet: RuleSet = RuleSet.S17,
        hands: Int = 1,
        doubles: Int = 0,
        bankroll: Long = STARTING_BANKROLL,
    ): Move? {
        val played = List(hands - 1) { PlayerHand(cards("Kc 8d"), bet, split = true, finish = Finish.STOOD) }
        val waiting = PlayerHand(cards(hand), bet shl doubles, doubles = doubles, split = hands > 1)
        return Round(TableRules(ruleSet.dealerHitsSoft17, ruleSet.redoubling), bet, bankroll, Shoe(emptyList()), cards("$upcard 5h"), played + waiting, active = hands - 1).correctMove()
    }

    @Test
    fun followsTheChartWhereTheTableAllowsItsMove() {
        assertEquals(Move.HIT, move("Kc 6d", "Ks"))
        assertEquals(Move.DOUBLE, move("5c 6d", "6s"))
        assertEquals(Move.SPLIT, move("8c 8d", "Ks"))
        assertEquals(Move.SURRENDER, move("Kc 6d", "As", RuleSet.H17))
        // 11 vs 2 doubles with fewer than 4 cards
        assertEquals(Move.DOUBLE, move("2c 4d 5h", "2s"))
        assertEquals(Move.HIT, move("2c 2d 3h 4s", "2s"))
    }

    @Test
    fun aDoubledHandThatCantRedoubleStandsOrRescuesAsItWouldWithoutRedoubling() {
        // Doubled 12 vs 7 redoubles with redoubling, and without it stands. Each double draws a card
        assertEquals(Move.REDOUBLE, move("5c 5d 2h", "7s", RuleSet.H17_REDOUBLE, doubles = 1))
        assertEquals(Move.STAND, move("2c 2d 2h 3s 3c", "7s", RuleSet.H17_REDOUBLE, doubles = MAX_DOUBLES))
        assertEquals(Move.STAND, move("5c 5d 2h", "7s", RuleSet.H17_REDOUBLE, doubles = 1, bankroll = 0))
        // Doubled 12 vs 9 rescues either way
        assertEquals(Move.RESCUE, move("2c 2d 2h 3s 3c", "9s", RuleSet.H17_REDOUBLE, doubles = MAX_DOUBLES))
        // Doubled 10 vs 5 has no row without redoubling, so it stands as 16 does
        assertEquals(Move.STAND, move("2c 2d 2h 2s 2c", "5s", RuleSet.H17_REDOUBLE, doubles = MAX_DOUBLES))
    }

    @Test
    fun aDoubledHandWithNoRowWithoutRedoublingRescuesAs16DoesWhereTheDealerBustsTooSeldom() {
        // 3-2 doubled against a 9 draws a 4, and with no chips left to redouble it rescues as 16 does rather than standing
        assertEquals(Move.RESCUE, move("3c 2d 4h", "9s", RuleSet.H17_REDOUBLE, doubles = 1, bankroll = 0))
        assertEquals(Move.RESCUE, move("2c 3d 4h", "Ks", RuleSet.S17, doubles = 1))
        assertEquals(Move.STAND, move("2c 3d 4h", "6s", RuleSet.S17, doubles = 1))
    }

    @Test
    fun theCorrectMoveIsAlwaysOneTheTableAllows() {
        RuleSet.entries.forEach { ruleSet ->
            val random = Random(ruleSet.ordinal)
            repeat(20_000) {
                // Bankrolls from one bet to six, so some doubles and splits can't be covered
                var round = Round.deal(TableRules(ruleSet.dealerHitsSoft17, ruleSet.redoubling), bet, bet * random.nextInt(1, 7), Shoe.shuffled(random))
                while (!round.settled) {
                    if (round.waitingForNextHand) {
                        round = requireNotNull(round.nextHand())
                        continue
                    }
                    val moves = round.moves()
                    val move = round.correctMove()
                    assertTrue("$ruleSet ${round.activeHand} vs ${round.upcard}: $move not in $moves", move in moves)
                    round = requireNotNull(round.play(moves.random(random)))
                }
            }
        }
    }

    @Test
    fun withNoSurrenderAfterASplitRhHitsAndEightsAgainstAnAceSplitAgainThenHit() {
        assertEquals(Move.HIT, move("9c 7d", "As", RuleSet.H17, hands = 2))
        assertEquals(Move.SPLIT, move("8c 8d", "As", RuleSet.H17, hands = 2))
        assertEquals(Move.HIT, move("8c 8d", "As", RuleSet.H17, hands = MAX_HANDS))
        assertEquals(Move.HIT, move("8c 8d", "As", RuleSet.H17, hands = 2, bankroll = 0))
    }

    @Test
    fun aDoubleTheBankrollCantCoverStandsOnSoft18OrMoreAndHitsAnythingLess() {
        assertEquals(Move.HIT, move("5c 6d", "6s", bankroll = 0))
        assertEquals(Move.STAND, move("Ac 7d", "4s", bankroll = 0))
        assertEquals(Move.HIT, move("Ac 6d", "4s", bankroll = 0))
        // 5-5 doubles from the pairs table, and falls back as the 10 it totals
        assertEquals(Move.HIT, move("5c 5d", "6s", bankroll = 0))
    }

    @Test
    fun aPairThatCantBeSplitAgainPlaysByItsTotal() {
        assertEquals(Move.STAND, move("9c 9d", "5s", hands = MAX_HANDS))
        assertEquals(Move.HIT, move("8c 8d", "Ks", hands = MAX_HANDS))
        assertEquals(Move.HIT, move("2c 2d", "4s", hands = 2, bankroll = 0))
        // Soft 12 hits, from the soft table's A-A row, or with no such row
        assertEquals(Move.HIT, move("Ac Ad", "6s", hands = MAX_HANDS))
        assertEquals(Move.HIT, move("Ac Ad", "6s", RuleSet.H17_REDOUBLE, hands = MAX_HANDS))
    }

    @Test
    fun splitSevensAgainstASevenSplitAgainSinceASplitHandEarnsNoSuperBonus() {
        assertEquals(Move.HIT, move("7c 7c", "7s"))
        assertEquals(Move.SPLIT, move("7c 7c", "7s", hands = 2))
    }

    @Test
    fun noMoveOnceTheRoundIsSettled() {
        val round = Round.deal(TableRules(), bet, STARTING_BANKROLL, Shoe(cards("Kc 7s 7d Kh")))
        val settled = requireNotNull(round.play(Move.STAND))

        assertNull(settled.correctMove())
    }
}
