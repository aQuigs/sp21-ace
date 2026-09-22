package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.DECKS
import com.aquigs.sp21ace.domain.strategy.PENETRATIONS
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
        assertEquals(spanishShoe(DECKS).groupingBy { it }.eachCount(), shoe.cards.groupingBy { it }.eachCount())
    }

    @Test
    fun drawsFromTheTop() {
        val (first, rest) = Shoe(cards("7h Kc")).draw()

        assertEquals(card("7h"), first)
        assertEquals(card("Kc"), rest.draw().first)
    }

    @Test
    fun theCutCardComesOutThePenetrationOfTheWayIn() {
        val shoe = Shoe.shuffled(Random(1))

        for ((penetration, cutCard) in listOf(PENETRATIONS.first to 28, 75 to 216, PENETRATIONS.last to 244)) {
            assertFalse(shoe.copy(dealt = cutCard - 1).needsShuffle(penetration))
            assertTrue(shoe.copy(dealt = cutCard).needsShuffle(penetration))
        }
    }

    @Test
    fun aRoundDealtBeforeTheCutCardStartsWhereTheLastLeftOff() {
        val shoe = Shoe.shuffled(Random(1)).copy(dealt = 100)

        assertEquals(shoe.copy(roundStart = 100), shoe.forNextRound(Random(2), penetration = 75))
    }

    @Test
    fun aRoundThatRunsTheShoeOutDealsOnFromTheDiscardsShuffledAndTheNextRoundGetsAFreshShoe() {
        // Two discards, then a round that has dealt the shoe's last four cards
        val shoe = Shoe(cards("2c 3c 4c 5c 6c 7c"), dealt = 6, roundStart = 2, seed = 1)

        val (card, rest) = shoe.draw()

        assertTrue(card in cards("2c 3c"))
        assertEquals(cards("4c 5c 6c 7c"), rest.cards.take(4))
        assertEquals(cards("2c 3c").toSet(), rest.cards.drop(4).toSet())
        assertEquals(5, rest.dealt)
        assertTrue(rest.needsShuffle(PENETRATIONS.last))
        assertEquals(288, rest.forNextRound(Random(2), PENETRATIONS.last).cards.size)
    }
}
