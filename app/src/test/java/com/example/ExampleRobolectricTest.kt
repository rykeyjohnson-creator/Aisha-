package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.AishaAssistant
import com.example.assistant.CommandNormalizer
import com.example.memory.MemoryManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aisha", appName)
    }

    @Test
    fun `verify command normalizer`() {
        assertEquals("hey aisha", CommandNormalizer.sanitize("  HEY AISHA!!!  "))
        assertEquals("what time is it", CommandNormalizer.extractCoreCommand("Hey Aisha what time is it?"))
        assertEquals("aisha", CommandNormalizer.extractCoreCommand("Aisha"))
        assertEquals("kitne baje hain", CommandNormalizer.extractCoreCommand("Aisha kitne baje hain"))
        assertTrue(CommandNormalizer.isHindiQuery("aisha tum kaun ho"))
        assertTrue(CommandNormalizer.isHindiQuery("kitne baje hain"))
    }

    @Test
    fun `verify aisha assistant level 1 and level 2 responses`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryManager = MemoryManager(context)
        val assistant = AishaAssistant(memoryManager = memoryManager)

        // 1. Level 1 Greetings
        assertEquals("Hello! I'm Aisha. How can I help you?", assistant.process("hi").text)
        assertEquals("Hello! I'm Aisha. How can I help you?", assistant.process("hello").text)
        assertEquals("Hello! I'm Aisha. How can I help you?", assistant.process("hi aisha").text)

        // 2. Identity
        assertEquals("I'm Aisha, your AI voice assistant.", assistant.process("who are you").text)
        assertEquals("My name is Aisha.", assistant.process("what is your name").text)
        assertEquals("I'm ready to help.", assistant.process("how are you").text)

        // 3. Time & Date (Dynamic)
        val timeResponse = assistant.process("what time is it").text
        assertTrue(timeResponse.startsWith("The current time is "))

        val dateResponse = assistant.process("what is today's date").text
        assertTrue(dateResponse.startsWith("Today is "))

        // 4. Hindi & Hinglish
        val hindiWho = assistant.process("aisha tum kaun ho").text
        assertEquals("Main Aisha hoon, aapki AI voice assistant.", hindiWho)

        val hindiTime = assistant.process("kitne baje hain").text
        assertTrue(hindiTime.startsWith("Abhi samay hai "))

        val hindiDate = assistant.process("aaj ki date kya hai").text
        assertTrue(hindiDate.startsWith("Aaj ki tareekh hai "))

        // 5. Level 2 Memory: User Name
        val nameLearnResponse = assistant.process("My name is Rahul").text
        assertTrue(nameLearnResponse.contains("Rahul"))

        val nameRecallResponse = assistant.process("What is my name?").text
        assertEquals("Your name is Rahul.", nameRecallResponse)

        // 6. Level 2 Memory: Preferences
        val prefLearnResponse = assistant.process("I like space").text
        assertTrue(prefLearnResponse.contains("space"))

        val prefRecallResponse = assistant.process("What do I like?").text
        assertTrue(prefRecallResponse.contains("space"))

        // 7. Context prompt building
        val contextPrompt = memoryManager.buildContextPrompt()
        assertTrue(contextPrompt.contains("Rahul"))
        assertTrue(contextPrompt.contains("space"))
    }

    @Test
    fun `verify MemoryManager preferences and recent dialogue persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryManager = MemoryManager(context)

        // Test preferences save and retrieve
        memoryManager.savePreference("favorite_language", "Kotlin")
        memoryManager.savePreference("music_genre", "Synthwave")

        assertEquals("Kotlin", memoryManager.getPreference("favorite_language"))
        assertEquals("Synthwave", memoryManager.getPreference("music_genre"))
        assertEquals(2, memoryManager.getAllPreferences().size)

        // Test dialogue pair saving and retrieving
        memoryManager.saveDialoguePair("Explain quantum computing", "Quantum computing uses qubits...")
        memoryManager.saveDialoguePair("Is it faster?", "For specific algorithms, yes.")

        val pairs = memoryManager.getRecentDialoguePairs()
        assertEquals(2, pairs.size)
        assertEquals("Explain quantum computing", pairs[0].first)
        assertEquals("Quantum computing uses qubits...", pairs[0].second)

        // Test context prompt inclusion
        val prompt = memoryManager.buildContextPrompt()
        assertTrue(prompt.contains("favorite_language: Kotlin"))
        assertTrue(prompt.contains("music_genre: Synthwave"))
        assertTrue(prompt.contains("Explain quantum computing"))
        assertTrue(prompt.contains("Quantum computing uses qubits..."))
    }
}
