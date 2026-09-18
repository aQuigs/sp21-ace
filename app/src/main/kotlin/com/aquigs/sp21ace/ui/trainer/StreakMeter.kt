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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.trainer.STREAK_RUNGS
import com.aquigs.sp21ace.domain.trainer.streakRung
import kotlin.math.roundToInt

private val CircleSize = 36.dp
private val DotSize = 8.dp
private val RailWidth = 3.dp
private val Gap = 4.dp

/**
 * A ladder of doubling rungs with the streak circled on the highest rung it has reached. It takes the height its modifier
 * gives it, and without one just enough to fit every rung label. The rung labels sit left of the ladder, or right of it with
 * [numbersOnRight], whatever the language.
 */
@Composable
fun StreakMeter(streak: Int, modifier: Modifier = Modifier, numbersOnRight: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val reached = streakRung(streak)
    // Grey until the first right answer, as in Blackjack Ace
    val lit = streak > 0
    val accent = if (lit) scheme.secondary else scheme.outlineVariant
    val description = stringResource(R.string.streak_count, streak)

    Layout(
        contents = listOf<@Composable () -> Unit>(
            { STREAK_RUNGS.forEach { Text(text = "$it", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } },
            { Spacer(Modifier.drawBehind { drawRail(reached, accent, scheme.outlineVariant) }) },
            {
                Box(modifier = Modifier.background(accent, CircleShape), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$streak",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = if (lit) scheme.onSecondary else scheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        // Low enough that a four-digit streak still fits the circle at the largest font sizes, on a line as tall as
                        // the number, because the body style's 24 sp line outgrows the circle there and sets the number low
                        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 14.sp),
                        lineHeight = 1.em,
                        maxLines = 1,
                    )
                }
            },
            {
                Text(
                    text = stringResource(R.string.streak),
                    color = if (lit) scheme.secondary else scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
        ),
        // One item for a screen reader, which has no use for the rung labels
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) { (labelMeasurables, railMeasurables, circleMeasurables, captionMeasurables), constraints ->
        val circleSize = CircleSize.roundToPx()
        val gap = Gap.roundToPx()
        val labels = labelMeasurables.map { it.measure(Constraints()) }
        val caption = captionMeasurables.single().measure(Constraints())

        val fitsEveryLabel = circleSize + labels.maxOf { it.height } * STREAK_RUNGS.lastIndex + gap + caption.height
        val height = fitsEveryLabel.coerceIn(constraints.minHeight, constraints.maxHeight)
        // A window squeezed shorter than the meter would otherwise hand the rail a negative height, which throws
        val ladderHeight = (height - gap - caption.height).coerceAtLeast(circleSize)
        val rail = railMeasurables.single().measure(Constraints.fixed(circleSize, ladderHeight))
        val circle = circleMeasurables.single().measure(Constraints.fixed(circleSize, circleSize))

        val labelWidth = labels.maxOf { it.width }
        // The caption centres under the rail, so a caption wider than the labels and circle pushes the rail away from the labels
        val railX = maxOf(labelWidth + gap + circleSize / 2, caption.width / 2)
        val meterWidth = railX + maxOf(circleSize, caption.width) / 2
        fun centre(index: Int) = rungCentre(index, ladderHeight.toFloat(), circleSize.toFloat()).roundToInt()

        layout(meterWidth, height) {
            // Laid out with the labels on the left, then turned around for the right, rather than mirrored by the language
            fun Placeable.placeOnSide(x: Int, y: Int) = place(if (numbersOnRight) meterWidth - x - width else x, y)

            // On a squeezed ladder every rung keeps its dot, but a label that would overlap the one below it is left out
            var lastLabelTop = Int.MAX_VALUE
            labels.forEachIndexed { index, label ->
                val top = centre(index) - label.height / 2
                if (top + label.height <= lastLabelTop) {
                    label.placeOnSide((labelWidth - label.width) / 2, top)
                    lastLabelTop = top
                }
            }
            rail.placeOnSide(railX - circleSize / 2, 0)
            circle.placeOnSide(railX - circleSize / 2, centre(reached) - circleSize / 2)
            caption.placeOnSide(railX - caption.width / 2, ladderHeight + gap)
        }
    }
}

// Evenly up the ladder, inset by half a circle so a circle on the bottom or top rung stays inside it
private fun rungCentre(index: Int, ladderHeight: Float, circleSize: Float): Float =
    ladderHeight - circleSize / 2 - (ladderHeight - circleSize) * index / STREAK_RUNGS.lastIndex

private fun DrawScope.drawRail(reached: Int, accent: Color, rail: Color) {
    fun at(index: Int) = Offset(center.x, rungCentre(index, size.height, CircleSize.roundToPx().toFloat()))

    drawLine(rail, at(STREAK_RUNGS.lastIndex), at(0), RailWidth.toPx())
    drawLine(accent, at(reached), at(0), RailWidth.toPx())
    for (index in STREAK_RUNGS.indices) drawCircle(if (index < reached) accent else rail, DotSize.toPx() / 2, at(index))
}
