package com.aquigs.sp21ace.domain.drill

import java.io.Serializable

/** Right answers in a row, as every drill counts them. Serializable so it saves with the drill's state. */
data class Streak(val count: Int = 0) : Serializable {
    /** The index in [RUNGS] of the highest rung reached, which stays the top rung past 256. */
    val rung: Int get() = RUNGS.indexOfLast { it <= count }

    /** A right answer adds one, and a wrong one drops it to zero. */
    fun after(isCorrect: Boolean) = if (isCorrect) Streak(count + 1) else Streak()

    companion object {
        /** The streak meter's doubling scale. */
        val RUNGS = listOf(0, 1, 2, 4, 8, 16, 32, 64, 128, 256)
    }
}
