package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

class PracticeHistoryStoreLoadTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { folder.newFile("practice_history.txt") }

    // The store's background work runs only when a test says, so a test can answer while the file is still loading
    private val queued = ArrayDeque<Runnable>()

    private fun runQueued() {
        while (queued.isNotEmpty()) queued.removeFirst().run()
    }

    private fun store() = PracticeHistoryStore(file, queued::addLast)

    private val saved = PracticeAnswer(Instant.ofEpochMilli(1_789_000_000_000), RuleSet.S17, TrainerHand(cards("9c 7d"), card("As")), Move.HIT, Move.HIT)
    private val givenWhileLoading = PracticeAnswer(Instant.ofEpochMilli(1_789_000_005_000), RuleSet.H17_REDOUBLE, TrainerHand(cards("8h 8s"), card("6d")), Move.STAND, Move.SPLIT)

    @Test
    fun anAnswerGivenWhileTheFileLoadsFollowsTheSavedOnesInMemoryAndInTheFile() {
        file.writeText("\n" + PracticeLine.print(saved))
        val store = store()

        store.append(givenWhileLoading)

        assertNull(store.history.value)

        runQueued()

        assertEquals(listOf(saved, givenWhileLoading), store.history.value)
        assertEquals(listOf(saved, givenWhileLoading), store().also { runQueued() }.history.value)
    }

    @Test
    fun loadingSkipsALineCutShortAndNeverWritesTheFile() {
        // Cut short when the app was killed, so no newline follows it
        val text = "\n" + PracticeLine.print(saved) + "\n" + PracticeLine.print(givenWhileLoading).take(30)
        file.writeText(text)

        val store = store().also { runQueued() }

        assertEquals(listOf(saved), store.history.value)
        assertEquals(text, file.readText())
    }
}
