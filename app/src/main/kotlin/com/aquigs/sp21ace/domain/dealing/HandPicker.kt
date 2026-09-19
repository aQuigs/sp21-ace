package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.countsCards
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

// A table deals card-count hands too seldom to learn the rarer ones, such as the 5- and 6-card hits, so their switch keeps a share of the deals for them
private const val CARD_COUNT_SHARE = 0.25

/**
 * Deals the trainer's hands under [rules]: only the types still switched on, and hands of 3 or more cards only while their switch
 * is on, or every type when none of those can come up. Built once per change of settings, so a deal only weighs and picks.
 */
class HandPicker(rules: RuleSet, private val customization: HandCustomization) {
    private class Group(val hand: HandKey, val ways: Weighted<Dealable>)

    private val chart = StrategyCharts.forRules(rules)

    private val groups: List<Group> = dealableHands(rules).hands.filterKeys { customization.multiCardHands || it !is MultiCardHand }.let { all ->
        val possible = all.values.flatten().mapTo(HashSet()) { it.type }
        val dealt = (possible - customization.switchedOff).ifEmpty { possible }

        all.mapNotNull { (hand, ways) -> ways.filter { it.type in dealt }.takeIf { it.isNotEmpty() }?.let { Group(hand, Weighted(it) { way -> way.chance }) } }
    }

    private val cardCount: List<Group> =
        if (customization.cardCountHands) groups.filter { it.hand is MultiCardHand && chart.play(it.hand.row, it.hand.upcard).countsCards } else emptyList()

    private val asAtATable = Weighted(groups) { it.ways.total }

    /** The hands still dealt. */
    internal val hands: List<HandKey> get() = groups.map { it.hand }

    /**
     * A hand as often as a round at a table deals it to a player following the chart, or under Prioritize worse hands in inverse
     * proportion to the accuracy [history] gives it. That weight is split over the ways still dealt, so a hand the switches cut
     * down to a few suits keeps all of it. With the card-count switch on, [CARD_COUNT_SHARE] of the deals are a card-count hand
     * instead, each as often as the next, or under Prioritize worse hands by its accuracy.
     */
    fun pick(history: List<PracticeAnswer>, random: Random = Random.Default): TrainerHand {
        val cardCountDeal = cardCount.isNotEmpty() && random.nextDouble() < CARD_COUNT_SHARE
        val group = when (customization.handsDealt) {
            HandsDealt.RANDOM -> if (cardCountDeal) cardCount.random(random) else asAtATable.pick(random)
            HandsDealt.PRIORITIZE_WORSE ->
                history.tallyByHand(chart).let { tallies -> Weighted(if (cardCountDeal) cardCount else groups) { weight(tallies[it.hand]) } }.pick(random)
        }

        return group.ways.pick(random).deal(random)
    }
}
