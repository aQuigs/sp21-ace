package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.FakeIndices
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class FlashcardLineTest {
    private val wrongStand = FlashcardAnswer(Instant.ofEpochMilli(1_789_000_000_000), Game.BLACKJACK, FakeIndices.STAND, -1, 2350.milliseconds, cards = 2)

    @Test
    fun printsEveryFieldByName() {
        // Saved lines must keep loading, so a change here has to read the old lines too
        assertEquals("at=1789000000000 index=STAND:18:2:99:AT_OR_ABOVE answer=-1 ms=2350 cards=2 game=BLACKJACK", FlashcardLine.print(wrongStand))
    }

    @Test
    fun readsBackEveryAnswerItPrints() {
        val answers = listOf(
            wrongStand,
            // Untimed, as after an interruption
            FlashcardAnswer(Instant.ofEpochMilli(1_789_000_005_000), Game.BLACKJACK, FakeIndices.INSURANCE, -98, null, cards = 0),
            FlashcardAnswer(Instant.ofEpochMilli(1_789_000_010_000), Game.BLACKJACK, FakeIndices.HIT, 3, 0.milliseconds, cards = 4),
            FlashcardAnswer(Instant.ofEpochMilli(1_789_000_015_000), Game.BLACKJACK, FakeIndices.SPLIT, -99, 61_000.milliseconds, cards = 2),
        )

        assertEquals(answers, answers.map { FlashcardLine.parse(FlashcardLine.print(it)) })
        assertEquals("at=1789000005000 index=INSURANCE:-98:AT_OR_ABOVE answer=-98 cards=0 game=BLACKJACK", FlashcardLine.print(answers[1]))
    }

    @Test
    fun readsBackAHandWithNoIndexAndAnAnswerOfNoIndex() {
        val noIndex = FlashcardAnswer(Instant.EPOCH, Game.BLACKJACK, FakeIndices.noIndex("STAND", "16", "8"), null, null, cards = 2)
        val answers = listOf(noIndex, noIndex.copy(answer = 3), wrongStand.copy(answer = null))

        assertEquals(answers, answers.map { FlashcardLine.parse(FlashcardLine.print(it)) })
        assertEquals("at=0 index=STAND:16:8:NONE:AT_OR_ABOVE answer=NONE cards=2 game=BLACKJACK", FlashcardLine.print(noIndex))
        assertNull(FlashcardLine.parse("at=0 index=INSURANCE:NONE:AT_OR_ABOVE answer=NONE cards=0 game=BLACKJACK"))
    }

    @Test
    fun aFieldAddedLaterDoesntStopALineLoading() {
        assertEquals(wrongStand, FlashcardLine.parse(FlashcardLine.print(wrongStand).replace(" answer=", " preset=x answer=")))
    }

    @Test
    fun aLineCutShortGarbledOrWithAFieldMissingOrTwiceReadsAsNothing() {
        val line = FlashcardLine.print(wrongStand)

        for (cutShort in (0 until line.length).map(line::take)) {
            assertNull(cutShort, FlashcardLine.parse(cutShort))
        }

        val garbled = listOf(
            "hello",
            // An unknown game, play or index, a number that isn't one, a negative time or card count
            line.replace("BLACKJACK", "POKER"),
            line.replace("STAND:", "FOLD:"),
            line.replace("STAND:18:2", "STAND:4:2"),
            line.replace("answer=-1", "answer=one"),
            line.replace("ms=2350", "ms=-5"),
            // So long it reads as an infinite time, which no percentile can work with
            line.replace("ms=2350", "ms=${Long.MAX_VALUE}"),
            line.replace("cards=2", "cards=-1"),
            // A field missing, a field twice, and a cut-short line run on into the next
            line.replace("index=STAND:18:2:99:AT_OR_ABOVE ", ""),
            "$line answer=3",
            line.replace("ms=2350", "ms=2350 ms=10"),
            line.take(30) + line,
        )
        for (each in garbled) {
            assertNull(each, FlashcardLine.parse(each))
        }
    }
}
