package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.model.AishaResponse
import com.example.model.ResponseSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Aisha Level 2 AI Brain powered by Gemini (gemini-3.5-flash).
 * Securely communicates with the Gemini REST API using BuildConfig credentials,
 * with timeout resilience, conversational context injection, and TTS speech optimization.
 */
class AishaAiEngine(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Checks if the Gemini API key is configured and not the default placeholder.
     */
    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Checks if active network connectivity is present.
     */
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Sends user query along with conversation context and system instructions to Gemini.
     */
    suspend fun generateResponse(
        query: String,
        contextPrompt: String = "",
        isHindi: Boolean = false
    ): AishaResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()

        // 1. Validate configuration
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val msg = if (isHindi) {
                "AI brain configure nahi hai. Kripya AI Studio Secrets mein Gemini API key set karein."
            } else {
                "My AI brain key is not configured. Please set the Gemini API key in the AI Studio Secrets panel."
            }
            return@withContext AishaResponse(
                text = msg,
                spokenText = msg,
                isHindi = isHindi,
                source = ResponseSource.ERROR
            )
        }

        // 2. Validate network
        if (!isNetworkAvailable()) {
            val msg = if (isHindi) {
                "Internet connection uplabdh nahi hai. Kripya apna network check karein."
            } else {
                "I'm unable to connect to the internet right now. Please check your connection."
            }
            return@withContext AishaResponse(
                text = msg,
                spokenText = msg,
                isHindi = isHindi,
                source = ResponseSource.ERROR
            )
        }

        try {
            val requestBodyJson = buildRequestBody(query, contextPrompt)
            val requestBody = requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                val fallback = if (isHindi) {
                    "Mujhe abhi apne AI brain se judne mein dikkat aa rahi hai. Kripya thodi der baad prayas karein."
                } else {
                    "I'm having trouble connecting to my AI brain right now. Please try again in a moment."
                }
                return@withContext AishaResponse(
                    text = fallback,
                    spokenText = fallback,
                    isHindi = isHindi,
                    source = ResponseSource.ERROR
                )
            }

            // Parse response
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val fallback = if (isHindi) {
                    "Main iska uttar nahi dhoondh paayi."
                } else {
                    "I couldn't generate a response for that."
                }
                return@withContext AishaResponse(
                    text = fallback,
                    spokenText = fallback,
                    isHindi = isHindi,
                    source = ResponseSource.ERROR
                )
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text").trim()

            // Prepare text and clean spoken text for TTS
            val spokenCleaned = cleanForSpeech(rawText)

            return@withContext AishaResponse(
                text = rawText,
                spokenText = spokenCleaned,
                isHindi = isHindi,
                source = ResponseSource.AI
            )

        } catch (e: Exception) {
            val fallback = if (isHindi) {
                "Mujhe sampark karne mein dikkat aayi. Kripya punah prayas karein."
            } else {
                "I'm having trouble connecting to my AI brain right now."
            }
            return@withContext AishaResponse(
                text = fallback,
                spokenText = fallback,
                isHindi = isHindi,
                source = ResponseSource.ERROR
            )
        }
    }

    private fun buildRequestBody(query: String, contextPrompt: String): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstructionText = """
            You are Aisha, a premium futuristic female AI voice assistant.
            You are friendly, concise, natural, respectful, and helpful.
            IMPORTANT FOR VOICE OUTPUT:
            1. Keep your answers concise (1 to 3 short sentences maximum) because your response will be spoken aloud to the user via Text-to-Speech.
            2. Do NOT use markdown symbols, asterisks, bullet points, numbering, or headers. Use plain natural conversational sentences only.
            3. You understand English, Hindi, and Hinglish. If spoken to in Hindi or Hinglish, reply naturally in Hindi or Hinglish. If addressed in English, reply in English.
            4. Identify yourself as Aisha when asked.
            ${if (contextPrompt.isNotBlank()) "\n[MEMORY & CONTEXT]\n$contextPrompt\nAlways use this stored memory and recent dialogue context to tailor your response to the user." else ""}
        """.trimIndent()

        val sysContent = JSONObject()
        val sysParts = JSONArray()
        val sysPart = JSONObject().apply { put("text", systemInstructionText) }
        sysParts.put(sysPart)
        sysContent.put("parts", sysParts)
        root.put("systemInstruction", sysContent)

        // Contents
        val contentsArray = JSONArray()
        val userContent = JSONObject()
        userContent.put("role", "user")
        val userParts = JSONArray()
        val userPart = JSONObject().apply { put("text", query) }
        userParts.put(userPart)
        userContent.put("parts", userParts)
        contentsArray.put(userContent)
        root.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 250)
        }
        root.put("generationConfig", genConfig)

        return root
    }

    /**
     * Sanitizes response for Text-To-Speech by removing markdown formatting
     * (asterisks, bullet hashes, code backticks) so the TTS engine speaks naturally.
     */
    private fun cleanForSpeech(input: String): String {
        return input
            .replace(Regex("[*#_`~>]+"), "") // remove markdown punctuation
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // strip markdown links
            .replace(Regex("\\s+"), " ") // normalize spacing
            .trim()
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }
}
