package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameState {
    SETUP, PLAYING, FINISHED
}

class LudoViewModel : ViewModel() {

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _currentTurnPlayerIdx = MutableStateFlow(0)
    val currentTurnPlayerIdx: StateFlow<Int> = _currentTurnPlayerIdx.asStateFlow()

    private val _gameState = MutableStateFlow(GameState.SETUP)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _diceValue = MutableStateFlow(1)
    val diceValue: StateFlow<Int> = _diceValue.asStateFlow()

    private val _diceState = MutableStateFlow(DiceState.IDLE)
    val diceState: StateFlow<DiceState> = _diceState.asStateFlow()

    private val _canRoll = MutableStateFlow(false)
    val canRoll: StateFlow<Boolean> = _canRoll.asStateFlow()

    private val _canMove = MutableStateFlow(false)
    val canMove: StateFlow<Boolean> = _canMove.asStateFlow()

    private val _statusMessage = MutableStateFlow("Setup your Ludo Match!")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _winners = MutableStateFlow<List<Int>>(emptyList()) // List of winning player indices
    val winners: StateFlow<List<Int>> = _winners.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private var botCoroutineJob: Job? = null

    init {
        resetToSetup()
    }

    /**
     * Resets the game state back to setup mode.
     */
    fun resetToSetup() {
        botCoroutineJob?.cancel()
        _gameState.value = GameState.SETUP
        _currentTurnPlayerIdx.value = 0
        _diceValue.value = 1
        _diceState.value = DiceState.IDLE
        _canRoll.value = false
        _canMove.value = false
        _winners.value = emptyList()
        _isAnimating.value = false
        _statusMessage.value = "Configure players and press Start Game!"

        // Initialize default players
        _players.value = List(4) { idx ->
            Player(
                idx = idx,
                name = PlayerNames[idx],
                type = when (idx) {
                    0 -> PlayerType.HUMAN
                    1 -> PlayerType.BOT // By default vs 1 Bot
                    else -> PlayerType.DISABLED
                },
                color = PlayerColors[idx]
            )
        }
    }

    /**
     * Configures a player's capability (Human, Bot, or Disabled).
     */
    fun configurePlayer(idx: Int, type: PlayerType) {
        _players.value = _players.value.map { p ->
            if (p.idx == idx) p.copy(type = type) else p
        }
    }

    /**
     * Starts the game with the selected configuration.
     */
    fun startGame() {
        val activePlayers = _players.value.filter { it.isActive }
        if (activePlayers.size < 2) {
            _statusMessage.value = "Need at least 2 active players to start!"
            return
        }

        _gameState.value = GameState.PLAYING
        _winners.value = emptyList()
        _isAnimating.value = false

        // Select the first active player to start
        val firstActivePlayer = _players.value.first { it.isActive }
        _currentTurnPlayerIdx.value = firstActivePlayer.idx
        _canRoll.value = true
        _canMove.value = false
        _diceState.value = DiceState.IDLE
        _statusMessage.value = "${firstActivePlayer.name}'s turn! Roll the dice."

        triggerBotTurnIfNeeded()
    }

    /**
     * Simulates rolling the dice.
     */
    fun rollDice() {
        if (!_canRoll.value || _diceState.value == DiceState.ROLLING || _isAnimating.value) return

        _canRoll.value = false
        _diceState.value = DiceState.ROLLING
        _statusMessage.value = "${getCurrentPlayer()?.name} is rolling..."

        viewModelScope.launch {
            // Animate rolling values visually
            for (i in 0..6) {
                _diceValue.value = Random.nextInt(1, 7)
                delay(80)
            }

            val finalValue = _diceValue.value
            _diceState.value = DiceState.ROLLED

            val playerIdx = _currentTurnPlayerIdx.value
            val validTokenIds = getValidMoveTokens(playerIdx, finalValue)

            if (validTokenIds.isEmpty()) {
                _statusMessage.value = "${getCurrentPlayer()?.name} rolled a $finalValue, but has no valid moves!"
                delay(1500)
                passTurn()
            } else {
                _statusMessage.value = "${getCurrentPlayer()?.name} rolled a $finalValue! Select a token to move."
                _canMove.value = true
            }
        }
    }

    /**
     * User clicks on a token to move it.
     */
    fun onTokenClick(playerIdx: Int, tokenId: Int) {
        if (!_canMove.value || _isAnimating.value || playerIdx != _currentTurnPlayerIdx.value) return
        val currentPlayType = getCurrentPlayer()?.type ?: return
        if (currentPlayType != PlayerType.HUMAN) return

        val rolledVal = _diceValue.value
        val validIds = getValidMoveTokens(playerIdx, rolledVal)
        if (!validIds.contains(tokenId)) return

        moveTokenSequence(playerIdx, tokenId, rolledVal)
    }

    /**
     * Animated incremental sliding sequence of a token step-by-step.
     */
    private fun moveTokenSequence(playerIdx: Int, tokenId: Int, rollValue: Int) {
        _canMove.value = false
        _isAnimating.value = true

        viewModelScope.launch {
            val player = _players.value.first { it.idx == playerIdx }
            val token = player.tokens.first { it.id == tokenId }
            val startStep = token.step
            val targetStep = if (startStep == 0) 1 else startStep + rollValue

            // Hoop/slide step by step to display clean walking
            for (currentStep in startStep + 1..targetStep) {
                updateTokenStepInState(playerIdx, tokenId, currentStep)
                delay(180) // Delay per cell to create smooth walking pace
            }

            _isAnimating.value = false
            handleMoveArrival(playerIdx, tokenId, targetStep)
        }
    }

    /**
     * Updates a token's step position in our state flow.
     */
    private fun updateTokenStepInState(playerIdx: Int, tokenId: Int, newStep: Int) {
        _players.value = _players.value.map { p ->
            if (p.idx == playerIdx) {
                val newTokens = p.tokens.map { t ->
                    if (t.id == tokenId) t.copy(step = newStep) else t
                }
                p.copy(tokens = newTokens)
            } else {
                p
            }
        }
    }

    /**
     * Handle arrival on destination cell (logic for capture, home entry, etc.).
     */
    private fun handleMoveArrival(playerIdx: Int, tokenId: Int, targetStep: Int) {
        val finalRollVal = _diceValue.value

        // 1. Capture Check (Only if landed on common track 1..51)
        if (targetStep in 1..51) {
            val trackIdx = getTrackIndexForStep(playerIdx, targetStep)
            val cellCoords = trackCoordinates[trackIdx]

            if (!isSafeCoordinate(cellCoords)) {
                var capturedAny = false
                val updatedPlayers = _players.value.map { p ->
                    if (p.idx != playerIdx && p.isActive) {
                        val newTokens = p.tokens.map { t ->
                            if (getTrackIndexForStep(p.idx, t.step) == trackIdx) {
                                capturedAny = true
                                t.copy(step = 0) // Capture back to Yard!
                            } else {
                                t
                            }
                        }
                        p.copy(tokens = newTokens)
                    } else {
                        p
                    }
                }

                if (capturedAny) {
                    _players.value = updatedPlayers
                    _statusMessage.value = "💥 Splendid! ${getCurrentPlayer()?.name} captured an opponent! Extra turn!"
                    _canRoll.value = true
                    _canMove.value = false
                    _diceState.value = DiceState.IDLE
                    triggerBotTurnIfNeeded()
                    return
                }
            }
        }

        // 2. Victory/Finished home check
        if (targetStep == 57) {
            val currentPlayer = _players.value.first { it.idx == playerIdx }
            _statusMessage.value = "⭐ Hurrah! ${currentPlayer.name}'s token reached home! Extra turn!"

            if (currentPlayer.tokens.all { it.step == 57 }) {
                // Player has completed the game
                if (!_winners.value.contains(playerIdx)) {
                    _winners.value = _winners.value + playerIdx
                }

                // Check if only 1 active player is left who hasn't won
                val remainingPlayers = _players.value.filter { p -> p.isActive && p.tokens.any { !it.isFinished } }
                if (remainingPlayers.size <= 1) {
                    _gameState.value = GameState.FINISHED
                    _statusMessage.value = "Game Complete! Winner is ${_players.value[_winners.value.first()].name}! 🎉"
                    return
                }
            }

            // Enter extra turn for entering center home
            _canRoll.value = true
            _canMove.value = false
            _diceState.value = DiceState.IDLE
            triggerBotTurnIfNeeded()
            return
        }

        // 3. Roll 6 Check (Grants extra turn)
        if (finalRollVal == 6) {
            _statusMessage.value = "🎲 ${getCurrentPlayer()?.name} rolled a 6 and gets an extra turn!"
            _canRoll.value = true
            _canMove.value = false
            _diceState.value = DiceState.IDLE
            triggerBotTurnIfNeeded()
        } else {
            // Standard Pass Turn
            passTurn()
        }
    }

    /**
     * Passes the turn to the next active, non-finished player.
     */
    private fun passTurn() {
        var nextIdx = (_currentTurnPlayerIdx.value + 1) % 4
        for (i in 0..3) {
            val nextPlayer = _players.value[nextIdx]
            if (nextPlayer.isActive && !nextPlayer.isFinished) {
                _currentTurnPlayerIdx.value = nextIdx
                _canRoll.value = true
                _canMove.value = false
                _diceState.value = DiceState.IDLE
                _statusMessage.value = "${nextPlayer.name}'s turn! Roll the dice."
                triggerBotTurnIfNeeded()
                return
            }
            nextIdx = (nextIdx + 1) % 4
        }
    }

    /**
     * Executes the automatic smart AI bot pipeline if current player is a BOT.
     */
    private fun triggerBotTurnIfNeeded() {
        val currentPlayer = getCurrentPlayer() ?: return
        if (currentPlayer.type == PlayerType.BOT && _gameState.value == GameState.PLAYING && _canRoll.value) {
            botCoroutineJob?.cancel()
            botCoroutineJob = viewModelScope.launch {
                // Time delay to make UI updates comfortable
                delay(1000)
                rollDice()

                // Wait for rolling to finish
                while (_diceState.value == DiceState.ROLLING) {
                    delay(100)
                }

                delay(1000)

                val validIds = getValidMoveTokens(currentPlayer.idx, _diceValue.value)
                if (validIds.isEmpty()) {
                    // Handled automatically by rollDice when valid moves is empty, wait and passTurn
                } else {
                    val bestTokenId = selectAiToken(currentPlayer.idx, validIds, _diceValue.value)
                    _isAnimating.value = true
                    _canMove.value = false

                    val startStep = currentPlayer.tokens[bestTokenId].step
                    val targetStep = if (startStep == 0) 1 else startStep + _diceValue.value

                    _statusMessage.value = "🤖 ${currentPlayer.name} (CPU) is moving token..."

                    for (currentStep in startStep + 1..targetStep) {
                        updateTokenStepInState(currentPlayer.idx, bestTokenId, currentStep)
                        delay(200)
                    }

                    _isAnimating.value = false
                    handleMoveArrival(currentPlayer.idx, bestTokenId, targetStep)
                }
            }
        }
    }

    /**
     * Heuristics engine to pick the absolute optimal token for the Bot to move.
     */
    private fun selectAiToken(playerIdx: Int, validIds: List<Int>, rolledVal: Int): Int {
        val player = _players.value[playerIdx]

        // 1. CAPTURE CHANCE: Top priority! If any move results in landing on opponent and capturing them.
        for (id in validIds) {
            val tok = player.tokens[id]
            val currentStep = tok.step
            val nextStep = if (currentStep == 0) 1 else currentStep + rolledVal

            if (nextStep in 1..51) {
                val nextTrackIdx = getTrackIndexForStep(playerIdx, nextStep)
                val cell = trackCoordinates[nextTrackIdx]

                if (!isSafeCoordinate(cell)) {
                    val opponentExists = _players.value.any { op ->
                        op.idx != playerIdx && op.isActive && op.tokens.any { opTok ->
                            getTrackIndexForStep(op.idx, opTok.step) == nextTrackIdx
                        }
                    }
                    if (opponentExists) return id
                }
            }
        }

        // 2. ENTRY TO HOME: Second priority! Reach final yard (57).
        for (id in validIds) {
            val tok = player.tokens[id]
            if (tok.step + rolledVal == 57) return id
        }

        // 3. SECURING OUT OF DANGER (ESC): If currently in a vulnerable track block (opponent within 1-6 cells behind us) and can escape.
        for (id in validIds) {
            val tok = player.tokens[id]
            val currentStep = tok.step
            val nextStep = if (currentStep == 0) 1 else currentStep + rolledVal

            val currentTrackIdx = getTrackIndexForStep(playerIdx, currentStep)
            val currentCoord = if (currentTrackIdx != -1) trackCoordinates[currentTrackIdx] else null

            val isCurrentlyInDanger = currentCoord != null && !isSafeCoordinate(currentCoord) && run {
                _players.value.any { op ->
                    op.idx != playerIdx && op.isActive && op.tokens.any { opTok ->
                        val opTrack = getTrackIndexForStep(op.idx, opTok.step)
                        if (opTrack != -1 && currentTrackIdx != -1) {
                            val distBackward = (currentTrackIdx - opTrack + 52) % 52
                            distBackward in 1..6
                        } else false
                    }
                }
            }

            if (isCurrentlyInDanger) {
                if (nextStep >= 52) return id // Escapes directly to home safety path!
                val nextTrack = getTrackIndexForStep(playerIdx, nextStep)
                if (isSafeCoordinate(trackCoordinates[nextTrack])) return id // Escapes to a safe star!
            }
        }

        // 4. UNLEASHING FROM YARD: If rolled a 6, release a token from the yard if our starting point is empty of our own.
        if (rolledVal == 6) {
            val yardToken = validIds.find { player.tokens[it].step == 0 }
            if (yardToken != null) {
                val alreadyOnStart = player.tokens.any { it.step == 1 }
                if (!alreadyOnStart) return yardToken
            }
        }

        // 5. ENTERING CHANNELS: Prioritize advancing tokens that can get closer to entering the Home path.
        val enteringHomePathToken = validIds.find { id ->
            val step = player.tokens[id].step
            step < 52 && step + rolledVal >= 52
        }
        if (enteringHomePathToken != null) return enteringHomePathToken

        // 6. DEFAULT PROGRESSIVE: Move the token that has advanced the furthest along.
        return validIds.maxByOrNull { id -> player.tokens[id].step } ?: validIds.first()
    }

    /**
     * Fetches details of the current active player.
     */
    private fun getCurrentPlayer(): Player? {
        return _players.value.getOrNull(_currentTurnPlayerIdx.value)
    }

    /**
     * Checks which token IDs are valid to move for a player given a rolled dice value.
     */
    fun getValidMoveTokens(playerIdx: Int, rolledVal: Int): List<Int> {
        val player = _players.value.getOrNull(playerIdx) ?: return emptyList()
        val validIds = mutableListOf<Int>()

        for (t in player.tokens) {
            if (t.isFinished) continue

            if (t.isInYard) {
                if (rolledVal == 6) {
                    validIds.add(t.id)
                }
            } else {
                if (t.step + rolledVal <= 57) {
                    validIds.add(t.id)
                }
            }
        }
        return validIds
    }

    override fun onCleared() {
        botCoroutineJob?.cancel()
        super.onCleared()
    }
}
