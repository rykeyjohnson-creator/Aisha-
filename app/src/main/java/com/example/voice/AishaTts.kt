package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manages Text-To-Speech playback for Aisha.
 * Handles safe initialization, language fallbacks (English & Hindi),
 * utterance lifecycle listeners, and resource release.
 */
class AishaTts(
    context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private val tag = "AishaTts"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private var onSpeechStartListener: (() -> Unit)? = null
    private var onSpeechEndListener: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to instantiate TextToSpeech", e)
            onInitComplete?.invoke(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupProgressListener()
            // Set default voice parameters
            tts?.setPitch(1.05f) // slightly higher, friendly female voice tone
            tts?.setSpeechRate(0.98f) // clear, natural pacing
            configureDefaultLanguage()
            onInitComplete?.invoke(true)
        } else {
            Log.e(tag, "TextToSpeech init failed with status: $status")
            isInitialized = false
            onInitComplete?.invoke(false)
        }
    }

    private fun configureDefaultLanguage() {
        val t = tts ?: return
        val defaultLocale = Locale.US
        val res = t.setLanguage(defaultLocale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to system default locale
            t.setLanguage(Locale.getDefault())
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeechStartListener?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                onSpeechEndListener?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onSpeechEndListener?.invoke()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(tag, "TTS Error code: $errorCode for utterance: $utteranceId")
                onSpeechEndListener?.invoke()
            }
        })
    }

    /**
     * Speaks the given text with optional Hindi language preference.
     * Gracefully falls back to English if Hindi TTS data is missing.
     */
    fun speak(
        text: String,
        preferHindi: Boolean = false,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready to speak")
            onDone?.invoke()
            return
        }

        this.onSpeechStartListener = onStart
        this.onSpeechEndListener = onDone

        try {
            stop()

            if (preferHindi) {
                val hindiLocale = Locale("hi", "IN")
                val availability = tts?.isLanguageAvailable(hindiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = hindiLocale
                } else {
                    configureDefaultLanguage()
                }
            } else {
                configureDefaultLanguage()
            }

            val utteranceId = "aisha_utterance_${System.currentTimeMillis()}"
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                Log.e(tag, "TTS speak failed")
                onDone?.invoke()
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during speak", e)
            onDone?.invoke()
        }
    }

    /**
     * Stops any currently ongoing speech.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping TTS", e)
        }
    }

    /**
     * Releases TextToSpeech resources.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.w(tag, "Error during TTS shutdown", e)
        }
    }
}
