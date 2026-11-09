package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantType
import com.example.ui.theme.KingGlow
import com.example.ui.theme.KingPrimary
import com.example.ui.theme.KingSecondary
import com.example.ui.theme.MayaGlow
import com.example.ui.theme.MayaPrimary
import com.example.ui.theme.MayaSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisAnimatedSphere(
    assistant: AssistantType,
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_sphere")

    // Slow continuous rotation for outer rings
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    // Counter rotation for inner ring
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    // Breathing pulse for idle state
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // High energy pulse when speaking or listening
    val activeWave by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 450 else 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeWave"
    )

    val primaryColor = if (assistant == AssistantType.KING) KingPrimary else MayaPrimary
    val secondaryColor = if (assistant == AssistantType.KING) KingSecondary else MayaSecondary
    val glowColor = if (assistant == AssistantType.KING) KingGlow else MayaGlow

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = size / 2),
                role = Role.Button,
                onClick = onClick
            )
            .testTag("jarvis_animated_sphere"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.85f

            // Dynamic scale factor based on activity
            val dynamicScale = when {
                isListening -> (1.0f + rmsLevel * 0.35f)
                isSpeaking -> activeWave
                isThinking -> breathingPulse * 1.08f
                else -> breathingPulse
            }

            val currentRadius = baseRadius * dynamicScale

            // Outer Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isListening || isSpeaking) 0.35f else 0.15f),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.35f
                ),
                radius = currentRadius * 1.35f,
                center = center
            )

            // Outer Segmented Orbit Ring 1 (Forward Rotation)
            val dashSegments = PathEffect.dashPathEffect(floatArrayOf(40f, 25f, 15f, 25f), rotation1 * 2f)
            drawCircle(
                color = primaryColor.copy(alpha = 0.6f),
                radius = currentRadius * 1.05f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = dashSegments, cap = StrokeCap.Round)
            )

            // Inner Segmented Ring 2 (Counter Rotation)
            val innerDash = PathEffect.dashPathEffect(floatArrayOf(25f, 20f), rotation2 * 2.5f)
            drawCircle(
                color = secondaryColor.copy(alpha = 0.5f),
                radius = currentRadius * 0.88f,
                center = center,
                style = Stroke(width = 1.8.dp.toPx(), pathEffect = innerDash)
            )

            // Soundwave Radar Arcs when listening or speaking
            if (isListening || isSpeaking) {
                val waveCount = 8
                val angleStep = 360f / waveCount
                for (i in 0 until waveCount) {
                    val angle = (i * angleStep + rotation1) * (Math.PI / 180.0)
                    val waveLength = 12.dp.toPx() + (rmsLevel * 25.dp.toPx())
                    val start = Offset(
                        (center.x + (currentRadius * 0.88f) * cos(angle)).toFloat(),
                        (center.y + (currentRadius * 0.88f) * sin(angle)).toFloat()
                    )
                    val end = Offset(
                        (center.x + (currentRadius * 0.88f + waveLength) * cos(angle)).toFloat(),
                        (center.y + (currentRadius * 0.88f + waveLength) * sin(angle)).toFloat()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.8f),
                        start = start,
                        end = end,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Central Glowing AI Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor,
                        secondaryColor,
                        glowColor.copy(alpha = 0.4f)
                    ),
                    center = center,
                    radius = currentRadius * 0.55f
                ),
                radius = currentRadius * 0.55f,
                center = center
            )

            // Core Concentric Precision Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = currentRadius * 0.28f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Center Singularity Point
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx() * (if (isListening) 1.5f else 1f),
                center = center
            )

            // Orbital Data Nodes
            val nodeCount = 3
            for (i in 0 until nodeCount) {
                val nodeAngle = ((i * 120f) + rotation1) * (Math.PI / 180.0)
                val nodePos = Offset(
                    (center.x + currentRadius * cos(nodeAngle)).toFloat(),
                    (center.y + currentRadius * sin(nodeAngle)).toFloat()
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = nodePos
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.5.dp.toPx(),
                    center = nodePos,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}
