package com.aquigs.sp21ace.ui.trainer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.trainer.STREAK_RUNGS
import com.aquigs.sp21ace.domain.trainer.streakRung
import kotlin.math.roundToInt

private val CircleSize = 36.dp
private val DotSize = 8.dp
private val RailWidth = 3.dp
private val Gap = 4.dp

/** A ladder of doubling rungs as tall as the space given, with the streak circled on the highest rung it has reached. */
@Composable
fun StreakMeter(streak: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val reached = STREAK_RUNGS.indexOf(streakRung(streak))
    // Grey until the first right answer, as in Blackjack Ace
    val lit = streak > 0
    val accent = if (lit) scheme.secondary else scheme.outlineVariant
    val description = stringResource(R.string.streak_count, streak)

    Layout(
        content = {
            STREAK_RUNGS.forEach { Text(text = "$it", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }

            Spacer(Modifier.drawBehind { drawRail(reached, accent, scheme.outlineVariant) })

            Box(modifier = Modifier.background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Text(
                    text = "$streak",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = if (lit) scheme.onSecondary else scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 14.sp),
                    maxLines = 1,
                )
            }

            Text(
                text = stringResource(R.string.streak),
                color = if (lit) scheme.secondary else scheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
        },
        // One item for a screen reader, which has no use for the rung labels
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) { measurables, constraints ->
        val circleSize = CircleSize.roundToPx()
        val gap = Gap.roundToPx()
        val labels = measurables.take(STREAK_RUNGS.size).map { it.measure(Constraints()) }
        val (railMeasurable, circleMeasurable, captionMeasurable) = measurables.drop(STREAK_RUNGS.size)

        val caption = captionMeasurable.measure(Constraints())
        // A window squeezed shorter than the meter would otherwise hand the rail a negative height, which throws
        val ladderHeight = (constraints.maxHeight - caption.height - gap).coerceAtLeast(circleSize)
        val rail = railMeasurable.measure(Constraints.fixed(circleSize, ladderHeight))
        val circle = circleMeasurable.measure(Constraints.fixed(circleSize, circleSize))

        val labelWidth = labels.maxOf { it.width }
        val railX = labelWidth + gap + circleSize / 2
        fun centre(index: Int) = rungCentre(index, ladderHeight.toFloat(), circleSize.toFloat()).roundToInt()

        // Wide enough for the caption, which centres under the rail rather than under the whole meter
        layout(railX + maxOf(circleSize, caption.width) / 2, constraints.maxHeight) {
            labels.forEachIndexed { index, label -> label.placeRelative((labelWidth - label.width) / 2, centre(index) - label.height / 2) }
            rail.placeRelative(railX - circleSize / 2, 0)
            circle.placeRelative(railX - circleSize / 2, centre(reached) - circleSize / 2)
            caption.placeRelative(railX - caption.width / 2, ladderHeight + gap)
        }
    }
}

// Evenly up the ladder, inset by half a circle so a circle on the bottom or top rung stays inside it
private fun rungCentre(index: Int, ladderHeight: Float, circleSize: Float): Float =
    ladderHeight - circleSize / 2 - (ladderHeight - circleSize) * index / STREAK_RUNGS.lastIndex

// The rail is laid out one circle wide and as tall as the ladder
private fun DrawScope.drawRail(reached: Int, accent: Color, rail: Color) {
    fun at(index: Int) = Offset(center.x, rungCentre(index, size.height, size.width))

    drawLine(rail, at(STREAK_RUNGS.lastIndex), at(0), RailWidth.toPx())
    drawLine(accent, at(reached), at(0), RailWidth.toPx())
    STREAK_RUNGS.indices.forEach { drawCircle(if (it < reached) accent else rail, DotSize.toPx() / 2, at(it)) }
}
