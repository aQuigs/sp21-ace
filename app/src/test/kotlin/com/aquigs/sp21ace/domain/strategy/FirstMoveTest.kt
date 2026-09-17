package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.Card
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
    fun hitsWhileTheSquaresBonusHandCanStillBeMade() {
        // Hard 14 with the dealer hitting soft 17 is S4* vs 4, S5' vs 5 and S6" vs 6
        assertEquals(Move.HIT, h17.firstMove(cards("6c 8d"), card("4s")))
        assertEquals(Move.STAND, h17.firstMove(cards("5c 9d"), card("4s")))
        assertEquals(Move.STAND, h17.firstMove(cards("6s 8h"), card("5s")))
        assertEquals(Move.HIT, h17.firstMove(cards("6h 8h"), card("5s")))
        assertEquals(Move.STAND, h17.firstMove(cards("6h 8h"), card("6s")))
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
    fun answersEveryTwoCardStartingHandUnderEveryRuleSet() {
        val deck = spanishShoe(decks = 1)

        for (ruleSet in RuleSet.entries) {
            val chart = StrategyCharts.forRules(ruleSet)
            for (first in deck) for (second in deck) for (upcard in deck) {
                val hand = listOf(first, second)
                if (!hand.isBlackjack()) chart.firstMove(hand, upcard)
            }
        }
    }

    @Test
    fun tenValueCardsAreTheTenUpcard() {
        assertEquals(listOf(Upcard.TEN, Upcard.TEN, Upcard.TEN, Upcard.ACE, Upcard.NINE), cards("Jc Qd Ks Ah 9c").map(Card::upcard))
    }
}
