package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.model.*
import kotlin.math.min

@Composable
fun LudoBoard(
    players: List<Player>,
    validTokenIds: List<Int>,
    currentTurnIdx: Int,
    isAnimating: Boolean,
    canMove: Boolean,
    onTokenClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for valid click tokens
    val infiniteTransition = rememberInfiniteTransition(label = "PulsingTokens")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInYellowEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "token_pulsing"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp))
            .border(width = 1.5.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val cellSize = boardSize / 15f

        // 1. Draw Static Board Grid on Canvas Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLudoBoardBackground(cellSize, boardSize)
        }

        // 2. Gather tokens and group them by cell coordinate to resolve overlays
        val tokensByPosition = remember(players) {
            val map = mutableMapOf<CellPosition, MutableList<Token>>()
            for (player in players) {
                if (!player.isActive) continue
                for (token in player.tokens) {
                    val pos = getCellPositionForStep(player.idx, token.step, token.id)
                    map.getOrPut(pos) { mutableListOf() }.add(token)
                }
            }
            map
        }

        // 3. Draw active tokens grouped dynamically
        for ((cellPos, tokenList) in tokensByPosition) {
            val count = tokenList.size
            val cellLeft = cellPos.x * cellSize
            val cellTop = cellPos.y * cellSize

            tokenList.forEachIndexed { num, token ->
                val playerColor = PlayerColors[token.playerIdx]
                val isValidMove = canMove && !isAnimating &&
                        token.playerIdx == currentTurnIdx &&
                        validTokenIds.contains(token.id)

                // Layout sub-offsetting inside shared cell to prevent overlaps
                val (offsetX, offsetY, customSizeRatio) = remember(count, num, cellSize) {
                    when (count) {
                        1 -> {
                            // Centered completely
                            val ts = cellSize * 0.72f
                            Triple((cellSize - ts) / 2f, (cellSize - ts) / 2f, 0.72f)
                        }
                        2 -> {
                            // Split diagonally
                            val ts = cellSize * 0.44f
                            if (num == 0) {
                                Triple(2f, 2f, 0.44f)
                            } else {
                                Triple(cellSize - ts - 2f, cellSize - ts - 2f, 0.44f)
                            }
                        }
                        3 -> {
                            // 3-way triangular split
                            val ts = cellSize * 0.42f
                            when (num) {
                                0 -> Triple((cellSize - ts) / 2f, 2f, 0.42f)
                                1 -> Triple(2f, cellSize - ts - 2f, 0.42f)
                                else -> Triple(cellSize - ts - 2f, cellSize - ts - 2f, 0.42f)
                            }
                        }
                        else -> {
                            // 2x2 Mini-Grid layout for 4 tokens
                            val ts = cellSize * 0.38f
                            when (num) {
                                0 -> Triple(2f, 2f, 0.38f)
                                1 -> Triple(cellSize - ts - 2f, 2f, 0.38f)
                                2 -> Triple(2f, cellSize - ts - 2f, 0.38f)
                                else -> Triple(cellSize - ts - 2f, cellSize - ts - 2f, 0.48f)
                            }
                        }
                    }
                }

                val tokenWidth = (cellSize * customSizeRatio).dp
                val densityValue = LocalDensity.current.density // Local display density

                Box(
                    modifier = Modifier
                        .absoluteOffset(
                            x = ((cellLeft + offsetX) / densityValue).dp,
                            y = ((cellTop + offsetY) / densityValue).dp
                        )
                        .size(tokenWidth)
                        .clip(CircleShape)
                        .scale(if (isValidMove) pulseScale else 1.0f)
                        .background(if (isValidMove) Color.White else playerColor)
                        .border(
                            width = if (isValidMove) 3.dp else 2.dp,
                            color = if (isValidMove) playerColor else Color.White,
                            shape = CircleShape
                        )
                        .shadow(elevation = 3.dp, shape = CircleShape)
                        .clickable(
                            enabled = isValidMove,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTokenClick(token.playerIdx, token.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Small inner eye dot inside pawn token to give tactile visual depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.4f)
                            .clip(CircleShape)
                            .background(if (isValidMove) playerColor else Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }
    }
}

private val FastOutSlowInYellowEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * Draws the master template of Ludo Layouts on Board coordinate boxes.
 */
fun DrawScope.drawLudoBoardBackground(cellSize: Float, boardSize: Float) {
    // 1. Draw grid lanes backdrop (Row 6, 7, 8 and Col 6, 7, 8)
    val lightGray = Color(0xFFEEEEEE)
    val gridBorderCol = Color(0xFF0F172A) // slate-900 border
    val strokeWidth = 1.dp.toPx()

    // Draw all cells as clear slate-800 backgrounds with outlines
    for (x in 0..14) {
        for (y in 0..14) {
            // Corner home bases are handled separately, so only draw track cells
            val isCornerHome = (x < 6 && y < 6) || (x >= 9 && y < 6) || (x < 6 && y >= 9) || (x >= 9 && y >= 9)
            if (!isCornerHome) {
                drawRect(
                    color = Color(0xFF1E293B), // slate-800 cells
                    topLeft = Offset(x * cellSize, y * cellSize),
                    size = Size(cellSize, cellSize)
                )
                drawRect(
                    color = gridBorderCol,
                    topLeft = Offset(x * cellSize, y * cellSize),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }

    // 2. Draw 4 Corner Home Yards
    // Top-Left: Red (0,0 to 6,6)
    drawHomeBase(0f, 0f, cellSize, 0)
    // Top-Right: Green (9,0 to 15,6)
    drawHomeBase(9 * cellSize, 0f, cellSize, 1)
    // Bottom-Right: Yellow (9,9 to 15,15)
    drawHomeBase(9 * cellSize, 9 * cellSize, cellSize, 2)
    // Bottom-Left: Blue (0,9 to 6,15)
    drawHomeBase(0f, 9 * cellSize, cellSize, 3)

    // 3. Draw Colored Home paths leading to center
    // Red home path: row 7, cols 1..5
    for (x in 1..5) {
        drawRect(
            color = PlayerColors[0],
            topLeft = Offset(x * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = gridBorderCol,
            topLeft = Offset(x * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = strokeWidth)
        )
    }
    // Green home path: col 7, rows 1..5
    for (y in 1..5) {
        drawRect(
            color = PlayerColors[1],
            topLeft = Offset(7 * cellSize, y * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = gridBorderCol,
            topLeft = Offset(7 * cellSize, y * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = strokeWidth)
        )
    }
    // Yellow home path: row 7, cols 9..13
    for (x in 9..13) {
        drawRect(
            color = PlayerColors[2],
            topLeft = Offset(x * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = gridBorderCol,
            topLeft = Offset(x * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = strokeWidth)
        )
    }
    // Blue home path: col 7, rows 9..13
    for (y in 9..13) {
        drawRect(
            color = PlayerColors[3],
            topLeft = Offset(7 * cellSize, y * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = gridBorderCol,
            topLeft = Offset(7 * cellSize, y * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = strokeWidth)
        )
    }

    // 4. Draw Start Cell background fills
    // Red start: (1,6)
    drawRect(color = PlayerColors[0], topLeft = Offset(1 * cellSize, 6 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = gridBorderCol, topLeft = Offset(1 * cellSize, 6 * cellSize), size = Size(cellSize, cellSize), style = Stroke(width = strokeWidth))

    // Green start: (8,1)
    drawRect(color = PlayerColors[1], topLeft = Offset(8 * cellSize, 1 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = gridBorderCol, topLeft = Offset(8 * cellSize, 1 * cellSize), size = Size(cellSize, cellSize), style = Stroke(width = strokeWidth))

    // Yellow start: (13,8)
    drawRect(color = PlayerColors[2], topLeft = Offset(13 * cellSize, 8 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = gridBorderCol, topLeft = Offset(13 * cellSize, 8 * cellSize), size = Size(cellSize, cellSize), style = Stroke(width = strokeWidth))

    // Blue start: (6,13)
    drawRect(color = PlayerColors[3], topLeft = Offset(6 * cellSize, 13 * cellSize), size = Size(cellSize, cellSize))
    drawRect(color = gridBorderCol, topLeft = Offset(6 * cellSize, 13 * cellSize), size = Size(cellSize, cellSize), style = Stroke(width = strokeWidth))

    // 5. Draw Star icons on Star Safe spots (Star cells: (6,3), (11,6), (8,11), (3,8)) and colored start boxes
    val stars_list = listOf(
        Pair(6, 3), Pair(11, 6), Pair(8, 11), Pair(3, 8), // Star safe zones
        Pair(1, 6), Pair(8, 1), Pair(13, 8), Pair(6, 13)  // Start safe zones
    )
    for (star in stars_list) {
        val starCenterX = star.first * cellSize + cellSize / 2f
        val starCenterY = star.second * cellSize + cellSize / 2f
        // Draw standard star
        drawLudoStar(
            centerX = starCenterX,
            centerY = starCenterY,
            radius = cellSize * 0.35f,
            color = if (stars_list.indexOf(star) < 4) Color(0xFF94A3B8) else Color.White
        )
    }

    // 6. Draw Center converging Triangles (6,6) to (9,9)
    val mid = boardSize / 2f
    val cLeft = 6 * cellSize
    val cTop = 6 * cellSize
    val cRight = 9 * cellSize
    val cBottom = 9 * cellSize

    // Red Center Triangle (Left)
    val redPath = Path().apply {
        moveTo(cLeft, cTop)
        lineTo(mid, mid)
        lineTo(cLeft, cBottom)
        close()
    }
    drawPath(redPath, PlayerColors[0])

    // Green Center Triangle (Top)
    val greenPath = Path().apply {
        moveTo(cLeft, cTop)
        lineTo(mid, mid)
        lineTo(cRight, cTop)
        close()
    }
    drawPath(greenPath, PlayerColors[1])

    // Yellow Center Triangle (Right)
    val yellowPath = Path().apply {
        moveTo(cRight, cTop)
        lineTo(mid, mid)
        lineTo(cRight, cBottom)
        close()
    }
    drawPath(yellowPath, PlayerColors[2])

    // Blue Center Triangle (Bottom)
    val bluePath = Path().apply {
        moveTo(cLeft, cBottom)
        lineTo(mid, mid)
        lineTo(cRight, cBottom)
        close()
    }
    drawPath(bluePath, PlayerColors[3])

    // Draw dark border around center cross triangle convergence
    val centerLinePath = Path().apply {
        moveTo(cLeft, cTop)
        lineTo(cRight, cBottom)
        moveTo(cRight, cTop)
        lineTo(cLeft, cBottom)
    }
    drawPath(centerLinePath, color = gridBorderCol, style = Stroke(width = strokeWidth * 2))
    drawRect(color = gridBorderCol, topLeft = Offset(cLeft, cTop), size = Size(3 * cellSize, 3 * cellSize), style = Stroke(width = strokeWidth * 2))
}

/**
 * Draws a beautiful 6x6 player Home base square with circular yard tokens slots.
 */
fun DrawScope.drawHomeBase(left: Float, top: Float, cellSize: Float, playerIdx: Int) {
    val color = PlayerColors[playerIdx]
    val interiorColor = PlayerInteriorColors[playerIdx]

    // Background fill of base card
    drawRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(cellSize * 6f, cellSize * 6f)
    )

    // Inner Deep Card nested nicely
    val margin = cellSize * 0.8f
    val whiteCardSize = cellSize * 4.4f
    drawRoundRect(
        color = interiorColor,
        topLeft = Offset(left + margin, top + margin),
        size = Size(whiteCardSize, whiteCardSize),
        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    )

    // Yard slot coordinates for tokens sitting inside White Card
    val slotOffsets = listOf(
        Offset(left + cellSize * 1.5f + cellSize / 2f, top + cellSize * 1.5f + cellSize / 2f),
        Offset(left + cellSize * 3.5f + cellSize / 2f, top + cellSize * 1.5f + cellSize / 2f),
        Offset(left + cellSize * 1.5f + cellSize / 2f, top + cellSize * 3.5f + cellSize / 2f),
        Offset(left + cellSize * 3.5f + cellSize / 2f, top + cellSize * 3.5f + cellSize / 2f)
    )

    // Draw circular Slots inside home bases to host inactive pawns with glowing highlights
    for (offset in slotOffsets) {
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = cellSize * 0.55f,
            center = offset
        )
        drawCircle(
            color = color,
            radius = cellSize * 0.55f,
            center = offset,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

/**
 * Custom 5-Point star painter on canvas.
 */
fun DrawScope.drawLudoStar(centerX: Float, centerY: Float, radius: Float, color: Color) {
    val path = Path().apply {
        val angle = Math.PI / 5
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) radius else radius * 0.4f
            val x = centerX + r * Math.sin(i * angle).toFloat()
            val y = centerY - r * Math.cos(i * angle).toFloat()
            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }
    drawPath(path, color = color)
}
