package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import org.junit.Assert.assertEquals
import org.junit.Test

class HandTotalsTest {
    private fun total(player: String) = TrainerHand(cards(player), card("6d")).playerTotal

    @Test
    fun aHardOrSoftHandReadsAsItsChartRow() {
        assertEquals("16", total("9c 7d"))
        assertEquals("A-7", total("7h As"))
    }

    @Test
    fun aPairReadsByItsTotalAndOnlyAPairOfAcesAsItsRow() {
        assertEquals("16", total("8h 8s"))
        assertEquals("20", total("Kh Qs"))
        assertEquals("A-A", total("Ah As"))
    }

    @Test
    fun aLongerHandReadsByItsTotal() {
        assertEquals("A-6", total("2c Ah 4s"))
        assertEquals("17", total("Ah 6d Kc"))
        assertEquals("A-2", total("Ac Ad Ah"))
    }

    @Test
    fun aBustReadsAsItsTotal() {
        assertEquals("26", totalLabel(cards("Kc 6d Qs")))
    }
}
