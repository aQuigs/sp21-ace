package com.aquigs.sp21ace.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.BonusException
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

private val RowLabelWidth = 44.dp
private val Gap = 2.dp
private val SwatchSize = 32.dp

// A phone's width makes legible squares; any wider and a tablet or a landscape phone blows them up past a screenful
private val MaxGridWidth = 480.dp

@Composable
fun StrategyChartScreen(chart: StrategyChart, rules: RuleSet, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tables = ChartTable.entries.filter { chart.hands(it).isNotEmpty() }
    var selected by rememberSaveable { mutableStateOf(ChartTable.HARD) }

    SubPage(title = stringResource(R.string.strategy_chart), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Scrollable, because the tables after Hard, Soft and Pairs have long names
            SecondaryScrollableTabRow(selectedTabIndex = tables.indexOf(selected), edgePadding = 0.dp, minTabWidth = 72.dp) {
                tables.forEach { table ->
                    Tab(
                        selected = table == selected,
                        onClick = { selected = table },
                        text = { Text(stringResource(table.title)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
                    modifier = Modifier.widthIn(max = MaxGridWidth).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                ChartGrid(chart, selected, Modifier.widthIn(max = MaxGridWidth))
            }
        }
    }
}

@Composable
private fun rulesCaption(rules: RuleSet): String {
    val soft17AndDoubling = when (rules) {
        RuleSet.H17_REDOUBLE -> listOf(R.string.rules_dealer_hits_soft_17, R.string.rules_redoubling)
        RuleSet.H17 -> listOf(R.string.rules_dealer_hits_soft_17)
        RuleSet.S17 -> listOf(R.string.rules_dealer_stands_soft_17)
    }

    // Every published chart is for six decks
    return (soft17AndDoubling + R.string.rules_six_decks).map { stringResource(it) }.joinToString(" · ")
}

/** "Your hand" reads up beside the rows from the first one down, "Dealer's upcard" heads the columns, and the legend follows the squares. */
@Composable
private fun ChartGrid(chart: StrategyChart, table: ChartTable, modifier: Modifier = Modifier) {
    val hands = chart.hands(table)

    Layout(
        content = {
            AxisCaption(stringResource(R.string.your_hand), Modifier.padding(end = 4.dp).readingUp())

            Column(modifier = Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AxisCaption(stringResource(R.string.dealers_upcard), Modifier.padding(start = RowLabelWidth + Gap))
                GridRow(label = null) {
                    Upcard.entries.forEach {
                        Text(
                            text = it.label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                hands.forEach { hand ->
                    GridRow(label = hand) {
                        Upcard.entries.forEach { upcard -> Square(hand, upcard, chart.play(table, hand, upcard)) }
                    }
                }
                Legend(
                    plays = hands.flatMap { hand -> Upcard.entries.mapNotNull { chart.play(table, hand, it) } },
                    modifier = Modifier.padding(start = RowLabelWidth + Gap, top = 16.dp),
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val (captionMeasurable, headerMeasurable, bodyMeasurable) = measurables
        val caption = captionMeasurable.measure(Constraints())
        val beside = Constraints(maxWidth = (constraints.maxWidth - caption.width).coerceAtLeast(0))
        val header = headerMeasurable.measure(beside)
        val body = bodyMeasurable.measure(beside)

        layout(constraints.maxWidth, header.height + maxOf(caption.height, body.height)) {
            header.placeRelative(caption.width, 0)
            caption.placeRelative(0, header.height)
            body.placeRelative(caption.width, header.height)
        }
    }
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
private fun GridRow(label: String?, squares: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(RowLabelWidth).padding(end = 4.dp), contentAlignment = Alignment.CenterEnd) {
            label?.let {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        squares()
    }
}

@Composable
private fun RowScope.Square(hand: String, upcard: Upcard, play: Play?) {
    val size = Modifier.weight(1f).aspectRatio(1f)

    if (play == null) {
        Spacer(size)
    } else {
        val code = play.code
        val description = stringResource(R.string.square_description, hand, upcard.label, code)

        // A code alone says nothing to a screen reader, which can't see the row and column it sits in
        Box(
            modifier = size
                .actionFill(play.action, Sp21AceTheme.colors.chart)
                .semantics(mergeDescendants = true) { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = code,
                modifier = Modifier.padding(horizontal = 1.dp),
                autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 13.sp),
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// As in Blackjack Ace, the legend lists only what the table uses
@Composable
private fun Legend(plays: List<Play>, modifier: Modifier = Modifier) {
    val cardCounts = plays.mapNotNull { it.hitWithCards }.sorted()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        plays.map { it.action }.distinct().sorted().forEach { action ->
            LegendEntry(action.code, stringResource(action.legendLabel), fill = action)
        }
        if (cardCounts.isNotEmpty()) {
            LegendEntry(listOf(cardCounts.first(), cardCounts.last()).distinct().joinToString("-"), stringResource(R.string.legend_card_count))
        }
        plays.mapNotNull { it.bonusException }.distinct().sorted().forEach { exception ->
            LegendEntry(exception.mark, stringResource(exception.legendLabel))
        }
        if (plays.any { it.debated }) {
            LegendEntry("†", stringResource(R.string.legend_debated))
        }
    }
}

@Composable
private fun LegendEntry(symbol: String, label: String, fill: Action? = null) {
    val swatch = fill?.let { Modifier.actionFill(it, Sp21AceTheme.colors.chart) } ?: Modifier

    // One item for a screen reader: the symbol, then what it means
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = swatch.size(SwatchSize), contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 13.sp),
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

private val ChartTable.title: Int
    get() = when (this) {
        ChartTable.HARD -> R.string.table_hard
        ChartTable.SOFT -> R.string.table_soft
        ChartTable.PAIRS -> R.string.table_pairs
        ChartTable.RESCUE -> R.string.table_rescue
        ChartTable.AFTER_DOUBLE_HARD -> R.string.table_after_double_hard
        ChartTable.AFTER_DOUBLE_SOFT -> R.string.table_after_double_soft
    }

private val Action.legendLabel: Int
    get() = when (this) {
        Action.HIT -> R.string.move_hit
        Action.STAND -> R.string.move_stand
        Action.DOUBLE -> R.string.move_double
        Action.SPLIT -> R.string.move_split
        Action.SURRENDER -> R.string.move_surrender
        Action.SURRENDER_OR_HIT -> R.string.legend_surrender_or_hit
    }

private val BonusException.legendLabel: Int
    get() = when (this) {
        BonusException.ANY_678 -> R.string.legend_any_678
        BonusException.SUITED_678 -> R.string.legend_suited_678
        BonusException.SPADED_678 -> R.string.legend_spaded_678
        BonusException.SUITED_777 -> R.string.legend_suited_777
    }
