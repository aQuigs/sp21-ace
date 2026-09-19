package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstMoveTest {
    private val h17 = StrategyCharts.forRules(RuleSet.H17)
    private val s17 = StrategyCharts.forRules(RuleSet.S17)

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
    fun hitsWhileTheSquaresBonusHandCanStillBeMade() {
        // Hard 14 with the dealer hitting soft 17 is S4* vs 4, S5' vs 5 and S6" vs 6
        assertEquals(Move.HIT, h17.firstMove(cards("6c 8d"), card("4s")))
        assertEquals(Move.STAND, h17.firstMove(cards("5c 9d"), card("4s")))
        assertEquals(Move.STAND, h17.firstMove(cards("6s 8h"), card("5s")))
        assertEquals(Move.HIT, h17.firstMove(cards("6h 8h"), card("5s")))
        assertEquals(Move.HIT, h17.firstMove(cards("6s 8s"), card("5s")))
        assertEquals(Move.STAND, h17.firstMove(cards("6h 8h"), card("6s")))
        assertEquals(Move.STAND, h17.firstMove(cards("6s 8h"), card("6s")))
        assertEquals(Move.HIT, h17.firstMove(cards("6s 8s"), card("6s")))
    }

    @Test
    fun hitsSuitedSevensAgainstASevenForTheSuperBonus() {
        // 7-7 vs 7 is P$
        assertEquals(Move.HIT, h17.firstMove(cards("7h 7h"), card("7c")))
        assertEquals(Move.SPLIT, h17.firstMove(cards("7h 7s"), card("7c")))
    }

    @Test
    fun surrendersWhereTheChartSaysSurrender() {
        // 16 vs A is RH when the dealer hits soft 17 and H when it stands; 8-8 vs A is R and P
        assertEquals(Move.SURRENDER, h17.firstMove(cards("9c 7d"), card("As")))
        assertEquals(Move.HIT, s17.firstMove(cards("9c 7d"), card("As")))
        assertEquals(Move.SURRENDER, h17.firstMove(cards("8c 8d"), card("As")))
        assertEquals(Move.SPLIT, s17.firstMove(cards("8c 8d"), card("As")))
    }

    @Test
    fun everyTwoCardAnswerFollowsItsFixtureSquareReadByTheChartLegend() {
        val deck = spanishShoe(decks = 1)
        val hands = deck.flatMap { first -> deck.map { second -> listOf(first, second) } }.filterNot { it.isBlackjack() }
        val upcards = Rank.entries.map { Card(it, Suit.CLUBS) }

        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val chart = StrategyCharts.forRules(ruleSet)
            val codes = Fixtures.rows(ruleSet).associate { (table, hand, upcard, code) -> listOf(table, hand, upcard) to code }

            hands.flatMap { hand ->
                upcards.mapNotNull { upcard ->
                    val expected = legendMove(codes.getValue(printedSquare(hand, upcard)), hand, upcard)
                    val actual = chart.firstMove(hand, upcard)
                    if (expected == actual) null else "$ruleSet $hand vs $upcard: legend $expected, firstMove $actual"
                }
            }
        }

        assertEquals(emptyList<String>(), mismatches)
    }

    @Test
    fun tenValueCardsAreTheTenUpcard() {
        assertEquals(listOf(Upcard.TEN, Upcard.TEN, Upcard.TEN, Upcard.ACE, Upcard.NINE), cards("Jc Qd Ks Ah 9c").map(Card::upcard))
    }

    // The oracle reads the fixture's text codes by the printed legend, sharing no code with chartRow or firstMove, so a
    // mistake in either can't hide behind itself
    private fun printedSquare(hand: List<Card>, upcard: Card): List<String> {
        val (first, second) = hand.map { it.rank.value }
        val label = { value: Int -> if (value == 1) "A" else "$value" }
        val row = when {
            first == second -> listOf("PAIRS", "${label(first)}-${label(first)}")
            first == 1 || second == 1 -> listOf("SOFT", "A-${first + second - 1}")
            else -> listOf("HARD", "${first + second}")
        }

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
}
