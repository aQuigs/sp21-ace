package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.strategy.RuleSet
import java.time.Instant

/** One play history line, a hand each, split hands apart: named fields, such as `at=1789000000000 rules=S17 outcome=WIN net=2500 strategy=CORRECT`. */
internal object PlayLine {
    fun print(hand: PlayedHand): String = LineFields.print(
        "at" to hand.playedAt.toEpochMilli(),
        "rules" to hand.ruleSet.name,
        "outcome" to hand.outcome.name,
        "net" to hand.net,
        "strategy" to hand.grade.name,
    )

    /** Null for a line it can't read, such as one cut short when the app was killed mid-write. */
    fun parse(line: String): PlayedHand? = try {
        val fields = LineFields(line)

        PlayedHand(
            playedAt = Instant.ofEpochMilli(fields["at"].toLong()),
            ruleSet = RuleSet.valueOf(fields["rules"]),
            outcome = Outcome.valueOf(fields["outcome"]),
            net = fields["net"].toLong(),
            grade = StrategyGrade.valueOf(fields["strategy"]),
        )
    } catch (e: RuntimeException) {
        null
    }
}
