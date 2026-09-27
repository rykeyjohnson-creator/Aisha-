package com.example.memory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.MemoryItem
import com.example.model.MemoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.aishaDataStore: DataStore<Preferences> by preferencesDataStore(name = "aisha_memory_store")

/**
 * Manages conversation context, user preferences, and recent dialogue pairs
 * persisted locally via Jetpack DataStore.
 * Ensures stored context is formatted and supplied into prompts sent to AishaAiEngine.
 */
class MemoryManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        private val KEY_MEMORIES_JSON = stringPreferencesKey("persisted_memories_json")
        private val KEY_DIALOGUE_PAIRS_JSON = stringPreferencesKey("persisted_dialogue_pairs_json")
        private const val MAX_RECENT_TURNS = 10
    }

    // In-memory cache for fast, non-blocking synchronous access
    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    // Recent dialogue pairs (User Query -> Aisha Response)
    private val _recentTurns = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val recentTurns: StateFlow<List<Pair<String, String>>> = _recentTurns.asStateFlow()

    init {
        scope.launch {
            loadFromDataStore()
        }
    }

    /**
     * Loads persisted preferences and dialogue pairs from DataStore into memory cache.
     */
    private suspend fun loadFromDataStore() {
        try {
            val prefs = context.aishaDataStore.data.first()

            // 1. Load memories (user info & preferences)
            val memoriesJson = prefs[KEY_MEMORIES_JSON]
            if (!memoriesJson.isNullOrEmpty()) {
                val list = mutableListOf<MemoryItem>()
                val array = JSONArray(memoriesJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MemoryItem(
                            key = obj.getString("key"),
                            value = obj.getString("value"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            type = try {
                                MemoryType.valueOf(obj.getString("type"))
                            } catch (e: Exception) {
                                MemoryType.CONVERSATION
                            }
                        )
                    )
                }
                _memories.value = list
            }

            // 2. Load recent dialogue pairs
            val dialogueJson = prefs[KEY_DIALOGUE_PAIRS_JSON]
            if (!dialogueJson.isNullOrEmpty()) {
                val pairs = mutableListOf<Pair<String, String>>()
                val array = JSONArray(dialogueJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val userText = obj.optString("user", "")
                    val aishaText = obj.optString("aisha", "")
                    if (userText.isNotBlank() && aishaText.isNotBlank()) {
                        pairs.add(userText to aishaText)
                    }
                }
                _recentTurns.value = pairs
            }
        } catch (e: Exception) {
            // Non-fatal: in-memory state will be used if disk read fails
        }
    }

    /**
     * Persists memory items (user info, preferences) to DataStore.
     */
    private fun persistMemoriesToDisk() {
        scope.launch {
            try {
                val array = JSONArray()
                _memories.value.forEach { item ->
                    val obj = JSONObject()
                    obj.put("key", item.key)
                    obj.put("value", item.value)
                    obj.put("timestamp", item.timestamp)
                    obj.put("type", item.type.name)
                    array.put(obj)
                }
                context.aishaDataStore.edit { prefs ->
                    prefs[KEY_MEMORIES_JSON] = array.toString()
                }
            } catch (e: Exception) {
                // Safeguard against disk write exceptions
            }
        }
    }

    /**
     * Persists recent dialogue pairs to DataStore.
     */
    private fun persistDialoguePairsToDisk() {
        scope.launch {
            try {
                val array = JSONArray()
                _recentTurns.value.takeLast(MAX_RECENT_TURNS).forEach { (user, aisha) ->
                    val obj = JSONObject()
                    obj.put("user", user)
                    obj.put("aisha", aisha)
                    array.put(obj)
                }
                context.aishaDataStore.edit { prefs ->
                    prefs[KEY_DIALOGUE_PAIRS_JSON] = array.toString()
                }
            } catch (e: Exception) {
                // Safeguard against disk write exceptions
            }
        }
    }

    // =========================================================================
    // User Preferences & Profile Management
    // =========================================================================

    /**
     * Saves or updates a user preference.
     */
    fun savePreference(key: String, value: String) {
        remember(key = key, value = value, type = MemoryType.PREFERENCE)
    }

    /**
     * Retrieves a stored user preference by key.
     */
    fun getPreference(key: String): String? {
        return _memories.value.find {
            it.type == MemoryType.PREFERENCE && it.key.equals(key, ignoreCase = true)
        }?.value
    }

    /**
     * Retrieves all stored preferences as a Map of Key to Value.
     */
    fun getAllPreferences(): Map<String, String> {
        return _memories.value
            .filter { it.type == MemoryType.PREFERENCE }
            .associate { it.key to it.value }
    }

    /**
     * Removes a stored preference by key.
     */
    fun removePreference(key: String) {
        val updated = _memories.value.filterNot {
            it.type == MemoryType.PREFERENCE && it.key.equals(key, ignoreCase = true)
        }
        _memories.value = updated
        persistMemoriesToDisk()
    }

    /**
     * Stores or updates a remembered fact with a specified MemoryType (USER_INFO, PREFERENCE, etc.).
     */
    fun remember(key: String, value: String, type: MemoryType = MemoryType.CONVERSATION) {
        val updated = _memories.value.filterNot { it.key.equals(key, ignoreCase = true) } +
                MemoryItem(key = key, value = value, type = type)
        _memories.value = updated
        persistMemoriesToDisk()
    }

    /**
     * Retrieves a remembered value by key.
     */
    fun recall(key: String): String? {
        return _memories.value.find { it.key.equals(key, ignoreCase = true) }?.value
    }

    /**
     * Returns all items marked as PREFERENCE.
     */
    fun getPreferences(): List<MemoryItem> {
        return _memories.value.filter { it.type == MemoryType.PREFERENCE }
    }

    // =========================================================================
    // Conversation Context & Dialogue Pairs Management
    // =========================================================================

    /**
     * Saves a user query and Aisha reply pair to both memory cache and DataStore.
     */
    fun saveDialoguePair(userText: String, aishaReply: String) {
        if (userText.isBlank() || aishaReply.isBlank()) return
        val current = _recentTurns.value.toMutableList()
        current.add(userText.trim() to aishaReply.trim())
        if (current.size > MAX_RECENT_TURNS) {
            current.removeAt(0)
        }
        _recentTurns.value = current
        persistDialoguePairsToDisk()
    }

    /**
     * Alias for saveDialoguePair for compatibility with existing Level 1 / Level 2 code.
     */
    fun addTurn(userText: String, aishaReply: String) {
        saveDialoguePair(userText, aishaReply)
    }

    /**
     * Returns the most recent dialogue pairs up to the specified limit.
     */
    fun getRecentDialoguePairs(limit: Int = 6): List<Pair<String, String>> {
        return _recentTurns.value.takeLast(limit)
    }

    /**
     * Clears conversational dialogue history from memory and DataStore.
     */
    fun clearRecentDialogue() {
        _recentTurns.value = emptyList()
        persistDialoguePairsToDisk()
    }

    /**
     * Clears all memory (preferences, profile, and conversation turns).
     */
    fun clearAll() {
        _memories.value = emptyList()
        _recentTurns.value = emptyList()
        scope.launch {
            try {
                context.aishaDataStore.edit { it.clear() }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // =========================================================================
    // AI Context Prompt Generation
    // =========================================================================

    /**
     * Formats stored user preferences, profile details, and recent dialogue pairs
     * into a clean context prompt to be passed directly to AishaAiEngine.
     */
    fun buildContextPrompt(): String {
        val parts = mutableListOf<String>()

        // 1. User Profile & Preferences
        val profileParts = mutableListOf<String>()
        val userName = recall("user_name")
        if (!userName.isNullOrBlank()) {
            profileParts.add("User's name: $userName")
        }

        val allPrefs = getAllPreferences()
        if (allPrefs.isNotEmpty()) {
            val formattedPrefs = allPrefs.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            profileParts.add("Preferences: $formattedPrefs")
        }

        // Also check any MemoryItem marked PREFERENCE that may have custom keys
        val generalPrefs = getPreferences().filterNot { allPrefs.containsKey(it.key) }
        if (generalPrefs.isNotEmpty()) {
            val list = generalPrefs.joinToString("; ") { "${it.key}: ${it.value}" }
            profileParts.add("Additional preferences: $list")
        }

        if (profileParts.isNotEmpty()) {
            parts.add("[User Information & Preferences]\n" + profileParts.joinToString("\n"))
        }

        // 2. Recent Dialogue Pairs (Context Window)
        val turns = getRecentDialoguePairs(limit = 4)
        if (turns.isNotEmpty()) {
            val history = turns.joinToString("\n") { (user, aisha) ->
                "User: $user\nAisha: $aisha"
            }
            parts.add("[Recent Dialogue Context]\n$history")
        }

        return parts.joinToString("\n\n").trim()
    }
}
