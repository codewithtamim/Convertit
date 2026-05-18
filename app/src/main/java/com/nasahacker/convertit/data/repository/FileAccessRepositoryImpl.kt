package com.nasahacker.convertit.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.AudioFile
import com.nasahacker.convertit.domain.repository.FileAccessRepository
import com.nasahacker.convertit.util.AppConfig.FOLDER_DIR
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

class FileAccessRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FileAccessRepository {
        
        companion object {
            private const val TAG = "FileAccessRepositoryImpl"
        }
        override fun getAudioFilesFromConvertedFolder(customSaveUri: Uri?): List<AudioFile> {
            Log.d(TAG, "getAudioFilesFromConvertedFolder called with URI: $customSaveUri")
            if (customSaveUri != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                DocumentsContract.isTreeUri(customSaveUri)
            ) {
                return getAudioFilesFromDocumentTree(customSaveUri)
            }
            val convertedDir = getOutputDirectory(customSaveUri)
            Log.d(TAG, "Resolved directory: ${convertedDir.absolutePath}")
            Log.d(TAG, "Directory exists: ${convertedDir.exists()}, isDirectory: ${convertedDir.isDirectory}")
            
            val formats = context.resources.getStringArray(R.array.format_array).toList()
            Log.d(TAG, "Looking for formats: $formats")

            val allFiles = convertedDir
                .takeIf { it.exists() && it.isDirectory }
                ?.listFiles()
            
            Log.d(TAG, "Total files in directory: ${allFiles?.size ?: 0}")
            allFiles?.forEach { file ->
                Log.d(TAG, "Found file: ${file.name} (extension: ${file.extension})")
            }

            val audioFiles = allFiles
                ?.filter { file ->
                    val isAudioFile = formats.any { file.extension.equals(it.trimStart('.'), ignoreCase = true) }
                    Log.d(TAG, "File ${file.name} is audio file: $isAudioFile")
                    isAudioFile
                }?.map { file ->
                    AudioFile(
                        name = file.name,
                        size = getReadableFileSize(file),
                        format = file.extension,
                        file = file,
                        contentUri = null,
                    )
                } ?: emptyList()
            
            Log.d(TAG, "Returning ${audioFiles.size} audio files")
            return audioFiles
        }

        override fun getFileFromUri(uri: Uri): File? {
            return try {
                val fileName = getFileName(context, uri)
                if (fileName == null || fileName.isEmpty()) {
                    Log.e(TAG, "Failed to get file name from URI: $uri")
                    return null
                }
                
                val fileSize = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        pfd.statSize
                    } ?: 0L
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get file size: ${e.message}")
                    0L
                }
                
                if (fileSize > 1024 * 1024 * 1024) {
                    Log.e(TAG, "File too large: ${fileSize / (1024 * 1024)} MB")
                    throw Exception("File too large")
                }

                val file = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                } ?: run {
                    Log.e(TAG, "Failed to open input stream for URI: $uri")
                    return null
                }
                file
            } catch (e: Exception) {
                Log.e(TAG, "Error converting URI to File: ${e.message}", e)
                e.printStackTrace()
                null
            }
        }

        override fun getReadableFileSize(file: File): String {
            val sizeInBytes = file.length()
            if (sizeInBytes <= 0) return "0 B"
            val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
            val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
            val size = sizeInBytes / 1024.0.pow(digitGroups.toDouble())
            return String.format(
                context.getString(R.string.label_file_size),
                if (size >= 100) size.toInt().toDouble() else size,
                units[digitGroups],
            )
        }

        private fun getAudioFilesFromDocumentTree(treeUri: Uri): List<AudioFile> {
            val formats =
                context.resources.getStringArray(R.array.format_array).map {
                    it.trimStart('.').lowercase()
                }
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            return tree
                .listFiles()
                .filter { it.isFile }
                .mapNotNull { doc ->
                    val name = doc.name ?: return@mapNotNull null
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (formats.none { it == ext }) return@mapNotNull null
                    val len = doc.length()
                    AudioFile(
                        name = name,
                        size = getReadableFileSizeFromBytes(len),
                        format = ext,
                        file = null,
                        contentUri = doc.uri,
                    )
                }.sortedBy { it.name.lowercase() }
        }

        override fun getOutputDirectory(customSaveUri: Uri?): File {
            Log.d(TAG, "getOutputDirectory called with: $customSaveUri")
            if (customSaveUri != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                DocumentsContract.isTreeUri(customSaveUri)
            ) {
                return File(context.cacheDir, "${FOLDER_DIR}_stage").apply { mkdirs() }
            }
            return if (customSaveUri != null) {
                try {
                    val customPath = customSaveUri.path
                    Log.d(TAG, "Custom URI path: '$customPath'")
                    if (customPath != null && customPath.contains("/tree/primary:")) {
                        val actualPath = customPath.replace("/tree/primary:", "/storage/emulated/0/")
                        Log.d(TAG, "Converted to actual path: '$actualPath'")
                        Log.d(TAG, "Original customPath was: '$customPath'")
                        val customDir = File(actualPath)
                        if (customDir.exists() || customDir.mkdirs()) {
                            if (customDir.canWrite()) {
                                Log.d(TAG, "Using custom directory: ${customDir.absolutePath}")
                                customDir
                            } else {
                                Log.d(TAG, "Custom directory not writable, using default")
                                getDefaultOutputDirectory()
                            }
                        } else {
                            Log.d(TAG, "Failed to create custom directory, using default")
                            getDefaultOutputDirectory()
                        }
                    } else if (customPath != null && customPath.contains("/tree/")) {
                        val actualPath = customPath.replace("/tree/", "/storage/").replace(":", "/")
                        Log.d(TAG, "Alternative path conversion: '$actualPath'")
                        val customDir = File(actualPath)
                        if ((customDir.exists() || customDir.mkdirs()) && customDir.canWrite()) {
                            Log.d(TAG, "Using alternative custom directory: ${customDir.absolutePath}")
                            customDir
                        } else {
                            Log.d(TAG, "Alternative custom directory failed, using default")
                            getDefaultOutputDirectory()
                        }
                    } else {
                        Log.d(TAG, "Custom path doesn't match expected patterns: '$customPath', using default")
                        getDefaultOutputDirectory()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception processing custom URI: ${e.message}")
                    getDefaultOutputDirectory()
                }
            } else {
                Log.d(TAG, "No custom URI provided, using default directory")
                getDefaultOutputDirectory()
            }
        }

        private fun getDefaultOutputDirectory(): File =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                FOLDER_DIR,
            ).apply {
                setReadable(true)
                setWritable(true)
                mkdirs()
            }

        private fun getFileName(
            context: Context,
            uri: Uri,
        ): String? =
            if (uri.scheme == "content") {
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                cursor.getString(nameIndex)
                            } else {
                                uri.lastPathSegment ?: "unknown_file"
                            }
                        } else {
                            uri.lastPathSegment ?: "unknown_file"
                        }
                    } ?: uri.lastPathSegment ?: "unknown_file"
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting file name from content URI: ${e.message}")
                    uri.lastPathSegment ?: "unknown_file"
                }
            } else {
                uri.path?.let { File(it).name } ?: uri.lastPathSegment ?: "unknown_file"
            }
        
        private fun getFileSizeFromUri(uri: Uri): Long {
            return try {
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIndex >= 0) {
                                cursor.getLong(sizeIndex)
                            } else {
                                0L
                            }
                        } else {
                            0L
                        }
                    } ?: 0L
                } else {
                    uri.path?.let { File(it).length() } ?: 0L
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file size from URI: ${e.message}")
                0L
            }
        }
        
        private fun getReadableFileSizeFromBytes(sizeInBytes: Long): String {
            if (sizeInBytes <= 0) return "0 B"
            val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
            val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toInt()
            val size = sizeInBytes / 1024.0.pow(digitGroups.toDouble())
            return String.format(
                context.getString(R.string.label_file_size),
                if (size >= 100) size.toInt().toDouble() else size,
                units[digitGroups],
            )
        }
        
        override fun getFileInfoFromUri(uri: Uri): Triple<String, String, String> {
            return try {
                val fileName = getFileName(context, uri) ?: "Unknown file"
                val fileSize = getFileSizeFromUri(uri)
                val readableSize = getReadableFileSizeFromBytes(fileSize)
                val fileExtension = if (fileName.contains('.')) {
                    fileName.substringAfterLast('.', "unknown")
                } else {
                    "unknown"
                }
                
                Triple(fileName, readableSize, fileExtension)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file info from URI: ${e.message}", e)
                Triple("Unknown file", "0 B", "unknown")
            }
        }

        // maybe we need in future so keep it
//    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String =
//        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
//            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
//        } ?: "unknown"
    }
