@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component.expressive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard "cancel + confirm" footer for dialogs, rendered as an expressive
 * [ButtonGroup] with connected shapes via [ButtonDefaults.shapes].
 *
 * Set [destructive] to colour the confirm button with the error palette.
 */
@Suppress("DEPRECATION")
@Composable
fun DialogActionButtonGroup(
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    destructive: Boolean = false,
) {
    ButtonGroup(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onDismiss,
            enabled = dismissEnabled,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = dismissText, style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shapes = ButtonDefaults.shapes(),
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = confirmText, style = MaterialTheme.typography.labelLarge)
        }
    }
}
