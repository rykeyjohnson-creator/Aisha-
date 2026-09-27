package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Conversation card displaying user commands and Aisha's responses,
 * along with quick command buttons ("Hi", "Time", "Date").
 */
@Composable
fun ConversationCard(
    messages: List<ChatMessage>,
    onQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to the latest message whenever new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Quick Command Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickCommandButton(
                title = "Hi",
                icon = Icons.Rounded.WavingHand,
                contentDescription = stringResource(R.string.quick_hi_desc),
                onClick = { onQuickCommand("Hi") },
                modifier = Modifier.weight(1f)
            )
            QuickCommandButton(
                title = "Time",
                icon = Icons.Rounded.AccessTime,
                contentDescription = stringResource(R.string.quick_time_desc),
                onClick = { onQuickCommand("Time") },
                modifier = Modifier.weight(1f)
            )
            QuickCommandButton(
                title = "Date",
                icon = Icons.Rounded.CalendarToday,
                contentDescription = stringResource(R.string.quick_date_desc),
                onClick = { onQuickCommand("Date") },
                modifier = Modifier.weight(1f)
            )
        }

        // Conversation Glass Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp, max = 170.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE0E152F),
                            Color(0xEE090D20)
                        )
                    )
                )
                .border(1.dp, Color(0x383D7BFF), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            if (messages.isEmpty()) {
                // Empty state greeting
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✦ Aisha is standing by",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = NeonCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the microphone or type below to begin.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageItem(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val roleColor = if (isUser) NeonCyan else NeonViolet
    val roleTitle = if (isUser) "User:" else "Aisha ✦:"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isUser) Color(0x2200E5FF) else Color(0x22B388FF))
            .border(
                1.dp,
                if (isUser) Color(0x3300E5FF) else Color(0x33B388FF),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = roleTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = roleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                fontSize = 13.5.sp,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun QuickCommandButton(
    title: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA111A38))
            .border(1.dp, Color(0x4000E5FF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("quick_${title.lowercase()}")
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            )
        }
    }
}
