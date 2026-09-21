package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private const val BET = 2_500L
private const val BANKROLL = 100_000L

/** Deals [player] against [dealer] from a shoe stacked in dealing order, with [draws] next in it. */
private fun deal(player: String, dealer: String, draws: String = "", ruleSet: RuleSet = RuleSet.S17, bet: Long = BET, bankroll: Long = BANKROLL): Round {
    val (first, second) = cards(player)
    val (upcard, hole) = cards(dealer)
    val rest = if (draws.isEmpty()) emptyList() else cards(draws)
    return dealRound(ruleSet, bet, bankroll, Shoe(listOf(first, upcard, second, hole) + rest))
}

private fun Round.then(vararg moves: Move): Round = moves.fold(this) { round, move -> requireNotNull(round.play(move)) { "Can't $move" } }

private fun Round.outcomes() = requireNotNull(results).map { it.outcome to it.net }

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
            assertTrue(requireNotNull(round.results).single().blackjack)
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
        val round = deal("8c 8d", "Ks 5h", draws = "3c Kh Qd 6c").then(Move.SPLIT, Move.HIT, Move.STAND)

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
    fun aSplitPlaysEachHandInTurnDrawingItsSecondCardWhenItsReached() {
        val split = deal("8c 8d", "7s Kh", draws = "3h Ks Qd").then(Move.SPLIT)
        assertEquals(listOf(cards("8c 3h"), cards("8d")), split.hands.map { it.cards })
        assertEquals(BANKROLL - 2 * BET, split.bankroll)
        assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE), split.moves())

        val second = split.then(Move.HIT)
        assertEquals(1, second.active)
        assertEquals(cards("8d Qd"), second.hands[1].cards)

        val done = second.then(Move.STAND)
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
        val round = deal("As Ad", "7s Kh", draws = "Kc 5c 2d").then(Move.SPLIT, Move.HIT, Move.STAND)

        assertEquals(listOf(cards("As Kc"), cards("Ad 5c 2d")), round.hands.map { it.cards })
        assertEquals(listOf(Outcome.WIN to BET, Outcome.WIN to BET), round.outcomes())
        assertFalse(requireNotNull(round.results).first().blackjack)
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
        assertNull(requireNotNull(doubled.results).single().bonus)
    }

    @Test
    fun paysTheSuitBonusesOnAThreeCard678Or777() {
        fun bonusOf(player: String, draw: String) = requireNotNull(deal(player, "Ks 7h", draws = draw).then(Move.HIT).results).single()

        assertEquals(HandResult(Outcome.WIN, 3_750, bonus = Bonus.MIXED_678), bonusOf("6c 7d", "8h"))
        assertEquals(HandResult(Outcome.WIN, 5_000, bonus = Bonus.SUITED_678), bonusOf("6h 7h", "8h"))
        assertEquals(HandResult(Outcome.WIN, 7_500, bonus = Bonus.SPADED_678), bonusOf("6s 7s", "8s"))
        assertEquals(HandResult(Outcome.WIN, 3_750, bonus = Bonus.MIXED_777), bonusOf("7c 7d", "7h"))
    }

    @Test
    fun aSuited777AgainstA7WinsTheSuperBonusByTheSizeOfTheBet() {
        fun superBonus(bet: Long, upcard: String = "7s") = requireNotNull(deal("7h 7h", "$upcard Kc", draws = "7h", bet = bet).then(Move.HIT).results).single()

        assertEquals(HandResult(Outcome.WIN, 1_000 + 100_000, bonus = Bonus.SUITED_777, superBonus = 100_000), superBonus(500))
        assertEquals(HandResult(Outcome.WIN, 5_000 + 500_000, bonus = Bonus.SUITED_777, superBonus = 500_000), superBonus(2_500))
        assertEquals(HandResult(Outcome.WIN, 800, bonus = Bonus.SUITED_777), superBonus(400))
        assertEquals(HandResult(Outcome.WIN, 5_000, bonus = Bonus.SUITED_777), superBonus(2_500, upcard = "8s"))
    }

    @Test
    fun aSplitHandEarnsItsBonusButNotTheSuperBonus() {
        val round = deal("7h 7h", "7s Kc", draws = "7h 7h Ks").then(Move.SPLIT, Move.HIT, Move.STAND)

        assertEquals(HandResult(Outcome.WIN, 5_000, bonus = Bonus.SUITED_777), requireNotNull(round.results).first())
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

        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(round) } }.toByteArray()
        assertEquals(round, ObjectInputStream(bytes.inputStream()).use { it.readObject() })
    }
}
