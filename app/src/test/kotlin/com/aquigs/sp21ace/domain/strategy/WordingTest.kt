package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.ChartTable.AFTER_DOUBLE_HARD
import com.aquigs.sp21ace.domain.strategy.ChartTable.HARD
import com.aquigs.sp21ace.domain.strategy.ChartTable.PAIRS
import com.aquigs.sp21ace.domain.strategy.ChartTable.SOFT
import com.aquigs.sp21ace.domain.strategy.RuleSet.H17
import com.aquigs.sp21ace.domain.strategy.RuleSet.H17_REDOUBLE
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
    fun leadsWithTheHitWhenTheCardCountMakesTheHandAHit() {
        assertEquals("Hit with 3 or more cards. Otherwise double", words(S17, HARD, "11", Upcard.TEN, Move.HIT, cards = 3))
        assertEquals(
            "Hit with 4 or more cards. Otherwise stand, but hit while any 6-7-8 is possible",
            words(S17, HARD, "14", Upcard.FOUR, Move.HIT, cards = 4),
        )
        assertEquals("Stand, but hit with 4 or more cards or while any 6-7-8 is possible", words(S17, HARD, "14", Upcard.FOUR, Move.STAND, cards = 3))
        // Late surrender comes only with the first two cards, so past them RH hits like a count of 3
        assertEquals("Hit with 3 or more cards. Otherwise surrender", words(S17, HARD, "17", Upcard.ACE, Move.HIT, cards = 3))
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

    @Test
    fun wordsTheSquaresOfAHandAlreadyDoubledBlankOnesIncluded() {
        assertEquals("Redouble", squareWords(H17_REDOUBLE, AFTER_DOUBLE_HARD, "11", Upcard.TWO))
        assertEquals("Rescue", squareWords(H17_REDOUBLE, AFTER_DOUBLE_HARD, "16", Upcard.EIGHT))
        assertEquals("Rescue", squareWords(S17, AFTER_DOUBLE_HARD, "16", Upcard.TEN))
        assertEquals("Stand, no rescue", squareWords(S17, AFTER_DOUBLE_HARD, "12", Upcard.TWO))
    }

    @Test
    fun theLegendListsOnlyWhatTheTableUses() {
        assertEquals(
            listOf(
                LegendEntry("H", "Hit", Action.HIT),
                LegendEntry("S", "Stand", Action.STAND),
                LegendEntry("D", "Double", Action.DOUBLE),
                LegendEntry("RH", "Surrender, otherwise hit", Action.SURRENDER_OR_HIT),
                LegendEntry("3-6", "Hit with that many cards or more"),
                LegendEntry("*", "Hit while any 6-7-8 is possible"),
                LegendEntry("\"", "Hit while a spaded 6-7-8 is possible"),
                LegendEntry("†", "Sources still debate this square"),
            ),
            StrategyCharts.forRules(S17).legend(HARD),
        )
        assertEquals(listOf("H", "S", "D", "P", "$"), StrategyCharts.forRules(S17).legend(PAIRS).map { it.symbol })
    }

    @Test
    fun aDoubledHandsLegendNamesRedoublesRescuesAndBlankSquares() {
        assertEquals(
            listOf(
                LegendEntry("S", "Stand", Action.STAND),
                LegendEntry("D", "Redouble", Action.DOUBLE),
                LegendEntry("R", "Rescue", Action.SURRENDER),
                LegendEntry("†", "Sources still debate this square"),
            ),
            StrategyCharts.forRules(H17_REDOUBLE).legend(AFTER_DOUBLE_HARD),
        )
        assertEquals(
            listOf(LegendEntry("R", "Rescue", Action.SURRENDER), LegendEntry("", "Stand, no rescue")),
            StrategyCharts.forRules(S17).legend(AFTER_DOUBLE_HARD),
        )
    }

    // Read from the shipped charts, which ChartCellsTest pins to the fixtures, so every code worded here is a real square
    private fun words(ruleSet: RuleSet, table: ChartTable, hand: String, upcard: Upcard, correctMove: Move, cards: Int = 2): String =
        requireNotNull(StrategyCharts.forRules(ruleSet).play(table, hand, upcard)).inPlainWords(correctMove, cards)

    private fun squareWords(ruleSet: RuleSet, table: ChartTable, hand: String, upcard: Upcard): String =
        ChartSquare(ChartRow(table, hand), upcard).inPlainWords(StrategyCharts.forRules(ruleSet).play(table, hand, upcard))
}
