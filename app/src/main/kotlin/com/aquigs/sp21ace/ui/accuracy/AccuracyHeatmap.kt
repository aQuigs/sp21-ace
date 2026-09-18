package com.aquigs.sp21ace.ui.accuracy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.dealing.dealtRows
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.ui.components.ChartGrid
import com.aquigs.sp21ace.ui.components.CodeSquare
import com.aquigs.sp21ace.ui.theme.HeatmapColors
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

// Blackjack Ace's eight steps, only the ends labelled
private val SCALE = listOf("0", "", "", "", "", "", "", "100")

/**
 * [table] as [rules]' chart prints it, in the rows the trainer deals, each square with answers filled by how often they were
 * right, over a scale from 0 to 100.
 */
@Composable
fun AccuracyHeatmap(rules: RuleSet, table: ChartTable, bySquare: Map<ChartSquare, Tally>, modifier: Modifier = Modifier) {
    val chart = StrategyCharts.forRules(rules)
    val rows = dealtRows(rules)
    val colors = Sp21AceTheme.colors.heatmap

    // As in Blackjack Ace, a row no dealt hand is read from, such as hard 21, would only ever stay blank
    ChartGrid(
        chart = chart,
        table = table,
        hands = chart.hands(table).filter { ChartRow(table, it) in rows },
        footerCodes = SCALE,
        modifier = modifier,
        square = { square, play, codeStyle, squareModifier -> HeatSquare(square, play, bySquare[square], colors, codeStyle, squareModifier) },
        footer = { swatchSize, gap, codeStyle -> Scale(colors, swatchSize, gap, codeStyle) },
    )
}

@Composable
private fun HeatSquare(square: ChartSquare, play: Play?, tally: Tally?, colors: HeatmapColors, codeStyle: TextStyle, modifier: Modifier) {
    val hand = square.row.hand
    val upcard = square.upcard.label
    val words = square.inPlainWords(play)
    val permille = tally?.accuracyPermille
    val description = if (permille == null) {
        stringResource(R.string.square_no_answers, hand, upcard, words)
    } else {
        stringResource(R.string.square_accuracy, hand, upcard, words, permille / 10)
    }

    // In words, because a screen reader can't see the row and column a square sits in, or its colour
    CodeSquare(
        code = play?.code.orEmpty(),
        style = codeStyle,
        modifier = modifier
            .then(if (permille == null) Modifier else Modifier.heat(colors.at(permille / 1000f), MaterialTheme.colorScheme.outlineVariant))
            .semantics(mergeDescendants = true) { contentDescription = description },
        color = if (permille == null) Sp21AceTheme.colors.unansweredCode else Color.Unspecified,
    )
}

@Composable
private fun Scale(colors: HeatmapColors, swatchSize: Dp, gap: Dp, codeStyle: TextStyle) {
    val outline = MaterialTheme.colorScheme.outlineVariant

    // For the eye only, since every square already says in words how often it was right
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
    ) {
        SCALE.forEachIndexed { step, label ->
            CodeSquare(label, codeStyle, Modifier.size(swatchSize).heat(colors.at(step / SCALE.lastIndex.toFloat()), outline))
        }
    }
}

// The light theme's middle step is all but the page's colour, so the outline tells a square about half right from one without answers
private fun Modifier.heat(fill: Color, outline: Color): Modifier = background(fill).border(Dp.Hairline, outline)
