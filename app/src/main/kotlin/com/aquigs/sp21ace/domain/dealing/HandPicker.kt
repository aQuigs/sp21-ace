package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

/**
 * Deals the trainer's hands under [rules]: only the types still switched on, or every type when none of those can come up.
 * Built once per change of settings, so a deal only weighs and picks.
 */
class HandPicker(rules: RuleSet, private val customization: HandCustomization) {
    // A card-value hand's suits still dealt, picked from by their ways to deal
    private class Group(val values: HandValues, val hands: List<DealableHand>) {
        val runningWays: List<Int> = hands.map { it.ways }.runningReduce { total, ways -> total + ways }
    }

    private val groups: List<Group> = dealableHands(rules).let { all ->
        val possible = all.values.flatten().mapTo(HashSet()) { it.type }
        val dealt = (possible - customization.switchedOff).ifEmpty { possible }
        all.mapNotNull { (values, hands) -> hands.filter { it.type in dealt }.takeIf { it.isNotEmpty() }?.let { Group(values, it) } }
    }

    private val randomWeights: List<Double> = groups.map { it.runningWays.last().toDouble() }.running()

    /** The card-value hands still dealt. */
    internal val hands: List<HandValues> get() = groups.map { it.values }

    /**
     * A hand as often as a shoe deals it, or under Prioritize worse hands in inverse proportion to the accuracy [history] gives
     * its card values. That weight is split over the suits still dealt, so a hand the switches cut down to a few suits keeps
     * all of it.
     */
    fun pick(history: List<PracticeAnswer>, random: Random = Random.Default): TrainerHand {
        val weights = when (customization.handsDealt) {
            HandsDealt.RANDOM -> randomWeights
            HandsDealt.PRIORITIZE_WORSE -> history.tallyByHand().let { tallies -> groups.map { weight(tallies[it.values]) }.running() }
        }

        val target = random.nextDouble(weights.last())
        val group = groups[weights.indexOfFirst { it > target }]
        val way = random.nextInt(group.runningWays.last())
        return group.hands[group.runningWays.indexOfFirst { it > way }].deal(random)
    }
}

private fun List<Double>.running() = runningReduce { total, weight -> total + weight }
