package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.truecount.Decks
import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import com.aquigs.sp21ace.domain.truecount.TrueCountCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class TrueCountLineTest {
    private val timed = TrueCountAnswer(
        Instant.ofEpochMilli(1_789_000_000_000),
        TrueCountCard(6, Decks(14), CountRounding.FLOOR, 7),
        2,
        2350.milliseconds,
    )

    @Test
    fun printsEveryFieldByName() {
        // Saved lines must keep loading, so a change here has to read the old lines too
        assertEquals("at=1789000000000 rc=7 left=14 answer=2 ms=2350 shoe=6 rounding=FLOOR", TrueCountLine.print(timed))
    }

    @Test
    fun readsBackEveryAnswerItPrints() {
        val answers = CountRounding.entries.map { rounding ->
            timed.copy(card = TrueCountCard(8, Decks(3), rounding, -5), answer = -6, time = null)
        } + timed

        assertEquals(answers, answers.map { TrueCountLine.parse(TrueCountLine.print(it)) })
    }

    @Test
    fun aLineCutShortGarbledOrWithAFieldMissingOrTwiceReadsAsNothing() {
        val line = TrueCountLine.print(timed)

        for (cutShort in (0 until line.length).map(line::take)) {
            assertNull(cutShort, TrueCountLine.parse(cutShort))
        }

        val garbled = listOf(
            "hello",
            line.replace("FLOOR", "CEILING"),
            line.replace("shoe=6", "shoe=5"),
            line.replace("left=14", "left=0"),
            line.replace("left=14", "left=25"),
            line.replace("rc=7", "rc=seven"),
            line.replace("ms=2350", "ms=-5"),
            line.replace("ms=2350", "ms=${Long.MAX_VALUE}"),
            line.replace("answer=2 ", ""),
            "$line answer=3",
            line.take(30) + line,
        )
        for (each in garbled) {
            assertNull(each, TrueCountLine.parse(each))
        }
    }
}
