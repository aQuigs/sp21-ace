package com.aquigs.sp21ace.ui.components

import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

/** The app bar's colours: navy on every screen, unless answer feedback recolours its [container]. */
@Composable
fun appBarColors(container: Color = Sp21AceTheme.colors.appBar): TopAppBarColors {
    val content = Sp21AceTheme.colors.onAppBar

    return TopAppBarDefaults.topAppBarColors(
        containerColor = container,
        navigationIconContentColor = content,
        titleContentColor = content,
        actionIconContentColor = content,
    )
}
