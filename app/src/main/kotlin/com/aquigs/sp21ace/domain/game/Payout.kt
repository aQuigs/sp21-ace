package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.SIX_SEVEN_EIGHT
import com.aquigs.sp21ace.domain.cards.allSpades
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.suited
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

/**
 * The Bonus 21 payouts, on a 21 that wasn't doubled, nor split where the table pays split hands none. A 6-7-8 or 7-7-7 has to be
 * the hand's only three cards.
 */
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

/** A hand still standing below 21 waits on the dealer's total. A 21 wins whatever the dealer makes. */
internal val PlayerHand.awaitsDealer: Boolean get() = finish == Finish.STOOD && total.value < 21

/**
 * Settles the hand against the dealer's finished [dealer] cards. A player blackjack beats a dealer's, and any other 21 beats any
 * dealer 21 but a blackjack, which the dealer peeks for, so only a hand dealt nothing more than its first two cards can meet one.
 */
internal fun PlayerHand.settle(dealer: List<Card>, splitBonuses: Boolean): HandResult {
    val dealerTotal = dealer.total().value

    return when {
        finish == Finish.SURRENDERED || finish == Finish.RESCUED -> HandResult(Outcome.LOSE, -wager / 2)
        finish == Finish.BUSTED -> HandResult(Outcome.LOSE, -wager)
        isBlackjack -> HandResult(Outcome.WIN, Odds.THREE_TO_TWO.on(wager), blackjack = true)
        dealer.isBlackjack() -> HandResult(Outcome.LOSE, -wager)
        total.value == 21 -> twentyOne(dealer.first(), splitBonuses)
        dealerTotal > 21 || total.value > dealerTotal -> HandResult(Outcome.WIN, wager)
        total.value == dealerTotal -> HandResult(Outcome.PUSH, 0)
        else -> HandResult(Outcome.LOSE, -wager)
    }
}

// A doubled 21 still wins, but only at even money, as does a split one where split hands earn no bonus
private fun PlayerHand.twentyOne(upcard: Card, splitBonuses: Boolean): HandResult {
    if (doubled || (split && !splitBonuses)) return HandResult(Outcome.WIN, wager)

    val bonus = bonus(cards)
    val superBonus = if (split) 0 else superBonus(bonus, upcard, wager)
    return HandResult(Outcome.WIN, (bonus?.odds ?: Odds.EVEN).on(wager) + superBonus, bonus = bonus, superBonus = superBonus)
}

private fun bonus(cards: List<Card>): Bonus? = when {
    cards.size >= 7 -> Bonus.SEVEN_CARD_21
    cards.size == 6 -> Bonus.SIX_CARD_21
    cards.size == 5 -> Bonus.FIVE_CARD_21
    cards.size == 3 && cards.map { it.rank }.toSet() == SIX_SEVEN_EIGHT -> cards.bySuits(Bonus.MIXED_678, Bonus.SUITED_678, Bonus.SPADED_678)
    cards.size == 3 && cards.all { it.rank == Rank.SEVEN } -> cards.bySuits(Bonus.MIXED_777, Bonus.SUITED_777, Bonus.SPADED_777)
    else -> null
}

private fun List<Card>.bySuits(mixed: Bonus, oneSuit: Bonus, spades: Bonus): Bonus = when {
    allSpades -> spades
    suited -> oneSuit
    else -> mixed
}

private const val FIVE_DOLLARS = 500L
private const val TWENTY_FIVE_DOLLARS = 2_500L

/**
 * The Super Bonus for a suited 7-7-7 against any 7, on top of its 7-7-7 bonus: $1,000 on a [wager] of $5 to $24.99 and $5,000
 * from $25. The regulations set no prize below $5.
 */
private fun superBonus(bonus: Bonus?, upcard: Card, wager: Long): Long = when {
    (bonus != Bonus.SUITED_777 && bonus != Bonus.SPADED_777) || upcard.rank != Rank.SEVEN -> 0
    wager >= TWENTY_FIVE_DOLLARS -> 500_000
    wager >= FIVE_DOLLARS -> 100_000
    else -> 0
}
