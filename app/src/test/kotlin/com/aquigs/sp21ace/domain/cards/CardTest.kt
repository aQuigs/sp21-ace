package com.aquigs.sp21ace.domain.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTest {
    @Test
    fun aSpanishShoeHasFortyEightCardsADeckWithJQKAsItsOnlyTens() {
        val shoe = spanishShoe(decks = 6)

        assertEquals(288, shoe.size)
        assertEquals(Rank.entries.associateWith { 24 }, shoe.groupingBy { it.rank }.eachCount())
        assertEquals(setOf(Rank.JACK, Rank.QUEEN, Rank.KING), Rank.entries.filter { it.value == 10 }.toSet())
    }

    @Test
    fun countsOneAceAsElevenWhileThatDoesNotBust() {
        assertEquals(HandTotal(17, soft = true), cards("As 6d").total())
        assertEquals(HandTotal(17, soft = false), cards("As 6d Kc").total())
        assertEquals(HandTotal(12, soft = true), cards("As Ah").total())
        assertEquals(HandTotal(20, soft = false), cards("Kc Qd").total())
    }

    @Test
    fun onlyATwoCardTwentyOneIsBlackjack() {
        assertTrue(cards("As Jd").isBlackjack())
        assertFalse(cards("As 5d 5c").isBlackjack())
    }
}
