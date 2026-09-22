package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.serializedAndBack
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.PENETRATIONS
import com.aquigs.sp21ace.domain.strategy.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private const val BET = 2_500L
private const val BANKROLL = 100_000L

/** Deals [player] against [dealer] from a shoe stacked in dealing order, with [draws] next in it. */
private fun deal(
    player: String,
    dealer: String,
    draws: String = "",
    ruleSet: RuleSet = RuleSet.S17,
    bet: Long = BET,
    bankroll: Long = BANKROLL,
    insurance: Boolean = false,
): Round {
    val (first, second) = cards(player)
    val (upcard, hole) = cards(dealer)
    val rest = if (draws.isEmpty()) emptyList() else cards(draws)
    return Round.deal(ruleSet, bet, bankroll, Shoe(listOf(first, upcard, second, hole) + rest), insurance)
}

private fun Round.then(vararg moves: Move): Round = moves.fold(this) { round, move -> requireNotNull(round.play(move)) { "Can't $move" } }

private fun Round.next(): Round = requireNotNull(nextHand()) { "No hand waits" }

private fun Round.outcomes() = requireNotNull(results).map { it.outcome to it.net }

private fun Round.result(hand: Int = 0) = requireNotNull(results)[hand]

class RoundTest {
    @Test
    fun dealsTheFirstAndThirdCardsToThePlayerAndTheSecondAndFourthToTheDealer() {
        val round = deal("9c 7d", "6s Kh")

        assertEquals(cards("9c 7d"), round.hands.single().cards)
        assertEquals(cards("6s Kh"), round.dealer)
        assertEquals(BANKROLL - BET, round.bankroll)
        assertFalse(round.settled)
        assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SURRENDER), round.moves())
    }

    @Test
    fun paysABlackjackThreeToTwoAtOnceEvenAgainstADealerBlackjack() {
        for (dealer in listOf("6s Kh", "Ah Kc")) {
            val round = deal("As Kd", dealer)

            assertEquals(listOf(Outcome.WIN to 3_750L), round.outcomes())
            assertTrue(round.result().blackjack)
            assertEquals(BANKROLL + 3_750, round.bankroll)
        }
    }

    @Test
    fun aDealerBlackjackUnderAnAceOrAFaceCardTakesTheBetBeforeThePlayerActs() {
        for (dealer in listOf("As Kh", "Ks Ah")) {
            val round = deal("9c 7d", dealer)

            assertEquals(listOf(Outcome.LOSE to -BET), round.outcomes())
            assertEquals(emptySet<Move>(), round.moves())
        }
    }

    @Test
    fun aStandingHandBeatsALowerTotalOrABustAndPushesAnEqualOne() {
        assertEquals(listOf(Outcome.WIN to BET), deal("Kc 9d", "Ks 8h").then(Move.STAND).outcomes())
        assertEquals(listOf(Outcome.PUSH to 0L), deal("Kc 8d", "Ks 8h").then(Move.STAND).outcomes())
        assertEquals(listOf(Outcome.LOSE to -BET), deal("Kc 7d", "Ks 8h").then(Move.STAND).outcomes())
        assertEquals(listOf(Outcome.WIN to BET), deal("Kc 2d", "Ks 6h", draws = "Qc").then(Move.STAND).outcomes())
    }

    @Test
    fun theDealerStandsOnSoft17OrHitsItAsTheRulesSay() {
        val stands = deal("Kc 9d", "As 6d", draws = "4h").then(Move.STAND)
        assertEquals(cards("As 6d"), stands.dealer)
        assertEquals(listOf(Outcome.WIN to BET), stands.outcomes())

        val hits = deal("Kc 9d", "As 6d", draws = "4h", ruleSet = RuleSet.H17).then(Move.STAND)
        assertEquals(cards("As 6d 4h"), hits.dealer)
        assertEquals(listOf(Outcome.LOSE to -BET), hits.outcomes())
    }

    @Test
    fun theDealerDrawsNothingOnceNoHandIsLeftToBeat() {
        val busted = deal("Kc 6d", "6s Kh", draws = "Qs").then(Move.HIT)
        assertEquals(listOf(Outcome.LOSE to -BET), busted.outcomes())
        assertEquals(cards("6s Kh"), busted.dealer)

        val twentyOne = deal("5c 6d", "Ks 5h", draws = "Kh 6c").then(Move.HIT)
        assertEquals(listOf(Outcome.WIN to BET), twentyOne.outcomes())
        assertEquals(cards("Ks 5h"), twentyOne.dealer)
    }

    @Test
    fun aPlayer21BeatsADealer21() {
        // The hand left standing on 18 makes the dealer draw to 21
        val round = deal("8c 8d", "Ks 5h", draws = "3c Kh Qd 6c").then(Move.SPLIT, Move.HIT).next().then(Move.STAND)

        assertEquals(cards("Ks 5h 6c"), round.dealer)
        assertEquals(listOf(Outcome.WIN to BET, Outcome.LOSE to -BET), round.outcomes())
    }

    @Test
    fun hittingDrawsACardAndStandsOn21OrEndsTheHandOnABust() {
        val hit = deal("5c 6d", "Ks 7h", draws = "2c").then(Move.HIT)
        assertEquals(cards("5c 6d 2c"), hit.hands.single().cards)
        assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE), hit.moves())

        assertEquals(Finish.STOOD, deal("5c 6d", "Ks 7h", draws = "Kc").then(Move.HIT).hands.single().finish)
        assertEquals(Finish.BUSTED, deal("Kc 6d", "Ks 7h", draws = "Qc").then(Move.HIT).hands.single().finish)
    }

    @Test
    fun surrenderLosesHalfTheBetAndComesOnlyWithTheFirstTwoCards() {
        assertEquals(listOf(Outcome.LOSE to -BET / 2), deal("Kc 6d", "Ks 9h").then(Move.SURRENDER).outcomes())

        assertNull(deal("5c 6d", "Ks 9h", draws = "2c").then(Move.HIT).play(Move.SURRENDER))
        assertNull(deal("8c 8d", "Ks 9h", draws = "8h").then(Move.SPLIT).play(Move.SURRENDER))
    }

    @Test
    fun aDoubleDoublesTheWagerAndDrawsOneCardAfterWhichTheHandCanOnlyStandOrRescue() {
        val round = deal("5c 6d", "6s Kh", draws = "2c").then(Move.DOUBLE)
        val hand = round.hands.single()

        assertEquals(cards("5c 6d 2c"), hand.cards)
        assertEquals(2 * BET, hand.wager)
        assertEquals(BANKROLL - 2 * BET, round.bankroll)
        assertEquals(setOf(Move.STAND, Move.RESCUE), round.moves())
    }

    @Test
    fun aDoubleComesWithAnyNumberOfCards() {
        val round = deal("2c 3d", "6s Kh", draws = "2h 4s").then(Move.HIT, Move.DOUBLE)

        assertEquals(cards("2c 3d 2h 4s"), round.hands.single().cards)
        assertEquals(2 * BET, round.hands.single().wager)
    }

    @Test
    fun aRescueGivesBackHalfTheWagerSoItForfeitsTheOriginalBetAfterOneDouble() {
        assertEquals(listOf(Outcome.LOSE to -BET), deal("5c 6d", "6s Kh", draws = "2c").then(Move.DOUBLE, Move.RESCUE).outcomes())

        val redoubled = deal("5c 6d", "6s Kh", draws = "Ac 2d", ruleSet = RuleSet.H17_REDOUBLE).then(Move.DOUBLE, Move.REDOUBLE, Move.RESCUE)
        assertEquals(listOf(Outcome.LOSE to -2 * BET), redoubled.outcomes())
        assertEquals(BANKROLL - 2 * BET, redoubled.bankroll)
    }

    @Test
    fun redoublingComesOnlyWithItsRulesAndStopsAfterThreeDoubles() {
        val once = deal("5c 6d", "6s Kh", draws = "Ac 2d 2h 8s", ruleSet = RuleSet.H17_REDOUBLE).then(Move.DOUBLE)
        assertEquals(setOf(Move.STAND, Move.REDOUBLE, Move.RESCUE), once.moves())
        assertNull(deal("5c 6d", "6s Kh", draws = "Ac", ruleSet = RuleSet.H17).then(Move.DOUBLE).play(Move.REDOUBLE))

        val thrice = once.then(Move.REDOUBLE, Move.REDOUBLE)
        assertEquals(cards("5c 6d Ac 2d 2h"), thrice.hands.single().cards)
        assertEquals(8 * BET, thrice.hands.single().wager)
        assertEquals(setOf(Move.STAND, Move.RESCUE), thrice.moves())

        // 6 and K is 16, which draws the 8 and busts
        assertEquals(listOf(Outcome.WIN to 8 * BET), thrice.then(Move.STAND).outcomes())
    }

    @Test
    fun aDoubleOrSplitHasToBeCoveredByTheBankroll() {
        assertEquals(setOf(Move.HIT, Move.STAND, Move.SURRENDER), deal("8c 8d", "6s Kh", bankroll = BET).moves())
        assertEquals(
            setOf(Move.STAND, Move.RESCUE),
            deal("5c 6d", "6s Kh", draws = "2c", ruleSet = RuleSet.H17_REDOUBLE, bankroll = 2 * BET).then(Move.DOUBLE).moves(),
        )
    }

    @Test
    fun aSplitPlaysEachHandInTurnWaitingOnEachFinishedOneButTheLastAndDrawingTheNextOnesSecondCardWhenItsReached() {
        val split = deal("8c 8d", "7s Kh", draws = "3h Ks Qd").then(Move.SPLIT)
        assertEquals(listOf(cards("8c 3h"), cards("8d")), split.hands.map { it.cards })
        assertEquals(BANKROLL - 2 * BET, split.bankroll)
        assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE), split.moves())
        assertNull(split.nextHand())

        // The first hand draws a K to 21 and waits, with nothing to decide
        val finished = split.then(Move.HIT)
        assertTrue(finished.waitingForNextHand)
        assertEquals(0, finished.active)
        assertNull(finished.activeHand)
        assertEquals(emptySet<Move>(), finished.moves())
        assertNull(finished.play(Move.STAND))
        assertEquals(cards("8d"), finished.hands[1].cards)

        val second = finished.next()
        assertFalse(second.waitingForNextHand)
        assertEquals(1, second.active)
        assertEquals(cards("8d Qd"), second.hands[1].cards)

        // The last hand goes straight to the dealer
        val done = second.then(Move.STAND)
        assertTrue(done.settled)
        assertEquals(listOf(Outcome.WIN to BET, Outcome.WIN to BET), done.outcomes())
        assertEquals(BANKROLL + 2 * BET, done.bankroll)
    }

    @Test
    fun anyTwoTenValueCardsSplit() {
        assertTrue(Move.SPLIT in deal("Kc Qd", "7s Kh").moves())
    }

    @Test
    fun splitsToFourHandsAtMost() {
        val four = deal("8c 8d", "7s Kh", draws = "8h 8s 8c").then(Move.SPLIT, Move.SPLIT, Move.SPLIT)

        assertEquals(MAX_HANDS, four.hands.size)
        assertEquals(cards("8c 8c"), four.hands[0].cards)
        assertFalse(Move.SPLIT in four.moves())
    }

    @Test
    fun splitAcesDrawAndA21FromThemIsPaidEvenMoneyNotAsABlackjack() {
        val round = deal("As Ad", "7s Kh", draws = "Kc 5c 2d").then(Move.SPLIT).next().then(Move.HIT, Move.STAND)

        assertEquals(listOf(cards("As Kc"), cards("Ad 5c 2d")), round.hands.map { it.cards })
        assertEquals(listOf(Outcome.WIN to BET, Outcome.WIN to BET), round.outcomes())
        assertFalse(round.result().blackjack)
    }

    @Test
    fun paysTheCardCountBonusesOnA21ButOnlyEvenMoneyOnceDoubled() {
        assertEquals(listOf(Outcome.WIN to 3_750L), deal("2c 3d", "Ks 7h", draws = "4h 5s 7c").then(Move.HIT, Move.HIT, Move.HIT).outcomes())
        assertEquals(listOf(Outcome.WIN to 5_000L), deal("2c 3d", "Ks 7h", draws = "2h 4s 5c 5d").then(Move.HIT, Move.HIT, Move.HIT, Move.HIT).outcomes())
        assertEquals(
            listOf(Outcome.WIN to 7_500L),
            deal("2c 2d", "Ks 7h", draws = "3h 3s 4c 4d 3c").then(Move.HIT, Move.HIT, Move.HIT, Move.HIT, Move.HIT).outcomes(),
        )

        val doubled = deal("2c 3d", "Ks 7h", draws = "4h 5s 7c").then(Move.HIT, Move.HIT, Move.DOUBLE)
        assertEquals(listOf(Outcome.WIN to 2 * BET), doubled.outcomes())
        assertNull(doubled.result().bonus)
    }

    @Test
    fun paysTheSuitBonusesOnAThreeCard678Or777() {
        fun bonusOf(player: String, draw: String) = deal(player, "Ks 7h", draws = draw).then(Move.HIT).result()

        assertEquals(HandResult(Outcome.WIN, 3_750, bonus = Bonus.MIXED_678), bonusOf("6c 7d", "8h"))
        assertEquals(HandResult(Outcome.WIN, 5_000, bonus = Bonus.SUITED_678), bonusOf("6h 7h", "8h"))
        assertEquals(HandResult(Outcome.WIN, 7_500, bonus = Bonus.SPADED_678), bonusOf("6s 7s", "8s"))
        assertEquals(HandResult(Outcome.WIN, 3_750, bonus = Bonus.MIXED_777), bonusOf("7c 7d", "7h"))
    }

    @Test
    fun aSuited777AgainstA7WinsTheSuperBonusByTheSizeOfTheBet() {
        fun superBonus(bet: Long, upcard: String = "7s") = deal("7h 7h", "$upcard Kc", draws = "7h", bet = bet).then(Move.HIT).result()

        assertEquals(HandResult(Outcome.WIN, 1_000 + 100_000, bonus = Bonus.SUITED_777, superBonus = 100_000), superBonus(500))
        assertEquals(HandResult(Outcome.WIN, 5_000 + 500_000, bonus = Bonus.SUITED_777, superBonus = 500_000), superBonus(2_500))
        assertEquals(HandResult(Outcome.WIN, 800, bonus = Bonus.SUITED_777), superBonus(400))
        assertEquals(HandResult(Outcome.WIN, 5_000, bonus = Bonus.SUITED_777), superBonus(2_500, upcard = "8s"))
    }

    @Test
    fun aSplitHandEarnsItsBonusButNotTheSuperBonus() {
        val round = deal("7h 7h", "7s Kc", draws = "7h 7h Ks").then(Move.SPLIT, Move.HIT).next().then(Move.STAND)

        assertEquals(HandResult(Outcome.WIN, 5_000, bonus = Bonus.SUITED_777), round.result())
    }

    @Test
    fun aMoveTheHandCantMakeChangesNothing() {
        assertNull(deal("9c 7d", "6s Kh").play(Move.SPLIT))
        assertNull(deal("9c 7d", "6s Kh").play(Move.RESCUE))
        assertNull(deal("9c 7d", "6s Kh").then(Move.SURRENDER).play(Move.HIT))
    }

    @Test
    fun survivesSerialization() {
        val round = deal("8c 8d", "7s Kh", draws = "3h Ks Qd").then(Move.SPLIT)

        assertEquals(round, round.serializedAndBack())
    }

    @Test
    fun aSettledRoundFromAFullShoeSurvivesSerialization() {
        val round = Round.deal(RuleSet.S17, BET, BANKROLL, Shoe.shuffled(Random(3))).let { it.play(Move.STAND) ?: it }

        assertEquals(round, round.serializedAndBack())
    }

    @Test
    fun aDealNeedsAnEvenBet() {
        assertThrows(IllegalArgumentException::class.java) { deal("9c 7d", "6s Kh", bet = 2_501) }
    }

    @Test
    fun splitAcesResplitAndTwoThatDrawTenValueCardsSettleWithNoDecision() {
        assertTrue(Move.SPLIT in deal("As Ad", "7s Kh", draws = "Ah").then(Move.SPLIT).moves())

        val first21 = deal("As Ad", "7s Kh", draws = "Kc Qd").then(Move.SPLIT)
        assertTrue(first21.waitingForNextHand)

        val both21 = first21.next()
        assertEquals(listOf(Outcome.WIN to BET, Outcome.WIN to BET), both21.outcomes())
        assertEquals(cards("7s Kh"), both21.dealer)
    }

    @Test
    fun aSplitHandCanDoubleAndThenRescue() {
        // 8-3 doubles onto a 2 and is rescued, and 8-Q stands on 18 against the dealer's 19
        val round = deal("8c 8d", "Ks 9h", draws = "3h 2c Qd").then(Move.SPLIT, Move.DOUBLE, Move.RESCUE).next().then(Move.STAND)

        assertEquals(listOf(Outcome.LOSE to -BET, Outcome.LOSE to -BET), round.outcomes())
        assertEquals(BANKROLL - 2 * BET, round.bankroll)
    }

    @Test
    fun aDoubledHandThatBustsLosesTheWholeWagerAndARescueLeavesTheDealerDrawingNothing() {
        val busted = deal("Kc 3d", "6s Kh", draws = "Qs").then(Move.DOUBLE)
        assertEquals(listOf(Outcome.LOSE to -2 * BET), busted.outcomes())

        val rescued = deal("5c 6d", "6s Kh", draws = "2c 8s").then(Move.DOUBLE, Move.RESCUE)
        assertEquals(cards("6s Kh"), rescued.dealer)
    }

    @Test
    fun aDoubled678Or777PaysEvenMoneyAndNoSuperBonus() {
        assertEquals(HandResult(Outcome.WIN, 2 * BET), deal("6c 7d", "Ks 7h", draws = "8h").then(Move.DOUBLE).result())
        assertEquals(HandResult(Outcome.WIN, 2 * BET), deal("7h 7h", "7s Kc", draws = "7h").then(Move.DOUBLE).result())
    }

    @Test
    fun aSpaded777AgainstA7PaysThreeToOneAndTheSuperBonusWhoseTopTierStartsAt25() {
        assertEquals(
            HandResult(Outcome.WIN, 7_500 + 500_000, bonus = Bonus.SPADED_777, superBonus = 500_000),
            deal("7s 7s", "7d Kc", draws = "7s").then(Move.HIT).result(),
        )
        assertEquals(100_000L, deal("7s 7s", "7d Kc", draws = "7s", bet = 2_498).then(Move.HIT).result().superBonus)
    }

    @Test
    fun aDealerWhoHitsSoft17HitsOneOfThreeCards() {
        // A-A-5 is soft 17, and the 4 makes 21
        assertEquals(listOf(Outcome.WIN to BET), deal("Kc 9d", "As Ah", draws = "5c 4d").then(Move.STAND).outcomes())
        assertEquals(listOf(Outcome.LOSE to -BET), deal("Kc 9d", "As Ah", draws = "5c 4d", ruleSet = RuleSet.H17).then(Move.STAND).outcomes())
    }

    @Test
    fun insuranceIsOfferedAgainstAnAceBeforeThePeekWhereTheTableOffersItAndTheBankrollCoversIt() {
        val offered = deal("9c 7d", "As Kh", insurance = true)
        assertEquals(Insurance.OFFERED, offered.insurance)
        assertFalse(offered.settled)
        assertNull(offered.activeHand)
        assertEquals(emptySet<Move>(), offered.moves())
        assertEquals(BANKROLL - BET, offered.bankroll)
        // Even on a player blackjack, which is paid once it's answered
        assertEquals(Insurance.OFFERED, deal("As Kd", "Ah 6c", insurance = true).insurance)

        assertTrue(deal("9c 7d", "As Kh").settled)
        assertNull(deal("9c 7d", "Ks Ah", insurance = true).insurance)
        assertNull(deal("9c 7d", "As 6h", insurance = true, bankroll = BET + BET / 2 - 2).insurance)
        assertEquals(Insurance.OFFERED, deal("9c 7d", "As 6h", insurance = true, bankroll = BET + BET / 2).insurance)
    }

    @Test
    fun declinedInsuranceCostsNothingAndTheDealerThenPeeks() {
        val blackjack = requireNotNull(deal("9c 7d", "As Kh", insurance = true).insure(take = false))
        assertNull(blackjack.insurance)
        assertEquals(listOf(Outcome.LOSE to -BET), blackjack.outcomes())
        assertEquals(BANKROLL - BET, blackjack.bankroll)

        val played = requireNotNull(deal("9c 7d", "As 6h", insurance = true).insure(take = false))
        assertFalse(played.settled)
        assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SURRENDER), played.moves())
        assertEquals(BANKROLL - BET, played.bankroll)
    }

    @Test
    fun takenInsuranceCostsHalfTheBetAndPaysTwoToOneOnADealerBlackjack() {
        // The hand still loses its bet, but the insurance wins it back
        val blackjack = requireNotNull(deal("9c 7d", "As Kh", insurance = true).insure(take = true))
        assertEquals(listOf(Outcome.LOSE to -BET), blackjack.outcomes())
        assertEquals(BET, blackjack.insuranceNet)
        assertEquals(BANKROLL, blackjack.bankroll)

        val lost = requireNotNull(deal("Kc 9d", "As 6h", insurance = true).insure(take = true))
        assertFalse(lost.settled)
        assertEquals(-BET / 2, lost.insuranceNet)
        assertEquals(BANKROLL - BET - BET / 2, lost.bankroll)
        val stood = lost.then(Move.STAND)
        assertEquals(listOf(Outcome.WIN to BET), stood.outcomes())
        assertEquals(BANKROLL + BET - BET / 2, stood.bankroll)

        // A player blackjack is paid 3 to 2 against the dealer's, and the insurance 2 to 1 on top
        val both = requireNotNull(deal("As Kd", "Ah Kc", insurance = true).insure(take = true))
        assertEquals(listOf(Outcome.WIN to 3_750L), both.outcomes())
        assertEquals(BANKROLL + 3_750 + BET, both.bankroll)
    }

    @Test
    fun insuranceIsAnsweredOnceAndOnlyWhenOffered() {
        assertNull(deal("9c 7d", "As 6h", insurance = true).insure(take = true)?.insure(take = false))
        assertNull(deal("9c 7d", "6s Ah", insurance = true).insure(take = true))
    }

    @Test
    fun randomPlayAlwaysConservesTheChipsAndOffersMovesExactlyUntilTheRoundSettles() {
        val random = Random(7)

        for (ruleSet in RuleSet.entries) {
            var shoe = Shoe.shuffled(random)
            var bankroll = 1_000_000_000L
            repeat(3_000) {
                shoe = shoe.forNextRound(random, PENETRATIONS.last)
                var round = Round.deal(ruleSet, BET, bankroll, shoe, insurance = random.nextBoolean())
                while (!round.settled) {
                    assertEquals(round.activeHand != null, round.moves().isNotEmpty())
                    round = when {
                        round.offeringInsurance -> requireNotNull(round.insure(take = random.nextBoolean()))
                        round.waitingForNextHand -> round.next()
                        else -> requireNotNull(round.play(round.moves().random(random)))
                    }
                }

                assertEquals(emptySet<Move>(), round.moves())
                assertEquals(bankroll + requireNotNull(round.results).sumOf { it.net } + round.insuranceNet, round.bankroll)
                assertTrue(round.shoe.dealt - round.shoe.roundStart <= 72)
                shoe = round.shoe
                bankroll = round.bankroll
            }
        }
    }
}
