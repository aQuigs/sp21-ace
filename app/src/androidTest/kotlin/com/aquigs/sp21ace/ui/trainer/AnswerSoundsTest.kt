package com.aquigs.sp21ace.ui.trainer

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AnswerSoundsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sounds = listOf(R.raw.right_answer, R.raw.wrong_answer)

    @Test
    fun eachAnswerSoundDecodesIntoASoundPool() {
        val pool = SoundPool.Builder().build()
        val statuses = ConcurrentHashMap<Int, Int>()
        val loaded = CountDownLatch(sounds.size)
        pool.setOnLoadCompleteListener { _, sample, status ->
            statuses[sample] = status
            loaded.countDown()
        }

        try {
            // A SoundPool decodes the whole clip as it loads, and reports 0 once it has
            val samples = sounds.map { pool.load(context, it, 1) }

            assertTrue("still loading", loaded.await(5, TimeUnit.SECONDS))
            assertEquals(samples.associateWith { 0 }, statuses.toMap())
        } finally {
            pool.release()
        }
    }

    @Test
    fun eachAnswerSoundIsAShortClip() {
        for (sound in sounds) {
            val name = context.resources.getResourceEntryName(sound)
            val player = checkNotNull(MediaPlayer.create(context, sound)) { "$name doesn't open" }

            try {
                assertTrue("$name lasts ${player.duration} ms", player.duration in 200..800)
            } finally {
                player.release()
            }
        }
    }
}
