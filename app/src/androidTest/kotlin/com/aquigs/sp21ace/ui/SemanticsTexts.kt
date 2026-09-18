package com.aquigs.sp21ace.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction

/** A node's texts in reading order. A merged row or card is one item for a screen reader, so they come title first. */
fun SemanticsNodeInteraction.texts(): List<String> = fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }
