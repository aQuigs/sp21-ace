package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainerHandTest {
    @Test
    fun namesTheHandAgainstAnUpcardFrom2To10OrA() {
        assertEquals("Hard 16 vs A", TrainerHand(cards("9c 7d"), card("As")).matchup)
        assertEquals("Soft 17 vs 10", TrainerHand(cards("As 6d"), card("Kh")).matchup)
        assertEquals("Pair of 10s vs 2", TrainerHand(cards("Qs Jh"), card("2c")).matchup)
    }
}
