package com.aquigs.sp21ace.ui.components

import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Move

internal val Move.displayName: Int
    get() = when (this) {
        Move.HIT -> R.string.move_hit
        Move.STAND -> R.string.move_stand
        Move.DOUBLE -> R.string.move_double
        Move.SPLIT -> R.string.move_split
        Move.SURRENDER -> R.string.move_surrender
    }
