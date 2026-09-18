package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
import java.time.Instant

/** An answer as the history records it, and the trainer it leaves with the next hand dealt. */
data class Recorded(val state: TrainerState, val answer: PracticeAnswer)

/**
 * Grades [move] on [asked] under [rules] and deals the next hand. The store records the answer only after, so [deal] is handed
 * [history] with it added, and a hand just missed weighs as missed on the very next deal. An answer to a hand no longer on
 * the table records nothing: null.
 */
fun TrainerState.record(
    asked: TrainerHand,
    move: Move,
    rules: RuleSet,
    history: List<PracticeAnswer>,
    answeredAt: Instant,
    deal: (List<PracticeAnswer>) -> TrainerHand,
): Recorded? {
    fun recorded(grade: Grade) = PracticeAnswer(answeredAt, rules, grade)

    val answered = answer(asked, move, StrategyCharts.forRules(rules)) { grade -> deal(history + recorded(grade)) } ?: return null
    return Recorded(answered.state, recorded(answered.grade))
}
