package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import java.io.Serializable

/** [correctMove] can differ from the square's [play], because a card count or a bonus exception turns the play into a hit. */
data class Grade(val hand: TrainerHand, val play: Play, val answer: Move, val correctMove: Move) : Serializable {
    val isCorrect: Boolean get() = answer == correctMove
}

/**
 * Serializable so the activity saves it as it is through recreation and process death: a restored verdict is the one
 * given, never a regrade against rules that may have changed since.
 */
data class TrainerState(val hand: TrainerHand, val lastGrade: Grade? = null, val streak: Int = 0) : Serializable

/** A graded answer, and the trainer it leaves: the next hand dealt, the verdict shown and the streak moved on. */
data class Answered(val state: TrainerState, val grade: Grade)

/**
 * Grades the answer to [asked] and deals the next hand at once, because the trainer never waits for a continue tap. [deal] is
 * handed the grade, so a deal that weighs answers can count this one. An answer to a hand no longer on the table, such as a
 * second tap before the screen redraws, or a move the hand doesn't allow, grades nothing: null.
 */
fun TrainerState.answer(asked: TrainerHand, move: Move, chart: StrategyChart, deal: (Grade) -> TrainerHand): Answered? {
    if (asked != hand || move !in hand.moves(chart.redoubling)) return null

    val grade = Grade(hand, chart.play(hand), move, chart.correctMove(hand))
    return Answered(copy(hand = deal(grade), lastGrade = grade, streak = nextStreak(streak, grade.isCorrect)), grade)
}

/** A right answer adds one to a streak, and a wrong one drops it to zero. */
fun nextStreak(streak: Int, isCorrect: Boolean): Int = if (isCorrect) streak + 1 else 0

/** The streak meter's doubling scale. */
val STREAK_RUNGS = listOf(0, 1, 2, 4, 8, 16, 32, 64, 128, 256)

/** The index in [STREAK_RUNGS] of the highest rung [streak] has reached, which stays the top rung past 256. */
fun streakRung(streak: Int): Int = STREAK_RUNGS.indexOfLast { it <= streak }
