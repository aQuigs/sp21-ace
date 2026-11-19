package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.isPair
import com.aquigs.sp21ace.domain.cards.plusCard
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.upcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
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
        generateSequence(dealer.deal(null, random = random)) { dealer.deal(it, random = random) }.take(count).toList()

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

    private fun answers(index: PlayIndex, correct: Int, incorrect: Int) =
        List(correct + incorrect) { FlashcardAnswer(Instant.EPOCH, Game.BLACKJACK, index, if (it < correct) index.threshold else index.threshold + 1, null, 2) }

    @Test
    fun prioritizeWorseShowsAnIndexInInverseProportionToItsAccuracyAsTheTrainerWeighsHands() {
        val never = FakeIndices.STAND
        val mostly = FakeIndices.SPLIT
        val unanswered = FakeIndices.DOUBLE
        // Answers to the same hands with another number or direction are other facts, so they don't count
        val history = answers(never, correct = 0, incorrect = 3) + answers(mostly, correct = 3, incorrect = 1) +
            answers(unanswered.copy(threshold = 1), correct = 0, incorrect = 5) +
            answers(unanswered.copy(direction = IndexDirection.AT_OR_BELOW), correct = 0, incorrect = 5)
        val dealer = FlashcardDealer(preset(IndexEntry(never), IndexEntry(mostly), IndexEntry(unanswered)), FlashcardOptions(CardsShown.PRIORITIZE_WORSE))
        val random = Random(3)

        // Never right weighs 1 / 5%, right 3 times in 4 weighs 1 / 75%, and unanswered 1 / 50%
        val weights = mapOf(never to 20.0, mostly to 4.0 / 3, unanswered to 2.0)
        val shown = List(40_000) { dealer.deal(null, history, random)!!.index }.groupingBy { it }.eachCount()
        for ((index, weight) in weights) {
            assertEquals("$index", weight / weights.values.sum(), shown.getValue(index) / 40_000.0, 0.01)
        }
    }

    @Test
    fun prioritizeWorseStillNeverShowsTheSameIndexTwiceInARow() {
        val dealer = FlashcardDealer(preset(IndexEntry(FakeIndices.STAND), IndexEntry(FakeIndices.SPLIT)), FlashcardOptions(CardsShown.PRIORITIZE_WORSE))
        val history = answers(FakeIndices.STAND, correct = 0, incorrect = 10)
        val random = Random(4)

        val cards = generateSequence(dealer.deal(null, history, random)) { dealer.deal(it, history, random) }.take(200).toList()
        for ((previous, next) in cards.zipWithNext()) {
            assertNotEquals(previous.index, next.index)
        }
    }

    // Every deviation the model allows, each with every play its hand kind offers against every upcard, and insurance
    private val everyDeviation: List<Deviation> = HandKind.entries.flatMap { kind ->
        kind.values.flatMap { value -> kind.plays.flatMap { play -> Upcard.entries.map { Deviation.OnHand(IndexHand(kind, value), it, play) } } }
    } + Deviation.Insurance

    private fun List<Card>.prefixesFromTwoCards() = (2..size).map { take(it).total() }

    @Test
    fun withHandsOf3OrMoreCardsOnAStandOrHitOnATotalIs3Or4CardsAboutHalfTheTimeAndEveryOtherHandTwo() {
        val random = Random(5)

        for (deviation in everyDeviation) {
            val dealer = FlashcardDealer(preset(IndexEntry(PlayIndex(deviation, 0))), FlashcardOptions(multiCardHands = true))
            val cards = deals(dealer, count = 100, random = random)
            val sizes = cards.map { it.player.size }

            val hand = (deviation as? Deviation.OnHand)?.hand
            val drawsOn = deviation is Deviation.OnHand && deviation.play in setOf(DeviationPlay.STAND, DeviationPlay.HIT) && hand!!.kind != HandKind.PAIR
            val canBeMore = drawsOn && MULTI_CARD_HANDS[HandTotal(hand!!.value, soft = hand.kind == HandKind.SOFT)] != null
            when {
                hand == null -> assertEquals("$deviation", setOf(0), sizes.toSet())
                canBeMore -> {
                    assertTrue("$deviation", sizes.all { it in 2..4 })
                    assertEquals("$deviation", 50.0, sizes.count { it > 2 }.toDouble(), 20.0)
                }
                else -> assertEquals("$deviation", setOf(2), sizes.toSet())
            }

            for (card in cards.filter { it.player.size > 2 }) {
                val total = card.player.total()
                assertEquals("$card", HandTotal(hand!!.value, soft = hand.kind == HandKind.SOFT), total)
                // Never a blackjack, a bust or a 21 on the way
                assertTrue("$card", card.player.prefixesFromTwoCards().all { it.value < 21 })
                assertEquals("$card", deviation.upcard, card.upcard.upcard)
            }
        }
    }

    @Test
    fun everyTotalButHard5ComesIn3Or4CardsAndEachSizeOfHandHasItsShare() {
        val totals = listOf(HandKind.HARD, HandKind.SOFT).flatMap { kind -> kind.values.map { HandTotal(it, soft = kind == HandKind.SOFT) } }

        assertEquals(totals - HandTotal(5, soft = false), totals.filter { it in MULTI_CARD_HANDS })
        for ((total, bySize) in MULTI_CARD_HANDS) {
            for (hands in bySize) {
                assertEquals("$total", 1, hands.map { it.size }.distinct().size)
                assertTrue("$total", hands.all { it.size in 3..4 && it.fold(HandTotal(0, soft = false), HandTotal::plusCard) == total })
            }
        }
        // Hard 19 as 3 cards and as 4, so a hand of each size comes up as often as the other
        val hard19 = MULTI_CARD_HANDS.getValue(HandTotal(19, soft = false))
        assertEquals(listOf(3, 4), hard19.map { it.first().size }.sorted())
        assertTrue(listOf(10, 7, 2) in hard19.flatten())
        assertFalse(listOf(1, 10, 8) in hard19.flatten())
    }

    @Test
    fun withHandsOf3OrMoreCardsOffEveryHandIsTwoCards() {
        val stands = everyDeviation.filter { it is Deviation.OnHand && it.play == DeviationPlay.STAND }.distinctBy { (it as Deviation.OnHand).hand }
        for (deviation in stands) {
            val cards = deals(FlashcardDealer(preset(IndexEntry(PlayIndex(deviation, 0)))), count = 50)
            assertTrue("$deviation", cards.all { it.player.size == 2 })
        }
    }
}
