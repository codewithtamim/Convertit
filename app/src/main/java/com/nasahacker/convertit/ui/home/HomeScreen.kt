package com.nasahacker.convertit.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.ConvertitDarkPreview
import com.nasahacker.convertit.domain.model.ConvertitLightPreview
import com.nasahacker.convertit.service.ConvertItService
import com.nasahacker.convertit.domain.model.ConversionStatus
import com.nasahacker.convertit.ui.component.AudioItem
import com.nasahacker.convertit.ui.component.ConvertingItem
import com.nasahacker.convertit.ui.component.DialogConvertAlertDialog
import com.nasahacker.convertit.ui.component.DialogEditMetadata
import com.nasahacker.convertit.ui.component.ExpandableFab
import com.nasahacker.convertit.ui.component.RatingDialog
import com.nasahacker.convertit.util.IntentLauncher
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch

/**
 * Convertit Android app
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 */

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val intentLauncher = remember { IntentLauncher(context as Activity) }
    val uriList by viewModel.uriList.collectAsStateWithLifecycle()
    val totalMediaDurationMs by viewModel.totalMediaDurationMs.collectAsStateWithLifecycle()
    val openConversionSheetAfterShare by viewModel.openConversionSheetAfterShare.collectAsStateWithLifecycle()
    val metadataUri by viewModel.metadataUri.collectAsStateWithLifecycle()
    val conversionStatus by viewModel.conversionStatus.collectAsStateWithLifecycle()
    val selectedCustomLocation by viewModel.selectedCustomLocation.collectAsStateWithLifecycle()
    val convertingItems by viewModel.convertingItems.collectAsStateWithLifecycle()
    val lastConvertedFileUri by viewModel.lastConvertedFileUri.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showVideoDialog by rememberSaveable { mutableStateOf(false) }
    var showMetadataDialog by rememberSaveable { mutableStateOf(false) }
    var showReviewDialog by rememberSaveable { mutableStateOf(false) }
    
    // Snackbar for conversion success
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.updateUriList(result.data)
                showDialog = true
            }
        }

    val videoPickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.updateUriList(result.data)
                showVideoDialog = true
            }
        }

    val folderPickLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                        viewModel.onSelectedCustomLocation(uri.toString())
                        
                        
                        val displayName = when {
                            uri.scheme == "content" && uri.path?.contains("/tree/primary:") == true -> {
                                uri.lastPathSegment?.substringAfterLast(':')?.replace("%2F", "/") ?: "Custom folder"
                            }
                            uri.scheme == "content" -> {
                                uri.lastPathSegment?.substringAfterLast(':') ?: "Custom folder"
                            }
                            else -> {
                                uri.lastPathSegment ?: "Custom folder"
                            }
                        }
                        
                        Toast
                            .makeText(context, "Save location updated to: $displayName", Toast.LENGTH_LONG)
                            .show()
                    } catch (e: Exception) {
                        Toast
                            .makeText(context, "Failed to set custom location", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

    val metadataPickFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.updateMetadataUri(result.data)
                showMetadataDialog = true
            }
        }

    val isDontShowAgain by viewModel.isDontShowAgain.collectAsStateWithLifecycle()
    val shouldShowReviewDialog =
        remember(showReviewDialog, isDontShowAgain) {
            showReviewDialog && !isDontShowAgain
        }

    LaunchedEffect(openConversionSheetAfterShare) {
        if (openConversionSheetAfterShare) {
            showDialog = true
            viewModel.clearOpenConversionSheetAfterShare()
        }
    }

    LaunchedEffect(showDialog, showVideoDialog, uriList) {
        // Both audio and video dialogs share `totalMediaDurationMs` for their
        // estimate row, so the probe needs to fire whichever dialog opens —
        // otherwise picking a video left the estimate stuck on "loading…".
        if ((showDialog || showVideoDialog) && uriList.isNotEmpty()) {
            viewModel.loadMediaDurationsForConversionDialog(uriList)
        }
    }

    LaunchedEffect(conversionStatus) {
        conversionStatus?.let { isSuccess ->
            if (isSuccess) {
                viewModel.clearUriList()
                viewModel.resetConversionStatus()
                showReviewDialog = true

                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.conversion_success_message),
                        duration = androidx.compose.material3.SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    RatingDialog(
        showReviewDialog = shouldShowReviewDialog,
        dontShowAgainInitially = isDontShowAgain,
        onSaveDontShowAgain = { checked -> viewModel.onIsDontShowAgainSelected(checked) },
        onConfirm = { showReviewDialog = false },
        onDismiss = { showReviewDialog = false },
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Show converting items at the top with progress animation
            items(
                items = convertingItems,
                key = { it.uri.toString() }
            ) { item ->
                ConvertingItem(
                    item = item,
                    isVisible = item.status != ConversionStatus.COMPLETED && item.status != ConversionStatus.FAILED
                )
            }
            
            // Show selected files (only if not already in convertingItems)
            items(uriList) { uri ->
                if (convertingItems.none { it.uri == uri }) {
                    val fileInfo = remember(uri) { 
                        viewModel.getFileInfoFromUri(uri)
                    }
                    AudioItem(
                        fileName = fileInfo.first,
                        fileSize = fileInfo.second,
                        format = fileInfo.third,
                    )
                }
            }
        }

        ExpandableFab(
            onEditMetadataClick = {
                intentLauncher.openMetadataEditorFilePicker(metadataPickFileLauncher)
            },
            onConvertAudioClick = {
                if (ConvertItService.isForegroundServiceStarted) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.label_warning),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    intentLauncher.openFilePicker(pickFileLauncher)
                }
            },
            onConvertVideoClick = {
                if (ConvertItService.isForegroundServiceStarted) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.label_warning),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    showVideoDialog = true
                    intentLauncher.openVideoFilePicker(videoPickFileLauncher)
                }
            },
            onCustomSaveLocationClick = {
                val displayLocation = when {
                    selectedCustomLocation.isBlank() -> "Default (Music/ConvertIt)"
                    selectedCustomLocation.startsWith("content://") -> {
                        val parsed = Uri.parse(selectedCustomLocation)
                        parsed.lastPathSegment?.substringAfterLast(':')?.replace("%2F", "/") ?: "Custom folder"
                    }
                    selectedCustomLocation.startsWith("/") -> {
                        File(selectedCustomLocation).name.takeIf { it.isNotBlank() } ?: "ConvertIt"
                    }
                    else -> selectedCustomLocation
                }
                Toast.makeText(context, "Current: $displayLocation", Toast.LENGTH_LONG).show()
                intentLauncher.openFolderPicker(folderPickLauncher)
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        )
        
        // Snackbar for conversion success
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }

    DialogConvertAlertDialog(
        showDialog = showDialog,
        onDismiss = {
            viewModel.clearUriList()
            showDialog = false
        },
        onCancel = {
            viewModel.clearUriList()
            showDialog = false
        },
        uris = uriList,
        totalSourceDurationForEstimateMs = totalMediaDurationMs,
        onStartConversion = { speed, uris, bitrate, format, sampleRate ->
            viewModel.startConversion(speed, uris, bitrate, format, sampleRate)
        },
    )
    
    // Video Conversion Dialog
    DialogConvertAlertDialog(
        showDialog = showVideoDialog,
        onDismiss = {
            viewModel.clearUriList()
            showVideoDialog = false
        },
        onCancel = {
            viewModel.clearUriList()
            showVideoDialog = false
        },
        uris = uriList,
        totalSourceDurationForEstimateMs = totalMediaDurationMs,
        onStartConversion = { speed, uris, bitrate, format, sampleRate ->
            viewModel.startConversion(speed, uris, bitrate, format, sampleRate)
        },
    )

    DialogEditMetadata(
        showDialog = showMetadataDialog,
        audioUri = metadataUri,
        onDismissRequest = {
            showMetadataDialog = false
            viewModel.setMetadataUri(null)
        },
        onMetadataSaved = {},
        onLoadMetadata = { uri -> viewModel.loadMetadata(uri) },
        onSaveMetadata = { uri, metadata -> viewModel.saveMetadata(uri, metadata) },
        onSaveCoverArt = { uri, bitmap -> viewModel.saveCoverArt(uri, bitmap) },
    )
}

@ConvertitLightPreview
@ConvertitDarkPreview
@Composable
fun PreviewHomeScreen() {
    HomeScreen()
}
