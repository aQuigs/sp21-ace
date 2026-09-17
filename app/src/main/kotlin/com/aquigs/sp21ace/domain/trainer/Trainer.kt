package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.domain.strategy.play
import java.io.Serializable

/** [correctMove] can differ from the square's [play], because a bonus exception turns the play into a hit. */
data class Grade(val hand: TrainerHand, val play: Play, val answer: Move, val correctMove: Move) : Serializable {
    val isCorrect: Boolean get() = answer == correctMove
}

/**
 * Serializable so the activity saves it as it is through recreation and process death: a restored verdict is the one
 * given, never a regrade against rules that may have changed since.
 */
data class TrainerState(val hand: TrainerHand, val lastGrade: Grade? = null, val streak: Int = 0) : Serializable

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
    return TrainerState(deal(), grade, if (grade.isCorrect) streak + 1 else 0)
}

/** The streak meter's doubling scale. */
val STREAK_RUNGS = listOf(0, 1, 2, 4, 8, 16, 32, 64, 128, 256)

/** The highest rung [streak] has reached. Past the top rung the meter stays there while the streak keeps counting. */
fun streakRung(streak: Int): Int = STREAK_RUNGS.last { it <= streak }
