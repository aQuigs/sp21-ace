package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.drill.Streak
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.serializedAndBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class FlashcardRunTest {
    private fun preset(vararg indices: PlayIndex) = IndexPreset("p", "P", Game.BLACKJACK, basedOn = null, entries = indices.map { IndexEntry(it) })

    // The built-in H17 numbers: stand on hard 16 vs 10 at 0 or higher, and insure at +3 or higher
    private val sixteenVsTen = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17").entries.first { it.index.deviation.title == "Hard 16 vs 10" }.index
    private val insurance = PlayIndex(Deviation.Insurance, 3)

    private val dealer = FlashcardDealer(preset(sixteenVsTen, insurance))
    private val sixteenCard = Flashcard(sixteenVsTen, cards("9c 7d"), card("Kh"))
    private val insuranceCard = Flashcard(insurance, emptyList(), card("As"))
    private val noIndex = Flashcard(FakeIndices.noIndex("STAND", "16", "8"), cards("9c 7d"), card("8h"))
    private val at = Instant.ofEpochMilli(1_789_000_000_000)

    @Test
    fun gradesTheCardShowingThenDealsTheNextAndRecordsTheAnswer() {
        val answered = FlashcardRun(sixteenCard).answer(sixteenCard, 0, dealer, 2350.milliseconds, at) { _, _ -> insuranceCard }

        assertEquals(FlashcardRun(insuranceCard, FlashcardGrade(sixteenCard, 0, Tally(1, 0), timeMillis = 2350), streak = Streak(1)), answered?.run)
        assertEquals(FlashcardAnswer(at, Game.BLACKJACK, sixteenVsTen, 0, 2350.milliseconds, cards = 2), answered?.answer)
        assertTrue(answered!!.answer.isCorrect && answered.run.lastGrade!!.isCorrect)
    }

    @Test
    fun theGradeKeepsTheTimeTheAnswerTookOrNoneWhenItWasntTimed() {
        fun time(took: Duration?) = FlashcardRun(sixteenCard).answer(sixteenCard, 0, dealer, took, at)?.run?.lastGrade?.time

        assertEquals(2350.milliseconds, time(2350.milliseconds))
        assertNull(time(null))
    }

    @Test
    fun aWrongNumberIsGradedWrongAndAnUntimedAnswerStillCounts() {
        val answered = requireNotNull(FlashcardRun(insuranceCard).answer(insuranceCard, -3, dealer, time = null, at) { _, _ -> sixteenCard })

        assertFalse(answered.run.lastGrade!!.isCorrect)
        assertEquals(FlashcardAnswer(at, Game.BLACKJACK, insurance, -3, time = null, cards = 0), answered.answer)
        assertFalse(answered.answer.isCorrect)
    }

    @Test
    fun theDealIsHandedTheHistoryWithTheAnswerAddedAndByDefaultDealsAnotherIndex() {
        val earlier = FlashcardAnswer(at, Game.BLACKJACK, insurance, 3, null, cards = 0)
        var handed: List<FlashcardAnswer>? = null
        val answered = FlashcardRun(sixteenCard).answer(sixteenCard, 1, dealer, null, at, listOf(earlier)) { previous, history ->
            assertEquals(sixteenCard, previous)
            handed = history
            insuranceCard
        }

        assertEquals(listOf(earlier, answered!!.answer), handed)
        assertEquals(insurance, FlashcardRun(sixteenCard).answer(sixteenCard, 1, dealer, null, at)?.run?.card?.fact)
    }

    @Test
    fun aTapOnACardNoLongerShowingOrOnAnIndexNoLongerDealtGradesNothing() {
        val run = FlashcardRun(insuranceCard, FlashcardGrade(sixteenCard, 0, Tally(1, 0), timeMillis = null))
        assertNull(run.answer(sixteenCard, 0, dealer, null, at) { _, _ -> error("A stale tap must not deal") })

        val swapped = FlashcardDealer(preset(sixteenVsTen.copy(threshold = 1)))
        assertNull(FlashcardRun(sixteenCard).answer(sixteenCard, 0, swapped, null, at) { _, _ -> error("A stale index must not deal") })
    }

    @Test
    fun aCardWhoseIndexIsNoLongerDealtIsRedealtAndOneStillDealtStays() {
        val run = FlashcardRun(sixteenCard, FlashcardGrade(insuranceCard, 3, Tally(1, 0), timeMillis = null))
        assertSame(run, run.dealtFrom(dealer) { error("A card still dealt stays") })

        // The number edited, so the old card would grade against it
        val edited = FlashcardDealer(preset(sixteenVsTen.copy(threshold = 1), insurance))
        val redealt = run.dealtFrom(edited) { insuranceCard }
        assertEquals(FlashcardRun(insuranceCard, run.lastGrade), redealt)
        repeat(50) { assertTrue(run.dealtFrom(edited).card?.fact in edited.indices) }

        // Nothing to deal, so no verdict on an index perhaps no longer drilled either
        assertEquals(FlashcardRun(null), run.dealtFrom(FlashcardDealer(preset())))
        assertEquals(sixteenVsTen, FlashcardRun(null).dealtFrom(FlashcardDealer(preset(sixteenVsTen))).card?.fact)
    }

    @Test
    fun aHandOf3OrMoreCardsIsRedealtOnceTheyAreSwitchedOff() {
        val threeCards = FlashcardRun(Flashcard(sixteenVsTen, cards("2c 4d Jh"), card("Kh")))
        val withThreeCards = FlashcardDealer(preset(sixteenVsTen), FlashcardOptions(multiCardHands = true))

        assertSame(threeCards, threeCards.dealtFrom(withThreeCards) { error("A card these options deal stays") })
        repeat(20) { assertEquals(2, threeCards.dealtFrom(FlashcardDealer(preset(sixteenVsTen))).card?.player?.size) }
    }

    @Test
    fun theGradeTalliesEveryAnswerToItsIndexThisOneIncludedWhetherRightOrWrong() {
        fun earlier(index: PlayIndex, answer: Int) = FlashcardAnswer(at, Game.BLACKJACK, index, answer, null, cards = 2)
        // Another index, and the same hand under another number, are facts of their own
        val history = listOf(earlier(sixteenVsTen, 0), earlier(sixteenVsTen, 2), earlier(insurance, 3), earlier(sixteenVsTen.copy(threshold = 1), 1))
        fun tally(number: Int) = FlashcardRun(sixteenCard).answer(sixteenCard, number, dealer, null, at, history)?.run?.lastGrade?.tally

        assertEquals(Tally(correct = 2, incorrect = 1), tally(0))
        assertEquals(Tally(correct = 1, incorrect = 2), tally(1))
    }

    @Test
    fun anAnswerGivenBeforeTheHistoryLoadsHasNoTally() {
        val grade = FlashcardRun(sixteenCard).answer(sixteenCard, 0, dealer, null, at, history = null)?.run?.lastGrade

        assertEquals(FlashcardGrade(sixteenCard, 0, tally = null, timeMillis = null), grade)
    }

    @Test
    fun aClearedHistoryTakesTheVerdictAndTheStreakAndLeavesTheCard() {
        val run = FlashcardRun(sixteenCard, FlashcardGrade(insuranceCard, 3, Tally(1, 0), timeMillis = null), streak = Streak(4))

        assertEquals(FlashcardRun(sixteenCard), run.historyCleared())
    }

    @Test
    fun rightAnswersClimbTheStreakAndAWrongOneDropsItToZero() {
        fun FlashcardRun.answered(number: Int) = requireNotNull(answer(sixteenCard, number, dealer, null, at) { _, _ -> sixteenCard }).run

        val two = FlashcardRun(sixteenCard).answered(0).answered(0)
        assertEquals(Streak(2), two.streak)
        assertEquals(Streak(), two.answered(1).streak)
    }

    @Test
    fun aCardRedealtKeepsTheStreakEvenWithNothingLeftToDeal() {
        val run = FlashcardRun(sixteenCard, FlashcardGrade(insuranceCard, 3, Tally(1, 0), timeMillis = null), streak = Streak(4))

        assertEquals(Streak(4), run.dealtFrom(FlashcardDealer(preset(insurance))).streak)
        assertEquals(FlashcardRun(null, streak = Streak(4)), run.dealtFrom(FlashcardDealer(preset())))
    }

    @Test
    fun noIndexIsRightOnlyForAHandThatHasNone() {
        val withNoIndex = FlashcardDealer(preset(sixteenVsTen), FlashcardOptions(noIndexCards = true))
        fun answered(card: Flashcard, answer: Int?) = requireNotNull(FlashcardRun(card).answer(card, answer, withNoIndex, null, at) { _, _ -> sixteenCard })

        val right = answered(noIndex, null)
        assertTrue(right.run.lastGrade!!.isCorrect && right.answer.isCorrect)
        assertEquals(FlashcardAnswer(at, Game.BLACKJACK, noIndex.fact, null, null, cards = 2), right.answer)
        assertEquals(Streak(1), right.run.streak)

        for (wrong in listOf(answered(noIndex, 0), answered(sixteenCard, null))) {
            assertFalse(wrong.run.lastGrade!!.isCorrect || wrong.answer.isCorrect)
            assertEquals(Streak(), wrong.run.streak)
        }
    }

    @Test
    fun aRunSurvivesTheActivitysSavedState() {
        val run = FlashcardRun(insuranceCard, FlashcardGrade(sixteenCard, -2, Tally(2, 1), timeMillis = 2350), streak = Streak(3))

        assertEquals(run, run.serializedAndBack())

        val noIndexRun = FlashcardRun(noIndex, FlashcardGrade(noIndex, null, tally = null, timeMillis = null))
        assertEquals(noIndexRun, noIndexRun.serializedAndBack())
    }
}
