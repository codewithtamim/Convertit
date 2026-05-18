package com.nasahacker.convertit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionEstimatesTest {

    @Test
    fun parseBitrate_k_suffix() {
        assertEquals(256_000L, ConversionEstimates.parseBitrateToBitsPerSecond("256k"))
    }

    @Test
    fun mp3_60s_256k_matches_bitrate_formula() {
        val r =
            ConversionEstimates.compute(
                totalSourceDurationMs = 60_000L,
                selectedFormat = ".mp3",
                selectedBitrate = "256k",
                selectedSampleRate = "44100",
                playbackSpeed = 1f,
            ) as ConversionEstimates.EstimateState.Ready
        val expected = (60.0 * 256_000.0 / 8.0 * 1.05).toLong()
        assertEquals(expected, r.sizeMinBytes)
    }

    @Test
    fun playback_speed_2x_halves_lossy_size() {
        val base =
            ConversionEstimates.compute(
                60_000L,
                ".mp3",
                "256k",
                "44100",
                1f,
            ) as ConversionEstimates.EstimateState.Ready
        val at2x =
            ConversionEstimates.compute(
                60_000L,
                ".mp3",
                "256k",
                "44100",
                2f,
            ) as ConversionEstimates.EstimateState.Ready
        assertEquals(base.sizeMinBytes!! / 2, at2x.sizeMinBytes!!)
    }

    @Test
    fun encode_time_tracks_source_workload_not_sped_up_output_timeline() {
        val at1x =
            ConversionEstimates.compute(60_000L, ".mp3", "256k", "44100", 1f) as
                ConversionEstimates.EstimateState.Ready
        val at2x =
            ConversionEstimates.compute(60_000L, ".mp3", "256k", "44100", 2f) as
                ConversionEstimates.EstimateState.Ready
        assertEquals(at1x.encodeSecondsMin, at2x.encodeSecondsMin)
        assertEquals(at1x.encodeSecondsMax, at2x.encodeSecondsMax)
    }

    @Test
    fun mp3_encode_range_faster_than_realtime_heuristic() {
        val r =
            ConversionEstimates.compute(60_000L, ".mp3", "256k", "44100", 1f) as
                ConversionEstimates.EstimateState.Ready
        assertTrue(r.encodeSecondsMax <= 20L)
        assertTrue(r.encodeSecondsMin >= 1L)
    }

    @Test
    fun wav_pcm_size_stereo() {
        val r =
            ConversionEstimates.compute(
                60_000L,
                ".wav",
                "256k",
                "44100",
                1f,
            ) as ConversionEstimates.EstimateState.Ready
        val expected = (60.0 * 44100 * 2 * 2).toLong()
        assertEquals(expected, r.sizeMinBytes)
    }
}
