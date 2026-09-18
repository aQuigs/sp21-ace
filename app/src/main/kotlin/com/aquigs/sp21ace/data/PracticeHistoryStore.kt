package com.aquigs.sp21ace.data

import android.content.Context
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Keeps every trainer answer through restarts, a line each, appended to [file]. A test passes its own file, so it never touches the app's history. */
class PracticeHistoryStore(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "practice_history.txt"))

    /** Waits for the answers still being written, so a recreated activity loads the one given just before. Lines it can't read are skipped. */
    fun load(): List<PracticeAnswer> = io.submit(
        Callable {
            val text = if (file.exists()) file.readText() else ""
            // A write cut short by the app being killed leaves no newline, and the next answer would run on into its line
            if (text.isNotEmpty() && !text.endsWith('\n')) file.appendText("\n")
            text.lineSequence().mapNotNull(PracticeLine::parse).toList()
        },
    ).get()

    /** Returns at once and writes on a background thread. */
    fun append(answer: PracticeAnswer) {
        // submit, not execute, so a failed write, such as on a full disk, loses that answer instead of crashing the app
        io.submit { file.appendText(PracticeLine.print(answer) + "\n") }
    }

    fun clear() {
        io.submit(Callable { file.delete() }).get()
    }

    private companion object {
        // One thread for every store, so answers land in the order given, and a load or clear waits for the writes queued before it
        val io: ExecutorService = Executors.newSingleThreadExecutor()
    }
}
