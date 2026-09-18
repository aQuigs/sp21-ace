package com.aquigs.sp21ace.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

private val TILE_ACTIONS = listOf(listOf(Action.HIT, Action.STAND), listOf(Action.DOUBLE, Action.SURRENDER))
private val SwatchShape = RoundedCornerShape(3.dp)

/** A shortcut to the strategy chart: four of its colours on a small raised tile, as large as [modifier] makes it. */
@Composable
fun ChartTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sp21AceTheme.colors.chart
    val description = stringResource(R.string.open_strategy_chart)

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceBright,
        shadowElevation = 3.dp,
    ) {
        ProvideDefaultFontScale {
            // The letters are for the eye; a screen reader says what the tile does
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp).clearAndSetSemantics {},
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TILE_ACTIONS.forEach { row ->
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        row.forEach { action ->
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(SwatchShape).actionFill(action, colors),
                                contentAlignment = Alignment.Center,
                            ) {
                                // The body style's 24 sp line is taller than the square and sets the letter low
                                Text(text = action.code, fontSize = 9.sp, lineHeight = 1.em, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
