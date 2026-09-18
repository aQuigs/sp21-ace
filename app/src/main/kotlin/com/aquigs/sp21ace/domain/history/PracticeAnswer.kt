package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import java.time.Instant

/**
 * A trainer answer as it was graded, under the rules then in force, so a later change of rules never regrades it. The hand
 * keeps every card's suit, because bonus exceptions and drilling one exact hand depend on it.
 */
data class PracticeAnswer(val answeredAt: Instant, val ruleSet: RuleSet, val hand: TrainerHand, val answer: Move, val correctMove: Move) {
    constructor(answeredAt: Instant, ruleSet: RuleSet, grade: Grade) : this(answeredAt, ruleSet, grade.hand, grade.answer, grade.correctMove)

    // Only a hand with a chart row was ever asked, so a record of any other, such as a hand past 21, is refused
    init {
        chartRow(hand.player)
    }

    val isCorrect: Boolean get() = answer == correctMove

    /** The row of the chart square the hand is read from, beside [hand]'s upcard. Its table is the kind of hand: hard, soft or a pair. */
    val row: ChartRow get() = chartRow(hand.player)
}
