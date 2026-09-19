package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.afterDoublingRow
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.handClass
import com.aquigs.sp21ace.domain.strategy.upcard
import java.io.Serializable

private val EVERY_MOVE = Move.entries.toSet()
private val MOVES_PAST_TWO_CARDS = setOf(Move.HIT, Move.STAND, Move.DOUBLE)

/**
 * A trainer question: the player's cards against the dealer's upcard. A [doubled] hand asks what to do after doubling, its last
 * card the one the double drew. The hole card stays face down, so it is never drawn.
 */
data class TrainerHand(val player: List<Card>, val upcard: Card, val doubled: Boolean = false) : Serializable {
    /** The row the hand is read from, a doubled hand's from the after-doubling tables whatever the rules. */
    val row: ChartRow get() = if (doubled) afterDoublingRow(player) else chartRow(player)

    /** "Hard 16 vs A", or with 3 or more cards "4-card hard 15 vs 2", since how many cards there are can change the play. */
    val matchup: String
        get() {
            val kind = handClass(player).let { if (player.size > 2) "${player.size}-card ${it.lowercase()}" else it }
            return "$kind vs ${upcard.upcard.label}"
        }

    /** The moves the player can make. Splitting and late surrender come only with the first two cards. */
    val moves: Set<Move> get() = if (player.size == 2) EVERY_MOVE else MOVES_PAST_TWO_CARDS
}
