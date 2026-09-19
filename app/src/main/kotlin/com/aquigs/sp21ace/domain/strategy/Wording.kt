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
 * The square in words. When its card count or its bonus exception makes [correctMove] a hit for a hand of [cards] cards, that
 * hit leads, so the words never open with a move the grade just called wrong; with no grade, the square's own play leads.
 * [afterDoubling] words the play for a hand already doubled.
 */
fun Play.inPlainWords(correctMove: Move? = null, cards: Int = 2, afterDoubling: Boolean = false): String {
    val square = forCards(cards)
    val play = square.action.inPlainWords(afterDoubling)
    val count = square.hitWithCards?.let { "with $it or more cards" }
    val bonus = bonusException?.inPlainWords
    val lead = when {
        correctMove != Move.HIT -> null
        square.hitWithCards?.let { cards >= it } == true -> count
        else -> bonus
    }
    val exceptions = listOfNotNull(count, bonus).filter { it != lead }

    return buildString {
        if (lead != null) append("Hit $lead. Otherwise ${play.lowercase()}") else append(play)
        if (exceptions.isNotEmpty()) append(", but hit ").append(exceptions.joinToString(" or "))
        if (debated) append(" $DEBATED_MARK (debated)")
    }
}

/** The square in words from the [play] its chart prints there. */
fun ChartSquare.inPlainWords(play: Play): String = play.inPlainWords(afterDoubling = row.table.afterDoubling)

/** A line of a table's legend: a symbol as the table prints it, what it means, and the action whose colour it sits on. */
data class LegendEntry(val symbol: String, val meaning: String, val fill: Action? = null)

/** As in Blackjack Ace, the legend lists only what [table] uses. */
fun StrategyChart.legend(table: ChartTable): List<LegendEntry> {
    val plays = plays(table)
    val cardCounts = plays.mapNotNull { it.hitWithCards }

    return buildList {
        plays.map { it.action }.distinct().sorted().forEach { add(LegendEntry(it.code, it.inPlainWords(table.afterDoubling), fill = it)) }
        if (cardCounts.isNotEmpty()) add(LegendEntry(setOf(cardCounts.min(), cardCounts.max()).joinToString("-"), "Hit with that many cards or more"))
        plays.mapNotNull { it.bonusException }.distinct().sorted().forEach { add(LegendEntry(it.mark, "Hit ${it.inPlainWords}")) }
        if (plays.any { it.debated }) add(LegendEntry(DEBATED_MARK, "Sources still debate this square"))
    }
}

private fun Action.inPlainWords(afterDoubling: Boolean): String = when (this) {
    Action.HIT -> "Hit"
    Action.STAND -> "Stand"
    Action.DOUBLE -> if (afterDoubling) "Redouble" else "Double"
    Action.SPLIT -> "Split"
    Action.SURRENDER -> if (afterDoubling) "Rescue" else "Surrender"
    Action.SURRENDER_OR_HIT -> "Surrender, otherwise hit"
}

private val BonusException.inPlainWords: String
    get() = when (this) {
        BonusException.ANY_678 -> "while any 6-7-8 is possible"
        BonusException.SUITED_678 -> "while a suited 6-7-8 is possible"
        BonusException.SPADED_678 -> "while a spaded 6-7-8 is possible"
        BonusException.SUITED_777 -> "suited 7s"
    }
