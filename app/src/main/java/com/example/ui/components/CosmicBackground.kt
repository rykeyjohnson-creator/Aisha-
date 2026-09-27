package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class CosmicStar(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val baseAlpha: Float,
    val pulseSpeed: Int
)

/**
 * Procedural deep space cosmic background with twinkling stars and subtle nebula glow.
 */
@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier
) {
    val stars = remember {
        val random = Random(42)
        List(45) {
            CosmicStar(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat(),
                radius = random.nextFloat() * 2f + 1f,
                baseAlpha = random.nextFloat() * 0.5f + 0.3f,
                pulseSpeed = random.nextInt(1500, 3500)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starTwinkle")
    val twinkleFactor by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Base deep cosmic space gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF04060E),
                    Color(0xFF070B1B),
                    Color(0xFF0D122B),
                    Color(0xFF060914)
                )
            )
        )

        // 2. Cosmic Nebula glow top-right (soft cyan/blue)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1A00E5FF),
                    Color(0x0800B0FF),
                    Color.Transparent
                ),
                center = Offset(width * 0.85f, height * 0.15f),
                radius = width * 0.65f
            ),
            center = Offset(width * 0.85f, height * 0.15f),
            radius = width * 0.65f
        )

        // 3. Cosmic Nebula glow center-left (soft violet)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x18B388FF),
                    Color(0x067C4DFF),
                    Color.Transparent
                ),
                center = Offset(width * 0.15f, height * 0.45f),
                radius = width * 0.7f
            ),
            center = Offset(width * 0.15f, height * 0.45f),
            radius = width * 0.7f
        )

        // 4. Subtle planets/stars
        stars.forEach { star ->
            val alpha = (star.baseAlpha * twinkleFactor).coerceIn(0.1f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.xRatio * width, star.yRatio * height)
            )
        }
    }
}
