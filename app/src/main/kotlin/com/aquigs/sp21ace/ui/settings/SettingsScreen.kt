package com.aquigs.sp21ace.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.ui.components.ActionRow
import com.aquigs.sp21ace.ui.components.ChoicePage
import com.aquigs.sp21ace.ui.components.ChoiceRow
import com.aquigs.sp21ace.ui.components.ConfirmDialog
import com.aquigs.sp21ace.ui.components.SettingsHeader
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.components.SwitchRow

/** Blackjack Ace's Settings, less what this app has nothing for yet: the discard tray and the play history. */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onOpenColorTheme: () -> Unit,
    onOpenButtonLocation: () -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }

    SettingsPage(title = stringResource(R.string.settings), onBack = onBack, modifier = modifier) {
        ChoiceRow(title = stringResource(R.string.color_theme), value = stringResource(settings.colorTheme.title), onClick = onOpenColorTheme)
        ChoiceRow(title = stringResource(R.string.button_location), value = stringResource(settings.buttonLocation.title), onClick = onOpenButtonLocation)
        VisibilityRow(R.string.hand_totals, settings.handTotals) { onChange(settings.copy(handTotals = it)) }
        VisibilityRow(R.string.strategy_chart_button, settings.chartButton) { onChange(settings.copy(chartButton = it)) }
        SwitchRow(
            title = stringResource(R.string.sound_effects),
            summary = stringResource(if (settings.soundEffects) R.string.on else R.string.muted),
            checked = settings.soundEffects,
            onCheckedChange = { onChange(settings.copy(soundEffects = it)) },
        )
        HorizontalDivider()

        SettingsHeader(stringResource(R.string.practice))
        VisibilityRow(R.string.streak_meter, settings.streakMeter) { onChange(settings.copy(streakMeter = it)) }
        ActionRow(title = stringResource(R.string.clear_practice_history), onClick = { confirmingClear = true })
        HorizontalDivider()

        SettingsHeader(stringResource(R.string.play))
        SwitchRow(
            title = stringResource(R.string.warn_on_incorrect_move),
            summary = stringResource(if (settings.warnOnIncorrectMove) R.string.yes else R.string.no),
            checked = settings.warnOnIncorrectMove,
            onCheckedChange = { onChange(settings.copy(warnOnIncorrectMove = it)) },
        )
        VisibilityRow(R.string.hint_button, settings.hintButton) { onChange(settings.copy(hintButton = it)) }
    }

    // Asks first, as Blackjack Ace does, because nothing brings a cleared history back
    if (confirmingClear) {
        ConfirmDialog(
            title = stringResource(R.string.clear_history_title),
            message = stringResource(R.string.clear_history_message),
            confirmLabel = stringResource(R.string.clear),
            onConfirm = {
                confirmingClear = false
                onClearHistory()
            },
            onDismiss = { confirmingClear = false },
        )
    }
}

@Composable
fun ColorThemeScreen(settings: Settings, onChange: (Settings) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.color_theme),
        choices = ColorTheme.entries.map { it to stringResource(it.title) },
        selected = settings.colorTheme,
        onSelect = { onChange(settings.copy(colorTheme = it)) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun ButtonLocationScreen(settings: Settings, onChange: (Settings) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.button_location),
        choices = ButtonLocation.entries.map { it to stringResource(it.title) },
        selected = settings.buttonLocation,
        onSelect = { onChange(settings.copy(buttonLocation = it)) },
        onBack = onBack,
        modifier = modifier,
    )
}

// As in Blackjack Ace, the summary says whether that part of the trainer shows
@Composable
private fun VisibilityRow(@StringRes title: Int, visible: Boolean, onChange: (Boolean) -> Unit) {
    SwitchRow(
        title = stringResource(title),
        summary = stringResource(if (visible) R.string.visible else R.string.hidden),
        checked = visible,
        onCheckedChange = onChange,
    )
}

private val ColorTheme.title: Int
    get() = when (this) {
        ColorTheme.SYSTEM -> R.string.theme_system
        ColorTheme.LIGHT -> R.string.theme_light
        ColorTheme.DARK -> R.string.theme_dark
    }

private val ButtonLocation.title: Int
    get() = when (this) {
        ButtonLocation.LEFT -> R.string.left
        ButtonLocation.RIGHT -> R.string.right
    }
