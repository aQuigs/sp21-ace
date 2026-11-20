package com.aquigs.sp21ace.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.HandKind
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.strategy.Upcard
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class FlashcardHistoryStoreTest {
    // Its own file, so the tests never touch the history the app itself saved
    private val file = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "flashcard_history_test.txt")

    private val sixteenVsTen = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 16), Upcard.TEN, DeviationPlay.STAND), 0)
    private val rightStand = FlashcardAnswer(Instant.ofEpochMilli(1_789_000_000_000), Game.BLACKJACK, sixteenVsTen, 0, 1_800.milliseconds, cards = 2)
    private val cutShort = FlashcardAnswer(Instant.ofEpochMilli(1_789_000_002_000), Game.BLACKJACK, sixteenVsTen, 2, 900.milliseconds, cards = 2)
    private val untimedInsurance = FlashcardAnswer(Instant.ofEpochMilli(1_789_000_005_000), Game.BLACKJACK, PlayIndex(Deviation.Insurance, 3), -1, null, cards = 0)

    // A new store loads after every write already queued, so it loads what the file will hold
    private fun loadAfresh(): List<FlashcardAnswer> = runBlocking { FlashcardHistoryStore(file).history.filterNotNull().first() }

    @Before
    fun setUp() = FlashcardHistoryStore(file).clear()

    @After
    fun tearDown() = FlashcardHistoryStore(file).clear()

    @Test
    fun aNewStoreOnTheSameFileLoadsEveryAnswerInTheOrderGiven() {
        assertEquals(emptyList<FlashcardAnswer>(), loadAfresh())

        val store = FlashcardHistoryStore(file)
        store.append(rightStand)
        store.append(untimedInsurance)

        assertEquals(listOf(rightStand, untimedInsurance), loadAfresh())
    }

    @Test
    fun anAnswerCutShortWhenTheAppWasKilledIsSkippedAndTheNextAnswerLoadsIntact() {
        FlashcardHistoryStore(file).append(rightStand)
        // Loading waits for the append to land, so the cut-short line goes after it
        loadAfresh()
        // Written as the store writes, a newline first, but killed partway
        file.appendText("\n" + FlashcardLine.print(cutShort).take(30))

        FlashcardHistoryStore(file).append(untimedInsurance)

        assertEquals(listOf(rightStand, untimedInsurance), loadAfresh())
    }
}
