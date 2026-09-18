package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit

// A poker-size card is 2.5 by 3.5 inches
private const val ASPECT_RATIO = 2.5f / 3.5f

// How much of the card underneath stays uncovered: enough to read its corner index
private const val OVERLAP_STEP = 0.2f

private val CardShape = RoundedCornerShape(percent = 5)
private val Edge = Color(0xFFD9D9D9)
private val Red = Color(0xFFC8102E)
private val Black = Color(0xFF1B1B1B)

// The brand navy and saffron, fixed rather than themed because a card looks the same in either theme
private val BackNavy = Color(0xFF1F3A5F)
private val BackSaffron = Color(0xFFD99A1E)

// U+FE0E asks for the text glyph. Without it Android draws these suits from the colour emoji font, which ignores the ink colour.
private val Suit.glyph: String
    get() = when (this) {
        Suit.SPADES -> "♠"
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
    } + "︎"

// Pip centres as fractions of the card's width and height. Pips below the middle print upside down.
private val PIPS: Map<Rank, List<Offset>> = run {
    val (left, middle, right) = listOf(0.28f, 0.5f, 0.72f)
    val ends = listOf(Offset(middle, 0.2f), Offset(middle, 0.8f))
    val corners = listOf(Offset(left, 0.2f), Offset(right, 0.2f), Offset(left, 0.8f), Offset(right, 0.8f))
    val sides = corners + Offset(left, 0.5f) + Offset(right, 0.5f)
    val centre = Offset(middle, 0.5f)

    mapOf(
        Rank.TWO to ends,
        Rank.THREE to ends + centre,
        Rank.FOUR to corners,
        Rank.FIVE to corners + centre,
        Rank.SIX to sides,
        Rank.SEVEN to sides + Offset(middle, 0.35f),
        Rank.EIGHT to sides + Offset(middle, 0.35f) + Offset(middle, 0.65f),
        Rank.NINE to listOf(0.2f, 0.4f, 0.6f, 0.8f).flatMap { listOf(Offset(left, it), Offset(right, it)) } + centre,
    )
}

@Composable
fun PlayingCard(card: Card, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val description = card.name()

    Spacer(modifier.cardSurface().semantics { contentDescription = description }.drawBehind { drawFace(card, measurer) })
}

@Composable
fun CardBack(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.face_down_card)

    Spacer(modifier.cardSurface().semantics { contentDescription = description }.drawBehind { drawBack() })
}

/** Deals cards left to right, each over most of the one before, as large as the space allows up to [maxCardHeight]. */
@Composable
fun OverlappingCards(modifier: Modifier = Modifier, maxCardHeight: Dp = 256.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val steps = (measurables.size - 1).coerceAtLeast(0)
        val widthPerHeight = ASPECT_RATIO * (1 + OVERLAP_STEP * steps)
        // Rounded down, so a fan that fills its space never spills past it onto what sits beside it
        val cardHeight = minOf(maxCardHeight.toPx(), constraints.maxHeight.toFloat(), constraints.maxWidth / widthPerHeight).toInt()
        val cardWidth = (cardHeight * ASPECT_RATIO).toInt()
        val step = (cardWidth * OVERLAP_STEP).toInt()
        val cards = measurables.map { it.measure(Constraints.fixed(cardWidth, cardHeight)) }

        layout(cardWidth + step * steps, cardHeight) {
            cards.forEachIndexed { index, card -> card.place(index * step, 0) }
        }
    }
}

private fun Modifier.cardSurface(): Modifier = aspectRatio(ASPECT_RATIO)
    .shadow(elevation = 3.dp, shape = CardShape)
    .background(Color.White, CardShape)
    .border(Dp.Hairline, Edge, CardShape)

@Composable
private fun Card.name(): String {
    val rankName = when (rank) {
        Rank.ACE -> stringResource(R.string.ace)
        Rank.JACK -> stringResource(R.string.jack)
        Rank.QUEEN -> stringResource(R.string.queen)
        Rank.KING -> stringResource(R.string.king)
        else -> rank.label
    }
    val suitName = when (suit) {
        Suit.SPADES -> R.string.spades
        Suit.HEARTS -> R.string.hearts
        Suit.DIAMONDS -> R.string.diamonds
        Suit.CLUBS -> R.string.clubs
    }

    return stringResource(R.string.card_name, rankName, stringResource(suitName))
}

private fun DrawScope.drawFace(card: Card, measurer: TextMeasurer) {
    val ink = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Red else Black

    // Sizes follow the card rather than the font scale, because a card's print is part of its picture
    fun measure(text: String, widthFraction: Float, weight: FontWeight = FontWeight.Normal) =
        measurer.measure(text, TextStyle(color = ink, fontSize = (size.width * widthFraction).toSp(), fontWeight = weight))

    val rankIndex = measure(card.rank.label, 0.17f, FontWeight.Medium)
    val suitIndex = measure(card.suit.glyph, 0.12f)
    val indexCentre = size.width * 0.1f
    for (turn in listOf(0f, 180f)) {
        rotate(turn) {
            drawCentred(rankIndex, Offset(indexCentre, size.width * 0.02f + rankIndex.size.height / 2f))
            drawCentred(suitIndex, Offset(indexCentre, size.width * 0.02f + rankIndex.size.height * 0.95f + suitIndex.size.height / 2f))
        }
    }

    when (card.rank) {
        Rank.ACE -> drawCentred(measure(card.suit.glyph, 0.45f), center)
        Rank.JACK, Rank.QUEEN, Rank.KING -> {
            // Set in further than OVERLAP_STEP, so a covered court card shows its index but not a sliver of frame
            val frame = Offset(size.width * 0.23f, size.height * 0.15f)
            drawRect(ink, frame, Size(size.width - 2 * frame.x, size.height - 2 * frame.y), alpha = 0.5f, style = Stroke(size.width * 0.012f))
            drawCentred(measure(card.rank.label, 0.4f, FontWeight.Bold), center)
        }
        else -> {
            val pip = measure(card.suit.glyph, 0.22f)
            PIPS.getValue(card.rank).forEach { (x, y) ->
                val at = Offset(size.width * x, size.height * y)
                rotate(if (y > 0.5f) 180f else 0f, pivot = at) { drawCentred(pip, at) }
            }
        }
    }
}

private fun DrawScope.drawBack() {
    val margin = size.width * 0.06f
    val corner = CornerRadius(margin / 2)
    val stroke = size.width * 0.012f
    drawRoundRect(BackNavy, Offset(margin, margin), Size(size.width - 2 * margin, size.height - 2 * margin), corner)

    clipRect(margin, margin, size.width - margin, size.height - margin) {
        var x = -size.height
        while (x < size.width) {
            drawLine(BackSaffron, Offset(x, 0f), Offset(x + size.height, size.height), stroke, alpha = 0.45f)
            drawLine(BackSaffron, Offset(x, size.height), Offset(x + size.height, 0f), stroke, alpha = 0.45f)
            x += size.width / 6
        }
    }

    val frame = margin * 2
    drawRoundRect(BackSaffron, Offset(frame, frame), Size(size.width - 2 * frame, size.height - 2 * frame), corner, style = Stroke(stroke * 1.5f))
}

private fun DrawScope.drawCentred(text: TextLayoutResult, at: Offset) =
    drawText(text, topLeft = at - Offset(text.size.width / 2f, text.size.height / 2f))
