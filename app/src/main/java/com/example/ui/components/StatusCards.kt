package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiStatus
import com.example.model.AssistantState
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted

/**
 * Three small premium futuristic glass status cards:
 * VOICE / READY (or ACTIVE), AI / (ONLINE, THINKING, OFFLINE, READY), SYSTEM / READY
 */
@Composable
fun StatusCards(
    assistantState: AssistantState,
    aiStatus: AiStatus,
    modifier: Modifier = Modifier
) {
    val voiceStatus = when (assistantState) {
        AssistantState.READY, AssistantState.ERROR -> "READY"
        AssistantState.LISTENING -> "LISTENING"
        AssistantState.PROCESSING, AssistantState.THINKING -> "PROCESSING"
        AssistantState.SPEAKING -> "SPEAKING"
    }

    val voiceColor = when (assistantState) {
        AssistantState.READY, AssistantState.ERROR -> GlowGreen
        AssistantState.LISTENING -> NeonCyan
        AssistantState.PROCESSING, AssistantState.THINKING -> NeonViolet
        AssistantState.SPEAKING -> NeonCyan
    }

    val aiDisplayStatus = when (aiStatus) {
        AiStatus.ONLINE -> "ONLINE"
        AiStatus.THINKING -> "THINKING"
        AiStatus.READY -> "READY"
        AiStatus.OFFLINE -> "OFFLINE"
        AiStatus.ERROR -> "ERROR"
    }

    val aiColor = when (aiStatus) {
        AiStatus.ONLINE -> NeonViolet
        AiStatus.THINKING -> NeonCyan
        AiStatus.READY -> GlowGreen
        AiStatus.OFFLINE -> Color(0xFFEF9A9A)
        AiStatus.ERROR -> Color(0xFFFF8A80)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatusCardItem(
            modifier = Modifier.weight(1f),
            title = "VOICE",
            status = voiceStatus,
            accentColor = voiceColor
        )
        StatusCardItem(
            modifier = Modifier.weight(1f),
            title = "AI BRAIN",
            status = aiDisplayStatus,
            accentColor = aiColor
        )
        StatusCardItem(
            modifier = Modifier.weight(1f),
            title = "SYSTEM",
            status = "READY",
            accentColor = GlowGreen
        )
    }
}

@Composable
private fun StatusCardItem(
    title: String,
    status: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0E152B))
            .border(1.dp, Color(0x333D7BFF), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}
