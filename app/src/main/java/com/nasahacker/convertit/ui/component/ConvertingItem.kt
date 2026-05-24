@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nasahacker.convertit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.ConversionItem
import com.nasahacker.convertit.domain.model.ConversionStatus

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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AudioItem(
                    fileName = item.fileName,
                    fileSize = item.fileSize,
                    format = item.format,
                    modifier = Modifier.weight(1f),
                )

                if (item.status == ConversionStatus.PENDING || item.status == ConversionStatus.CONVERTING) {
                    FilledTonalIconButton(
                        onClick = onCancel,
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape()),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_close_24),
                            contentDescription = stringResource(R.string.label_cancel),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error,
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
