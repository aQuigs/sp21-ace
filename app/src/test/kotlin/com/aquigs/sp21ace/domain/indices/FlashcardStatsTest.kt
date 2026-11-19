package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FlashcardStatsTest {
    private val now = Instant.parse("2026-11-19T12:00:00Z")

    private val stand = FakeIndices.STAND
    private val split = FakeIndices.SPLIT
    private val double = FakeIndices.DOUBLE
    private val insurance = FakeIndices.INSURANCE

    private fun preset(vararg entries: IndexEntry) = IndexPreset("p", "P", Game.BLACKJACK, basedOn = null, entries = entries.toList())

    private val preset = preset(IndexEntry(stand), IndexEntry(split), IndexEntry(double))

    private fun answer(index: PlayIndex, right: Boolean = true, time: Duration? = 1.seconds, at: Instant = now) =
        FlashcardAnswer(at, Game.BLACKJACK, index, if (right) index.threshold else index.threshold + 1, time, cards = 2)

    private fun ms(vararg times: Long) = times.map { it.milliseconds }

    private fun List<FlashcardAnswer>.stats(period: Period = Period.ALL_TIME, of: IndexPreset = preset) = flashcardStats(of, period, now)

    private fun FlashcardStats.row(index: PlayIndex) = indices.single { it.index == index }

    @Test
    fun noTimesHaveNoPercentile() {
        assertNull(percentile(emptyList(), 0.5))
        assertNull(TimeSpread.of(emptyList()))
    }

    @Test
    fun oneTimeIsEveryPercentile() {
        val time = 1700.milliseconds
        assertEquals(TimeSpread(time, time, time, time, time), TimeSpread.of(listOf(time)))
    }

    @Test
    fun twoTimesGiveTheirMeanAsTheMedianAndEveryOtherPercentileInProportionBetweenThem() {
        assertEquals(TimeSpread(1100.milliseconds, 1250.milliseconds, 1500.milliseconds, 1750.milliseconds, 1900.milliseconds), TimeSpread.of(ms(2000, 1000)))
    }

    @Test
    fun manyTimesInterpolateBetweenTheTwoEitherSideOfTheRankAndNeverLeaveTheirRange() {
        // Ranks 0.9, 2.25, 4.5, 6.75 and 8.1 of the ten sorted times
        val times = ms(900, 100, 800, 200, 700, 300, 600, 400, 500, 1000)

        assertEquals(TimeSpread(190.milliseconds, 325.milliseconds, 550.milliseconds, 775.milliseconds, 910.milliseconds), TimeSpread.of(times))
        assertEquals(100.milliseconds, percentile(times.sorted(), 0.0))
        assertEquals(1000.milliseconds, percentile(times.sorted(), 1.0))
    }

    @Test
    fun aBucketHoldsItsLowerEdgeButNotItsUpperAndFiveSecondsOrMoreGoToTheLast() {
        assertEquals(0, TimeHistogram.bucketOf(Duration.ZERO))
        assertEquals(0, TimeHistogram.bucketOf(499.milliseconds))
        assertEquals(1, TimeHistogram.bucketOf(500.milliseconds))
        assertEquals(9, TimeHistogram.bucketOf(4999.milliseconds))
        assertEquals(10, TimeHistogram.bucketOf(5.seconds))
        assertEquals(10, TimeHistogram.bucketOf(10.minutes()))
        assertEquals(5.seconds, TimeHistogram.start(TimeHistogram.BUCKETS))

        val histogram = TimeHistogram.of(ms(0, 499, 500, 2250, 4999, 5000, 60_000))
        assertEquals(listOf(2, 1, 0, 0, 1, 0, 0, 0, 0, 1, 2), histogram.counts)
        assertEquals(7, histogram.total)
    }

    private fun Int.minutes() = (this * 60).seconds

    @Test
    fun anUntimedAnswerCountsForAccuracyButNeverForSpeed() {
        val stats = listOf(answer(stand, time = 2.seconds), answer(stand, right = false, time = null)).stats()

        assertEquals(Tally(correct = 1, incorrect = 1), stats.overall)
        assertEquals(1, stats.times.timed)
        assertEquals(2.seconds, stats.times.spread?.median)
        assertEquals(2, stats.row(stand).answers)
        assertEquals(1, stats.row(stand).times.timed)
    }

    @Test
    fun answersButNoneTimedHaveATallyAndNoSpread() {
        val stats = listOf(answer(stand, time = null)).stats()

        assertEquals(Tally(correct = 1, incorrect = 0), stats.overall)
        assertNull(stats.times.spread)
        assertEquals(0, stats.times.timed)
        assertNull(stats.row(stand).times.spread)
    }

    @Test
    fun aPeriodCountsOnlyTheAnswersNewerThanWhereItReachesBack() {
        val start = requireNotNull(Period.WEEK.start(now))
        val history = listOf(answer(stand, right = false, at = start), answer(stand, at = start.plusMillis(1)), answer(split, at = now.plusMillis(1)))

        assertEquals(Tally(correct = 1, incorrect = 0), history.stats(Period.WEEK).overall)
        assertEquals(Tally(correct = 2, incorrect = 1), history.stats(Period.ALL_TIME).overall)
    }

    @Test
    fun anEditedNumberStartsAFreshRecordAndAnswersToIndicesOutsideThePresetDontCount() {
        val edited = stand.copy(threshold = stand.threshold - 1)
        val otherDirection = stand.copy(direction = IndexDirection.AT_OR_BELOW)
        val history = listOf(answer(stand, right = false), answer(edited), answer(edited), answer(otherDirection), answer(insurance))

        val before = history.stats()
        assertEquals(Tally(correct = 0, incorrect = 1), before.overall)
        assertEquals(Tally(correct = 0, incorrect = 1), before.row(stand).tally)

        val after = history.stats(of = preset.edit(edited))
        assertEquals(Tally(correct = 2, incorrect = 0), after.overall)
        assertEquals(Tally(correct = 2, incorrect = 0), after.row(edited).tally)
    }

    @Test
    fun everyIndexSwitchedOnHasARowAndASwitchedOffOneOnlyWithAnswersInThePeriod() {
        val presetWithOff = preset(IndexEntry(stand), IndexEntry(split, enabled = false), IndexEntry(double, enabled = false), IndexEntry(insurance))
        val stats = listOf(answer(double), answer(split, at = now.minusSeconds(3 * 24 * 3600))).stats(Period.TODAY, presetWithOff)

        assertEquals(listOf(stand, double, insurance), stats.indices.map { it.index })
        assertEquals(listOf(true, false, true), stats.indices.map { it.enabled })
        assertEquals(listOf(0, 1, 0), stats.indices.map { it.answers })
    }

    @Test
    fun theAxisEndsAtTheLongestNinetiethPercentileUpToAWholeSecondFrom2To10Seconds() {
        fun axis(time: Duration) = listOf(time).map { answer(stand, time = it) }.stats().axisEnd

        assertEquals(2.seconds, emptyList<FlashcardAnswer>().stats().axisEnd)
        assertEquals(2.seconds, axis(800.milliseconds))
        assertEquals(3.seconds, axis(2001.milliseconds))
        assertEquals(4.seconds, axis(4.seconds))
        assertEquals(10.seconds, axis(30.seconds))
    }

    private fun stats(index: PlayIndex, correct: Int, incorrect: Int, median: Duration? = null, p90: Duration? = median) = IndexStats(
        index,
        enabled = true,
        Tally(correct, incorrect),
        AnswerTimes(median?.let { TimeSpread(it, it, it, it, p90!!) }, TimeHistogram(List(TimeHistogram.BUCKETS + 1) { 0 })),
    )

    @Test
    fun leastAccurateComesFirstThenTheMoreAnsweredAtTheSameAccuracyThenThePresetsOrderWithUnansweredLast() {
        val rows = listOf(
            stats(FakeIndices.STAND, correct = 0, incorrect = 0),
            stats(FakeIndices.SPLIT, correct = 9, incorrect = 1),
            stats(FakeIndices.DOUBLE, correct = 1, incorrect = 1),
            stats(FakeIndices.SURRENDER, correct = 2, incorrect = 2),
            stats(FakeIndices.HIT, correct = 1, incorrect = 1),
            stats(FakeIndices.INSURANCE, correct = 0, incorrect = 0),
        )

        assertEquals(
            listOf(FakeIndices.SURRENDER, FakeIndices.DOUBLE, FakeIndices.HIT, FakeIndices.SPLIT, FakeIndices.STAND, FakeIndices.INSURANCE),
            rows.sortedBy(IndexOrder.LEAST_ACCURATE).map { it.index },
        )
    }

    @Test
    fun slowestComesFirstByMedianThenNinetiethPercentileThenThePresetsOrderWithUntimedThenUnansweredLast() {
        val rows = listOf(
            stats(FakeIndices.STAND, correct = 0, incorrect = 0),
            stats(FakeIndices.SPLIT, correct = 1, incorrect = 0),
            stats(FakeIndices.DOUBLE, correct = 1, incorrect = 0, median = 2.seconds, p90 = 3.seconds),
            stats(FakeIndices.SURRENDER, correct = 1, incorrect = 0, median = 2.seconds, p90 = 4.seconds),
            stats(FakeIndices.HIT, correct = 1, incorrect = 0, median = 2.seconds, p90 = 3.seconds),
            stats(FakeIndices.INSURANCE, correct = 1, incorrect = 0, median = 1.seconds),
        )

        assertEquals(
            listOf(FakeIndices.SURRENDER, FakeIndices.DOUBLE, FakeIndices.HIT, FakeIndices.INSURANCE, FakeIndices.SPLIT, FakeIndices.STAND),
            rows.sortedBy(IndexOrder.SLOWEST).map { it.index },
        )
    }
}
