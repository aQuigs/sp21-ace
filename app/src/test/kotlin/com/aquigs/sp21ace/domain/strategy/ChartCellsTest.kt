package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Test

// The fixtures list every square with the sources that print it, so a square can only change together with its cited row
class ChartCellsTest {
    @Test
    fun h17WithRedoublingMatchesFixture() = assertMatchesFixture(RuleSet.H17_REDOUBLE, "h17-redouble.tsv")

    @Test
    fun h17MatchesFixture() = assertMatchesFixture(RuleSet.H17, "h17.tsv")

    @Test
    fun s17MatchesFixture() = assertMatchesFixture(RuleSet.S17, "s17.tsv")

    private data class Square(val table: ChartTable, val hand: String, val upcard: Upcard)

    private fun assertMatchesFixture(ruleSet: RuleSet, fixture: String) {
        val expected = fixtureSquares(fixture)
        val chart = StrategyCharts.forRules(ruleSet)
        val actual = chart.tableIds.flatMap { table ->
            chart.hands(table).flatMap { hand ->
                Upcard.entries.mapNotNull { upcard -> chart.play(table, hand, upcard)?.let { Square(table, hand, upcard) to it } }
            }
        }.toMap()

        val mismatches = (expected.keys + actual.keys).filter { expected[it] != actual[it] }
            .map { "$it: fixture ${expected[it]}, chart ${actual[it]}" }
        assertEquals("Squares that differ from $fixture", emptyList<String>(), mismatches)
    }

    private fun fixtureSquares(name: String): Map<Square, Play> {
        val text = requireNotNull(javaClass.getResource("/strategy/$name")) { "Missing fixture $name" }.readText()
        return text.lines().drop(1).filter(String::isNotBlank).associate { line ->
            val (table, hand, upcard, code, debated) = line.split('\t')
            Square(ChartTable.valueOf(table), hand, Upcard.fromLabel(upcard)) to Play.parse(code).copy(debated = debated == "yes")
        }
    }
}
