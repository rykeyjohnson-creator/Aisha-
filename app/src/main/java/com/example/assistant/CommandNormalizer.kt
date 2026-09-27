package com.example.assistant

import java.util.Locale

/**
 * Normalizes user text and voice input into sanitized, predictable command formats.
 * Handles casing, excess whitespace, punctuation, wake words ("Aisha", "Hey Aisha", etc.),
 * and simple Hindi/Hinglish phrase variants.
 */
object CommandNormalizer {

    private val PREFIX_PATTERNS = listOf(
        "hey aisha",
        "hi aisha",
        "hello aisha",
        "ok aisha",
        "okay aisha",
        "suno aisha",
        "sun aisha",
        "are aisha",
        "aisha"
    )

    /**
     * Sanitizes raw input: trims, lowercases, removes non-alphanumeric punctuation (except spaces),
     * and collapses multiple spaces into single space.
     */
    fun sanitize(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) return ""
        return rawInput
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s\\u0900-\\u097F]"), " ") // keep English letters, numbers, spaces, and Devanagari Unicode
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Extracts the core query by stripping leading wake/call words (e.g. "hey aisha", "aisha").
     * Returns either the stripped core command or the sanitized string if no prefix or if the
     * whole string was just the wake word.
     */
    fun extractCoreCommand(rawInput: String?): String {
        val sanitized = sanitize(rawInput)
        if (sanitized.isEmpty()) return ""

        for (prefix in PREFIX_PATTERNS) {
            if (sanitized == prefix) {
                return prefix // e.g. user just said "aisha" or "hi aisha"
            }
            if (sanitized.startsWith("$prefix ")) {
                val remainder = sanitized.removePrefix("$prefix ").trim()
                if (remainder.isNotEmpty()) {
                    return remainder
                }
            }
        }
        return sanitized
    }

    /**
     * Checks if the query looks like Hindi or Hinglish based on keywords or Devanagari script.
     */
    fun isHindiQuery(text: String): Boolean {
        val sanitized = sanitize(text)
        val hindiMarkers = listOf(
            "kaun", "kya", "kaise", "kahan", "kitne", "baje", "hain", "hai",
            "aaj", "tareekh", "din", "samay", "namaste", "namaskar", "shukriya",
            "dhanyawad", "madad", "theek", "tum", "aap", "mera", "meri", "hum"
        )
        // Check for devanagari characters
        if (text.any { it in '\u0900'..'\u097F' }) return true

        val words = sanitized.split(" ")
        return words.any { it in hindiMarkers }
    }
}
