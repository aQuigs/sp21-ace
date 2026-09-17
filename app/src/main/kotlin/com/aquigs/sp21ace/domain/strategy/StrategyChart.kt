package com.aquigs.sp21ace.domain.strategy

/** The rule combinations with published charts, all for six decks. */
enum class RuleSet { H17_REDOUBLE, H17, S17 }

enum class ChartTable { HARD, SOFT, PAIRS, RESCUE, AFTER_DOUBLE_HARD, AFTER_DOUBLE_SOFT }

enum class Upcard(val label: String) {
    TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), ACE("A");

    companion object {
        fun fromLabel(label: String): Upcard =
            requireNotNull(entries.firstOrNull { it.label == label }) { "Unknown upcard: $label" }
    }
}

/** Hands are keyed as the charts print them: "16" for hard totals, "A-7" for soft totals, "8-8" for pairs. */
class StrategyChart(private val tables: Map<ChartTable, Map<String, Map<Upcard, Play>>>) {
    val tableIds: Set<ChartTable> get() = tables.keys

    fun hands(table: ChartTable): List<String> = tables[table]?.keys?.toList().orEmpty()

    fun play(table: ChartTable, hand: String, upcard: Upcard): Play? = tables[table]?.get(hand)?.get(upcard)

    companion object {
        // Sparse tables such as Double Down Rescue only print the squares where their play applies
        private const val NO_PLAY = "."
        private val WHITESPACE = Regex("\\s+")

        fun parse(grids: Map<ChartTable, String>): StrategyChart =
            StrategyChart(grids.mapValues { (table, grid) -> parseGrid(table, grid) })

        private fun parseGrid(table: ChartTable, grid: String): Map<String, Map<Upcard, Play>> {
            val rows = grid.lines().map(String::trim).filter(String::isNotEmpty).map { it.split(WHITESPACE) }
            val upcards = rows.first().drop(1).map(Upcard::fromLabel)

            return rows.drop(1).associate { row ->
                require(row.size == upcards.size + 1) { "$table ${row.first()} has ${row.size - 1} squares, expected ${upcards.size}" }
                row.first() to upcards.zip(row.drop(1))
                    .filter { (_, code) -> code != NO_PLAY }
                    .associate { (upcard, code) -> upcard to Play.parse(code) }
            }
        }
    }
}

object StrategyCharts {
    private val charts = RuleSet.entries.associateWith { StrategyChart.parse(CHART_GRIDS.getValue(it)) }

    fun forRules(ruleSet: RuleSet): StrategyChart = charts.getValue(ruleSet)
}
