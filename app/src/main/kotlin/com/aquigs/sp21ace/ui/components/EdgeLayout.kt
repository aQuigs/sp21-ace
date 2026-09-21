package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.ui.chart.ChartTile

/** The trainer's and the table's [parts] left to right, or right to left with the buttons on the left, so the buttons keep to the edge. */
@Composable
fun EdgeRow(buttonsOnLeft: Boolean, parts: List<@Composable RowScope.() -> Unit>, modifier: Modifier = Modifier) {
    // Left and Right name sides of the screen, so a right-to-left language mustn't reverse the row
    Row(modifier = modifier, horizontalArrangement = Arrangement.Absolute.Left) {
        (if (buttonsOnLeft) parts.reversed() else parts).forEach { part -> part() }
    }
}

/**
 * The chart tile, when [chartTile] is on, and whatever goes [underTile], over the [buttons] down the screen's edge. As wide as a
 * button, the tile takes no room from the cards.
 */
@Composable
fun EdgeControls(
    buttonsOnLeft: Boolean,
    chartTile: Boolean,
    onOpenChart: () -> Unit,
    underTile: @Composable () -> Unit = {},
    buttons: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = if (buttonsOnLeft) AbsoluteAlignment.Left else AbsoluteAlignment.Right) {
        if (chartTile) ChartTile(onClick = onOpenChart, modifier = Modifier.size(CircleButtonSize))
        underTile()
        // Holds the buttons at the bottom whether or not the tile shows
        Spacer(Modifier.weight(1f))
        ProvideDefaultFontScale {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = buttons)
        }
    }
}
