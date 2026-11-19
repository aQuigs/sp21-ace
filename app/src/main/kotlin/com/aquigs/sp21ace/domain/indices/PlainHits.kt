package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.totalOf
import com.aquigs.sp21ace.domain.strategy.Upcard

/**
 * Whether a shoe game's basic strategy plainly hits [points], a hand of 2 or more cards in the order dealt with an ace as 1,
 * against [upcard]: hits whatever the rules, with no double, split, surrender or stand to weigh. A flashcard's hand of 3 or more
 * cards is one a basic strategy player could hold only if every hand on the way to it was one of these.
 *
 * An allow-list, not a whole blackjack chart, so it holds only hands no rule changes; the indices `SOURCES.md` cites the charts and
 * says what is left out.
 */
fun plainlyHits(points: List<Int>, upcard: Upcard): Boolean {
    require(points.size >= 2) { "A hand of ${points.size} cards" }

    val total = totalOf(points)
    val sevenOrMore = upcard >= Upcard.SEVEN
    return when {
        // An ace among the first two cards makes them soft
        points.size == 2 -> !total.soft && points[0] != points[1] && (total.value in 5..8 || total.value in 12..14 && sevenOrMore)
        total.soft -> total.value <= 17 || total.value == 18 && upcard >= Upcard.NINE
        else -> total.value <= 11 || total.value == 12 && (upcard <= Upcard.THREE || sevenOrMore) || total.value in 13..16 && sevenOrMore
    }
}
