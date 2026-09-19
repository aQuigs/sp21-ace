package com.aquigs.sp21ace.ui.chart

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
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.domain.strategy.legend
import com.aquigs.sp21ace.ui.components.ChartGrid
import com.aquigs.sp21ace.ui.components.CodeSquare
import com.aquigs.sp21ace.ui.components.DoubledTabbedPages
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

@Composable
fun StrategyChartScreen(rules: RuleSet, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val chart = StrategyCharts.forRules(rules)
    var chosen by rememberSaveable { mutableStateOf(ChartTable.HARD) }
    val legends = remember(chart) { chart.tables.associateWith(chart::legend) }

    SubPage(title = stringResource(R.string.strategy_chart), onBack = onBack, modifier = modifier) { padding ->
        DoubledTabbedPages(
            tabs = chart.tables,
            doubled = ChartTable::afterDoubling,
            selected = chosen,
            onSelect = { chosen = it },
            title = { it.tabTitle },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { table ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Caption(rulesCaption(rules))
                if (table.afterDoubling) Caption(stringResource(if (rules.redoubling) R.string.doubled_moves_redoubling else R.string.doubled_moves))
                ChartGrid(
                    chart = chart,
                    table = table,
                    hands = chart.hands(table),
                    footerCodes = legends.values.flatten().map { it.symbol },
                    modifier = Modifier.widthIn(max = MaxContentWidth),
                    square = { square, play, codeStyle, squareModifier -> Square(square, play, codeStyle, squareModifier) },
                    footer = { swatchSize, _, codeStyle -> Legend(legends.getValue(table), swatchSize, codeStyle) },
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun rulesCaption(rules: RuleSet): String = listOfNotNull(
    if (rules.dealerHitsSoft17) R.string.rules_dealer_hits_soft_17 else R.string.rules_dealer_stands_soft_17,
    R.string.rules_redoubling.takeIf { rules.redoubling },
    // Every published chart is for six decks
    R.string.rules_six_decks,
).map { stringResource(it) }.joinToString(" · ")

@Composable
private fun Square(square: ChartSquare, play: Play, codeStyle: TextStyle, modifier: Modifier) {
    val description = stringResource(R.string.square_description, square.row.hand, square.upcard.label, square.inPlainWords(play))

    // In words, because a screen reader can't see the row and column a code sits in, or the legend that explains it
    ActionSquare(
        code = play.code,
        fill = play.action,
        style = codeStyle,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    )
}

/** A code on its action's colour, or a mark on its own. */
@Composable
private fun ActionSquare(code: String, fill: Action?, style: TextStyle, modifier: Modifier = Modifier) {
    CodeSquare(code, style, if (fill != null) modifier.actionFill(fill, Sp21AceTheme.colors.chart) else modifier)
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

/** A table's name on its own, as Customize Hands' switches name it. The chart's tabs name it within its group as [tabTitle]. */
internal val ChartTable.title: Int
    get() = when (this) {
        ChartTable.HARD -> R.string.table_hard
        ChartTable.SOFT -> R.string.table_soft
        ChartTable.PAIRS -> R.string.table_pairs
        ChartTable.AFTER_DOUBLE_HARD -> R.string.table_after_double_hard
        ChartTable.AFTER_DOUBLE_SOFT -> R.string.table_after_double_soft
    }

/** A table's name under the choice between hands not yet doubled and hands already doubled, which says which of the two it is. */
internal val ChartTable.tabTitle: Int
    get() = when (this) {
        ChartTable.HARD, ChartTable.AFTER_DOUBLE_HARD -> R.string.table_hard
        ChartTable.SOFT, ChartTable.AFTER_DOUBLE_SOFT -> R.string.table_soft
        ChartTable.PAIRS -> R.string.table_pairs
    }
