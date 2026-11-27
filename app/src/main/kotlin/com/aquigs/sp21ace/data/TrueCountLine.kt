package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.truecount.Decks
import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import com.aquigs.sp21ace.domain.truecount.TrueCountCard
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * One true count history line: named fields, such as `at=1789000000000 rc=7 left=14 answer=2 ms=2350 shoe=6 rounding=FLOOR`, the
 * decks left in quarters. An answer with no time has no `ms`. The rounding comes last, so a line cut short anywhere loses or
 * garbles a field and reads as nothing.
 */
internal object TrueCountLine {
    private const val AT = "at"
    private const val RUNNING_COUNT = "rc"
    private const val LEFT = "left"
    private const val ANSWER = "answer"
    private const val MS = "ms"
    private const val SHOE = "shoe"
    private const val ROUNDING = "rounding"

    fun print(answer: TrueCountAnswer): String {
        val fields = listOfNotNull(
            AT to answer.answeredAt.toEpochMilli(),
            RUNNING_COUNT to answer.card.runningCount,
            LEFT to answer.card.decksLeft.quarters,
            ANSWER to answer.answer,
            answer.time?.let { MS to it.inWholeMilliseconds },
            SHOE to answer.card.shoe,
            ROUNDING to answer.card.rounding.name,
        )
        return LineFields.print(*fields.toTypedArray())
    }

    /** Null for a line it can't read, such as one cut short when the app was killed mid-write. */
    fun parse(line: String): TrueCountAnswer? = try {
        val fields = LineFields(line)

        TrueCountAnswer(
            answeredAt = Instant.ofEpochMilli(fields[AT].toLong()),
            card = TrueCountCard(
                shoe = fields[SHOE].toInt(),
                decksLeft = Decks(fields[LEFT].toInt()),
                rounding = CountRounding.valueOf(fields[ROUNDING]),
                runningCount = fields[RUNNING_COUNT].toInt(),
            ),
            answer = fields[ANSWER].toInt(),
            time = fields.ifSaved(MS)?.toLong()?.milliseconds,
        )
    } catch (e: RuntimeException) {
        null
    }
}
