package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.afterDoublingPlay
import com.aquigs.sp21ace.domain.strategy.afterDoublingRow
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.correctMoveAfterDoubling
import com.aquigs.sp21ace.domain.strategy.handClass
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.strategy.upcard
import java.io.Serializable

private val FIRST_TWO_CARDS = setOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SPLIT, Move.SURRENDER)
private val PAST_TWO_CARDS = setOf(Move.HIT, Move.STAND, Move.DOUBLE)

// A double draws one card and no more, so a doubled hand can only stand, redouble or rescue
private val AFTER_DOUBLING = setOf(Move.STAND, Move.RESCUE)

/**
 * A trainer question: the player's cards against the dealer's upcard. A [doubled] hand asks what to do after doubling, its last
 * card the one the double drew. The hole card stays face down, so it is never drawn.
 */
data class TrainerHand(val player: List<Card>, val upcard: Card, val doubled: Boolean = false) : Serializable {
    /** The row the hand is read from, a doubled hand's from the after-doubling tables whatever the rules. */
    val row: ChartRow get() = if (doubled) afterDoublingRow(player) else chartRow(player)

    /**
     * "Hard 16 vs A", or with 3 or more cards "4-card hard 15 vs 2", since how many cards there are can change the play. After
     * doubling they can't, so "Doubled hard 16 vs 10".
     */
    val matchup: String
        get() {
            val kind = handClass(player).let {
                when {
                    doubled -> "Doubled ${it.lowercase()}"
                    player.size > 2 -> "${player.size}-card ${it.lowercase()}"
                    else -> it
                }
            }
            return "$kind vs ${upcard.upcard.label}"
        }

    /** The moves the player can make. Splitting and late surrender come only with the first two cards, and a redouble only with [redoubling]. */
    fun moves(redoubling: Boolean): Set<Move> = when {
        doubled -> if (redoubling) AFTER_DOUBLING + Move.REDOUBLE else AFTER_DOUBLING
        player.size == 2 -> FIRST_TWO_CARDS
        else -> PAST_TWO_CARDS
    }
}

/** The square [hand] is read from, a doubled hand's from the chart's tables for doubled hands. */
fun StrategyChart.play(hand: TrainerHand): Play =
    if (hand.doubled) afterDoublingPlay(hand.player.total(), hand.upcard.upcard) else play(hand.player, hand.upcard)

fun StrategyChart.correctMove(hand: TrainerHand): Move =
    if (hand.doubled) correctMoveAfterDoubling(hand.player.total(), hand.upcard.upcard) else correctMove(hand.player, hand.upcard)
