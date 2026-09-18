package com.aquigs.sp21ace.data

import android.content.Context
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Keeps every trainer answer through restarts, a line each, appended to [file]. The file is read once, on [io], and the
 * history lives in memory from then on.
 */
class PracticeHistoryStore internal constructor(private val file: File, private val io: Executor) {
    /** A store of its own on [file], so a test never touches the app's history. */
    constructor(file: File) : this(file, IO)

    private val lock = Any()
    private var answers = emptyList<PracticeAnswer>()
    private var loaded = false
    private val state = MutableStateFlow<List<PracticeAnswer>?>(null)

    /** Null until the file has loaded, then every answer in the order given. */
    val history: StateFlow<List<PracticeAnswer>?> = state.asStateFlow()

    init {
        io.execute {
            val saved = try {
                if (file.exists()) file.useLines { lines -> lines.mapNotNull(PracticeLine::parse).toList() } else emptyList()
            } catch (e: IOException) {
                // This session's answers still count
                emptyList()
            }

            // Answers given while the file loaded follow the saved ones, and a clear meanwhile has already dropped those
            update {
                if (!loaded) answers = saved + answers
                loaded = true
            }
        }
    }

    /** Adds [answer] to the history at once and writes it on the background thread. */
    fun append(answer: PracticeAnswer) {
        update { answers = answers + answer }
        // A newline first, so a line cut short when the app was killed stays on a line of its own and reads as nothing
        write { file.appendText("\n" + PracticeLine.print(answer)) }
    }

    /** Forgets every answer. */
    fun clear() {
        update {
            answers = emptyList()
            loaded = true
        }
        write { file.delete() }
    }

    private inline fun update(change: () -> Unit) = synchronized(lock) {
        change()
        state.value = answers.takeIf { loaded }
    }

    private fun write(change: () -> Unit) = io.execute {
        try {
            change()
        } catch (e: IOException) {
            // A failed write, such as on a full disk, costs that answer's line rather than the app
        }
    }

    companion object {
        // One thread for every store, so lines land in the order given, and a new store loads after the writes queued before it
        private val IO: Executor = Executors.newSingleThreadExecutor()

        @Volatile
        private var app: PracticeHistoryStore? = null

        /** The app's own history, one store a process, so a recreated activity finds it loaded instead of reading the file again. */
        fun forApp(context: Context): PracticeHistoryStore = app ?: synchronized(this) {
            app ?: PracticeHistoryStore(File(context.applicationContext.filesDir, "practice_history.txt")).also { app = it }
        }
    }
}
