package com.aquigs.sp21ace.ui

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight

/** A swipe across the middle of the screen, over a sub-page's tabbed pages, to the tab after the open one. */
fun ComposeTestRule.swipeToNextTab() {
    onRoot().performTouchInput { swipeLeft() }
}

/** A swipe across the middle of the screen, over a sub-page's tabbed pages, to the tab before the open one. */
fun ComposeTestRule.swipeToPreviousTab() {
    onRoot().performTouchInput { swipeRight() }
}
