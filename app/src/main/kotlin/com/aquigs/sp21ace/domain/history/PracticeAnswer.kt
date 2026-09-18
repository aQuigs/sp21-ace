package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import java.time.Instant

/**
 * A trainer answer as it was graded, under the rules then in force, so a later change of rules never regrades it. The hand
 * keeps every card's suit, because bonus exceptions and drilling one exact hand depend on it.
 */
data class PracticeAnswer(val answeredAt: Instant, val ruleSet: RuleSet, val hand: TrainerHand, val answer: Move, val correctMove: Move) {
    constructor(answeredAt: Instant, ruleSet: RuleSet, grade: Grade) : this(answeredAt, ruleSet, grade.hand, grade.answer, grade.correctMove)

    val isCorrect: Boolean get() = answer == correctMove

    /**
     * The chart square the hand is read from, its row's table the kind of hand. No row reads a hand past 21, which was never
     * asked, so a record of one is refused.
     */
    val square: ChartSquare = ChartSquare(hand.row, hand.upcard.upcard)
}
