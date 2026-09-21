package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.history.PracticeAnswer
import java.io.File
import java.util.concurrent.Executor

/** Every trainer answer, kept through restarts. */
class PracticeHistoryStore internal constructor(file: File, io: Executor) :
    HistoryStore<PracticeAnswer>(file, io, PracticeLine::parse, PracticeLine::print) {
    /** A store of its own on [file], so a test never touches the app's history. */
    constructor(file: File) : this(file, IO)

    companion object : AppStore<PracticeHistoryStore>("practice_history.txt", { PracticeHistoryStore(it) })
}
