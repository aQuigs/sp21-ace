package com.aquigs.sp21ace.domain.strategy

/** The shoe size every chart is published for. */
const val DECKS = 6

/** The rule combinations with published charts, all for [DECKS] decks. Casinos only offer redoubling where the dealer hits soft 17. */
enum class RuleSet(val dealerHitsSoft17: Boolean, val redoubling: Boolean) {
    H17_REDOUBLE(dealerHitsSoft17 = true, redoubling = true),
    H17(dealerHitsSoft17 = true, redoubling = false),
    S17(dealerHitsSoft17 = false, redoubling = false),
}

/** [afterDoubling] marks the tables for a hand already doubled, where D is a redouble and R a rescue. */
enum class ChartTable(val afterDoubling: Boolean = false) {
    HARD,
    SOFT,
    PAIRS,

    /**
     * Plays for a hard hand already doubled. Without redoubling the charts print it as Double Down Rescue, hard 12 to 17 only,
     * where a hand with no row means no rescue: stand on the doubled hand.
     */
    AFTER_DOUBLE_HARD(afterDoubling = true),
    AFTER_DOUBLE_SOFT(afterDoubling = true),
}

enum class Upcard(val label: String) {
    TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), ACE("A");

    companion object {
        fun fromLabel(label: String): Upcard =
            requireNotNull(entries.firstOrNull { it.label == label }) { "Unknown upcard: $label" }
    }
}

/**
 * Hands are keyed as the charts print them: "16" for hard totals, "A-7" for soft totals, "8-8" for pairs.
 * [play] is null wherever the chart prints nothing: a table this rule set doesn't have, or a hand without a row.
 */
class StrategyChart(private val squares: Map<ChartTable, Map<String, Map<Upcard, Play>>>, val redoubling: Boolean) {
    /** The tables this rule set prints, in chart order. */
    val tables: List<ChartTable> = ChartTable.entries.filter { squares[it].orEmpty().isNotEmpty() }

    fun hands(table: ChartTable): List<String> = squares[table]?.keys?.toList().orEmpty()

    fun printsRow(row: ChartRow): Boolean = squares[row.table]?.containsKey(row.hand) == true

    fun play(table: ChartTable, hand: String, upcard: Upcard): Play? = squares[table]?.get(hand)?.get(upcard)

    /** Every square [table] prints, row by row, which is every upcard of every row. */
    fun plays(table: ChartTable): List<Play> = squares[table].orEmpty().values.flatMap { it.values }

    companion object {
        private val WHITESPACE = Regex("\\s+")

        fun parse(grids: Map<ChartTable, String>, redoubling: Boolean): StrategyChart =
            StrategyChart(grids.mapValues { (table, grid) -> parseGrid(table, grid) }, redoubling)

        private fun parseGrid(table: ChartTable, grid: String): Map<String, Map<Upcard, Play>> {
            val rows = grid.lines().map(String::trim).filter(String::isNotEmpty).map { it.split(WHITESPACE) }
            val header = requireNotNull(rows.firstOrNull()) { "$table has no header row" }
            require(header.drop(1) == Upcard.entries.map(Upcard::label)) { "$table header must list every upcard from 2 to A" }
            val body = rows.drop(1)

            return body.associate { row ->
                require(row.size == Upcard.entries.size + 1) { "$table ${row.first()} has ${row.size - 1} squares, expected ${Upcard.entries.size}" }
                row.first() to Upcard.entries.zip(row.drop(1)).associate { (upcard, code) -> upcard to Play.parse(code) }
            }.also { require(it.size == body.size) { "$table repeats a hand" } }
        }
    }
}

object StrategyCharts {
    private val charts = RuleSet.entries.associateWith { StrategyChart.parse(CHART_GRIDS.getValue(it), it.redoubling) }

    fun forRules(ruleSet: RuleSet): StrategyChart = charts.getValue(ruleSet)
}
