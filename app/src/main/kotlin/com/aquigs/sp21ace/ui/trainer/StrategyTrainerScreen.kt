package com.aquigs.sp21ace.ui.trainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyTrainerScreen(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sp21AceTheme.colors

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.strategy_trainer), modifier = Modifier.semantics { heading() })
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.open_menu))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.appBar,
                    titleContentColor = colors.onAppBar,
                    actionIconContentColor = colors.onAppBar,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.app_name),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
