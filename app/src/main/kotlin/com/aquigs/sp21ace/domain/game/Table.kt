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
 * time, [resultIndex], as Blackjack Ace steps through split hands.
 */
data class Table(
    val bankroll: Long,
    val shoe: Shoe,
    val bet: Long = 0,
    val round: Round? = null,
    val dealerDrawsShown: Int = 0,
    val resultIndex: Int = 0,
    val hinted: Boolean = false,
) : Serializable {
    /**
     * The chips the player owns. Chips riding on a round still count until it's settled, so a round cut short by a restart gives
     * back its bets rather than losing them.
     */
    val chips: Long
        get() = when {
            round == null -> bankroll + bet
            round.settled -> round.bankroll
            else -> round.bankroll + round.hands.sumOf { it.wager }
        }

    /** The chips off the table, which the bankroll shows. A settled round's payout waits for the dealer's last card, as its result does. */
    val available: Long
        get() = when {
            round == null -> bankroll
            round.settled && !revealed -> round.bankroll - round.hands.sumOf { it.wager } - requireNotNull(round.net)
            else -> round.bankroll
        }

    /** How many of the dealer's cards are face up: the upcard while the player plays, then the hole card and each card drawn. */
    val dealerCardsShown: Int get() = round?.let { if (it.settled) minOf(2 + dealerDrawsShown, it.dealer.size) else 1 } ?: 0

    /** Whether the round is settled with every dealer card face up, so its results show. */
    val revealed: Boolean get() = round?.settled == true && dealerCardsShown == round.dealer.size

    /** The hand on show: the one being played, the last one played while the dealer plays, then the one whose result is shown. */
    val shownIndex: Int?
        get() = round?.let {
            when {
                !it.settled -> it.active
                revealed -> resultIndex
                else -> it.hands.lastIndex
            }
        }

    val shownHand: PlayerHand? get() = shownIndex?.let { round?.hands?.get(it) }

    val shownResult: HandResult? get() = if (revealed) round?.results?.get(resultIndex) else null

    val hasNextResult: Boolean get() = revealed && resultIndex < requireNotNull(round).hands.lastIndex

    fun addChip(value: Long): Table? = if (round == null && value <= bankroll) copy(bankroll = bankroll - value, bet = bet + value) else null

    fun clearBet(): Table = if (round == null) copy(bankroll = bankroll + bet, bet = 0) else this

    fun topUp(amount: Long): Table = if (round == null) copy(bankroll = bankroll + amount) else copy(round = round.copy(bankroll = round.bankroll + amount))

    /** Deals the bet, from a fresh shuffle once the cut card is out. */
    fun deal(ruleSet: RuleSet, random: Random): Table? {
        if (round != null || bet == 0L) return null

        return copy(bet = 0, round = Round.deal(ruleSet, bet, bankroll + bet, shoe.forNextRound(random)))
    }

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

    /** Shows the next hand's result, or after the last clears the table and bets the same again if the bankroll covers it. */
    fun next(): Table? {
        val round = round?.takeIf { revealed } ?: return null
        if (hasNextResult) return copy(resultIndex = resultIndex + 1)

        val again = if (round.bet <= round.bankroll) round.bet else 0
        return Table(bankroll = round.bankroll - again, shoe = round.shoe, bet = again)
    }
}
