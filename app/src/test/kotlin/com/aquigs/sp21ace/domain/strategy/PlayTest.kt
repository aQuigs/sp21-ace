package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.strategy.ChartTable.HARD
import com.aquigs.sp21ace.domain.strategy.ChartTable.PAIRS
import com.aquigs.sp21ace.domain.strategy.ChartTable.SOFT
import com.aquigs.sp21ace.domain.strategy.RuleSet.H17
import com.aquigs.sp21ace.domain.strategy.RuleSet.S17
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayTest {
    @Test
    fun parsesPlainPlays() {
        assertEquals(Play(Action.HIT), Play.parse("H"))
        assertEquals(Play(Action.SPLIT), Play.parse("P"))
        assertEquals(Play(Action.SURRENDER), Play.parse("R"))
        assertEquals(Play(Action.SURRENDER_OR_HIT), Play.parse("RH"))
    }

    @Test
    fun parsesCardCountsAndBonusMarks() {
        assertEquals(Play(Action.DOUBLE, hitWithCards = 4), Play.parse("D4"))
        assertEquals(Play(Action.STAND, hitWithCards = 4, bonusException = BonusException.ANY_678), Play.parse("S4*"))
        assertEquals(Play(Action.STAND, hitWithCards = 5, bonusException = BonusException.SUITED_678), Play.parse("S5'"))
        assertEquals(Play(Action.STAND, hitWithCards = 6, bonusException = BonusException.SPADED_678), Play.parse("S6\""))
        assertEquals(Play(Action.SPLIT, bonusException = BonusException.SUITED_777), Play.parse("P$"))
    }

    @Test
    fun marksDebatedSquares() {
        assertEquals(Play(Action.DOUBLE, hitWithCards = 3, debated = true), Play.parse("D3†"))
    }

    @Test
    fun rejectsCodesTheChartsNeverPrint() {
        for (code in listOf("X", "H3", "RH4", "R*", "D3*", "S*", "P6$")) {
            assertThrows(code, IllegalArgumentException::class.java) { Play.parse(code) }
        }
    }

    @Test
    fun printsPlainCodes() {
        assertEquals("H", code(S17, HARD, "16", Upcard.TEN))
        assertEquals("S", code(S17, HARD, "18", Upcard.TEN))
        assertEquals("D", code(S17, HARD, "10", Upcard.FIVE))
        assertEquals("P", code(S17, PAIRS, "8-8", Upcard.ACE))
        assertEquals("R", code(H17, PAIRS, "8-8", Upcard.ACE))
        assertEquals("RH", code(S17, HARD, "17", Upcard.ACE))
    }

    @Test
    fun printsCardCountsAndBonusMarks() {
        assertEquals("D3", code(S17, HARD, "11", Upcard.TEN))
        assertEquals("S4*", code(S17, HARD, "14", Upcard.FOUR))
        assertEquals("S5'", code(H17, HARD, "14", Upcard.FIVE))
        assertEquals("S6\"", code(H17, HARD, "14", Upcard.SIX))
        assertEquals("P$", code(S17, PAIRS, "7-7", Upcard.SEVEN))
    }

    @Test
    fun printsADaggerAfterADebatedSquare() {
        assertEquals("S6\"†", code(S17, HARD, "15", Upcard.SIX))
        assertEquals("S†", code(S17, SOFT, "A-9", Upcard.TEN))
    }

    @Test
    fun everySquarePrintsTheCodeItsFixtureSpells() {
        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val chart = StrategyCharts.forRules(ruleSet)

            Fixtures.rows(ruleSet).mapNotNull { (table, hand, upcard, printed, debated) ->
                val expected = printed + if (debated == "yes") "†" else ""
                val actual = chart.play(ChartTable.valueOf(table), hand, Upcard.fromLabel(upcard))?.code
                "$ruleSet $table $hand vs $upcard: fixture $expected, chart $actual".takeIf { actual != expected }
            }
        }

        assertEquals("Squares printed differently from their fixtures", emptyList<String>(), mismatches)
    }

    // Read from the shipped charts, which ChartCellsTest pins to the fixtures, so every code printed here is a real square
    private fun code(ruleSet: RuleSet, table: ChartTable, hand: String, upcard: Upcard): String =
        requireNotNull(StrategyCharts.forRules(ruleSet).play(table, hand, upcard)).code
}
