package com.aquigs.sp21ace.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize

const val DISSOLVE_MILLIS = 400

/**
 * Shows [value], and when it changes dissolves the old one into the new in place, as Blackjack Ace deals its next hand.
 *
 * Unlike a crossfade, the layout takes the new value's size at once, so a hand of a different width moves to its place at the tap
 * rather than jumping when the fade ends, and the old one fades out over it, placed by [alignment] against the new one. The new
 * value keeps its place in the composition, so a screen reader follows the same node from answer to answer and announces each
 * change, and the old one fades out silent.
 */
@Composable
fun <T> Dissolve(
    value: T,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    durationMillis: Int = DISSOLVE_MILLIS,
    content: @Composable (T) -> Unit,
) {
    val transition = updateTransition(value, label = "dissolve")
    // Keyed, so each new value fades in from nothing rather than taking over the finished fade of the one before
    val incoming by key(value) {
        transition.animateFloat({ tween(durationMillis) }, label = "incoming alpha") { if (it == value) 1f else 0f }
    }

    Box(modifier, contentAlignment = alignment) {
        if (transition.currentState != transition.targetState) {
            Box(Modifier.takingNoRoom(alignment).alpha(1f - incoming).clearAndSetSemantics {}) { content(transition.currentState) }
        }
        Box(Modifier.alpha(incoming)) { content(value) }
    }
}

// Measured as it would be but sized to nothing, and placed about the point the parent aligns it to as the parent would place it
private fun Modifier.takingNoRoom(alignment: Alignment) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(0, 0) { placeable.place(alignment.align(IntSize(placeable.width, placeable.height), IntSize.Zero, layoutDirection)) }
}
