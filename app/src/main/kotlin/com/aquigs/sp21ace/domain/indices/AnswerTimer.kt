package com.aquigs.sp21ace.domain.indices

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Times each flashcard from when it is first shown to its answer, by [clock], a monotonic time in milliseconds. A card is timed
 * only if this timer saw it [dealt] and then [shown] with nothing [interrupted] it since, so a card that outlives the timer, as
 * one restored in a recreated activity does, is never timed from partway through.
 */
class AnswerTimer(private val clock: () -> Long) {
    private var card: Flashcard? = null
    private var shownAt: Long? = null
    private var waiting = false

    /** [card] is the new card, not yet on screen. */
    fun dealt(card: Flashcard?) {
        this.card = card
        shownAt = null
        waiting = card != null
    }

    /** [card] is on screen, which starts its time if it is the card dealt and is showing for the first time. */
    fun shown(card: Flashcard) {
        if (waiting && card == this.card) {
            shownAt = clock()
            waiting = false
        }
    }

    /** The card showing was covered or left, so its answer will have no time. */
    fun interrupted() {
        shownAt = null
    }

    /** The time to show on [card]: zero until it first shows, then its time, or null when it isn't timed. */
    fun display(card: Flashcard): Duration? = if (waiting && card == this.card) Duration.ZERO else elapsed(card)

    /** How long [card] has been showing, or null when it isn't timed. */
    fun elapsed(card: Flashcard): Duration? = shownAt?.takeIf { card == this.card }?.let { (clock() - it).coerceAtLeast(0).milliseconds }
}
