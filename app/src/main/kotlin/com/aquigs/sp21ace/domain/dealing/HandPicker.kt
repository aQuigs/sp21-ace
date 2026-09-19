package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

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

    private val asAtATable = Weighted(groups) { it.ways.total }

    /** The hands still dealt. */
    internal val hands: List<HandKey> get() = groups.map { it.hand }

    /**
     * A hand as often as a round at a table deals it to a player following the chart, or under Prioritize worse hands in inverse
     * proportion to the accuracy [history] gives it. That weight is split over the ways still dealt, so a hand the switches cut
     * down to a few suits keeps all of it.
     */
    fun pick(history: List<PracticeAnswer>, random: Random = Random.Default): TrainerHand {
        val picks = when (customization.handsDealt) {
            HandsDealt.RANDOM -> asAtATable
            HandsDealt.PRIORITIZE_WORSE -> history.tallyByHand(chart).let { tallies -> Weighted(groups) { weight(tallies[it.hand]) } }
        }

        return picks.pick(random).ways.pick(random).deal(random)
    }
}
