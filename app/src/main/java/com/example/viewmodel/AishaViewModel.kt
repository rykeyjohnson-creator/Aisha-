package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AishaAiEngine
import com.example.assistant.AishaAssistant
import com.example.memory.MemoryManager
import com.example.model.AiStatus
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.ResponseSource
import com.example.voice.AishaTts
import com.example.voice.VoiceController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Level 2 ViewModel managing Aisha's reactive UI state, unifying voice, text,
 * local commands, memory, and Gemini AI queries through a single pipeline.
 */
class AishaViewModel(application: Application) : AndroidViewModel(application), VoiceController.VoiceListener {

    private val memoryManager = MemoryManager(application, viewModelScope)
    private val aiEngine = AishaAiEngine(application)
    private val assistant = AishaAssistant(
        memoryManager = memoryManager,
        aiEngine = aiEngine
    )

    private var voiceController: VoiceController? = null
    private var aishaTts: AishaTts? = null

    private val _assistantState = MutableStateFlow(AssistantState.READY)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _aiStatus = MutableStateFlow(calculateInitialAiStatus())
    val aiStatus: StateFlow<AiStatus> = _aiStatus.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _rmsdB = MutableStateFlow(0f)
    val rmsdB: StateFlow<Float> = _rmsdB.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        // Initialize TTS safely with application context
        aishaTts = AishaTts(application) { success ->
            if (!success) {
                viewModelScope.launch {
                    _snackbarMessages.emit("Voice synthesizer active with default engine.")
                }
            }
        }

        // Initialize SpeechRecognizer safely
        voiceController = VoiceController(application, this)

        // Observe memory count
        viewModelScope.launch {
            memoryManager.memories.collect {
                updateAiStatus()
            }
        }
    }

    private fun calculateInitialAiStatus(): AiStatus {
        return if (!aiEngine.isConfigured()) {
            AiStatus.READY
        } else if (!aiEngine.isNetworkAvailable()) {
            AiStatus.OFFLINE
        } else {
            AiStatus.ONLINE
        }
    }

    private fun updateAiStatus() {
        if (_assistantState.value != AssistantState.THINKING) {
            _aiStatus.value = calculateInitialAiStatus()
        }
    }

    /**
     * Unified handler for ALL user inputs:
     * Voice transcription, text input, and quick buttons.
     */
    fun processInput(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // 1. Post user message
            val userMsg = ChatMessage(sender = MessageSender.USER, text = trimmed)
            _messages.value = _messages.value + userMsg

            // 2. Set processing/thinking state
            _assistantState.value = AssistantState.PROCESSING
            _rmsdB.value = 0f

            // 3. Process via unified AishaAssistant response engine
            _assistantState.value = AssistantState.THINKING
            _aiStatus.value = AiStatus.THINKING

            val response = assistant.process(trimmed)

            // Update AI status card back from thinking
            _aiStatus.value = if (response.source == ResponseSource.ERROR) {
                if (!aiEngine.isNetworkAvailable()) AiStatus.OFFLINE else AiStatus.READY
            } else {
                calculateInitialAiStatus()
            }

            // 4. Post Aisha response message (full markdown/clean text displayed)
            val aishaMsg = ChatMessage(sender = MessageSender.AISHA, text = response.text)
            _messages.value = _messages.value + aishaMsg

            // 5. Speak response via TTS using spokenText (cleaned for audio playback)
            _assistantState.value = AssistantState.SPEAKING
            aishaTts?.speak(
                text = response.spokenText,
                preferHindi = response.isHindi,
                onStart = {
                    viewModelScope.launch {
                        _assistantState.value = AssistantState.SPEAKING
                    }
                },
                onDone = {
                    viewModelScope.launch {
                        _assistantState.value = AssistantState.READY
                    }
                }
            )
        }
    }

    /**
     * Toggles microphone listening or stops speaking.
     */
    fun onMicTapped(hasRecordAudioPermission: Boolean, onRequestPermission: () -> Unit) {
        if (!hasRecordAudioPermission) {
            onRequestPermission()
            return
        }

        when (_assistantState.value) {
            AssistantState.LISTENING -> {
                voiceController?.stopListening()
                _assistantState.value = AssistantState.PROCESSING
            }
            AssistantState.SPEAKING -> {
                aishaTts?.stop()
                _assistantState.value = AssistantState.READY
            }
            AssistantState.PROCESSING, AssistantState.THINKING -> {
                // Wait for current processing
            }
            AssistantState.READY, AssistantState.ERROR -> {
                aishaTts?.stop()
                _assistantState.value = AssistantState.LISTENING
                voiceController?.startListening()
            }
        }
    }

    /**
     * Called when microphone permission was denied by the user.
     */
    fun onPermissionDenied() {
        _assistantState.value = AssistantState.READY
        viewModelScope.launch {
            _snackbarMessages.emit("Microphone permission is required for voice commands.")
        }
    }

    // VoiceController Callbacks
    override fun onReady() {
        _assistantState.value = AssistantState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _assistantState.value = AssistantState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        _rmsdB.value = rmsdB
    }

    override fun onEndOfSpeech() {
        _assistantState.value = AssistantState.PROCESSING
    }

    override fun onSpeechRecognized(text: String) {
        processInput(text)
    }

    override fun onError(userMessage: String) {
        _assistantState.value = AssistantState.READY
        _rmsdB.value = 0f
        viewModelScope.launch {
            _snackbarMessages.emit(userMessage)
        }
    }

    fun onActivityStop() {
        voiceController?.cancel()
        aishaTts?.stop()
        if (_assistantState.value == AssistantState.LISTENING || _assistantState.value == AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.READY
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceController?.destroy()
        aishaTts?.shutdown()
    }
}
