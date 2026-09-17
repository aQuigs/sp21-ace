package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.domain.strategy.upcard

/** [correctMove] can differ from the square's [play], because a bonus exception turns the play into a hit. */
data class Grade(val hand: TrainerHand, val play: Play, val correctMove: Move, val isCorrect: Boolean)

/** Asks one hand at a time. Answering deals the next hand at once, because the trainer never waits for a continue tap. */
class Trainer(private val chart: StrategyChart, private val deal: () -> TrainerHand = ::dealTrainerHand) {
    var hand: TrainerHand = deal()
        private set

    fun answer(move: Move): Grade {
        val row = chartRow(hand.player)
        val play = requireNotNull(chart.play(row.table, row.hand, hand.upcard.upcard)) { "No chart square for ${hand.matchup}" }
        val correctMove = chart.firstMove(hand.player, hand.upcard)

        return Grade(hand, play, correctMove, move == correctMove).also { hand = deal() }
    }
}
