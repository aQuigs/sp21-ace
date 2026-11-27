package com.aquigs.sp21ace.domain.drill

import com.aquigs.sp21ace.domain.history.Tally
import java.io.Serializable
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A flashcard whose right answer is [answer], an [N]. Serializable so the activity saves it with the run showing it. */
interface DrillCard<N> : Serializable {
    val answer: N
}

/**
 * The answer given for a card, which is right when it is the card's own, the [tally] of every answer to what the card asked, this
 * one included, or null when the history hadn't loaded, and the [time] it took, or null when it wasn't timed. The time is kept in
 * milliseconds because a Duration isn't Serializable.
 */
data class DrillGrade<C : DrillCard<N>, N>(val card: C, val answer: N, val tally: Tally?, val timeMillis: Long?) : Serializable {
    val isCorrect: Boolean get() = answer == card.answer
    val time: Duration? get() = timeMillis?.milliseconds
}

/** Deals a drill's cards, each a [C] answered with an [N], and turns an answer to one into the [A] its history keeps. */
interface CardDealer<C : DrillCard<N>, N, A> {
    /** A card other than [previous], or null with none to deal. [history] is every answer given, for a deal that weighs them. */
    fun deal(previous: C?, history: List<A> = emptyList(), random: Random = Random.Default): C?

    /** Whether [card] is one this dealer could deal, so a card left from other options is redealt. */
    fun couldDeal(card: C): Boolean

    /** [answer] given for [card] as the history keeps it, [time] null when the card wasn't timed. */
    fun record(card: C, answer: N, time: Duration?, answeredAt: Instant): A

    /** How often [history] has what [card] asks answered right, whatever else the card shows. */
    fun tally(card: C, history: List<A>): Tally
}

/**
 * An endless flashcard run: the [card] showing, or none while there is none to deal, the verdict on the last one, and the
 * [streak] of right answers. Serializable so the activity saves it as it is through recreation and process death.
 */
data class DrillRun<C : DrillCard<N>, N>(val card: C?, val lastGrade: DrillGrade<C, N>? = null, val streak: Streak = Streak()) : Serializable {
    /**
     * The run with a card from [dealer] in place of one it could not deal, such as after a change of options, so a card is never
     * graded against a stale answer. A card it could deal stays. With nothing left to deal, the last verdict goes too, since it
     * may be on something no longer drilled.
     */
    fun dealtFrom(dealer: CardDealer<C, N, *>, deal: (previous: C?) -> C? = { dealer.deal(it) }): DrillRun<C, N> {
        if (card != null && dealer.couldDeal(card)) return this

        val next = deal(card)
        return copy(card = next, lastGrade = lastGrade.takeIf { next != null })
    }

    /** The run once the history is cleared, with nothing left that counts its answers. */
    fun historyCleared(): DrillRun<C, N> = copy(lastGrade = null, streak = Streak())

    /**
     * Grades [answer] as the answer to [asked] and deals the next card at once, because a run never waits for a continue tap.
     * [deal] is handed [history], null until it loads, with this answer added, which the store holds only once it is recorded, so a
     * deal that weighs answers counts it. A tap on a card no longer showing, such as a second tap before the screen redraws, or on one [dealer]
     * could not deal, grades nothing: null.
     */
    fun <A> answer(
        asked: C,
        answer: N,
        dealer: CardDealer<C, N, A>,
        time: Duration?,
        answeredAt: Instant,
        history: List<A>? = emptyList(),
        deal: (previous: C, history: List<A>) -> C? = { previous, answers -> dealer.deal(previous, answers) },
    ): AnsweredCard<C, N, A>? {
        if (asked != card || !dealer.couldDeal(asked)) return null

        val recorded = dealer.record(asked, answer, time, answeredAt)
        val answers = history.orEmpty() + recorded
        val grade = DrillGrade(asked, answer, history?.let { dealer.tally(asked, answers) }, time?.inWholeMilliseconds)
        return AnsweredCard(DrillRun(deal(asked, answers), grade, streak.after(grade.isCorrect)), recorded)
    }
}

/** An answer as the history records it, and the run it leaves with the next card dealt. */
data class AnsweredCard<C : DrillCard<N>, N, A>(val run: DrillRun<C, N>, val answer: A)
