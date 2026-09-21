package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.DECKS
import java.io.Serializable
import kotlin.random.Random

/**
 * A shoe dealt from the top, [dealt] cards in. The cut card sits three quarters of the way in, Blackjack Ace's default, which
 * leaves 72 cards, far more than a round uses.
 */
data class Shoe(val cards: List<Card>, val dealt: Int = 0) : Serializable {
    /** A round only starts from a shoe the cut card hasn't come out of, so one that has is shuffled first. */
    val pastCutCard: Boolean get() = dealt >= cards.size * 3 / 4

    fun draw(): Pair<Card, Shoe> = cards[dealt] to copy(dealt = dealt + 1)

    companion object {
        fun shuffled(random: Random): Shoe = Shoe(spanishShoe(DECKS).shuffled(random))
    }
}
