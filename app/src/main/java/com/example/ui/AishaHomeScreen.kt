package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AiStatus
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.ui.components.AishaCharacterView
import com.example.ui.components.AishaHeader
import com.example.ui.components.ConversationCard
import com.example.ui.components.CosmicBackground
import com.example.ui.components.StatusCards
import com.example.ui.components.VoiceInputBar

/**
 * Main futuristic Aisha home screen composable.
 * Level 2 features:
 * - Real-time AI Brain status (ONLINE, THINKING, OFFLINE, READY)
 * - Persistent short-term conversational memory with instant context recall
 * - Full speech recognition & synthesis, quick commands, dynamic clock & date
 */
@Composable
fun AishaHomeScreen(
    assistantState: AssistantState,
    aiStatus: AiStatus,
    messages: List<ChatMessage>,
    rmsdB: Float,
    snackbarHostState: SnackbarHostState,
    onMicClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cosmic deep space environment
            CosmicBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Scrollable main content container for smaller phones & keyboard avoidance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Top Section (Greeting, Aisha ✦, Live Clock, Date, Status badge)
                    AishaHeader(assistantState = assistantState)

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Status Cards (VOICE, AI BRAIN, SYSTEM)
                    StatusCards(
                        assistantState = assistantState,
                        aiStatus = aiStatus
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Central Aisha Animated Hologram Character Area
                    AishaCharacterView(
                        state = assistantState,
                        rmsdB = rmsdB
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Conversation History & Quick Command Buttons ("Hi", "Time", "Date")
                    ConversationCard(
                        messages = messages,
                        onQuickCommand = onQuickCommand
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 5. Fixed Bottom Voice & Text Input Section
                VoiceInputBar(
                    assistantState = assistantState,
                    onMicClick = onMicClick,
                    onSendMessage = onSendMessage
                )
            }
        }
    }
}
