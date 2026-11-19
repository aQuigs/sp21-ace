package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.runtime.Immutable
import com.aquigs.sp21ace.domain.indices.Flashcard
import com.aquigs.sp21ace.domain.indices.FlashcardOptions
import com.aquigs.sp21ace.domain.indices.FlashcardRun
import com.aquigs.sp21ace.domain.indices.IndexPresets

/** What the card-counting screens show: the flashcard run, the index presets and the flashcard options. */
@Immutable
data class IndexFlashcardsState(val run: FlashcardRun, val presets: IndexPresets, val options: FlashcardOptions)

/** What the card-counting screens ask for, which the composition root carries out with the stores. */
interface IndexFlashcardsActions {
    /** [number] tapped as the answer to [asked], the card that frame showed. */
    fun answer(asked: Flashcard, number: Int)

    /** [card] is on screen with nothing over it. */
    fun shown(card: Flashcard)

    /** The card on screen was covered or left, so its answer will have no time. */
    fun interrupted()

    /** The presets as an edit on the Indices or Index Presets page leaves them. */
    fun changePresets(presets: IndexPresets)

    fun changeOptions(options: FlashcardOptions)
}
