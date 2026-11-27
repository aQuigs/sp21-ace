package com.aquigs.sp21ace.domain.drill

import com.aquigs.sp21ace.domain.dealing.Weighted
import com.aquigs.sp21ace.domain.dealing.weight
import com.aquigs.sp21ace.domain.history.Tally
import kotlin.random.Random

/** What a flashcard asks for next: anything its drill covers as often as the next, or what is answered worse more often. */
enum class CardsShown {
    RANDOM,
    PRIORITIZE_WORSE,
    ;

    /** One of [candidates], as often as their [tallies] say it should come up, which are worked out only when they count. */
    internal fun <T> pick(candidates: List<T>, random: Random, tallies: () -> Map<T, Tally>): T = when (this) {
        RANDOM -> candidates.random(random)
        PRIORITIZE_WORSE -> tallies().let { Weighted(candidates) { candidate -> weight(it[candidate]) } }.pick(random)
    }
}
