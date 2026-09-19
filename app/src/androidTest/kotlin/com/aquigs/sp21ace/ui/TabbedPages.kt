package com.aquigs.sp21ace.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight

/**
 * Only nodes laid out on screen. After a page change a pager composes the page beyond the open one, ready for the next swipe,
 * without placing it, and a lookup would otherwise find that page's copy of a text too.
 */
val isPlaced = SemanticsMatcher("is placed") { node -> generateSequence(node.layoutInfo) { it.parentInfo }.all { it.isPlaced } }

/** The node showing [text] on the open page, or anywhere else on screen. */
fun SemanticsNodeInteractionsProvider.onPlacedNodeWithText(text: String): SemanticsNodeInteraction = onNode(hasText(text) and isPlaced)

/** A swipe across the middle of the screen, over a sub-page's tabbed pages, to the tab after the open one. */
fun ComposeTestRule.swipeToNextTab() {
    onRoot().performTouchInput { swipeLeft() }
}

/** A swipe across the middle of the screen, over a sub-page's tabbed pages, to the tab before the open one. */
fun ComposeTestRule.swipeToPreviousTab() {
    onRoot().performTouchInput { swipeRight() }
}
