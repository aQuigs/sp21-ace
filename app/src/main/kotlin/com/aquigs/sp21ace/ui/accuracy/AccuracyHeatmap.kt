package com.aquigs.sp21ace.ui.accuracy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.aquigs.sp21ace.ui.components.CodeText
import com.aquigs.sp21ace.ui.components.HEAT_SCALE
import com.aquigs.sp21ace.ui.components.HeatScale
import com.aquigs.sp21ace.ui.components.HeatSquare

/**
 * [table] as [rules]' chart prints it, in the rows the trainer deals, each square with answers filled by
 * how often they were right, over a scale from 0 to 100.
 */
@Composable
fun AccuracyHeatmap(rules: RuleSet, table: ChartTable, bySquare: Map<ChartSquare, Tally>, modifier: Modifier = Modifier) {
    val chart = StrategyCharts.forRules(rules)
    val rows = dealtRows(rules)

    // As in Blackjack Ace, a row no dealt hand is read from, such as hard 21, would only ever stay blank
    ChartGrid(
        chart = chart,
        table = table,
        hands = chart.hands(table).filter { ChartRow(table, it) in rows },
        footerCodes = HEAT_SCALE,
        modifier = modifier,
        square = { square, play, codes, squareModifier -> AccuracySquare(square, play, bySquare[square], codes, squareModifier) },
        footer = { swatchSize, gap, codes -> HeatScale(swatchSize, gap, codes) },
    )
}

@Composable
private fun AccuracySquare(square: ChartSquare, play: Play, tally: Tally?, codes: CodeText, modifier: Modifier) {
    val hand = square.row.hand
    val upcard = square.upcard.label
    val words = square.inPlainWords(play)
    val permille = tally?.accuracyPermille
    val description = if (permille == null) {
        stringResource(R.string.square_no_answers, hand, upcard, words)
    } else {
        stringResource(R.string.square_accuracy, hand, upcard, words, permille / 10)
    }

    HeatSquare(play.code, tally, description, codes, modifier)
}
