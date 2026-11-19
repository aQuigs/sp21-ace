package com.aquigs.sp21ace.domain.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CardTest {
    @Test
    fun everyCardReadsBackFromItsCodeAndNothingElseReadsAsACard() {
        for (each in Rank.entries.flatMap { rank -> Suit.entries.map { Card(rank, it) } }) {
            assertEquals(each, card(each.code))
        }

        // A 1, an unknown suit, and no rank
        for (code in listOf("1h", "Ax", "s", "")) {
            assertThrows(code, IllegalArgumentException::class.java) { card(code) }
        }
    }

    @Test
    fun aSpanishShoeHasFortyEightCardsADeckWithJQKAsItsOnlyTens() {
        val shoe = spanishShoe(decks = 6)

        assertEquals(288, shoe.size)
        assertEquals(SPANISH_RANKS.associateWith { 24 }, shoe.groupingBy { it.rank }.eachCount())
        assertEquals(setOf(Rank.JACK, Rank.QUEEN, Rank.KING), SPANISH_RANKS.filter { it.value == 10 }.toSet())
        assertFalse(shoe.any { it.rank == Rank.TEN })
    }

    @Test
    fun countsOneAceAsElevenWhileThatDoesNotBust() {
        assertEquals(HandTotal(17, soft = true), cards("As 6d").total())
        assertEquals(HandTotal(17, soft = false), cards("As 6d Kc").total())
        assertEquals(HandTotal(12, soft = true), cards("As Ah").total())
        assertEquals(HandTotal(20, soft = false), cards("Kc Qd").total())
        // An ace after a hard 12 can only count 1, and a soft 18 that draws a 5 goes hard rather than bust
        assertEquals(HandTotal(13, soft = false), cards("Kc 2d As").total())
        assertEquals(HandTotal(13, soft = false), cards("As 7d 5c").total())
    }

    @Test
    fun onlyATwoCardTwentyOneIsBlackjack() {
        assertTrue(cards("As Jd").isBlackjack())
        assertFalse(cards("As 5d 5c").isBlackjack())
    }
}
