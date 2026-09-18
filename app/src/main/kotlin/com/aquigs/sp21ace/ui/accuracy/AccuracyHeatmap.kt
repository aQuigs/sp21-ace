package com.aquigs.sp21ace.ui.accuracy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.ui.chart.ChartGrid
import com.aquigs.sp21ace.ui.chart.CodeSquare
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.theme.HeatmapColors
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

// Blackjack Ace's eight steps, only the ends labelled
private val SCALE = listOf("0", "", "", "", "", "", "", "100")
private val SwatchGap = 2.dp

/** [table] as [chart] prints it, each square with answers filled by how often they were right, over a scale from 0 to 100. */
@Composable
fun AccuracyHeatmap(chart: StrategyChart, table: ChartTable, bySquare: Map<ChartSquare, Tally>, modifier: Modifier = Modifier) {
    val colors = Sp21AceTheme.colors.heatmap

    ChartGrid(
        chart = chart,
        table = table,
        footerCodes = SCALE,
        modifier = modifier,
        square = { hand, upcard, codeStyle, squareModifier ->
            val square = ChartSquare(ChartRow(table, hand), upcard)
            HeatSquare(chart, square, bySquare[square], colors, codeStyle, squareModifier)
        },
        footer = { swatchSize, codeStyle -> Scale(colors, swatchSize, codeStyle) },
    )
}

@Composable
private fun HeatSquare(chart: StrategyChart, square: ChartSquare, tally: Tally?, colors: HeatmapColors, codeStyle: TextStyle, modifier: Modifier) {
    val (table, hand) = square.row
    val upcard = square.upcard.label
    val play = requireNotNull(chart.play(table, hand, square.upcard)) { "No chart square for $hand vs $upcard" }
    val move = stringResource(play.firstMove.displayName)
    val permille = tally?.accuracyPermille
    val description = if (permille == null) {
        stringResource(R.string.square_no_answers, hand, upcard, move)
    } else {
        stringResource(R.string.square_accuracy, hand, upcard, move, permille / 10)
    }

    // In words, because a screen reader can't see the row and column a square sits in, or its colour
    CodeSquare(
        code = play.code,
        style = codeStyle,
        modifier = modifier
            .then(if (permille == null) Modifier else Modifier.background(colors.at(permille / 1000f)))
            .semantics(mergeDescendants = true) { contentDescription = description },
        color = if (permille == null) MaterialTheme.colorScheme.outline else Color.Unspecified,
    )
}

@Composable
private fun Scale(colors: HeatmapColors, swatchSize: Dp, codeStyle: TextStyle) {
    // For the eye only, since every square already says in words how often it was right
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(SwatchGap, Alignment.CenterHorizontally),
    ) {
        SCALE.forEachIndexed { step, label ->
            CodeSquare(label, codeStyle, Modifier.size(swatchSize).background(colors.at(step / SCALE.lastIndex.toFloat())))
        }
    }
}
