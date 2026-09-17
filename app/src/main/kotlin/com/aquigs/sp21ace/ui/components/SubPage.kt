package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

/** A full-screen page opened over a root screen, with a back arrow and its title in the app bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubPage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable (PaddingValues) -> Unit) {
    val colors = Sp21AceTheme.colors

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = title, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.appBar,
                    navigationIconContentColor = colors.onAppBar,
                    titleContentColor = colors.onAppBar,
                ),
            )
        },
        content = content,
    )
}
