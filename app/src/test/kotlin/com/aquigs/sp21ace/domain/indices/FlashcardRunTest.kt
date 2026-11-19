package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.serializedAndBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class FlashcardRunTest {
    private fun preset(vararg indices: PlayIndex) = IndexPreset("p", "P", Game.BLACKJACK, basedOn = null, entries = indices.map { IndexEntry(it) })

    // The built-in H17 numbers: stand on hard 16 vs 10 at 0 or higher, and insure at +3 or higher
    private val sixteenVsTen = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17").entries.first { it.index.deviation.title == "Hard 16 vs 10" }.index
    private val insurance = PlayIndex(Deviation.Insurance, 3)

    private val dealer = FlashcardDealer(preset(sixteenVsTen, insurance))
    private val sixteenCard = Flashcard(sixteenVsTen, cards("9c 7d"), card("Kh"))
    private val insuranceCard = Flashcard(insurance, emptyList(), card("As"))
    private val at = Instant.ofEpochMilli(1_789_000_000_000)

    @Test
    fun gradesTheCardShowingThenDealsTheNextAndRecordsTheAnswer() {
        val answered = FlashcardRun(sixteenCard).answer(sixteenCard, 0, dealer, 2350.milliseconds, at) { insuranceCard }

        assertEquals(FlashcardRun(insuranceCard, FlashcardGrade(sixteenCard, 0)), answered?.run)
        assertEquals(FlashcardAnswer(at, Game.BLACKJACK, sixteenVsTen, 0, 2350.milliseconds, cards = 2), answered?.answer)
        assertTrue(answered!!.answer.isCorrect && answered.run.lastGrade!!.isCorrect)
    }

    @Test
    fun aWrongNumberIsGradedWrongAndAnUntimedAnswerStillCounts() {
        val answered = requireNotNull(FlashcardRun(insuranceCard).answer(insuranceCard, -3, dealer, time = null, at) { sixteenCard })

        assertFalse(answered.run.lastGrade!!.isCorrect)
        assertEquals(FlashcardAnswer(at, Game.BLACKJACK, insurance, -3, time = null, cards = 0), answered.answer)
        assertFalse(answered.answer.isCorrect)
    }

    @Test
    fun theDealIsHandedTheAnswerAndByDefaultDealsAnotherIndex() {
        var handed: FlashcardAnswer? = null
        val answered = FlashcardRun(sixteenCard).answer(sixteenCard, 1, dealer, null, at) { handed = it; insuranceCard }

        assertEquals(answered?.answer, handed)
        assertEquals(insurance, FlashcardRun(sixteenCard).answer(sixteenCard, 1, dealer, null, at)?.run?.card?.index)
    }

    @Test
    fun aTapOnACardNoLongerShowingOrOnAnIndexNoLongerDealtGradesNothing() {
        val run = FlashcardRun(insuranceCard, FlashcardGrade(sixteenCard, 0))
        assertNull(run.answer(sixteenCard, 0, dealer, null, at) { error("A stale tap must not deal") })

        val swapped = FlashcardDealer(preset(sixteenVsTen.copy(threshold = 1)))
        assertNull(FlashcardRun(sixteenCard).answer(sixteenCard, 0, swapped, null, at) { error("A stale index must not deal") })
    }

    @Test
    fun aCardWhoseIndexIsNoLongerDealtIsRedealtAndOneStillDealtStays() {
        val run = FlashcardRun(sixteenCard, FlashcardGrade(insuranceCard, 3))
        assertSame(run, run.dealtFrom(dealer) { error("A card still dealt stays") })

        // The number edited, so the old card would grade against it
        val edited = FlashcardDealer(preset(sixteenVsTen.copy(threshold = 1), insurance))
        val redealt = run.dealtFrom(edited) { insuranceCard }
        assertEquals(FlashcardRun(insuranceCard, run.lastGrade), redealt)
        repeat(50) { assertTrue(run.dealtFrom(edited).card?.index in edited.indices) }

        assertEquals(FlashcardRun(null, run.lastGrade), run.dealtFrom(FlashcardDealer(preset())))
        assertEquals(sixteenVsTen, FlashcardRun(null).dealtFrom(FlashcardDealer(preset(sixteenVsTen))).card?.index)
    }

    @Test
    fun aHandOf3OrMoreCardsIsRedealtOnceTheyAreSwitchedOff() {
        val threeCards = FlashcardRun(Flashcard(sixteenVsTen, cards("2c 4d Jh"), card("Kh")))
        val withThreeCards = FlashcardDealer(preset(sixteenVsTen), FlashcardOptions(multiCardHands = true))

        assertSame(threeCards, threeCards.dealtFrom(withThreeCards) { error("A card these options deal stays") })
        repeat(20) { assertEquals(2, threeCards.dealtFrom(FlashcardDealer(preset(sixteenVsTen))).card?.player?.size) }
    }

    @Test
    fun aRunSurvivesTheActivitysSavedState() {
        val run = FlashcardRun(insuranceCard, FlashcardGrade(sixteenCard, -2))

        assertEquals(run, run.serializedAndBack())
    }
}
