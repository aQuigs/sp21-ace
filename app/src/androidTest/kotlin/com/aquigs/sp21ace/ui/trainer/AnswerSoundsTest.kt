package com.aquigs.sp21ace.ui.trainer

import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnswerSoundsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun eachAnswerSoundIsAShortClipTheDeviceCanDecode() {
        for (sound in listOf(R.raw.right_answer, R.raw.wrong_answer)) {
            val name = context.resources.getResourceEntryName(sound)
            val player = checkNotNull(MediaPlayer.create(context, sound)) { "$name doesn't decode" }

            try {
                assertTrue("$name lasts ${player.duration} ms", player.duration in 200..800)
            } finally {
                player.release()
            }
        }
    }
}
