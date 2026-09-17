package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.chartRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

class TrainerHandTest {
    @Test
    fun dealsHardSoftAndPairHandsButNeverABlackjack() {
        val random = Random(21)
        val hands = List(5_000) { dealTrainerHand(random) }

        hands.forEach { assertEquals(2, it.player.size) }
        hands.forEach { assertFalse(it.player.isBlackjack()) }
        assertEquals(setOf(ChartTable.HARD, ChartTable.SOFT, ChartTable.PAIRS), hands.map { chartRow(it.player).table }.toSet())
    }
}
