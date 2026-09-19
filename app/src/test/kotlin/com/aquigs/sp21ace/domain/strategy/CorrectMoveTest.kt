package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectMoveTest {
    private val h17 = StrategyCharts.forRules(RuleSet.H17)
    private val s17 = StrategyCharts.forRules(RuleSet.S17)
    private val upcards = Rank.entries.map { Card(it, Suit.CLUBS) }

    @Test
    fun readsTwoCardPairsFromThePairsTableAndOtherHandsByTotal() {
        assertEquals(ChartRow(ChartTable.PAIRS, "8-8"), chartRow(cards("8s 8d")))
        assertEquals(ChartRow(ChartTable.PAIRS, "10-10"), chartRow(cards("Kc Qd")))
        assertEquals(ChartRow(ChartTable.PAIRS, "A-A"), chartRow(cards("Ah Ac")))
        assertEquals(ChartRow(ChartTable.SOFT, "A-7"), chartRow(cards("7h As")))
        assertEquals(ChartRow(ChartTable.SOFT, "A-5"), chartRow(cards("As 2d 3c")))
        assertEquals(ChartRow(ChartTable.HARD, "16"), chartRow(cards("9c 7d")))
        assertEquals(ChartRow(ChartTable.HARD, "12"), chartRow(cards("As 5d 6c")))
    }

    @Test
    fun readsADoubledHandFromTheAfterDoublingTablesByItsTotal() {
        assertEquals(ChartRow(ChartTable.AFTER_DOUBLE_HARD, "14"), afterDoublingRow(cards("5c 6d 3h")))
        assertEquals(ChartRow(ChartTable.AFTER_DOUBLE_SOFT, "A-7"), afterDoublingRow(cards("As 5d 2c")))
        // An ace isn't enough: soft 17 doubled and drawing a 9 makes hard 16
        assertEquals(ChartRow(ChartTable.AFTER_DOUBLE_HARD, "16"), afterDoublingRow(cards("As 6d 9c")))
    }

    @Test
    fun rulesWithoutRedoublingPrintDoubledHardHandsInDoubleDownRescueAndNoSoftOnes() {
        val redouble = StrategyCharts.forRules(RuleSet.H17_REDOUBLE)

        RuleSet.entries.forEach { assertEquals("$it", it.redoubling, StrategyCharts.forRules(it).redoubling) }
        assertEquals(ChartTable.RESCUE, h17.printedTable(ChartTable.AFTER_DOUBLE_HARD))
        assertNull(s17.printedTable(ChartTable.AFTER_DOUBLE_SOFT))
        assertEquals(ChartTable.AFTER_DOUBLE_HARD, redouble.printedTable(ChartTable.AFTER_DOUBLE_HARD))
        assertEquals(ChartTable.HARD, h17.printedTable(ChartTable.HARD))

        // Double Down Rescue's rows are hard 12 to 17
        assertEquals(ChartRow(ChartTable.RESCUE, "12"), h17.doubledRow(HandTotal(12, soft = false)))
        assertNull(h17.doubledRow(HandTotal(18, soft = false)))
        assertNull(s17.doubledRow(HandTotal(18, soft = true)))
        assertEquals(ChartRow(ChartTable.AFTER_DOUBLE_SOFT, "A-7"), redouble.doubledRow(HandTotal(18, soft = true)))
    }

    @Test
    fun aDoubledHandRedoublesOnlyWithRedoublingAndStandsWhereDoubleDownRescuePrintsNoRescue() {
        val redouble = StrategyCharts.forRules(RuleSet.H17_REDOUBLE)
        val hard16 = HandTotal(16, soft = false)
        val soft18 = HandTotal(18, soft = true)

        // Doubled hard 16 vs 10 is a rescue under every rule set
        RuleSet.entries.forEach { assertEquals("$it", Move.RESCUE, StrategyCharts.forRules(it).correctMoveAfterDoubling(hard16, Upcard.TEN)) }
        // Hard 10 vs 5 and soft 18 vs 4 redouble with redoubling, and stand without, where no row prints them
        assertEquals(Move.REDOUBLE, redouble.correctMoveAfterDoubling(HandTotal(10, soft = false), Upcard.FIVE))
        assertEquals(Move.REDOUBLE, redouble.correctMoveAfterDoubling(soft18, Upcard.FOUR))
        assertEquals(Move.STAND, h17.correctMoveAfterDoubling(HandTotal(10, soft = false), Upcard.FIVE))
        assertEquals(Move.STAND, h17.correctMoveAfterDoubling(soft18, Upcard.FOUR))
        // Hard 16 vs 6 is S with the dealer hitting soft 17 and blank with it standing, both a stand
        assertEquals(Play(Action.STAND), h17.afterDoublingPlay(hard16, Upcard.SIX))
        assertEquals(Play(Action.STAND), s17.afterDoublingPlay(hard16, Upcard.SIX))
    }

    @Test
    fun hitsWhileTheSquaresBonusHandCanStillBeMade() {
        // Hard 14 with the dealer hitting soft 17 is S4* vs 4, S5' vs 5 and S6" vs 6
        assertEquals(Move.HIT, h17.correctMove(cards("6c 8d"), card("4s")))
        assertEquals(Move.STAND, h17.correctMove(cards("5c 9d"), card("4s")))
        assertEquals(Move.STAND, h17.correctMove(cards("6s 8h"), card("5s")))
        assertEquals(Move.HIT, h17.correctMove(cards("6h 8h"), card("5s")))
        assertEquals(Move.HIT, h17.correctMove(cards("6s 8s"), card("5s")))
        assertEquals(Move.STAND, h17.correctMove(cards("6h 8h"), card("6s")))
        assertEquals(Move.STAND, h17.correctMove(cards("6s 8h"), card("6s")))
        assertEquals(Move.HIT, h17.correctMove(cards("6s 8s"), card("6s")))
    }

    @Test
    fun hitsSuitedSevensAgainstASevenForTheSuperBonus() {
        // 7-7 vs 7 is P$
        assertEquals(Move.HIT, h17.correctMove(cards("7h 7h"), card("7c")))
        assertEquals(Move.SPLIT, h17.correctMove(cards("7h 7s"), card("7c")))
    }

    @Test
    fun aBonusHandIsOneWhoseRanksCouldMakeItsSquaresBonusWhateverItsSuits() {
        // With the dealer hitting soft 17, hard 14 vs 6 is S6", hard 13 vs 6 S4* and 7-7 vs 7 P$
        for (hand in listOf("6s 8s", "6h 8d", "8c 6c")) assertTrue(hand, h17.bonusHand(cards(hand), Upcard.SIX))
        assertTrue(h17.bonusHand(cards("6c 7d"), Upcard.SIX))
        assertTrue(h17.bonusHand(cards("7h 7s"), Upcard.SEVEN))
        // A 5-9 makes no 6-7-8, and neither 7-7 vs 6 nor hard 14 vs 7 carries a bonus mark
        assertFalse(h17.bonusHand(cards("5c 9d"), Upcard.SIX))
        assertFalse(h17.bonusHand(cards("7h 7h"), Upcard.SIX))
        assertFalse(h17.bonusHand(cards("6c 8d"), Upcard.SEVEN))
        // With the dealer standing on soft 17, hard 13 vs 6 is a plain hit
        assertFalse(s17.bonusHand(cards("6c 7d"), Upcard.SIX))
    }

    @Test
    fun surrendersWhereTheChartSaysSurrender() {
        // 16 vs A is RH when the dealer hits soft 17 and H when it stands; 8-8 vs A is R and P
        assertEquals(Move.SURRENDER, h17.correctMove(cards("9c 7d"), card("As")))
        assertEquals(Move.HIT, s17.correctMove(cards("9c 7d"), card("As")))
        assertEquals(Move.SURRENDER, h17.correctMove(cards("8c 8d"), card("As")))
        assertEquals(Move.SPLIT, s17.correctMove(cards("8c 8d"), card("As")))
    }

    @Test
    fun aCardCountTurnsThePlayIntoAHitAndPastTwoCardsSurrenderIsGone() {
        // With the dealer standing on soft 17, 11 vs 10 is D3, 14 vs 4 is S4* and 17 vs A is RH
        assertEquals(Move.DOUBLE, s17.correctMove(cards("5c 6d"), card("Ks")))
        assertEquals(Move.HIT, s17.correctMove(cards("2c 4d 5h"), card("Ks")))
        assertEquals(Move.STAND, s17.correctMove(cards("5c 4d 5h"), card("4s")))
        assertEquals(Move.HIT, s17.correctMove(cards("2c 3d 4h 5s"), card("4s")))
        assertEquals(Move.SURRENDER, s17.correctMove(cards("9c 8d"), card("As")))
        assertEquals(Move.HIT, s17.correctMove(cards("5c 4d 8h"), card("As")))
    }

    @Test
    fun everyTwoCardAnswerFollowsItsFixtureSquareReadByTheChartLegend() {
        val deck = spanishShoe(decks = 1)
        val hands = deck.flatMap { first -> deck.map { second -> listOf(first, second) } }.filterNot { it.isBlackjack() }

        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val chart = StrategyCharts.forRules(ruleSet)
            val codes = fixtureCodes(ruleSet)

            hands.flatMap { hand ->
                upcards.mapNotNull { upcard ->
                    val expected = legendMove(codes.getValue(printedSquare(hand, upcard)), hand, upcard)
                    val actual = chart.correctMove(hand, upcard)
                    if (expected == actual) null else "$ruleSet $hand vs $upcard: legend $expected, correctMove $actual"
                }
            }
        }

        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun everyAnswerTo3To7CardsFollowsItsFixtureSquareByItsTotalAndCardCount() {
        val hands = multiCardHands(maxCards = 7)

        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val chart = StrategyCharts.forRules(ruleSet)
            val codes = fixtureCodes(ruleSet)

            hands.flatMap { hand ->
                upcards.mapNotNull { upcard ->
                    val expected = legendMove(codes.getValue(printedTotalSquare(hand, upcard)), cards = hand.size)
                    val actual = chart.correctMove(hand, upcard)
                    if (expected == actual) null else "$ruleSet $hand vs $upcard: legend $expected, correctMove $actual"
                }
            }
        }

        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun everyAnswerToADoubledHandFollowsItsFixtureSquareReadByTheChartLegend() {
        // Hard 6 and soft 13 are the lowest a double reaches
        val totals = (6..20).map { HandTotal(it, soft = false) } + (13..20).map { HandTotal(it, soft = true) }

        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val chart = StrategyCharts.forRules(ruleSet)
            val codes = fixtureCodes(ruleSet)

            totals.flatMap { total ->
                val hand = if (total.soft) "A-${total.value - 11}" else "${total.value}"
                upcards.mapNotNull { upcard ->
                    val square = listOf(if (!ruleSet.redoubling) "RESCUE" else if (total.soft) "AFTER_DOUBLE_SOFT" else "AFTER_DOUBLE_HARD", hand, label(upcard.rank.value))
                    // Double Down Rescue leaves a square blank, and prints no row, where the doubled hand stands
                    val code = if (ruleSet.redoubling) codes.getValue(square) else codes[square] ?: "S"
                    val expected = when (code.first()) {
                        'S' -> Move.STAND
                        'D' -> Move.REDOUBLE
                        'R' -> Move.RESCUE
                        else -> error("No move after doubling for $code")
                    }
                    val actual = chart.correctMoveAfterDoubling(total, upcard.upcard)
                    if (expected == actual) null else "$ruleSet doubled $total vs $upcard: legend $expected, correctMoveAfterDoubling $actual"
                }
            }
        }

        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun tenValueCardsAreTheTenUpcard() {
        assertEquals(listOf(Upcard.TEN, Upcard.TEN, Upcard.TEN, Upcard.ACE, Upcard.NINE), cards("Jc Qd Ks Ah 9c").map(Card::upcard))
    }

    // The oracles read the fixture's text codes by the printed legend, sharing no code with chartRow or correctMove, so a
    // mistake in either can't hide behind itself
    private fun fixtureCodes(ruleSet: RuleSet) = Fixtures.rows(ruleSet).associate { (table, hand, upcard, code) -> listOf(table, hand, upcard) to code }

    private fun label(value: Int) = if (value == 1) "A" else "$value"

    private fun printedSquare(hand: List<Card>, upcard: Card): List<String> {
        val (first, second) = hand.map { it.rank.value }
        val row = when {
            first == second -> listOf("PAIRS", "${label(first)}-${label(first)}")
            first == 1 || second == 1 -> listOf("SOFT", "A-${first + second - 1}")
            else -> listOf("HARD", "${first + second}")
        }

        return row + label(upcard.rank.value)
    }

    private fun printedTotalSquare(hand: List<Card>, upcard: Card): List<String> {
        val points = hand.sumOf { it.rank.value }
        val row = if (hand.any { it.rank == Rank.ACE } && points <= 11) listOf("SOFT", "A-${points - 1}") else listOf("HARD", "$points")
        return row + label(upcard.rank.value)
    }

    private fun legendMove(code: String, hand: List<Card>, upcard: Card): Move {
        val sixSevenEight = hand[0].rank != hand[1].rank && hand.all { it.rank.value in 6..8 }
        val suited = hand[0].suit == hand[1].suit
        val hits = when (code.last()) {
            '*' -> sixSevenEight
            '\'' -> sixSevenEight && suited
            '"' -> sixSevenEight && hand.all { it.suit == Suit.SPADES }
            '$' -> suited && upcard.rank == Rank.SEVEN
            else -> false
        }
        if (hits) return Move.HIT

        return when (code.first()) {
            'H' -> Move.HIT
            'S' -> Move.STAND
            'D' -> Move.DOUBLE
            'P' -> Move.SPLIT
            else -> Move.SURRENDER
        }
    }

    // Past two cards no bonus hand is left to make, and no surrender either, so RH is a hit
    private fun legendMove(code: String, cards: Int): Move {
        val count = code.firstOrNull(Char::isDigit)?.digitToInt()

        return when {
            count != null && cards >= count -> Move.HIT
            code.first() == 'H' || code.startsWith("RH") -> Move.HIT
            code.first() == 'S' -> Move.STAND
            code.first() == 'D' -> Move.DOUBLE
            else -> error("No move past two cards for $code")
        }
    }

    // Every set of card values from 3 to [maxCards] cards below 21, suits aside, since past two cards they decide nothing
    private fun multiCardHands(maxCards: Int): List<List<Card>> {
        val ranks = Rank.entries.distinctBy { it.value }

        fun grow(hand: List<Rank>): List<List<Rank>> {
            val points = hand.sumOf { it.value }
            val total = if (Rank.ACE in hand && points <= 11) points + 10 else points
            if (total >= 21) return emptyList()

            val grown = if (hand.size == maxCards) emptyList() else ranks.filter { it >= hand.last() }.flatMap { grow(hand + it) }
            return listOfNotNull(hand.takeIf { it.size >= 3 }) + grown
        }

        return ranks.flatMap { grow(listOf(it)) }.map { hand -> hand.mapIndexed { i, rank -> Card(rank, Suit.entries[i % Suit.entries.size]) } }
    }
}
