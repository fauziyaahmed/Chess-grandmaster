package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    MOVE,
    CAPTURE,
    CHECK,
    CASTLE,
    PROMOTE,
    GAME_END,
    PUZZLE_SUCCESS,
    PUZZLE_FAIL
}

/**
 * Zero-asset low-latency PCM audio synthesizer for tactile chess sound effects.
 */
class ChessSoundPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    fun play(effect: SoundEffect, enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            try {
                val samples = when (effect) {
                    SoundEffect.MOVE -> synthClick(freqHz = 240.0, durationMs = 55, decay = 48.0)
                    SoundEffect.CAPTURE -> synthClick(freqHz = 165.0, durationMs = 80, decay = 36.0)
                    SoundEffect.CASTLE -> synthDoubleClick(220.0, 290.0)
                    SoundEffect.CHECK -> synthTone(freqHz = 660.0, durationMs = 130, decay = 18.0)
                    SoundEffect.PROMOTE -> synthArpeggio(doubleArrayOf(440.0, 554.37, 659.25))
                    SoundEffect.GAME_END -> synthArpeggio(doubleArrayOf(392.0, 493.88, 587.33))
                    SoundEffect.PUZZLE_SUCCESS -> synthArpeggio(doubleArrayOf(523.25, 659.25, 783.99))
                    SoundEffect.PUZZLE_FAIL -> synthTone(freqHz = 185.0, durationMs = 150, decay = 14.0)
                }
                playPcm(samples)
            } catch (_: Throwable) {
                // Ignore audio errors on headless/test devices
            }
        }
    }

    private fun synthClick(freqHz: Double, durationMs: Int, decay: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            val wave = sin(2.0 * PI * freqHz * t) + 0.35 * sin(2.0 * PI * freqHz * 2.1 * t)
            out[i] = (wave * env * 14000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthDoubleClick(f1: Double, f2: Double): ShortArray {
        val a = synthClick(f1, 45, 50.0)
        val gap = ShortArray((sampleRate * 35) / 1000)
        val b = synthClick(f2, 55, 45.0)
        return a + gap + b
    }

    private fun synthTone(freqHz: Double, durationMs: Int, decay: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            val wave = sin(2.0 * PI * freqHz * t)
            out[i] = (wave * env * 12000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthArpeggio(freqs: DoubleArray): ShortArray {
        val seg = (sampleRate * 70) / 1000
        val out = ShortArray(seg * freqs.size)
        freqs.forEachIndexed { idx, f ->
            val tone = synthTone(f, 70, 22.0)
            tone.copyInto(out, destinationOffset = idx * seg)
        }
        return out
    }

    private fun playPcm(samples: ShortArray) {
        val bufferBytes = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes.coerceAtLeast(1024))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        val durationMs = (samples.size * 1000L) / sampleRate + 60L
        Thread.sleep(durationMs)
        track.stop()
        track.release()
    }
}
