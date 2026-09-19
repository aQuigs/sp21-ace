package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.total

/** The answers a player can give to a hand's first decision. */
enum class Move { HIT, STAND, DOUBLE, SPLIT, SURRENDER }

data class ChartRow(val table: ChartTable, val hand: String)

data class ChartSquare(val row: ChartRow, val upcard: Upcard)

val Card.upcard: Upcard get() = if (rank.value == 10) Upcard.TEN else Upcard.fromLabel(rank.label)

/** The row a hand is read from: a two-card pair from the pairs table, any other hand by its [totalRow]. */
fun chartRow(hand: List<Card>): ChartRow =
    if (hand.size == 2 && hand[0].upcard == hand[1].upcard) hand[0].upcard.label.let { ChartRow(ChartTable.PAIRS, "$it-$it") } else totalRow(hand)

/** The row a hand is read from by its total alone: soft 18 from the soft table as "A-7", and hard 16 from the hard table as "16". */
fun totalRow(hand: List<Card>): ChartRow {
    val total = hand.total()
    require(total.value <= 21) { "A busted hand has no chart row" }

    return if (total.soft) ChartRow(ChartTable.SOFT, "A-${total.value - 11}") else ChartRow(ChartTable.HARD, "${total.value}")
}

/**
 * The row a doubled hand is read from by its total: "16" from After doubling: hard and "A-7" from After doubling: soft, whatever
 * the rules. Rules without redoubling print Double Down Rescue instead, which reads its rows, hard 12 to 17, by total too.
 */
fun afterDoublingRow(hand: List<Card>): ChartRow {
    val row = totalRow(hand)
    return ChartRow(if (row.table == ChartTable.SOFT) ChartTable.AFTER_DOUBLE_SOFT else ChartTable.AFTER_DOUBLE_HARD, row.hand)
}

fun StrategyChart.play(hand: List<Card>, upcard: Card): Play {
    val row = chartRow(hand)
    return requireNotNull(play(row.table, row.hand, upcard.upcard)) { "No chart square for ${row.hand} vs ${upcard.upcard.label}" }
}

/**
 * The chart's answer to a two-card starting hand. Late surrender is always allowed on the first decision, so RH means
 * surrender. Card-count exceptions start at 3 cards and never apply here, but a bonus exception turns the play into a
 * hit while its bonus hand can still be made.
 */
fun StrategyChart.firstMove(hand: List<Card>, upcard: Card): Move {
    require(hand.size == 2) { "A first decision has two cards, not ${hand.size}" }
    val play = play(hand, upcard)

    if (play.bonusException?.canStillMake(hand, upcard) == true) return Move.HIT

    return when (play.action) {
        Action.HIT -> Move.HIT
        Action.STAND -> Move.STAND
        Action.DOUBLE -> Move.DOUBLE
        Action.SPLIT -> Move.SPLIT
        Action.SURRENDER, Action.SURRENDER_OR_HIT -> Move.SURRENDER
    }
}

private val SIX_SEVEN_EIGHT = setOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT)

private fun BonusException.canStillMake(hand: List<Card>, upcard: Card): Boolean {
    val canMake678 = hand.all { it.rank in SIX_SEVEN_EIGHT } && hand.distinctBy { it.rank }.size == hand.size
    val suited = hand.distinctBy { it.suit }.size == 1

    return when (this) {
        BonusException.ANY_678 -> canMake678
        BonusException.SUITED_678 -> canMake678 && suited
        BonusException.SPADED_678 -> canMake678 && hand.all { it.suit == Suit.SPADES }
        BonusException.SUITED_777 -> upcard.rank == Rank.SEVEN && hand.all { it.rank == Rank.SEVEN } && suited
    }
}
