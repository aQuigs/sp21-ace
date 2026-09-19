package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.total

/** The answers a player can give to a hand. */
enum class Move { HIT, STAND, DOUBLE, SPLIT, SURRENDER }

data class ChartRow(val table: ChartTable, val hand: String)

data class ChartSquare(val row: ChartRow, val upcard: Upcard)

val Card.upcard: Upcard get() = if (rank.value == 10) Upcard.TEN else Upcard.fromLabel(rank.label)

/** The row a hand is read from: a two-card pair from the pairs table, any other hand by its [totalRow]. */
fun chartRow(hand: List<Card>): ChartRow =
    if (hand.size == 2 && hand[0].upcard == hand[1].upcard) hand[0].upcard.label.let { ChartRow(ChartTable.PAIRS, "$it-$it") } else totalRow(hand)

/** The row a hand is read from by its total alone. */
fun totalRow(hand: List<Card>): ChartRow = hand.total().row

/** The row a total is read from: soft 18 from the soft table as "A-7", and hard 16 from the hard table as "16". */
val HandTotal.row: ChartRow
    get() {
        require(value <= 21) { "A busted hand has no chart row" }
        return if (soft) ChartRow(ChartTable.SOFT, "A-${value - 11}") else ChartRow(ChartTable.HARD, "$value")
    }

/**
 * The row a doubled hand is read from by its total: "16" from After doubling: hard and "A-7" from After doubling: soft, whatever
 * the rules. Rules without redoubling print Double Down Rescue instead, which reads its rows, hard 12 to 17, by total too.
 */
fun afterDoublingRow(hand: List<Card>): ChartRow {
    val row = totalRow(hand)
    return ChartRow(if (row.table == ChartTable.SOFT) ChartTable.AFTER_DOUBLE_SOFT else ChartTable.AFTER_DOUBLE_HARD, row.hand)
}

fun StrategyChart.play(hand: List<Card>, upcard: Card): Play = play(chartRow(hand), upcard.upcard)

fun StrategyChart.play(row: ChartRow, upcard: Upcard): Play = requireNotNull(play(row.table, row.hand, upcard)) { "No chart square for ${row.hand} vs ${upcard.label}" }

/**
 * The chart's answer to a hand not yet doubled. A bonus exception turns the play into a hit while its bonus hand can still be
 * made, which only two cards can.
 */
fun StrategyChart.correctMove(hand: List<Card>, upcard: Card): Move {
    val play = play(hand, upcard)
    return if (play.bonusException?.canStillMake(hand, upcard.upcard) == true) Move.HIT else play.move(cards = hand.size)
}

/**
 * Whether [hand]'s ranks could make the bonus its square marks against [upcard] in some suits, so the suits may decide its move: a
 * 6-7, 6-8 or 7-8 on a 6-7-8 mark, or 7-7 against a 7 on $. Three cards could only make it at 21, which leaves nothing to decide.
 */
internal fun StrategyChart.bonusHand(hand: List<Card>, upcard: Upcard): Boolean = play(chartRow(hand), upcard).bonusException?.ranksCanMake(hand, upcard) == true

/**
 * The square as it reads for a hand of [cards] cards. Late surrender comes only with the first two, so past them RH is a surrender
 * that hits with 3 or more cards, which grading and the words both read the same way.
 */
internal fun Play.forCards(cards: Int): Play = if (action == Action.SURRENDER_OR_HIT && cards > 2) copy(action = Action.SURRENDER, hitWithCards = 3) else this

/** Whether the number of cards decides the square's move in a hand of 3 or more cards: D3 to D6, S4 to S6, and RH. */
internal val Play.countsCards: Boolean get() = forCards(cards = 3).hitWithCards != null

/** The move a square calls for in a hand of [cards] cards, bonus exceptions aside. */
internal fun Play.move(cards: Int): Move {
    val square = forCards(cards)
    if (square.hitWithCards != null && cards >= square.hitWithCards) return Move.HIT

    return when (square.action) {
        Action.HIT -> Move.HIT
        Action.STAND -> Move.STAND
        Action.DOUBLE -> Move.DOUBLE
        Action.SPLIT -> Move.SPLIT
        Action.SURRENDER, Action.SURRENDER_OR_HIT -> Move.SURRENDER
    }
}

private val SIX_SEVEN_EIGHT = setOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT)

private fun BonusException.ranksCanMake(hand: List<Card>, upcard: Upcard): Boolean = when (this) {
    BonusException.ANY_678, BonusException.SUITED_678, BonusException.SPADED_678 ->
        hand.all { it.rank in SIX_SEVEN_EIGHT } && hand.distinctBy { it.rank }.size == hand.size
    BonusException.SUITED_777 -> upcard == Upcard.SEVEN && hand.all { it.rank == Rank.SEVEN }
}

private fun BonusException.canStillMake(hand: List<Card>, upcard: Upcard): Boolean = ranksCanMake(hand, upcard) && when (this) {
    BonusException.ANY_678 -> true
    BonusException.SUITED_678, BonusException.SUITED_777 -> hand.all { it.suit == hand[0].suit }
    BonusException.SPADED_678 -> hand.all { it.suit == Suit.SPADES }
}
