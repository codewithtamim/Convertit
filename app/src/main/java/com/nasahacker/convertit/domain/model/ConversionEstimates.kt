package com.nasahacker.convertit.domain.model

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Heuristic conversion output size / encode time for the conversion dialog.
 * Not exact; device CPU and source complexity affect real encode time.
 *
 * Encode wall-clock is approximated as a fraction of **source duration** (like using FFprobe duration with
 * FFmpeg `time=` progress — see
 * https://stackoverflow.com/questions/44543575/ffmpeg-estimated-execution-time). Output size still uses the
 * sped-up output timeline.
 */
object ConversionEstimates {

    private const val LOSSY_CONTAINER_FUDGE = 1.05
    private const val FLAC_MIN_RATIO = 0.38
    private const val FLAC_MAX_RATIO = 0.62

    private val VIDEO_EXT = setOf("mp4", "mkv", "mov", "webm", "avi", "m4v")
    private val PCM_EXT = setOf("wav", "aiff")
    private val LOSSLESS_COMPRESSED_EXT = setOf("flac", "alac")

    sealed class EstimateState {
        data object Unavailable : EstimateState()
        data object Loading : EstimateState()

        data class Ready(
            val sizeMinBytes: Long?,
            val sizeMaxBytes: Long?,
            val encodeSecondsMin: Long,
            val encodeSecondsMax: Long,
            val isVideoContainerOutput: Boolean,
            val isLosslessCompressedRange: Boolean,
        ) : EstimateState()
    }

    fun compute(
        totalSourceDurationMs: Long,
        selectedFormat: String,
        selectedBitrate: String,
        selectedSampleRate: String,
        playbackSpeed: Float,
    ): EstimateState {
        // Sentinel contract:
        //   < 0  -> probe still in flight
        //   = 0  -> probe completed but couldn't determine duration
        //          (e.g. video with no audio stream / unsupported container);
        //          surface as Unavailable so the dialog stops showing a spinner.
        //   > 0  -> use it
        if (totalSourceDurationMs < 0L) return EstimateState.Loading
        if (totalSourceDurationMs == 0L) return EstimateState.Unavailable
        val baseSourceMs = totalSourceDurationMs

        val speed = playbackSpeed.coerceIn(0.25f, 4f)
        val effectiveOutputMs = (baseSourceMs / speed).roundToLong().coerceAtLeast(1L)
        val outputTimelineSec = effectiveOutputMs / 1000.0
        val encodeWorkloadSec = baseSourceMs / 1000.0
        val ext = selectedFormat.trim().removePrefix(".").lowercase()

        return when {
            ext in VIDEO_EXT -> {
                val (encMin, encMax) = encodeTimeRangeSec(encodeWorkloadSec, EncodeTier.Heavy)
                EstimateState.Ready(
                    sizeMinBytes = null,
                    sizeMaxBytes = null,
                    encodeSecondsMin = encMin,
                    encodeSecondsMax = encMax,
                    isVideoContainerOutput = true,
                    isLosslessCompressedRange = false,
                )
            }
            ext in PCM_EXT -> estimatePcm(outputTimelineSec, encodeWorkloadSec, selectedSampleRate)
            ext in LOSSLESS_COMPRESSED_EXT ->
                estimateLosslessCompressed(outputTimelineSec, encodeWorkloadSec, selectedSampleRate)
            else -> estimateLossy(outputTimelineSec, encodeWorkloadSec, selectedBitrate, ext)
        }
    }

    private fun estimatePcm(
        outputTimelineSec: Double,
        encodeWorkloadSec: Double,
        selectedSampleRate: String,
    ): EstimateState {
        val channels = 2
        val sampleRateHz = selectedSampleRate.toIntOrNull()?.coerceAtLeast(8000) ?: 44100
        val bytesPerSample = 2
        val (encMin, encMax) = encodeTimeRangeSec(encodeWorkloadSec, EncodeTier.Fast)
        val bytesPerSecond = sampleRateHz.toLong() * channels * bytesPerSample.toLong()
        val sizeBytes = (outputTimelineSec * bytesPerSecond).roundToLong()
        return EstimateState.Ready(
            sizeMinBytes = sizeBytes,
            sizeMaxBytes = sizeBytes,
            encodeSecondsMin = encMin,
            encodeSecondsMax = encMax,
            isVideoContainerOutput = false,
            isLosslessCompressedRange = false,
        )
    }

    private fun estimateLosslessCompressed(
        outputTimelineSec: Double,
        encodeWorkloadSec: Double,
        selectedSampleRate: String,
    ): EstimateState {
        val channels = 2
        val sampleRateHz = selectedSampleRate.toIntOrNull()?.coerceAtLeast(8000) ?: 44100
        val pcmBytes = outputTimelineSec * sampleRateHz * channels * 2.0
        val minB = (pcmBytes * FLAC_MIN_RATIO).roundToLong()
        val maxB = (pcmBytes * FLAC_MAX_RATIO).roundToLong()
        val (encMin, encMax) = encodeTimeRangeSec(encodeWorkloadSec, EncodeTier.Medium)
        return EstimateState.Ready(
            sizeMinBytes = minB,
            sizeMaxBytes = maxB,
            encodeSecondsMin = encMin,
            encodeSecondsMax = encMax,
            isVideoContainerOutput = false,
            isLosslessCompressedRange = true,
        )
    }

    private fun estimateLossy(
        outputTimelineSec: Double,
        encodeWorkloadSec: Double,
        selectedBitrate: String,
        ext: String,
    ): EstimateState {
        val tier =
            when (ext) {
                "mp3", "aac", "m4a" -> EncodeTier.Medium
                "ogg", "opus", "mka" -> EncodeTier.MediumHeavy
                else -> EncodeTier.Heavy
            }
        val (encMin, encMax) = encodeTimeRangeSec(encodeWorkloadSec, tier)
        val bps = parseBitrateToBitsPerSecond(selectedBitrate)
        if (bps == null) {
            return EstimateState.Ready(
                sizeMinBytes = null,
                sizeMaxBytes = null,
                encodeSecondsMin = encMin,
                encodeSecondsMax = encMax,
                isVideoContainerOutput = false,
                isLosslessCompressedRange = false,
            )
        }
        val bytes = (outputTimelineSec * bps / 8.0 * LOSSY_CONTAINER_FUDGE).roundToLong()
        return EstimateState.Ready(
            sizeMinBytes = bytes,
            sizeMaxBytes = bytes,
            encodeSecondsMin = encMin,
            encodeSecondsMax = encMax,
            isVideoContainerOutput = false,
            isLosslessCompressedRange = false,
        )
    }

    private enum class EncodeTier { Fast, Medium, MediumHeavy, Heavy }

    private fun encodeTimeRangeSec(workloadSec: Double, tier: EncodeTier): Pair<Long, Long> {
        val (low, high) =
            when (tier) {
                EncodeTier.Fast -> 0.018 to 0.09
                EncodeTier.Medium -> 0.035 to 0.22
                EncodeTier.MediumHeavy -> 0.05 to 0.32
                EncodeTier.Heavy -> 0.28 to 1.45
            }
        val minS = max(1L, (workloadSec * low).roundToLong())
        val maxS = max(minS + 1L, (workloadSec * high).roundToLong())
        return minS to maxS
    }

    fun parseBitrateToBitsPerSecond(bitrate: String): Long? {
        val s = bitrate.trim().lowercase()
        if (s.isEmpty()) return null
        return when {
            s.endsWith("k") -> s.dropLast(1).toDoubleOrNull()?.times(1000)?.toLong()
            s.endsWith("m") -> s.dropLast(1).toDoubleOrNull()?.times(1_000_000)?.toLong()
            else -> s.toLongOrNull()
        }
    }

    fun formatByteCountSi(bytes: Long): String {
        if (bytes < 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = bytes.toDouble()
        var u = 0
        while (v >= 1000 && u < units.lastIndex) {
            v /= 1000
            u++
        }
        return if (u == 0) {
            "${bytes} ${units[0]}"
        } else {
            "%.1f %s".format(v, units[u])
        }
    }

    fun formatDurationRange(minSec: Long, maxSec: Long): String {
        if (maxSec <= minSec) return formatDurationSeconds(minSec)
        return "${formatDurationSeconds(minSec)}–${formatDurationSeconds(maxSec)}"
    }

    fun formatDurationSeconds(totalSec: Long): String {
        if (totalSec < 60) return "${totalSec}s"
        val m = totalSec / 60
        val s = totalSec % 60
        return if (s == 0L) "${m} min" else "${m} min ${s}s"
    }
}
