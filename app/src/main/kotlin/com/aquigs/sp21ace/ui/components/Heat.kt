package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

/** What [HeatScale] prints: Blackjack Ace's eight steps, only the ends labeled. */
internal val HEAT_SCALE = listOf("0", "", "", "", "", "", "", "100")

/**
 * A heatmap's square printing [code], filled by how often [tally]'s answers were right, or left bare with none. [description] says
 * it in words, because a screen reader can't see the row and column a square sits in, or its color.
 */
@Composable
internal fun HeatSquare(code: String, tally: Tally?, description: String, codes: CodeText, modifier: Modifier = Modifier) {
    val permille = tally?.accuracyPermille
    val colors = Sp21AceTheme.colors

    CodeSquare(
        code = code,
        codes = codes,
        modifier = modifier
            .then(if (permille == null) Modifier else Modifier.heat(colors.heatmap.at(permille / 1000f), MaterialTheme.colorScheme.outlineVariant))
            .semantics(mergeDescendants = true) { contentDescription = description },
        color = if (permille == null) colors.unansweredCode else Color.Unspecified,
    )
}

/** The scale a heatmap's squares are filled over, from 0 to 100. */
@Composable
internal fun HeatScale(swatchSize: Dp, gap: Dp, codes: CodeText) {
    val colors = Sp21AceTheme.colors.heatmap
    val outline = MaterialTheme.colorScheme.outlineVariant

    // For the eye only, since every square already says in words how often it was right
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
    ) {
        HEAT_SCALE.forEachIndexed { step, label ->
            CodeSquare(label, codes, Modifier.size(swatchSize).heat(colors.at(step / HEAT_SCALE.lastIndex.toFloat()), outline))
        }
    }
}

// The light theme's middle step is all but the page's color, so the outline tells a square about half right from one without answers
private fun Modifier.heat(fill: Color, outline: Color): Modifier = background(fill).border(Dp.Hairline, outline)
