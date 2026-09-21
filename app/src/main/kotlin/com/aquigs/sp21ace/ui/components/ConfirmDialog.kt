package com.aquigs.sp21ace.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R

/**
 * Asks before an action, with a button named for it and Cancel. The dialog opens under the finger that asked for it, so every way
 * out waits out a double tap, the second tap of which could otherwise land on the button or dismiss the dialog from outside it.
 */
@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val armed by rememberArmed(Unit)

    AlertDialog(
        onDismissRequest = { if (armed) onDismiss() },
        confirmButton = { TextButton(onClick = { if (armed) onConfirm() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = { if (armed) onDismiss() }) { Text(stringResource(R.string.cancel)) } },
        title = { Text(title) },
        text = { Text(message) },
    )
}
