package com.nasahacker.convertit.domain.model

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

enum class AudioSampleRate(
    val hz: String,
) {
    SR_8000("8000"),
    SR_12000("12000"),
    SR_16000("16000"),
    SR_22050("22050"),
    SR_24000("24000"),
    SR_32000("32000"),
    SR_44100("44100"),
    SR_48000("48000"),
    SR_88200("88200"),
    SR_96000("96000"),
    SR_192000("192000"),
    ;

    companion object {
        fun fromHz(value: String?): AudioSampleRate =
            entries.find { it.hz.equals(value, ignoreCase = true) } ?: SR_44100
    }
}
