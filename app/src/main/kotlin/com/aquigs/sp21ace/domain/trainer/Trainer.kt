package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.domain.strategy.play

/** [correctMove] can differ from the square's [play], because a bonus exception turns the play into a hit. */
data class Grade(val hand: TrainerHand, val play: Play, val answer: Move, val correctMove: Move) {
    val isCorrect: Boolean get() = answer == correctMove
}

data class TrainerState(val hand: TrainerHand, val lastGrade: Grade? = null)

/**
 * Grades the answer to [asked] and deals the next hand at once, because the trainer never waits for a continue tap. An
 * answer to a hand no longer on the table, such as a second tap before the screen redraws, changes nothing.
 */
fun TrainerState.answer(
    asked: TrainerHand,
    move: Move,
    chart: StrategyChart,
    deal: () -> TrainerHand = ::dealTrainerHand,
): TrainerState {
    if (asked != hand) return this

    val grade = Grade(hand, chart.play(hand.player, hand.upcard), move, chart.firstMove(hand.player, hand.upcard))
    return TrainerState(deal(), grade)
}
