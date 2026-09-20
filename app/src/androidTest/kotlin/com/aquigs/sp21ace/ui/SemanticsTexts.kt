package com.aquigs.sp21ace.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Nodes laid out on screen, which a node composed but never placed, or inside a layout that isn't, is not. */
val isPlaced = SemanticsMatcher("is placed") { node -> generateSequence(node.layoutInfo) { it.parentInfo }.all { it.isPlaced } }

/**
 * Nodes within every horizontal pager they sit in, which one on a page placed beside the open one, ready to slide in, is not.
 * Unclipped, so a node scrolled up or down out of view still counts, to be scrolled to.
 */
private val isOnOpenPage = SemanticsMatcher("is on the open page") { node ->
    val left = node.positionInRoot.x
    generateSequence(node.parent) { it.parent }
        .filter { hasScrollToKeyAction().matches(it) && SemanticsProperties.HorizontalScrollAxisRange in it.config }
        .all { pager -> left < pager.positionInRoot.x + pager.size.width && left + node.size.width > pager.positionInRoot.x }
}

/**
 * Finds only nodes on screen. A pager places every page beside the open one, ready for a swipe, and after a page change composes
 * the one beyond it without placing it, so a plain lookup would find those pages' copies of a text too.
 */
val SemanticsNodeInteractionsProvider.onScreen: SemanticsNodeInteractionsProvider
    get() = object : SemanticsNodeInteractionsProvider {
        override fun onNode(matcher: SemanticsMatcher, useUnmergedTree: Boolean) =
            this@onScreen.onNode(matcher and isPlaced and isOnOpenPage, useUnmergedTree)

        override fun onAllNodes(matcher: SemanticsMatcher, useUnmergedTree: Boolean) =
            this@onScreen.onAllNodes(matcher and isPlaced and isOnOpenPage, useUnmergedTree)
    }

/** A node's texts in reading order. A merged row or card is one item for a screen reader, so they come title first. */
fun SemanticsNodeInteraction.texts(): List<String> = fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

/** The layout of a text node, which the unmerged tree holds even for text a control hides from a screen reader. */
fun SemanticsNode.textLayout(): TextLayoutResult =
    mutableListOf<TextLayoutResult>().also { config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()

/**
 * Fails unless the text sits inside its node. Judged by where each line ends, since a text given more room than it needs is laid
 * out at the full width and reads as overflowing its node, and Compose pads a letter-spaced text's intrinsic width by half a
 * pixel it never draws. A line taller than its node, as a sp line height in a small box makes it, sets the letters off centre or
 * out of view.
 */
fun TextLayoutResult.assertFits() {
    val widest = (0 until lineCount).maxOf { getLineRight(it) }
    val tall = multiParagraph.height
    val cutShort = multiParagraph.didExceedMaxLines

    assertTrue(
        "${layoutInput.text} ends at ${widest}px of ${size.width}px and is ${tall}px of ${size.height}px tall on $lineCount lines, cut short: $cutShort",
        !cutShort && widest <= size.width && tall <= size.height,
    )
}

/** Fails unless the text fits its node on a single line. */
fun TextLayoutResult.assertFitsOnOneLine() {
    assertEquals("${layoutInput.text} lines", 1, lineCount)
    assertFits()
}
