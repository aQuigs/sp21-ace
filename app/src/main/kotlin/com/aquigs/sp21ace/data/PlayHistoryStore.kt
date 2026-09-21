package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.history.PlayedHand
import java.io.File

/** Every hand played at the table, kept through restarts. */
class PlayHistoryStore(file: File) : HistoryStore<PlayedHand>(file, IO, PlayLine::parse, PlayLine::print) {
    companion object : AppStore<PlayHistoryStore>("play_history.txt", ::PlayHistoryStore)
}
