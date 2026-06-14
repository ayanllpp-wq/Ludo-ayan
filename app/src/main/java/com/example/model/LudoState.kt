package com.example.model

import androidx.compose.ui.graphics.Color

enum class PlayerType {
    HUMAN, BOT, DISABLED
}

enum class DiceState {
    IDLE, ROLLING, ROLLED
}

// 4 players colors
val PlayerColors = listOf(
    Color(0xFFFFB4AB), // 0: Red (M3 Peach/Light Red)
    Color(0xFFB4F0AD), // 1: Green (M3 Mint/Light Green)
    Color(0xFFB9F38D), // 2: Yellow (M3 Lime/Yellow-Green)
    Color(0xFFD1E4FF)  // 3: Blue (M3 Ice Blue)
)

val PlayerInteriorColors = listOf(
    Color(0xFF690005), // 0: Deep Red
    Color(0xFF00390A), // 1: Deep Green
    Color(0xFF243600), // 2: Deep Yellow-Green
    Color(0xFF00315C)  // 3: Deep Blue
)


val PlayerNames = listOf("Red", "Green", "Yellow", "Blue")

data class Token(
    val id: Int,
    val playerIdx: Int,
    val step: Int = 0 // 0 = Yard, 1..51 = Common Track, 52..56 = Home Path, 57 = Finished Center
) {
    val isFinished: Boolean get() = step == 57
    val isInYard: Boolean get() = step == 0
}

data class Player(
    val idx: Int,
    val name: String,
    val type: PlayerType,
    val color: Color,
    val tokens: List<Token> = List(4) { id -> Token(id, idx) }
) {
    val isFinished: Boolean get() = tokens.all { it.isFinished }
    val isActive: Boolean get() = type != PlayerType.DISABLED
}

data class CellPosition(val x: Int, val y: Int)

// 52 common track coordinate positions clockwise
val trackCoordinates = listOf(
    CellPosition(0, 6), CellPosition(1, 6), CellPosition(2, 6), CellPosition(3, 6), CellPosition(4, 6), CellPosition(5, 6), // 0..5 (Left lane top)
    CellPosition(6, 5), CellPosition(6, 4), CellPosition(6, 3), CellPosition(6, 2), CellPosition(6, 1), CellPosition(6, 0), // 6..11 (Top lane left)
    CellPosition(7, 0), // 12 (Top center)
    CellPosition(8, 0), CellPosition(8, 1), CellPosition(8, 2), CellPosition(8, 3), CellPosition(8, 4), CellPosition(8, 5), // 13..18 (Top lane right)
    CellPosition(9, 6), CellPosition(10, 6), CellPosition(11, 6), CellPosition(12, 6), CellPosition(13, 6), CellPosition(14, 6), // 19..24 (Right lane top)
    CellPosition(14, 7), // 25 (Right center)
    CellPosition(14, 8), CellPosition(13, 8), CellPosition(12, 8), CellPosition(11, 8), CellPosition(10, 8), CellPosition(9, 8), // 26..31 (Right lane bottom)
    CellPosition(8, 9), CellPosition(8, 10), CellPosition(8, 11), CellPosition(8, 12), CellPosition(8, 13), CellPosition(8, 14), // 32..37 (Bottom lane right)
    CellPosition(7, 14), // 38 (Bottom center)
    CellPosition(6, 14), CellPosition(6, 13), CellPosition(6, 12), CellPosition(6, 11), CellPosition(6, 10), CellPosition(6, 9), // 39..44 (Bottom lane left)
    CellPosition(5, 8), CellPosition(4, 8), CellPosition(3, 8), CellPosition(2, 8), CellPosition(1, 8), CellPosition(0, 8), // 45..50 (Left lane bottom)
    CellPosition(0, 7) // 51 (Left center)
)

// Safe points on the track (starts and designated stars)
val safeTrackIndices = setOf(
    1,  // Red Start: (1,6)
    8,  // Star 1: (6,3)
    14, // Green Start: (8,1)
    21, // Star 2: (11,6)
    27, // Yellow Start: (13,8)
    34, // Star 3: (8,11)
    40, // Blue Start: (6,13)
    47  // Star 4: (3,8)
)

/**
 * Returns the 15x15 cell coordinates (x,y) on the board for a player's token at a given step.
 * tokenId is used to distribute tokens in the yard and center to avoid exact overlays.
 */
fun getCellPositionForStep(playerIdx: Int, step: Int, tokenId: Int = 0): CellPosition {
    if (step == 0) {
        // Yard positions mapped beautifully inside 6x6 homes
        return when (playerIdx) {
            0 -> when (tokenId) { // Red top-left
                0 -> CellPosition(1, 1)
                1 -> CellPosition(4, 1)
                2 -> CellPosition(1, 4)
                else -> CellPosition(4, 4)
            }
            1 -> when (tokenId) { // Green top-right
                0 -> CellPosition(10, 1)
                1 -> CellPosition(13, 1)
                2 -> CellPosition(10, 4)
                else -> CellPosition(13, 4)
            }
            2 -> when (tokenId) { // Yellow bottom-right
                0 -> CellPosition(10, 10)
                1 -> CellPosition(13, 10)
                2 -> CellPosition(10, 13)
                else -> CellPosition(13, 13)
            }
            else -> when (tokenId) { // Blue bottom-left
                0 -> CellPosition(1, 10)
                1 -> CellPosition(4, 10)
                2 -> CellPosition(1, 13)
                else -> CellPosition(4, 13)
            }
        }
    }

    if (step == 57) {
        // Center Home coordinates
        return when (playerIdx) {
            0 -> CellPosition(6, 7)
            1 -> CellPosition(7, 6)
            2 -> CellPosition(8, 7)
            else -> CellPosition(7, 8)
        }
    }

    if (step <= 51) {
        // Traveling on common outer path (52 cells)
        val startIdx = when (playerIdx) {
            0 -> 1
            1 -> 14
            2 -> 27
            else -> 40
        }
        val trackIdx = (startIdx + (step - 1)) % 52
        return trackCoordinates[trackIdx]
    }

    // Step 52..56 are on local home paths (5 cells leading to center)
    val homePathOffset = step - 52
    return when (playerIdx) {
        0 -> CellPosition(1 + homePathOffset, 7) // Red row 7 (left side going right)
        1 -> CellPosition(7, 1 + homePathOffset) // Green col 7 (top side going down)
        2 -> CellPosition(13 - homePathOffset, 7) // Yellow row 7 (right side going left)
        else -> CellPosition(7, 13 - homePathOffset) // Blue col 7 (bottom side going up)
    }
}

/**
 * Returns true if a cell coordinate corresponds to a safe on-track cell (starts and star spots).
 */
fun isSafeCoordinate(coords: CellPosition): Boolean {
    val trackIdx = trackCoordinates.indexOf(coords)
    return trackIdx != -1 && safeTrackIndices.contains(trackIdx)
}

/**
 * Returns the track index [0..51] if a step/player pair maps to the common outer track.
 * Returns -1 if it's in the yard or home path.
 */
fun getTrackIndexForStep(playerIdx: Int, step: Int): Int {
    if (step in 1..51) {
        val startIdx = when (playerIdx) {
            0 -> 1
            1 -> 14
            2 -> 27
            else -> 40
        }
        return (startIdx + (step - 1)) % 52
    }
    return -1
}
