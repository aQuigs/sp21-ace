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
    private val store = PracticeHistoryStore(file)

    private val rightHit = PracticeAnswer(Instant.ofEpochMilli(1_789_000_000_000), RuleSet.S17, TrainerHand(cards("9c 7d"), card("As")), Move.HIT, Move.HIT)
    private val wrongStand = PracticeAnswer(Instant.ofEpochMilli(1_789_000_005_000), RuleSet.H17_REDOUBLE, TrainerHand(cards("8h 8s"), card("6d")), Move.STAND, Move.SPLIT)

    @Before
    fun setUp() = store.clear()

    @After
    fun tearDown() = store.clear()

    @Test
    fun loadsNothingBeforeTheFirstAnswer() {
        assertEquals(emptyList<PracticeAnswer>(), store.load())
    }

    @Test
    fun aNewStoreOnTheSameFileLoadsEveryAnswerInTheOrderGiven() {
        store.append(rightHit)
        store.append(wrongStand)

        assertEquals(listOf(rightHit, wrongStand), PracticeHistoryStore(file).load())
    }

    @Test
    fun anAnswerCutShortWhenTheAppWasKilledIsSkippedAndTheNextAnswerStillLoads() {
        store.append(rightHit)
        // Waits for that write, so the cut-short line lands after it
        store.load()
        file.appendText(PracticeLine.print(wrongStand).take(30))

        assertEquals(listOf(rightHit), PracticeHistoryStore(file).load())

        store.append(wrongStand)

        assertEquals(listOf(rightHit, wrongStand), PracticeHistoryStore(file).load())
    }
}
