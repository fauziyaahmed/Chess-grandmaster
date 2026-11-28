package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.coach.CoachVoiceEngine
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.EndgameCategory
import com.example.chess.engine.EndgameDrill
import com.example.chess.engine.EndgameLibrary
import com.example.chess.engine.GameResult
import com.example.chess.engine.Move
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.Square
import com.example.chess.engine.TerminationReason
import com.example.data.ChessSoundPlayer
import com.example.data.SoundEffect
import com.example.data.SoundPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EndgameStatus {
    PLAYING,
    CHECKMATED_WIN,
    STALEMATE_FAIL,
    DRAW_FAIL,
    LOST_FAIL
}

data class EndgameCompletionEvent(
    val drillId: String,
    val stars: Int,
    val movesUsed: Int
)

data class EndgameUiState(
    val selectedCategory: EndgameCategory? = null,
    val currentDrill: EndgameDrill = EndgameLibrary.ALL_DRILLS.first(),
    val board: ChessBoard = ChessBoard.fromFen(EndgameLibrary.ALL_DRILLS.first().fen),
    val boardHistory: List<ChessBoard> = listOf(ChessBoard.fromFen(EndgameLibrary.ALL_DRILLS.first().fen)),
    val sanHistory: List<String> = emptyList(),
    val moveSquaresHistory: List<Pair<Int, Int>> = emptyList(),
    val reviewPly: Int? = null,
    val playerSide: PieceColor = PieceColor.WHITE,
    val movesUsed: Int = 0,
    val selectedSquare: Int? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val lastMoveFrom: Int? = null,
    val lastMoveTo: Int? = null,
    val hintFrom: Int? = null,
    val hintTo: Int? = null,
    val hintsUsed: Int = 0,
    val showControlOverlay: Boolean = true,
    val controlledSquares: Set<Int> = emptySet(),
    val isDefenderThinking: Boolean = false,
    val isHintAnalyzing: Boolean = false,
    val pendingPromotion: PendingPromotion? = null,
    val status: EndgameStatus = EndgameStatus.PLAYING,
    val terminationReason: TerminationReason = TerminationReason.NONE,
    val evalLabel: String = "+9.00",
    val mateInMoves: Int? = null,
    val coachMessage: String = EndgameLibrary.ALL_DRILLS.first().strategyGuide,
    val earnedStars: Int = 0,
    val isVoiceSpeaking: Boolean = false
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

class EndgameViewModel(
    private val soundPlayer: ChessSoundPlayer
) : ViewModel() {

    private val engine = ChessEngine()
    private val _state = MutableStateFlow(EndgameUiState())
    val state: StateFlow<EndgameUiState> = _state.asStateFlow()

    private val _completionEvents = MutableSharedFlow<EndgameCompletionEvent>(extraBufferCapacity = 8)
    val completionEvents: SharedFlow<EndgameCompletionEvent> = _completionEvents.asSharedFlow()

    private var defenderJob: Job? = null
    private var evalJob: Job? = null
    private var voiceObserverJob: Job? = null

    private var soundEnabled: Boolean = true
    private var autoPromoteQueen: Boolean = false
    private var voiceEnabled: Boolean = true
    private var voiceEngine: CoachVoiceEngine? = null

    init {
        loadDrill(EndgameLibrary.ALL_DRILLS.first())
    }

    fun attachVoiceEngine(engine: CoachVoiceEngine?) {
        if (engine != null && voiceEngine !== engine) {
            voiceEngine = engine
            voiceObserverJob?.cancel()
            voiceObserverJob = viewModelScope.launch {
                engine.isSpeaking.collect { speaking ->
                    _state.update { it.copy(isVoiceSpeaking = speaking) }
                }
            }
        }
    }

    fun syncPreferences(
        sound: Boolean,
        autoQueen: Boolean,
        coachVoice: Boolean,
        soundVolume: Float = 0.85f,
        soundPack: SoundPack = SoundPack.CLASSIC_WOOD
    ) {
        soundEnabled = sound
        autoPromoteQueen = autoQueen
        voiceEnabled = coachVoice
        soundPlayer.configure(soundVolume, soundPack)
        if (!coachVoice) {
            voiceEngine?.stop()
        }
    }

    fun selectCategory(category: EndgameCategory?) {
        val pool = EndgameLibrary.drillsForCategory(category)
        val cur = _state.value.currentDrill
        if (category != null && cur.category != category && pool.isNotEmpty()) {
            loadDrill(pool.first(), categoryOverride = category)
        } else {
            _state.update { it.copy(selectedCategory = category) }
        }
    }

    fun loadDrill(drill: EndgameDrill, categoryOverride: EndgameCategory? = _state.value.selectedCategory) {
        defenderJob?.cancel()
        evalJob?.cancel()
        val startBoard = ChessBoard.fromFen(drill.fen)
        val side = startBoard.sideToMove
        val controlled = computeControlledSquares(startBoard, side)
        _state.value = EndgameUiState(
            selectedCategory = categoryOverride,
            currentDrill = drill,
            board = startBoard,
            boardHistory = listOf(startBoard),
            sanHistory = emptyList(),
            playerSide = side,
            movesUsed = 0,
            showControlOverlay = _state.value.showControlOverlay,
            controlledSquares = controlled,
            status = EndgameStatus.PLAYING,
            terminationReason = TerminationReason.NONE,
            coachMessage = drill.strategyGuide,
            evalLabel = "+9.00",
            mateInMoves = null
        )
        refreshLiveEvaluation(startBoard, side)
    }

    fun retryDrill() {
        loadDrill(_state.value.currentDrill)
    }

    fun nextDrill() {
        val pool = EndgameLibrary.drillsForCategory(_state.value.selectedCategory)
        if (pool.isEmpty()) return
        val idx = pool.indexOfFirst { it.id == _state.value.currentDrill.id }
        val next = pool[(idx + 1) % pool.size]
        loadDrill(next)
    }

    fun toggleControlOverlay() {
        _state.update { cur ->
            val next = !cur.showControlOverlay
            cur.copy(
                showControlOverlay = next,
                controlledSquares = if (next) computeControlledSquares(cur.board, cur.playerSide) else emptySet()
            )
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
            if (live.status == EndgameStatus.PLAYING && !live.isDefenderThinking && live.board.sideToMove == live.playerSide) {
                val tapped = live.board.pieceAt(sq)
                if (tapped != null && tapped.color == live.playerSide) {
                    _state.update { it.copy(selectedSquare = sq, legalMovesForSelected = live.board.generateLegalMovesFrom(sq)) }
                }
            }
            return
        }
        if (cur.status != EndgameStatus.PLAYING) return
        if (cur.isDefenderThinking || cur.pendingPromotion != null) return
        if (cur.board.sideToMove != cur.playerSide) return

        val selected = cur.selectedSquare
        if (selected != null) {
            val matching = cur.legalMovesForSelected.filter { it.to == sq }
            if (matching.isNotEmpty()) {
                if (matching.size > 1 && matching.any { it.promotion != null }) {
                    if (autoPromoteQueen) {
                        val q = matching.first { it.promotion == PieceType.QUEEN }
                        playUserMove(q)
                    } else {
                        _state.update { it.copy(pendingPromotion = PendingPromotion(selected, sq)) }
                    }
                } else {
                    playUserMove(matching.first())
                }
                return
            }
        }

        val tappedPiece = cur.board.pieceAt(sq)
        if (tappedPiece != null && tappedPiece.color == cur.playerSide) {
            _state.update {
                it.copy(
                    selectedSquare = sq,
                    legalMovesForSelected = cur.board.generateLegalMovesFrom(sq)
                )
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
        if (move != null) playUserMove(move)
    }

    fun cancelPromotion() {
        _state.update {
            it.copy(pendingPromotion = null, selectedSquare = null, legalMovesForSelected = emptyList())
        }
    }

    private fun playUserMove(move: Move) {
        val cur = _state.value
        val before = cur.board
        val legal = before.generateLegalMoves()
        val san = before.toSan(move, legal)
        val isCapture = before.pieceAt(move.to) != null || move.isEnPassant
        val next = before.makeMove(move)
        val isCheck = next.isInCheck(next.sideToMove)
        val (termResult, termReason) = next.evaluateTermination()

        val nextMovesUsed = cur.movesUsed + 1
        val controlled = if (cur.showControlOverlay) computeControlledSquares(next, cur.playerSide) else emptySet()

        if (termResult != GameResult.IN_PROGRESS) {
            handleDrillTerminated(
                nextBoard = next,
                san = san,
                move = move,
                movesUsed = nextMovesUsed,
                controlled = controlled,
                termResult = termResult,
                termReason = termReason
            )
            return
        }

        when {
            isCheck -> soundPlayer.play(SoundEffect.CHECK, soundEnabled)
            move.promotion != null -> soundPlayer.play(SoundEffect.PROMOTE, soundEnabled)
            isCapture -> soundPlayer.play(SoundEffect.CAPTURE, soundEnabled)
            else -> soundPlayer.play(SoundEffect.MOVE, soundEnabled)
        }

        _state.update {
            it.copy(
                board = next,
                boardHistory = it.boardHistory + next,
                sanHistory = it.sanHistory + san,
                moveSquaresHistory = it.moveSquaresHistory + (move.from to move.to),
                reviewPly = null,
                movesUsed = nextMovesUsed,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = move.from,
                lastMoveTo = move.to,
                hintFrom = null,
                hintTo = null,
                controlledSquares = controlled,
                isDefenderThinking = true,
                coachMessage = if (isCheck) {
                    "Check! Enemy King is forced to respond — keep shrinking its escape squares."
                } else {
                    cur.currentDrill.strategyGuide
                }
            )
        }

        triggerDefenderReply(next)
    }

    private fun triggerDefenderReply(boardAfterUser: ChessBoard) {
        defenderJob?.cancel()
        defenderJob = viewModelScope.launch {
            delay(260L)
            val snap = _state.value
            if (snap.status != EndgameStatus.PLAYING) return@launch
            if (snap.board.sideToMove == snap.playerSide) {
                _state.update { it.copy(isDefenderThinking = false) }
                return@launch
            }

            val eval = withContext(Dispatchers.Default) {
                engine.analyzePosition(boardAfterUser, maxDepth = 4, timeLimitMs = 420L)
            }
            val reply = eval.bestMove ?: boardAfterUser.generateLegalMoves().firstOrNull()
            if (reply == null) {
                _state.update { it.copy(isDefenderThinking = false) }
                return@launch
            }

            val legal = boardAfterUser.generateLegalMoves()
            val replySan = boardAfterUser.toSan(reply, legal)
            val replyCapture = boardAfterUser.pieceAt(reply.to) != null || reply.isEnPassant
            val afterReply = boardAfterUser.makeMove(reply)
            val (termResult, termReason) = afterReply.evaluateTermination()
            val controlled = if (_state.value.showControlOverlay) {
                computeControlledSquares(afterReply, _state.value.playerSide)
            } else emptySet()

            if (termResult != GameResult.IN_PROGRESS) {
                handleDrillTerminated(
                    nextBoard = afterReply,
                    san = replySan,
                    move = reply,
                    movesUsed = _state.value.movesUsed,
                    controlled = controlled,
                    termResult = termResult,
                    termReason = termReason
                )
                return@launch
            }

            when {
                afterReply.isInCheck(afterReply.sideToMove) -> soundPlayer.play(SoundEffect.CHECK, soundEnabled)
                replyCapture -> soundPlayer.play(SoundEffect.CAPTURE, soundEnabled)
                else -> soundPlayer.play(SoundEffect.OPPONENT_MOVE, soundEnabled)
            }

            _state.update {
                it.copy(
                    board = afterReply,
                    boardHistory = it.boardHistory + afterReply,
                    sanHistory = it.sanHistory + replySan,
                    moveSquaresHistory = it.moveSquaresHistory + (reply.from to reply.to),
                    lastMoveFrom = reply.from,
                    lastMoveTo = reply.to,
                    controlledSquares = controlled,
                    isDefenderThinking = false
                )
            }
            refreshLiveEvaluation(afterReply, _state.value.playerSide)
        }
    }

    private fun handleDrillTerminated(
        nextBoard: ChessBoard,
        san: String,
        move: Move,
        movesUsed: Int,
        controlled: Set<Int>,
        termResult: GameResult,
        termReason: TerminationReason
    ) {
        val cur = _state.value
        val playerWon = (termResult == GameResult.WHITE_WIN && cur.playerSide == PieceColor.WHITE) ||
            (termResult == GameResult.BLACK_WIN && cur.playerSide == PieceColor.BLACK)

        val nextStatus = when {
            playerWon -> EndgameStatus.CHECKMATED_WIN
            termReason == TerminationReason.STALEMATE -> EndgameStatus.STALEMATE_FAIL
            termResult == GameResult.DRAW -> EndgameStatus.DRAW_FAIL
            else -> EndgameStatus.LOST_FAIL
        }

        val stars = if (playerWon) {
            calculateStars(movesUsed = movesUsed, parMoves = cur.currentDrill.parMoves, hintsUsed = cur.hintsUsed)
        } else 0

        val feedbackMsg = when (nextStatus) {
            EndgameStatus.CHECKMATED_WIN ->
                "Checkmate in $movesUsed moves (Par ${cur.currentDrill.parMoves})! Technique mastered: ${cur.currentDrill.techniqueName}."
            EndgameStatus.STALEMATE_FAIL ->
                "Stalemate! Black's King is not in check and has zero legal squares. ${cur.currentDrill.stalemateWarning} Tap Undo to fix your last move."
            EndgameStatus.DRAW_FAIL ->
                "Draw by ${termReason.label}. Try keeping tension on the defender's King — tap Undo or Retry!"
            EndgameStatus.LOST_FAIL ->
                "Defense slipped away! Tap Undo or Retry to convert this winning endgame."
            EndgameStatus.PLAYING -> cur.coachMessage
        }

        when {
            playerWon && termReason == TerminationReason.CHECKMATE ->
                soundPlayer.play(SoundEffect.CHECKMATE, soundEnabled)
            playerWon -> soundPlayer.play(SoundEffect.PUZZLE_SUCCESS, soundEnabled)
            nextStatus == EndgameStatus.STALEMATE_FAIL || nextStatus == EndgameStatus.DRAW_FAIL ->
                soundPlayer.play(SoundEffect.DRAW, soundEnabled)
            else -> soundPlayer.play(SoundEffect.PUZZLE_FAIL, soundEnabled)
        }
        if (playerWon) {
            _completionEvents.tryEmit(EndgameCompletionEvent(cur.currentDrill.id, stars, movesUsed))
        }

        _state.update {
            it.copy(
                board = nextBoard,
                boardHistory = it.boardHistory + nextBoard,
                sanHistory = it.sanHistory + san,
                moveSquaresHistory = it.moveSquaresHistory + (move.from to move.to),
                reviewPly = null,
                movesUsed = movesUsed,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = move.from,
                lastMoveTo = move.to,
                hintFrom = null,
                hintTo = null,
                controlledSquares = controlled,
                isDefenderThinking = false,
                status = nextStatus,
                terminationReason = termReason,
                earnedStars = stars,
                coachMessage = feedbackMsg
            )
        }

        if (voiceEnabled && (nextStatus == EndgameStatus.CHECKMATED_WIN || nextStatus == EndgameStatus.STALEMATE_FAIL)) {
            voiceEngine?.speak(feedbackMsg)
        }
    }

    fun undoLastTurn() {
        defenderJob?.cancel()
        val cur = _state.value
        if (cur.boardHistory.size <= 1) return

        var trimmedBoards = cur.boardHistory.dropLast(1)
        var trimmedSans = cur.sanHistory.dropLast(1)
        var trimmedSquares = cur.moveSquaresHistory.dropLast(1)
        while (trimmedBoards.size > 1 && trimmedBoards.last().sideToMove != cur.playerSide) {
            trimmedBoards = trimmedBoards.dropLast(1)
            trimmedSans = trimmedSans.dropLast(1)
            trimmedSquares = trimmedSquares.dropLast(1)
        }

        val rolledBoard = trimmedBoards.last()
        val rolledMovesUsed = trimmedSans.countIndexedBySide(cur.playerSide, cur.boardHistory.first().sideToMove)
        val controlled = if (cur.showControlOverlay) computeControlledSquares(rolledBoard, cur.playerSide) else emptySet()

        _state.update {
            it.copy(
                board = rolledBoard,
                boardHistory = trimmedBoards,
                sanHistory = trimmedSans,
                moveSquaresHistory = trimmedSquares,
                reviewPly = null,
                movesUsed = rolledMovesUsed,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMoveFrom = trimmedSquares.lastOrNull()?.first,
                lastMoveTo = trimmedSquares.lastOrNull()?.second,
                hintFrom = null,
                hintTo = null,
                isDefenderThinking = false,
                pendingPromotion = null,
                status = EndgameStatus.PLAYING,
                terminationReason = TerminationReason.NONE,
                earnedStars = 0,
                controlledSquares = controlled,
                coachMessage = "Move undone — look carefully for checks and cutoff squares!"
            )
        }
        refreshLiveEvaluation(rolledBoard, cur.playerSide)
    }

    fun requestHint() {
        val cur = _state.value
        if (cur.status != EndgameStatus.PLAYING || cur.isDefenderThinking || cur.isHintAnalyzing) return
        if (cur.board.sideToMove != cur.playerSide) return

        viewModelScope.launch {
            _state.update { it.copy(isHintAnalyzing = true) }
            val eval = withContext(Dispatchers.Default) {
                engine.analyzePosition(cur.board, maxDepth = 5, timeLimitMs = 600L)
            }
            val best = eval.bestMove
            if (best == null) {
                _state.update { it.copy(isHintAnalyzing = false) }
                return@launch
            }
            val mateNote = eval.mateIn?.let { m ->
                val absM = kotlin.math.abs(m)
                " (forces mate in $absM)"
            } ?: ""
            val tip = "Hint: Play ${eval.bestMoveSan}$mateNote. ${cur.currentDrill.strategyGuide}"
            _state.update {
                it.copy(
                    hintFrom = best.from,
                    hintTo = best.to,
                    hintsUsed = it.hintsUsed + 1,
                    isHintAnalyzing = false,
                    coachMessage = tip,
                    evalLabel = eval.formattedScore(),
                    mateInMoves = eval.mateIn?.let { m -> kotlin.math.abs(m) }
                )
            }
            if (voiceEnabled) {
                voiceEngine?.speak(tip)
            }
        }
    }

    fun speakCurrentCoachTip() {
        voiceEngine?.speak(_state.value.coachMessage)
    }

    fun stopVoice() {
        voiceEngine?.stop()
    }

    private fun refreshLiveEvaluation(board: ChessBoard, playerSide: PieceColor) {
        evalJob?.cancel()
        evalJob = viewModelScope.launch {
            val eval = withContext(Dispatchers.Default) {
                engine.analyzePosition(board, maxDepth = 4, timeLimitMs = 260L)
            }
            val playerMate = eval.mateIn?.let { m ->
                val isWinningMate = (playerSide == PieceColor.WHITE && m > 0) || (playerSide == PieceColor.BLACK && m < 0)
                if (isWinningMate) kotlin.math.abs(m) else null
            }
            _state.update {
                it.copy(
                    evalLabel = eval.formattedScore(),
                    mateInMoves = playerMate
                )
            }
        }
    }

    private fun List<String>.countIndexedBySide(targetSide: PieceColor, startSide: PieceColor): Int {
        var side = startSide
        var count = 0
        for (i in indices) {
            if (side == targetSide) count++
            side = side.opposite()
        }
        return count
    }

    companion object {
        fun calculateStars(movesUsed: Int, parMoves: Int, hintsUsed: Int): Int {
            return when {
                hintsUsed == 0 && movesUsed <= parMoves -> 3
                movesUsed <= parMoves + 4 && hintsUsed <= 1 -> 2
                else -> 1
            }
        }

        fun computeControlledSquares(board: ChessBoard, bySide: PieceColor): Set<Int> {
            val set = HashSet<Int>(32)
            for (sq in 0..63) {
                if (board.isSquareAttacked(sq, bySide)) {
                    set.add(sq)
                }
            }
            return set
        }
    }
}
