package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
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
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.play

private val RowLabelWidth = 44.dp
private val RowLabelPadding = 4.dp
private val Gap = 2.dp
private val CodePadding = 1.dp
private val MaxCodeSize = 13.sp

private enum class GridPart { Caption, Grid }

/**
 * [table]'s squares in the rows for [hands], each drawn by [square] from its square and play into the modifier that sizes it.
 * "Your hand" reads up beside the rows from the first one down, "Dealer's upcard" heads the columns, and [footer] follows the
 * squares, given their size and the gap between them. Every code, and each of the [footerCodes] the footer prints, gets the
 * one size that fits a square.
 */
@Composable
internal fun ChartGrid(
    chart: StrategyChart,
    table: ChartTable,
    hands: List<String>,
    footerCodes: List<String>,
    modifier: Modifier = Modifier,
    square: @Composable (square: ChartSquare, play: Play, codeStyle: TextStyle, modifier: Modifier) -> Unit,
    footer: @Composable (squareSize: Dp, gap: Dp, codeStyle: TextStyle) -> Unit,
) {
    val codeStyle = MaterialTheme.typography.labelLarge.copy(fontSize = MaxCodeSize)
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val widest = rememberWidestText(chart, footerCodes, measurer, codeStyle, labelStyle)

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
                        // Handed over from this row rather than left to what the slot captures: a changed slot re-runs in the
                        // rows already drawn, which still hold the previous table's hands, before the rows follow the new table
                        val row = ChartRow(table, hand)
                        Upcard.entries.forEach { upcard ->
                            square(ChartSquare(row, upcard), chart.play(row, upcard), codes, Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
                Box(modifier = Modifier.padding(start = RowLabelWidth + Gap, top = 16.dp)) { footer(squareWidth.toDp(), Gap, codes) }
            }
        }.map { it.measure(beside) }

        layout(constraints.maxWidth, header.height + maxOf(caption.height, body.height)) {
            header.placeRelative(caption.width, 0)
            caption.placeRelative(0, header.height)
            body.placeRelative(caption.width, header.height)
        }
    }
}

/** The widest code, upcard and hand of every table, footer codes included, so text keeps one size from tab to tab. */
private class WidestText(val code: String, val upcard: String, val hand: String)

@Composable
private fun rememberWidestText(
    chart: StrategyChart,
    footerCodes: List<String>,
    measurer: TextMeasurer,
    codeStyle: TextStyle,
    labelStyle: TextStyle,
): WidestText = remember(chart, footerCodes, measurer, codeStyle, labelStyle) {
    fun widest(texts: List<String>, style: TextStyle) =
        texts.distinct().maxBy { measurer.measure(it, style, softWrap = false, maxLines = 1).size.width }

    val codes = footerCodes + chart.tables.flatMap(chart::plays).map { it.code }

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

/** A code centred over whatever [modifier] draws, padded as the grid's one code size allows for. */
@Composable
internal fun CodeSquare(code: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text = code, modifier = Modifier.padding(horizontal = CodePadding), color = color, softWrap = false, maxLines = 1, style = style)
    }
}
