package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.indices.CountRounding
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class TrueCountStatsTest {
    private val now = Instant.ofEpochMilli(1_789_000_000_000)

    private fun answer(shoe: Int, quarters: Int, runningCount: Int, answer: Int, millis: Int? = null, at: Instant = now, rounding: CountRounding = CountRounding.FLOOR) =
        TrueCountAnswer(at, TrueCountCard(shoe, Decks(quarters), rounding, runningCount), answer, millis?.milliseconds)

    @Test
    fun eachShoeAnsweredGetsASectionTheSmallestFirst() {
        val stats = listOf(answer(8, 16, 4, 1), answer(2, 4, 4, 4), answer(8, 16, 4, 2)).trueCountStats(Period.ALL_TIME, now)

        assertEquals(listOf(2, 8), stats.map { it.shoe })
        assertEquals(listOf(Tally(1, 0), Tally(1, 1)), stats.map { it.tally })
    }

    @Test
    fun squaresAreTheDecksLeftByTheRightAnswerWithThePadsEndsPooled() {
        val stats = listOf(
            // +2 answered right and wrong, +7 and +8 as "+6 or higher", and −1.5 under two roundings
            answer(6, 14, 7, 2),
            answer(6, 14, 8, 1),
            answer(6, 4, 7, 6),
            answer(6, 4, 8, 5),
            answer(6, 16, -6, -2),
            answer(6, 16, -6, -1, rounding = CountRounding.TRUNCATE),
        ).trueCountStats(Period.ALL_TIME, now).single()

        val expected = mapOf(
            TrueCountSquare(Decks(14), 2) to Tally(1, 1),
            TrueCountSquare(Decks(4), 6) to Tally(1, 1),
            TrueCountSquare(Decks(16), -2) to Tally(1, 0),
            TrueCountSquare(Decks(16), -1) to Tally(1, 0),
        )
        assertEquals(expected, stats.bySquare)
        assertEquals(listOf("4", "3.5", "1"), stats.decksLeft.map { it.label })
    }

    @Test
    fun onlyAnswersInThePeriodCountAndOnlyTimedOnesForSpeed() {
        val history = listOf(
            answer(6, 14, 7, 2, millis = 1_000),
            answer(6, 14, 7, 2),
            answer(6, 14, 7, 2, millis = 9_000, at = now - Duration.ofDays(2)),
        )

        val today = history.trueCountStats(Period.TODAY, now).single()
        assertEquals(Tally(2, 0), today.tally)
        assertEquals(1 to 1_000.milliseconds, today.times.timed to today.times.spread?.median)
        assertEquals(3, history.trueCountStats(Period.WEEK, now).single().tally.total)
        assertEquals(emptyList<ShoeStats>(), emptyList<TrueCountAnswer>().trueCountStats(Period.TODAY, now))
    }
}
