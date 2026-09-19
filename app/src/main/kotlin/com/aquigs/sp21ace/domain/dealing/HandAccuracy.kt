package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.TallyCounter
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.countsCards
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.strategy.totalRow
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand

// So a hand never answered right comes up 20 times as often as one always answered right, rather than every time until it is
private const val ACCURACY_FLOOR = 0.05

private const val NO_ANSWERS_ACCURACY = 0.5

/** A hand as Prioritize worse hands tells hands apart, each weighed by how often it was answered right. */
internal sealed interface HandKey {
    val upcard: Upcard
}

/** Two cards: the player's two card values, lower first, against the upcard's. Suits, and J, Q and K, read alike. */
internal data class HandValues(val low: Upcard, val high: Upcard, override val upcard: Upcard) : HandKey

/**
 * 3 or more cards: the row the total is read from against the upcard, and the move the chart calls for, so a card count that
 * changes the move makes another hand.
 */
internal data class MultiCardHand(val row: ChartRow, override val upcard: Upcard, val move: Move) : HandKey {
    val type: HandType get() = HandType(row.table, move)
}

internal fun handValues(player: List<Card>, upcard: Upcard): HandValues {
    require(player.size == 2) { "A dealt hand has two cards, not ${player.size}" }
    val (low, high) = player.map { it.upcard }.sorted()
    return HandValues(low, high, upcard)
}

internal val TrainerHand.values: HandValues get() = handValues(player, upcard.upcard)

/** The hand as Prioritize worse hands tells it apart under [chart], or null for a doubled hand, which it never deals. */
internal fun TrainerHand.key(chart: StrategyChart): HandKey? = when {
    doubled -> null
    player.size == 2 -> values
    else -> MultiCardHand(totalRow(player), upcard.upcard, chart.correctMove(player, upcard))
}

/**
 * Every answer ever given to each hand, however old and whatever rules graded it, as Blackjack Ace counts them. A hand of 3 or
 * more cards is told apart by the move [chart] calls for, so its answers count for the hand those rules deal.
 */
internal fun List<PracticeAnswer>.tallyByHand(chart: StrategyChart): Map<HandKey, Tally> {
    val tallies = TallyCounter<HandKey>()
    for (answer in this) answer.hand.key(chart)?.let { tallies.add(it, answer.isCorrect) }
    return tallies.toMap()
}

/** Every answer ever given, by the type its grade filed the hand under, for Customize Hands' subtitles. A type no answer called for reads as none answered. */
fun List<PracticeAnswer>.tallyByHandType(): Map<HandType, Tally> {
    val tallies = TallyCounter<HandType>()
    for (answer in this) tallies.add(HandType(answer.square.row.table, answer.correctMove), answer.isCorrect)

    val counted = tallies.toMap()
    return HAND_TYPES.associateWith { counted[it] ?: Tally(correct = 0, incorrect = 0) }
}

/** Every answer ever given to a hand of 3 or more cards not yet doubled, for their Customize Hands switch. */
fun List<PracticeAnswer>.multiCardTally(): Tally = filter { it.isMultiCard }.tally()

/** Every answer ever given to a card-count hand, told apart by the rules that graded it as the other switches' answers are, for its switch. */
fun List<PracticeAnswer>.cardCountTally(): Tally =
    filter { it.isMultiCard && StrategyCharts.forRules(it.ruleSet).play(it.square.row, it.square.upcard).countsCards }.tally()

private val PracticeAnswer.isMultiCard: Boolean get() = hand.player.size > 2 && !hand.doubled

private fun List<PracticeAnswer>.tally(): Tally = count { it.isCorrect }.let { Tally(correct = it, incorrect = size - it) }

/** How heavily a hand weighs under Prioritize worse hands: the inverse of its accuracy, with no answers counting as 50%. */
internal fun weight(tally: Tally?): Double {
    val accuracy = tally?.takeIf { it.total > 0 }?.let { it.correct.toDouble() / it.total } ?: NO_ANSWERS_ACCURACY
    return 1 / maxOf(accuracy, ACCURACY_FLOOR)
}
