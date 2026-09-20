package com.aquigs.sp21ace.ui.trainer

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aquigs.sp21ace.R

/** The sound an answer makes when sound effects are on, as Blackjack Ace plays one for each answer. */
fun interface AnswerSounds {
    fun play(correct: Boolean)
}

/** Both sounds while [enabled], so they load as sound effects are switched on, ready by the next answer, and cost nothing muted. */
@Composable
fun rememberAnswerSounds(enabled: Boolean): AnswerSounds? {
    val context = LocalContext.current.applicationContext

    return remember(context, enabled) { if (enabled) SoundPoolAnswerSounds(context) else null }
}

// A SoundPool keeps both sounds decoded, so each starts at the tap rather than after a player spins up. Game audio follows the
// media volume, as Blackjack Ace's sounds do. One stream, so a quick next answer cuts the last sound off rather than piling on it.
private class SoundPoolAnswerSounds(context: Context) : AnswerSounds, RememberObserver {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
        .build()
    private val right = pool.load(context, R.raw.right_answer, 1)
    private val wrong = pool.load(context, R.raw.wrong_answer, 1)

    override fun play(correct: Boolean) {
        pool.play(if (correct) right else wrong, 1f, 1f, 1, 0, 1f)
    }

    override fun onRemembered() {}

    override fun onForgotten() = pool.release()

    override fun onAbandoned() = pool.release()
}
