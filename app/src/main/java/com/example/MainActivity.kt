package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LudoAppScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun LudoAppScreen(modifier: Modifier = Modifier) {
    val viewModel: LudoViewModel = viewModel()

    val players by viewModel.players.collectAsState()
    val currentTurnPlayerIdx by viewModel.currentTurnPlayerIdx.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val diceValue by viewModel.diceValue.collectAsState()
    val diceState by viewModel.diceState.collectAsState()
    val canRoll by viewModel.canRoll.collectAsState()
    val canMove by viewModel.canMove.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val winners by viewModel.winners.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()

    val activePlayer = players.getOrNull(currentTurnPlayerIdx)
    val activePlayerColor = activePlayer?.color ?: Color.Gray

    // Rolling list of past match logs
    val gameLogs = remember { mutableStateListOf<String>() }

    // Clear logs on game setup restart
    LaunchedEffect(gameState) {
        if (gameState == GameState.SETUP) {
            gameLogs.clear()
        }
    }

    // Add telemetry logs on turn changes or event triggers
    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty() && !statusMessage.startsWith("Setup") && !statusMessage.startsWith("Configure")) {
            gameLogs.add(0, statusMessage)
            if (gameLogs.size > 25) {
                gameLogs.removeLast()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
    ) {
        val isWideScreen = maxWidth > 680.dp

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Header Deck
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Ludo Classic",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ARENA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Text(
                        text = if (gameState == GameState.PLAYING) "Battle in progress" else "Setup session",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (gameState == GameState.PLAYING) {
                    OutlinedButton(
                        onClick = { viewModel.resetToSetup() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart icons",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 2. Main Arena Content
            if (gameState == GameState.SETUP) {
                GameSetupScreen(
                    players = players,
                    onConfigurePlayer = { idx, type -> viewModel.configurePlayer(idx, type) },
                    onStartGame = { viewModel.startGame() },
                    statusMessage = statusMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp)
                )
            } else {
                // Gameplay HUD
                if (isWideScreen) {
                    // Landscape / Tablet Split Deck
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left: Board
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            LudoBoard(
                                players = players,
                                validTokenIds = if (canMove) viewModel.getValidMoveTokens(currentTurnPlayerIdx, diceValue) else emptyList(),
                                currentTurnIdx = currentTurnPlayerIdx,
                                isAnimating = isAnimating,
                                canMove = canMove,
                                onTokenClick = { pIdx, tId -> viewModel.onTokenClick(pIdx, tId) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Right: Interactive Controls + Broadcast
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TurnIndicatorCard(
                                activePlayerName = activePlayer?.name ?: "",
                                activePlayerColor = activePlayerColor,
                                isCpu = activePlayer?.type == PlayerType.BOT,
                                statusMsg = statusMessage,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DiceView(
                                    diceValue = diceValue,
                                    diceState = diceState,
                                    canRoll = canRoll,
                                    onRollClick = { viewModel.rollDice() },
                                    playerIdx = currentTurnPlayerIdx
                                )
                            }

                            MatchLogConsole(
                                logs = gameLogs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                } else {
                    // Portrait stacked view
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top segment: Board directly
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.4f),
                            contentAlignment = Alignment.Center
                        ) {
                            LudoBoard(
                                players = players,
                                validTokenIds = if (canMove) viewModel.getValidMoveTokens(currentTurnPlayerIdx, diceValue) else emptyList(),
                                currentTurnIdx = currentTurnPlayerIdx,
                                isAnimating = isAnimating,
                                canMove = canMove,
                                onTokenClick = { pIdx, tId -> viewModel.onTokenClick(pIdx, tId) },
                                modifier = Modifier.fillMaxHeight()
                            )
                        }

                        // Bottom dynamic segment: Controls HUD
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TurnIndicatorCard(
                                    activePlayerName = activePlayer?.name ?: "",
                                    activePlayerColor = activePlayerColor,
                                    isCpu = activePlayer?.type == PlayerType.BOT,
                                    statusMsg = statusMessage,
                                    modifier = Modifier.weight(1.3f)
                                )

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DiceView(
                                        diceValue = diceValue,
                                        diceState = diceState,
                                        canRoll = canRoll,
                                        onRollClick = { viewModel.rollDice() },
                                        playerIdx = currentTurnPlayerIdx
                                    )
                                }
                            }

                            MatchLogConsole(
                                logs = gameLogs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Victory Championship Dialog
        if (gameState == GameState.FINISHED && winners.isNotEmpty()) {
            ChampionshipDialog(
                winners = winners,
                players = players,
                onDismiss = { viewModel.resetToSetup() }
            )
        }
    }
}

@Composable
fun TurnIndicatorCard(
    activePlayerName: String,
    activePlayerColor: Color,
    isCpu: Boolean,
    statusMsg: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
        border = BorderStroke(1.5.dp, activePlayerColor.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(activePlayerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activePlayerName.take(1),
                    color = Color(0xFF0F1113), // dark background contrast for light text
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "CURRENT TURN",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8), // slate-400
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$activePlayerName Player",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = activePlayerColor
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCpu) Color(0xFF1E293B) else activePlayerColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCpu) "CPU BOT" else "HUMAN",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCpu) Color.White else activePlayerColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusMsg,
                    fontSize = 12.sp,
                    color = Color(0xFFF1F5F9), // slate-100
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MatchLogConsole(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to top when a new log appears
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F).copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Text(
                text = "MATCH LOG CAST",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF94A3B8), // slate-400
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Match caster idle. Roll to start action!",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8).copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(M3Blue)
                            )
                            Text(
                                text = log,
                                fontSize = 11.5.sp,
                                color = Color(0xFFF1F5F9), // slate-100
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChampionshipDialog(
    winners: List<Int>,
    players: List<Player>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RE-SETUP MATCH", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 CHAMPIONSHIP OVER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ludo Arena Tournament Finished",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Official Finishing Ranking:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                winners.forEachIndexed { rank, playerIdx ->
                    val name = PlayerNames[playerIdx]
                    val color = PlayerColors[playerIdx]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.1f))
                            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rank + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "$name Player",
                                fontWeight = FontWeight.ExtraBold,
                                color = color,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = when (rank) {
                                0 -> "CHAMPION🥇"
                                1 -> "RUNNER UP🥈"
                                2 -> "THIRD PLACE🥉"
                                else -> "FINISHED"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
    )
}

