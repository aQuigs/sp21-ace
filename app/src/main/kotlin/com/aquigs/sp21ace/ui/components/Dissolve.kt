package com.aquigs.sp21ace.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round

const val DISSOLVE_MILLIS = 400

/**
 * Shows [value], and when it changes dissolves the old one into the new in place, as Blackjack Ace deals its next hand. A value
 * with the same [contentKey] as the one it replaces takes its place without a dissolve, so what it shows can dissolve on its own.
 *
 * Unlike a crossfade, the layout takes the new value's size at once, so a hand of a different width moves to its place at the tap
 * rather than jumping when the fade ends. A value on its way out stays where it was on screen, at the size it had, and is silent
 * to a screen reader. Each value fades from wherever it has got to, so one that changes again mid-dissolve never jumps.
 * [modifier] sits on the one node that outlasts every value, so what a screen reader must keep hearing from, such as a live
 * region, belongs there.
 */
@Composable
fun <T> Dissolve(
    value: T,
    modifier: Modifier = Modifier,
    durationMillis: Int = DISSOLVE_MILLIS,
    contentKey: (T) -> Any? = { it },
    content: @Composable (T) -> Unit,
) {
    // Numbered rather than keyed, so a value with the key of one still fading out fades in afresh rather than bringing it back
    val slots = remember { mutableStateListOf(Slot(0, value)) }
    val newest = slots.last()

    if (value != newest.value) {
        if (contentKey(value) == contentKey(newest.value)) slots[slots.lastIndex] = Slot(newest.id, value) else slots += Slot(newest.id + 1, value)
    }
    val transition = updateTransition(slots.last().id, label = "dissolve")
    if (transition.currentState == transition.targetState && slots.size > 1) slots.retainAll { it.id == transition.targetState }

    Box(modifier) {
        slots.forEach { slot ->
            key(slot.id) {
                val alpha by transition.animateFloat({ tween(durationMillis) }, label = "alpha") { if (it == slot.id) 1f else 0f }
                val laidOut = remember { LaidOut() }
                val place = if (slot.id == transition.targetState) {
                    Modifier.recording(laidOut)
                } else {
                    Modifier.stayingWhereItWas(laidOut).clearAndSetSemantics {}
                }

                Box(place.graphicsLayer { this.alpha = alpha }) { content(slot.value) }
            }
        }
    }
}

private data class Slot<T>(val id: Int, val value: T)

// Not state: written as the value is laid out, where a write to state read there would lay it out again
private class LaidOut {
    var constraints: Constraints? = null
    var position: Offset? = null
}

// Saved as it is placed, which a query of its intrinsic size never is, and wherever the layout around it moves it to
private fun Modifier.recording(laidOut: LaidOut) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
        laidOut.constraints = constraints
        placeable.place(0, 0)
    }
}.onGloballyPositioned { laidOut.position = it.positionInRoot() }

// Measured as it last was but sized to nothing, so only the new value sizes the layout, and put back where it last was on screen
// however the new value has moved the layout
private fun Modifier.stayingWhereItWas(laidOut: LaidOut) = layout { measurable, constraints ->
    val placeable = measurable.measure(laidOut.constraints ?: constraints)

    layout(0, 0) {
        val was = laidOut.position
        val here = coordinates?.positionInRoot()
        placeable.place(if (was == null || here == null) IntOffset.Zero else (was - here).round())
    }
}
