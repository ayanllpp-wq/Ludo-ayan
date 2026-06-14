package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.model.PlayerColors
import com.example.model.PlayerType

@Composable
fun GameSetupScreen(
    players: List<Player>,
    onConfigurePlayer: (Int, PlayerType) -> Unit,
    onStartGame: () -> Unit,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                border = BorderStroke(1.5.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F).copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val titleGradient = Brush.linearGradient(
                colors = listOf(Color(0xFF60A5FA), Color(0xFF818CF8), Color(0xFFC084FC))
            )
            Text(
                text = "Ludo Arena Setup",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = titleGradient
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Match up to 4 players. Opponent slots can be filled by smart automatic CPU bots.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Players list config in a nice grid format (2x2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerSetupCard(
                        player = players[0],
                        canDisable = false, // Player 1 must be active
                        onTypeChanged = { type -> onConfigurePlayer(0, type) },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerSetupCard(
                        player = players[1],
                        canDisable = true,
                        onTypeChanged = { type -> onConfigurePlayer(1, type) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerSetupCard(
                        player = players[2],
                        canDisable = true,
                        onTypeChanged = { type -> onConfigurePlayer(2, type) },
                        modifier = Modifier.weight(1f)
                    )
                    PlayerSetupCard(
                        player = players[3],
                        canDisable = true,
                        onTypeChanged = { type -> onConfigurePlayer(3, type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(visible = statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // High contrast launch Button
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Game icon"
                    )
                    Text(
                        text = "START LUDO MATCH",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSetupCard(
    player: Player,
    canDisable: Boolean,
    onTypeChanged: (PlayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            player.color.copy(alpha = 0.15f),
            player.color.copy(alpha = 0.03f)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (player.isActive) player.color.copy(alpha = 0.7f) else Color(0xFF334155).copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Player visual badge indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(player.color)
                )
                Text(
                    text = player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (player.isActive) player.color else Color.Gray
                )
            }

            // Options Selection block
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TypeSelectorChip(
                    label = "HUMAN",
                    isSelected = player.type == PlayerType.HUMAN,
                    color = player.color,
                    onClick = { onTypeChanged(PlayerType.HUMAN) }
                )

                TypeSelectorChip(
                    label = "BOT / CPU",
                    isSelected = player.type == PlayerType.BOT,
                    color = player.color,
                    onClick = { onTypeChanged(PlayerType.BOT) }
                )

                if (canDisable) {
                    TypeSelectorChip(
                        label = "DISABLED",
                        isSelected = player.type == PlayerType.DISABLED,
                        color = Color.Gray,
                        onClick = { onTypeChanged(PlayerType.DISABLED) }
                    )
                }
            }
        }
    }
}

@Composable
fun TypeSelectorChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF1E293B)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) color else Color(0xFF94A3B8), // slate-400
            textAlign = TextAlign.Center
        )
    }
}
