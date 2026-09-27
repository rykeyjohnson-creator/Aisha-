package com.example.assistant

import com.example.action.AishaActionDispatcher
import com.example.action.DefaultActionDispatcher
import com.example.ai.AishaAiEngine
import com.example.memory.MemoryManager
import com.example.model.AishaResponse
import com.example.model.MemoryType
import com.example.model.ResponseSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Aisha Level 2 Response Engine.
 * Responsibilities:
 * 1. Checks and processes Local Commands FIRST (Time, Date, Identity, Greetings) with zero latency.
 * 2. Checks and manages conversational Memory (User facts, preferences).
 * 3. Evaluates action dispatchers (preparing for Level 3).
 * 4. Routes general reasoning and conversational queries to Gemini AI Brain (AishaAiEngine).
 */
class AishaAssistant(
    val memoryManager: MemoryManager? = null,
    val aiEngine: AishaAiEngine? = null,
    val actionDispatcher: AishaActionDispatcher = DefaultActionDispatcher()
) {

    /**
     * Unified suspend process method.
     * All user queries (voice, text, chips) route through this single entry point.
     */
    suspend fun process(rawInput: String): AishaResponse {
        val sanitized = CommandNormalizer.sanitize(rawInput)
        val core = CommandNormalizer.extractCoreCommand(rawInput)
        val isHindi = CommandNormalizer.isHindiQuery(rawInput)

        // 1. Check Local Commands FIRST (Preserve 100% of Level 1 behavior)

        // Greetings
        if (isGreeting(sanitized, core)) {
            val reply = if (isHindi && (sanitized.contains("namaste") || sanitized.contains("namaskar"))) {
                "Namaste! Main Aisha hoon. Main aapki kya madad kar sakti hoon?"
            } else {
                "Hello! I'm Aisha. How can I help you?"
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // Identity / Who are you
        if (isIdentityQuery(sanitized, core)) {
            val reply = if (isHindi) {
                "Main Aisha hoon, aapki AI voice assistant."
            } else {
                "I'm Aisha, your AI voice assistant."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // Name query
        if (isNameQuery(sanitized, core)) {
            val reply = if (isHindi) {
                "Mera naam Aisha hai."
            } else {
                "My name is Aisha."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // Status / How are you
        if (isHowAreYouQuery(sanitized, core)) {
            val reply = if (isHindi) {
                "Main theek hoon, aur aapki madad ke liye taiyar hoon."
            } else {
                "I'm ready to help."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // Time queries
        if (isTimeQuery(sanitized, core)) {
            val formattedTime = getCurrentDeviceTime()
            val reply = if (isHindi) {
                "Abhi samay hai $formattedTime."
            } else {
                "The current time is $formattedTime."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // Date queries
        if (isDateQuery(sanitized, core)) {
            val formattedDate = getCurrentDeviceDate()
            val reply = if (isHindi) {
                "Aaj ki tareekh hai $formattedDate."
            } else {
                "Today is $formattedDate."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // Politeness & common intents
        if (isThanksQuery(sanitized, core)) {
            val reply = if (isHindi) {
                "Aapka swagat hai! Main hamesha aapki madad ke liye yahan hoon."
            } else {
                "You're very welcome! I'm always here to help."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        if (isFarewellQuery(sanitized, core)) {
            val reply = if (isHindi) {
                "Alvida! Jab bhi zaroorat ho, mujhe aawaz dijiye."
            } else {
                "Goodbye! Call on me anytime you need assistance."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        if (isHelpQuery(sanitized, core)) {
            val reply = if (isHindi) {
                "Aap mujhse samay, tareekh, vigyan, ya koi bhi sawal pooch sakte hain. Kahiye: 'Kitne baje hain' ya 'Tell me about space'!"
            } else {
                "You can ask me the time, date, who I am, or any conversational question like 'Explain black holes' or 'Tell me a story'!"
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // 2. Memory Operations (Level 2: User Name & Preferences)
        val nameLearnMatch = extractNameDeclaration(sanitized)
        if (nameLearnMatch != null) {
            memoryManager?.remember("user_name", nameLearnMatch, MemoryType.USER_INFO)
            val reply = if (isHindi) {
                "Namaste $nameLearnMatch! Aapse milkar accha laga. Main aapka naam yaad rakhungi."
            } else {
                "Nice to meet you, $nameLearnMatch! I will remember your name."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        if (isUserNameRecallQuery(sanitized, core)) {
            val rememberedName = memoryManager?.recall("user_name")
            val reply = if (!rememberedName.isNullOrBlank()) {
                if (isHindi) "Aapka naam $rememberedName hai." else "Your name is $rememberedName."
            } else {
                if (isHindi) "Maine abhi tak aapka naam nahi jaana. Aap keh sakte hain: 'Mera naam Rahul hai'."
                else "I don't know your name yet. You can tell me by saying 'My name is Rahul'."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        val prefLearnMatch = extractPreferenceDeclaration(sanitized)
        if (prefLearnMatch != null) {
            memoryManager?.remember("like_$prefLearnMatch", prefLearnMatch, MemoryType.PREFERENCE)
            val reply = if (isHindi) {
                "Maine note kar liya hai ki aapko $prefLearnMatch pasand hai."
            } else {
                "Noted! I'll remember that you like $prefLearnMatch."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        if (isUserPreferenceRecallQuery(sanitized, core)) {
            val prefs = memoryManager?.getPreferences()
            val reply = if (!prefs.isNullOrEmpty()) {
                val items = prefs.joinToString(", ") { it.value }
                if (isHindi) "Aapne bataya tha ki aapko ye pasand hai: $items." else "You mentioned that you like: $items."
            } else {
                if (isHindi) "Maine abhi aapki pasand note nahi ki hai. Aap keh sakte hain: 'Mujhe space pasand hai'."
                else "You haven't told me your preferences yet. Try saying 'I like space'."
            }
            val res = AishaResponse(text = reply, spokenText = reply, isHindi = isHindi, source = ResponseSource.LOCAL)
            memoryManager?.addTurn(rawInput, reply)
            return res
        }

        // 3. Level 3 Hook (Check action dispatcher)
        if (actionDispatcher.canHandle(rawInput)) {
            actionDispatcher.dispatch(rawInput)
            // (Level 3 will return specific action results here)
        }

        // 4. Gemini AI Brain Integration (Level 2)
        if (aiEngine != null) {
            val contextPrompt = memoryManager?.buildContextPrompt() ?: ""
            val aiResponse = aiEngine.generateResponse(
                query = rawInput,
                contextPrompt = contextPrompt,
                isHindi = isHindi
            )
            if (aiResponse.source == ResponseSource.AI) {
                memoryManager?.addTurn(rawInput, aiResponse.text)
            }
            return aiResponse
        }

        // 5. Default fallback if AI engine is not attached
        val fallback = if (isHindi) {
            "Main Aisha hoon. Main aapke sawalon ke jawab de sakti hoon ya samay aur tareekh bata sakti hoon."
        } else {
            "I'm Aisha. I can help answer your questions, remember facts, or tell you the time and date."
        }
        return AishaResponse(
            text = fallback,
            spokenText = fallback,
            isHindi = isHindi,
            source = ResponseSource.LOCAL
        )
    }

    private fun extractNameDeclaration(sanitized: String): String? {
        val patterns = listOf(
            Regex("my name is ([a-zA-Z]+)", RegexOption.IGNORE_CASE),
            Regex("i am ([a-zA-Z]+)", RegexOption.IGNORE_CASE),
            Regex("call me ([a-zA-Z]+)", RegexOption.IGNORE_CASE),
            Regex("mera naam ([a-zA-Z]+) hai", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(sanitized)
            if (match != null && match.groupValues.size > 1) {
                val name = match.groupValues[1].capitalize(Locale.ROOT)
                // Filter out non-names like "aisha", "here", "ready"
                if (!name.equals("aisha", ignoreCase = true) && !name.equals("ready", ignoreCase = true)) {
                    return name
                }
            }
        }
        return null
    }

    private fun isUserNameRecallQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "what is my name",
            "what's my name",
            "whats my name",
            "do you know my name",
            "who am i",
            "mera naam kya hai",
            "kya tum mera naam janti ho",
            "mera naam batao"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun extractPreferenceDeclaration(sanitized: String): String? {
        val patterns = listOf(
            Regex("i like (.+)", RegexOption.IGNORE_CASE),
            Regex("i love (.+)", RegexOption.IGNORE_CASE),
            Regex("mujhe (.+) pasand hai", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(sanitized)
            if (match != null && match.groupValues.size > 1) {
                val pref = match.groupValues[1].trim()
                if (pref.isNotBlank()) return pref
            }
        }
        return null
    }

    private fun isUserPreferenceRecallQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "what do i like",
            "what are my preferences",
            "do you know what i like",
            "mujhe kya pasand hai"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isGreeting(sanitized: String, core: String): Boolean {
        val matches = listOf(
            "hi", "hello", "hey", "hi aisha", "hello aisha", "hey aisha",
            "namaste", "namaskar", "namaste aisha", "namaskar aisha",
            "aisha", "sun aisha", "suno aisha"
        )
        return sanitized in matches || core in listOf("hi", "hello", "hey", "namaste", "namaskar")
    }

    private fun isIdentityQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "who are you",
            "who r u",
            "who are u",
            "aisha tum kaun ho",
            "tum kaun ho",
            "aap kaun hain",
            "aap kaun ho",
            "kaun ho tum",
            "kaun ho aap"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isNameQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "what is your name",
            "what's your name",
            "whats your name",
            "tell me your name",
            "your name",
            "tumhara naam kya hai",
            "aapka naam kya hai",
            "naam kya hai"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isHowAreYouQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "how are you",
            "how are u",
            "how r you",
            "how r u",
            "how do you do",
            "aisha kaise ho",
            "kaise ho",
            "kaisi ho",
            "aap kaise hain",
            "kya haal hai",
            "sab theek"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isTimeQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "what time is it",
            "what is the time",
            "whats the time",
            "current time",
            "tell me the time",
            "time please",
            "time",
            "kitne baje hain",
            "kitne baje hai",
            "kya samay hua hai",
            "kya time hua hai",
            "samay kya hai",
            "time kya hua",
            "kitna time hua"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isDateQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "what is today's date",
            "what is todays date",
            "what's today's date",
            "whats todays date",
            "what is the date",
            "today's date",
            "todays date",
            "what date is it",
            "current date",
            "date today",
            "date",
            "aaj ki date kya hai",
            "aaj ki date",
            "aaj kaun si date hai",
            "aaj kaun si tareekh hai",
            "aaj ki tareekh kya hai",
            "aaj ki tareekh",
            "aaj kaun sa din hai"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isThanksQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "thank you", "thanks", "thank u", "shukriya", "dhanyawad", "dhanyavad"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isFarewellQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "bye", "goodbye", "good bye", "see you", "alvida", "phir milenge"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    private fun isHelpQuery(sanitized: String, core: String): Boolean {
        val patterns = listOf(
            "help", "commands", "what can you do", "kya kar sakti ho", "madad"
        )
        return patterns.any { sanitized.contains(it) || core.contains(it) }
    }

    /**
     * Gets the actual current device time dynamically.
     */
    fun getCurrentDeviceTime(): String {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(Date())
    }

    /**
     * Gets the actual current device date dynamically.
     * Example format: Sunday, 27 September 2026
     */
    fun getCurrentDeviceDate(): String {
        val format = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        return format.format(Date())
    }

    /**
     * Returns a time-appropriate greeting:
     * "Good morning,", "Good afternoon,", "Good evening,"
     */
    fun getGreetingHeader(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..21 -> "Good evening,"
            else -> "Good night,"
        }
    }
}
