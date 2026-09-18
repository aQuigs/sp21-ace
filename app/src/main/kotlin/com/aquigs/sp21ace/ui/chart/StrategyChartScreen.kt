package com.aquigs.sp21ace.ui.chart

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.LegendEntry
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.domain.strategy.legend
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.PageTabRow
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

private val RowLabelWidth = 44.dp
private val RowLabelPadding = 4.dp
private val Gap = 2.dp
private val CodePadding = 1.dp
private val MaxCodeSize = 13.sp

@Composable
fun StrategyChartScreen(rules: RuleSet, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val chart = StrategyCharts.forRules(rules)
    var chosen by rememberSaveable { mutableStateOf(ChartTable.HARD) }
    // A tab the rules have since dropped, such as Double Down Rescue once redoubling is allowed, falls back to the first
    val selected = chosen.takeIf { it in chart.tables } ?: chart.tables.first()

    SubPage(title = stringResource(R.string.strategy_chart), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Scrollable, because the tables after Hard, Soft and Pairs have long names
            PageTabRow(tabs = chart.tables, selected = selected, onSelect = { chosen = it }, title = { it.title }, scrollable = true)

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
                ChartGrid(chart, selected, Modifier.widthIn(max = MaxContentWidth))
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

private enum class GridPart { Caption, Grid }

/** "Your hand" reads up beside the rows from the first one down, "Dealer's upcard" heads the columns, and the legend follows the squares. */
@Composable
private fun ChartGrid(chart: StrategyChart, table: ChartTable, modifier: Modifier = Modifier) {
    val hands = chart.hands(table)
    val legend = remember(chart, table) { chart.legend(table) }
    val codeStyle = MaterialTheme.typography.labelLarge.copy(fontSize = MaxCodeSize)
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val widest = rememberWidestText(chart, measurer, codeStyle, labelStyle)

    SubcomposeLayout(modifier) { constraints ->
        val caption = subcompose(GridPart.Caption) {
            AxisCaption(stringResource(R.string.your_hand), Modifier.padding(end = 4.dp).readingUp())
        }.single().measure(Constraints())
        val beside = Constraints(maxWidth = (constraints.maxWidth - caption.width).coerceAtLeast(0))
        val squareWidth = ((beside.maxWidth - (RowLabelWidth + Gap * Upcard.entries.size).roundToPx()) / Upcard.entries.size).coerceAtLeast(0)

        // Every square is as wide as the next, so one size fits the widest text of each kind. Each square searching for
        // its own size costs a handful of text layouts apiece, and at a minimum size it silently cuts the text short.
        val codes = shrunkToFit(measurer, widest.code, codeStyle, squareWidth - (CodePadding * 2).roundToPx())
        val upcards = shrunkToFit(measurer, widest.upcard, labelStyle, squareWidth)
        val handLabels = shrunkToFit(measurer, widest.hand, labelStyle, (RowLabelWidth - RowLabelPadding).roundToPx())

        val (header, body) = subcompose(GridPart.Grid) {
            Column(modifier = Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AxisCaption(stringResource(R.string.dealers_upcard), Modifier.padding(start = RowLabelWidth + Gap))
                GridRow(label = null, labelStyle = handLabels) {
                    Upcard.entries.forEach {
                        Text(text = it.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, softWrap = false, maxLines = 1, style = upcards)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                hands.forEach { hand ->
                    GridRow(label = hand, labelStyle = handLabels) {
                        Upcard.entries.forEach { upcard -> Square(chart, table, hand, upcard, codes) }
                    }
                }
                Legend(legend, swatchSize = squareWidth.toDp(), codes, Modifier.padding(start = RowLabelWidth + Gap, top = 16.dp))
            }
        }.map { it.measure(beside) }

        layout(constraints.maxWidth, header.height + maxOf(caption.height, body.height)) {
            header.placeRelative(caption.width, 0)
            caption.placeRelative(0, header.height)
            body.placeRelative(caption.width, header.height)
        }
    }
}

/** The widest code, upcard and hand of every table, so text keeps one size from tab to tab. */
private class WidestText(val code: String, val upcard: String, val hand: String)

@Composable
private fun rememberWidestText(chart: StrategyChart, measurer: TextMeasurer, codeStyle: TextStyle, labelStyle: TextStyle): WidestText =
    remember(chart, measurer, codeStyle, labelStyle) {
        fun widest(texts: List<String>, style: TextStyle) =
            texts.distinct().maxBy { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width }

        val codes = chart.tables.flatMap { table ->
            chart.legend(table).map { it.symbol } + chart.hands(table).flatMap { hand -> Upcard.entries.mapNotNull { chart.play(table, hand, it)?.code } }
        }

        WidestText(
            code = widest(codes, codeStyle),
            upcard = widest(Upcard.entries.map { it.label }, labelStyle),
            hand = widest(chart.tables.flatMap { chart.hands(it) }, labelStyle),
        )
    }

// Scaled in pixels, because Android enlarges big font sizes less than small ones, and measured again, because glyphs
// don't narrow in exact proportion to the size
private fun Density.shrunkToFit(measurer: TextMeasurer, text: String, style: TextStyle, width: Int): TextStyle {
    var fitted = style
    var textWidth = measurer.measure(text, fitted, softWrap = false, maxLines = 1).size.width

    while (textWidth > width) {
        val ratio = width.coerceAtLeast(0).toFloat() / textWidth
        fitted = fitted.copy(fontSize = (fitted.fontSize.toPx() * ratio).toSp(), lineHeight = (fitted.lineHeight.toPx() * ratio).toSp())
        textWidth = measurer.measure(text, fitted, softWrap = false, maxLines = 1).size.width
    }
    return fitted
}

@Composable
private fun AxisCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
    )
}

// Laid out as tall as the text is wide, then turned a quarter to read up the side
private fun Modifier.readingUp(): Modifier = layout { measurable, _ ->
    val text = measurable.measure(Constraints())

    layout(text.height, text.width) {
        text.placeWithLayer((text.height - text.width) / 2, (text.width - text.height) / 2) { rotationZ = -90f }
    }
}

@Composable
private fun GridRow(label: String?, labelStyle: TextStyle, squares: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(RowLabelWidth).padding(end = RowLabelPadding), contentAlignment = Alignment.CenterEnd) {
            label?.let { Text(text = it, softWrap = false, maxLines = 1, style = labelStyle) }
        }
        squares()
    }
}

@Composable
private fun RowScope.Square(chart: StrategyChart, table: ChartTable, hand: String, upcard: Upcard, codeStyle: TextStyle) {
    val play = chart.play(table, hand, upcard)
    val description = stringResource(R.string.square_description, hand, upcard.label, chart.inPlainWords(table, hand, upcard))

    // In words, because a screen reader can't see the row and column a code sits in, or the legend that explains it
    CodeSquare(
        code = play?.code.orEmpty(),
        fill = play?.action,
        style = codeStyle,
        modifier = Modifier.weight(1f).aspectRatio(1f).semantics(mergeDescendants = true) { contentDescription = description },
    )
}

/** A code on its action's colour, a mark on its own, or an outlined empty square where a table prints nothing. */
@Composable
private fun CodeSquare(code: String, fill: Action?, style: TextStyle, modifier: Modifier = Modifier) {
    val background = when {
        fill != null -> Modifier.actionFill(fill, Sp21AceTheme.colors.chart)
        code.isEmpty() -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> Modifier
    }

    Box(modifier = modifier.then(background), contentAlignment = Alignment.Center) {
        Text(text = code, modifier = Modifier.padding(horizontal = CodePadding), softWrap = false, maxLines = 1, style = style)
    }
}

@Composable
private fun Legend(entries: List<LegendEntry>, swatchSize: Dp, codeStyle: TextStyle, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.forEach { entry ->
            // One item for a screen reader: the symbol, then what it means
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CodeSquare(entry.symbol, entry.fill, codeStyle, Modifier.size(swatchSize))
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
        ChartTable.RESCUE -> R.string.table_rescue
        ChartTable.AFTER_DOUBLE_HARD -> R.string.table_after_double_hard
        ChartTable.AFTER_DOUBLE_SOFT -> R.string.table_after_double_soft
    }
