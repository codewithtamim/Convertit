package com.nasahacker.convertit.ui.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nasahacker.convertit.domain.model.AudioBitrate
import com.nasahacker.convertit.domain.model.AudioFormat
import com.nasahacker.convertit.domain.model.ConversionItem
import com.nasahacker.convertit.domain.model.ConversionStatus
import com.nasahacker.convertit.domain.model.Metadata
import com.nasahacker.convertit.domain.repository.FileAccessRepository
import com.nasahacker.convertit.domain.usecase.GetDontShowAgainUseCase
import com.nasahacker.convertit.domain.usecase.GetSelectedCustomLocationUseCase
import com.nasahacker.convertit.domain.usecase.LoadMetadataUseCase
import com.nasahacker.convertit.domain.usecase.SaveDontShowAgainUseCase
import com.nasahacker.convertit.domain.usecase.SaveMetadataUseCase
import com.nasahacker.convertit.domain.usecase.SaveSelectedCustomLocationUseCase
import com.nasahacker.convertit.domain.usecase.StartAudioConversionUseCase
import com.nasahacker.convertit.domain.repository.AudioConverterRepository
import com.nasahacker.convertit.service.ConvertItService
import com.nasahacker.convertit.util.AppConfig
import com.nasahacker.convertit.util.AppUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
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

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val application: Application,
        private val fileAccessRepository: FileAccessRepository,
        private val getDontShowAgain: GetDontShowAgainUseCase,
        private val getSelectedCustomLocation: GetSelectedCustomLocationUseCase,
        private val saveDontShowAgain: SaveDontShowAgainUseCase,
        private val saveSelectedCustomLocation: SaveSelectedCustomLocationUseCase,
        private val startAudioConversion: StartAudioConversionUseCase,
        private val loadMetadata: LoadMetadataUseCase,
        private val saveMetadata: SaveMetadataUseCase,
        private val audioConverterRepository: AudioConverterRepository,
    ) : ViewModel() {
        
        companion object {
            private const val TAG = "HomeViewModel"
        }

        private var areReceiversRegistered = false

        private val _uriList = MutableStateFlow<ArrayList<Uri>>(ArrayList())
        val uriList: StateFlow<ArrayList<Uri>> = _uriList

        private val _openConversionSheetAfterShare = MutableStateFlow(false)
        val openConversionSheetAfterShare: StateFlow<Boolean> = _openConversionSheetAfterShare

        private val _metadataUri = MutableStateFlow<Uri?>(null)
        val metadataUri: StateFlow<Uri?> = _metadataUri

        private val _conversionStatus = MutableStateFlow<Boolean?>(null)
        val conversionStatus: StateFlow<Boolean?> = _conversionStatus

        private val _isConversionInProgress = MutableStateFlow(false)
        val isConversionInProgress: StateFlow<Boolean> = _isConversionInProgress

        private val _conversionProgress = MutableStateFlow(0)
        val conversionProgress: StateFlow<Int> = _conversionProgress

        /**
         * Sum of source durations for all URIs in the conversion dialog (batch estimate).
         *
         * Sentinel contract consumed by `ConversionEstimates.compute`:
         *   `-1L` -> probe still in flight (UI shows the calculating spinner)
         *   `0L`  -> probe completed but couldn't determine duration (e.g. video
         *           with no audio stream / unsupported container) -> UI shows
         *           "Estimate unavailable" instead of looping the spinner forever.
         *   `>0`  -> real duration in ms.
         */
        private val _totalMediaDurationMs = MutableStateFlow(-1L)
        val totalMediaDurationMs: StateFlow<Long> = _totalMediaDurationMs

        private val _convertingItems = MutableStateFlow<List<ConversionItem>>(emptyList())
        val convertingItems: StateFlow<List<ConversionItem>> = _convertingItems

        /** Last successfully converted file URI for the snackbar action */
        private val _lastConvertedFileUri = MutableStateFlow<Uri?>(null)
        val lastConvertedFileUri: StateFlow<Uri?> = _lastConvertedFileUri

        val isDontShowAgain: StateFlow<Boolean> =
            getDontShowAgain().stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                false,
            )

        val selectedCustomLocation: StateFlow<String> =
            getSelectedCustomLocation().stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                "",
            )

        fun onIsDontShowAgainSelected(value: Boolean) {
            viewModelScope.launch { saveDontShowAgain(value) }
        }

        fun onSelectedCustomLocation(value: String) {
            Log.d(TAG, "onSelectedCustomLocation called with: '$value'")
            viewModelScope.launch { 
                saveSelectedCustomLocation(value)
            }
        }

        private val conversionStatusReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val isSuccess = intent?.getBooleanExtra(AppConfig.IS_SUCCESS, false) == true
                    viewModelScope.launch {
                        _conversionStatus.value = isSuccess
                        _isConversionInProgress.value = false
                        if (isSuccess) {
                            _conversionProgress.value = 100
                        } else {
                            _conversionProgress.value = 0
                        }
                        _convertingItems.value = emptyList()
                        clearUriList()
                    }
                }
            }

        private val conversionProgressReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val fileUriString = intent?.getStringExtra(AppConfig.FILE_URI) ?: return
                    val progress = intent.getFloatExtra(AppConfig.FILE_PROGRESS, 0f)
                    
                    viewModelScope.launch {
                        val currentItems = _convertingItems.value
                        val fileUri = Uri.parse(fileUriString)
                        
                        if (currentItems.isEmpty()) {
                            // App was reopened, create item from broadcast
                            val fileInfo = withContext(Dispatchers.IO) {
                                fileAccessRepository.getFileInfoFromUri(fileUri)
                            }
                            val newItem = ConversionItem(
                                uri = fileUri,
                                fileName = fileInfo.first,
                                fileSize = fileInfo.second,
                                format = fileInfo.third,
                                targetFormat = "",
                                progress = progress,
                                status = ConversionStatus.CONVERTING
                            )
                            _convertingItems.value = listOf(newItem)
                        } else {
                            // Update existing item
                            _convertingItems.value = currentItems.map { item ->
                                if (item.uri.toString() == fileUriString) {
                                    item.copy(progress = progress, status = ConversionStatus.CONVERTING)
                                } else {
                                    item
                                }
                            }
                        }
                    }
                }
            }

        private val fileConversionCompleteReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val fileUriString = intent?.getStringExtra(AppConfig.FILE_URI) ?: return
                    val isSuccess = intent.getBooleanExtra(AppConfig.IS_SUCCESS, false)
                    
                    viewModelScope.launch {
                        val currentItems = _convertingItems.value
                        if (currentItems.isEmpty()) return@launch
                        
                        // Find the matching item by URI, or fall back to first converting item
                        val completedIndex = currentItems.indexOfFirst { it.uri.toString() == fileUriString }
                            .takeIf { it >= 0 } ?: currentItems.indexOfFirst { it.status == ConversionStatus.CONVERTING }
                            .takeIf { it >= 0 } ?: 0
                        
                        // Mark item as completed to trigger exit animation
                        val updatedItems = currentItems.mapIndexed { index, item ->
                            if (index == completedIndex) {
                                item.copy(status = if (isSuccess) ConversionStatus.COMPLETED else ConversionStatus.FAILED)
                            } else {
                                item
                            }
                        }
                        _convertingItems.value = updatedItems
                        
                        // Wait for animation, then remove the specific completed item
                        delay(AppConfig.ANIMATION_DURATION_MS)
                        _convertingItems.value = _convertingItems.value.filterIndexed { index, _ -> index != completedIndex }
                    }
                }
            }

        private val conversionStateReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val uriStrings = intent?.getStringArrayListExtra(AppConfig.PENDING_URIS) ?: return
                    val targetFormat = intent.getStringExtra(AppConfig.TARGET_FORMAT) ?: ""
                    
                    viewModelScope.launch {
                        val uris = uriStrings.map { Uri.parse(it) }
                        val items = withContext(Dispatchers.IO) {
                            uris.mapIndexed { index, uri ->
                                val fileInfo = fileAccessRepository.getFileInfoFromUri(uri)
                                ConversionItem(
                                    uri = uri,
                                    fileName = fileInfo.first,
                                    fileSize = fileInfo.second,
                                    format = fileInfo.third,
                                    targetFormat = targetFormat,
                                    progress = 0f,
                                    status = if (index == 0) ConversionStatus.CONVERTING else ConversionStatus.PENDING
                                )
                            }
                        }
                        _convertingItems.value = items
                        Log.d(TAG, "Restored ${items.size} items from service state")
                    }
                }
            }

        fun resetConversionStatus() {
            viewModelScope.launch {
                _conversionStatus.value = null
            }
        }

        init {
            startListeningForBroadcasts()
            requestServiceState()
        }

        private fun requestServiceState() {
            if (ConvertItService.isForegroundServiceStarted) {
                _isConversionInProgress.value = true
                val intent = Intent(application, ConvertItService::class.java).apply {
                    action = AppConfig.ACTION_REQUEST_STATE
                }
                application.startService(intent)
            }
        }

        fun updateMetadataUri(intent: Intent?) {
            viewModelScope.launch {
                intent?.let {
                    it.data?.let { uri ->
                        takePersistentUriPermissions(listOf(uri))
                        _metadataUri.emit(uri)
                    }
                }
            }
        }

        fun setMetadataUri(uri: Uri?) {
            viewModelScope.launch {
                _metadataUri.emit(uri)
            }
        }

        fun updateUriList(intent: Intent?) {
            viewModelScope.launch {
                intent?.let {
                    val uris = AppUtil.getUriListFromIntent(it)
                    if (uris.isNotEmpty()) {
                        takePersistentUriPermissions(uris)
                        val updatedList = ArrayList(_uriList.value).apply { addAll(uris) }
                        _uriList.value = updatedList
                    }
                }
            }
        }

        fun clearOpenConversionSheetAfterShare() {
            _openConversionSheetAfterShare.value = false
        }

        /** Replace home URIs and open conversion sheet (Open with / Share into app). */
        fun applySharedUrisForConversion(uris: List<Uri>) {
            viewModelScope.launch {
                if (uris.isEmpty()) return@launch
                takePersistentUriPermissions(uris)
                _uriList.value = ArrayList(uris.distinct())
                _openConversionSheetAfterShare.value = true
            }
        }

        private fun takePersistentUriPermissions(uris: List<Uri>) {
            val contentResolver = application.contentResolver
            for (uri in uris) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.w(TAG, "Could not take persistent permission for $uri: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error taking persistent permission for $uri: ${e.message}")
                }
            }
        }

        private fun startListeningForBroadcasts() {
            if (areReceiversRegistered) {
                Log.d(TAG, "Receivers already registered, skipping registration")
                return
            }
            
            val intentFilter = IntentFilter(AppConfig.CONVERT_BROADCAST_ACTION)
            ContextCompat.registerReceiver(
                application,
                conversionStatusReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            val progressIntentFilter = IntentFilter(AppConfig.CONVERT_PROGRESS_ACTION)
            ContextCompat.registerReceiver(
                application,
                conversionProgressReceiver,
                progressIntentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            val fileCompleteIntentFilter = IntentFilter(AppConfig.CONVERT_FILE_COMPLETE_ACTION)
            ContextCompat.registerReceiver(
                application,
                fileConversionCompleteReceiver,
                fileCompleteIntentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            val stateIntentFilter = IntentFilter(AppConfig.CONVERT_STATE_ACTION)
            ContextCompat.registerReceiver(
                application,
                conversionStateReceiver,
                stateIntentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            
            areReceiversRegistered = true
            Log.d(TAG, "Broadcast receivers registered successfully")
        }

        fun clearUriList() {
            viewModelScope.launch {
                _uriList.value = ArrayList()
                _totalMediaDurationMs.value = -1L
            }
        }

        /** Sums per-file durations for batch size/time estimates. */
        fun loadMediaDurationsForConversionDialog(uris: List<Uri>) {
            viewModelScope.launch {
                if (uris.isEmpty()) {
                    _totalMediaDurationMs.value = 0L
                    return@launch
                }
                _totalMediaDurationMs.value = -1L
                var sum = 0L
                for (u in uris) {
                    sum += audioConverterRepository.getMediaDuration(u)
                }
                _totalMediaDurationMs.value = sum
                Log.d(TAG, "Loaded total media duration for estimate: $sum ms (${uris.size} files)")
            }
        }

        fun getFileFromUri(uri: Uri): File? = fileAccessRepository.getFileFromUri(uri)

        fun getReadableFileSize(file: File): String = fileAccessRepository.getReadableFileSize(file)
        
        fun getFileInfoFromUri(uri: Uri): Triple<String, String, String> = fileAccessRepository.getFileInfoFromUri(uri)

        private suspend fun createConversionItems(uris: List<Uri>, targetFormat: String): List<ConversionItem> {
            return uris.map { uri ->
                val fileInfo = fileAccessRepository.getFileInfoFromUri(uri)
                ConversionItem(
                    uri = uri,
                    fileName = fileInfo.first,
                    fileSize = fileInfo.second,
                    format = fileInfo.third,
                    targetFormat = targetFormat,
                    progress = 0f,
                    status = ConversionStatus.PENDING
                )
            }
        }

        fun startConversion(
            speed: String,
            uris: ArrayList<Uri>,
            bitrate: String,
            format: String,
            sampleRate: String,
        ) {
            viewModelScope.launch {
                _isConversionInProgress.value = true
                _conversionProgress.value = 0
                _convertingItems.value = createConversionItems(uris.toList(), format)

                val audioFormat = AudioFormat.fromExtension(format)
                val audioBitrate = AudioBitrate.fromBitrate(bitrate)
                val audioSampleRate = AudioSampleRate.fromHz(sampleRate)

                startAudioConversion(
                    customSaveUri = null,
                    playbackSpeed = speed,
                    uris = uris.toList(),
                    outputFormat = audioFormat,
                    bitrate = audioBitrate,
                    sampleRate = audioSampleRate,
                    onSuccess = { convertedFiles ->
                    },
                    onFailure = { error ->
                    },
                    onProgress = { progress ->
                        _conversionProgress.value = progress
                    },
                )
            }
        }

        fun startConversionWithCue(
            speed: String,
            audioUri: Uri,
            cueUri: Uri,
            bitrate: String,
            format: String,
            sampleRate: String,
        ) {
            viewModelScope.launch {
                _isConversionInProgress.value = true
                _conversionProgress.value = 0
                _convertingItems.value = createConversionItems(listOf(audioUri), format)

                startCueConversionService(arrayListOf(audioUri), bitrate, speed, format, cueUri, sampleRate)
            }
        }

        private fun startCueConversionService(
            uris: ArrayList<Uri>,
            bitrate: String,
            playbackSpeed: String,
            outputFormat: String,
            cueUri: Uri,
            sampleRate: String,
        ) {
            Log.d(
                TAG,
                "Starting CUE conversion service with the following details:\n" + 
                "URI List Size: ${uris.size}\n" +
                "Bitrate: $bitrate\n" +
                "Format: $outputFormat\n" +
                "Sample Rate: $sampleRate\n" +
                "CUE URI: ${cueUri.lastPathSegment ?: "unknown"}"
            )

            val intent =
                Intent(application, ConvertItService::class.java).apply {
                    putParcelableArrayListExtra(AppConfig.URI_LIST, uris)
                    putExtra(AppConfig.BITRATE, bitrate)
                    putExtra(AppConfig.AUDIO_PLAYBACK_SPEED, playbackSpeed)
                    putExtra(AppConfig.AUDIO_FORMAT, outputFormat)
                    putExtra(AppConfig.CUE_URI, cueUri)
                    putExtra(AppConfig.AUDIO_SAMPLE_RATE, sampleRate)
                }

            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.N_MR1) {
                Log.d(TAG, "Starting foreground service for CUE conversion...")
                application.startForegroundService(intent)
            } else {
                Log.d(TAG, "Starting regular service for CUE conversion...")
                application.startService(intent)
            }
        }

        suspend fun loadMetadata(audioUri: Uri): Metadata = loadMetadata.invoke(audioUri)

        suspend fun saveMetadata(
            audioUri: Uri,
            metadata: Metadata,
        ): Boolean = saveMetadata.invoke(audioUri, metadata)

        suspend fun saveCoverArt(
            audioUri: Uri,
            bitmap: Bitmap?,
        ): Boolean = saveMetadata.invoke(audioUri, bitmap)

        override fun onCleared() {
            super.onCleared()
            if (areReceiversRegistered) {
                try {
                    application.unregisterReceiver(conversionStatusReceiver)
                    application.unregisterReceiver(conversionProgressReceiver)
                    application.unregisterReceiver(fileConversionCompleteReceiver)
                    application.unregisterReceiver(conversionStateReceiver)
                    areReceiversRegistered = false
                    Log.d(TAG, "Broadcast receivers unregistered successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering receivers: ${e.message}")
                }
            }
        }
    }
