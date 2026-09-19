package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.Move
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

    @Test
    fun namesADoubledHandByItsDoublesRatherThanItsCardsSinceTheyNoLongerChangeThePlay() {
        assertEquals("Doubled hard 16 vs 10", TrainerHand(cards("5c 6d 5h"), card("Ks"), doubles = 1).matchup)
        assertEquals("Doubled soft 18 vs 4", TrainerHand(cards("2c 5d As"), card("4h"), doubles = 1).matchup)
        assertEquals("Redoubled hard 14 vs 9", TrainerHand(cards("2c 3d 2h 7s"), card("9d"), doubles = 2).matchup)
    }

    @Test
    fun aDoubledHandCanOnlyStandRescueOrRedoubleWhereTheRulesAllowAnotherDouble() {
        val twoCards = TrainerHand(cards("9c 7d"), card("As"))
        val threeCards = TrainerHand(cards("9c 4d 3h"), card("As"))
        val doubled = TrainerHand(cards("5c 6d 5h"), card("Ks"), doubles = 1)
        val atTheLastDouble = TrainerHand(cards("2c 2d Ah 2s As"), card("Ks"), doubles = 3)

        for (redoubling in listOf(false, true)) {
            assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SPLIT, Move.SURRENDER), twoCards.moves(redoubling))
            assertEquals(setOf(Move.HIT, Move.STAND, Move.DOUBLE), threeCards.moves(redoubling))
            assertEquals(setOf(Move.STAND, Move.RESCUE), atTheLastDouble.moves(redoubling))
        }
        assertEquals(setOf(Move.STAND, Move.RESCUE), doubled.moves(redoubling = false))
        assertEquals(setOf(Move.STAND, Move.REDOUBLE, Move.RESCUE), doubled.moves(redoubling = true))
    }
}
