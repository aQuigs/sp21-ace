package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.drill.AnswerPad
import com.aquigs.sp21ace.domain.drill.DrillCard
import com.aquigs.sp21ace.domain.drill.DrillGrade
import com.aquigs.sp21ace.domain.drill.DrillRun
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.time.Duration

// The pad always offers the published range, so a preset of small numbers doesn't give the answer away by its size
private const val MIN_PAD_REACH = 5

/** The pad a flashcard is answered on for the preset, −m to +m, m at least 5 and as large as any number switched on. */
val IndexPreset.flashcardPad: AnswerPad
    get() = maxOf(MIN_PAD_REACH, enabledIndices.maxOfOrNull { it.threshold.absoluteValue } ?: 0).let { AnswerPad(-it..it) }

/** An index flashcard: an [index] and the cards that ask for it, the [player]'s hand, none for insurance, against the dealer's [upcard]. */
data class Flashcard(val index: PlayIndex, val player: List<Card>, val upcard: Card) : DrillCard {
    override val answer: Int get() = index.threshold
}

typealias FlashcardGrade = DrillGrade<Flashcard>

typealias FlashcardRun = DrillRun<Flashcard>

/**
 * A flashcard answer as the history keeps it. Stats key on the [index] itself, its number and direction included, so the same fact
 * shares its record across presets. [time] is how long the card was on screen before the answer, or null when its display was
 * interrupted. [cards] is how many the player's hand held, none for insurance.
 */
data class FlashcardAnswer(val answeredAt: Instant, val game: Game, val index: PlayIndex, val answer: Int, val time: Duration?, val cards: Int) {
    init {
        require(time == null || (!time.isNegative() && time.isFinite())) { "An answer time out of range: $time" }
        require(cards >= 0) { "A hand of $cards cards" }
    }

    val isCorrect: Boolean get() = answer == index.threshold
}
