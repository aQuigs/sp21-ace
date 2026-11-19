package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.serializedAndBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IndexDraftTest {
    @Test
    fun aDraftIsAnIndexOnlyOnceItsHandUpcardAndPlayAreChosen() {
        val draft = IndexDraft(direction = IndexDirection.AT_OR_BELOW, threshold = -1)

        assertNull(draft.index)
        assertNull(draft.copy(value = 13, upcard = Upcard.TWO).index)
        assertNull(draft.copy(value = 13, play = DeviationPlay.HIT).index)
        assertEquals(
            PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 13), Upcard.TWO, DeviationPlay.HIT), -1, IndexDirection.AT_OR_BELOW),
            draft.copy(value = 13, upcard = Upcard.TWO, play = DeviationPlay.HIT).index,
        )
    }

    @Test
    fun insuranceNeedsNothingChosenAndOffersNoValueOrPlay() {
        val insurance = IndexDraft(threshold = 3).withKind(IndexKind.INSURANCE)

        assertEquals(PlayIndex(Deviation.Insurance, 3), insurance.index)
        assertEquals(IntRange.EMPTY, insurance.values)
        assertEquals(emptyList<DeviationPlay>(), insurance.plays)
    }

    @Test
    fun anotherKindClearsTheValueAndAPlayItDoesNotOfferButKeepsTheRest() {
        val hard = IndexDraft(IndexKind.HARD, 16, Upcard.TEN, DeviationPlay.SURRENDER, IndexDirection.AT_OR_BELOW, 2)

        assertEquals(IndexDraft(IndexKind.SOFT, null, Upcard.TEN, null, IndexDirection.AT_OR_BELOW, 2), hard.withKind(IndexKind.SOFT))
        assertEquals(IndexDraft(IndexKind.PAIR, null, Upcard.TEN, DeviationPlay.SURRENDER, IndexDirection.AT_OR_BELOW, 2), hard.withKind(IndexKind.PAIR))
        assertEquals(hard, hard.withKind(IndexKind.HARD))
        assertEquals(HandKind.SOFT.values, hard.withKind(IndexKind.SOFT).values)
        assertEquals(HandKind.PAIR.plays, hard.withKind(IndexKind.PAIR).plays)
    }

    @Test
    fun aValuePlayOrNumberTheFormDoesNotOfferIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { IndexDraft(IndexKind.SOFT, value = 12) }
        assertThrows(IllegalArgumentException::class.java) { IndexDraft(IndexKind.SOFT, play = DeviationPlay.SURRENDER) }
        assertThrows(IllegalArgumentException::class.java) { IndexDraft(threshold = INDEX_NUMBERS.last + 1) }
    }

    @Test
    fun aHalfFilledDraftSurvivesTheActivitysSavedState() {
        val draft = IndexDraft(IndexKind.PAIR, 8, null, DeviationPlay.SURRENDER, IndexDirection.AT_OR_BELOW, -4)

        assertEquals(draft, draft.serializedAndBack())
    }
}
