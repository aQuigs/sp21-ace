package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Test

// The fixtures list every square with the sources that print its play, so a square can't change without its fixture row and every square cites a source
class ChartCellsTest {
    @Test
    fun everyRuleSetMatchesItsFixture() {
        val mismatches = RuleSet.entries.flatMap { ruleSet ->
            val expected = fixtureSquares(ruleSet)
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

    private fun fixtureSquares(ruleSet: RuleSet): Map<Square, Play> {
        val squares = Fixtures.rows(ruleSet).map { fields ->
            val (table, hand, upcard, code, debated) = fields
            val sources = fields[5]
            require(sources.isNotBlank()) { "$ruleSet: no source cited in $fields" }
            val isDebated = when (debated) {
                "yes" -> true
                "no" -> false
                else -> error("$ruleSet: debated must be yes or no in $fields")
            }

            Square(ChartTable.valueOf(table), hand, Upcard.fromLabel(upcard)) to Play.parse(code).copy(debated = isDebated)
        }

        return squares.toMap().also { require(it.size == squares.size) { "$ruleSet lists a square twice" } }
    }
}
