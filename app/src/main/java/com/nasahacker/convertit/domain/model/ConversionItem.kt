package com.nasahacker.convertit.domain.model

import android.net.Uri

/**
 * Represents a file being converted with its progress state.
 */
data class ConversionItem(
    val uri: Uri,
    val fileName: String,
    val fileSize: String,
    val format: String,
    val targetFormat: String,
    val progress: Float = 0f,
    val status: ConversionStatus = ConversionStatus.PENDING
)

enum class ConversionStatus {
    PENDING,
    CONVERTING,
    COMPLETED,
    FAILED
}
