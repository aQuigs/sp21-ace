package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.ChartTable.HARD
import com.aquigs.sp21ace.domain.strategy.ChartTable.PAIRS
import com.aquigs.sp21ace.domain.strategy.ChartTable.SOFT
import com.aquigs.sp21ace.domain.strategy.RuleSet.H17
import com.aquigs.sp21ace.domain.strategy.RuleSet.S17
import org.junit.Assert.assertEquals
import org.junit.Test

class WordingTest {
    @Test
    fun namesHardSoftAndPairHands() {
        assertEquals("Hard 14", handClass(cards("8s 6d")))
        assertEquals("Soft 17", handClass(cards("As 6d")))
        assertEquals("Pair of 8s", handClass(cards("8s 8d")))
        assertEquals("Pair of aces", handClass(cards("Ah Ac")))
        assertEquals("Pair of 10s", handClass(cards("Kc Jd")))
    }

    @Test
    fun wordsPlainPlays() {
        assertEquals("Hit", words(S17, HARD, "16", Upcard.TEN, Move.HIT))
        assertEquals("Stand", words(S17, HARD, "18", Upcard.TEN, Move.STAND))
        assertEquals("Double", words(S17, HARD, "10", Upcard.FIVE, Move.DOUBLE))
        assertEquals("Split", words(S17, PAIRS, "8-8", Upcard.ACE, Move.SPLIT))
        assertEquals("Surrender", words(H17, PAIRS, "8-8", Upcard.ACE, Move.SURRENDER))
        assertEquals("Surrender, otherwise hit", words(S17, HARD, "17", Upcard.ACE, Move.SURRENDER))
    }

    @Test
    fun wordsCardCountsAndBonusMarksAsTimesToHit() {
        assertEquals("Double, but hit with 3 or more cards", words(S17, HARD, "11", Upcard.TEN, Move.DOUBLE))
        assertEquals("Stand, but hit with 4 or more cards or while any 6-7-8 is possible", words(S17, HARD, "14", Upcard.FOUR, Move.STAND))
        assertEquals("Stand, but hit with 5 or more cards or while a suited 6-7-8 is possible", words(H17, HARD, "14", Upcard.FIVE, Move.STAND))
        assertEquals("Stand, but hit with 5 or more cards or while a spaded 6-7-8 is possible", words(S17, HARD, "15", Upcard.FOUR, Move.STAND))
        assertEquals("Split, but hit suited 7s", words(S17, PAIRS, "7-7", Upcard.SEVEN, Move.SPLIT))
    }

    @Test
    fun leadsWithTheHitWhenABonusMakesTheHandAHit() {
        assertEquals(
            "Hit while any 6-7-8 is possible. Otherwise stand, but hit with 4 or more cards",
            words(S17, HARD, "14", Upcard.FOUR, Move.HIT),
        )
        assertEquals("Hit suited 7s. Otherwise split", words(S17, PAIRS, "7-7", Upcard.SEVEN, Move.HIT))
    }

    @Test
    fun marksDebatedSquares() {
        assertEquals("Stand † (debated)", words(S17, SOFT, "A-9", Upcard.TEN, Move.STAND))
        assertEquals(
            "Stand, but hit with 6 or more cards or while a spaded 6-7-8 is possible † (debated)",
            words(S17, HARD, "15", Upcard.SIX, Move.STAND),
        )
        assertEquals(
            "Hit while a spaded 6-7-8 is possible. Otherwise stand, but hit with 6 or more cards † (debated)",
            words(S17, HARD, "15", Upcard.SIX, Move.HIT),
        )
    }

    // Read from the shipped charts, which ChartCellsTest pins to the fixtures, so every code worded here is a real square
    private fun words(ruleSet: RuleSet, table: ChartTable, hand: String, upcard: Upcard, correctMove: Move): String =
        requireNotNull(StrategyCharts.forRules(ruleSet).play(table, hand, upcard)).inPlainWords(correctMove)
}
