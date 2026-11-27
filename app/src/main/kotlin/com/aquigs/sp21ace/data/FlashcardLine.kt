package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.Game
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * One flashcard history line: named fields, such as
 * `at=1789000000000 index=STAND:16:10:0:AT_OR_ABOVE answer=-1 ms=2350 cards=2 game=BLACKJACK`, the index as [IndexCode] prints it.
 * An answer of no index is `NONE`, and one with no time has no `ms`. The game comes last, so a line cut short anywhere loses or
 * garbles a field and reads as nothing.
 */
internal object FlashcardLine {
    private const val AT = "at"
    private const val INDEX = "index"
    private const val ANSWER = "answer"
    private const val MS = "ms"
    private const val CARDS = "cards"
    private const val GAME = "game"

    fun print(answer: FlashcardAnswer): String {
        val fields = listOfNotNull(
            AT to answer.answeredAt.toEpochMilli(),
            INDEX to IndexCode.print(answer.fact),
            ANSWER to IndexCode.printNumber(answer.answer),
            answer.time?.let { MS to it.inWholeMilliseconds },
            CARDS to answer.cards,
            GAME to answer.game.name,
        )
        return LineFields.print(*fields.toTypedArray())
    }

    /** Null for a line it can't read, such as one cut short when the app was killed mid-write. */
    fun parse(line: String): FlashcardAnswer? = try {
        val fields = LineFields(line)

        FlashcardAnswer(
            answeredAt = Instant.ofEpochMilli(fields[AT].toLong()),
            game = Game.valueOf(fields[GAME]),
            fact = IndexCode.parseFact(fields[INDEX]),
            answer = IndexCode.parseNumber(fields[ANSWER]),
            time = fields.ifSaved(MS)?.toLong()?.milliseconds,
            cards = fields[CARDS].toInt(),
        )
    } catch (e: RuntimeException) {
        null
    }
}
