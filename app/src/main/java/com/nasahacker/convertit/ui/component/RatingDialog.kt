package com.nasahacker.convertit.ui.component

/**
 * @author Tamim Hossain
 * @email tamimh.dev@gmail.com
 * @license Apache-2.0
 *
 * ConvertIt is a free and easy-to-use audio converter app.
 * It supports popular audio formats like MP3 and M4A.
 * With options for high-quality bitrates ranging from 128k to 320k,
 * ConvertIt offers a seamless conversion experience tailored to your needs.
 */

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nasahacker.convertit.App
import com.nasahacker.convertit.R

@Composable
fun RatingDialog(
    showReviewDialog: Boolean,
    dontShowAgainInitially: Boolean,
    onSaveDontShowAgain: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedRating by remember { mutableIntStateOf(0) }
    val appPackageName = App.application.packageName
    var dontShowAgain by remember { mutableStateOf(dontShowAgainInitially) }

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.label_enjoying_app),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.label_feedback_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Star rating row
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(5) { index ->
                            IconButton(
                                onClick = { selectedRating = index + 1 }
                            ) {
                                Icon(
                                    imageVector = if (selectedRating > index) 
                                        Icons.Filled.Star 
                                    else 
                                        Icons.Filled.StarOutline,
                                    contentDescription = stringResource(R.string.label_rate_stars, index + 1),
                                    tint = if (selectedRating > index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    if (selectedRating > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedRating) {
                                1 -> stringResource(R.string.label_feedback_1)
                                2 -> stringResource(R.string.label_feedback_2)
                                3 -> stringResource(R.string.label_feedback_3)
                                4 -> stringResource(R.string.label_feedback_4)
                                5 -> stringResource(R.string.label_feedback_5)
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = stringResource(R.string.label_dont_show_again),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onSaveDontShowAgain(dontShowAgain)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.label_not_now))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            context.getString(R.string.label_google_play, appPackageName).toUri()
                        )
                        context.startActivity(intent)
                        onSaveDontShowAgain(dontShowAgain)
                        onConfirm()
                    },
                    enabled = selectedRating > 0
                ) {
                    Text(stringResource(R.string.label_rate_now))
                }
            }
        )
    }
}
