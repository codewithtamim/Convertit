@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.ConversionItem
import com.nasahacker.convertit.domain.model.ConversionStatus

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConvertingItem(
    item: ConversionItem,
    isVisible: Boolean = true,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress",
    )

    var showMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        exit = slideOutHorizontally(
            animationSpec = tween(300),
            targetOffsetX = { fullWidth -> fullWidth },
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box {
                AudioItem(
                    fileName = item.fileName,
                    fileSize = item.fileSize,
                    format = item.format,
                    onLongClick = {
                        if (item.status == ConversionStatus.PENDING || item.status == ConversionStatus.CONVERTING) {
                            showMenu = true
                        }
                    },
                )

                if (item.status == ConversionStatus.PENDING || item.status == ConversionStatus.CONVERTING) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.label_cancel_conversion),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_close_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCancel()
                            },
                        )
                    }
                }
            }

            when (item.status) {
                ConversionStatus.PENDING -> {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                ConversionStatus.CONVERTING -> {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                ConversionStatus.COMPLETED,
                ConversionStatus.FAILED,
                ConversionStatus.CANCELLED -> {}
            }
        }
    }
}
