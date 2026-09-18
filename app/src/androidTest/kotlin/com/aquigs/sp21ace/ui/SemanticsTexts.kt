package com.aquigs.sp21ace.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

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
