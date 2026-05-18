package com.nasahacker.convertit.domain.repository

import android.net.Uri
import com.nasahacker.convertit.domain.model.AudioBitrate
import com.nasahacker.convertit.domain.model.AudioFormat
import com.nasahacker.convertit.domain.model.AudioSampleRate

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

interface AudioConverterRepository {
    /** Duration in milliseconds from FFprobe, or 0 if unknown. */
    suspend fun getMediaDuration(uri: Uri): Long

    suspend fun convertAudio(
        customSaveUri: Uri?,
        playbackSpeed: String,
        uris: List<Uri>,
        outputFormat: AudioFormat,
        bitrate: AudioBitrate,
        sampleRate: AudioSampleRate,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit,
    )

    suspend fun performConversion(
        customSaveUri: Uri?,
        playbackSpeed: String,
        uris: List<Uri>,
        outputFormat: AudioFormat,
        bitrate: AudioBitrate,
        sampleRate: AudioSampleRate,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onFileStart: (Uri, Int, Int) -> Unit = { _, _, _ -> },
        onFileProgress: (Uri, Float) -> Unit = { _, _ -> },
        onFileComplete: (Uri, Boolean) -> Unit = { _, _ -> },
    )

    suspend fun convertWithCueSplitting(
        customSaveUri: Uri?,
        playbackSpeed: String,
        uri: Uri,
        outputFormat: AudioFormat,
        bitrate: AudioBitrate,
        sampleRate: AudioSampleRate,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit,
    )

    suspend fun convertWithManualCue(
        customSaveUri: Uri?,
        playbackSpeed: String,
        audioUri: Uri,
        cueUri: Uri,
        outputFormat: AudioFormat,
        bitrate: AudioBitrate,
        sampleRate: AudioSampleRate,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit,
    )
}
