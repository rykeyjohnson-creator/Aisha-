package com.example.action

/**
 * Extension point for Level 3 Android system and application actions.
 * Cleanly decouples intent execution (torch, apps, volume, media) from AI/response engine.
 * For Level 2, provides an empty/ready implementation without fake actions.
 */
interface AishaActionDispatcher {
    fun canHandle(command: String): Boolean
    suspend fun dispatch(command: String): Boolean
}

/**
 * Level 2 default no-op dispatcher preparing the project for Level 3.
 */
class DefaultActionDispatcher : AishaActionDispatcher {
    override fun canHandle(command: String): Boolean {
        // Level 3 will register triggers here (open YouTube, toggle torch, adjust volume, etc.)
        return false
    }

    override suspend fun dispatch(command: String): Boolean {
        // Will execute actual Android intents in Level 3
        return false
    }
}
