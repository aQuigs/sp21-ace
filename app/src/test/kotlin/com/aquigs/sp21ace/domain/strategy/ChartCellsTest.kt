package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Test

// The fixtures list every square with the sources that print its play, so a square can't change without its fixture row and every square cites a source
class ChartCellsTest {
    @Test
    fun everyRuleSetMatchesItsFixture() {
        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val expected = fixtureSquares(ruleSet.name.lowercase().replace('_', '-') + ".tsv")
            val chart = StrategyCharts.forRules(ruleSet)
            val actual = ChartTable.entries.flatMap { table ->
                chart.hands(table).flatMap { hand ->
                    Upcard.entries.mapNotNull { upcard -> chart.play(table, hand, upcard)?.let { Square(table, hand, upcard) to it } }
                }
            }.toMap()

            (expected.keys + actual.keys).filter { expected[it] != actual[it] }
                .map { "$ruleSet $it: fixture ${expected[it]}, chart ${actual[it]}" }
        }

        assertEquals("Squares that differ from their fixtures", emptyList<String>(), mismatches)
    }

    private data class Square(val table: ChartTable, val hand: String, val upcard: Upcard)

    private fun fixtureSquares(name: String): Map<Square, Play> {
        val text = requireNotNull(javaClass.getResource("/strategy/$name")) { "Missing fixture $name" }.readText()
        val squares = text.lines().drop(1).filter(String::isNotBlank).map { line ->
            val fields = line.split('\t')
            require(fields.size == 6) { "$name: expected 6 fields in \"$line\"" }
            val (table, hand, upcard, code, debated) = fields
            val sources = fields[5]
            require(sources.isNotBlank()) { "$name: no source cited in \"$line\"" }
            val isDebated = when (debated) {
                "yes" -> true
                "no" -> false
                else -> error("$name: debated must be yes or no in \"$line\"")
            }

            Square(ChartTable.valueOf(table), hand, Upcard.fromLabel(upcard)) to Play.parse(code).copy(debated = isDebated)
        }

        return squares.toMap().also { require(it.size == squares.size) { "$name lists a square twice" } }
    }
}
