package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect(val displayName: String) {
    MOVE("Piece Move"),
    OPPONENT_MOVE("Opponent Move"),
    CAPTURE("Capture"),
    CASTLE("Castling"),
    CHECK("Check"),
    PROMOTE("Promotion"),
    CHECKMATE("Checkmate"),
    DRAW("Draw / Stalemate"),
    GAME_END("Game Over"),
    PUZZLE_SUCCESS("Puzzle Solved"),
    PUZZLE_FAIL("Mistake / Wrong Move"),
    LOW_TIME("Low Time Alert")
}

enum class SoundPack(val displayName: String, val description: String) {
    CLASSIC_WOOD("Classic Wood", "Warm wooden board knocks & chimes"),
    CRYSTAL_GLASS("Crystal Glass", "Bright crystalline resonant tones"),
    RETRO_ARCADE("Retro Arcade", "Crisp 8-bit chiptune audio")
}

/**
 * Hardware-mixed SoundPool + Procedural PCM Sound Effect Manager for Grandmaster.
 * Plays bundled WAV clips from res/raw via SoundPool when Context is attached on CLASSIC_WOOD,
 * and synthesizes pack-accurate PCM audio clips for Crystal Glass, Retro Arcade, and headless environments.
 */
class SoundEffectManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var soundPool: SoundPool? = null
    private val rawSoundIds = HashMap<Int, Int>()
    private val loadedSampleIds = HashSet<Int>()

    @Volatile
    private var masterVolume: Float = 0.85f

    @Volatile
    private var currentPack: SoundPack = SoundPack.CLASSIC_WOOD

    private val _lastPlayedEffect = MutableStateFlow<SoundEffect?>(null)
    val lastPlayedEffect: StateFlow<SoundEffect?> = _lastPlayedEffect.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<SoundEffect>>(emptyList())
    val recentEvents: StateFlow<List<SoundEffect>> = _recentEvents.asStateFlow()

    fun attachContext(context: Context) {
        if (soundPool != null) return
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build()
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    synchronized(loadedSampleIds) { loadedSampleIds.add(sampleId) }
                }
            }
            val appCtx = context.applicationContext
            val rawResIds = listOf(
                R.raw.sfx_move,
                R.raw.sfx_capture,
                R.raw.sfx_castle,
                R.raw.sfx_check,
                R.raw.sfx_promote,
                R.raw.sfx_checkmate,
                R.raw.sfx_draw,
                R.raw.sfx_fail
            )
            rawResIds.forEach { resId ->
                rawSoundIds[resId] = pool.load(appCtx, resId, 1)
            }
            soundPool = pool
        } catch (_: Throwable) {
            // Safe fallback if SoundPool unavailable in test environment
        }
    }

    fun configure(volume: Float, pack: SoundPack) {
        masterVolume = volume.coerceIn(0f, 1f)
        currentPack = pack
    }

    fun clearRecordedEvents() {
        _lastPlayedEffect.value = null
        _recentEvents.value = emptyList()
    }

    fun preview(effect: SoundEffect) {
        val vol = masterVolume.coerceAtLeast(0.45f)
        recordEvent(effect)
        dispatchPlayback(effect, vol, currentPack)
    }

    fun play(effect: SoundEffect, enabled: Boolean) {
        if (!enabled) return
        val vol = masterVolume
        if (vol <= 0.01f) return
        recordEvent(effect)
        dispatchPlayback(effect, vol, currentPack)
    }

    private fun recordEvent(effect: SoundEffect) {
        _lastPlayedEffect.value = effect
        _recentEvents.update { (it + effect).takeLast(25) }
    }

    private fun dispatchPlayback(effect: SoundEffect, volume: Float, pack: SoundPack) {
        val rawRes = rawResourceFor(effect)
        val pool = soundPool
        val sampleId = if (pack == SoundPack.CLASSIC_WOOD && rawRes != null) rawSoundIds[rawRes] else null
        val isPoolReady = sampleId != null && synchronized(loadedSampleIds) { sampleId in loadedSampleIds }

        if (pool != null && isPoolReady && sampleId != null) {
            val rate = when (effect) {
                SoundEffect.OPPONENT_MOVE -> 0.92f
                SoundEffect.LOW_TIME -> 1.18f
                else -> 1.0f
            }
            try {
                pool.play(sampleId, volume, volume, 1, 0, rate)
                return
            } catch (_: Throwable) {
                // Fall back to PCM synthesis below
            }
        }

        scope.launch {
            try {
                val samples = synthesizeClip(effect, pack, volume)
                playPcm(samples)
            } catch (_: Throwable) {
                // Ignore audio errors on headless/test devices
            }
        }
    }

    fun release() {
        try {
            soundPool?.release()
        } catch (_: Throwable) {
            // Ignore cleanup errors
        } finally {
            soundPool = null
            rawSoundIds.clear()
            synchronized(loadedSampleIds) { loadedSampleIds.clear() }
        }
    }

    private fun rawResourceFor(effect: SoundEffect): Int? = when (effect) {
        SoundEffect.MOVE, SoundEffect.OPPONENT_MOVE -> R.raw.sfx_move
        SoundEffect.CAPTURE -> R.raw.sfx_capture
        SoundEffect.CASTLE -> R.raw.sfx_castle
        SoundEffect.CHECK, SoundEffect.LOW_TIME -> R.raw.sfx_check
        SoundEffect.PROMOTE -> R.raw.sfx_promote
        SoundEffect.CHECKMATE, SoundEffect.PUZZLE_SUCCESS -> R.raw.sfx_checkmate
        SoundEffect.DRAW, SoundEffect.GAME_END -> R.raw.sfx_draw
        SoundEffect.PUZZLE_FAIL -> R.raw.sfx_fail
    }

    private fun playPcm(samples: ShortArray) {
        if (samples.isEmpty()) return
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
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes.coerceAtLeast(1024))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        val durationMs = (samples.size * 1000L) / SAMPLE_RATE + 60L
        Thread.sleep(durationMs)
        track.stop()
        track.release()
    }

    companion object {
        const val SAMPLE_RATE = 22050

        fun synthesizeClip(
            effect: SoundEffect,
            pack: SoundPack = SoundPack.CLASSIC_WOOD,
            volume: Float = 1.0f
        ): ShortArray {
            val vol = volume.coerceIn(0f, 1f).toDouble()
            val raw: ShortArray = when (pack) {
                SoundPack.CLASSIC_WOOD -> when (effect) {
                    SoundEffect.MOVE -> synthWoodKnock(220.0, 60, 46.0)
                    SoundEffect.OPPONENT_MOVE -> synthWoodKnock(190.0, 60, 48.0)
                    SoundEffect.CAPTURE -> synthCaptureImpact(155.0)
                    SoundEffect.CASTLE -> synthDoubleKnock(205.0, 275.0)
                    SoundEffect.CHECK -> synthTone(740.0, 150, 16.0)
                    SoundEffect.PROMOTE -> synthArpeggio(doubleArrayOf(440.0, 554.37, 659.25, 880.0), stepMs = 68, lastMs = 160)
                    SoundEffect.CHECKMATE -> synthArpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), stepMs = 85, lastMs = 280)
                    SoundEffect.DRAW -> synthArpeggio(doubleArrayOf(392.0, 349.23), stepMs = 100, lastMs = 160)
                    SoundEffect.GAME_END -> synthArpeggio(doubleArrayOf(392.0, 493.88, 587.33), stepMs = 80, lastMs = 180)
                    SoundEffect.PUZZLE_SUCCESS -> synthArpeggio(doubleArrayOf(523.25, 659.25, 783.99), stepMs = 75, lastMs = 180)
                    SoundEffect.PUZZLE_FAIL -> synthArpeggio(doubleArrayOf(246.94, 185.0), stepMs = 90, lastMs = 160)
                    SoundEffect.LOW_TIME -> synthTone(880.0, 90, 28.0)
                }

                SoundPack.CRYSTAL_GLASS -> when (effect) {
                    SoundEffect.MOVE -> synthTone(620.0, 65, 32.0)
                    SoundEffect.OPPONENT_MOVE -> synthTone(540.0, 65, 34.0)
                    SoundEffect.CAPTURE -> synthDoubleTone(480.0, 760.0)
                    SoundEffect.CASTLE -> synthDoubleTone(520.0, 680.0)
                    SoundEffect.CHECK -> synthTone(1174.66, 160, 14.0)
                    SoundEffect.PROMOTE -> synthArpeggio(doubleArrayOf(587.33, 739.99, 880.0, 1174.66), stepMs = 65, lastMs = 180)
                    SoundEffect.CHECKMATE -> synthArpeggio(doubleArrayOf(659.25, 830.61, 987.77, 1318.51), stepMs = 85, lastMs = 300)
                    SoundEffect.DRAW -> synthArpeggio(doubleArrayOf(523.25, 493.88), stepMs = 100, lastMs = 170)
                    SoundEffect.GAME_END -> synthArpeggio(doubleArrayOf(493.88, 587.33, 739.99), stepMs = 80, lastMs = 190)
                    SoundEffect.PUZZLE_SUCCESS -> synthArpeggio(doubleArrayOf(659.25, 830.61, 987.77), stepMs = 75, lastMs = 190)
                    SoundEffect.PUZZLE_FAIL -> synthTone(260.0, 155, 14.0)
                    SoundEffect.LOW_TIME -> synthTone(1318.5, 85, 30.0)
                }

                SoundPack.RETRO_ARCADE -> when (effect) {
                    SoundEffect.MOVE -> synthSquare(330.0, 48, 35.0)
                    SoundEffect.OPPONENT_MOVE -> synthSquare(280.0, 48, 35.0)
                    SoundEffect.CAPTURE -> synthSquare(180.0, 85, 22.0)
                    SoundEffect.CASTLE -> synthDoubleSquare(300.0, 420.0)
                    SoundEffect.CHECK -> synthSquare(880.0, 120, 14.0)
                    SoundEffect.PROMOTE -> synthSquareArpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), 65, 160)
                    SoundEffect.CHECKMATE -> synthSquareArpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51), 75, 250)
                    SoundEffect.DRAW -> synthSquareArpeggio(doubleArrayOf(440.0, 392.0), 95, 150)
                    SoundEffect.GAME_END -> synthSquareArpeggio(doubleArrayOf(440.0, 523.25, 659.25), 75, 160)
                    SoundEffect.PUZZLE_SUCCESS -> synthSquareArpeggio(doubleArrayOf(587.33, 739.99, 880.0), 70, 170)
                    SoundEffect.PUZZLE_FAIL -> synthSquareArpeggio(doubleArrayOf(220.0, 164.8), 85, 150)
                    SoundEffect.LOW_TIME -> synthSquare(987.77, 75, 28.0)
                }
            }
            if (vol >= 0.999) return raw
            return ShortArray(raw.size) { idx -> (raw[idx] * vol).toInt().toShort() }
        }

        private fun synthWoodKnock(freqHz: Double, durationMs: Int, decay: Double): ShortArray {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * decay)
                val transient = exp(-t * 210.0) * 0.45 * sin(2.0 * PI * 860.0 * t)
                val body = sin(2.0 * PI * freqHz * t) + 0.42 * sin(2.0 * PI * freqHz * 1.95 * t)
                out[i] = ((body * 0.8 + transient) * env * 18000).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthCaptureImpact(freqHz: Double): ShortArray {
            val count = (SAMPLE_RATE * 92) / 1000
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 34.0)
                val snap = exp(-t * 170.0) * 0.65 * sin(2.0 * PI * 1120.0 * t)
                val body = sin(2.0 * PI * freqHz * t) + 0.5 * sin(2.0 * PI * freqHz * 2.0 * t)
                out[i] = ((body + snap) * env * 20000).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthDoubleKnock(f1: Double, f2: Double): ShortArray {
            val a = synthWoodKnock(f1, 50, 48.0)
            val gap = ShortArray((SAMPLE_RATE * 38) / 1000)
            val b = synthWoodKnock(f2, 58, 42.0)
            return a + gap + b
        }

        private fun synthDoubleTone(f1: Double, f2: Double): ShortArray {
            val a = synthTone(f1, 55, 28.0)
            val gap = ShortArray((SAMPLE_RATE * 25) / 1000)
            val b = synthTone(f2, 70, 24.0)
            return a + gap + b
        }

        private fun synthDoubleSquare(f1: Double, f2: Double): ShortArray {
            val a = synthSquare(f1, 45, 32.0)
            val gap = ShortArray((SAMPLE_RATE * 28) / 1000)
            val b = synthSquare(f2, 55, 28.0)
            return a + gap + b
        }

        private fun synthTone(freqHz: Double, durationMs: Int, decay: Double): ShortArray {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * decay)
                val wave = sin(2.0 * PI * freqHz * t) + 0.25 * sin(2.0 * PI * freqHz * 2.0 * t)
                out[i] = (wave * env * 13500).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthArpeggio(freqs: DoubleArray, stepMs: Int, lastMs: Int): ShortArray {
            val chunks = freqs.mapIndexed { idx, f ->
                val dur = if (idx == freqs.lastIndex) lastMs else stepMs
                val dec = if (idx == freqs.lastIndex) 9.0 else 20.0
                synthTone(f, dur, dec)
            }
            val total = chunks.sumOf { it.size }
            val out = ShortArray(total)
            var offset = 0
            chunks.forEach { chunk ->
                chunk.copyInto(out, offset)
                offset += chunk.size
            }
            return out
        }

        private fun synthSquare(freqHz: Double, durationMs: Int, decay: Double): ShortArray {
            val count = (SAMPLE_RATE * durationMs) / 1000
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * decay)
                val s = if (sin(2.0 * PI * freqHz * t) >= 0.0) 1.0 else -1.0
                out[i] = (s * env * 10500).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            return out
        }

        private fun synthSquareArpeggio(freqs: DoubleArray, stepMs: Int, lastMs: Int): ShortArray {
            val chunks = freqs.mapIndexed { idx, f ->
                val dur = if (idx == freqs.lastIndex) lastMs else stepMs
                val dec = if (idx == freqs.lastIndex) 10.0 else 22.0
                synthSquare(f, dur, dec)
            }
            val total = chunks.sumOf { it.size }
            val out = ShortArray(total)
            var offset = 0
            chunks.forEach { chunk ->
                chunk.copyInto(out, offset)
                offset += chunk.size
            }
            return out
        }
    }
}

typealias ChessSoundPlayer = SoundEffectManager
