package com.aquigs.sp21ace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Tally

/** How often answers were right, such as "66.6%", rounded down so 100.0% means none wrong. Null with nothing answered, which each screen shows its own way. */
@Composable
fun Tally.percentText(): String? = accuracyPermille?.let { stringResource(R.string.percentage, it / 10.0) }
