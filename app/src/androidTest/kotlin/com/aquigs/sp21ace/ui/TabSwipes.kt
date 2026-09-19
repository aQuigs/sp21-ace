package com.aquigs.sp21ace.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight

// Across the middle of the screen, where a sub-page's pages sit under its tabs. Never past the first or last tab: under the test
// clock the stretch at the edge never settles, and the run hangs rather than failing.

fun SemanticsNodeInteractionsProvider.swipeToNextTab() = onRoot().performTouchInput { swipeLeft() }

fun SemanticsNodeInteractionsProvider.swipeToPreviousTab() = onRoot().performTouchInput { swipeRight() }
