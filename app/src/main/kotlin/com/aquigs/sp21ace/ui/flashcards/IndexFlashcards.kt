package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.runtime.Immutable
import com.aquigs.sp21ace.domain.indices.Flashcard
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.FlashcardOptions
import com.aquigs.sp21ace.domain.indices.FlashcardRun
import com.aquigs.sp21ace.domain.indices.IndexPresets

/** What the index flashcards' pages show: the flashcard run, the index presets, the flashcard options and every answer given. */
@Immutable
data class IndexFlashcardsState(
    val run: FlashcardRun,
    val presets: IndexPresets,
    val options: FlashcardOptions,
    val history: List<FlashcardAnswer>,
)

/** What the index flashcards' pages ask for, which the composition root carries out with the stores. */
interface IndexFlashcardsActions : DrillActions<Flashcard> {
    /** The presets as an edit on the Indices or Index Presets page leaves them. */
    fun changePresets(presets: IndexPresets)

    fun changeOptions(options: FlashcardOptions)
}
