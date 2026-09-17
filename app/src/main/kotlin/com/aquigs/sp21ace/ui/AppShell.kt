package com.aquigs.sp21ace.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import com.aquigs.sp21ace.ui.trainer.StrategyTrainerScreen
import kotlinx.coroutines.launch

enum class Destination(@StringRes val title: Int, @DrawableRes val icon: Int) {
    StrategyTrainer(R.string.strategy_trainer, R.drawable.ic_home),
}

// Blackjack Ace's drawer leaves about a third of the screen uncovered; Material's 360dp default covers almost all of it.
private val DrawerWidth = 280.dp

@Composable
fun AppShell(modifier: Modifier = Modifier) {
    var destination by rememberSaveable { mutableStateOf(Destination.StrategyTrainer) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Root screens have nothing behind them, so Back toggles the drawer instead of leaving the app. targetValue, not
    // isOpen, so a Back pressed mid-animation reverses it.
    BackHandler {
        scope.launch {
            if (drawerState.targetValue == DrawerValue.Open) drawerState.close() else drawerState.open()
        }
    }

    // Edge to edge, the status bar icons sit over whatever reaches the top of the screen, and only dark icons read over
    // a light colour such as the light theme's drawer
    val underStatusBar = when (drawerState.targetValue) {
        DrawerValue.Open -> DrawerDefaults.modalContainerColor
        DrawerValue.Closed -> Sp21AceTheme.colors.appBar
    }
    val lightStatusBar = underStatusBar.luminance() > 0.5f
    val window = LocalActivity.current?.window
    SideEffect {
        window?.let { WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = lightStatusBar }
    }

    ModalNavigationDrawer(
        drawerContent = {
            Drawer(
                selected = destination,
                onSelect = {
                    destination = it
                    scope.launch { drawerState.close() }
                },
            )
        },
        modifier = modifier,
        drawerState = drawerState,
    ) {
        when (destination) {
            Destination.StrategyTrainer -> StrategyTrainerScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
        }
    }
}

@Composable
private fun Drawer(selected: Destination, onSelect: (Destination) -> Unit) {
    // The overload taking drawerState registers its own back handler, which would compete with AppShell's.
    ModalDrawerSheet(modifier = Modifier.width(DrawerWidth)) {
        // The extra 16dp is the item's own start padding, so the header lines up with the icons
        Text(
            text = stringResource(R.string.basic_strategy),
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .padding(start = 16.dp, top = 18.dp, bottom = 18.dp)
                .semantics { heading() },
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleSmall,
        )

        Destination.entries.forEach { destination ->
            NavigationDrawerItem(
                label = { Text(stringResource(destination.title)) },
                selected = destination == selected,
                onClick = { onSelect(destination) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
            )
        }
    }
}
