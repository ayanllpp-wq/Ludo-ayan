package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun DiceView(
    diceValue: Int,
    diceState: DiceState,
    canRoll: Boolean,
    onRollClick: () -> Unit,
    playerIdx: Int,
    modifier: Modifier = Modifier
) {
    val activePlayerColor = PlayerColors[playerIdx]
    val activePlayerInteriorColor = PlayerInteriorColors[playerIdx]

    // Spin transitions when rolling
    val infiniteTransition = rememberInfiniteTransition(label = "DiceRotation")
    
    val rotationAngle by if (diceState == DiceState.ROLLING) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dice_rot"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val bounceScale by if (diceState == DiceState.ROLLING) {
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(200, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dice_bounce"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Column(
        modifier = modifier
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Explaining text
        Text(
            text = when {
                canRoll && diceState == DiceState.IDLE -> "TAP DICE TO ROLL"
                diceState == DiceState.ROLLING -> "ROLLING..."
                else -> "ROLLED: $diceValue"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (canRoll) activePlayerColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 1.2.sp
        )

        // Rounded Dice Box Canvas Container
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(bounceScale)
                .rotate(rotationAngle)
                .shadow(
                    elevation = if (canRoll) 10.dp else 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White, Color(0xFFF0F0F0))
                    )
                )
                .clickable(enabled = canRoll && diceState != DiceState.ROLLING) {
                    onRollClick()
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw Dice face dots on Custom Canvas
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val dotRadius = w * 0.08f
                val padding = w * 0.22f

                // Draw standard red/colored core border around die face for premium styling
                drawRoundRect(
                    color = activePlayerColor.copy(alpha = 0.4f),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Dot Coordinate Maps
                val dots = when (diceValue) {
                    1 -> listOf(Offset(w / 2, h / 2))
                    2 -> listOf(
                        Offset(padding, padding),
                        Offset(w - padding, h - padding)
                    )
                    3 -> listOf(
                        Offset(padding, padding),
                        Offset(w / 2, h / 2),
                        Offset(w - padding, h - padding)
                    )
                    4 -> listOf(
                        Offset(padding, padding),
                        Offset(w - padding, padding),
                        Offset(padding, h - padding),
                        Offset(w - padding, h - padding)
                    )
                    5 -> listOf(
                        Offset(padding, padding),
                        Offset(w - padding, padding),
                        Offset(w / 2, h / 2),
                        Offset(padding, h - padding),
                        Offset(w - padding, h - padding)
                    )
                    else -> listOf( // 6
                        Offset(padding, padding),
                        Offset(w - padding, padding),
                        Offset(padding, h / 2),
                        Offset(w - padding, h / 2),
                        Offset(padding, h - padding),
                        Offset(w - padding, h - padding)
                    )
                }

                // Draw dots
                for (dot in dots) {
                    drawCircle(
                        color = if (diceValue == 6) activePlayerColor else Color.DarkGray,
                        radius = dotRadius,
                        center = dot
                    )
                }
            }
        }

        // Accessibility Button block
        Button(
            onClick = onRollClick,
            enabled = canRoll && diceState != DiceState.ROLLING,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = activePlayerColor,
                contentColor = activePlayerInteriorColor,
                disabledContainerColor = Color(0xFF1C1B1F),
                disabledContentColor = Color(0xFF94A3B8).copy(alpha = 0.5f)
            ),
            modifier = Modifier.height(42.dp)
        ) {
            Text(
                text = "ROLL DICE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
