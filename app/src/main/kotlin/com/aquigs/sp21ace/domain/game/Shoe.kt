package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.DECKS
import java.io.Serializable
import kotlin.random.Random

/**
 * A shoe dealt from the top, [dealt] cards in. The round on the table was dealt from [roundStart] on, so the cards before it are
 * the discards. Should a round run the shoe out, the dealer shuffles the discards by [seed] and deals on, and the shoe needs a
 * fresh shuffle before the next round, as it does once the cut card is out.
 */
data class Shoe(
    val cards: List<Card>,
    val dealt: Int = 0,
    val roundStart: Int = 0,
    val seed: Long = 0,
    val ranOut: Boolean = false,
) : Serializable {
    /** Whether the cut card, [penetration] percent of the way in, is out, or the shoe ran out, so it needs shuffling. */
    fun needsShuffle(penetration: Int): Boolean = ranOut || dealt >= cards.size * penetration / 100

    fun draw(): Pair<Card, Shoe> = draw(1).let { (drawn, rest) -> drawn.single() to rest }

    fun draw(count: Int): Pair<List<Card>, Shoe> {
        val shoe = if (dealt + count > cards.size) discardsShuffledIn() else this
        return shoe.cards.subList(shoe.dealt, shoe.dealt + count).toList() to shoe.copy(dealt = shoe.dealt + count)
    }

    /** This shoe with a round about to be dealt from it, or a fresh shuffle once it needs one. */
    fun forNextRound(random: Random, penetration: Int): Shoe = if (needsShuffle(penetration)) shuffled(random) else copy(roundStart = dealt)

    // The round's cards stay on the table, so they move to the front, ahead of the cards left and the discards
    private fun discardsShuffledIn(): Shoe = Shoe(
        cards = cards.subList(roundStart, cards.size) + cards.subList(0, roundStart).shuffled(Random(seed)),
        dealt = dealt - roundStart,
        seed = seed,
        ranOut = true,
    )

    companion object {
        fun shuffled(random: Random): Shoe = Shoe(spanishShoe(DECKS).shuffled(random), seed = random.nextLong())
    }
}
