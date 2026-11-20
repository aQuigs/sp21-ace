package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.isPair
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.upcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FlashcardDealerTest {
    private fun preset(vararg entries: IndexEntry) = IndexPreset("p", "P", Game.BLACKJACK, basedOn = null, entries = entries.toList())

    // Every kind of hand an index names: the built-in H17 set's hard totals, pair of tens and insurance, and the fakes' soft
    // totals, pair of aces and hard 5 and 19, at the ends of the hard totals two different cards make
    private val everyKind = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17").let { set ->
        set.copy(entries = set.entries + FakeIndices.ALL.filter { it.deviation != Deviation.Insurance }.map { IndexEntry(it) })
    }

    // Enough deals that a draw of 1 in 4 or 1 in 40 has turned up
    private fun deals(dealer: FlashcardDealer, count: Int = 4000, random: Random = Random(1)): List<Flashcard> =
        generateSequence(dealer.deal(null, random)) { dealer.deal(it, random) }.take(count).toList()

    @Test
    fun everyCardShowsItsIndexsHandAndUpcardInTwoCardsAndInsuranceTheDealersAceAlone() {
        for (card in deals(FlashcardDealer(everyKind))) {
            val deviation = card.index.deviation
            assertEquals("$card", deviation.upcard, card.upcard.upcard)

            if (deviation !is Deviation.OnHand) {
                assertEquals("$card", emptyList<Any>(), card.player)
                continue
            }

            val hand = deviation.hand
            val total = card.player.total()
            assertEquals("$card", 2, card.player.size)
            assertFalse("$card", card.player.isBlackjack())
            when (hand.kind) {
                HandKind.HARD -> assertTrue("$card", !total.soft && total.value == hand.value && !card.player.isPair)
                HandKind.SOFT -> assertTrue("$card", total.soft && total.value == hand.value && !card.player.isPair)
                HandKind.PAIR -> assertTrue("$card", card.player.isPair && card.player[0].rank.value == hand.value)
            }
        }
    }

    @Test
    fun everyIndexSwitchedOnIsDealtAndNoOtherAndNeverTheSameTwiceInARow() {
        val off = FakeIndices.STAND
        val dealer = FlashcardDealer(everyKind.copy(entries = everyKind.entries.map { if (it.index == off) it.copy(enabled = false) else it }))
        val cards = deals(dealer)

        assertEquals(everyKind.enabledIndices.toSet() - off, cards.map { it.index }.toSet())
        assertEquals(dealer.indices, cards.map { it.index }.toSet())
        for ((previous, next) in cards.zipWithNext()) {
            assertNotEquals(previous.index, next.index)
        }
    }

    @Test
    fun aLoneIndexComesEveryTimeButNeverAsTheVeryCardBefore() {
        for (index in listOf(FakeIndices.INSURANCE, FakeIndices.SPLIT)) {
            val cards = deals(FlashcardDealer(preset(IndexEntry(index))), count = 200)

            assertTrue(cards.all { it.index == index })
            for ((previous, next) in cards.zipWithNext()) {
                assertNotEquals(previous, next)
            }
        }
    }

    @Test
    fun aTenValueUpcardOrPairIsA10JackQueenOrKing() {
        val tens = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17").entries.filter { it.index.deviation.upcard == Upcard.TEN || it.index.deviation.let { d -> d is Deviation.OnHand && d.hand == IndexHand(HandKind.PAIR, 10) } }
        val cards = deals(FlashcardDealer(preset(*tens.toTypedArray())))

        val tenValues = setOf(Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING)
        assertEquals(tenValues, cards.filter { it.upcard.upcard == Upcard.TEN }.map { it.upcard.rank }.toSet())
        assertEquals(tenValues, cards.filter { it.player.isPair }.flatMap { card -> card.player.map { it.rank } }.toSet())
    }

    @Test
    fun withNothingSwitchedOnThereIsNoCard() {
        assertNull(FlashcardDealer(preset()).deal(null))
        assertNull(FlashcardDealer(preset(IndexEntry(FakeIndices.STAND, enabled = false))).deal(null))
    }

    @Test
    fun thePadReaches5EitherSideOrAsFarAsTheLargestNumberSwitchedOn() {
        fun index(threshold: Int) = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 16), Upcard.TEN, DeviationPlay.STAND), threshold)
        fun insurance(threshold: Int) = PlayIndex(Deviation.Insurance, threshold)

        assertEquals(-5..5, preset().flashcardAnswers)
        assertEquals(-5..5, IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17").flashcardAnswers)
        assertEquals(-7..7, preset(IndexEntry(index(7)), IndexEntry(insurance(-2))).flashcardAnswers)
        assertEquals(-10..10, preset(IndexEntry(index(3)), IndexEntry(insurance(-10))).flashcardAnswers)
        // A number switched off doesn't widen the pad
        assertEquals(-5..5, preset(IndexEntry(index(3)), IndexEntry(insurance(-10), enabled = false)).flashcardAnswers)
    }
}
