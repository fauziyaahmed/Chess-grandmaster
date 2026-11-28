package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.coach.CoachFeedback
import com.example.chess.coach.CoachQueryType
import com.example.chess.coach.CoachRepository
import com.example.chess.coach.CoachVoiceEngine
import com.example.chess.coach.EngineCoachDavidRepository
import com.example.chess.coach.VoiceMoveParser
import com.example.chess.engine.Chess960
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameResult
import com.example.chess.engine.GameVariant
import com.example.chess.engine.Move
import com.example.chess.engine.OpeningBook
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.Square
import com.example.chess.engine.TerminationReason
import com.example.chess.engine.TimeControl
import com.example.chess.engine.TimeControlCategory
import com.example.data.ChessSoundPlayer
import com.example.data.MatchStakes
import com.example.data.RatingCalculator
import com.example.data.SoundEffect
import com.example.data.SoundPack
import com.example.data.bluetooth.BluetoothChessManager
import com.example.data.bluetooth.BtConnectionState
import com.example.data.bluetooth.BtDeviceInfo
import com.example.data.bluetooth.BtPacket
import com.example.data.local.PlayerProfileEntity
import com.example.data.repository.ChessRepository
import com.example.ui.components.CastleDirectionGuide
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.random.Random

enum class PlayOpponentMode(val label: String) {
    VS_AI("vs Engine AI"),
    PASS_AND_PLAY("Pass & Play Friend"),
    BLUETOOTH_FRIEND("Bluetooth Friend"),
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
    val coachDavidEnabled: Boolean = false,
    val isRated: Boolean = true,
    val isBlindMode: Boolean = false
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

data class ConditionalMoveRule(
    val id: Long,
    val expectedOpponentUci: String,
    val expectedOpponentSan: String,
    val responseUci: String,
    val responseSan: String
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
    val moveSquaresHistory: List<Pair<Int, Int>> = emptyList(),
    val reviewPly: Int? = null,
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
    val coachArrowTo: Int? = null,
    // Fischer Chess960 Castling Direction Guide (auto-off after castling)
    val whiteHasCastled: Boolean = false,
    val blackHasCastled: Boolean = false,
    val showCastleGuide: Boolean = true,
    val castleGuides: List<CastleDirectionGuide> = emptyList(),
    // Match Rating Stakes & Post-game Performance
    val preMatchStakes: MatchStakes? = null,
    val whiteRatingDelta: Int? = null,
    val blackRatingDelta: Int? = null,
    val postGameAccuracy: Double? = null,
    val postGamePerfRating: Int? = null,
    // Conditional Moves
    val conditionalMoves: List<ConditionalMoveRule> = emptyList(),
    val conditionalStatusNotice: String? = null,
    // Bluetooth Friend state
    val btConnectionState: BtConnectionState = BtConnectionState.IDLE,
    val btStatusText: String = "Ready for Bluetooth match",
    val btPeerName: String? = null,
    val btDevices: List<BtDeviceInfo> = emptyList(),
    // Coach David Voice & Blind Audio Chess
    val isVoiceSpeaking: Boolean = false,
    val blindfoldHidePieces: Boolean = false,
    val blindAnnouncement: String = "",
    val blindSpeechRate: Float = 1.0f
) {
    val displayedPly: Int
        get() = (reviewPly ?: sanHistory.size).coerceIn(0, (boardHistory.size - 1).coerceAtLeast(0))

    val isReviewingHistory: Boolean
        get() = reviewPly != null && reviewPly < sanHistory.size

    val displayedBoard: ChessBoard
        get() = boardHistory.getOrNull(displayedPly) ?: board

    val displayedLastMoveFrom: Int?
        get() = if (displayedPly == 0) null
        else moveSquaresHistory.getOrNull(displayedPly - 1)?.first
            ?: if (!isReviewingHistory) lastMoveFrom else null

    val displayedLastMoveTo: Int?
        get() = if (displayedPly == 0) null
        else moveSquaresHistory.getOrNull(displayedPly - 1)?.second
            ?: if (!isReviewingHistory) lastMoveTo else null
}

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
    private var conditionalJob: Job? = null
    private var btObserverJob: Job? = null
    private var voiceObserverJob: Job? = null
    private var startGameJob: Job? = null

    private var soundEnabled: Boolean = true
    private var soundVolume: Float = 0.85f
    private var soundPack: SoundPack = SoundPack.CLASSIC_WOOD
    private var autoPromoteQueen: Boolean = false
    private var coachVoiceEnabled: Boolean = true
    private var lowTimeAlertedWhite: Boolean = false
    private var lowTimeAlertedBlack: Boolean = false

    private var bluetoothManager: BluetoothChessManager? = null
    private var coachVoiceEngine: CoachVoiceEngine? = null
    private val ruleIdSeq = AtomicLong(1L)

    init {
        startNewGame(GameSetupConfig())
    }

    fun attachPlatformServices(
        bluetooth: BluetoothChessManager?,
        voiceEngine: CoachVoiceEngine?
    ) {
        if (bluetooth != null && bluetoothManager !== bluetooth) {
            bluetoothManager = bluetooth
            btObserverJob?.cancel()
            btObserverJob = viewModelScope.launch {
                launch {
                    bluetooth.connectionState.collect { st ->
                        _state.update { it.copy(btConnectionState = st) }
                    }
                }
                launch {
                    bluetooth.statusText.collect { txt ->
                        _state.update { it.copy(btStatusText = txt) }
                    }
                }
                launch {
                    bluetooth.connectedPeerName.collect { peer ->
                        _state.update { it.copy(btPeerName = peer) }
                    }
                }
                launch {
                    bluetooth.devices.collect { devs ->
                        _state.update { it.copy(btDevices = devs) }
                    }
                }
                launch {
                    bluetooth.incomingPackets.collect { packet ->
                        handleIncomingBluetoothPacket(packet)
                    }
                }
            }
        }

        if (voiceEngine != null && coachVoiceEngine !== voiceEngine) {
            coachVoiceEngine = voiceEngine
            voiceObserverJob?.cancel()
            voiceObserverJob = viewModelScope.launch {
                voiceEngine.isSpeaking.collect { speaking ->
                    _state.update { it.copy(isVoiceSpeaking = speaking) }
                }
            }
        }
    }

    fun syncPreferences(
        sound: Boolean,
        autoQueen: Boolean,
        voiceEnabled: Boolean = coachVoiceEnabled,
        volume: Float = soundVolume,
        pack: SoundPack = soundPack
    ) {
        soundEnabled = sound
        soundVolume = volume
        soundPack = pack
        soundPlayer.configure(volume, pack)
        autoPromoteQueen = autoQueen
        coachVoiceEnabled = voiceEnabled
        if (!voiceEnabled) {
            coachVoiceEngine?.stop()
        }
    }

    fun startNewGame(config: GameSetupConfig) {
        val previous = _state.value
        clockJob?.cancel()
        aiJob?.cancel()
        coachJob?.cancel()
        conditionalJob?.cancel()
        startGameJob?.cancel()
        lowTimeAlertedWhite = false
        lowTimeAlertedBlack = false

        startGameJob = viewModelScope.launch {
            if (previous.result == GameResult.IN_PROGRESS &&
                previous.sanHistory.isNotEmpty() &&
                previous.savedMatchId == null &&
                previous.config.opponentMode != PlayOpponentMode.SPECTATOR
            ) {
                persistMatchSnapshot(previous, GameResult.IN_PROGRESS, "Saved Early", isRated = false, accuracyPercent = 0.0)
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
                PlayOpponentMode.VS_AI, PlayOpponentMode.TOURNAMENT, PlayOpponentMode.BLUETOOTH_FRIEND ->
                    config.chosenColor ?: if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
            }

            val formatKey = if (config.variant == GameVariant.FISCHER960) "FISCHER960" else config.timeControl.category.name
            val playerRating = ratingForFormat(profile, formatKey)
            val formatGames = gamesForFormat(profile, formatKey)

            val oppElo = config.customOpponentRating ?: when (config.opponentMode) {
                PlayOpponentMode.BLUETOOTH_FRIEND, PlayOpponentMode.PASS_AND_PLAY -> playerRating
                else -> RatingCalculator.eloForSkillLevel(config.aiSkillLevel)
            }
            val oppTitle = config.customOpponentName ?: when (config.opponentMode) {
                PlayOpponentMode.BLUETOOTH_FRIEND -> _state.value.btPeerName ?: "Bluetooth Friend"
                PlayOpponentMode.PASS_AND_PLAY -> "Local Friend"
                else -> "Grandmaster AI Lv.${config.aiSkillLevel}"
            }

            val (wName, bName, wRating, bRating) = when (config.opponentMode) {
                PlayOpponentMode.PASS_AND_PLAY -> listOf(profile.username, oppTitle, playerRating.toString(), oppElo.toString())
                PlayOpponentMode.SPECTATOR -> listOf("AlphaEngine A", "AlphaEngine B", "2250", "2250")
                PlayOpponentMode.VS_AI, PlayOpponentMode.TOURNAMENT, PlayOpponentMode.BLUETOOTH_FRIEND -> {
                    if (humanSide == PieceColor.WHITE) {
                        listOf(profile.username, oppTitle, playerRating.toString(), oppElo.toString())
                    } else {
                        listOf(oppTitle, profile.username, oppElo.toString(), playerRating.toString())
                    }
                }
            }

            val stakes = if (config.isRated &&
                config.opponentMode != PlayOpponentMode.SPECTATOR &&
                config.opponentMode != PlayOpponentMode.PASS_AND_PLAY
            ) {
                RatingCalculator.calculateExpectedStakes(playerRating, oppElo, formatGames)
            } else null

            val initialMs = config.timeControl.initialSeconds * 1000L
            val now = System.currentTimeMillis()
            val (eco, openName) = OpeningBook.identifyOpening(emptyList(), config.variant, pos960)
            val initialGuides = computeCastleGuides(
                board = startBoard,
                whiteHasCastled = false,
                blackHasCastled = false,
                targetSide = humanSide,
                enabled = true
            )

            val initialCoach = if (config.coachDavidEnabled) {
                CoachFeedback(
                    headline = "Coach David Active",
                    message = "Coach David: Training mode on for ${config.variant.displayName} (${config.timeControl.label}). Play a move or tap Hint, Top 2 Moves, Lesson, or Plan!",
                    evalFormatted = "+0.00",
                    centipawnsWhite = 0,
                    mateIn = null,
                    bestMoveSan = "",
                    phaseTitle = "Opening"
                )
            } else null

            _state.update { prev ->
                prev.copy(
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
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    lastMoveFrom = null,
                    lastMoveTo = null,
                    sanHistory = emptyList(),
                    boardHistory = listOf(startBoard),
                    moveSquaresHistory = emptyList(),
                    reviewPly = null,
                    moveStartTimestampMs = now,
                    gameStartTimestampMs = now,
                    result = GameResult.IN_PROGRESS,
                    termination = TerminationReason.NONE,
                    openingEco = eco,
                    openingName = openName,
                    isAiThinking = false,
                    statusMessage = "",
                    pendingPromotion = null,
                    savedMatchId = null,
                    drawOfferedByOpponent = false,
                    coachFeedback = initialCoach,
                    isCoachAnalyzing = false,
                    showCoachArrow = false,
                    coachArrowFrom = null,
                    coachArrowTo = null,
                    whiteHasCastled = false,
                    blackHasCastled = false,
                    showCastleGuide = true,
                    castleGuides = initialGuides,
                    preMatchStakes = stakes,
                    whiteRatingDelta = null,
                    blackRatingDelta = null,
                    postGameAccuracy = null,
                    postGamePerfRating = null,
                    conditionalMoves = emptyList(),
                    conditionalStatusNotice = null,
                    blindfoldHidePieces = false,
                    blindAnnouncement = if (config.isBlindMode) {
                        val sideLabel = if (humanSide == PieceColor.BLACK) "Black" else "White"
                        "Blind Audio Chess started. You play $sideLabel against $oppTitle. Use Voice Move, Piece Selector, or tap any square to hear it."
                    } else "",
                    chatMessages = listOf(
                        ChatMessage(
                            sender = "Arbiter",
                            text = when {
                                config.isBlindMode ->
                                    "Blind Audio Chess active (${config.variant.displayName}, ${config.timeControl.label}). Voice narration & TalkBack enabled."
                                config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND ->
                                    "Bluetooth match live (${config.variant.displayName}, ${config.timeControl.label})."
                                config.variant == GameVariant.FISCHER960 ->
                                    "Chess960 Position #$pos960 initialized. Arrows show castling targets until you castle!"
                                else ->
                                    "${config.timeControl.label} ${config.timeControl.category.displayName} ${if (config.isRated) "Rated" else "Casual"} match started."
                            },
                            isSelf = false
                        )
                    )
                )
            }

            if (config.isBlindMode) {
                coachVoiceEngine?.setSpeechRate(_state.value.blindSpeechRate)
                coachVoiceEngine?.speak(_state.value.blindAnnouncement, skipFormatting = true)
            } else if (initialCoach != null && coachVoiceEnabled) {
                coachVoiceEngine?.speak(initialCoach.message)
            }

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
                    if (remaining in 1L..9_999L && !lowTimeAlertedWhite &&
                        (cur.playerSide == null || cur.playerSide == PieceColor.WHITE) &&
                        cur.config.opponentMode != PlayOpponentMode.SPECTATOR
                    ) {
                        lowTimeAlertedWhite = true
                        soundPlayer.play(SoundEffect.LOW_TIME, soundEnabled)
                    }
                    if (remaining == 0L) {
                        finishGame(GameResult.BLACK_WIN, TerminationReason.TIMEOUT)
                        break
                    }
                } else {
                    val remaining = (cur.blackTimeMs - elapsed).coerceAtLeast(0L)
                    _state.update { it.copy(blackTimeMs = remaining) }
                    if (remaining in 1L..9_999L && !lowTimeAlertedBlack &&
                        (cur.playerSide == null || cur.playerSide == PieceColor.BLACK) &&
                        cur.config.opponentMode != PlayOpponentMode.SPECTATOR
                    ) {
                        lowTimeAlertedBlack = true
                        soundPlayer.play(SoundEffect.LOW_TIME, soundEnabled)
                    }
                    if (remaining == 0L) {
                        finishGame(GameResult.WHITE_WIN, TerminationReason.TIMEOUT)
                        break
                    }
                }
            }
        }
    }

    fun jumpToMovePly(ply: Int) {
        _state.update { cur ->
            val maxPly = cur.sanHistory.size
            val clamped = ply.coerceIn(0, maxPly)
            cur.copy(
                reviewPly = if (clamped >= maxPly) null else clamped,
                selectedSquare = null,
                legalMovesForSelected = emptyList()
            )
        }
    }

    fun onSquareTapped(sq: Int) {
        val cur = _state.value
        if (cur.isReviewingHistory) {
            _state.update { it.copy(reviewPly = null, selectedSquare = null, legalMovesForSelected = emptyList()) }
            val live = _state.value
            if (live.result == GameResult.IN_PROGRESS && live.config.opponentMode != PlayOpponentMode.SPECTATOR &&
                (live.playerSide == null || live.board.sideToMove == live.playerSide)
            ) {
                val tapped = live.board.pieceAt(sq)
                if (tapped != null && tapped.color == live.board.sideToMove) {
                    _state.update { it.copy(selectedSquare = sq, legalMovesForSelected = live.board.generateLegalMovesFrom(sq)) }
                }
            }
            return
        }
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
            val blindText = if (cur.config.isBlindMode) {
                VoiceMoveParser.describeSquareForBlind(cur.board, sq, cur.playerSide)
            } else cur.blindAnnouncement
            _state.update {
                it.copy(
                    selectedSquare = sq,
                    legalMovesForSelected = legal,
                    blindAnnouncement = blindText
                )
            }
            if (cur.config.isBlindMode) {
                coachVoiceEngine?.speak(blindText, skipFormatting = true)
            }
        } else {
            val blindText = if (cur.config.isBlindMode) {
                VoiceMoveParser.describeSquareForBlind(cur.board, sq, cur.playerSide)
            } else cur.blindAnnouncement
            _state.update {
                it.copy(
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    blindAnnouncement = blindText
                )
            }
            if (cur.config.isBlindMode) {
                coachVoiceEngine?.speak(blindText, skipFormatting = true)
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

    private fun executeMove(
        move: Move,
        isRemoteMove: Boolean = false,
        syncedWhiteMs: Long? = null,
        syncedBlackMs: Long? = null
    ) {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return

        val boardBefore = cur.board
        val legal = boardBefore.generateLegalMoves()
        val san = boardBefore.toSan(move, legal)
        val isCapture = boardBefore.pieceAt(move.to) != null || move.isEnPassant
        val nextBoard = boardBefore.makeMove(move)
        val isCheck = nextBoard.isInCheck(nextBoard.sideToMove)

        val incMs = cur.config.timeControl.incrementSeconds * 1000L
        val nextWhiteMs = syncedWhiteMs ?: if (boardBefore.sideToMove == PieceColor.WHITE && cur.sanHistory.isNotEmpty()) cur.whiteTimeMs + incMs else cur.whiteTimeMs
        val nextBlackMs = syncedBlackMs ?: if (boardBefore.sideToMove == PieceColor.BLACK && cur.sanHistory.isNotEmpty()) cur.blackTimeMs + incMs else cur.blackTimeMs

        val newSanHistory = cur.sanHistory + san
        val (eco, openName) = OpeningBook.identifyOpening(newSanHistory, cur.config.variant, cur.chess960Pos)

        // Update castling flags — auto turns OFF Chess960 guide once the player has castled
        val nextWhiteCastled = cur.whiteHasCastled ||
            (move.isCastling && boardBefore.sideToMove == PieceColor.WHITE) ||
            (nextBoard.whiteCastleKingsideRookFile < 0 && nextBoard.whiteCastleQueensideRookFile < 0)
        val nextBlackCastled = cur.blackHasCastled ||
            (move.isCastling && boardBefore.sideToMove == PieceColor.BLACK) ||
            (nextBoard.blackCastleKingsideRookFile < 0 && nextBoard.blackCastleQueensideRookFile < 0)

        val (termResult, termReason) = nextBoard.evaluateTermination()
        val wasOpponentMove = cur.playerSide != null && boardBefore.sideToMove != cur.playerSide

        when {
            termReason == TerminationReason.CHECKMATE ->
                soundPlayer.play(SoundEffect.CHECKMATE, soundEnabled)
            termResult == GameResult.DRAW ->
                soundPlayer.play(SoundEffect.DRAW, soundEnabled)
            termResult != GameResult.IN_PROGRESS ->
                soundPlayer.play(SoundEffect.GAME_END, soundEnabled)
            isCheck ->
                soundPlayer.play(SoundEffect.CHECK, soundEnabled)
            move.isCastling ->
                soundPlayer.play(SoundEffect.CASTLE, soundEnabled)
            move.promotion != null ->
                soundPlayer.play(SoundEffect.PROMOTE, soundEnabled)
            isCapture ->
                soundPlayer.play(SoundEffect.CAPTURE, soundEnabled)
            wasOpponentMove ->
                soundPlayer.play(SoundEffect.OPPONENT_MOVE, soundEnabled)
            else ->
                soundPlayer.play(SoundEffect.MOVE, soundEnabled)
        }

        // Evaluate Conditional Move match if this was the opponent's move
        var triggeredConditional: ConditionalMoveRule? = null
        var nextConditionalList = cur.conditionalMoves
        var nextConditionalNotice = cur.conditionalStatusNotice

        if (wasOpponentMove && cur.conditionalMoves.isNotEmpty()) {
            val matched = cur.conditionalMoves.firstOrNull { it.expectedOpponentUci == move.toUci() }
            if (matched != null) {
                triggeredConditional = matched
                nextConditionalList = emptyList()
                nextConditionalNotice = "Conditional triggered: If $san → ${matched.responseSan}"
            } else {
                nextConditionalList = emptyList()
                nextConditionalNotice = "Opponent played $san — conditional lines cleared"
            }
        }

        val updatedGuides = if (termResult == GameResult.IN_PROGRESS) {
            computeCastleGuides(
                board = nextBoard,
                whiteHasCastled = nextWhiteCastled,
                blackHasCastled = nextBlackCastled,
                targetSide = cur.playerSide,
                enabled = cur.showCastleGuide
            )
        } else emptyList()

        val blindMoveSpoken = if (cur.config.isBlindMode) {
            VoiceMoveParser.describeMoveForBlind(boardBefore, move, nextBoard)
        } else cur.blindAnnouncement

        _state.update {
            it.copy(
                board = nextBoard,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = move.from,
                lastMoveTo = move.to,
                sanHistory = newSanHistory,
                boardHistory = it.boardHistory + nextBoard,
                moveSquaresHistory = it.moveSquaresHistory + (move.from to move.to),
                reviewPly = if (wasOpponentMove && it.isReviewingHistory) it.reviewPly else null,
                whiteTimeMs = nextWhiteMs,
                blackTimeMs = nextBlackMs,
                openingEco = eco,
                openingName = openName,
                drawOfferedByOpponent = false,
                showCoachArrow = false,
                coachArrowFrom = null,
                coachArrowTo = null,
                whiteHasCastled = nextWhiteCastled,
                blackHasCastled = nextBlackCastled,
                castleGuides = updatedGuides,
                conditionalMoves = nextConditionalList,
                conditionalStatusNotice = nextConditionalNotice,
                blindAnnouncement = blindMoveSpoken,
                statusMessage = when {
                    move.isCastling && cur.config.variant == GameVariant.FISCHER960 ->
                        "Castled! Chess960 castling guide turned off."
                    isCheck && termResult == GameResult.IN_PROGRESS -> "Check!"
                    else -> ""
                }
            )
        }

        if (cur.config.isBlindMode) {
            coachVoiceEngine?.speak(blindMoveSpoken, skipFormatting = true)
        }

        // Send over Bluetooth if this was our local move in BLUETOOTH_FRIEND mode
        if (cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND && !isRemoteMove) {
            bluetoothManager?.sendPacket(BtPacket.MoveSync(move.toUci(), nextWhiteMs, nextBlackMs))
        }

        val wasHumanMove = cur.playerSide == null || boardBefore.sideToMove == cur.playerSide
        if (cur.config.coachDavidEnabled && wasHumanMove && !cur.config.isBlindMode) {
            triggerCoachMoveFeedback(boardBefore, nextBoard, move, san, newSanHistory, cur.playerSide)
        }

        if (termResult != GameResult.IN_PROGRESS) {
            finishGame(termResult, termReason, playEndSound = false)
            return
        }

        if (triggeredConditional != null) {
            conditionalJob?.cancel()
            conditionalJob = viewModelScope.launch {
                delay(180L)
                val latest = _state.value
                if (latest.result != GameResult.IN_PROGRESS) return@launch
                if (latest.playerSide != null && latest.board.sideToMove != latest.playerSide) return@launch
                val reply = latest.board.generateLegalMoves()
                    .firstOrNull { it.toUci() == triggeredConditional.responseUci }
                if (reply != null) {
                    executeMove(reply)
                }
            }
        } else {
            maybeTriggerAiTurn()
        }
    }

    // --- Conditional Moves ---

    fun addConditionalMove(
        expectedOpponentUci: String,
        expectedOpponentSan: String,
        responseUci: String,
        responseSan: String
    ) {
        val rule = ConditionalMoveRule(
            id = ruleIdSeq.getAndIncrement(),
            expectedOpponentUci = expectedOpponentUci,
            expectedOpponentSan = expectedOpponentSan,
            responseUci = responseUci,
            responseSan = responseSan
        )
        _state.update { cur ->
            val filtered = cur.conditionalMoves.filterNot { it.expectedOpponentUci == expectedOpponentUci }
            cur.copy(
                conditionalMoves = filtered + rule,
                conditionalStatusNotice = "Armed: If $expectedOpponentSan → $responseSan"
            )
        }
    }

    fun playMoveWithConditional(
        myMoveUci: String,
        expectedOpponentUci: String,
        expectedOpponentSan: String,
        responseUci: String,
        responseSan: String
    ) {
        val cur = _state.value
        val myMove = cur.board.generateLegalMoves().firstOrNull { it.toUci() == myMoveUci } ?: return
        addConditionalMove(expectedOpponentUci, expectedOpponentSan, responseUci, responseSan)
        executeMove(myMove)
    }

    fun removeConditionalMove(ruleId: Long) {
        _state.update { cur ->
            cur.copy(
                conditionalMoves = cur.conditionalMoves.filterNot { it.id == ruleId },
                conditionalStatusNotice = null
            )
        }
    }

    fun clearConditionalMoves() {
        _state.update { it.copy(conditionalMoves = emptyList(), conditionalStatusNotice = null) }
    }

    // --- Chess960 Castling Direction Toggle ---

    fun toggleCastleGuide() {
        _state.update { cur ->
            val nextEnabled = !cur.showCastleGuide
            cur.copy(
                showCastleGuide = nextEnabled,
                castleGuides = computeCastleGuides(
                    board = cur.board,
                    whiteHasCastled = cur.whiteHasCastled,
                    blackHasCastled = cur.blackHasCastled,
                    targetSide = cur.playerSide,
                    enabled = nextEnabled
                )
            )
        }
    }

    // --- Coach David + Voice ---

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
            if (coachVoiceEnabled) {
                coachVoiceEngine?.speak(feedback.message)
            }
        }
    }

    fun toggleCoachDavid() {
        val cur = _state.value
        val nextEnabled = !cur.config.coachDavidEnabled
        if (!nextEnabled) {
            coachVoiceEngine?.stop()
        }
        _state.update {
            it.copy(
                config = it.config.copy(coachDavidEnabled = nextEnabled),
                showCoachArrow = if (!nextEnabled) false else it.showCoachArrow,
                coachArrowFrom = if (!nextEnabled) null else it.coachArrowFrom,
                coachArrowTo = if (!nextEnabled) null else it.coachArrowTo,
                coachFeedback = if (nextEnabled && it.coachFeedback == null) {
                    CoachFeedback(
                        headline = "Coach David Joined",
                        message = "Coach David: I'm analyzing live alongside you. Ask for a Hint, Top 2 Moves, Lesson, or Strategic Plan anytime!",
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
            if (coachVoiceEnabled) {
                coachVoiceEngine?.speak(feedback.message)
            }
        }
    }

    fun speakCurrentCoachMessage() {
        val msg = _state.value.coachFeedback?.message ?: return
        coachVoiceEngine?.speak(msg)
    }

    fun stopCoachVoice() {
        coachVoiceEngine?.stop()
    }

    fun undoMistakeAndRetry() {
        requestTakeback()
        _state.update {
            it.copy(statusMessage = "Mistake undone — try Coach David's stronger idea!")
        }
        askCoachDavid(CoachQueryType.HINT_BEST_MOVE)
    }

    fun hideCoachArrow() {
        _state.update { it.copy(showCoachArrow = false, coachArrowFrom = null, coachArrowTo = null) }
    }

    // --- Blind Audio Chess Controls ---

    fun toggleBlindMode() {
        val next = !_state.value.config.isBlindMode
        val announcement = if (next) {
            "Blind Audio Chess enabled. " + VoiceMoveParser.describeBoardForBlind(_state.value.board, _state.value.playerSide)
        } else {
            ""
        }
        _state.update {
            it.copy(
                config = it.config.copy(isBlindMode = next),
                blindfoldHidePieces = if (!next) false else it.blindfoldHidePieces,
                blindAnnouncement = announcement
            )
        }
        if (next) {
            coachVoiceEngine?.speak(announcement, skipFormatting = true)
        }
    }

    fun toggleBlindfoldHidePieces() {
        val next = !_state.value.blindfoldHidePieces
        val msg = if (next) "Pure Blindfold view active: board pieces hidden." else "Board pieces visible."
        _state.update { it.copy(blindfoldHidePieces = next, blindAnnouncement = msg) }
        coachVoiceEngine?.speak(msg, skipFormatting = true)
    }

    fun setBlindSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.6f, 1.8f)
        _state.update { it.copy(blindSpeechRate = clamped) }
        coachVoiceEngine?.setSpeechRate(clamped)
    }

    fun speakBoardSummary() {
        val cur = _state.value
        val summary = VoiceMoveParser.describeBoardForBlind(cur.board, cur.playerSide)
        _state.update { it.copy(blindAnnouncement = summary) }
        coachVoiceEngine?.speak(summary, skipFormatting = true)
    }

    fun speakLastMoveBlind() {
        val cur = _state.value
        val idx = cur.sanHistory.lastIndex
        val msg = if (idx < 0 || cur.boardHistory.size < 2) {
            "No moves played yet. ${VoiceMoveParser.colorWord(cur.board.sideToMove)} to move."
        } else {
            val prevBoard = cur.boardHistory[idx]
            val curBoard = cur.boardHistory[idx + 1]
            val sqs = cur.moveSquaresHistory.getOrNull(idx)
            val m = if (sqs != null) {
                prevBoard.generateLegalMoves().firstOrNull { it.from == sqs.first && it.to == sqs.second }
            } else null
            if (m != null) VoiceMoveParser.describeMoveForBlind(prevBoard, m, curBoard)
            else "Last move played was ${cur.sanHistory[idx]}."
        }
        _state.update { it.copy(blindAnnouncement = msg) }
        coachVoiceEngine?.speak(msg, skipFormatting = true)
    }

    fun speakLegalMovesBlind() {
        val cur = _state.value
        val selected = cur.selectedSquare
        val msg = if (selected != null) {
            val group = VoiceMoveParser.movablePiecesForBlind(cur.board).firstOrNull { it.square == selected }
            if (group != null) {
                "Legal moves for ${group.pieceName} on ${group.squareAlgebraic}: " +
                    group.legalOptions.joinToString(", ") { it.spokenLabel }
            } else {
                "No legal moves from selected square."
            }
        } else {
            val groups = VoiceMoveParser.movablePiecesForBlind(cur.board)
            "Movable pieces: " + groups.joinToString(", ") { "${it.pieceName} on ${it.squareAlgebraic} (${it.legalOptions.size})" }
        }
        _state.update { it.copy(blindAnnouncement = msg) }
        coachVoiceEngine?.speak(msg, skipFormatting = true)
    }

    fun speakBlindHint() {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        viewModelScope.launch {
            val eval = withContext(Dispatchers.Default) {
                engine.analyzePosition(cur.board, maxDepth = 4, timeLimitMs = 450L)
            }
            val best = eval.bestMove
            val msg = if (best != null) {
                val after = cur.board.makeMove(best)
                "Suggested move: " + VoiceMoveParser.describeMoveForBlind(cur.board, best, after)
            } else {
                "No legal moves available."
            }
            _state.update { it.copy(blindAnnouncement = msg) }
            coachVoiceEngine?.speak(msg, skipFormatting = true)
        }
    }

    fun executeBlindMove(move: Move) {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        if (cur.playerSide != null && cur.board.sideToMove != cur.playerSide) return
        val legal = cur.board.generateLegalMoves()
        val resolved = legal.firstOrNull { it.toUci() == move.toUci() } ?: return
        executeMove(resolved)
    }

    fun submitSpokenOrTypedMove(transcript: String): Boolean {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return false
        if (cur.playerSide != null && cur.board.sideToMove != cur.playerSide) return false
        val move = VoiceMoveParser.parseSpokenMove(transcript, cur.board)
        if (move != null) {
            executeMove(move)
            return true
        }
        val errMsg = "Unrecognized or illegal move: $transcript"
        _state.update { it.copy(blindAnnouncement = errMsg) }
        coachVoiceEngine?.speak(errMsg, skipFormatting = true)
        return false
    }

    // --- Bluetooth Match Setup & Handlers ---

    fun refreshBluetoothDevices() {
        bluetoothManager?.refreshPairedDevices()
    }

    fun startBluetoothDiscovery() {
        bluetoothManager?.startDiscovery()
    }

    fun disconnectBluetooth() {
        bluetoothManager?.disconnect()
    }

    fun hostBluetoothMatch(
        variant: GameVariant,
        chess960Pos: Int?,
        timeControl: TimeControl,
        hostColor: PieceColor?,
        isRated: Boolean,
        coachEnabled: Boolean,
        onMatchReady: () -> Unit
    ) {
        val bt = bluetoothManager ?: return
        viewModelScope.launch {
            val profile = repository.profileFlow.first() ?: PlayerProfileEntity()
            val pos = if (variant == GameVariant.FISCHER960) {
                chess960Pos?.coerceIn(0, 959) ?: Chess960.randomPositionNumber()
            } else Chess960.STANDARD_POSITION_NUMBER
            val hostPlaysWhite = (hostColor ?: PieceColor.WHITE) == PieceColor.WHITE
            val myRating = if (variant == GameVariant.FISCHER960) profile.fischerRating else profile.rapidRating

            bt.hostMatch { peerName ->
                bt.sendPacket(
                    BtPacket.StartMatch(
                        variant = variant,
                        chess960Pos = pos,
                        timeControlLabel = timeControl.label,
                        hostPlaysWhite = hostPlaysWhite,
                        hostName = profile.username,
                        hostRating = myRating,
                        isRated = isRated
                    )
                )
                startNewGame(
                    GameSetupConfig(
                        opponentMode = PlayOpponentMode.BLUETOOTH_FRIEND,
                        variant = variant,
                        timeControl = timeControl,
                        chosenColor = if (hostPlaysWhite) PieceColor.WHITE else PieceColor.BLACK,
                        chess960Position = pos,
                        customOpponentName = peerName,
                        customOpponentRating = myRating,
                        isRated = isRated,
                        coachDavidEnabled = coachEnabled
                    )
                )
                onMatchReady()
            }
        }
    }

    fun connectBluetoothFriend(
        address: String,
        fallbackCoachEnabled: Boolean,
        onMatchReady: () -> Unit
    ) {
        val bt = bluetoothManager ?: return
        bt.connectToFriend(address) {
            if (_state.value.config.opponentMode != PlayOpponentMode.BLUETOOTH_FRIEND) {
                _state.update {
                    it.copy(config = it.config.copy(coachDavidEnabled = fallbackCoachEnabled))
                }
            }
            onMatchReady()
        }
    }

    internal fun onIncomingBluetoothPacketForTest(packet: BtPacket) {
        handleIncomingBluetoothPacket(packet)
    }

    private fun handleIncomingBluetoothPacket(packet: BtPacket) {
        when (packet) {
            is BtPacket.StartMatch -> {
                val tc = TimeControl.fromLabel(packet.timeControlLabel)
                val mySide = if (packet.hostPlaysWhite) PieceColor.BLACK else PieceColor.WHITE
                startNewGame(
                    GameSetupConfig(
                        opponentMode = PlayOpponentMode.BLUETOOTH_FRIEND,
                        variant = packet.variant,
                        timeControl = tc,
                        chosenColor = mySide,
                        chess960Position = packet.chess960Pos,
                        customOpponentName = packet.hostName,
                        customOpponentRating = packet.hostRating,
                        isRated = packet.isRated,
                        coachDavidEnabled = _state.value.config.coachDavidEnabled
                    )
                )
            }

            is BtPacket.MoveSync -> {
                val cur = _state.value
                if (cur.result != GameResult.IN_PROGRESS) return
                if (cur.config.opponentMode != PlayOpponentMode.BLUETOOTH_FRIEND) return
                val move = cur.board.generateLegalMoves().firstOrNull { it.toUci() == packet.uci } ?: return
                executeMove(
                    move = move,
                    isRemoteMove = true,
                    syncedWhiteMs = packet.whiteMs,
                    syncedBlackMs = packet.blackMs
                )
            }

            is BtPacket.ResignSync -> {
                val cur = _state.value
                if (cur.result != GameResult.IN_PROGRESS) return
                val winner = when (cur.playerSide) {
                    PieceColor.WHITE -> GameResult.WHITE_WIN
                    PieceColor.BLACK -> GameResult.BLACK_WIN
                    null -> GameResult.WHITE_WIN
                }
                _state.update {
                    it.copy(
                        chatMessages = it.chatMessages + ChatMessage(packet.senderName, "I resign. Good game!", isSelf = false)
                    )
                }
                finishGame(winner, TerminationReason.RESIGNATION, skipBtNotify = true)
            }

            BtPacket.DrawOfferSync -> {
                _state.update {
                    it.copy(
                        drawOfferedByOpponent = true,
                        statusMessage = "Bluetooth friend offered a draw"
                    )
                }
            }

            BtPacket.DrawAcceptSync -> {
                finishGame(GameResult.DRAW, TerminationReason.AGREED_DRAW, skipBtNotify = true)
            }

            is BtPacket.ChatSync -> {
                _state.update {
                    it.copy(
                        chatMessages = (it.chatMessages + ChatMessage(packet.sender, packet.text, isSelf = false)).takeLast(40)
                    )
                }
            }
        }
    }

    fun respondToDrawOffer(accept: Boolean) {
        val cur = _state.value
        if (!cur.drawOfferedByOpponent || cur.result != GameResult.IN_PROGRESS) return
        _state.update { it.copy(drawOfferedByOpponent = false) }
        if (accept) {
            if (cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) {
                bluetoothManager?.sendPacket(BtPacket.DrawAcceptSync)
            }
            finishGame(GameResult.DRAW, TerminationReason.AGREED_DRAW, skipBtNotify = true)
        } else {
            _state.update { it.copy(statusMessage = "Draw offer declined") }
        }
    }

    // --- Persistence & Match Control ---

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
                isRated = false,
                accuracyPercent = 0.0
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
            PlayOpponentMode.PASS_AND_PLAY, PlayOpponentMode.BLUETOOTH_FRIEND -> false
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
        if (cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) {
            val sender = if (cur.playerSide == PieceColor.WHITE) cur.whiteName else cur.blackName
            bluetoothManager?.sendPacket(BtPacket.ResignSync(sender))
        }
        finishGame(winner, TerminationReason.RESIGNATION, skipBtNotify = true)
    }

    fun offerDraw() {
        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        if (cur.config.opponentMode == PlayOpponentMode.PASS_AND_PLAY) {
            finishGame(GameResult.DRAW, TerminationReason.AGREED_DRAW)
            return
        }
        if (cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) {
            bluetoothManager?.sendPacket(BtPacket.DrawOfferSync)
            _state.update { it.copy(statusMessage = "Draw offer sent to friend...") }
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
        if (cur.config.opponentMode == PlayOpponentMode.TOURNAMENT || cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) {
            _state.update { it.copy(statusMessage = "Takebacks disabled in ${cur.config.opponentMode.label}") }
            return
        }
        aiJob?.cancel()
        conditionalJob?.cancel()
        val stepsBack = if (cur.config.opponentMode == PlayOpponentMode.VS_AI) {
            if (cur.board.sideToMove == cur.playerSide && cur.sanHistory.size >= 2) 2 else 1
        } else 1
        if (cur.sanHistory.size < stepsBack) return

        val newBoardHistory = cur.boardHistory.dropLast(stepsBack)
        val newSanHistory = cur.sanHistory.dropLast(stepsBack)
        val newMoveSquares = cur.moveSquaresHistory.dropLast(stepsBack)
        val rolledBoard = newBoardHistory.last()
        val (eco, openName) = OpeningBook.identifyOpening(newSanHistory, cur.config.variant, cur.chess960Pos)

        val wCastled = rolledBoard.whiteCastleKingsideRookFile < 0 && rolledBoard.whiteCastleQueensideRookFile < 0
        val bCastled = rolledBoard.blackCastleKingsideRookFile < 0 && rolledBoard.blackCastleQueensideRookFile < 0

        _state.update {
            it.copy(
                board = rolledBoard,
                boardHistory = newBoardHistory,
                sanHistory = newSanHistory,
                moveSquaresHistory = newMoveSquares,
                reviewPly = null,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = newMoveSquares.lastOrNull()?.first,
                lastMoveTo = newMoveSquares.lastOrNull()?.second,
                isAiThinking = false,
                openingEco = eco,
                openingName = openName,
                showCoachArrow = false,
                coachArrowFrom = null,
                coachArrowTo = null,
                whiteHasCastled = wCastled,
                blackHasCastled = bCastled,
                castleGuides = computeCastleGuides(rolledBoard, wCastled, bCastled, cur.playerSide, cur.showCastleGuide),
                conditionalMoves = emptyList(),
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
        val cur = _state.value
        if (cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) {
            val sender = if (cur.playerSide == PieceColor.WHITE) cur.whiteName else cur.blackName
            bluetoothManager?.sendPacket(BtPacket.ChatSync(sender, filtered))
        }
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

    private fun finishGame(
        result: GameResult,
        reason: TerminationReason,
        skipBtNotify: Boolean = false,
        playEndSound: Boolean = true
    ) {
        clockJob?.cancel()
        aiJob?.cancel()
        conditionalJob?.cancel()
        if (playEndSound) {
            val endSound = if (result == GameResult.DRAW) SoundEffect.DRAW else SoundEffect.GAME_END
            soundPlayer.play(endSound, soundEnabled)
        }

        val cur = _state.value
        if (cur.result != GameResult.IN_PROGRESS) return
        if (!skipBtNotify && cur.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND && reason == TerminationReason.RESIGNATION) {
            val sender = if (cur.playerSide == PieceColor.WHITE) cur.whiteName else cur.blackName
            bluetoothManager?.sendPacket(BtPacket.ResignSync(sender))
        }

        _state.update {
            it.copy(
                result = result,
                termination = reason,
                isAiThinking = false,
                castleGuides = emptyList(),
                conditionalMoves = emptyList(),
                statusMessage = "${result.displayLabel} · ${reason.label}"
            )
        }

        viewModelScope.launch {
            val isRatedGame = cur.config.isRated &&
                cur.config.opponentMode != PlayOpponentMode.SPECTATOR &&
                cur.config.opponentMode != PlayOpponentMode.PASS_AND_PLAY

            val accuracy = withContext(Dispatchers.Default) {
                estimateMatchAccuracy(cur.boardHistory, cur.sanHistory, cur.playerSide)
            }

            val matchId = persistMatchSnapshot(
                cur = cur,
                result = result,
                terminationLabel = reason.label,
                isRated = isRatedGame,
                accuracyPercent = accuracy
            )
            val saved = withContext(NonCancellable) { repository.getMatchById(matchId) }
            val wDelta = saved?.let { it.whiteRatingAfter - it.whiteRatingBefore }
            val bDelta = saved?.let { it.blackRatingAfter - it.blackRatingBefore }

            val oppRating = if (cur.playerSide == PieceColor.WHITE) cur.blackRating else cur.whiteRating
            val score = when {
                cur.playerSide == null -> null
                result == GameResult.DRAW -> 0.5
                (result == GameResult.WHITE_WIN && cur.playerSide == PieceColor.WHITE) ||
                    (result == GameResult.BLACK_WIN && cur.playerSide == PieceColor.BLACK) -> 1.0
                else -> 0.0
            }
            val perfRating = if (score != null && cur.sanHistory.isNotEmpty()) {
                RatingCalculator.performanceRating(oppRating, score, accuracy)
            } else null

            _state.update {
                it.copy(
                    savedMatchId = matchId,
                    whiteRatingDelta = wDelta,
                    blackRatingDelta = bDelta,
                    postGameAccuracy = if (cur.sanHistory.isNotEmpty()) accuracy else null,
                    postGamePerfRating = perfRating
                )
            }

            val tId = cur.config.tournamentId
            val tMatchId = cur.config.tournamentMatchId
            if (tId != null && tMatchId != null) {
                withContext(NonCancellable) {
                    repository.completeTournamentMatchAndSimulateRound(tId, tMatchId, result, matchId)
                }
            }
        }
    }

    private fun estimateMatchAccuracy(
        boards: List<ChessBoard>,
        sanMoves: List<String>,
        playerSide: PieceColor?
    ): Double {
        if (sanMoves.isEmpty() || boards.size < 2) return 0.0
        val sampleIndices = sanMoves.indices.filter { idx ->
            val moverIsWhite = idx % 2 == 0
            playerSide == null || (playerSide == PieceColor.WHITE && moverIsWhite) || (playerSide == PieceColor.BLACK && !moverIsWhite)
        }.takeLast(10)
        if (sampleIndices.isEmpty()) return 75.0

        var totalAcc = 0.0
        var counted = 0
        for (i in sampleIndices) {
            val before = boards.getOrNull(i) ?: continue
            val after = boards.getOrNull(i + 1) ?: continue
            val evalBefore = engine.analyzePosition(before, maxDepth = 2, timeLimitMs = 35L)
            val evalAfter = engine.analyzePosition(after, maxDepth = 2, timeLimitMs = 35L)
            val isWhiteMove = before.sideToMove == PieceColor.WHITE
            val bestCp = if (isWhiteMove) evalBefore.centipawnsWhite else -evalBefore.centipawnsWhite
            val playedCp = if (isWhiteMove) evalAfter.centipawnsWhite else -evalAfter.centipawnsWhite
            val cpLoss = (bestCp - playedCp).coerceIn(0, 600)
            val moveAcc = (103.0 - (cpLoss * 0.22)).coerceIn(30.0, 99.5)
            totalAcc += moveAcc
            counted++
        }
        return if (counted == 0) 75.0 else (totalAcc / counted).coerceIn(25.0, 99.5)
    }

    private suspend fun persistMatchSnapshot(
        cur: ActiveGameState,
        result: GameResult,
        terminationLabel: String,
        isRated: Boolean,
        accuracyPercent: Double
    ): Long = withContext(NonCancellable) {
        val durationSec = ((System.currentTimeMillis() - cur.gameStartTimestampMs) / 1000L).coerceAtLeast(1L)
        val pgn = ChessBoard.buildPgn(
            event = when (cur.config.opponentMode) {
                PlayOpponentMode.TOURNAMENT -> "Grandmaster Tournament"
                PlayOpponentMode.BLUETOOTH_FRIEND -> "Grandmaster Bluetooth Match"
                else -> if (isRated) "Grandmaster Rated Match" else "Grandmaster Casual Match"
            },
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
            accuracyPercent = accuracyPercent,
            pgnText = pgn,
            isRated = isRated
        )
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        aiJob?.cancel()
        coachJob?.cancel()
        conditionalJob?.cancel()
        btObserverJob?.cancel()
        voiceObserverJob?.cancel()
        coachVoiceEngine?.stop()
    }

    companion object {
        fun computeCastleGuides(
            board: ChessBoard,
            whiteHasCastled: Boolean,
            blackHasCastled: Boolean,
            targetSide: PieceColor?,
            enabled: Boolean
        ): List<CastleDirectionGuide> {
            if (!enabled || board.variant != GameVariant.FISCHER960) return emptyList()
            val side = targetSide ?: board.sideToMove
            val alreadyCastled = if (side == PieceColor.WHITE) whiteHasCastled else blackHasCastled
            if (alreadyCastled) return emptyList()

            val backRank = if (side == PieceColor.WHITE) 0 else 7
            val kingSq = board.findKing(side)
            if (kingSq !in 0..63 || Square.rankOf(kingSq) != backRank) return emptyList()

            val ksRookFile = if (side == PieceColor.WHITE) board.whiteCastleKingsideRookFile else board.blackCastleKingsideRookFile
            val qsRookFile = if (side == PieceColor.WHITE) board.whiteCastleQueensideRookFile else board.blackCastleQueensideRookFile
            val legalMoves = if (board.sideToMove == side) board.generateLegalMoves() else emptyList()

            val guides = mutableListOf<CastleDirectionGuide>()
            if (ksRookFile in 0..7) {
                guides.add(
                    CastleDirectionGuide(
                        fromKingSq = kingSq,
                        toKingSq = Square.of(6, backRank),
                        fromRookSq = Square.of(ksRookFile, backRank),
                        toRookSq = Square.of(5, backRank),
                        isKingside = true,
                        isCurrentlyLegal = legalMoves.any { it.isCastling && it.castleKingside }
                    )
                )
            }
            if (qsRookFile in 0..7) {
                guides.add(
                    CastleDirectionGuide(
                        fromKingSq = kingSq,
                        toKingSq = Square.of(2, backRank),
                        fromRookSq = Square.of(qsRookFile, backRank),
                        toRookSq = Square.of(3, backRank),
                        isKingside = false,
                        isCurrentlyLegal = legalMoves.any { it.isCastling && !it.castleKingside }
                    )
                )
            }
            return guides
        }

        private fun ratingForFormat(profile: PlayerProfileEntity, formatKey: String): Int = when (formatKey) {
            "BULLET" -> profile.bulletRating
            "BLITZ" -> profile.blitzRating
            "RAPID" -> profile.rapidRating
            "CLASSICAL" -> profile.classicalRating
            else -> profile.fischerRating
        }

        private fun gamesForFormat(profile: PlayerProfileEntity, formatKey: String): Int = when (formatKey) {
            "BULLET" -> profile.bulletWins + profile.bulletLosses + profile.bulletDraws
            "BLITZ" -> profile.blitzWins + profile.blitzLosses + profile.blitzDraws
            "RAPID" -> profile.rapidWins + profile.rapidLosses + profile.rapidDraws
            "CLASSICAL" -> profile.classicalWins + profile.classicalLosses + profile.classicalDraws
            else -> profile.fischerWins + profile.fischerLosses + profile.fischerDraws
        }
    }
}
