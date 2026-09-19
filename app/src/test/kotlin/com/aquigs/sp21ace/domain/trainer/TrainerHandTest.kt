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

    @Test
    fun namesAHandOf3OrMoreCardsByHowManyThereAre() {
        assertEquals("3-card soft 17 vs 7", TrainerHand(cards("As 2d 4c"), card("7h")).matchup)
        assertEquals("4-card hard 15 vs 2", TrainerHand(cards("2c 3d 4h 6s"), card("2d")).matchup)
        // Three 5s are no pair
        assertEquals("3-card hard 15 vs A", TrainerHand(cards("5c 5d 5h"), card("Ah")).matchup)
    }
}
