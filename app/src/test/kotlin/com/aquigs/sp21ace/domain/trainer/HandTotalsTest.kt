package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.chartRow
import org.junit.Assert.assertEquals
import org.junit.Test

class HandTotalsTest {
    private fun hand(player: String, upcard: String = "6d") = TrainerHand(cards(player), card(upcard))

    @Test
    fun everyTwoCardHardOrSoftHandReadsAsItsChartRow() {
        val deck = spanishShoe(decks = 1)

        for (first in deck) {
            for (second in deck) {
                val player = listOf(first, second)
                if (chartRow(player).table == ChartTable.PAIRS) continue

                assertEquals("$player", chartRow(player).hand, TrainerHand(player, card("6d")).playerTotal)
            }
        }
    }

    @Test
    fun aPairReadsAsItsTotalAndAPairOfAcesAsItsRow() {
        assertEquals("16", hand("8h 8s").playerTotal)
        assertEquals("20", hand("Kh Qs").playerTotal)
        assertEquals("A-A", hand("Ah As").playerTotal)
    }

    @Test
    fun aLongerHandReadsByItsTotal() {
        assertEquals("A-6", hand("2c Ah 4s").playerTotal)
        assertEquals("17", hand("Ah 6d Kc").playerTotal)
    }

    @Test
    fun theDealerReadsAsTheChartsColumn() {
        assertEquals("10", hand("9c 7d", upcard = "Kh").dealerTotal)
        assertEquals("A", hand("9c 7d", upcard = "As").dealerTotal)
        assertEquals("7", hand("9c 7d", upcard = "7s").dealerTotal)
    }
}
