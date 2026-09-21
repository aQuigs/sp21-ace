package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.total
import java.io.Serializable

/** Pays [win] for every [stake] bet. */
enum class Odds(val win: Int, val stake: Int) {
    EVEN(1, 1),
    THREE_TO_TWO(3, 2),
    TWO_TO_ONE(2, 1),
    THREE_TO_ONE(3, 1),
    ;

    fun on(wager: Long): Long = wager * win / stake
}

/** The Bonus 21 payouts, on a 21 that wasn't doubled. A 6-7-8 or 7-7-7 has to be the hand's only three cards. */
enum class Bonus(val odds: Odds) {
    FIVE_CARD_21(Odds.THREE_TO_TWO),
    SIX_CARD_21(Odds.TWO_TO_ONE),
    SEVEN_CARD_21(Odds.THREE_TO_ONE),
    MIXED_678(Odds.THREE_TO_TWO),
    SUITED_678(Odds.TWO_TO_ONE),
    SPADED_678(Odds.THREE_TO_ONE),
    MIXED_777(Odds.THREE_TO_TWO),
    SUITED_777(Odds.TWO_TO_ONE),
    SPADED_777(Odds.THREE_TO_ONE),
}

enum class Outcome { WIN, PUSH, LOSE }

/** How a hand settled: [net] is what it won or lost in cents, the Super Bonus included. */
data class HandResult(val outcome: Outcome, val net: Long, val blackjack: Boolean = false, val bonus: Bonus? = null, val superBonus: Long = 0) :
    Serializable

private val SIX_SEVEN_EIGHT = setOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT)

/** The bonus a 21 of these cards pays, if any. */
fun bonus(cards: List<Card>): Bonus? {
    val suits = when {
        cards.all { it.suit == Suit.SPADES } -> 2
        cards.all { it.suit == cards[0].suit } -> 1
        else -> 0
    }
    return when {
        cards.size >= 7 -> Bonus.SEVEN_CARD_21
        cards.size == 6 -> Bonus.SIX_CARD_21
        cards.size == 5 -> Bonus.FIVE_CARD_21
        cards.size == 3 && cards.map { it.rank }.toSet() == SIX_SEVEN_EIGHT -> listOf(Bonus.MIXED_678, Bonus.SUITED_678, Bonus.SPADED_678)[suits]
        cards.size == 3 && cards.all { it.rank == Rank.SEVEN } -> listOf(Bonus.MIXED_777, Bonus.SUITED_777, Bonus.SPADED_777)[suits]
        else -> null
    }
}

private const val FIVE_DOLLARS = 500L
private const val TWENTY_FIVE_DOLLARS = 2_500L

/**
 * The Super Bonus a hand of [cards] against [upcard] earns on a [wager] in cents: a suited 7-7-7 against any 7 wins $1,000 on a
 * bet of $5 to $24.99 and $5,000 from $25, on top of its 7-7-7 bonus. The regulations set no prize below $5. Split and doubled
 * hands never earn it.
 */
fun superBonus(cards: List<Card>, upcard: Card, wager: Long): Long {
    val suited777 = cards.size == 3 && cards.all { it.rank == Rank.SEVEN && it.suit == cards[0].suit }
    return when {
        !suited777 || upcard.rank != Rank.SEVEN -> 0
        wager >= TWENTY_FIVE_DOLLARS -> 500_000
        wager >= FIVE_DOLLARS -> 100_000
        else -> 0
    }
}

/**
 * Settles [hand] against the dealer's finished [dealer] cards. A player blackjack beats a dealer's, and any other 21 beats any
 * dealer 21 but a blackjack, which the dealer peeks for, so only a hand dealt nothing more than its first two cards can meet one.
 */
internal fun settle(hand: PlayerHand, dealer: List<Card>): HandResult {
    val total = hand.total.value
    val dealerTotal = dealer.total().value

    return when {
        hand.finish == Finish.SURRENDERED || hand.finish == Finish.RESCUED -> HandResult(Outcome.LOSE, -hand.wager / 2)
        hand.finish == Finish.BUSTED -> HandResult(Outcome.LOSE, -hand.wager)
        hand.isBlackjack -> HandResult(Outcome.WIN, Odds.THREE_TO_TWO.on(hand.wager), blackjack = true)
        dealer.isBlackjack() -> HandResult(Outcome.LOSE, -hand.wager)
        total == 21 -> twentyOne(hand, dealer.first())
        dealerTotal > 21 || total > dealerTotal -> HandResult(Outcome.WIN, hand.wager)
        total == dealerTotal -> HandResult(Outcome.PUSH, 0)
        else -> HandResult(Outcome.LOSE, -hand.wager)
    }
}

// A doubled 21 still wins, but only at even money
private fun twentyOne(hand: PlayerHand, upcard: Card): HandResult {
    if (hand.doubled) return HandResult(Outcome.WIN, hand.wager)

    val bonus = bonus(hand.cards)
    val superBonus = if (hand.split) 0 else superBonus(hand.cards, upcard, hand.wager)
    return HandResult(Outcome.WIN, (bonus?.odds ?: Odds.EVEN).on(hand.wager) + superBonus, bonus = bonus, superBonus = superBonus)
}
