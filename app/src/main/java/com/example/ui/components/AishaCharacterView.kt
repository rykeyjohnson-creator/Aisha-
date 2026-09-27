package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Large central Aisha character area featuring a futuristic sci-fi female AI avatar,
 * surrounded by glowing holographic rings, responsive audio soundwaves, and state-driven animations.
 */
@Composable
fun AishaCharacterView(
    state: AssistantState,
    rmsdB: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aishaCharacterAnim")

    // Idle breathing pulse
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathing"
    )

    // Orbital ring slow rotation
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    // Speaking wave oscillation
    val speakingWave by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingWave"
    )

    // Fast rotation for processing
    val processingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "processingRotation"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.42f

            // Effective scale depending on assistant state
            val scale = when (state) {
                AssistantState.READY, AssistantState.ERROR -> idleBreathing
                AssistantState.LISTENING -> {
                    // React dynamically to speech input RMS dB or default listening expansion
                    val audioBoost = (rmsdB.coerceIn(0f, 10f) / 10f) * 0.15f
                    1.05f + audioBoost
                }
                AssistantState.PROCESSING, AssistantState.THINKING -> 1.0f
                AssistantState.SPEAKING -> speakingWave
            }

            // Glow color depending on state
            val primaryGlowColor = when (state) {
                AssistantState.READY, AssistantState.ERROR -> NeonCyan
                AssistantState.LISTENING -> NeonCyan
                AssistantState.PROCESSING, AssistantState.THINKING -> NeonViolet
                AssistantState.SPEAKING -> NeonMagenta
            }

            val secondaryGlowColor = when (state) {
                AssistantState.READY, AssistantState.ERROR -> NeonViolet
                AssistantState.LISTENING -> Color(0xFF00B0FF)
                AssistantState.PROCESSING, AssistantState.THINKING -> NeonCyan
                AssistantState.SPEAKING -> NeonViolet
            }

            // 1. Ambient outer aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = 0.28f * scale),
                        secondaryGlowColor.copy(alpha = 0.12f * scale),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f * scale
                ),
                radius = baseRadius * 1.45f * scale,
                center = center
            )

            // 2. Outer holographic orbit rings with tick marks
            val currentRotation = if (state == AssistantState.PROCESSING || state == AssistantState.THINKING) processingRotation else orbitRotation
            rotate(degrees = currentRotation, pivot = center) {
                // Outer ring
                drawCircle(
                    color = primaryGlowColor.copy(alpha = 0.4f),
                    radius = baseRadius * 1.18f * scale,
                    center = center,
                    style = Stroke(width = 1.8f)
                )

                // Arc segments (cybernetic tech markings)
                val arcRadius = baseRadius * 1.25f * scale
                drawArc(
                    color = primaryGlowColor,
                    startAngle = 15f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = secondaryGlowColor,
                    startAngle = 195f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            // 3. Counter-rotating inner tech ring
            rotate(degrees = -currentRotation * 0.7f, pivot = center) {
                val innerArcRadius = baseRadius * 1.08f * scale
                drawArc(
                    color = secondaryGlowColor.copy(alpha = 0.5f),
                    startAngle = 100f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerArcRadius, center.y - innerArcRadius),
                    size = Size(innerArcRadius * 2, innerArcRadius * 2),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = secondaryGlowColor.copy(alpha = 0.5f),
                    startAngle = 280f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerArcRadius, center.y - innerArcRadius),
                    size = Size(innerArcRadius * 2, innerArcRadius * 2),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }

            // 4. Central dark glass sphere with gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF141D3B),
                        Color(0xFF090E20),
                        Color(0xFF050714)
                    ),
                    center = center,
                    radius = baseRadius * 0.95f
                ),
                radius = baseRadius * 0.95f,
                center = center
            )

            // Inner boundary glowing ring
            drawCircle(
                color = primaryGlowColor.copy(alpha = 0.75f),
                radius = baseRadius * 0.95f,
                center = center,
                style = Stroke(width = 2f)
            )

            // 5. Stylized Holographic Female AI Avatar (Aisha)
            // Cybernetic celestial feminine silhouette
            val headRadius = baseRadius * 0.32f
            val headCenter = Offset(center.x, center.y - baseRadius * 0.18f)

            // Flowing holographic hair / cybernetic veil
            val hairPath = Path().apply {
                moveTo(headCenter.x - headRadius * 1.4f, headCenter.y + headRadius * 0.8f)
                cubicTo(
                    headCenter.x - headRadius * 1.6f, headCenter.y - headRadius * 1.2f,
                    headCenter.x - headRadius * 0.5f, headCenter.y - headRadius * 1.8f,
                    headCenter.x, headCenter.y - headRadius * 1.8f
                )
                cubicTo(
                    headCenter.x + headRadius * 0.5f, headCenter.y - headRadius * 1.8f,
                    headCenter.x + headRadius * 1.6f, headCenter.y - headRadius * 1.2f,
                    headCenter.x + headRadius * 1.4f, headCenter.y + headRadius * 0.8f
                )
                cubicTo(
                    headCenter.x + headRadius * 1.1f, headCenter.y + headRadius * 1.8f,
                    headCenter.x + headRadius * 0.6f, headCenter.y + headRadius * 2.2f,
                    headCenter.x, headCenter.y + headRadius * 2.2f
                )
                cubicTo(
                    headCenter.x - headRadius * 0.6f, headCenter.y + headRadius * 2.2f,
                    headCenter.x - headRadius * 1.1f, headCenter.y + headRadius * 1.8f,
                    headCenter.x - headRadius * 1.4f, headCenter.y + headRadius * 0.8f
                )
                close()
            }
            drawPath(
                path = hairPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        secondaryGlowColor.copy(alpha = 0.45f),
                        primaryGlowColor.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )

            // Face silhouette oval
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0F7FA),
                        Color(0xFF80DEEA),
                        Color(0xFF00ACC1)
                    )
                ),
                topLeft = Offset(headCenter.x - headRadius * 0.7f, headCenter.y - headRadius * 0.9f),
                size = Size(headRadius * 1.4f, headRadius * 1.8f)
            )

            // Glowing cybernetic eyes
            val eyeOffsetY = headCenter.y - headRadius * 0.1f
            val eyeOffsetX = headRadius * 0.35f
            // Left eye
            drawCircle(
                color = primaryGlowColor,
                radius = headRadius * 0.16f,
                center = Offset(headCenter.x - eyeOffsetX, eyeOffsetY)
            )
            drawCircle(
                color = Color.White,
                radius = headRadius * 0.08f,
                center = Offset(headCenter.x - eyeOffsetX, eyeOffsetY)
            )
            // Right eye
            drawCircle(
                color = primaryGlowColor,
                radius = headRadius * 0.16f,
                center = Offset(headCenter.x + eyeOffsetX, eyeOffsetY)
            )
            drawCircle(
                color = Color.White,
                radius = headRadius * 0.08f,
                center = Offset(headCenter.x + eyeOffsetX, eyeOffsetY)
            )

            // Celestial Aisha Star ✦ on the forehead / tiara
            val starCenter = Offset(headCenter.x, headCenter.y - headRadius * 0.65f)
            val starSize = headRadius * 0.28f
            val starPath = Path().apply {
                moveTo(starCenter.x, starCenter.y - starSize)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x + starSize, starCenter.y)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x, starCenter.y + starSize)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x - starSize, starCenter.y)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x, starCenter.y - starSize)
                close()
            }
            drawPath(
                path = starPath,
                color = Color.White
            )

            // Futuristic shoulder / collar lines
            val shoulderPath = Path().apply {
                moveTo(headCenter.x - baseRadius * 0.7f, center.y + baseRadius * 0.75f)
                cubicTo(
                    headCenter.x - baseRadius * 0.35f, center.y + baseRadius * 0.5f,
                    headCenter.x + baseRadius * 0.35f, center.y + baseRadius * 0.5f,
                    headCenter.x + baseRadius * 0.7f, center.y + baseRadius * 0.75f
                )
            }
            drawPath(
                path = shoulderPath,
                color = primaryGlowColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // 6. Soundwave visualization rings when speaking or listening
            if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
                val numWaves = 8
                for (i in 0 until numWaves) {
                    val angle = (i * (360f / numWaves)) * (Math.PI / 180f).toFloat()
                    val waveHeight = if (state == AssistantState.SPEAKING) {
                        baseRadius * (0.12f * sin(i * 1.5f + speakingWave * 5f))
                    } else {
                        baseRadius * (0.08f + (rmsdB.coerceIn(0f, 8f) / 8f) * 0.08f)
                    }
                    val startR = baseRadius * 0.98f
                    val endR = startR + waveHeight

                    val p1 = Offset(center.x + startR * cos(angle), center.y + startR * sin(angle))
                    val p2 = Offset(center.x + endR * cos(angle), center.y + endR * sin(angle))

                    drawLine(
                        color = primaryGlowColor.copy(alpha = 0.85f),
                        start = p1,
                        end = p2,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
