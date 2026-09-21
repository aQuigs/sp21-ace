package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.ui.theme.disabledContent

val CircleButtonSize = 64.dp

/**
 * Blackjack Ace's round outlined button, at most [CircleButtonSize] across. [label] is for the eye, such as capitals or SURR.,
 * and a screen reader says [name].
 */
@Composable
fun CircleButton(name: String, onClick: () -> Unit, modifier: Modifier = Modifier, label: String = name.uppercase(), enabled: Boolean = true) {
    val color = MaterialTheme.colorScheme.primary
    // An outlined button greys out its label but not its border
    val disabledColor = MaterialTheme.colorScheme.disabledContent

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .sizeIn(maxWidth = CircleButtonSize, maxHeight = CircleButtonSize)
            .aspectRatio(1f)
            .semantics { contentDescription = name },
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(3.dp, if (enabled) color else disabledColor),
        // The ring is drawn inside the circle in the label's colour, so the label is fitted inside the ring rather than across it
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            // A label as wide as a full-size circle allows touches the curve of its ring with its end letters, so it stops
            // 7dp short, as DOUBLE does at its largest. The smallest circles have no room to spare for that.
            modifier = Modifier.widthIn(max = CircleButtonSize - 14.dp),
            fontWeight = FontWeight.Bold,
            // DOUBLE already needs 8 sp inside the ring of a small phone's circles at a large font size, so a shorter screen, or REDOUBLE, needs less
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 13.sp),
            maxLines = 1,
        )
    }
}

/** A [CircleButton] for [move], SURR. to the eye for a surrender. */
@Composable
fun MoveButton(move: Move, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val name = stringResource(move.displayName)

    CircleButton(
        name = name,
        onClick = onClick,
        modifier = modifier,
        label = if (move == Move.SURRENDER) stringResource(R.string.surrender_short) else name.uppercase(),
        enabled = enabled,
    )
}
