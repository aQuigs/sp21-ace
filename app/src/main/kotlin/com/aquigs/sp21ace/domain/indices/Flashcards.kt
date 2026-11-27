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

/**
 * An index flashcard: a [fact] and the cards that ask for it, the [player]'s hand, none for insurance, against the dealer's
 * [upcard]. Its answer is null where there is no index.
 */
data class Flashcard(val fact: IndexFact, val player: List<Card>, val upcard: Card) : DrillCard<Int?> {
    override val answer: Int? get() = fact.threshold
}

typealias FlashcardGrade = DrillGrade<Flashcard, Int?>

typealias FlashcardRun = DrillRun<Flashcard, Int?>

/**
 * A flashcard answer as the history keeps it, [answer] null for no index. Stats key on the [fact] itself, an index's number and
 * direction included, so the same fact shares its record across presets. [time] is how long the card was on screen before the answer, or null when its display was
 * interrupted. [cards] is how many the player's hand held, none for insurance.
 */
data class FlashcardAnswer(val answeredAt: Instant, val game: Game, val fact: IndexFact, val answer: Int?, val time: Duration?, val cards: Int) {
    init {
        require(time == null || (!time.isNegative() && time.isFinite())) { "An answer time out of range: $time" }
        require(cards >= 0) { "A hand of $cards cards" }
    }

    val isCorrect: Boolean get() = answer == fact.threshold
}
