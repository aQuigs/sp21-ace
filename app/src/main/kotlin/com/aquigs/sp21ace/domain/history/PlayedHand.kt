package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.Round
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.strategy.RuleSet
import java.time.Instant

/** A hand played at the table as its round settled: what it came to, what it won or lost in cents, and how its play was graded. */
data class PlayedHand(val playedAt: Instant, val ruleSet: RuleSet, val outcome: Outcome, val net: Long, val grade: StrategyGrade)

/** Each hand of a settled round, split hands apart, in the order they were played, as Blackjack Ace counts them. */
fun Round.playedHands(at: Instant): List<PlayedHand> =
    hands.zip(requireNotNull(results) { "Only a settled round has been played" }) { hand, result ->
        PlayedHand(at, ruleSet, result.outcome, result.net, hand.strategy.grade)
    }

/**
 * The hands of the table's round if it settled on the change from [previous], by a move or on the deal, and none otherwise.
 * Nothing unsettles a round, so each round's hands come back once.
 */
fun Table.playedHandsSince(previous: Table, at: Instant): List<PlayedHand> =
    round?.takeIf { it.settled && previous.round?.settled != true }?.playedHands(at).orEmpty()
