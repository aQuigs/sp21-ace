package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

/**
 * A hand's label over its [cards], with its [total], when given, on the label's line at the cards' right edge, as in Blackjack Ace.
 * The cards take the modifier they are given, which fits them to the space.
 */
@Composable
fun HandArea(label: String, total: String?, modifier: Modifier = Modifier, cards: @Composable (Modifier) -> Unit) {
    val color = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier) {
        // A hand of 3 or more cards can fill the space, which sits centred, so its cards stop 8dp short either side and clear of the
        // buttons. The label and the total keep the whole width, which a narrow phone at a large font size needs for one line.
        val cardsMaxWidth = (maxWidth - 16.dp).coerceAtLeast(0.dp)

        // As wide as the cards, unless the label and the total need more, so the total ends where the cards do
        Column(modifier = Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                total?.let {
                    // A total broken over two lines would read as two numbers
                    Text(
                        text = it,
                        modifier = Modifier.alignByBaseline(),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            cards(Modifier.weight(1f, fill = false).widthIn(max = cardsMaxWidth))
        }
    }
}
