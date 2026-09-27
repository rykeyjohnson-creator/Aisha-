package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Manages Speech-To-Text using Android's SpeechRecognizer.
 * Ensures single active session, graceful error mapping, and lifecycle teardown.
 * Extensible for continuous listening in future Level 4.
 */
class VoiceController(
    private val context: Context,
    private val listener: VoiceListener
) {

    interface VoiceListener {
        fun onReady()
        fun onBeginningOfSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onEndOfSpeech()
        fun onSpeechRecognized(text: String)
        fun onError(userMessage: String)
    }

    private val tag = "VoiceController"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(InternalRecognitionListener())
                    }
                } else {
                    Log.w(tag, "Speech recognition is not available on this device")
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to create SpeechRecognizer", e)
            }
        }
    }

    /**
     * Starts listening for user speech.
     * Prevents multiple simultaneous sessions.
     */
    fun startListening() {
        mainHandler.post {
            if (isListening) {
                Log.d(tag, "Already listening, ignoring duplicate startListening call")
                return@post
            }

            if (speechRecognizer == null) {
                initializeRecognizer()
            }

            val recognizer = speechRecognizer
            if (recognizer == null) {
                listener.onError("Speech recognition is not supported or initialized on this device.")
                return@post
            }

            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN"))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }

                isListening = true
                recognizer.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                Log.e(tag, "Failed to start speech recognition", e)
                listener.onError("Could not start microphone. Please try again.")
            }
        }
    }

    /**
     * Stops listening and waits for final results.
     */
    fun stopListening() {
        mainHandler.post {
            if (!isListening) return@post
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(tag, "Error stopping recognition", e)
            }
        }
    }

    /**
     * Cancels any ongoing listening session.
     */
    fun cancel() {
        mainHandler.post {
            isListening = false
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(tag, "Error cancelling recognition", e)
            }
        }
    }

    /**
     * Completely destroys the recognizer and releases resources.
     */
    fun destroy() {
        mainHandler.post {
            isListening = false
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(tag, "Error destroying speech recognizer", e)
            }
        }
    }

    private inner class InternalRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            listener.onReady()
        }

        override fun onBeginningOfSpeech() {
            listener.onBeginningOfSpeech()
        }

        override fun onRmsChanged(rmsdB: Float) {
            listener.onRmsChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            listener.onEndOfSpeech()
        }

        override fun onError(error: Int) {
            isListening = false
            val message = mapErrorCodeToMessage(error)
            Log.w(tag, "Speech recognition error: $error -> $message")
            listener.onError(message)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topResult = matches?.firstOrNull()?.trim()
            if (!topResult.isNullOrEmpty()) {
                listener.onSpeechRecognized(topResult)
            } else {
                listener.onError("I couldn't hear you clearly. Please try again.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapErrorCodeToMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition cancelled."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice commands."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue while recognizing speech. Please check your connection."
            SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't hear you clearly. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy. Please try again."
            SpeechRecognizer.ERROR_SERVER -> "Voice server error. Please try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic when you're ready."
            else -> "I couldn't hear you. Please try again."
        }
    }
}
