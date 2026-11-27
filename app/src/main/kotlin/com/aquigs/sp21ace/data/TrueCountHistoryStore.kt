package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import java.io.File
import java.util.concurrent.Executor

/** Every true count answer, kept through restarts. */
class TrueCountHistoryStore internal constructor(file: File, io: Executor) :
    HistoryStore<TrueCountAnswer>(file, io, TrueCountLine::parse, TrueCountLine::print) {
    /** A store of its own on [file], so a test never touches the app's history. */
    constructor(file: File) : this(file, IO)

    companion object : AppStore<TrueCountHistoryStore>("true_count_history.txt", { TrueCountHistoryStore(it) })
}
