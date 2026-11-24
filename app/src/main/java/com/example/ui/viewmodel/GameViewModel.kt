package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.coach.CoachFeedback
import com.example.chess.coach.CoachQueryType
import com.example.chess.coach.CoachRepository
import com.example.chess.coach.EngineCoachDavidRepository
import com.example.chess.engine.Chess960
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameResult
import com.example.chess.engine.GameVariant
import com.example.chess.engine.Move
import com.example.chess.engine.OpeningBook
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.TerminationReason
import com.example.chess.engine.TimeControl
import com.example.chess.engine.TimeControlCategory
import com.example.data.ChessSoundPlayer
import com.example.data.RatingCalculator
import com.example.data.SoundEffect
import com.example.data.local.PlayerProfileEntity
import com.example.data.repository.ChessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

enum class PlayOpponentMode(val label: String) {
    VS_AI("vs Engine AI"),
    PASS_AND_PLAY("Pass & Play Friend"),
    SPECTATOR("Spectate AI Clash"),
    TOURNAMENT("Tournament Round")
}

data class GameSetupConfig(
    val opponentMode: PlayOpponentMode = PlayOpponentMode.VS_AI,
    val variant: GameVariant = GameVariant.STANDARD,
    val timeControl: TimeControl = TimeControl.PRESETS[5], // 10+0 Rapid
    val aiSkillLevel: Int = 4, // 1..8
    val chosenColor: PieceColor? = PieceColor.WHITE, // null = random
    val chess960Position: Int? = null, // null = random 0..959
    val customOpponentName: String? = null,
    val customOpponentRating: Int? = null,
    val tournamentId: Long? = null,
    val tournamentMatchId: Long? = null,
    val coachDavidEnabled: Boolean = false
)

data class ChatMessage(
    val sender: String,
    val text: String,
    val isSelf: Boolean
)

data class PendingPromotion(
    val from: Int,
    val to: Int
)

data class ActiveGameState(
    val config: GameSetupConfig = GameSetupConfig(),
    val board: ChessBoard = ChessBoard.standard(),
    val startFen: String = ChessBoard.STANDARD_FEN,
    val chess960Pos: Int = Chess960.STANDARD_POSITION_NUMBER,
    val playerSide: PieceColor? = PieceColor.WHITE,
    val whiteName: String = "You",
    val blackName: String = "Engine Lv.4 (1400)",
    val whiteRating: Int = 1200,
    val blackRating: Int = 1400,
    val whiteTimeMs: Long = 600_000L,
    val blackTimeMs: Long = 600_000L,
    val flipped: Boolean = false,
    val selectedSquare: Int? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val lastMoveFrom: Int? = null,
    val lastMoveTo: Int? = null,
    val sanHistory: List<String> = emptyList(),
    val boardHistory: List<ChessBoard> = listOf(ChessBoard.standard()),
    val moveStartTimestampMs: Long = System.currentTimeMillis(),
    val gameStartTimestampMs: Long = System.currentTimeMillis(),
    val result: GameResult = GameResult.IN_PROGRESS,
    val termination: TerminationReason = TerminationReason.NONE,
    val openingEco: String = "A00",
    val openingName: String = "Starting Position",
    val isAiThinking: Boolean = false,
    val statusMessage: String = "",
    val pendingPromotion: PendingPromotion? = null,
    val savedMatchId: Long? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val drawOfferedByOpponent: Boolean = false,
    val coachFeedback: CoachFeedback? = null,
    val isCoachAnalyzing: Boolean = false,
    val showCoachArrow: Boolean = false,
    val coachArrowFrom: Int? = null,
    val coachArrowTo: Int? = null
)

class GameViewModel(
    private val repository: ChessRepository,
    private val soundPlayer: ChessSoundPlayer,
    private val coachRepository: CoachRepository = EngineCoachDavidRepository()
) : ViewModel() {

    private val engine = ChessEngine()
    private val _state = MutableStateFlow(ActiveGameState())
    val state: StateFlow<ActiveGameState> = _state.asStateFlow()

    private var clockJob: Job? = null
    private var aiJob: Job? = null
    private var coachJob: Job? = null
    private var soundEnabled: Boolean = true
    private var autoPromoteQueen: Boolean = false

    init {
        startNewGame(GameSetupConfig())
    }

    fun syncPreferences(sound: Boolean, autoQueen: Boolean) {
        soundEnabled = sound
        autoPromoteQueen = autoQueen
    }

    fun startNewGame(config: GameSetupConfig) {
        val previous = _state.value
        clockJob?.cancel()
        aiJob?.cancel()
        coachJob?.cancel()

        viewModelScope.launch {
            // Persist any unfinished game that had moves so every played game is saved in history
            if (previous.result == GameResult.IN_PROGRESS &&
                previous.sanHistory.isNotEmpty() &&
                previous.savedMatchId == null &&
                previous.config.opponentMode != PlayOpponentMode.SPECTATOR
            ) {
                persistMatchSnapshot(previous, GameResult.IN_PROGRESS, "Saved Early", isRated = false)
            }

            val profile = repository.profileFlow.first() ?: PlayerProfileEntity()
            val pos960 = if (config.variant == GameVariant.FISCHER960) {
                config.chess960Position?.coerceIn(0, 959) ?: Chess960.randomPositionNumber()
            } else {
                Chess960.STANDARD_POSITION_NUMBER
            }
            val startBoard = if (config.variant == GameVariant.FISCHER960) {
                ChessBoard.fischer960(pos960)
            } else {
                ChessBoard.standard()
            }
            val startFen = startBoard.toFen()

            val humanSide: PieceColor? = when (config.opponentMode) {
                PlayOpponentMode.PASS_AND_PLAY, PlayOpponentMode.SPECTATOR -> null
                PlayOpponentMode.VS_AI, PlayOpponentMode.TOURNAMENT ->
                    config.chosenColor ?: if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
            }

            val playerRating = when {
                config.variant == GameVariant.FISCHER960 -> profile.fischerRating
                config.timeControl.category == TimeControlCategory.BULLET -> profile.bulletRating
                config.timeControl.category == TimeControlCategory.BLITZ -> profile.blitzRating
                config.timeControl.category == TimeControlCategory.RAPID -> profile.rapidRating
                else -> profile.classicalRating
            }

            val aiElo = config.customOpponentRating ?: RatingCalculator.eloForSkillLevel(config.aiSkillLevel)
            val aiTitle = config.customOpponentName ?: "Grandmaster AI Lv.${config.aiSkillLevel}"

            val (wName, bName, wRating, bRating) = when (config.opponentMode) {
                PlayOpponentMode.PASS_AND_PLAY -> listOf(profile.username, "Local Friend", playerRating.toString(), playerRating.toString())
                PlayOpponentMode.SPECTATOR -> listOf("AlphaEngine A", "AlphaEngine B", "2250", "2250")
                PlayOpponentMode.VS_AI, PlayOpponentMode.TOURNAMENT -> {
                    if (humanSide == PieceColor.WHITE) {
                        listOf(profile.username, aiTitle, playerRating.toString(), aiElo.toString())
                    } else {
                        listOf(aiTitle, profile.username, aiElo.toString(), playerRating.toString())
                    }
                }
            }

            val initialMs = config.timeControl.initialSeconds * 1000L
            val now = System.currentTimeMillis()
            val (eco, openName) = OpeningBook.identifyOpening(emptyList(), config.variant, pos960)

            val initialCoach = if (config.coachDavidEnabled) {
                CoachFeedback(
                    headline = "Coach David Active",
                    message = "Coach David: Training mode on for ${config.variant.displayName} (${config.timeControl.label}). Play a move or ask me for a hint, threat scan, or plan!",
                    evalFormatted = "+0.00",
                    centipawnsWhite = 0,
                    mateIn = null,
                    bestMoveSan = ""
                )
            } else null

            _state.value = ActiveGameState(
                config = config,
                board = startBoard,
                startFen = startFen,
                chess960Pos = pos960,
                playerSide = humanSide,
                whiteName = wName,
                blackName = bName,
                whiteRating = wRating.toInt(),
                blackRating = bRating.toInt(),
                whiteTimeMs = initialMs,
                blackTimeMs = initialMs,
                flipped = humanSide == PieceColor.BLACK,
                boardHistory = listOf(startBoard),
                moveStartTimestampMs = now,
                gameStartTimestampMs = now,
                openingEco = eco,
                openingName = openName,
                coachFeedback = initialCoach,
                chatMessages = listOf(
                    ChatMessage(
                        sender = "Arbiter",
                        text = if (config.variant == GameVariant.FISCHER960)
                            "Chess960 Position #$pos960 initialized. Good luck!"
                        else
                            "${config.timeControl.label} ${config.timeControl.category.displayName} match started.",
                        isSelf = false
                    )
                )
            )

            startClockLoop()
            maybeTriggerAiTurn()
        }
    }

    private fun startClockLoop() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (true) {
                delay(100L)
                val now = System.currentTimeMillis()
                val elapsed = now - lastTick
                lastTick = now

                val cur = _state.value
                if (cur.result != GameResult.IN_PROGRESS) break
                if (cur.sanHistory.isEmpty() && cur.config.opponentMode != PlayOpponentMode.SPECTATOR) {
                    continue
                }

                if (cur.board.sideToMove == PieceColor.WHITE) {
                    val remaining = (cur.whiteTimeMs - elapsed).coerceAtLeast(0L)
                    _state.update { it.copy(whiteTimeMs = remaining) }
                    if (remaining == 0L) {
                        finishGame(GameResult.BLACK_WIN, TerminationReason.TIMEOUT)
                        break
                    }
                } else {
                    val remaining = (cur.blackTimeMs - elapsed).coerceAtLeast(0L)
                    _state.update { it.copy(blackTimeMs = remaining) }
                    if (remaining == 0L) {
                        finishGame(GameResult.WHITE_WIN, TerminationReason.TIMEOUT)
                        break
                    }
                }
            }
        }
    }

    fun onSquareTapped(sq: Int) {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        if (cur.pendingPromotion != null) return
        if (cur.config.opponentMode == PlayOpponentMode.SPECTATOR) return
        if (cur.playerSide != null && cur.board.sideToMove != cur.playerSide) return

        val selected = cur.selectedSquare
        if (selected != null) {
            val matching = cur.legalMovesForSelected.filter {
                it.to == sq || (it.isCastling && it.castleRookFrom == sq)
            }
            if (matching.isNotEmpty()) {
                if (matching.size > 1 && matching.any { it.promotion != null }) {
                    if (autoPromoteQueen) {
                        val queenMove = matching.first { it.promotion == PieceType.QUEEN }
                        executeMove(queenMove)
                    } else {
                        _state.update { it.copy(pendingPromotion = PendingPromotion(selected, matching[0].to)) }
                    }
                } else {
                    executeMove(matching[0])
                }
                return
            }
        }

        val tappedPiece = cur.board.pieceAt(sq)
        if (tappedPiece != null && tappedPiece.color == cur.board.sideToMove) {
            val legal = cur.board.generateLegalMovesFrom(sq)
            _state.update {
                it.copy(selectedSquare = sq, legalMovesForSelected = legal)
            }
        } else {
            _state.update {
                it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    fun confirmPromotion(pieceType: PieceType) {
        val cur = _state.value
        val pending = cur.pendingPromotion ?: return
        val move = cur.board.generateLegalMovesFrom(pending.from)
            .firstOrNull { it.to == pending.to && it.promotion == pieceType }
        _state.update { it.copy(pendingPromotion = null) }
        if (move != null) executeMove(move)
    }

    fun cancelPromotion() {
        _state.update { it.copy(pendingPromotion = null, selectedSquare = null, legalMovesForSelected = emptyList()) }
    }

    private fun executeMove(move: Move) {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return

        val boardBefore = cur.board
        val legal = boardBefore.generateLegalMoves()
        val san = boardBefore.toSan(move, legal)
        val isCapture = boardBefore.pieceAt(move.to) != null || move.isEnPassant
        val nextBoard = boardBefore.makeMove(move)
        val isCheck = nextBoard.isInCheck(nextBoard.sideToMove)

        val incMs = cur.config.timeControl.incrementSeconds * 1000L
        val nextWhiteMs = if (boardBefore.sideToMove == PieceColor.WHITE && cur.sanHistory.isNotEmpty()) cur.whiteTimeMs + incMs else cur.whiteTimeMs
        val nextBlackMs = if (boardBefore.sideToMove == PieceColor.BLACK && cur.sanHistory.isNotEmpty()) cur.blackTimeMs + incMs else cur.blackTimeMs

        val newSanHistory = cur.sanHistory + san
        val (eco, openName) = OpeningBook.identifyOpening(newSanHistory, cur.config.variant, cur.chess960Pos)

        when {
            isCheck -> soundPlayer.play(SoundEffect.CHECK, soundEnabled)
            move.isCastling -> soundPlayer.play(SoundEffect.CASTLE, soundEnabled)
            move.promotion != null -> soundPlayer.play(SoundEffect.PROMOTE, soundEnabled)
            isCapture -> soundPlayer.play(SoundEffect.CAPTURE, soundEnabled)
            else -> soundPlayer.play(SoundEffect.MOVE, soundEnabled)
        }

        val (termResult, termReason) = nextBoard.evaluateTermination()

        _state.update {
            it.copy(
                board = nextBoard,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = move.from,
                lastMoveTo = move.to,
                sanHistory = newSanHistory,
                boardHistory = it.boardHistory + nextBoard,
                whiteTimeMs = nextWhiteMs,
                blackTimeMs = nextBlackMs,
                openingEco = eco,
                openingName = openName,
                drawOfferedByOpponent = false,
                showCoachArrow = false,
                coachArrowFrom = null,
                coachArrowTo = null,
                statusMessage = if (isCheck && termResult == GameResult.IN_PROGRESS) "Check!" else ""
            )
        }

        val wasHumanMove = cur.playerSide == null || boardBefore.sideToMove == cur.playerSide
        if (cur.config.coachDavidEnabled && wasHumanMove) {
            triggerCoachMoveFeedback(boardBefore, nextBoard, move, san, newSanHistory, cur.playerSide)
        }

        if (termResult != GameResult.IN_PROGRESS) {
            finishGame(termResult, termReason)
        } else {
            maybeTriggerAiTurn()
        }
    }

    private fun triggerCoachMoveFeedback(
        boardBefore: ChessBoard,
        boardAfter: ChessBoard,
        move: Move,
        san: String,
        sanHistoryAfter: List<String>,
        playerColor: PieceColor?
    ) {
        coachJob?.cancel()
        coachJob = viewModelScope.launch {
            _state.update { it.copy(isCoachAnalyzing = true) }
            val feedback = withContext(Dispatchers.Default) {
                coachRepository.evaluateMoveFeedback(boardBefore, boardAfter, move, san, sanHistoryAfter, playerColor)
            }
            _state.update {
                it.copy(
                    coachFeedback = feedback,
                    isCoachAnalyzing = false
                )
            }
        }
    }

    fun toggleCoachDavid() {
        val cur = _state.value
        val nextEnabled = !cur.config.coachDavidEnabled
        _state.update {
            it.copy(
                config = it.config.copy(coachDavidEnabled = nextEnabled),
                showCoachArrow = if (!nextEnabled) false else it.showCoachArrow,
                coachArrowFrom = if (!nextEnabled) null else it.coachArrowFrom,
                coachArrowTo = if (!nextEnabled) null else it.coachArrowTo,
                coachFeedback = if (nextEnabled && it.coachFeedback == null) {
                    CoachFeedback(
                        headline = "Coach David Joined",
                        message = "Coach David: I'm analyzing live alongside you. Ask for a Hint, Threat Check, or Strategic Plan anytime!",
                        evalFormatted = "+0.00",
                        centipawnsWhite = 0,
                        mateIn = null,
                        bestMoveSan = ""
                    )
                } else it.coachFeedback
            )
        }
        if (nextEnabled) {
            askCoachDavid(CoachQueryType.STRATEGIC_PLAN)
        }
    }

    fun askCoachDavid(queryType: CoachQueryType) {
        val cur = _state.value
        coachJob?.cancel()
        coachJob = viewModelScope.launch {
            _state.update { it.copy(isCoachAnalyzing = true) }
            val feedback = withContext(Dispatchers.Default) {
                coachRepository.answerCoachQuery(
                    queryType = queryType,
                    currentBoard = cur.board,
                    sanHistory = cur.sanHistory,
                    lastPlayedSan = cur.sanHistory.lastOrNull(),
                    lastEvalBefore = null,
                    lastMoveGrade = cur.coachFeedback?.moveGrade
                )
            }
            val showArrow = queryType == CoachQueryType.HINT_BEST_MOVE && feedback.bestMoveFrom != null
            _state.update {
                it.copy(
                    coachFeedback = feedback,
                    isCoachAnalyzing = false,
                    showCoachArrow = showArrow,
                    coachArrowFrom = if (showArrow) feedback.bestMoveFrom else null,
                    coachArrowTo = if (showArrow) feedback.bestMoveTo else null
                )
            }
        }
    }

    fun hideCoachArrow() {
        _state.update { it.copy(showCoachArrow = false, coachArrowFrom = null, coachArrowTo = null) }
    }

    fun saveGameToHistoryNow() {
        val cur = _state.value
        if (cur.sanHistory.isEmpty()) {
            _state.update { it.copy(statusMessage = "Play at least 1 move before saving to history.") }
            return
        }
        if (cur.savedMatchId != null) {
            _state.update { it.copy(statusMessage = "Game already saved to Match History!") }
            return
        }
        viewModelScope.launch {
            val id = persistMatchSnapshot(
                cur = cur,
                result = cur.result,
                terminationLabel = if (cur.result == GameResult.IN_PROGRESS) "Saved In Progress" else cur.termination.label,
                isRated = false
            )
            _state.update {
                it.copy(
                    savedMatchId = id,
                    statusMessage = "Match saved to History (#$id)"
                )
            }
        }
    }

    private fun maybeTriggerAiTurn() {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        val shouldAiMove = when (cur.config.opponentMode) {
            PlayOpponentMode.PASS_AND_PLAY -> false
            PlayOpponentMode.SPECTATOR -> true
            PlayOpponentMode.VS_AI, PlayOpponentMode.TOURNAMENT -> cur.board.sideToMove != cur.playerSide
        }
        if (!shouldAiMove) return

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _state.update { it.copy(isAiThinking = true) }
            val delayMs = if (cur.config.opponentMode == PlayOpponentMode.SPECTATOR) 550L else 220L
            delay(delayMs)
            val snapshot = _state.value
            if (snapshot.result != GameResult.IN_PROGRESS) {
                _state.update { it.copy(isAiThinking = false) }
                return@launch
            }
            val skill = if (snapshot.config.opponentMode == PlayOpponentMode.SPECTATOR) 6 else snapshot.config.aiSkillLevel
            val best = withContext(Dispatchers.Default) {
                engine.chooseMoveForSkill(snapshot.board, skill, timeLimitMs = 850L)
            }
            _state.update { it.copy(isAiThinking = false) }
            if (best != null) {
                executeMove(best)
            }
        }
    }

    fun resign() {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        val winner = when {
            cur.playerSide != null -> if (cur.playerSide == PieceColor.WHITE) GameResult.BLACK_WIN else GameResult.WHITE_WIN
            cur.board.sideToMove == PieceColor.WHITE -> GameResult.BLACK_WIN
            else -> GameResult.WHITE_WIN
        }
        finishGame(winner, TerminationReason.RESIGNATION)
    }

    fun offerDraw() {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        if (cur.config.opponentMode == PlayOpponentMode.PASS_AND_PLAY) {
            finishGame(GameResult.DRAW, TerminationReason.AGREED_DRAW)
            return
        }
        if (cur.config.opponentMode == PlayOpponentMode.VS_AI) {
            viewModelScope.launch {
                val eval = withContext(Dispatchers.Default) {
                    engine.analyzePosition(cur.board, maxDepth = 3, timeLimitMs = 250L)
                }
                val aiIsWhite = cur.playerSide == PieceColor.BLACK
                val aiAdvantageCp = if (aiIsWhite) eval.centipawnsWhite else -eval.centipawnsWhite
                if (aiAdvantageCp <= 60 && cur.sanHistory.size >= 10) {
                    finishGame(GameResult.DRAW, TerminationReason.AGREED_DRAW)
                } else {
                    _state.update {
                        it.copy(
                            statusMessage = "Engine declined draw offer",
                            chatMessages = it.chatMessages + ChatMessage(cur.blackName, "Draw declined — let's play on!", isSelf = false)
                        )
                    }
                }
            }
        }
    }

    fun requestTakeback() {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        if (cur.config.opponentMode == PlayOpponentMode.TOURNAMENT) {
            _state.update { it.copy(statusMessage = "Takebacks disabled in rated tournaments") }
            return
        }
        aiJob?.cancel()
        val stepsBack = if (cur.config.opponentMode == PlayOpponentMode.VS_AI) {
            if (cur.board.sideToMove == cur.playerSide && cur.sanHistory.size >= 2) 2 else 1
        } else 1
        if (cur.sanHistory.size < stepsBack) return

        val newBoardHistory = cur.boardHistory.dropLast(stepsBack)
        val newSanHistory = cur.sanHistory.dropLast(stepsBack)
        val (eco, openName) = OpeningBook.identifyOpening(newSanHistory, cur.config.variant, cur.chess960Pos)
        _state.update {
            it.copy(
                board = newBoardHistory.last(),
                boardHistory = newBoardHistory,
                sanHistory = newSanHistory,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = null,
                lastMoveTo = null,
                isAiThinking = false,
                openingEco = eco,
                openingName = openName,
                showCoachArrow = false,
                coachArrowFrom = null,
                coachArrowTo = null,
                statusMessage = "Takeback applied"
            )
        }
    }

    fun flipBoard() {
        _state.update { it.copy(flipped = !it.flipped) }
    }

    fun sendChatMessage(rawText: String, safeFilterEnabled: Boolean) {
        val clean = rawText.trim()
        if (clean.isBlank()) return
        val filtered = if (safeFilterEnabled) sanitizeChat(clean) else clean
        _state.update {
            it.copy(
                chatMessages = (it.chatMessages + ChatMessage("You", filtered, isSelf = true)).takeLast(40)
            )
        }
    }

    private fun sanitizeChat(input: String): String {
        val blocked = listOf("idiot", "stupid", "cheat", "hacker", "trash", "noob")
        var text = input
        blocked.forEach { w -> text = text.replace(Regex("\\b$w\\b", RegexOption.IGNORE_CASE), "***") }
        return text
    }

    private fun finishGame(result: GameResult, reason: TerminationReason) {
        clockJob?.cancel()
        aiJob?.cancel()
        soundPlayer.play(SoundEffect.GAME_END, soundEnabled)

        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return

        _state.update {
            it.copy(
                result = result,
                termination = reason,
                isAiThinking = false,
                statusMessage = "${result.displayLabel} · ${reason.label}"
            )
        }

        viewModelScope.launch {
            val matchId = persistMatchSnapshot(
                cur = cur,
                result = result,
                terminationLabel = reason.label,
                isRated = cur.config.opponentMode != PlayOpponentMode.SPECTATOR
            )
            _state.update { it.copy(savedMatchId = matchId) }

            val tId = cur.config.tournamentId
            val tMatchId = cur.config.tournamentMatchId
            if (tId != null && tMatchId != null) {
                withContext(NonCancellable) {
                    repository.completeTournamentMatchAndSimulateRound(tId, tMatchId, result, matchId)
                }
            }
        }
    }

    private suspend fun persistMatchSnapshot(
        cur: ActiveGameState,
        result: GameResult,
        terminationLabel: String,
        isRated: Boolean
    ): Long = withContext(NonCancellable) {
        val durationSec = ((System.currentTimeMillis() - cur.gameStartTimestampMs) / 1000L).coerceAtLeast(1L)
        val pgn = ChessBoard.buildPgn(
            event = if (cur.config.opponentMode == PlayOpponentMode.TOURNAMENT) "Grandmaster Tournament" else "Grandmaster Rated Match",
            whiteName = cur.whiteName,
            blackName = cur.blackName,
            whiteElo = cur.whiteRating,
            blackElo = cur.blackRating,
            result = result,
            variant = cur.config.variant,
            startFen = cur.startFen,
            sanMoves = cur.sanHistory,
            dateStr = LocalDate.now().toString().replace('-', '.'),
            opening = "${cur.openingEco} ${cur.openingName}"
        )
        val opponentRating = if (cur.playerSide == PieceColor.WHITE) cur.blackRating else cur.whiteRating
        repository.recordCompletedMatch(
            whiteName = cur.whiteName,
            blackName = cur.blackName,
            opponentRating = opponentRating,
            playerSide = cur.playerSide,
            variant = cur.config.variant,
            category = cur.config.timeControl.category,
            timeControlLabel = cur.config.timeControl.label,
            chess960Position = cur.chess960Pos,
            result = result,
            termination = terminationLabel,
            openingEco = cur.openingEco,
            openingName = cur.openingName,
            movesSan = cur.sanHistory,
            startFen = cur.startFen,
            finalFen = cur.board.toFen(),
            durationSeconds = durationSec,
            accuracyPercent = 0.0,
            pgnText = pgn,
            isRated = isRated
        )
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        aiJob?.cancel()
        coachJob?.cancel()
    }
}
