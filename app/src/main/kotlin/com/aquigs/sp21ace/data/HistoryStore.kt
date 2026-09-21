package com.aquigs.sp21ace.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Keeps a history through restarts, a line each, [print]ed and appended to [file]. The file is read once, on [io], and the
 * history lives in memory from then on. A line [parse] can't read, such as one cut short when the app was killed, is skipped.
 */
open class HistoryStore<T> internal constructor(
    private val file: File,
    private val io: Executor,
    private val parse: (String) -> T?,
    private val print: (T) -> String,
) {
    private val lock = Any()
    private var records = emptyList<T>()
    private var loaded = false
    private val state = MutableStateFlow<List<T>?>(null)

    /** Null until the file has loaded, then every record in the order added. */
    val history: StateFlow<List<T>?> = state.asStateFlow()

    init {
        io.execute {
            val saved = try {
                if (file.exists()) file.useLines { lines -> lines.mapNotNull(parse).toList() } else emptyList()
            } catch (e: IOException) {
                // This session's records still count
                emptyList()
            }

            // Records added while the file loaded follow the saved ones, and a clear meanwhile has already dropped those
            update {
                if (!loaded) records = saved + records
                loaded = true
            }
        }
    }

    /** Adds [added] to the history at once and writes them on the background thread. */
    fun append(added: List<T>) {
        update { records = records + added }
        // A newline before each, so a line cut short when the app was killed stays on a line of its own and reads as nothing
        write { file.appendText(added.joinToString("") { "\n" + print(it) }) }
    }

    fun append(record: T) = append(listOf(record))

    /** Forgets every record. */
    fun clear() {
        update {
            records = emptyList()
            loaded = true
        }
        write { file.delete() }
    }

    private inline fun update(change: () -> Unit) = synchronized(lock) {
        change()
        state.value = records.takeIf { loaded }
    }

    private fun write(change: () -> Unit) = io.execute {
        try {
            change()
        } catch (e: IOException) {
            // A failed write, such as on a full disk, costs those lines rather than the app
        }
    }

    internal companion object {
        // One thread for every store, so lines land in the order given, and a new store loads after the writes queued before it
        val IO: Executor = Executors.newSingleThreadExecutor()
    }
}
