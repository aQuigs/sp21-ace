package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.TallyCounter
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand

// So a hand never answered right comes up 20 times as often as one always answered right, rather than every time until it is
private const val ACCURACY_FLOOR = 0.05

private const val NO_ANSWERS_ACCURACY = 0.5

/** A hand as Prioritize worse hands tells hands apart: the player's two card values, lower first, against the upcard's. Suits, and J, Q and K, read alike. */
internal data class HandValues(val low: Upcard, val high: Upcard, val upcard: Upcard)

internal fun handValues(player: List<Card>, upcard: Upcard): HandValues {
    require(player.size == 2) { "A dealt hand has two cards, not ${player.size}" }
    val (low, high) = player.map { it.upcard }.sorted()
    return HandValues(low, high, upcard)
}

internal val TrainerHand.values: HandValues get() = handValues(player, upcard.upcard)

/** Every answer ever given to each two-card hand, however old and whatever rules graded it, as Blackjack Ace counts them. */
internal fun List<PracticeAnswer>.tallyByHand(): Map<HandValues, Tally> {
    val tallies = TallyCounter<HandValues>()
    for (answer in this) if (answer.hand.player.size == 2) tallies.add(answer.hand.values, answer.isCorrect)
    return tallies.toMap()
}

/** Every answer ever given, by the type its grade filed the hand under, for Customize Hands' subtitles. A type no answer called for reads as none answered. */
fun List<PracticeAnswer>.tallyByHandType(): Map<HandType, Tally> {
    val tallies = TallyCounter<HandType>()
    for (answer in this) tallies.add(HandType(answer.square.row.table, answer.correctMove), answer.isCorrect)

    val counted = tallies.toMap()
    return HAND_TYPES.associateWith { counted[it] ?: Tally(correct = 0, incorrect = 0) }
}

/** How heavily a hand weighs under Prioritize worse hands: the inverse of its accuracy, with no answers counting as 50%. */
internal fun weight(tally: Tally?): Double {
    val accuracy = tally?.takeIf { it.total > 0 }?.let { it.correct.toDouble() / it.total } ?: NO_ANSWERS_ACCURACY
    return 1 / maxOf(accuracy, ACCURACY_FLOOR)
}
