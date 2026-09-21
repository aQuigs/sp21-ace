package com.aquigs.sp21ace.ui.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.aquigs.sp21ace.ui.components.BrandNavy
import com.aquigs.sp21ace.ui.components.BrandSaffron
import com.aquigs.sp21ace.ui.components.Ink
import com.aquigs.sp21ace.ui.components.drawCentred

// Casino colours by value, as Blackjack Ace's are, with our navy and saffron for the 10 and the 50. In order, for topChip.
private val CHIP_COLORS: Map<Long, Color> = mapOf(
    500L to Color(0xFFB3202A),
    1_000L to BrandNavy,
    2_500L to Color(0xFF2E6B45),
    5_000L to BrandSaffron,
    10_000L to Ink,
    50_000L to Color(0xFF5B3A7A),
    100_000L to Color(0xFFC9A227),
)

private val Face = Color(0xFFF7F4EE)
private const val EDGE_SPOTS = 8

/** The biggest chip that fits in [amount], which a stack of it shows on top, or the smallest for less than any chip. */
internal fun topChip(amount: Long): Long = CHIP_COLORS.keys.lastOrNull { it <= amount } ?: CHIP_COLORS.keys.first()

/** A chip's value as written on it: 5 to 100, and 1K for a thousand. */
internal fun chipLabel(value: Long): String = if (value >= 100_000) "${value / 100_000}K" else "${value / 100}"

/** A casino chip of [value] cents: its colour, white spots around the edge, and a face with the value on it, or none when [labelled] is off. */
@Composable
fun Chip(value: Long, modifier: Modifier = Modifier, labelled: Boolean = true) {
    val color = CHIP_COLORS.getValue(topChip(value))
    val measurer = rememberTextMeasurer()

    Canvas(modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2
        drawCircle(color, radius)

        // Each spot fills a third of its eighth of the rim, centred on the rim's middle ring
        val spot = Stroke(width = radius * 0.2f, cap = StrokeCap.Butt)
        repeat(EDGE_SPOTS) { i ->
            drawArc(
                color = Face,
                startAngle = i * 360f / EDGE_SPOTS - 360f / EDGE_SPOTS / 6,
                sweepAngle = 360f / EDGE_SPOTS / 3,
                useCenter = false,
                topLeft = Offset(center.x - radius * 0.87f, center.y - radius * 0.87f),
                size = Size(radius * 1.74f, radius * 1.74f),
                style = spot,
            )
        }
        drawCircle(Face, radius * 0.62f)
        drawCircle(color, radius * 0.62f, style = Stroke(width = radius * 0.05f))

        if (labelled) {
            val label = chipLabel(value)
            val style = TextStyle(color = Ink, fontWeight = FontWeight.Bold, fontSize = (radius * if (label.length > 2) 0.42f else 0.5f).toSp())
            val text = measurer.measure(label, style)
            drawCentred(text, center)
        }
    }
}
