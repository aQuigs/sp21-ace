package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import java.io.Serializable
import kotlin.random.Random

/** Blackjack Ace's chips, its free top-ups and the bankroll it starts with, in cents. */
val CHIPS: List<Long> = listOf(500, 1_000, 2_500, 5_000, 10_000)
val TOP_UPS: List<Long> = listOf(10_000, 50_000, 100_000)
const val STARTING_BANKROLL = 100_000L

/**
 * The table between rounds and through one. Until a round is dealt, [bankroll] is the chips off the table and [bet] the ones in
 * the betting spot; a dealt [round] keeps its own. [hinted] is whether the player asked for a hint on the decision waiting. Once
 * the round is settled the dealer turns over [dealerDrawsShown] of the cards it drew, then the table shows one hand's result at a
 * time, [resultStep], as Blackjack Ace steps through split hands.
 */
data class Table(
    val bankroll: Long,
    val shoe: Shoe,
    val bet: Long = 0,
    val round: Round? = null,
    val dealerDrawsShown: Int = 0,
    val resultStep: Int = 0,
    val hinted: Boolean = false,
) : Serializable {
    /**
     * The chips the player owns. Chips riding on a round still count until it's settled, so a round cut short by a restart gives
     * back its bets rather than losing them. A hand the dealer's cards can't change counts as settled, though: a bust has lost
     * its bet, and a blackjack waiting on an insurance answer has won. Insurance taken on a round still going has lost.
     */
    val chips: Long
        get() = when {
            round == null -> bankroll + bet
            round.settled -> round.bankroll
            else -> round.bankroll + round.hands.sumOf { hand ->
                if (hand.finish == Finish.BUSTED || hand.isBlackjack) hand.wager + hand.settle(round.dealer).net else hand.wager
            }
        }

    /** The chips off the table, which the bankroll shows. A settled round's payout waits for the dealer's last card, as its result does. */
    val available: Long
        get() = when {
            round == null -> bankroll
            round.settled && !revealed -> round.bankroll - requireNotNull(round.returned)
            else -> round.bankroll
        }

    /** How many of the dealer's cards are face up: the upcard while the player plays, then the hole card and each card drawn. */
    val dealerCardsShown: Int get() = round?.let { if (it.settled) minOf(2 + dealerDrawsShown, it.dealer.size) else 1 } ?: 0

    /** Whether the round is settled with every dealer card face up, so its results show. */
    val revealed: Boolean get() = round?.settled == true && dealerCardsShown == round.dealer.size

    // As in Blackjack Ace, a split hand shown busting as it finished isn't shown again with the results
    private val resultHands: List<Int>
        get() = round?.hands?.let { hands -> hands.indices.filter { it == hands.lastIndex || hands[it].finish != Finish.BUSTED } }.orEmpty()

    /** The hand on show: the one being played, the last one played while the dealer plays, then the one whose result is shown. */
    val shownIndex: Int?
        get() = round?.let {
            when {
                !it.settled -> it.active
                revealed -> resultHands[resultStep]
                else -> it.hands.lastIndex
            }
        }

    val shownHand: PlayerHand? get() = shownIndex?.let { round?.hands?.get(it) }

    /** The shown hand's result, once the dealer's cards are all face up, or as a split hand busts, which the dealer can't change. */
    val shownResult: HandResult?
        get() = when {
            revealed -> shownIndex?.let { round?.results?.get(it) }
            round?.waitingForNextHand == true -> shownHand?.takeIf { it.finish == Finish.BUSTED }?.settle(round.dealer)
            else -> null
        }

    val hasNextResult: Boolean get() = revealed && resultStep < resultHands.lastIndex

    /** Whether NEXT has somewhere to go: the next split hand, or the next hand's result. */
    val hasNext: Boolean get() = round?.waitingForNextHand == true || hasNextResult

    fun addChip(value: Long): Table? = if (round == null && value <= bankroll) copy(bankroll = bankroll - value, bet = bet + value) else null

    fun clearBet(): Table = if (round == null) copy(bankroll = bankroll + bet, bet = 0) else this

    fun topUp(amount: Long): Table = if (round == null) copy(bankroll = bankroll + amount) else copy(round = round.copy(bankroll = round.bankroll + amount))

    /** Deals the bet, from a fresh shuffle once the cut card is out, offering [insurance] against an ace if the table does. */
    fun deal(ruleSet: RuleSet, random: Random, insurance: Boolean = false): Table? {
        if (round != null || bet == 0L) return null

        return copy(bet = 0, round = Round.deal(ruleSet, bet, bankroll + bet, shoe.forNextRound(random), insurance))
    }

    /** Whether the round waits on an answer to insurance, before anything else. */
    val offeringInsurance: Boolean get() = round?.insurance == Insurance.OFFERED

    /** Takes or declines the insurance offered. Blackjack Ace doesn't grade it or warn before taking it. */
    fun insure(take: Boolean): Table? = round?.insure(take)?.let { copy(round = it) }

    /** The move the hint shows, once asked for, until a move is made. */
    val hint: Move? get() = if (hinted) round?.correctMove() else null

    /** Whether a decision is waiting that the hint hasn't yet been shown for. */
    val canHint: Boolean get() = !hinted && round?.activeHand != null

    /** Shows the hint, which counts as help on the hand, as Blackjack Ace counts it. */
    fun showHint(): Table? = if (canHint) round?.withHelp()?.let { copy(round = it, hinted = true) } else null

    /** Backs out of a move the warning questioned, which Blackjack Ace counts as help, as it does the hint. */
    fun heedWarning(): Table? = round?.withHelp()?.let { copy(round = it) }

    /** Makes [move] on the hand waiting, which the round grades against the chart. */
    fun play(move: Move): Table? = round?.play(move)?.let { copy(round = it, hinted = false) }

    fun revealDealerCard(): Table? = if (round?.settled == true && !revealed) copy(dealerDrawsShown = dealerDrawsShown + 1) else null

    /**
     * Moves on to the next split hand, or shows the next hand's result, or after the last clears the table and bets the same again
     * if the bankroll covers it.
     */
    fun next(): Table? {
        round?.nextHand()?.let { return copy(round = it) }
        val round = round?.takeIf { revealed } ?: return null
        if (hasNextResult) return copy(resultStep = resultStep + 1)

        val again = if (round.bet <= round.bankroll) round.bet else 0
        return Table(bankroll = round.bankroll - again, shoe = round.shoe, bet = again)
    }
}
