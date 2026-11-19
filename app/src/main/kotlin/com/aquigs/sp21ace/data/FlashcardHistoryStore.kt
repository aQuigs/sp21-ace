package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import java.io.File
import java.util.concurrent.Executor

/** Every flashcard answer, kept through restarts. */
class FlashcardHistoryStore internal constructor(file: File, io: Executor) :
    HistoryStore<FlashcardAnswer>(file, io, FlashcardLine::parse, FlashcardLine::print) {
    /** A store of its own on [file], so a test never touches the app's history. */
    constructor(file: File) : this(file, IO)

    companion object : AppStore<FlashcardHistoryStore>("flashcard_history.txt", { FlashcardHistoryStore(it) })
}
