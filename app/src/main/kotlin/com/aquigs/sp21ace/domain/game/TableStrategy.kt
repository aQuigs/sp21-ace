package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.correctMoveAfterDoubling
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.strategy.totalRow
import com.aquigs.sp21ace.domain.strategy.upcard

/**
 * The chart's answer to the hand being played, or null once the round is settled. The charts print what to do when every move
 * is allowed, so where the table rules the chart's move out, the answer falls back, as SOURCES.md's known gaps explain:
 * - A doubled hand that can't redouble, at the limit or short of chips, stands or rescues as it would without redoubling.
 * - With no surrender, after a split, RH hits, and 8-8's R against an ace splits again while it can, then plays as hard 16.
 * - A double the bankroll can't cover stands on soft 18 or more and hits anything less.
 * - A pair that can't be split again plays by its total, soft 12 for a pair of aces.
 */
fun Round.correctMove(): Move? {
    val hand = activeHand ?: return null
    val chart = StrategyCharts.forRules(ruleSet)
    val moves = moves()

    if (hand.doubled) {
        // A doubled hand that can't redouble can only stand or rescue, as it can under the same dealer without redoubling
        val withoutRedoubling = TableRules(dealerHitsSoft17 = ruleSet.dealerHitsSoft17).ruleSet
        return chart.correctMoveAfterDoubling(hand.total, upcard.upcard).takeIf { it in moves }
            ?: StrategyCharts.forRules(withoutRedoubling).correctMoveAfterDoubling(hand.total, upcard.upcard)
    }
    return chart.tableMove(hand, upcard, moves)
}

private fun StrategyChart.tableMove(hand: PlayerHand, upcard: Card, moves: Set<Move>, row: ChartRow = chartRow(hand.cards)): Move {
    val move = correctMove(row, hand.cards, upcard, hand.split)

    return when {
        move in moves -> move
        move == Move.DOUBLE -> if (hand.total.soft && hand.total.value >= 18) Move.STAND else Move.HIT
        move == Move.SURRENDER && play(row, upcard.upcard).action == Action.SURRENDER_OR_HIT -> Move.HIT
        move == Move.SURRENDER && Move.SPLIT in moves -> Move.SPLIT
        // The pairs table's split or surrender, where neither can be made. Soft 12 has a row only where the chart prints one,
        // which H17 with redoubling doesn't, since it always splits aces
        row.table == ChartTable.PAIRS -> totalRow(hand.cards).let { if (printsRow(it)) tableMove(hand, upcard, moves, it) else Move.HIT }
        else -> error("No move for ${hand.cards} vs ${upcard.upcard.label} with only $moves")
    }
}
