package com.nasahacker.convertit.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.nasahacker.convertit.App
import com.nasahacker.convertit.R
import com.nasahacker.convertit.domain.model.AudioBitrate
import com.nasahacker.convertit.domain.model.AudioCodec
import com.nasahacker.convertit.domain.model.AudioFormat
import com.nasahacker.convertit.domain.model.AudioSampleRate
import com.nasahacker.convertit.domain.repository.AudioConverterRepository
import com.nasahacker.convertit.domain.repository.FileAccessRepository
import com.nasahacker.convertit.service.ConvertItService
import com.nasahacker.convertit.util.AppConfig.AUDIO_FORMAT
import com.nasahacker.convertit.util.AppConfig.AUDIO_PLAYBACK_SPEED
import com.nasahacker.convertit.util.AppConfig.BITRATE
import com.nasahacker.convertit.util.AppConfig.URI_LIST
import com.nasahacker.convertit.util.CueParser
import com.nasahacker.convertit.util.StorageUriUtils
import com.nasahacker.convertit.domain.model.CueFile
import com.nasahacker.convertit.domain.model.CueTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.io.inputStream

class AudioConverterRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val fileAccessRepository: FileAccessRepository,
    ) : AudioConverterRepository {
        val TAG = "Audio"

        override suspend fun getMediaDuration(uri: Uri): Long =
            withContext(Dispatchers.IO) {
                // Fast path: MediaMetadataRetriever reads metadata directly from
                // the content URI without copying the whole stream. For multi-
                // hundred-MB videos this is the difference between "instant"
                // and "spins for 30+ seconds while we copy bytes to cache".
                durationMsFromMediaRetriever(uri)?.let { return@withContext it }

                // Fallback: only when MediaMetadataRetriever can't read the URI
                // (rare formats, broken providers) do we pay the copy cost and
                // hand off to FFprobeKit, which handles a wider format range.
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val tempFile = File(context.cacheDir, "temp_duration_${System.currentTimeMillis()}")
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        try {
                            durationMsFromFilePath(tempFile.absolutePath)
                        } finally {
                            tempFile.delete()
                        }
                    } ?: 0L
                } catch (e: Exception) {
                    Log.e(TAG, "getMediaDuration: ${e.message}", e)
                    0L
                }
            }

        /**
         * Probe duration via [MediaMetadataRetriever] straight from the URI.
         * Returns `null` if the retriever couldn't open the source or read a
         * valid duration, so the caller can fall back to FFprobeKit.
         */
        private fun durationMsFromMediaRetriever(uri: Uri): Long? {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri)
                retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
            } catch (e: Exception) {
                Log.d(TAG, "MediaMetadataRetriever couldn't read $uri: ${e.message}")
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

        override suspend fun convertAudio(
            customSaveUri: Uri?,
            playbackSpeed: String,
            uris: List<Uri>,
            outputFormat: AudioFormat,
            bitrate: AudioBitrate,
            sampleRate: AudioSampleRate,
            onSuccess: (List<String>) -> Unit,
            onFailure: (String) -> Unit,
            onProgress: (Int) -> Unit,
        ) {
            startConversionService(uris, bitrate, playbackSpeed, outputFormat, sampleRate)
        }

        private fun startConversionService(
            uris: List<Uri>,
            bitrate: AudioBitrate,
            playbackSpeed: String,
            outputFormat: AudioFormat,
            sampleRate: AudioSampleRate,
        ) {
            Log.d(
                TAG,
                "Starting audio conversion service with the following details:\n" + "URI List Size: ${uris.size}\n" +
                    "Bitrate: ${bitrate.bitrate}\n" +
                    "Format: ${outputFormat.extension}\n" +
                    "Sample Rate: ${sampleRate.hz}",
            )

            val intent =
                Intent(App.application, ConvertItService::class.java).apply {
                    putParcelableArrayListExtra(URI_LIST, ArrayList(uris))
                    putExtra(BITRATE, bitrate.bitrate)
                    putExtra(AUDIO_PLAYBACK_SPEED, playbackSpeed)
                    putExtra(AUDIO_FORMAT, outputFormat.extension)
                    putExtra(com.nasahacker.convertit.util.AppConfig.AUDIO_SAMPLE_RATE, sampleRate.hz)
                }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                Log.d(TAG, "Starting foreground service...")
                App.application.startForegroundService(intent)
            } else {
                Log.d(TAG, "Starting regular service...")
                App.application.startService(intent)
            }
        }

        override suspend fun performConversion(
            customSaveUri: Uri?,
            playbackSpeed: String,
            uris: List<Uri>,
            outputFormat: AudioFormat,
            bitrate: AudioBitrate,
            sampleRate: AudioSampleRate,
            onSuccess: (List<String>) -> Unit,
            onFailure: (String) -> Unit,
            onProgress: (Int) -> Unit,
            onFileStart: (Uri, Int, Int) -> Unit,
            onFileProgress: (Uri, Float) -> Unit,
            onFileComplete: (Uri, Boolean) -> Unit,
        ) {

            for (uri in uris) {
                val fileName = getFileName(context.contentResolver, uri)
                val isFlacOrWav = fileName.endsWith(".flac", ignoreCase = true) || 
                                fileName.endsWith(".wav", ignoreCase = true)
                
                if (isFlacOrWav) {
                    val cueFile = findCueFileForUri(uri)
                    if (cueFile != null) {
                        Log.d(TAG, "Found CUE file for $fileName, using cue-based splitting")
                        convertWithCueSplitting(customSaveUri, playbackSpeed, uri, outputFormat, bitrate, sampleRate, onSuccess, onFailure, onProgress)
                        return
                    }
                }
            }
            

            val musicDir = fileAccessRepository.getOutputDirectory(customSaveUri)
            val outputPaths = mutableListOf<String>()
            val totalFiles = uris.size
            var processedFiles = 0
            var currentFileIndex = 0

            onProgress(0)

            for ((index, uri) in uris.withIndex()) {
                currentFileIndex = index
                onFileStart(uri, index, totalFiles)
                
                val inputFileName = getFileName(context.contentResolver, uri)
                val inputFileNameWithoutExtension = inputFileName.substringBeforeLast(".")

                var outputFileName = "${inputFileNameWithoutExtension}${outputFormat.extension}"
                var outputFilePath = File(musicDir, outputFileName).absolutePath

                var counter = 1
                while (
                    File(outputFilePath).exists() ||
                    (
                        customSaveUri != null &&
                            StorageUriUtils.isDocumentTreeUri(customSaveUri) &&
                            documentDisplayNameExistsInTree(customSaveUri, outputFileName)
                    )
                ) {
                    outputFileName =
                        "$inputFileNameWithoutExtension($counter)${outputFormat.extension}"
                    outputFilePath = File(musicDir, outputFileName).absolutePath
                    counter++
                }

                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }

                        val baseProgress = ((processedFiles.toFloat() / totalFiles) * 100).toInt()
                        onProgress(baseProgress)

                        val mediaDuration = durationMsFromFilePath(tempFile.absolutePath)

                        val coverArtPath = extractCoverArt(tempFile.absolutePath)

                        val ffmpegArgs = buildFFmpegArgs(
                            tempFile.absolutePath,
                            outputFilePath,
                            outputFormat,
                            bitrate,
                            playbackSpeed,
                            sampleRate,
                            coverArtPath,
                        )
                        
                        val sessionCompleted = CompletableDeferred<Boolean>()
                        val currentUri = uri
                        val finalOutputPath = outputFilePath
                        
                        val coverArtPathFinal = coverArtPath

                        FFmpegKit.executeWithArgumentsAsync(
                            ffmpegArgs,
                            { session ->
                                tempFile.delete()
                                coverArtPathFinal?.let { File(it).delete() }

                                if (ReturnCode.isSuccess(session.returnCode)) {
                                    val recordedOutput =
                                        if (StorageUriUtils.isDocumentTreeUri(customSaveUri)) {
                                            val treeUri = customSaveUri!!
                                            val committed =
                                                commitStagingFileToDocumentTree(
                                                    treeUri,
                                                    File(finalOutputPath),
                                                    outputFormat,
                                                )
                                            File(finalOutputPath).delete()
                                            if (committed == null) {
                                                onFileComplete(currentUri, false)
                                                onFailure(
                                                    context.getString(
                                                        R.string.label_conversion_failed_for_file_with_return_code,
                                                        inputFileName,
                                                        "save",
                                                    ),
                                                )
                                                sessionCompleted.complete(false)
                                                return@executeWithArgumentsAsync
                                            }
                                            committed.toString()
                                        } else {
                                            finalOutputPath
                                        }
                                    outputPaths.add(recordedOutput)
                                    processedFiles++
                                    onFileComplete(currentUri, true)

                                    val progress = ((processedFiles.toFloat() / totalFiles) * 100).toInt()
                                    onProgress(progress)
                                    sessionCompleted.complete(true)
                                } else {
                                    onFileComplete(currentUri, false)
                                    onFailure(
                                        context.getString(
                                            R.string.label_conversion_failed_for_file_with_return_code,
                                            inputFileName,
                                            session.returnCode.toString(),
                                        ),
                                    )
                                    sessionCompleted.complete(false)
                                }
                            },
                            null,
                            { statistics ->
                                if (statistics.time > 0 && mediaDuration > 0) {
                                    val fileProgress = (statistics.time.toFloat() / mediaDuration).coerceIn(0f, 1f)
                                    onFileProgress(currentUri, fileProgress)
                                    
                                    val fileProgressPercent = (fileProgress * 100).toInt()
                                    val totalProgress =
                                        baseProgress + ((fileProgressPercent * (100 / totalFiles)) / 100)
                                    onProgress(minOf(totalProgress, 99))
                                }
                            },
                        )
                        
                        val success = sessionCompleted.await()
                        if (!success) {
                            return
                        }
                    } ?: throw Exception("Failed to open input stream")
                } catch (e: Exception) {
                    onFileComplete(uri, false)
                    onFailure(
                        context.getString(
                            R.string.label_conversion_failed_for_file_with_return_code,
                            inputFileName,
                            e.message ?: "Unknown error",
                        ),
                    )
                    return
                }
            }
            
            onSuccess(outputPaths)
        }

        private fun buildFFmpegArgs(
            inputPath: String,
            outputPath: String,
            outputFormat: AudioFormat,
            bitrate: AudioBitrate,
            playbackSpeed: String,
            sampleRate: AudioSampleRate,
            coverArtPath: String? = null,
        ): Array<String> {
            Log.d(TAG, "Building FFmpeg args for format: ${outputFormat.extension}, bitrate: ${bitrate.bitrate}, sr: ${sampleRate.hz}, hasCover: ${coverArtPath != null}")

            val args = mutableListOf<String>()
            args.addAll(listOf("-y", "-i", inputPath))

            if (coverArtPath != null) {
                args.addAll(listOf("-i", coverArtPath))
            }

            if (coverArtPath != null) {
                args.addAll(listOf("-map", "0:a"))
                args.addAll(listOf("-map", "1:v"))
                args.addAll(listOf("-c:v", "mjpeg"))
                args.addAll(listOf("-disposition:v", "attached_pic"))
            } else {
                args.addAll(listOf("-map", "0:a"))
                args.addAll(listOf("-vn"))
            }

            args.addAll(listOf("-map_metadata", "0"))

            when (outputFormat) {
                AudioFormat.AMR_WB -> {
                    args.addAll(listOf("-ar", "16000", "-ac", "1"))
                }
                AudioFormat.OPUS -> {
                    args.addAll(listOf("-ar", sampleRate.hz))
                    if (bitrate.bitrate.replace("k", "").toIntOrNull()?.let { it <= 48 } == true) {
                        args.addAll(listOf("-application", "voip"))
                    }
                }
                else -> {
                    args.addAll(listOf("-ar", sampleRate.hz))
                }
            }

            args.addAll(listOf("-c:a", AudioCodec.fromFormat(outputFormat).codec))
            args.addAll(listOf("-b:a", bitrate.bitrate))

            if (playbackSpeed != "1.0") {
                args.addAll(listOf("-filter:a", "atempo=$playbackSpeed"))
            }

            args.add(outputPath)
            return args.toTypedArray()
        }

        private fun durationMsFromFilePath(filePath: String): Long =
            try {
                val session = FFprobeKit.getMediaInformation(filePath)
                val mediaInformation = session.mediaInformation
                if (mediaInformation != null) {
                    val duration = mediaInformation.duration
                    if (duration != null) {
                        (duration.toDouble() * 1000).toLong()
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } catch (e: Exception) {
                Log.e("AudioConverter", "Error getting audio duration: ${e.message}")
                0L
            }

        private fun extractCoverArt(inputPath: String): String? {
            return try {
                val outputPath = File(context.cacheDir, "cover_${System.currentTimeMillis()}.jpg").absolutePath
                val extractArgs = arrayOf(
                    "-i", inputPath,
                    "-an",
                    "-vcodec",
                    "copy",
                    "-y",
                    outputPath
                )
                val session = FFmpegKit.executeWithArguments(extractArgs)
                if (ReturnCode.isSuccess(session.returnCode) && File(outputPath).exists()) {
                    outputPath
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.d(TAG, "No cover art found or error extracting: ${e.message}")
                null
            }
        }

        override suspend fun convertWithCueSplitting(
            customSaveUri: Uri?,
            playbackSpeed: String,
            uri: Uri,
            outputFormat: AudioFormat,
            bitrate: AudioBitrate,
            sampleRate: AudioSampleRate,
            onSuccess: (List<String>) -> Unit,
            onFailure: (String) -> Unit,
            onProgress: (Int) -> Unit,
        ) {
            val musicDir = fileAccessRepository.getOutputDirectory(customSaveUri)
            val outputPaths = mutableListOf<String>()
            
            onProgress(0)
            
            try {

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val tempAudioFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}")
                    FileOutputStream(tempAudioFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    val coverArtPath = extractCoverArt(tempAudioFile.absolutePath)

                    val cueFile = findCueFileForUri(uri)
                    if (cueFile == null) {

                        Log.d(TAG, "No CUE file found, performing regular conversion")
                        performConversion(customSaveUri, playbackSpeed, listOf(uri), outputFormat, bitrate, sampleRate, onSuccess, onFailure, onProgress)
                        tempAudioFile.delete()
                        return
                    }
                    
                    val parsedCue = CueParser.parseCueFile(cueFile)
                    if (parsedCue == null || !parsedCue.hasValidTracks()) {
                        Log.w(TAG, "Invalid or empty CUE file, performing regular conversion")
                        performConversion(customSaveUri, playbackSpeed, listOf(uri), outputFormat, bitrate, sampleRate, onSuccess, onFailure, onProgress)
                        tempAudioFile.delete()
                        return
                    }
                    
                    Log.d(TAG, "Found CUE file with ${parsedCue.tracks.size} tracks")
                    
                    val tracks = parsedCue.getTracksWithEndTimes()
                    val totalTracks = tracks.size
                    var processedTracks = 0
                    

                    for ((index, track) in tracks.withIndex()) {
                        val baseProgress = ((processedTracks.toFloat() / totalTracks) * 100).toInt()
                        onProgress(baseProgress)
                        
                        val trackFileName = sanitizeFileName("${track.trackNumber.toString().padStart(2, '0')} - ${track.title}")
                        var outputFileName = "$trackFileName${outputFormat.extension}"
                        var outputFilePath = File(musicDir, outputFileName).absolutePath
                        

                        var counter = 1
                        while (
                            File(outputFilePath).exists() ||
                                (
                                    customSaveUri != null &&
                                        StorageUriUtils.isDocumentTreeUri(customSaveUri) &&
                                        documentDisplayNameExistsInTree(customSaveUri, outputFileName)
                                )
                        ) {
                            outputFileName = "$trackFileName($counter)${outputFormat.extension}"
                            outputFilePath = File(musicDir, outputFileName).absolutePath
                            counter++
                        }
                        
                        val ffmpegArgs = buildFFmpegArgsForTrack(
                            tempAudioFile.absolutePath,
                            outputFilePath,
                            outputFormat,
                            bitrate,
                            playbackSpeed,
                            track,
                            sampleRate,
                            coverArtPath,
                        )
                        
                        try {
                            val sessionCompleted = CompletableDeferred<Boolean>()
                            
                            FFmpegKit.executeWithArgumentsAsync(ffmpegArgs, { session ->
                                if (ReturnCode.isSuccess(session.returnCode)) {
                                    val recordedOutput =
                                        if (StorageUriUtils.isDocumentTreeUri(customSaveUri)) {
                                            val treeUri = customSaveUri!!
                                            val committed =
                                                commitStagingFileToDocumentTree(
                                                    treeUri,
                                                    File(outputFilePath),
                                                    outputFormat,
                                                )
                                            File(outputFilePath).delete()
                                            if (committed == null) {
                                                sessionCompleted.complete(false)
                                                return@executeWithArgumentsAsync
                                            }
                                            committed.toString()
                                        } else {
                                            outputFilePath
                                        }
                                    outputPaths.add(recordedOutput)
                                    processedTracks++
                                    
                                    val progress = ((processedTracks.toFloat() / totalTracks) * 100).toInt()
                                    onProgress(progress)
                                    
                                    Log.d(TAG, "Successfully converted track ${track.trackNumber}: ${track.title}")
                                    sessionCompleted.complete(true)
                                } else {
                                    Log.e(TAG, "Failed to convert track ${track.trackNumber}: ${session.failStackTrace}")
                                    sessionCompleted.complete(false)
                                }
                            })
                            
                            val success = sessionCompleted.await()
                            if (!success) {
                                onFailure(
                                    if (StorageUriUtils.isDocumentTreeUri(customSaveUri)) {
                                        "Failed to save track ${track.trackNumber} to the selected folder"
                                    } else {
                                        "Failed to convert track ${track.trackNumber}: ${track.title}"
                                    },
                                )
                                tempAudioFile.delete()
                                return
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting track ${track.trackNumber}: ${e.message}")
                            onFailure("Error converting track ${track.trackNumber}: ${e.message}")
                            tempAudioFile.delete()
                            return
                        }
                    }
                    
                    tempAudioFile.delete()
                    coverArtPath?.let { File(it).delete() }

                    val cueFileName = cueFile.name
                    if (cueFileName.endsWith("_embedded.cue")) {
                        cueFile.delete()
                    }
                    
                    onSuccess(outputPaths)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in CUE-based conversion: ${e.message}")
                onFailure("Error in CUE-based conversion: ${e.message}")
            }
        }
        
        private fun findCueFileForUri(audioUri: Uri): File? {
            try {
                val fileName = getFileName(context.contentResolver, audioUri)
                val isFlac = fileName.endsWith(".flac", ignoreCase = true)
                
                // Only check for embedded CUE in FLAC files since external CUE files are selected via file picker
                if (isFlac) {
                    context.contentResolver.openInputStream(audioUri)?.use { inputStream ->
                        val tempFlacFile = File(context.cacheDir, "temp_flac_${System.currentTimeMillis()}.flac")
                        FileOutputStream(tempFlacFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        
                        val embeddedCue = CueParser.extractEmbeddedCueFromFlac(tempFlacFile)
                        tempFlacFile.delete()
                        
                        if (embeddedCue != null) {
                            Log.d(TAG, "Found embedded CUE sheet in FLAC file")
                            return embeddedCue
                        }
                    }
                }
                
                return null
            } catch (e: Exception) {
                Log.e(TAG, "Error finding CUE file: ${e.message}")
                return null
            }
        }
        
        private fun buildFFmpegArgsForTrack(
            inputPath: String,
            outputPath: String,
            outputFormat: AudioFormat,
            bitrate: AudioBitrate,
            playbackSpeed: String,
            track: CueTrack,
            sampleRate: AudioSampleRate,
            coverArtPath: String? = null,
        ): Array<String> {
            val args = mutableListOf<String>()

            args.addAll(arrayOf("-y", "-i", inputPath))

            if (coverArtPath != null) {
                args.addAll(arrayOf("-i", coverArtPath))
            }

            if (coverArtPath != null) {
                args.addAll(arrayOf("-map", "0:a"))
                args.addAll(arrayOf("-map", "1:v"))
                args.addAll(arrayOf("-c:v", "mjpeg"))
                args.addAll(arrayOf("-disposition:v", "attached_pic"))
            } else {
                args.addAll(arrayOf("-map", "0:a", "-vn"))
            }

            args.addAll(arrayOf("-map_metadata", "0"))

            args.addAll(arrayOf("-ss", CueParser.formatSecondsForFFmpeg(track.startTimeSeconds)))
            

            track.endTimeSeconds?.let { endTime ->
                val duration = endTime - track.startTimeSeconds
                args.addAll(arrayOf("-t", CueParser.formatSecondsForFFmpeg(duration)))
            }
            

            args.addAll(arrayOf("-c:a", AudioCodec.fromFormat(outputFormat).codec))
            args.addAll(arrayOf("-b:a", bitrate.bitrate))
            

            if (playbackSpeed != "1.0") {
                args.addAll(arrayOf("-filter:a", "atempo=$playbackSpeed"))
            }
            

            when (outputFormat) {
                AudioFormat.AMR_WB -> {
                    args.addAll(arrayOf("-ar", "16000", "-ac", "1"))
                }
                AudioFormat.OPUS -> {
                    args.addAll(arrayOf("-ar", sampleRate.hz))
                    if (bitrate.bitrate.replace("k", "").toIntOrNull()?.let { it <= 48 } == true) {
                        args.addAll(arrayOf("-application", "voip"))
                    }
                }
                else -> {
                    args.addAll(arrayOf("-ar", sampleRate.hz))
                }
            }
            
            args.add(outputPath)
            
            Log.d(TAG, "FFmpeg args for track ${track.trackNumber}: ${args.joinToString(" ")}")
            return args.toTypedArray()
        }
        
        private fun sanitizeFileName(fileName: String): String {
            val invalidChars = Regex("[<>:\"/\\\\|?*\\x00-\\x1f]")
            val sanitized = fileName.replace(invalidChars, "_")
                .replace(Regex("\\s+"), " ")
                .trim()
                .trimEnd('.')  
                
            val maxLength = 200
            return if (sanitized.length > maxLength) {
                sanitized.substring(0, maxLength).trimEnd()
            } else {
                sanitized
            }.ifEmpty { "Track" } 
        }

        override suspend fun convertWithManualCue(
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
        ) {
            val musicDir = fileAccessRepository.getOutputDirectory(customSaveUri)
            val outputPaths = mutableListOf<String>()
            
            onProgress(0)
            
            try {
                context.contentResolver.openInputStream(audioUri)?.use { audioInputStream ->
                    val tempAudioFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}")
                    FileOutputStream(tempAudioFile).use { outputStream ->
                        audioInputStream.copyTo(outputStream)
                    }

                    val coverArtPath = extractCoverArt(tempAudioFile.absolutePath)

                    context.contentResolver.openInputStream(cueUri)?.use { cueInputStream ->
                        val cueFileName = getFileName(context.contentResolver, cueUri)
                        if (!cueFileName.lowercase().endsWith(".cue")) {
                            Log.w(TAG, "Selected file is not a CUE file: $cueFileName")
                            onFailure(context.getString(R.string.label_invalid_cue_file))
                            tempAudioFile.delete()
                            return
                        }
                        
                        val parsedCue = CueParser.parseCueFile(cueInputStream)
                        if (parsedCue == null || !parsedCue.hasValidTracks()) {
                            Log.w(TAG, "Invalid or empty CUE file, performing regular conversion")
                            performConversion(customSaveUri, playbackSpeed, listOf(audioUri), outputFormat, bitrate, sampleRate, onSuccess, onFailure, onProgress)
                            tempAudioFile.delete()
                            return
                        }
                        
                        Log.d(TAG, "Manual CUE file with ${parsedCue.tracks.size} tracks")
                        
                        val tracks = parsedCue.getTracksWithEndTimes()
                        val totalTracks = tracks.size
                        var processedTracks = 0
                        
                        for ((index, track) in tracks.withIndex()) {
                            val baseProgress = ((processedTracks.toFloat() / totalTracks) * 100).toInt()
                            onProgress(baseProgress)
                            
                            val trackFileName = sanitizeFileName("${track.trackNumber.toString().padStart(2, '0')} - ${track.title}")
                            var outputFileName = "$trackFileName${outputFormat.extension}"
                            var outputFilePath = File(musicDir, outputFileName).absolutePath
                            
                            Log.d(TAG, "Track ${track.trackNumber}: '$trackFileName' -> '$outputFilePath'")
                            
                            var counter = 1
                            while (
                                File(outputFilePath).exists() ||
                                    (
                                        customSaveUri != null &&
                                            StorageUriUtils.isDocumentTreeUri(customSaveUri) &&
                                            documentDisplayNameExistsInTree(customSaveUri, outputFileName)
                                    )
                            ) {
                                outputFileName = "$trackFileName($counter)${outputFormat.extension}"
                                outputFilePath = File(musicDir, outputFileName).absolutePath
                                counter++
                            }
                            
                            val ffmpegArgs = buildFFmpegArgsForTrack(
                                tempAudioFile.absolutePath,
                                outputFilePath,
                                outputFormat,
                                bitrate,
                                playbackSpeed,
                                track,
                                sampleRate,
                                coverArtPath,
                            )
                            
                            try {
                                Log.d(TAG, "FFmpeg command: ${ffmpegArgs.joinToString(" ")}")
                                val session = FFmpegKit.executeWithArguments(ffmpegArgs)
                                
                                if (ReturnCode.isSuccess(session.returnCode)) {
                                    val recordedOutput =
                                        if (StorageUriUtils.isDocumentTreeUri(customSaveUri)) {
                                            val treeUri = customSaveUri!!
                                            val committed =
                                                commitStagingFileToDocumentTree(
                                                    treeUri,
                                                    File(outputFilePath),
                                                    outputFormat,
                                                )
                                            File(outputFilePath).delete()
                                            if (committed == null) {
                                                onFailure("Failed to save track ${track.trackNumber} to the selected folder")
                                                tempAudioFile.delete()
                                                return
                                            }
                                            committed.toString()
                                        } else {
                                            outputFilePath
                                        }
                                    outputPaths.add(recordedOutput)
                                    processedTracks++
                                    
                                    val progress = ((processedTracks.toFloat() / totalTracks) * 100).toInt()
                                    onProgress(progress)
                                    
                                    Log.d(TAG, "Successfully converted track ${track.trackNumber}: ${track.title}")
                                } else {
                                    Log.e(TAG, "Failed to convert track ${track.trackNumber}: ${session.failStackTrace}")
                                    onFailure("Failed to convert track ${track.trackNumber}: ${track.title}")
                                    tempAudioFile.delete()
                                    coverArtPath?.let { File(it).delete() }
                                    return
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting track ${track.trackNumber}: ${e.message}")
                                onFailure("Error converting track ${track.trackNumber}: ${e.message}")
                                tempAudioFile.delete()
                                coverArtPath?.let { File(it).delete() }
                                return
                            }
                        }

                        tempAudioFile.delete()
                        coverArtPath?.let { File(it).delete() }
                        onSuccess(outputPaths)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in manual CUE-based conversion: ${e.message}")
                onFailure("Error in manual CUE-based conversion: ${e.message}")
            }
        }
        
        private fun documentDisplayNameExistsInTree(
            treeUri: Uri,
            displayName: String,
        ): Boolean {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            return tree.findFile(displayName) != null
        }

        private fun commitStagingFileToDocumentTree(
            treeUri: Uri,
            stagingFile: File,
            outputFormat: AudioFormat,
        ): Uri? {
            if (!stagingFile.exists()) return null
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
            val mime = StorageUriUtils.mimeTypeForAudioFormat(outputFormat)
            val doc = tree.createFile(mime, stagingFile.name) ?: return null
            return try {
                val stream = context.contentResolver.openOutputStream(doc.uri, "w")
                if (stream == null) {
                    doc.delete()
                    null
                } else {
                    stream.use { out ->
                        stagingFile.inputStream().use { it.copyTo(out) }
                    }
                    doc.uri
                }
            } catch (e: Exception) {
                Log.e(TAG, "commitStagingFileToDocumentTree: ${e.message}", e)
                doc.delete()
                null
            }
        }

        private fun getFileName(
            contentResolver: android.content.ContentResolver,
            uri: Uri,
        ): String =
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            } ?: "unknown"
    }
