package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.isPair
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.cards.totalOf
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.history.Tally
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
            val deviation = card.fact.deviation
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

        assertEquals(everyKind.enabledIndices.toSet() - off, cards.map { it.fact }.toSet())
        assertEquals(dealer.indices, cards.map { it.fact }.toSet())
        for ((previous, next) in cards.zipWithNext()) {
            assertNotEquals(previous.fact, next.fact)
        }
    }

    @Test
    fun aLoneIndexComesEveryTimeButNeverAsTheVeryCardBefore() {
        for (index in listOf(FakeIndices.INSURANCE, FakeIndices.SPLIT)) {
            val cards = deals(FlashcardDealer(preset(IndexEntry(index))), count = 200)

            assertTrue(cards.all { it.fact == index })
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

        assertEquals(-5..5, preset().flashcardPad.numbers)
        assertEquals(-5..5, IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17").flashcardPad.numbers)
        assertEquals(-7..7, preset(IndexEntry(index(7)), IndexEntry(insurance(-2))).flashcardPad.numbers)
        assertEquals(-10..10, preset(IndexEntry(index(3)), IndexEntry(insurance(-10))).flashcardPad.numbers)
        // A number switched off doesn't widen the pad
        assertEquals(-5..5, preset(IndexEntry(index(3)), IndexEntry(insurance(-10), enabled = false)).flashcardPad.numbers)
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
        val shown = List(40_000) { dealer.deal(null, history, random)!!.fact }.groupingBy { it }.eachCount()
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
            assertNotEquals(previous.fact, next.fact)
        }
    }

    // Every deviation the model allows, each with every play its hand kind offers against every upcard, and insurance
    private val everyDeviation: List<Deviation> = HandKind.entries.flatMap { kind ->
        kind.values.flatMap { value -> kind.plays.flatMap { play -> Upcard.entries.map { Deviation.OnHand(IndexHand(kind, value), it, play) } } }
    } + Deviation.Insurance


    @Test
    fun withHandsOf3OrMoreCardsOnAStandOrHitOnATotalIs3Or4CardsAboutHalfTheTimeAndEveryOtherHandTwo() {
        val random = Random(5)

        for (deviation in everyDeviation) {
            val dealer = FlashcardDealer(preset(IndexEntry(PlayIndex(deviation, 0))), FlashcardOptions(multiCardHands = true))
            val cards = deals(dealer, count = 100, random = random)
            val sizes = cards.map { it.player.size }

            val hand = (deviation as? Deviation.OnHand)?.hand
            val drawsOn = deviation is Deviation.OnHand && deviation.play in setOf(DeviationPlay.STAND, DeviationPlay.HIT) && hand!!.kind != HandKind.PAIR
            val canBeMore = drawsOn && (deviation as Deviation.OnHand).hand.total in multiCardHands(deviation.upcard)
            when {
                hand == null -> assertEquals("$deviation", setOf(0), sizes.toSet())
                canBeMore -> {
                    assertTrue("$deviation", sizes.all { it in 2..4 })
                    assertEquals("$deviation", 50.0, sizes.count { it > 2 }.toDouble(), 20.0)
                }
                else -> assertEquals("$deviation", setOf(2), sizes.toSet())
            }

            for (card in cards.filter { it.player.size > 2 }) {
                val onHand = deviation as Deviation.OnHand
                val points = card.player.map { it.rank.value }
                assertEquals("$card", onHand.hand.total, card.player.total())
                // Every hand on the way one basic strategy plainly hits against this upcard
                assertTrue("$card", (2 until points.size).all { plainlyHits(points.take(it), onHand.upcard) })
                assertEquals("$card", onHand.upcard, card.upcard.upcard)
            }
        }
    }

    @Test
    fun everyTotalButHard5And6AndSoft13To15ComesIn3Or4CardsAgainstEveryUpcardAndEachSizeOfHandHasItsShare() {
        val totals = listOf(HandKind.HARD, HandKind.SOFT).flatMap { kind -> kind.values.map { HandTotal(it, soft = kind == HandKind.SOFT) } }
        // Only a pair of 2s, a soft hand or a 3-card hard 4 or less leads to these, and none of those is a plain hit
        val twoCardsOnly = listOf(HandTotal(5, soft = false), HandTotal(6, soft = false)) + (13..15).map { HandTotal(it, soft = true) }

        for (upcard in Upcard.entries) {
            val byTotal = multiCardHands(upcard)
            assertEquals("${upcard.label}", totals - twoCardsOnly, totals.filter { it in byTotal })
            for ((total, bySize) in byTotal) {
                for (hands in bySize) {
                    assertEquals("$total", 1, hands.map { it.size }.distinct().size)
                    assertTrue("$total", hands.all { it.size in 3..4 && totalOf(it) == total })
                    assertTrue("$total", hands.all { hand -> (2 until hand.size).all { plainlyHits(hand.take(it), upcard) } })
                }
            }
        }
        // Hard 19 against a 10 as 3 cards and as 4, so a hand of each size comes up as often as the other
        val hard19 = multiCardHands(Upcard.TEN).getValue(HandTotal(19, soft = false))
        assertEquals(listOf(3, 4), hard19.map { it.first().size }.sorted())
        assertTrue(listOf(10, 3, 6) in hard19.flatten())
        // 10-7 stands, and an ace first makes a soft start
        assertFalse(listOf(10, 7, 2) in hard19.flatten())
        assertFalse(listOf(1, 10, 8) in hard19.flatten())
        // 12 vs 5 never as a pair of aces split nor a 6-5 doubled
        val hard12vs5 = multiCardHands(Upcard.FIVE).getValue(HandTotal(12, soft = false)).flatten()
        assertFalse(listOf(1, 1, 10) in hard12vs5)
        assertFalse(listOf(6, 5, 1) in hard12vs5)
        assertTrue(listOf(2, 3, 7) in hard12vs5)
    }

    @Test
    fun aDealerCouldDealOnlyCardsOfItsIndicesInHandsItsOptionsAllow() {
        val twelveVsFive = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 12), Upcard.FIVE, DeviationPlay.STAND), -2)
        fun card(vararg ranks: Rank) = Flashcard(twelveVsFive, ranks.map { Card(it, Suit.SPADES) }, Card(Rank.FIVE, Suit.HEARTS))
        val on = FlashcardDealer(preset(IndexEntry(twelveVsFive)), FlashcardOptions(multiCardHands = true))
        val off = FlashcardDealer(preset(IndexEntry(twelveVsFive)))

        assertTrue(on.couldDeal(card(Rank.TWO, Rank.THREE, Rank.SEVEN)))
        assertTrue(on.couldDeal(card(Rank.TEN, Rank.TWO)))
        assertFalse(on.couldDeal(card(Rank.SIX, Rank.FIVE, Rank.ACE)))
        assertFalse(off.couldDeal(card(Rank.TWO, Rank.THREE, Rank.SEVEN)))
        assertTrue(off.couldDeal(card(Rank.TEN, Rank.TWO)))
        assertFalse(FlashcardDealer(preset()).couldDeal(card(Rank.TEN, Rank.TWO)))
    }

    private fun onHand(play: DeviationPlay, hand: String, upcard: String) = Deviation.parse(play.name, hand, upcard) as Deviation.OnHand

    private fun near(preset: IndexPreset) = nearMisses(preset).map { "${it.deviation.playName} ${it.deviation.title}" }.toSet()

    @Test
    fun theNearMissesAreTheClosestUpcardEitherSideOfEachIndexWithNoneForTheHandAskingForItsPlay() {
        val h17 = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17")

        assertEquals(
            setOf(
                "Stand Hard 16 vs 8", "Stand Hard 16 vs A",
                "Stand Hard 15 vs 8", "Surrender Hard 15 vs 8",
                "Split Pair of 10s vs 4", "Split Pair of 10s vs 7",
                "Double Hard 10 vs 9", "Double Hard 11 vs 10",
                "Double Hard 9 vs 3", "Double Hard 9 vs 6", "Double Hard 9 vs 8",
                "Stand Hard 12 vs 7", "Stand Hard 13 vs 4",
                "Surrender Hard 14 vs 9", "Surrender Hard 14 vs A",
            ),
            near(h17),
        )
    }

    @Test
    fun aHandThePresetListsEvenSwitchedOffOrABuiltInSetDefinesIsNoNearMiss() {
        val sixteenVsTen = PlayIndex(onHand(DeviationPlay.STAND, "16", "10"), 0)
        // 16 vs 9 is the built-in sets' alone
        assertEquals(setOf("Stand Hard 16 vs 8", "Stand Hard 16 vs A"), near(preset(IndexEntry(sixteenVsTen))))

        // Listed for another play, and switched off
        val surrenderVsEight = PlayIndex(onHand(DeviationPlay.SURRENDER, "16", "8"), 4)
        assertEquals(setOf("Stand Hard 16 vs 7", "Stand Hard 16 vs A"), near(preset(IndexEntry(sixteenVsTen), IndexEntry(surrenderVsEight, enabled = false))))

        // A pair of 5s is hard 10, which the built-in sets index against a 10 and an ace
        val fivesVsNine = PlayIndex(onHand(DeviationPlay.DOUBLE, "5-5", "9"), 2)
        assertEquals(setOf("Double Pair of 5s vs 8"), near(preset(IndexEntry(fivesVsNine))))
        val tenVsTen = PlayIndex(onHand(DeviationPlay.DOUBLE, "10", "10"), 4)
        assertTrue("Double Hard 10 vs 9" !in near(preset(IndexEntry(tenVsTen), IndexEntry(fivesVsNine))))

        // A switched-off index has no near misses of its own, and insurance has none
        assertEquals(emptySet<String>(), near(preset(IndexEntry(sixteenVsTen, enabled = false), IndexEntry(FakeIndices.INSURANCE))))
    }

    @Test
    fun aNearMissAsksInItsIndexsDirection() {
        assertEquals(
            setOf(IndexDirection.AT_OR_BELOW),
            nearMisses(preset(IndexEntry(FakeIndices.HIT))).map { it.direction }.toSet(),
        )
    }

    @Test
    fun handsWithNoIndexAreDealtOnlyWhenSwitchedOnAndThenAboutOneCardInEight() {
        assertTrue(deals(FlashcardDealer(everyKind)).none { it.fact is NoIndex })

        // Prioritize worse weighs the indices alone, so the rate is the same
        val worse = deals(FlashcardDealer(everyKind, FlashcardOptions(CardsShown.PRIORITIZE_WORSE, noIndexCards = true)), count = 40_000)
        assertEquals(5000.0, worse.count { it.fact is NoIndex }.toDouble(), 250.0)

        val cards = deals(FlashcardDealer(everyKind, FlashcardOptions(noIndexCards = true)), count = 40_000)
        val noIndex = cards.filter { it.fact is NoIndex }
        assertEquals(5000.0, noIndex.size.toDouble(), 250.0)
        assertEquals(nearMisses(everyKind), noIndex.map { it.fact }.toSet())
        for ((previous, next) in cards.zipWithNext()) {
            assertNotEquals(previous.fact, next.fact)
        }
        for (card in noIndex) {
            val deviation = card.fact.deviation as Deviation.OnHand
            assertNull("$card", card.answer)
            assertEquals("$card", deviation.upcard, card.upcard.upcard)
            assertEquals("$card", deviation.hand.kind == HandKind.PAIR, card.player.isPair)
        }
    }

    @Test
    fun aHandWithNoIndexComesIn3Or4CardsAsAnIndexsHandDoes() {
        val dealer = FlashcardDealer(everyKind, FlashcardOptions(noIndexCards = true, multiCardHands = true))
        val stands = deals(dealer, count = 20_000).filter { it.fact is NoIndex && (it.fact.deviation as Deviation.OnHand).play == DeviationPlay.STAND }

        assertEquals(0.5, stands.count { it.player.size > 2 } / stands.size.toDouble(), 0.1)
        assertTrue(stands.all(dealer::couldDeal))
    }

    @Test
    fun aHandWithNoIndexCouldBeDealtOnlyWhileSwitchedOnAndWithNone() {
        val preset = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17")
        val noIndex = NoIndex(onHand(DeviationPlay.STAND, "16", "8"))
        val card = Flashcard(noIndex, listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.SPADES)), Card(Rank.EIGHT, Suit.HEARTS))
        val on = FlashcardOptions(noIndexCards = true)

        assertTrue(FlashcardDealer(preset, on).couldDeal(card))
        assertFalse(FlashcardDealer(preset).couldDeal(card))
        assertFalse(FlashcardDealer(preset.add(PlayIndex(noIndex.deviation, 7)), on).couldDeal(card))
    }

    @Test
    fun handsWithNoIndexShareATallyOfThoseThePresetStillHasNoneFor() {
        val preset = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17")
        val dealer = FlashcardDealer(preset, FlashcardOptions(noIndexCards = true))
        val sixteenVsEight = NoIndex(onHand(DeviationPlay.STAND, "16", "8"))
        fun answer(fact: IndexFact, answer: Int?) = FlashcardAnswer(Instant.EPOCH, Game.BLACKJACK, fact, answer, null, 2)
        val history = listOf(
            answer(sixteenVsEight, null),
            answer(NoIndex(onHand(DeviationPlay.STAND, "12", "7")), 3),
            // Listed in this preset, so no hand without an index here
            answer(NoIndex(onHand(DeviationPlay.STAND, "16", "10")), null),
            // No hand of this preset's, so it was never asked here
            answer(NoIndex(onHand(DeviationPlay.DOUBLE, "A-7", "6")), null),
            answer(preset.enabledIndices.first(), null),
        )
        val card = Flashcard(sixteenVsEight, emptyList(), Card(Rank.EIGHT, Suit.HEARTS))

        assertEquals(Tally(correct = 1, incorrect = 1), dealer.tally(card, history))
        assertEquals(Tally(correct = 0, incorrect = 1), dealer.tally(card.copy(fact = preset.enabledIndices.first()), history))
    }
}
