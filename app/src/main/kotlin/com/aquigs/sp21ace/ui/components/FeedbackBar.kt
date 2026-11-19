package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

// Blackjack Ace's verdict changes in about a quarter of a second, quicker than its cards dissolve
private const val FEEDBACK_MILLIS = 250

/**
 * A drill's app bar: its [title] until the first answer, then the [verdict] on the last one, in green or red with a mark, shown as
 * [feedback] draws it and spoken as right or wrong and then [spoken]. The menu button opens the drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> FeedbackBar(
    verdict: T?,
    title: String,
    isCorrect: (T) -> Boolean,
    spoken: String?,
    onOpenDrawer: () -> Unit,
    feedback: @Composable (T) -> Unit,
) {
    val colors = Sp21AceTheme.colors
    val correct = verdict?.let(isCorrect)
    // The app bar fades to a new color on its own, about as quickly as Blackjack Ace's
    val container = when (correct) {
        null -> colors.appBar
        true -> colors.correct
        false -> colors.wrong
    }

    // Right and wrong otherwise differ only in color and mark, which a screen reader can't announce
    val said = correct?.let { "${stringResource(if (it) R.string.right_answer else R.string.wrong_answer)}. $spoken" }

    TopAppBar(
        title = {
            Dissolve(
                verdict,
                // Each verdict dissolves in on its own, so a screen reader hears every one from the node the dissolve keeps
                Modifier.semantics(mergeDescendants = true) {
                    if (said == null) {
                        heading()
                    } else {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = said
                    }
                },
                durationMillis = FEEDBACK_MILLIS,
            ) { shown ->
                if (shown == null) Text(title) else feedback(shown)
            }
        },
        navigationIcon = {
            Dissolve(correct, durationMillis = FEEDBACK_MILLIS) { shownCorrect ->
                // The title speaks the verdict, so the mark stays silent
                if (shownCorrect != null) {
                    Icon(
                        painterResource(if (shownCorrect) R.drawable.ic_check_circle else R.drawable.ic_cancel),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp).size(52.dp),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenDrawer) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.open_menu))
            }
        },
        colors = appBarColors(container),
    )
}

/** A verdict's [text], shrunk to fit the app bar. */
@Composable
fun FeedbackText(text: AnnotatedString) {
    Text(
        text = text,
        // The longest squares need three lines, and an em line height keeps them inside the bar as autoSize shrinks the text
        autoSize = autoSizeDownTo(minSize = 10.dp, maxFontSize = 16.sp),
        maxLines = 3,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.2.em),
    )
}
