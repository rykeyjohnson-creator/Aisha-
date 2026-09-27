package com.example.model

/**
 * Represents the current operational state of the Aisha Assistant.
 */
enum class AssistantState(val statusLabel: String) {
    READY("● Ready"),
    LISTENING("◉ Listening..."),
    PROCESSING("◉ Processing..."),
    THINKING("◉ Thinking..."),
    SPEAKING("◉ Speaking..."),
    ERROR("● Ready")
}

/**
 * Real-time AI engine status indicator.
 */
enum class AiStatus(val label: String) {
    ONLINE("ONLINE"),
    THINKING("THINKING"),
    READY("READY"),
    OFFLINE("OFFLINE"),
    ERROR("ERROR")
}

/**
 * Message model for the conversation display.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    AISHA
}

/**
 * Structured response model from Aisha response engine.
 */
data class AishaResponse(
    val text: String,
    val spokenText: String = text,
    val isHindi: Boolean = false,
    val source: ResponseSource = ResponseSource.LOCAL,
    val shouldSpeak: Boolean = true
)

enum class ResponseSource {
    LOCAL,
    AI,
    ERROR
}

/**
 * Memory item for short-term and persistent conversational facts.
 */
data class MemoryItem(
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MemoryType = MemoryType.CONVERSATION
)

enum class MemoryType {
    CONVERSATION,
    PREFERENCE,
    USER_INFO
}
