package com.aquigs.sp21ace.ui.trainer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.ui.components.DISSOLVE_MILLIS
import com.aquigs.sp21ace.ui.components.DealerHand
import com.aquigs.sp21ace.ui.components.Dissolve
import com.aquigs.sp21ace.ui.components.DissolvingHand
import com.aquigs.sp21ace.ui.components.autoSizeDownTo
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

private val ThumbnailHeight = 64.dp

/** Recaps the last answered hand along the bottom of the trainer, tinted right or wrong. */
@Composable
fun PreviousHandPanel(grade: Grade?, modifier: Modifier = Modifier) {
    val colors = Sp21AceTheme.colors
    val background by animateColorAsState(
        targetValue = when (grade?.isCorrect) {
            null -> MaterialTheme.colorScheme.surfaceContainer
            true -> colors.correctTint
            false -> colors.wrongTint
        },
        animationSpec = tween(DISSOLVE_MILLIS),
        label = "recap tint",
    )
    val shown by animateFloatAsState(if (grade == null) 0f else 1f, tween(DISSOLVE_MILLIS), label = "recap alpha")

    Column(modifier = modifier.drawBehind { drawRect(background) }) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Blank before the first answer, as in Blackjack Ace, but still measured so the table above doesn't shift when the recap fills in
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer { alpha = shown }
                .then(if (grade == null) Modifier.clearAndSetSemantics {} else Modifier),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.previous_hand),
                modifier = Modifier.semantics { heading() },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Dissolving what changes, as the table above does
                RecapColumn(stringResource(R.string.you), grade) { recap -> DissolvingHand(recap.hand.player, sideways = recap.hand.doubled) }
                RecapColumn(stringResource(R.string.dealer), grade) { DealerHand(it.hand.upcard) }
                RecapColumn(stringResource(R.string.action), grade) { MoveTile(it.answer) }
                RecapColumn(stringResource(R.string.strategy), grade) { MoveTile(it.correctMove) }
            }
        }
    }
}

@Composable
private fun RowScope.RecapColumn(label: String, grade: Grade?, content: @Composable (Grade) -> Unit) {
    // One item for a screen reader: the label, then its cards or move
    Column(
        modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // At the largest font sizes "Strategy" would otherwise break mid-word in its quarter of a small phone
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize),
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
        )
        Box(modifier = Modifier.fillMaxWidth().height(ThumbnailHeight)) { grade?.let { content(it) } }
    }
}

@Composable
private fun MoveTile(move: Move) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Dissolve(move) {
            Text(
                text = stringResource(it.displayName),
                modifier = Modifier.padding(horizontal = 4.dp),
                autoSize = autoSizeDownTo(minSize = 8.dp, maxFontSize = 20.sp),
                maxLines = 1,
            )
        }
    }
}
