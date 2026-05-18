package com.nasahacker.convertit.util

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.nasahacker.convertit.domain.model.AudioFormat

object StorageUriUtils {
    fun isDocumentTreeUri(uri: Uri?): Boolean =
        uri != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            DocumentsContract.isTreeUri(uri)

    fun mimeTypeForAudioFormat(format: AudioFormat): String =
        when (format) {
            AudioFormat.MP3 -> "audio/mpeg"
            AudioFormat.M4A, AudioFormat.AAC -> "audio/mp4"
            AudioFormat.FLAC -> "audio/flac"
            AudioFormat.WAV -> "audio/wav"
            AudioFormat.OGG -> "audio/ogg"
            AudioFormat.OPUS -> "audio/opus"
            AudioFormat.AIFF -> "audio/aiff"
            AudioFormat.WMA -> "audio/x-ms-wma"
            AudioFormat.MKA -> "audio/x-matroska"
            AudioFormat.SPX -> "audio/ogg"
            AudioFormat.AMR_WB -> "audio/amr"
            AudioFormat.AVI -> "video/x-msvideo"
        }
}
