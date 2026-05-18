package com.nasahacker.convertit.ui.library

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.nasahacker.convertit.ui.component.AudioItem
import com.nasahacker.convertit.ui.component.NoFilesFoundCard
import com.nasahacker.convertit.ui.component.SectionHeader
import com.nasahacker.convertit.util.IntentLauncher

@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val intentLauncher = remember { IntentLauncher(context as Activity) }
    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshFiles()
    }

    if (audioFiles.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (isSelectionMode) {
                    item {
                        SelectionHeader(
                            selectedCount = selectedFiles.size,
                            onClearSelection = { viewModel.exitSelectionMode() },
                        )
                    }
                }

                if (audioFiles.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(R.string.label_converted_audio))
                    }
                    items(audioFiles, key = { "audio_${it.stableKey}" }) { item ->
                        AudioItem(
                            fileName = item.name,
                            fileSize = item.size,
                            format = item.format,
                            isActionVisible = true,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedFiles.contains(item.stableKey),
                            itemIcon = Icons.Filled.Audiotrack,
                            onOpenClick = {
                                intentLauncher.openAudioFromLibrary(item)
                            },
                            onShareClick = {
                                intentLauncher.shareAudioFromLibrary(item)
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    viewModel.enterSelectionMode()
                                }
                                viewModel.toggleFileSelection(item.stableKey)
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleFileSelection(item.stableKey)
                                }
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelectionMode && selectedFiles.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                FloatingActionButton(
                    onClick = {
                        intentLauncher.shareMultipleAudioFiles(viewModel.getSelectedAudioFiles())
                        viewModel.exitSelectionMode()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.label_share_selected),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    } else {
        NoFilesFoundCard()
    }
}

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    onClearSelection: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_selected_count, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onClearSelection) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.label_clear_selection),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@ConvertitLightPreview
@ConvertitDarkPreview
@Composable
fun PreviewLibraryScreen(modifier: Modifier = Modifier) {
    LibraryScreen()
}
