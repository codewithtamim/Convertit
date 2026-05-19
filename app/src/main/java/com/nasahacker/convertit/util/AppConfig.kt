package com.nasahacker.convertit.util

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

object AppConfig {
    const val GITHUB_ISSUES_URL = "https://github.com/TheByteArray/ConvertIt/issues"

    // Animation durations
    const val ANIMATION_DURATION_MS = 350L

    const val URI_LIST = "uri_list"
    const val BITRATE = "bitrate"
    const val AUDIO_FORMAT = "audio_format"
    const val AUDIO_PLAYBACK_SPEED = "audio_playback_speed"
    const val CUE_URI = "cue_uri"
    const val AUDIO_SAMPLE_RATE = "audio_sample_rate"
    
    // Broadcast actions
    const val CONVERT_BROADCAST_ACTION = "com.nasahacker.convertit.ACTION_CONVERSION_COMPLETE"
    const val CONVERT_PROGRESS_ACTION = "com.nasahacker.convertit.ACTION_CONVERSION_PROGRESS"
    const val CONVERT_FILE_COMPLETE_ACTION = "com.nasahacker.convertit.ACTION_FILE_CONVERSION_COMPLETE"
    const val CONVERT_STATE_ACTION = "com.nasahacker.convertit.ACTION_CONVERSION_STATE"
    const val ACTION_STOP_SERVICE = "com.nasahacker.convertit.ACTION_STOP_SERVICE"
    const val ACTION_REQUEST_STATE = "com.nasahacker.convertit.ACTION_REQUEST_STATE"
    
    // Broadcast extras
    const val IS_SUCCESS = "isSuccess"
    const val FILE_URI = "file_uri"
    const val FILE_PROGRESS = "file_progress"
    const val CURRENT_FILE_INDEX = "current_file_index"
    const val TOTAL_FILES = "total_files"
    const val PENDING_URIS = "pending_uris"
    const val TARGET_FORMAT = "target_format"
    
    // Notification
    const val CHANNEL_ID = "CONVERT_IT_CHANNEL_ID"
    const val CHANNEL_NAME = "ConvertIt Notifications"
    const val FOLDER_DIR = "ConvertIt"
    
    // Community links
    const val DISCORD_CHANNEL = "https://discord.com/invite/2WCsnpw4et"
    const val GITHUB_PROFILE = "https://github.com/codewithtamim"
    const val GITHUB_PROFILE_MOD = "https://github.com/moontahid"
    const val TELEGRAM_CHANNEL = "https://t.me/thebytearray"
    const val GITHUB_REPO = "https://github.com/TheByteArray/ConvertIt"
}
