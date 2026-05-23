package com.nasahacker.convertit.ui.component

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.ConversionEstimates
import com.nasahacker.convertit.ui.component.ProUpgradeDialog
import com.nasahacker.convertit.ui.component.expressive.DialogActionButtonGroup

/**
 * Convertit Android app
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogConvertAlertDialog(
    showDialog: Boolean,
    uris: ArrayList<Uri>,
    /** Sum of source durations (all files); used for batch size/time estimates. */
    totalSourceDurationForEstimateMs: Long = 0L,
    selectedCustomLocation: String = "",
    onChangeCustomLocation: () -> Unit = {},
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onStartConversion: (speed: String, uris: ArrayList<Uri>, bitrate: String, format: String, sampleRate: String) -> Unit = { _, _, _, _, _ -> },
) {
    var selectedFormat by remember(showDialog) { mutableStateOf(".mp3") }
    var selectedBitrate by remember(showDialog) { mutableStateOf("256k") }
    var selectedSampleRate by remember(showDialog) { mutableStateOf("44100") }
    var sliderValue by remember(showDialog) { mutableFloatStateOf(1.0f) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Pro upgrade dialog state
    var showProDialog by remember { mutableStateOf(false) }
    var proDialogFeature by remember { mutableStateOf("") }

    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = stringResource(R.string.label_conversion_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                ) {
                    DialogConvertContent(
                        selectedFormat = selectedFormat,
                        onFormatSelected = { selectedFormat = it },
                        selectedBitrate = selectedBitrate,
                        onBitrateSelected = { selectedBitrate = it },
                        selectedSampleRate = selectedSampleRate,
                        onSampleRateSelected = { selectedSampleRate = it },
                        sliderValue = sliderValue,
                        onSliderValueChanged = { sliderValue = it },
                        totalSourceDurationForEstimateMs = totalSourceDurationForEstimateMs,
                        onProFeatureClick = { featureName ->
                            proDialogFeature = featureName
                            showProDialog = true
                        },
                        selectedCustomLocation = selectedCustomLocation,
                        onChangeCustomLocation = onChangeCustomLocation,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DialogActionButtonGroup(
                    dismissText = stringResource(R.string.label_cancel),
                    confirmText = stringResource(R.string.label_convert),
                    onDismiss = onCancel,
                    onConfirm = {
                        Log.d(
                            "ZERO_DOLLAR",
                            "Starting Service with Format $selectedFormat And Bitrate $selectedBitrate SR $selectedSampleRate",
                        )
                        onStartConversion(
                            sliderValue.toString(),
                            uris,
                            selectedBitrate,
                            selectedFormat,
                            selectedSampleRate,
                        )
                        onDismiss()
                    },
                )
            }
        }
    }
    
    // Pro Upgrade Dialog
    ProUpgradeDialog(
        showDialog = showProDialog,
        onDismiss = { showProDialog = false },
        featureName = proDialogFeature
    )
}

@Composable
fun DialogConvertContent(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    selectedBitrate: String,
    onBitrateSelected: (String) -> Unit,
    selectedSampleRate: String,
    onSampleRateSelected: (String) -> Unit,
    sliderValue: Float,
    onSliderValueChanged: (Float) -> Unit,
    totalSourceDurationForEstimateMs: Long = 0L,
    onProFeatureClick: (String) -> Unit = {},
    selectedCustomLocation: String = "",
    onChangeCustomLocation: () -> Unit = {},
) {
    val allFormats = stringArrayResource(R.array.format_array).toList()
    val bitratesMp3 = stringArrayResource(R.array.bitrates_mp3).toList()
    val bitratesAac = stringArrayResource(R.array.bitrates_aac).toList()
    val bitratesM4a = stringArrayResource(R.array.bitrates_m4a).toList()
    val bitratesOgg = stringArrayResource(R.array.bitrates_ogg).toList()
    val bitratesOpus = stringArrayResource(R.array.bitrates_opus).toList()
    val bitratesWma = stringArrayResource(R.array.bitrates_wma).toList()
    val bitratesMka = stringArrayResource(R.array.bitrates_mka).toList()
    val bitratesSpx = stringArrayResource(R.array.bitrates_spx).toList()
    val bitratesArray = stringArrayResource(R.array.bitrates_array).toList()

    val sampleRatesGeneric = stringArrayResource(R.array.sample_rates_array).toList()
    val sampleRatesMp3 = stringArrayResource(R.array.sample_rates_mp3).toList()
    val sampleRatesAac = stringArrayResource(R.array.sample_rates_aac).toList()
    val sampleRatesM4a = stringArrayResource(R.array.sample_rates_m4a).toList()
    val sampleRatesOgg = stringArrayResource(R.array.sample_rates_ogg).toList()
    val sampleRatesOpus = stringArrayResource(R.array.sample_rates_opus).toList()
    val sampleRatesWma = stringArrayResource(R.array.sample_rates_wma).toList()
    val sampleRatesMka = stringArrayResource(R.array.sample_rates_mka).toList()
    val sampleRatesSpx = stringArrayResource(R.array.sample_rates_spx).toList()
    val sampleRatesAmr = stringArrayResource(R.array.sample_rates_amr).toList()

    val bitrateOptions =
        remember(selectedFormat) {
            when (selectedFormat) {
                ".mp3" -> bitratesMp3
                ".aac" -> bitratesAac
                ".m4a" -> bitratesM4a
                ".ogg" -> bitratesOgg
                ".opus" -> bitratesOpus
                ".wma" -> bitratesWma
                ".mka" -> bitratesMka
                ".spx" -> bitratesSpx
                else -> bitratesArray
            }
        }

    val sampleRateOptions =
        remember(selectedFormat) {
            when (selectedFormat) {
                ".mp3" -> sampleRatesMp3
                ".aac" -> sampleRatesAac
                ".m4a" -> sampleRatesM4a
                ".ogg" -> sampleRatesOgg
                ".opus" -> sampleRatesOpus
                ".wma" -> sampleRatesWma
                ".mka" -> sampleRatesMka
                ".spx" -> sampleRatesSpx
                ".amr" -> sampleRatesAmr
                else -> sampleRatesGeneric
            }
        }

    val conversionEstimate =
        remember(
            totalSourceDurationForEstimateMs,
            selectedFormat,
            selectedBitrate,
            selectedSampleRate,
            sliderValue,
        ) {
            ConversionEstimates.compute(
                totalSourceDurationMs = totalSourceDurationForEstimateMs,
                selectedFormat = selectedFormat,
                selectedBitrate = selectedBitrate,
                selectedSampleRate = selectedSampleRate,
                playbackSpeed = sliderValue,
            )
        }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Format Selection Section (Free features)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Format & Bitrate Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DropdownField(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.label_format_options),
                        options = allFormats,
                        selectedOption = selectedFormat,
                        onOptionSelected = {
                            onFormatSelected(it)
                            val validBitrates = when (it) {
                                ".mp3" -> bitratesMp3
                                ".aac" -> bitratesAac
                                ".m4a" -> bitratesM4a
                                ".ogg" -> bitratesOgg
                                ".opus" -> bitratesOpus
                                ".wma" -> bitratesWma
                                ".mka" -> bitratesMka
                                ".spx" -> bitratesSpx
                                else -> bitratesArray
                            }
                            val validSampleRates = when (it) {
                                ".mp3" -> sampleRatesMp3
                                ".aac" -> sampleRatesAac
                                ".m4a" -> sampleRatesM4a
                                ".ogg" -> sampleRatesOgg
                                ".opus" -> sampleRatesOpus
                                ".wma" -> sampleRatesWma
                                ".mka" -> sampleRatesMka
                                ".spx" -> sampleRatesSpx
                                ".amr" -> sampleRatesAmr
                                else -> sampleRatesGeneric
                            }
                            onSampleRateSelected(validSampleRates.first())
                            onBitrateSelected(validBitrates.firstOrNull() ?: "256k")
                        },
                    )

                    DropdownField(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.label_bitrate_options),
                        options = bitrateOptions,
                        selectedOption = selectedBitrate.takeIf { it in bitrateOptions } ?: bitrateOptions.firstOrNull() ?: "256k",
                        onOptionSelected = onBitrateSelected,
                    )
                }

                // Sample Rate (Free)
                DropdownField(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.label_sample_rate_options),
                    options = sampleRateOptions,
                    selectedOption = selectedSampleRate.takeIf { it in sampleRateOptions } ?: sampleRateOptions.first(),
                    onOptionSelected = onSampleRateSelected,
                    optionLabel = ::formatHzToKHz,
                )

                // Playback Speed (Free)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.label_slider),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "${"%.2f".format(sliderValue)}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = onSliderValueChanged,
                        valueRange = 0.5f..2.0f,
                        steps = 30,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // Save Location Section
        SaveLocationCard(
            selectedCustomLocation = selectedCustomLocation,
            onChangeCustomLocation = onChangeCustomLocation,
        )

        // Conversion Estimate Card
        ConversionEstimateCard(estimate = conversionEstimate)

        // Expandable Pro Features Section (Presets, Channel, Trim, CUE, Silence)
        ExpandableProFeaturesSection(onProFeatureClick = onProFeatureClick)
    }
}




@Composable
private fun SaveLocationCard(
    selectedCustomLocation: String,
    onChangeCustomLocation: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val displayName = remember(selectedCustomLocation) {
        when {
            selectedCustomLocation.isBlank() -> context.getString(R.string.label_save_location_default)
            selectedCustomLocation.startsWith("content://") -> {
                val uri = Uri.parse(selectedCustomLocation)
                uri.lastPathSegment?.substringAfterLast(':')?.replace("%2F", "/") ?: context.getString(R.string.label_custom_folder)
            }
            selectedCustomLocation.startsWith("/") -> {
                val name = java.io.File(selectedCustomLocation).name
                name.takeIf { it.isNotBlank() } ?: context.getString(R.string.label_custom_folder)
            }
            else -> selectedCustomLocation
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChangeCustomLocation() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_save_location),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = onChangeCustomLocation,
            ) {
                Text(
                    text = stringResource(R.string.label_change),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ProCompactLabel() {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pro_dialog_chip_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ExpandableProFeaturesSection(
    onProFeatureClick: (String) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pro_dialog_tools_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            if (isExpanded) {
                                stringResource(R.string.pro_dialog_tools_hint_expanded)
                            } else {
                                stringResource(R.string.pro_dialog_tools_hint_collapsed)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription =
                        if (isExpanded) {
                            stringResource(R.string.pro_dialog_a11y_collapse_tools)
                        } else {
                            stringResource(R.string.pro_dialog_a11y_expand_tools)
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("Save & Load Presets") },
                        icon = Icons.Filled.Bookmark,
                        title = stringResource(R.string.pro_dialog_presets_title),
                        subtitle = stringResource(R.string.pro_dialog_presets_subtitle),
                    )
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("AI Noise Reduction") },
                        icon = Icons.Filled.GraphicEq,
                        title = stringResource(R.string.pro_dialog_noise_title),
                        subtitle = stringResource(R.string.pro_dialog_noise_subtitle),
                    )
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("Channel Control") },
                        icon = Icons.Filled.SurroundSound,
                        title = stringResource(R.string.pro_dialog_channel_title),
                        subtitle = stringResource(R.string.pro_dialog_channel_subtitle),
                    )
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("Trim Audio/Video") },
                        icon = Icons.Filled.ContentCut,
                        title = stringResource(R.string.pro_dialog_trim_title),
                        subtitle = stringResource(R.string.pro_dialog_trim_subtitle),
                    )
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("Video compression") },
                        icon = Icons.Filled.Compress,
                        title = stringResource(R.string.pro_dialog_video_compression_title),
                        subtitle = stringResource(R.string.pro_dialog_video_compression_subtitle),
                    )
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("CUE Track Splitting") },
                        icon = Icons.Filled.LibraryMusic,
                        title = stringResource(R.string.pro_dialog_cue_title),
                        subtitle = stringResource(R.string.pro_dialog_cue_subtitle),
                    )
                    LockedProFeatureRow(
                        onClick = { onProFeatureClick("Silence Removal") },
                        icon = Icons.AutoMirrored.Filled.VolumeOff,
                        title = stringResource(R.string.pro_dialog_silence_title),
                        subtitle = stringResource(R.string.pro_dialog_silence_subtitle),
                    )
                }
            }
        }
    }
}

private val LockedProTrailingSlotWidth = 132.dp

@Composable
private fun LockedProTrailing() {
    Box(
        modifier =
            Modifier
                .width(LockedProTrailingSlotWidth)
                .heightIn(min = 40.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProCompactLabel()
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(R.string.pro_dialog_locked_feature_a11y),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LockedProFeatureRow(
    onClick: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            trailingContent = { LockedProTrailing() },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

@Composable
private fun ConversionEstimateSectionHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_conversion_estimate_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FeatureBetaBadge()
    }
}

@Composable
private fun EstimateMetricCapsule(
    label: String,
    value: String,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ConversionEstimateCard(
    estimate: ConversionEstimates.EstimateState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ConversionEstimateSectionHeader()
            when (estimate) {
                ConversionEstimates.EstimateState.Unavailable -> {
                    Text(
                        text = stringResource(R.string.label_conversion_estimate_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ConversionEstimates.EstimateState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.label_conversion_estimate_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is ConversionEstimates.EstimateState.Ready -> {
                    val minB = estimate.sizeMinBytes
                    val maxB = estimate.sizeMaxBytes
                    val sizeCapsuleValue: String? =
                        when {
                            estimate.isVideoContainerOutput -> null
                            estimate.isLosslessCompressedRange && minB != null && maxB != null ->
                                "${ConversionEstimates.formatByteCountSi(minB)}–${ConversionEstimates.formatByteCountSi(maxB)}"
                            minB != null -> ConversionEstimates.formatByteCountSi(minB)
                            else -> null
                        }
                    val timeText =
                        ConversionEstimates.formatDurationRange(
                            estimate.encodeSecondsMin,
                            estimate.encodeSecondsMax,
                        )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (sizeCapsuleValue != null) {
                            EstimateMetricCapsule(
                                label = stringResource(R.string.label_conversion_estimate_label_size),
                                value = sizeCapsuleValue,
                            )
                        }
                        EstimateMetricCapsule(
                            label = stringResource(R.string.label_conversion_estimate_label_time),
                            value = timeText,
                        )
                    }
                    if (estimate.isVideoContainerOutput) {
                        Text(
                            text = stringResource(R.string.label_conversion_estimate_video_size_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.label_conversion_estimate_device_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (String) -> String = { it },
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = optionLabel(selectedOption),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = optionLabel(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

fun formatHzToKHz(value: String): String {
    return try {
        val hz = value.toInt()
        val k = hz / 1000
        val rem = hz % 1000
        if (rem == 0) {
            "${k} kHz"
        } else {
            val decimal = (hz / 100.0).toInt() / 10.0
            "${decimal} kHz"
        }
    } catch (e: Exception) {
        value
    }
}

@Preview
@Composable
fun PreviewDialogConvertAlertDialog() {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        DialogConvertAlertDialog(
            showDialog = showDialog,
            onDismiss = { },
            onCancel = { showDialog = false },
            uris = ArrayList(),
        )
    }
}
