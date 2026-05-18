package com.nasahacker.convertit.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.nasahacker.convertit.R

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableFab(
    onEditMetadataClick: () -> Unit,
    onConvertAudioClick: () -> Unit,
    onConvertVideoClick: () -> Unit,
    onCustomSaveLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }
    
    val menuItems = listOf(
        FabMenuItem(
            icon = Icons.Filled.Folder,
            label = stringResource(R.string.label_custom_save_location_action),
            onClick = {
                onCustomSaveLocationClick()
                fabMenuExpanded = false
            }
        ),
        FabMenuItem(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.label_edit_metadata_action),
            onClick = {
                onEditMetadataClick()
                fabMenuExpanded = false
            }
        ),
        FabMenuItem(
            icon = Icons.Filled.VideoLibrary,
            label = stringResource(R.string.label_convert_video_action),
            onClick = {
                onConvertVideoClick()
                fabMenuExpanded = false
            }
        ),
        FabMenuItem(
            icon = Icons.Filled.Audiotrack,
            label = stringResource(R.string.label_convert_audio_action),
            onClick = {
                onConvertAudioClick()
                fabMenuExpanded = false
            }
        ),
    )

    val toggleContentDescription = stringResource(R.string.label_actions)
    val expandedStateDescription = stringResource(R.string.label_expanded)
    val collapsedStateDescription = stringResource(R.string.label_collapsed)

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier.semantics {
                    stateDescription = if (fabMenuExpanded) expandedStateDescription else collapsedStateDescription
                    contentDescription = toggleContentDescription
                },
                checked = fabMenuExpanded,
                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.MusicNote
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        menuItems.forEach { item ->
            FloatingActionButtonMenuItem(
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = null) },
                text = { Text(text = item.label) },
            )
        }
    }
}

private data class FabMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)
