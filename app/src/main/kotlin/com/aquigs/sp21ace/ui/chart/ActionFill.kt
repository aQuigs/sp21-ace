package com.aquigs.sp21ace.ui.chart

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.ui.theme.ChartColors

internal fun Modifier.actionFill(action: Action, colors: ChartColors): Modifier = when (action) {
    Action.HIT -> background(colors.hit)
    Action.STAND -> background(colors.stand)
    Action.DOUBLE -> background(colors.double)
    Action.SPLIT -> background(colors.split)
    Action.SURRENDER -> background(colors.surrender)
    // Split corner to corner, surrender first, so the square shows both halves of the play
    Action.SURRENDER_OR_HIT -> drawWithCache {
        val surrenderHalf = Path().apply {
            lineTo(size.width, 0f)
            lineTo(0f, size.height)
            close()
        }

        onDrawBehind {
            drawRect(colors.hit)
            drawPath(surrenderHalf, colors.surrender)
        }
    }
}
