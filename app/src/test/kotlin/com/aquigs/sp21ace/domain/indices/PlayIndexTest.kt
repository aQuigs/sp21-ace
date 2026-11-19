package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.serializedAndBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayIndexTest {
    private val everyHand = HandKind.entries.flatMap { kind -> kind.values.map { IndexHand(kind, it) } }

    @Test
    fun everyHandReadsBackFromItsLabelAsTheChartsKeyTheirRows() {
        for (hand in everyHand) {
            assertEquals(hand, IndexHand.parse(hand.label))
        }

        assertEquals(listOf("5", "19", "A-2", "A-9", "A-A", "2-2", "10-10"), listOf(HandKind.HARD to 5, HandKind.HARD to 19, HandKind.SOFT to 13, HandKind.SOFT to 20, HandKind.PAIR to 1, HandKind.PAIR to 2, HandKind.PAIR to 10).map { (kind, value) -> IndexHand(kind, value).label })
    }

    @Test
    fun noHandOutsideItsKindsValuesOrLabelReads() {
        // Two cards make a hard 4 only as 2-2, a hard 20 only as two tens and a soft 12 only as A-A, and a 21 has nothing left to decide
        for ((kind, value) in listOf(HandKind.HARD to 4, HandKind.HARD to 20, HandKind.HARD to 21, HandKind.SOFT to 12, HandKind.SOFT to 21, HandKind.PAIR to 0, HandKind.PAIR to 11)) {
            assertThrows("$kind $value", IllegalArgumentException::class.java) { IndexHand(kind, value) }
        }

        for (label in listOf("", "4", "20", "05", "A-10", "A-1", "11-11", "A-A-A", "8-9", "J-J", "x")) {
            assertThrows(label, IllegalArgumentException::class.java) { IndexHand.parse(label) }
        }
    }

    @Test
    fun onlyAPairSplitsAndInsuranceIsAgainstAnAce() {
        assertThrows(IllegalArgumentException::class.java) { Deviation.OnHand(IndexHand(HandKind.HARD, 16), Upcard.TEN, DeviationPlay.SPLIT) }
        assertEquals(Upcard.ACE, Deviation.Insurance.upcard)
    }

    @Test
    fun anIndexSurvivesTheActivitysSavedState() {
        for (index in FakeIndices.ALL) {
            assertEquals(index, index.serializedAndBack())
        }

        assertSame(Deviation.Insurance, (FakeIndices.INSURANCE.serializedAndBack() as PlayIndex).deviation)
    }

    @Test
    fun wordsAnIndexAsTheFlashcardsAndIndicesPageShowIt() {
        assertEquals(
            listOf("Hard 18 vs 2", "Pair of aces vs 3", "Soft 20 vs 10", "Hard 5 vs 9", "Hard 19 vs 4", "Insurance"),
            FakeIndices.ALL.map { it.deviation.title },
        )
        assertEquals(
            listOf(
                "Stand at +99 or higher",
                "Split at −99 or higher",
                "Double at 0 or higher",
                "Surrender at +98 or higher",
                "Hit at −97 or lower",
                "Insurance at −98 or higher",
            ),
            FakeIndices.ALL.map { it.inPlainWords },
        )
        assertEquals("Pair of 10s", IndexHand(HandKind.PAIR, 10).title)
        assertEquals("−2", signedCount(-2))
    }
}
