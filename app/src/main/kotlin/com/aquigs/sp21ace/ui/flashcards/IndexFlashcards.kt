package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.runtime.Immutable
import com.aquigs.sp21ace.domain.indices.Flashcard
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.FlashcardRun
import com.aquigs.sp21ace.domain.indices.IndexPresets

/** What the card-counting screens show: the flashcard run, the index presets and every flashcard answer. */
@Immutable
data class IndexFlashcardsState(val run: FlashcardRun, val presets: IndexPresets, val history: List<FlashcardAnswer>)

/** What the card-counting screens ask for, which the composition root carries out with the stores. */
interface IndexFlashcardsActions {
    /** [number] tapped as the answer to [asked], the card that frame showed. */
    fun answer(asked: Flashcard, number: Int)

    /** [card] is on screen with nothing over it. */
    fun shown(card: Flashcard)

    /** The card on screen was covered or left, so its answer will have no time. */
    fun interrupted()
}
