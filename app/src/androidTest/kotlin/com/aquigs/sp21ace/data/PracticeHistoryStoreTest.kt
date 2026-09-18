package com.aquigs.sp21ace.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
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

@RunWith(AndroidJUnit4::class)
class PracticeHistoryStoreTest {
    // Its own file, so the tests never touch the history the app itself saved
    private val file = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "practice_history_test.txt")

    private val rightHit = PracticeAnswer(Instant.ofEpochMilli(1_789_000_000_000), RuleSet.S17, TrainerHand(cards("9c 7d"), card("As")), Move.HIT, Move.HIT)
    private val cutShort = PracticeAnswer(Instant.ofEpochMilli(1_789_000_002_000), RuleSet.H17, TrainerHand(cards("Kc 6h"), card("As")), Move.HIT, Move.SURRENDER)
    private val wrongStand = PracticeAnswer(Instant.ofEpochMilli(1_789_000_005_000), RuleSet.H17_REDOUBLE, TrainerHand(cards("8h 8s"), card("6d")), Move.STAND, Move.SPLIT)

    // A new store loads after every write already queued, so it loads what the file will hold
    private fun loadAfresh(): List<PracticeAnswer> = runBlocking { PracticeHistoryStore(file).history.filterNotNull().first() }

    @Before
    fun setUp() = PracticeHistoryStore(file).clear()

    @After
    fun tearDown() = PracticeHistoryStore(file).clear()

    @Test
    fun loadsNothingBeforeTheFirstAnswer() {
        assertEquals(emptyList<PracticeAnswer>(), loadAfresh())
    }

    @Test
    fun aNewStoreOnTheSameFileLoadsEveryAnswerInTheOrderGiven() {
        val store = PracticeHistoryStore(file)
        store.append(rightHit)
        store.append(wrongStand)

        assertEquals(listOf(rightHit, wrongStand), loadAfresh())
    }

    @Test
    fun anAnswerCutShortWhenTheAppWasKilledIsSkippedAndTheNextAnswerLoadsIntact() {
        PracticeHistoryStore(file).append(rightHit)
        loadAfresh()
        // Written as the store writes, a newline first, but killed partway
        file.appendText("\n" + PracticeLine.print(cutShort).take(30))

        PracticeHistoryStore(file).append(wrongStand)

        assertEquals(listOf(rightHit, wrongStand), loadAfresh())
    }
}
