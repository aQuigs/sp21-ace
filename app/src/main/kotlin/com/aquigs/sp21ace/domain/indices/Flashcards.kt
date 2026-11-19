package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import java.io.Serializable
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.time.Duration

// The pad always offers the published range, so a preset of small numbers doesn't give the answer away by its size
private const val MIN_PAD_REACH = 5

/** The numbers a flashcard's pad offers for the preset, −m to +m, m at least 5 and as large as any number switched on. */
val IndexPreset.flashcardAnswers: IntRange
    get() = maxOf(MIN_PAD_REACH, enabledIndices.maxOfOrNull { it.threshold.absoluteValue } ?: 0).let { -it..it }

/** A flashcard: an [index] and the cards that ask for it, the [player]'s hand, none for insurance, against the dealer's [upcard]. */
data class Flashcard(val index: PlayIndex, val player: List<Card>, val upcard: Card) : Serializable

/** The number given for a card, which is right when it is the card's index. */
data class FlashcardGrade(val card: Flashcard, val answer: Int) : Serializable {
    val isCorrect: Boolean get() = answer == card.index.threshold
}

/**
 * An endless flashcard run: the [card] showing, or none while no index is switched on, and the verdict on the last one. Serializable
 * so the activity saves it as it is through recreation and process death.
 */
data class FlashcardRun(val card: Flashcard?, val lastGrade: FlashcardGrade? = null) : Serializable {
    /**
     * The run with a card from [dealer] in place of one whose index it no longer deals, such as after a preset swap or an edit, so
     * a card is never graded against a stale number. A card it still deals stays.
     */
    fun dealtFrom(dealer: FlashcardDealer, deal: (previous: Flashcard?) -> Flashcard? = dealer::deal): FlashcardRun =
        if (card != null && card.index in dealer.indices) this else copy(card = deal(card))
}

/**
 * A flashcard answer as the history keeps it. Stats key on the [index] itself, its number and direction included, so the same fact
 * shares its record across presets. [time] is how long the card was on screen before the answer, or null when its display was
 * interrupted. [cards] is how many the player's hand held, none for insurance.
 */
data class FlashcardAnswer(val answeredAt: Instant, val game: Game, val index: PlayIndex, val answer: Int, val time: Duration?, val cards: Int) {
    init {
        require(time == null || !time.isNegative()) { "A negative answer time: $time" }
        require(cards >= 0) { "A hand of $cards cards" }
    }

    val isCorrect: Boolean get() = answer == index.threshold
}

/** An answer as the history records it, and the run it leaves with the next card dealt. */
data class AnsweredFlashcard(val run: FlashcardRun, val answer: FlashcardAnswer)

/**
 * Grades [number] as the answer to [asked] and deals the next card at once, because a run never waits for a continue tap. [deal] is
 * handed the answer, so a deal that weighs answers can count this one. A tap on a card no longer showing, such as a second tap
 * before the screen redraws, or on one whose index [dealer] no longer deals, grades nothing: null.
 */
fun FlashcardRun.answer(
    asked: Flashcard,
    number: Int,
    dealer: FlashcardDealer,
    time: Duration?,
    answeredAt: Instant,
    deal: (FlashcardAnswer) -> Flashcard? = { dealer.deal(asked) },
): AnsweredFlashcard? {
    if (asked != card || asked.index !in dealer.indices) return null

    val recorded = FlashcardAnswer(answeredAt, dealer.game, asked.index, number, time, asked.player.size)
    return AnsweredFlashcard(FlashcardRun(deal(recorded), FlashcardGrade(asked, number)), recorded)
}
