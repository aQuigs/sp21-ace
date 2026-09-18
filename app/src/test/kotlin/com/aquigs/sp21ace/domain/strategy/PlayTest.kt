package com.aquigs.sp21ace.domain.strategy

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

    // The parser accepts only the charts' own order of play, card count, mark and dagger, so a square that reads back as
    // itself was printed in chart notation
    @Test
    fun everySquarePrintsACodeThatReadsBackAsTheSamePlay() {
        val misprinted = RuleSet.entries.flatMap { ruleSet ->
            val chart = StrategyCharts.forRules(ruleSet)

            chart.tables
                .flatMap { table -> chart.hands(table).flatMap { hand -> Upcard.entries.mapNotNull { chart.play(table, hand, it) } } }
                .filter { Play.parse(it.code) != it }
                .map { "$ruleSet ${it.code}" }
        }

        assertEquals(emptyList<String>(), misprinted)
    }
}
