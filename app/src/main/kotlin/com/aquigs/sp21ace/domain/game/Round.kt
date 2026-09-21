package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import java.io.Serializable

/** Split to 4 hands, aces included. */
const val MAX_HANDS = 4

/** Masque's Double-Double Down: a double and two redoubles, at most 8 times the bet. */
const val MAX_DOUBLES = 3

/** How a hand stopped taking decisions. */
enum class Finish { STOOD, BUSTED, SURRENDERED, RESCUED }

/**
 * One of the player's hands and the [wager] on it in cents, which every double doubles. A [split] hand can't be a blackjack or
 * surrendered, and earns no Super Bonus.
 */
data class PlayerHand(val cards: List<Card>, val wager: Long, val doubles: Int = 0, val split: Boolean = false, val finish: Finish? = null) :
    Serializable {
    val total: HandTotal get() = cards.total()
    val doubled: Boolean get() = doubles > 0
    val isBlackjack: Boolean get() = !split && cards.isBlackjack()
}

/**
 * A round from the deal to the settlement, in cents. [bankroll] is the chips off the table: the bet, each double and each split
 * move chips from it onto a hand, and the settlement pays back what the hands return. The dealer's second card stays face down
 * until the round is [settled].
 */
data class Round(
    val ruleSet: RuleSet,
    val bet: Long,
    val bankroll: Long,
    val shoe: Shoe,
    val dealer: List<Card>,
    val hands: List<PlayerHand>,
    val active: Int = 0,
    val results: List<HandResult>? = null,
) : Serializable {
    val upcard: Card get() = dealer.first()
    val settled: Boolean get() = results != null

    /** The hand waiting on a decision, or null once the round is settled. */
    val activeHand: PlayerHand? get() = if (settled) null else hands[active]

    /** What the round won or lost, once it's settled. */
    val net: Long? get() = results?.sumOf { it.net }

    /**
     * The moves the active hand can make. A double or split has to be covered by the bankroll. A doubled hand draws one card and
     * no more, so it can only stand, redouble or rescue. Late surrender comes only with the first two cards, before a split.
     */
    fun moves(): Set<Move> {
        val hand = activeHand ?: return emptySet()
        val covered = bankroll >= hand.wager

        if (hand.doubled) {
            val redouble = ruleSet.redoubling && hand.doubles < MAX_DOUBLES && covered
            return setOfNotNull(Move.STAND, Move.REDOUBLE.takeIf { redouble }, Move.RESCUE)
        }

        val pair = hand.cards.size == 2 && hand.cards[0].rank.value == hand.cards[1].rank.value
        return setOfNotNull(
            Move.HIT,
            Move.STAND,
            Move.DOUBLE.takeIf { covered },
            Move.SPLIT.takeIf { pair && hands.size < MAX_HANDS && covered },
            Move.SURRENDER.takeIf { hands.size == 1 && hand.cards.size == 2 },
        )
    }

    /** The round after the active hand makes [move], or null where it can't. */
    fun play(move: Move): Round? {
        if (move !in moves()) return null

        val hand = hands[active]
        return when (move) {
            Move.HIT -> draw { card -> replaceActive(hand.copy(cards = hand.cards + card).finishedAt21()) }
            Move.STAND -> replaceActive(hand.copy(finish = Finish.STOOD))
            Move.SURRENDER -> replaceActive(hand.copy(finish = Finish.SURRENDERED))
            Move.RESCUE -> replaceActive(hand.copy(finish = Finish.RESCUED))
            Move.DOUBLE, Move.REDOUBLE -> copy(bankroll = bankroll - hand.wager).draw { card ->
                replaceActive(hand.copy(cards = hand.cards + card, wager = hand.wager * 2, doubles = hand.doubles + 1).finishedAt21())
            }
            Move.SPLIT -> {
                val (first, second) = hand.cards
                val halves = listOf(PlayerHand(listOf(first), bet, split = true), PlayerHand(listOf(second), bet, split = true))
                copy(bankroll = bankroll - bet, hands = hands.take(active) + halves + hands.drop(active + 1)).nextHand()
            }
        }.nextIfFinished()
    }

    private fun draw(then: Round.(Card) -> Round): Round = shoe.draw().let { (card, rest) -> copy(shoe = rest).then(card) }

    private fun replaceActive(hand: PlayerHand): Round = copy(hands = hands.toMutableList().apply { set(active, hand) })

    private fun nextIfFinished(): Round = if (!settled && hands[active].finish != null) copy(active = active + 1).nextHand() else this

    // A split hand draws its second card once it's the one being played, and any hand that reaches 21 stands on it
    private fun nextHand(): Round {
        if (active == hands.size) return dealerPlays()

        val hand = hands[active]
        val dealt = if (hand.cards.size == 1) draw { card -> replaceActive(hand.copy(cards = hand.cards + card).finishedAt21()) } else this
        return dealt.nextIfFinished()
    }

    // The dealer only draws while a hand still standing below 21 has to be beaten, and settles every hand once done
    private fun dealerPlays(): Round {
        val beatable = hands.any { it.finish == Finish.STOOD && it.total.value < 21 }
        var round = this
        while (beatable && round.dealer.total().dealerHits(ruleSet)) round = round.draw { card -> copy(dealer = dealer + card) }
        return round.settle()
    }

    internal fun settle(): Round {
        val results = hands.map { settle(it, dealer) }
        val returned = hands.zip(results).sumOf { (hand, result) -> hand.wager + result.net }
        return copy(active = hands.size, bankroll = bankroll + returned, results = results)
    }
}

private fun PlayerHand.finishedAt21(): PlayerHand = when {
    total.value > 21 -> copy(finish = Finish.BUSTED)
    total.value == 21 -> copy(finish = Finish.STOOD)
    else -> this
}

private fun HandTotal.dealerHits(ruleSet: RuleSet): Boolean = value < 17 || (value == 17 && soft && ruleSet.dealerHitsSoft17)

/**
 * Deals a round of [bet] cents from [shoe], a card each to the player and the dealer and then a second each, the dealer's face
 * down. A player blackjack is paid at once, and a dealer showing an ace or a face card peeks for blackjack, so either one
 * settles the round before the player acts.
 */
fun dealRound(ruleSet: RuleSet, bet: Long, bankroll: Long, shoe: Shoe): Round {
    require(bet in 1..bankroll) { "A bet of $bet needs a bankroll to cover it, not $bankroll" }

    val (first, afterFirst) = shoe.draw()
    val (upcard, afterUpcard) = afterFirst.draw()
    val (second, afterSecond) = afterUpcard.draw()
    val (hole, rest) = afterSecond.draw()
    val round = Round(ruleSet, bet, bankroll - bet, rest, listOf(upcard, hole), listOf(PlayerHand(listOf(first, second), bet)))

    return if (round.hands[0].isBlackjack || round.dealer.isBlackjack()) round.settle() else round
}
