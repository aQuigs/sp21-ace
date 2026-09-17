package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.total

/** "Hard 14", "Soft 17", "Pair of 8s", "Pair of aces", or "Pair of 10s" for any two ten-value cards. */
fun handClass(hand: List<Card>): String = when (chartRow(hand).table) {
    ChartTable.PAIRS -> hand[0].upcard.let { if (it == Upcard.ACE) "Pair of aces" else "Pair of ${it.label}s" }
    ChartTable.SOFT -> "Soft ${hand.total().value}"
    else -> "Hard ${hand.total().value}"
}

/**
 * The square in words. When its bonus exception makes [correctMove] a hit, the hit leads, so the words never open with
 * a move the grade just called wrong.
 */
fun Play.inPlainWords(correctMove: Move): String {
    val play = when (action) {
        Action.HIT -> "Hit"
        Action.STAND -> "Stand"
        Action.DOUBLE -> "Double"
        Action.SPLIT -> "Split"
        Action.SURRENDER -> "Surrender"
        Action.SURRENDER_OR_HIT -> "Surrender, otherwise hit"
    }
    val bonus = when (bonusException) {
        BonusException.ANY_678 -> "while any 6-7-8 is possible"
        BonusException.SUITED_678 -> "while a suited 6-7-8 is possible"
        BonusException.SPADED_678 -> "while a spaded 6-7-8 is possible"
        BonusException.SUITED_777 -> "suited 7s"
        null -> null
    }
    val bonusHits = bonus != null && correctMove == Move.HIT
    val exceptions = listOfNotNull(hitWithCards?.let { "with $it or more cards" }, bonus.takeUnless { bonusHits })

    return buildString {
        if (bonusHits) append("Hit $bonus. Otherwise ${play.lowercase()}") else append(play)
        if (exceptions.isNotEmpty()) append(", but hit ").append(exceptions.joinToString(" or "))
        if (debated) append(" † (debated)")
    }
}
