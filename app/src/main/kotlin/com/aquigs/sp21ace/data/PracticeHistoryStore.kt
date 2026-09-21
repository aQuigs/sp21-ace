package com.aquigs.sp21ace.data

import android.content.Context
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import java.io.File
import java.util.concurrent.Executor

/** Every trainer answer, kept through restarts. */
class PracticeHistoryStore internal constructor(file: File, io: Executor) :
    HistoryStore<PracticeAnswer>(file, io, PracticeLine::parse, PracticeLine::print) {
    /** A store of its own on [file], so a test never touches the app's history. */
    constructor(file: File) : this(file, IO)

    companion object {
        @Volatile
        private var app: PracticeHistoryStore? = null

        /** The app's own history, one store a process, so a recreated activity finds it loaded instead of reading the file again. */
        fun forApp(context: Context): PracticeHistoryStore = app ?: synchronized(this) {
            app ?: PracticeHistoryStore(File(context.applicationContext.filesDir, "practice_history.txt")).also { app = it }
        }
    }
}
