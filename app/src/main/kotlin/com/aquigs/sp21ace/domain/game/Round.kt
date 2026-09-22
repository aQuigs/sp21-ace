package com.aquigs.sp21ace.domain.game

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.isPair
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.TableRules
import java.io.Serializable

/** Split to 4 hands, aces included. */
const val MAX_HANDS = 4

/** Masque's Double-Double Down: a double and two redoubles, at most 8 times the bet. */
const val MAX_DOUBLES = 3

/** How a hand stopped taking decisions. */
enum class Finish { STOOD, BUSTED, SURRENDERED, RESCUED }

/** Insurance pays 2 to 1, as New Jersey's, Pennsylvania's and Maryland's rules for Spanish 21 have it. */
val INSURANCE_ODDS = Odds.TWO_TO_ONE

/** Insurance against a dealer's ace, offered before the peek. Declining it leaves none. */
enum class Insurance { OFFERED, TAKEN }

/**
 * One of the player's hands and the [wager] on it in cents, which every double doubles. A [split] hand can't be a blackjack or
 * surrendered, and earns no Super Bonus. [strategy] is how its decisions measured up to the chart.
 */
data class PlayerHand(
    val cards: List<Card>,
    val wager: Long,
    val doubles: Int = 0,
    val split: Boolean = false,
    val finish: Finish? = null,
    val strategy: StrategyRecord = StrategyRecord(),
) : Serializable {
    val total: HandTotal get() = cards.total()
    val doubled: Boolean get() = doubles > 0
    val isBlackjack: Boolean get() = !split && cards.isBlackjack()
}

/**
 * A round from the deal to the settlement, in cents. [bankroll] is the chips off the table: the bet, each double, each split and
 * insurance move chips from it onto the table, and the settlement pays back what the hands and insurance return. The dealer's
 * second card stays face down until the round is [settled]. [active] is the hand being played, and once the round is settled it
 * is past the last hand. As in Blackjack Ace, a split hand that finishes before the last stays active, with nothing to decide,
 * until [nextHand] moves on. [insurance] is null unless it's on offer or taken. The round plays to the [rules] it was dealt
 * under, whatever the table's become since.
 */
data class Round(
    val rules: TableRules,
    val bet: Long,
    val bankroll: Long,
    val shoe: Shoe,
    val dealer: List<Card>,
    val hands: List<PlayerHand>,
    val active: Int = 0,
    val results: List<HandResult>? = null,
    val insurance: Insurance? = null,
) : Serializable {
    val ruleSet: RuleSet get() = rules.ruleSet
    val upcard: Card get() = dealer.first()
    val settled: Boolean get() = results != null

    /**
     * The hand waiting on a decision, or null while insurance waits on an answer, while a finished split hand waits for the next,
     * and once the round is settled.
     */
    val activeHand: PlayerHand?
        get() = if (settled || offeringInsurance) null else hands[active].takeIf { it.finish == null }

    /**
     * The chips the player owns. Chips riding on the round still count until it's settled, so a round cut short by a restart gives
     * back its bets rather than losing them. A hand the dealer's cards can't change counts as settled, though: a bust has lost
     * its bet, and a blackjack waiting on an insurance answer has won. Insurance taken on a round still going has lost.
     */
    val chips: Long
        get() = if (settled) {
            bankroll
        } else {
            bankroll + hands.sumOf { if (it.finish == Finish.BUSTED || it.isBlackjack) it.wager + it.settle(dealer, rules.splitBonuses).net else it.wager }
        }

    val waitingForNextHand: Boolean get() = !settled && hands[active].finish != null

    /** The round moved on from a finished split hand to the next, or null unless one waits. */
    fun nextHand(): Round? = if (waitingForNextHand) copy(active = active + 1).advance() else null

    val offeringInsurance: Boolean get() = insurance == Insurance.OFFERED

    /** Insurance costs half the bet, as in Blackjack Ace. */
    val insuranceBet: Long get() = bet / 2

    val insuranceWon: Boolean get() = insurance == Insurance.TAKEN && dealer.isBlackjack()

    val insuranceNet: Long
        get() = when {
            insuranceWon -> INSURANCE_ODDS.on(insuranceBet)
            insurance == Insurance.TAKEN -> -insuranceBet
            else -> 0
        }

    /** What the settlement paid back, once the round is settled: each hand's wager and what it won or lost, and insurance that won. */
    val returned: Long?
        get() = results?.let { results ->
            hands.zip(results).sumOf { (hand, result) -> hand.wager + result.net } + if (insuranceWon) insuranceBet + insuranceNet else 0
        }

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

        return setOfNotNull(
            Move.HIT,
            Move.STAND,
            Move.DOUBLE.takeIf { covered },
            Move.SPLIT.takeIf { hand.cards.isPair && hands.size < MAX_HANDS && covered },
            Move.SURRENDER.takeIf { hands.size == 1 && hand.cards.size == 2 },
        )
    }

    /** The round after the active hand makes [move], graded against the chart on that hand, or null where it can't. */
    fun play(move: Move): Round? {
        if (move !in moves()) return null

        val hand = hands[active].let { it.copy(strategy = it.strategy.withDecision(correct = move == correctMove())) }
        return when (move) {
            Move.HIT -> dealTo(hand)
            Move.STAND -> replaceActive(hand.copy(finish = Finish.STOOD))
            Move.SURRENDER -> replaceActive(hand.copy(finish = Finish.SURRENDERED))
            Move.RESCUE -> replaceActive(hand.copy(finish = Finish.RESCUED))
            Move.DOUBLE, Move.REDOUBLE -> copy(bankroll = bankroll - hand.wager).dealTo(hand.copy(wager = hand.wager * 2, doubles = hand.doubles + 1))
            Move.SPLIT -> {
                val (first, second) = hand.cards
                // The split's decision stays with the first of its hands, which takes the split hand's place
                val halves = listOf(PlayerHand(listOf(first), bet, split = true, strategy = hand.strategy), PlayerHand(listOf(second), bet, split = true))
                copy(bankroll = bankroll - bet, hands = hands.take(active) + halves + hands.drop(active + 1))
            }
        }.advance()
    }

    /** The round once offered insurance is [taken][take] or declined, then the peek; null unless it's waiting on an answer. */
    fun insure(take: Boolean): Round? = when {
        !offeringInsurance -> null
        take -> copy(bankroll = bankroll - insuranceBet, insurance = Insurance.TAKEN).peeked()
        else -> copy(insurance = null).peeked()
    }

    /** The round with help counted on the active hand, as the hint or a warning backed out of gives it, or null once it's settled. */
    fun withHelp(): Round? = activeHand?.let { replaceActive(it.copy(strategy = it.strategy.copy(helped = true))) }

    // Draws a card into the active hand, which stands on 21 and ends on a bust
    private fun dealTo(hand: PlayerHand): Round {
        val (card, rest) = shoe.draw()
        return copy(shoe = rest).replaceActive(hand.copy(cards = hand.cards + card).finishedAt21())
    }

    private fun replaceActive(hand: PlayerHand): Round = copy(hands = hands.toMutableList().apply { set(active, hand) })

    // Deals a split hand its second card once it's reached, and once the last hand is finished the dealer plays
    private fun advance(): Round {
        val hand = hands[active]
        return when {
            hand.cards.size == 1 -> dealTo(hand).advance()
            hand.finish == null || active < hands.lastIndex -> this
            else -> dealerPlays()
        }
    }

    // The dealer only draws while a hand waits on the dealer's total
    private fun dealerPlays(): Round {
        var round = this
        while (hands.any { it.awaitsDealer } && round.dealer.total().dealerHits(ruleSet)) {
            val (card, rest) = round.shoe.draw()
            round = round.copy(shoe = rest, dealer = round.dealer + card)
        }
        return round.payOut()
    }

    private fun payOut(): Round {
        val settled = copy(active = hands.size, results = hands.map { it.settle(dealer, rules.splitBonuses) })
        return settled.copy(bankroll = bankroll + requireNotNull(settled.returned))
    }

    private fun peeked(): Round = if (hands[0].isBlackjack || dealer.isBlackjack()) payOut() else this

    companion object {
        /**
         * Deals a round of [bet] cents from [shoe], a card each to the player and the dealer and then a second each, the dealer's
         * face down. A player blackjack is paid at once, and a dealer showing an ace or a face card peeks for blackjack, so either
         * one settles the round before the player acts. Where the [rules] offer insurance, a dealer showing an ace offers it
         * first, as long as the bankroll covers it, and peeks once it's answered. The bet is an even number of cents, so every
         * half the rules pay or give back is exact.
         */
        fun deal(rules: TableRules, bet: Long, bankroll: Long, shoe: Shoe): Round {
            require(bet in 1..bankroll) { "A bet of $bet needs a bankroll to cover it, not $bankroll" }
            require(bet % 2 == 0L) { "A bet of $bet cents has no exact half" }

            val (cards, rest) = shoe.startingRound().draw(4)
            val (first, upcard, second, hole) = cards
            val round = Round(rules, bet, bankroll - bet, rest, listOf(upcard, hole), listOf(PlayerHand(listOf(first, second), bet)))

            // Blackjack Ace offers it even on a player blackjack, which Masque's rules let a player insure once it's paid
            val offered = rules.insurance && upcard.rank == Rank.ACE && round.bankroll >= round.insuranceBet
            return if (offered) round.copy(insurance = Insurance.OFFERED) else round.peeked()
        }
    }
}

private fun PlayerHand.finishedAt21(): PlayerHand = when {
    total.value > 21 -> copy(finish = Finish.BUSTED)
    total.value == 21 -> copy(finish = Finish.STOOD)
    else -> this
}

private fun HandTotal.dealerHits(ruleSet: RuleSet): Boolean = value < 17 || (value == 17 && soft && ruleSet.dealerHitsSoft17)
