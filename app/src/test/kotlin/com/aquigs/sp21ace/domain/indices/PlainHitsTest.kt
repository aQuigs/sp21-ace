package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.totalOf
import com.aquigs.sp21ace.domain.strategy.Upcard
import org.junit.Assert.assertEquals
import org.junit.Test

class PlainHitsTest {
    // Each row reads across upcards 2 to A, H where Wizard of Odds' 4-8 deck charts hit with both soft 17 rules and nothing
    // else is open, - anywhere else. A row left out never plainly hits.
    private val twoCardHard = mapOf(
        5 to "HHHHHHHHHH",
        6 to "HHHHHHHHHH",
        7 to "HHHHHHHHHH",
        8 to "HHHHHHHHHH",
        // 9 to 11 double against some upcards, 15 and 16 surrender against some
        12 to "-----HHHHH",
        13 to "-----HHHHH",
        14 to "-----HHHHH",
    )

    // Once past two cards only a hit or a stand is open, so a double or surrender square reads as its fallback hit
    private val longerHard = (4..11).associateWith { "HHHHHHHHHH" } + mapOf(
        12 to "HH---HHHHH",
        13 to "-----HHHHH",
        14 to "-----HHHHH",
        15 to "-----HHHHH",
        16 to "-----HHHHH",
    )

    private val longerSoft = (12..17).associateWith { "HHHHHHHHHH" } + mapOf(18 to "-------HHH")

    private fun expected(row: String?, upcard: Upcard) = row?.get(upcard.ordinal) == 'H'

    private fun hands(size: Int): List<List<Int>> =
        (1 until size).fold((1..10).map { listOf(it) }) { hands, _ -> hands.flatMap { hand -> (1..10).map { hand + it } } }

    @Test
    fun twoCardsPlainlyHitOnlyAsAHard5To8OrA12To14AgainstA7OrMoreAndNeverAsAPairOrSoft() {
        for (hand in hands(2)) {
            val total = totalOf(hand)
            val row = twoCardHard[total.value].takeIf { !total.soft && hand[0] != hand[1] }
            for (upcard in Upcard.entries) assertEquals("$hand vs ${upcard.label}", expected(row, upcard), plainlyHits(hand, upcard))
        }
    }

    @Test
    fun threeOrFourCardsPlainlyHitAsTheChartsHitOrDoubleOrSurrenderFallingBackToAHit() {
        for (hand in hands(3) + hands(4)) {
            val total = totalOf(hand)
            val row = (if (total.soft) longerSoft else longerHard)[total.value]
            for (upcard in Upcard.entries) assertEquals("$hand vs ${upcard.label}", expected(row, upcard), plainlyHits(hand, upcard))
        }
    }
}
