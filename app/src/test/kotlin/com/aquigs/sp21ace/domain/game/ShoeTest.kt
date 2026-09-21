package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.spanishShoe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShoeTest {
    @Test
    fun shufflesSixSpanishDecks() {
        val shoe = Shoe.shuffled(Random(1))

        assertEquals(288, shoe.cards.size)
        assertEquals(spanishShoe(6).groupingBy { it }.eachCount(), shoe.cards.groupingBy { it }.eachCount())
    }

    @Test
    fun drawsFromTheTop() {
        val (first, rest) = Shoe(cards("7h Kc")).draw()

        assertEquals(card("7h"), first)
        assertEquals(card("Kc"), rest.draw().first)
    }

    @Test
    fun theCutCardComesOutThreeQuartersOfTheWayIn() {
        val shoe = Shoe.shuffled(Random(1))

        assertFalse(shoe.copy(dealt = 215).pastCutCard)
        assertTrue(shoe.copy(dealt = 216).pastCutCard)
    }
}
