package com.example.chess.coach

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class CoachVoiceEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        try {
            val appCtx = context.applicationContext
            tts = TextToSpeech(appCtx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val langRes = tts?.setLanguage(Locale.US)
                    tts?.setSpeechRate(1.02f)
                    tts?.setPitch(0.96f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                    _isReady.value = langRes != TextToSpeech.LANG_MISSING_DATA &&
                        langRes != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        } catch (_: Throwable) {
            _isReady.value = false
        }
    }

    fun setSpeechRate(rate: Float) {
        try {
            tts?.setSpeechRate(rate.coerceIn(0.6f, 1.8f))
        } catch (_: Throwable) {
        }
    }

    fun speak(rawText: String, skipFormatting: Boolean = false) {
        if (!_isReady.value || rawText.isBlank()) return
        val spoken = if (skipFormatting) rawText.trim() else formatChessSpeech(rawText)
        try {
            tts?.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "coach_david_${System.currentTimeMillis()}")
        } catch (_: Throwable) {
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }

    companion object {
        fun formatChessSpeech(raw: String): String {
            var s = raw
                .replace("Coach David:", "")
                .replace("O-O-O", "castle queenside")
                .replace("O-O", "castle kingside")
                .replace("#", " checkmate")
                .replace("+", " check")
                .trim()

            // Expand common algebraic piece moves like Nf3, Bxe5, Qxd8 into spoken words
            s = s.replace(Regex("\\b([KQRBN])x([a-h][1-8])\\b")) { m ->
                "${pieceWord(m.groupValues[1])} takes ${m.groupValues[2]}"
            }
            s = s.replace(Regex("\\b([KQRBN])([a-h][1-8])\\b")) { m ->
                "${pieceWord(m.groupValues[1])} ${m.groupValues[2]}"
            }
            s = s.replace(Regex("\\b([a-h])x([a-h][1-8])\\b")) { m ->
                "${m.groupValues[1]} takes ${m.groupValues[2]}"
            }
            return s.replace(Regex("\\s+"), " ").trim()
        }

        private fun pieceWord(symbol: String): String = when (symbol) {
            "K" -> "King"
            "Q" -> "Queen"
            "R" -> "Rook"
            "B" -> "Bishop"
            "N" -> "Knight"
            else -> symbol
        }
    }
}
