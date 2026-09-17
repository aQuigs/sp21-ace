package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.chartRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TrainerHandTest {
    @Test
    fun dealsHardSoftAndPairHandsButNeverABlackjack() {
        val random = Random(21)
        val hands = List(5_000) { dealTrainerHand(random) }

        assertTrue(hands.none { it.player.isBlackjack() })
        assertEquals(setOf(ChartTable.HARD, ChartTable.SOFT, ChartTable.PAIRS), hands.map { chartRow(it.player).table }.toSet())
    }

    @Test
    fun namesTheHandAgainstAnUpcardFrom2To10OrA() {
        assertEquals("Hard 16 vs A", TrainerHand(cards("9c 7d"), card("As")).matchup)
        assertEquals("Soft 17 vs 10", TrainerHand(cards("As 6d"), card("Kh")).matchup)
        assertEquals("Pair of 10s vs 2", TrainerHand(cards("Qs Jh"), card("2c")).matchup)
    }
}
