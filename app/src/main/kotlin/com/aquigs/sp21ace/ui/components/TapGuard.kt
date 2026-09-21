package com.aquigs.sp21ace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

// Android's double-tap timeout
internal const val TAP_GUARD_MILLIS = 300L

/**
 * False until [TAP_GUARD_MILLIS] after [keys] last changed, so the second tap of a double tap can't land on a button that has just
 * appeared under the first. Read it with `by` inside the tap's handler, which sees it arm before the screen redraws.
 */
@Composable
fun rememberArmed(vararg keys: Any?): State<Boolean> {
    val armed = remember(*keys) { mutableStateOf(false) }

    LaunchedEffect(*keys) {
        delay(TAP_GUARD_MILLIS)
        armed.value = true
    }
    return armed
}
