package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Top header containing dynamic greeting, title, live digital clock,
 * dynamic date, and animated assistant status badge.
 */
@Composable
fun AishaHeader(
    assistantState: AssistantState,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var dynamicGreeting by remember { mutableStateOf("Good evening,") }

    // Live clock ticker - updates every second
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            dynamicGreeting = when (hour) {
                in 5..11 -> "Good morning,"
                in 12..16 -> "Good afternoon,"
                in 17..21 -> "Good evening,"
                else -> "Good night,"
            }
            delay(1000L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic greeting
        Text(
            text = dynamicGreeting,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextSecondary,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Title Aisha ✦
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Aisha",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 32.sp,
                    letterSpacing = 1.5.sp
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✦",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = NeonCyan,
                    fontSize = 24.sp
                )
            )
        }

        Text(
            text = "Your AI Voice Assistant",
            style = MaterialTheme.typography.labelMedium.copy(
                color = NeonViolet,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Clock & Date in futuristic pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x8010162B))
                .border(1.dp, Color(0x402979FF), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
            Text(
                text = "  •  ",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )
            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Assistant Status Indicator
        StatusIndicatorBadge(state = assistantState)
    }
}

@Composable
private fun StatusIndicatorBadge(state: AssistantState) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusDotPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val (badgeBg, borderColor, dotColor) = when (state) {
        AssistantState.READY, AssistantState.ERROR -> Triple(Color(0x2200E676), Color(0x5500E676), GlowGreen)
        AssistantState.LISTENING -> Triple(Color(0x3300E5FF), Color(0x8800E5FF), NeonCyan)
        AssistantState.PROCESSING -> Triple(Color(0x33FFD600), Color(0x88FFD600), GlowAmber)
        AssistantState.THINKING -> Triple(Color(0x33B388FF), Color(0x88B388FF), NeonViolet)
        AssistantState.SPEAKING -> Triple(Color(0x33FF4081), Color(0x88FF4081), NeonMagenta)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(badgeBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (state == AssistantState.READY) 1f else dotAlpha))
            )

            Spacer(modifier = Modifier.width(8.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "statusLabelAnim"
            ) { targetState ->
                Text(
                    text = targetState.statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = dotColor,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
