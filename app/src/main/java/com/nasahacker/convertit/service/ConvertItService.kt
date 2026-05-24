package com.nasahacker.convertit.service

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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.arthenica.ffmpegkit.FFmpegKit
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.AudioBitrate
import com.nasahacker.convertit.domain.model.AudioFormat
import com.nasahacker.convertit.domain.model.AudioSampleRate
import com.nasahacker.convertit.domain.repository.AppRepository
import com.nasahacker.convertit.domain.repository.AudioConverterRepository
import com.nasahacker.convertit.util.AppConfig.ACTION_CANCEL_FILE
import com.nasahacker.convertit.util.AppConfig.ACTION_REQUEST_STATE
import com.nasahacker.convertit.util.AppConfig.ACTION_STOP_SERVICE
import com.nasahacker.convertit.util.AppConfig.CANCEL_FILE_URI
import com.nasahacker.convertit.util.AppConfig.AUDIO_FORMAT
import com.nasahacker.convertit.util.AppConfig.AUDIO_PLAYBACK_SPEED
import com.nasahacker.convertit.util.AppConfig.BITRATE
import com.nasahacker.convertit.util.AppConfig.CHANNEL_ID
import com.nasahacker.convertit.util.AppConfig.CONVERT_BROADCAST_ACTION
import com.nasahacker.convertit.util.AppConfig.CONVERT_PROGRESS_ACTION
import com.nasahacker.convertit.util.AppConfig.CONVERT_FILE_COMPLETE_ACTION
import com.nasahacker.convertit.util.AppConfig.CONVERT_STATE_ACTION
import com.nasahacker.convertit.util.AppConfig.CUE_URI
import com.nasahacker.convertit.util.AppConfig.CURRENT_FILE_INDEX
import com.nasahacker.convertit.util.AppConfig.FILE_PROGRESS
import com.nasahacker.convertit.util.AppConfig.FILE_URI
import com.nasahacker.convertit.util.AppConfig.IS_SUCCESS
import com.nasahacker.convertit.util.AppConfig.PENDING_URIS
import com.nasahacker.convertit.util.AppConfig.TARGET_FORMAT
import com.nasahacker.convertit.util.AppConfig.TOTAL_FILES
import com.nasahacker.convertit.util.AppConfig.URI_LIST
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConvertItService : Service() {
    companion object {
        private const val TAG = "ConvertItService"
        var isForegroundServiceStarted = false
    }

    @Inject
    lateinit var userPrefRepository: AppRepository

    @Inject
    lateinit var audioConverterRepository: AudioConverterRepository

    private val notificationId = 1
    private var conversionJob: Job? = null
    private var pendingUris: List<Uri> = emptyList()
    private var currentTargetFormat: String = ""
    private var currentFileIndex: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service destroyed")

        conversionJob?.cancel()
        conversionJob = null
        pendingUris = emptyList()

        FFmpegKit.cancel()
        Log.i(TAG, "Cancelled all FFmpeg sessions in onDestroy")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created - Process ID: ${android.os.Process.myPid()}")
        startForegroundServiceWithNotification()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.i(
            TAG,
            "onStartCommand: Received intent with action: ${intent?.action}, startId: $startId",
        )

        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.i(TAG, "Stopping service as per user request. startId: $startId")

            conversionJob?.cancel()
            conversionJob = null
            pendingUris = emptyList()

            FFmpegKit.cancel()
            Log.i(TAG, "Cancelled all FFmpeg sessions")

            showCompletionNotification(success = false, cancelled = true)
            broadcastConversionResult(Intent().apply { action = CONVERT_BROADCAST_ACTION }, false)
            stopForegroundService()
            return START_NOT_STICKY
        }
        
        if (intent?.action == ACTION_REQUEST_STATE) {
            Log.i(TAG, "State requested, broadcasting current state")
            broadcastCurrentState()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_CANCEL_FILE) {
            val cancelUri = intent.getStringExtra(CANCEL_FILE_URI)
            if (cancelUri != null) {
                Log.i(TAG, "Cancelling file: $cancelUri")
                audioConverterRepository.cancelFileConversion(cancelUri)
                val cancelledFileUri = Uri.parse(cancelUri)
                broadcastFileComplete(cancelledFileUri, false)
            }
            return START_NOT_STICKY
        }

        val uriList: ArrayList<Uri>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableArrayListExtra(URI_LIST, Uri::class.java)
            } else {
                intent?.getParcelableArrayListExtra(URI_LIST)
            }

        val bitrate = AudioBitrate.fromBitrate(intent?.getStringExtra(BITRATE))
        val format = AudioFormat.fromExtension(intent?.getStringExtra(AUDIO_FORMAT))
        val speed = intent?.getStringExtra(AUDIO_PLAYBACK_SPEED) ?: "1.0"
        val sampleRateHz = intent?.getStringExtra(com.nasahacker.convertit.util.AppConfig.AUDIO_SAMPLE_RATE)
        val sampleRate = AudioSampleRate.fromHz(sampleRateHz)
        val cueUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(CUE_URI, Uri::class.java)
        } else {
            intent?.getParcelableExtra(CUE_URI)
        }

        Log.d(
            TAG,
            """
            Conversion parameters:
            - Format: ${format.extension}
            - Bitrate: ${bitrate.bitrate}
            - Playback Speed: $speed
            - Sample Rate: ${sampleRate.hz}
            - Number of files: ${uriList?.size ?: 0}
            - CUE file: ${cueUri?.lastPathSegment ?: "None"}
            - Files: ${uriList?.joinToString { it.lastPathSegment ?: "unknown" }}
            """.trimIndent(),
        )

        if (uriList.isNullOrEmpty()) {
            Log.e(TAG, "No valid URIs provided. Stopping service. startId: $startId")
            showCompletionNotification(false)
            broadcastConversionResult(Intent().apply { action = CONVERT_BROADCAST_ACTION }, false)
            stopForegroundService()
            return START_NOT_STICKY
        }

            pendingUris = uriList.toList()
            currentTargetFormat = format.extension
            currentFileIndex = 0
        
        conversionJob?.cancel()
        conversionJob = null

        conversionJob =
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    Log.i(
                        TAG,
                        "Starting audio conversion for ${uriList.size} files. startId: $startId"
                    )

                    val customSaveLocation = userPrefRepository.selectedCustomLocation.firstOrNull().orEmpty()
                    Log.d(TAG, "Raw custom save location from preferences: '$customSaveLocation'")
                    
                    val customSaveUri = if (customSaveLocation.isNotBlank()) {
                        customSaveLocation.toUri().also { uri ->
                            Log.d(TAG, "Converted to URI: $uri (scheme: ${uri.scheme}, path: ${uri.path})")
                        }
                    } else {
                        Log.d(TAG, "Custom save location is empty, using null")
                        null
                    }
                    
                    if (cueUri != null && uriList.size == 1) {
                        // CUE-based conversion
                        val firstUri = uriList.firstOrNull()
                        if (firstUri == null) {
                            Log.e(TAG, "URI list became empty during CUE conversion. Aborting.")
                            showCompletionNotification(false)
                            broadcastConversionResult(Intent().apply { action = CONVERT_BROADCAST_ACTION }, false)
                            stopForegroundService()
                            return@launch
                        }
                        Log.i(TAG, "Starting CUE-based conversion with CUE file: ${cueUri.lastPathSegment}")
                        // Broadcast file start for CUE conversion
                        broadcastFileProgress(firstUri, 0f, 0, 1)
                        audioConverterRepository.convertWithManualCue(
                            customSaveUri = customSaveUri,
                            playbackSpeed = speed,
                            audioUri = firstUri,
                            cueUri = cueUri,
                            outputFormat = format,
                            bitrate = bitrate,
                            sampleRate = sampleRate,
                            onSuccess = {
                                Log.i(
                                    TAG,
                                    "CUE-based conversion completed successfully. startId: $startId",
                                )
                                broadcastFileComplete(firstUri, true)
                                showCompletionNotification(true)
                                broadcastConversionResult(
                                    Intent().apply {
                                        action = CONVERT_BROADCAST_ACTION
                                    },
                                    true,
                                )
                                stopForegroundService()
                            },
                            onFailure = { error ->
                                Log.e(TAG, "CUE-based conversion failed with error: $error. startId: $startId")
                                broadcastFileComplete(firstUri, false)
                                showCompletionNotification(false)
                                broadcastConversionResult(
                                    Intent().apply {
                                        action = CONVERT_BROADCAST_ACTION
                                    },
                                    false,
                                )
                                stopForegroundService()
                            },
                            onProgress = { progress ->
                                Log.v(TAG, "CUE conversion progress: $progress%. startId: $startId")
                                updateNotification(progress)
                                broadcastFileProgress(firstUri, progress / 100f, 0, 1)
                            },
                        )
                    } else {
                        // Regular conversion
                        Log.i(TAG, "Starting regular conversion for ${uriList.size} files")
                        audioConverterRepository.performConversion(
                            customSaveUri = customSaveUri,
                            playbackSpeed = speed,
                            uris = uriList,
                            outputFormat = format,
                            bitrate = bitrate,
                            sampleRate = sampleRate,
                            onSuccess = {
                                Log.i(
                                    TAG,
                                    "Conversion completed successfully for all files. startId: $startId",
                                )
                                showCompletionNotification(true)
                                broadcastConversionResult(
                                    Intent().apply {
                                        action = CONVERT_BROADCAST_ACTION
                                    },
                                    true,
                                )
                                stopForegroundService()
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Conversion failed with error: $error. startId: $startId")
                                showCompletionNotification(false)
                                broadcastConversionResult(
                                    Intent().apply {
                                        action = CONVERT_BROADCAST_ACTION
                                    },
                                    false,
                                )
                                stopForegroundService()
                            },
                            onProgress = { progress ->
                                Log.v(TAG, "Conversion progress: $progress%. startId: $startId")
                                updateNotification(progress)
                            },
                            onFileStart = { uri, index, total ->
                                Log.i(TAG, "Starting file ${index + 1}/$total: ${uri.lastPathSegment}")
                                currentFileIndex = index
                                broadcastFileProgress(uri, 0f, index, total)
                            },
                            onFileProgress = { uri, progress ->
                                broadcastFileProgress(uri, progress, currentFileIndex, pendingUris.size)
                            },
                            onFileComplete = { uri, success ->
                                Log.i(TAG, "File completed: ${uri.lastPathSegment}, success: $success")
                                pendingUris = pendingUris.filter { it != uri }
                                broadcastFileComplete(uri, success)
                            },
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during conversion: ${e.message}. startId: $startId")
                    showCompletionNotification(false)
                    broadcastConversionResult(
                        Intent().apply {
                            action = CONVERT_BROADCAST_ACTION
                        },
                        false,
                    )
                    stopForegroundService()
                }
            }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = createNotification("Preparing conversion...", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notification,
            )
        } else {
            startForeground(notificationId, notification)
        }
        isForegroundServiceStarted = true
    }

    private fun createNotification(
        message: String,
        progress: Int,
    ): Notification {
        val stopIntent =
            Intent(this, ConvertItService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle("ConvertIt")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setOngoing(true)
                .setProgress(100, progress, false)
                .addAction(
                    R.drawable.baseline_stop_24,
                    "Stop",
                    stopPendingIntent,
                )

        return builder.build()
    }

    private fun updateNotification(progress: Int) {
        val message = "Converting audio... $progress%"
        val notification = createNotification(message, progress)
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun showCompletionNotification(
        success: Boolean,
        cancelled: Boolean = false,
    ) {
        val message =
            when {
                cancelled -> "Conversion cancelled"
                success -> "Conversion completed successfully"
                else -> "Conversion failed"
            }

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle("ConvertIt")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(if (success) longArrayOf(0, 100, 100, 100) else null)
                .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun broadcastConversionResult(
        intent: Intent,
        success: Boolean,
    ) {
        intent.putExtra(IS_SUCCESS, success)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun broadcastFileProgress(
        uri: Uri,
        progress: Float,
        currentIndex: Int,
        totalFiles: Int,
    ) {
        val intent = Intent(CONVERT_PROGRESS_ACTION).apply {
            putExtra(FILE_URI, uri.toString())
            putExtra(FILE_PROGRESS, progress)
            putExtra(CURRENT_FILE_INDEX, currentIndex)
            putExtra(TOTAL_FILES, totalFiles)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastFileComplete(
        uri: Uri,
        success: Boolean,
    ) {
        val intent = Intent(CONVERT_FILE_COMPLETE_ACTION).apply {
            putExtra(FILE_URI, uri.toString())
            putExtra(IS_SUCCESS, success)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastCurrentState() {
        if (pendingUris.isEmpty()) return
        val uriStrings = ArrayList(pendingUris.map { it.toString() })
        val intent = Intent(CONVERT_STATE_ACTION).apply {
            putStringArrayListExtra(PENDING_URIS, uriStrings)
            putExtra(TARGET_FORMAT, currentTargetFormat)
            putExtra(CURRENT_FILE_INDEX, currentFileIndex)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        Log.i(TAG, "Broadcast current state: ${pendingUris.size} pending, index: $currentFileIndex")
    }

    @SuppressLint("Deprecated")
    private fun stopForegroundService() {
        isForegroundServiceStarted = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }
}
