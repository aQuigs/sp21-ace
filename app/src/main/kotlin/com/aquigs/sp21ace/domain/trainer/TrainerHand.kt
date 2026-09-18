package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.afterDoublingRow
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.handClass
import com.aquigs.sp21ace.domain.strategy.upcard
import java.io.Serializable

/**
 * A trainer question: the player's cards against the dealer's upcard. A [doubled] hand asks what to do after doubling, its last
 * card the one the double drew. The hole card stays face down, so it is never drawn.
 */
data class TrainerHand(val player: List<Card>, val upcard: Card, val doubled: Boolean = false) : Serializable {
    /** The row the hand is read from, a doubled hand's from the after-doubling tables whatever the rules. */
    val row: ChartRow get() = if (doubled) afterDoublingRow(player) else chartRow(player)

    val matchup: String get() = "${handClass(player)} vs ${upcard.upcard.label}"
}
