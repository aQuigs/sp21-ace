package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.bonusHand
import com.aquigs.sp21ace.domain.strategy.countsCards
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

// A table deals card-count hands too seldom to learn the rarer ones, such as the 5- and 6-card hits, so their switch keeps a share of the deals for them
private const val CARD_COUNT_SHARE = 0.25

// Bonus hands are rarer still, a spaded 6-8 about 1 hand in 20,000, but there are only 11, so a smaller share deals each often enough
private const val BONUS_SHARE = 0.1

/**
 * Deals the trainer's hands under [rules]: only the types still switched on, and hands of 3 or more cards only while their switch
 * is on, or every type when none of those can come up. A doubled hand has 3 or more cards whatever the switch, but with it off
 * only doubles from two cards. Built once per change of settings, so a deal only weighs and picks.
 */
class HandPicker(rules: RuleSet, private val customization: HandCustomization) {
    private class Group(val hand: HandKey, val ways: List<Dealable>) {
        val picks = Weighted(ways) { it.chance }
    }

    // Hands a deal is drawn from, for its share of the deals: a switch's own hands, each as often as the next, or else every hand
    // still dealt, as often as a table deals it
    private class Pool(val share: Double, val groups: List<Group>, val atRandom: Weighted<Group>)

    private val chart = StrategyCharts.forRules(rules)

    private val groups: List<Group> = dealableHands(rules).let { if (customization.multiCardHands) it.hands else it.withoutHits }.let { all ->
        // A hand carries the move it's graded with, so every way to deal it is graded alike
        val possible = all.values.mapTo(HashSet()) { it.first().type }
        val dealt = (possible - customization.switchedOff).ifEmpty { possible }

        all.filterValues { it.first().type in dealt }.map { (hand, ways) -> Group(hand, ways) }
    }

    private val pools: Weighted<Pool> = run {
        val dealt = groups.mapTo(HashSet()) { it.hand }
        val boosts = listOfNotNull(
            boost(customization.cardCountHands, CARD_COUNT_SHARE) { it.hand is MultiCardHand && chart.play(it.hand.row, it.hand.upcard).countsCards },
            // The other suits of a bonus hand only beside the suits that hit for the bonus, the move they're told apart from
            boost(customization.bonusHands, BONUS_SHARE) { group ->
                val way = group.ways.first()
                group.hand is HandValues && group.hand.copy(move = Move.HIT) in dealt && way is DealableHand && chart.bonusHand(way.player, way.upcard)
            },
        )

        val rest = 1 - boosts.sumOf { it.share }
        check(rest > 0) { "The switches' shares leave no deals as a table deals them" }

        Weighted(boosts + Pool(rest, groups, Weighted(groups) { it.picks.total })) { it.share }
    }

    /** The hands still dealt. */
    internal val hands: List<HandKey> get() = groups.map { it.hand }

    /**
     * A hand as often as a round at a table deals it to a player following the chart, or under Prioritize worse hands in inverse
     * proportion to the accuracy [history] gives it. With the card-count or bonus switch on, its share of the deals,
     * [CARD_COUNT_SHARE] or [BONUS_SHARE], is one of its hands instead, each as often as the next, or under Prioritize worse hands
     * by its accuracy.
     */
    fun pick(history: List<PracticeAnswer>, random: Random = Random.Default): TrainerHand {
        val pool = pools.pick(random)
        val group = when (customization.handsDealt) {
            HandsDealt.RANDOM -> pool.atRandom.pick(random)
            HandsDealt.PRIORITIZE_WORSE -> history.tallyByHand(chart).let { tallies -> Weighted(pool.groups) { weight(tallies[it.hand]) } }.pick(random)
        }

        return group.picks.pick(random).deal(random)
    }

    private fun boost(on: Boolean, share: Double, drills: (Group) -> Boolean): Pool? =
        if (on) groups.filter(drills).takeIf { it.isNotEmpty() }?.let { Pool(share, it, Weighted(it) { 1.0 }) } else null
}
