package com.aquigs.sp21ace.ui.hands

import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import com.aquigs.sp21ace.domain.dealing.HAND_TYPES
import com.aquigs.sp21ace.domain.dealing.HandType
import com.aquigs.sp21ace.ui.components.displayName

/** [type]'s switch while every group is open. Moves repeat from group to group, so it's the one in [type]'s place among its move's switches. */
fun SemanticsNodeInteractionsProvider.handTypeSwitch(context: Context, type: HandType): SemanticsNodeInteraction =
    onAllNodes(hasText(context.getString(type.move.displayName)) and isToggleable())[HAND_TYPES.filter { it.move == type.move }.indexOf(type)]
