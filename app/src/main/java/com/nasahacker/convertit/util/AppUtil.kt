package com.nasahacker.convertit.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

object AppUtil {
    fun receiverFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_EXPORTED
        } else {
            ContextCompat.RECEIVER_NOT_EXPORTED
        }

    fun getUriListFromIntent(intent: Intent): ArrayList<Uri> {
        val uriList = ArrayList<Uri>()
        try {
            intent.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uri ->
                        uriList.add(uri)
                    }
                }
            } ?: intent.data?.let { uriList.add(it) }
        } catch (e: Exception) {
            Log.e("AppUtil", "Error processing intent URIs: ${e.message}")
        }
        return uriList
    }

    /**
     * URIs from Open with / Share (ACTION_VIEW, ACTION_SEND, ACTION_SEND_MULTIPLE) and picker-style clipData.
     */
    fun shareOrViewUrisFromIntent(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val uris = mutableListOf<Uri>()
        try {
            when (intent.action) {
                Intent.ACTION_SEND -> {
                    IntentParcelableExtras.getParcelableUri(intent, Intent.EXTRA_STREAM)?.let { uris.add(it) }
                    if (uris.isEmpty()) intent.data?.let { uris.add(it) }
                    if (uris.isEmpty()) uris.addAll(getUriListFromIntent(intent))
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    IntentParcelableExtras.getParcelableUriArrayList(intent, Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
                    if (uris.isEmpty()) uris.addAll(getUriListFromIntent(intent))
                }
                Intent.ACTION_VIEW -> {
                    intent.data?.let { uris.add(it) }
                }
                else -> uris.addAll(getUriListFromIntent(intent))
            }
        } catch (e: Exception) {
            Log.e("AppUtil", "shareOrViewUrisFromIntent: ${e.message}")
        }
        return uris.distinct()
    }

    fun handleNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101,
            )
        }
    }
}
