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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aisha_memory")

/**
 * Manages short-term conversation context and persistent user info/preferences using DataStore.
 * Enables Aisha to remember user facts (name, preferences) and recent conversation turns.
 */
class MemoryManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val KEY_MEMORIES_JSON = stringPreferencesKey("memories_store")

    // In-memory cache for ultra-fast access
    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    // Recent conversation turns (User query -> Aisha answer)
    private val _recentTurns = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val recentTurns: StateFlow<List<Pair<String, String>>> = _recentTurns.asStateFlow()

    init {
        scope.launch {
            loadPersistedMemories()
        }
    }

    private suspend fun loadPersistedMemories() {
        try {
            val prefs = context.dataStore.data.first()
            val jsonStr = prefs[KEY_MEMORIES_JSON]
            if (!jsonStr.isNullOrEmpty()) {
                val list = mutableListOf<MemoryItem>()
                val array = JSONArray(jsonStr)
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
        } catch (e: Exception) {
            // Graceful fallback if storage read fails
        }
    }

    private fun persistMemories() {
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
                context.dataStore.edit { prefs ->
                    prefs[KEY_MEMORIES_JSON] = array.toString()
                }
            } catch (e: Exception) {
                // Ignore persistence errors silently to prevent crash
            }
        }
    }

    /**
     * Stores or updates a remembered fact (e.g. name, favorite topic).
     */
    fun remember(key: String, value: String, type: MemoryType) {
        val updated = _memories.value.filterNot { it.key.equals(key, ignoreCase = true) } +
                MemoryItem(key = key, value = value, type = type)
        _memories.value = updated
        persistMemories()
    }

    /**
     * Retrieves a remembered fact by key.
     */
    fun recall(key: String): String? {
        return _memories.value.find { it.key.equals(key, ignoreCase = true) }?.value
    }

    /**
     * Returns all stored preferences.
     */
    fun getPreferences(): List<MemoryItem> {
        return _memories.value.filter { it.type == MemoryType.PREFERENCE }
    }

    /**
     * Records a conversation turn and keeps the recent history capped to max 8 turns.
     */
    fun addTurn(userText: String, aishaReply: String) {
        val current = _recentTurns.value.toMutableList()
        current.add(userText to aishaReply)
        if (current.size > 8) {
            current.removeAt(0)
        }
        _recentTurns.value = current
    }

    /**
     * Builds a concise context string to supply to the AI model.
     */
    fun buildContextPrompt(): String {
        val builder = StringBuilder()

        val userName = recall("user_name")
        if (!userName.isNullOrBlank()) {
            builder.append("User's name is: $userName. ")
        }

        val prefs = getPreferences()
        if (prefs.isNotEmpty()) {
            val prefList = prefs.joinToString(", ") { "${it.key}: ${it.value}" }
            builder.append("User preferences: [$prefList]. ")
        }

        val turns = _recentTurns.value.takeLast(4)
        if (turns.isNotEmpty()) {
            builder.append("\nRecent conversation context:\n")
            for ((u, a) in turns) {
                builder.append("User: $u\nAisha: $a\n")
            }
        }

        return builder.toString().trim()
    }

    /**
     * Clears conversational history while retaining user identity.
     */
    fun clearHistory() {
        _recentTurns.value = emptyList()
    }
}
