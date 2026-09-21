package com.aquigs.sp21ace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Tally
import kotlin.math.roundToInt

/** How often answers were right, such as "66.6%", rounded down so 100.0% means none wrong. Null with nothing answered, which each screen shows its own way. */
@Composable
fun Tally.percentText(): String? = accuracyPermille?.let { stringResource(R.string.percentage, it / 10.0) }

/** [count]'s share of [total] in whole percent, as Blackjack Ace's Play Statistics give it, or "--" with nothing counted. */
@Composable
fun shareText(count: Int, total: Int): String =
    if (total == 0) stringResource(R.string.no_data) else stringResource(R.string.whole_percentage, (count * 100.0 / total).roundToInt())
