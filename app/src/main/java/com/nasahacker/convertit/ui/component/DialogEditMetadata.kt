package com.nasahacker.convertit.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.Metadata
import com.nasahacker.convertit.ui.component.expressive.DialogActionButtonGroup
import kotlinx.coroutines.launch

/**
 * Convertit Android app
 * <a href="https://github.com/thebytearray/Convertit">GitHub Repository</a>
 *
 * Created by Tamim Hossain.
 * Copyright (c) 2025 The Byte Array LTD.
 *
 * This file is part of the Convertit Android app.
 *
 * The Convertit Android app is free software: you can redistribute it and/or
 * modify it under the terms of the Apache License, Version 2.0 as published by
 * the Apache Software Foundation.
 *
 * The Convertit Android app is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the Apache License for more
 * details.
 *
 * You should have received a copy of the Apache License
 * along with the Convertit Android app. If not, see <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>.
 *
 * @author Tamim Hossain
 * @company The Byte Array LTD
 * @year 2025
 * @license Apache-2.0
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEditMetadata(
    showDialog: Boolean,
    audioUri: Uri?,
    onDismissRequest: () -> Unit,
    onMetadataSaved: () -> Unit = {},
    onLoadMetadata: suspend (Uri) -> Metadata = { Metadata() },
    onSaveMetadata: suspend (Uri, Metadata) -> Boolean = { _, _ -> false },
    onSaveCoverArt: suspend (Uri, Bitmap?) -> Boolean = { _, _ -> false },
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var metadata by remember { mutableStateOf(Metadata()) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var titleText by remember { mutableStateOf("") }
    var artistText by remember { mutableStateOf("") }
    var albumText by remember { mutableStateOf("") }
    var albumArtistText by remember { mutableStateOf("") }
    var genreText by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }
    var trackText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }

    var currentCoverArt by remember { mutableStateOf<Bitmap?>(null) }
    var newCoverArt by remember { mutableStateOf<Bitmap?>(null) }
    var hasCoverArtChanged by remember { mutableStateOf(false) }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType?.startsWith("image/") == true) {
                        val bitmap =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                        newCoverArt = bitmap
                        hasCoverArtChanged = true
                    } else {
                        Toast.makeText(context, context.getString(R.string.label_please_select_image_only), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.label_failed_to_load_image, e.message ?: ""),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }

    LaunchedEffect(showDialog, audioUri) {
        if (showDialog && audioUri != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val loadedMetadata = onLoadMetadata(audioUri)
                    metadata = loadedMetadata

                    titleText = loadedMetadata.title
                    artistText = loadedMetadata.artist
                    albumText = loadedMetadata.album
                    albumArtistText = loadedMetadata.albumArtist
                    genreText = loadedMetadata.genre
                    yearText = loadedMetadata.year
                    trackText = loadedMetadata.track
                    commentText = loadedMetadata.comment

                    currentCoverArt =
                        loadedMetadata.pictures.firstOrNull()?.let { picture ->
                            BitmapFactory.decodeByteArray(picture.data, 0, picture.data.size)
                        }
                    newCoverArt = null
                    hasCoverArtChanged = false
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.label_failed_to_load_metadata, e.message ?: ""),
                            Toast.LENGTH_SHORT,
                        ).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.label_edit_metadata),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Cover Art Section
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val displayBitmap = newCoverArt ?: currentCoverArt

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Cover Art Preview
                                    if (displayBitmap != null) {
                                        Box {
                                            Image(
                                                bitmap = displayBitmap.asImageBitmap(),
                                                contentDescription = stringResource(R.string.label_cover_art),
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                            // Remove button
                                            IconButton(
                                                onClick = {
                                                    currentCoverArt = null
                                                    newCoverArt = null
                                                    hasCoverArtChanged = true
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.errorContainer,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.label_remove),
                                                        modifier = Modifier.size(16.dp).padding(2.dp),
                                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = stringResource(R.string.label_no_cover_art),
                                                modifier = Modifier.size(32.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Cover Art Info & Button
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.label_cover_art),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (hasCoverArtChanged) {
                                            Text(
                                                text = stringResource(R.string.label_cover_art_will_be_updated),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        } else {
                                            Text(
                                                text = if (displayBitmap != null) stringResource(R.string.label_tap_to_change) else stringResource(R.string.label_no_cover_art),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        FilledTonalButton(
                                            onClick = { imagePickerLauncher.launch("image/*") },
                                            shape = RoundedCornerShape(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (displayBitmap != null) Icons.Default.Edit else Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(if (displayBitmap != null) R.string.label_change else R.string.label_add),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Track Info Section
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Section Header
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.label_track_info),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                OutlinedTextField(
                                    value = titleText,
                                    onValueChange = { titleText = it },
                                    label = { Text(stringResource(R.string.label_title)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    OutlinedTextField(
                                        value = trackText,
                                        onValueChange = { trackText = it },
                                        label = { Text(stringResource(R.string.label_track_number)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    )

                                    OutlinedTextField(
                                        value = yearText,
                                        onValueChange = { yearText = it },
                                        label = { Text(stringResource(R.string.label_year)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    )
                                }

                                OutlinedTextField(
                                    value = genreText,
                                    onValueChange = { genreText = it },
                                    label = { Text(stringResource(R.string.label_genre)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )
                            }
                        }

                        // Artist Info Section
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Section Header
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.label_artist_info),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                OutlinedTextField(
                                    value = artistText,
                                    onValueChange = { artistText = it },
                                    label = { Text(stringResource(R.string.label_artist)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )

                                OutlinedTextField(
                                    value = albumArtistText,
                                    onValueChange = { albumArtistText = it },
                                    label = { Text(stringResource(R.string.label_album_artist)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )
                            }
                        }

                        // Album Info Section
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Section Header
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Album,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.label_album_info),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                OutlinedTextField(
                                    value = albumText,
                                    onValueChange = { albumText = it },
                                    label = { Text(stringResource(R.string.label_album)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )

                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    label = { Text(stringResource(R.string.label_comment)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 3,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    DialogActionButtonGroup(
                        dismissText = stringResource(R.string.label_cancel),
                        confirmText = stringResource(if (isSaving) R.string.label_saving else R.string.label_save),
                        onDismiss = onDismissRequest,
                        onConfirm = {
                            if (audioUri != null) {
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        val updatedMetadata =
                                            Metadata(
                                                title = titleText,
                                                artist = artistText,
                                                album = albumText,
                                                albumArtist = albumArtistText,
                                                genre = genreText,
                                                year = yearText,
                                                track = trackText,
                                                comment = commentText,
                                            )

                                        var success = onSaveMetadata(audioUri, updatedMetadata)

                                        if (success && hasCoverArtChanged) {
                                            success = onSaveCoverArt(audioUri, newCoverArt)
                                        }

                                        if (success) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.label_metadata_saved_successfully),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            onMetadataSaved()
                                            onDismissRequest()
                                        } else {
                                            Toast
                                                .makeText(
                                                    context,
                                                    context.getString(R.string.label_failed_to_save_metadata),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.label_error_saving_metadata, e.message ?: ""),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        dismissEnabled = !isSaving,
                        confirmEnabled = !isSaving && !isLoading,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDialogEditMetadata() {
    MaterialTheme {
        DialogEditMetadata(
            showDialog = true,
            audioUri = null,
            onDismissRequest = {},
            onMetadataSaved = {},
        )
    }
}
