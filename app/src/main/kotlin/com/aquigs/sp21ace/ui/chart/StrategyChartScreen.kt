package com.aquigs.sp21ace.ui.chart

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.LegendEntry
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.domain.strategy.legend
import com.aquigs.sp21ace.ui.components.ChartGrid
import com.aquigs.sp21ace.ui.components.CodeSquare
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.PageTabRow
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

@Composable
fun StrategyChartScreen(rules: RuleSet, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val chart = StrategyCharts.forRules(rules)
    var chosen by rememberSaveable { mutableStateOf(ChartTable.HARD) }
    // A tab the rules have since dropped, such as After doubling: soft once redoubling isn't allowed, falls back to the first
    val selected = chosen.takeIf { it in chart.tables } ?: chart.tables.first()
    val legends = remember(chart) { chart.tables.associateWith(chart::legend) }

    SubPage(title = stringResource(R.string.strategy_chart), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Scrollable, because the tables after Hard, Soft and Pairs have long names
            PageTabRow(tabs = chart.tables, selected = selected, onSelect = { chosen = it }, title = chart::title, scrollable = true)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = rulesCaption(rules),
                    modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                ChartGrid(
                    chart = chart,
                    table = selected,
                    hands = chart.hands(selected),
                    footerCodes = legends.values.flatten().map { it.symbol },
                    modifier = Modifier.widthIn(max = MaxContentWidth),
                    square = { square, play, codeStyle, squareModifier -> Square(square, play, codeStyle, squareModifier) },
                    footer = { swatchSize, _, codeStyle -> Legend(legends.getValue(selected), swatchSize, codeStyle) },
                )
            }
        }
    }
}

@Composable
private fun rulesCaption(rules: RuleSet): String = listOfNotNull(
    if (rules.dealerHitsSoft17) R.string.rules_dealer_hits_soft_17 else R.string.rules_dealer_stands_soft_17,
    R.string.rules_redoubling.takeIf { rules.redoubling },
    // Every published chart is for six decks
    R.string.rules_six_decks,
).map { stringResource(it) }.joinToString(" · ")

@Composable
private fun Square(square: ChartSquare, play: Play?, codeStyle: TextStyle, modifier: Modifier) {
    val description = stringResource(R.string.square_description, square.row.hand, square.upcard.label, square.inPlainWords(play))

    // In words, because a screen reader can't see the row and column a code sits in, or the legend that explains it
    ActionSquare(
        code = play?.code.orEmpty(),
        fill = play?.action,
        style = codeStyle,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    )
}

/** A code on its action's colour, a mark on its own, or an outlined empty square where a table prints nothing. */
@Composable
private fun ActionSquare(code: String, fill: Action?, style: TextStyle, modifier: Modifier = Modifier) {
    val background = when {
        fill != null -> Modifier.actionFill(fill, Sp21AceTheme.colors.chart)
        code.isEmpty() -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> Modifier
    }

    CodeSquare(code, style, modifier.then(background))
}

@Composable
private fun Legend(entries: List<LegendEntry>, swatchSize: Dp, codeStyle: TextStyle) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.forEach { entry ->
            // One item for a screen reader: the symbol, then what it means
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionSquare(entry.symbol, entry.fill, codeStyle, Modifier.size(swatchSize))
                Text(text = entry.meaning, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

internal val ChartTable.title: Int
    get() = when (this) {
        ChartTable.HARD -> R.string.table_hard
        ChartTable.SOFT -> R.string.table_soft
        ChartTable.PAIRS -> R.string.table_pairs
        ChartTable.AFTER_DOUBLE_HARD -> R.string.table_after_double_hard
        ChartTable.AFTER_DOUBLE_SOFT -> R.string.table_after_double_soft
    }

/** A table's name as [this] chart prints it: without redoubling, After doubling: hard is the table the charts call Double Down Rescue. */
internal fun StrategyChart.title(table: ChartTable): Int =
    if (table == ChartTable.AFTER_DOUBLE_HARD && !redoubling) R.string.table_rescue else table.title
