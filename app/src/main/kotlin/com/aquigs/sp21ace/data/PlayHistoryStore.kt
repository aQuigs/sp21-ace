package com.aquigs.sp21ace.data

import android.content.Context
import com.aquigs.sp21ace.domain.history.PlayedHand
import java.io.File

/** Every hand played at the table, kept through restarts. */
class PlayHistoryStore(file: File) : HistoryStore<PlayedHand>(file, IO, PlayLine::parse, PlayLine::print) {
    companion object {
        @Volatile
        private var app: PlayHistoryStore? = null

        /** The app's own history, one store a process, as the practice history is. */
        fun forApp(context: Context): PlayHistoryStore = app ?: synchronized(this) {
            app ?: PlayHistoryStore(File(context.applicationContext.filesDir, "play_history.txt")).also { app = it }
        }
    }
}
