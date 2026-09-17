package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Test

// The fixtures list every square with the sources that print its play, so a square can't change without its fixture row and every square cites a source
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
        val squares = text.lines().drop(1).filter(String::isNotBlank).map { line ->
            val fields = line.split('\t')
            require(fields.size == 6) { "$name: expected 6 fields in \"$line\"" }
            val (table, hand, upcard, code, debated) = fields
            require(debated == "yes" || debated == "no") { "$name: debated must be yes or no in \"$line\"" }
            require(fields[5].isNotBlank()) { "$name: no source cited in \"$line\"" }
            Square(ChartTable.valueOf(table), hand, Upcard.fromLabel(upcard)) to Play.parse(code).copy(debated = debated == "yes")
        }
        require(squares.map { it.first }.toSet().size == squares.size) { "$name lists a square twice" }
        return squares.toMap()
    }
}
