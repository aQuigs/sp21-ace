package com.aquigs.sp21ace.ui.trainer

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer

/**
 * Keeps the trainer through activity recreation, such as a theme or font size change. Only cards and the chosen move are
 * saved: on restore the answer is graded again against [chart], so the verdict comes from the same place it always does.
 */
fun trainerStateSaver(chart: StrategyChart): Saver<TrainerState, Any> = listSaver<TrainerState, String>(
    save = { state -> listOf(state.hand.code()) + state.lastGrade?.let { listOf(it.hand.code(), it.answer.name) }.orEmpty() },
    restore = { saved ->
        val hand = trainerHand(saved[0])
        if (saved.size == 1) {
            TrainerState(hand)
        } else {
            val answered = trainerHand(saved[1])
            TrainerState(answered).answer(answered, Move.valueOf(saved[2]), chart) { hand }
        }
    },
)

/** Such as "9C 7D AS": the player's cards, then the upcard. */
private fun TrainerHand.code(): String = (player + upcard).joinToString(" ") { "${it.rank.label}${it.suit.name.first()}" }

private fun trainerHand(code: String): TrainerHand {
    val cards = code.split(" ").map { card ->
        Card(Rank.entries.first { it.label == card.dropLast(1) }, Suit.entries.first { it.name.first() == card.last() })
    }
    return TrainerHand(cards.dropLast(1), cards.last())
}
