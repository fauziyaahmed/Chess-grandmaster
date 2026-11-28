package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessPuzzle
import com.example.chess.engine.GameResult
import com.example.chess.engine.Move
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.PuzzleDifficulty
import com.example.chess.engine.PuzzleLibrary
import com.example.chess.engine.Square
import com.example.data.ChessSoundPlayer
import com.example.data.SoundEffect
import com.example.data.SoundPack
import com.example.data.repository.ChessRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PuzzleStatus {
    SOLVING,
    WRONG_TRY,
    SOLVED
}

data class PuzzleUiState(
    val currentPuzzle: ChessPuzzle = PuzzleLibrary.puzzleForDate(),
    val isDailyChallenge: Boolean = true,
    val selectedDifficultyFilter: PuzzleDifficulty? = null,
    val board: ChessBoard = ChessBoard.fromFen(PuzzleLibrary.puzzleForDate().fen),
    val stepIndex: Int = 0,
    val selectedSquare: Int? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val lastMoveFrom: Int? = null,
    val lastMoveTo: Int? = null,
    val hintFrom: Int? = null,
    val hintTo: Int? = null,
    val hintsUsed: Int = 0,
    val status: PuzzleStatus = PuzzleStatus.SOLVING,
    val ratingDelta: Int? = null,
    val showHintText: Boolean = false,
    val playerSide: PieceColor = PieceColor.WHITE,
    val isDailyStreakMode: Boolean = false,
    val streakRunCurrent: Int = 0,
    val streakRunTotal: Int = 0,
    val streakRunIndex: Int = 0,
    val streakRunEnded: Boolean = false
)

class PuzzleViewModel(
    private val repository: ChessRepository,
    private val soundPlayer: ChessSoundPlayer
) : ViewModel() {

    private val _state = MutableStateFlow(PuzzleUiState())
    val state: StateFlow<PuzzleUiState> = _state.asStateFlow()

    private val _dailySolvedEvents = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val dailySolvedEvents: SharedFlow<Long> = _dailySolvedEvents.asSharedFlow()

    private val _streakRunScoreEvents = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val streakRunScoreEvents: SharedFlow<Int> = _streakRunScoreEvents.asSharedFlow()

    private var soundEnabled = true
    private var hasRecordedAttemptForCurrent = false
    private var streakQueue: List<ChessPuzzle> = emptyList()

    init {
        loadDailyPuzzle()
    }

    fun syncSound(
        enabled: Boolean,
        volume: Float = 0.85f,
        pack: SoundPack = SoundPack.CLASSIC_WOOD
    ) {
        soundEnabled = enabled
        soundPlayer.configure(volume, pack)
    }

    fun loadDailyPuzzle() {
        loadPuzzle(PuzzleLibrary.puzzleForDate(), isDaily = true)
    }

    fun startDailyStreakRun() {
        val ladder = PuzzleLibrary.streakLadderForDate()
        streakQueue = ladder
        val firstPuzzle = ladder.first()
        hasRecordedAttemptForCurrent = false
        val startBoard = ChessBoard.fromFen(firstPuzzle.fen)
        _state.value = PuzzleUiState(
            currentPuzzle = firstPuzzle,
            isDailyChallenge = true,
            selectedDifficultyFilter = null,
            board = startBoard,
            stepIndex = 0,
            playerSide = startBoard.sideToMove,
            isDailyStreakMode = true,
            streakRunCurrent = 0,
            streakRunTotal = ladder.size,
            streakRunIndex = 0,
            streakRunEnded = false
        )
    }

    fun setDifficultyFilter(difficulty: PuzzleDifficulty?) {
        _state.update { it.copy(selectedDifficultyFilter = difficulty) }
        val pool = if (difficulty == null) PuzzleLibrary.ALL_PUZZLES
        else PuzzleLibrary.ALL_PUZZLES.filter { it.difficulty == difficulty }
        val next = pool.firstOrNull { it.id != _state.value.currentPuzzle.id } ?: pool.first()
        loadPuzzle(next, isDaily = false)
    }

    fun nextTrainingPuzzle() {
        val cur = _state.value
        if (cur.isDailyStreakMode) {
            if (cur.streakRunEnded) {
                startDailyStreakRun()
            } else {
                advanceStreakPuzzle()
            }
            return
        }
        val filter = cur.selectedDifficultyFilter
        val pool = if (filter == null) PuzzleLibrary.ALL_PUZZLES
        else PuzzleLibrary.ALL_PUZZLES.filter { it.difficulty == filter }
        val curIdx = pool.indexOfFirst { it.id == cur.currentPuzzle.id }
        val next = pool[(curIdx + 1) % pool.size]
        loadPuzzle(next, isDaily = false)
    }

    fun advanceStreakPuzzle() {
        val cur = _state.value
        if (!cur.isDailyStreakMode || streakQueue.isEmpty()) {
            startDailyStreakRun()
            return
        }
        val nextIdx = (cur.streakRunIndex + 1) % streakQueue.size
        val nextPuzzle = streakQueue[nextIdx]
        hasRecordedAttemptForCurrent = false
        val startBoard = ChessBoard.fromFen(nextPuzzle.fen)
        _state.value = PuzzleUiState(
            currentPuzzle = nextPuzzle,
            isDailyChallenge = nextIdx == 0,
            selectedDifficultyFilter = null,
            board = startBoard,
            stepIndex = 0,
            playerSide = startBoard.sideToMove,
            isDailyStreakMode = true,
            streakRunCurrent = cur.streakRunCurrent,
            streakRunTotal = streakQueue.size,
            streakRunIndex = nextIdx,
            streakRunEnded = false
        )
    }

    fun loadPuzzle(puzzle: ChessPuzzle, isDaily: Boolean) {
        hasRecordedAttemptForCurrent = false
        val startBoard = ChessBoard.fromFen(puzzle.fen)
        _state.value = PuzzleUiState(
            currentPuzzle = puzzle,
            isDailyChallenge = isDaily,
            selectedDifficultyFilter = _state.value.selectedDifficultyFilter,
            board = startBoard,
            stepIndex = 0,
            playerSide = startBoard.sideToMove,
            isDailyStreakMode = false
        )
    }

    fun retryPuzzle() {
        val cur = _state.value
        if (cur.isDailyStreakMode) {
            startDailyStreakRun()
        } else {
            loadPuzzle(cur.currentPuzzle, cur.isDailyChallenge)
        }
    }

    fun requestHint() {
        val cur = _state.value
        if (cur.status == PuzzleStatus.SOLVED) return
        val expectedUci = cur.currentPuzzle.solutionUci.getOrNull(cur.stepIndex) ?: return
        val fromSq = Square.fromAlgebraic(expectedUci.substring(0, 2))
        val toSq = Square.fromAlgebraic(expectedUci.substring(2, 4))
        _state.update {
            it.copy(
                hintFrom = fromSq,
                hintTo = toSq,
                hintsUsed = it.hintsUsed + 1,
                showHintText = true
            )
        }
    }

    fun onSquareTapped(sq: Int) {
        val cur = _state.value
        if (cur.status == PuzzleStatus.SOLVED || cur.streakRunEnded) return
        if (cur.board.sideToMove != cur.playerSide) return

        val selected = cur.selectedSquare
        if (selected != null) {
            val matching = cur.legalMovesForSelected.filter { it.to == sq }
            if (matching.isNotEmpty()) {
                val expectedUci = cur.currentPuzzle.solutionUci.getOrNull(cur.stepIndex) ?: return
                val chosenMove = if (matching.size > 1) {
                    matching.firstOrNull { it.toUci() == expectedUci } ?: matching.first { it.promotion == PieceType.QUEEN }
                } else {
                    matching[0]
                }
                submitMove(chosenMove)
                return
            }
        }

        val piece = cur.board.pieceAt(sq)
        if (piece != null && piece.color == cur.playerSide) {
            _state.update {
                it.copy(
                    selectedSquare = sq,
                    legalMovesForSelected = cur.board.generateLegalMovesFrom(sq),
                    status = PuzzleStatus.SOLVING
                )
            }
        } else {
            _state.update { it.copy(selectedSquare = null, legalMovesForSelected = emptyList()) }
        }
    }

    private fun submitMove(move: Move) {
        val cur = _state.value
        val puzzle = cur.currentPuzzle
        val expectedUci = puzzle.solutionUci.getOrNull(cur.stepIndex) ?: return
        val isCapture = cur.board.pieceAt(move.to) != null || move.isEnPassant
        val nextBoard = cur.board.makeMove(move)
        val isCheck = nextBoard.isInCheck(nextBoard.sideToMove)
        val (termRes, termReason) = nextBoard.evaluateTermination()
        val isImmediateMate = (termRes == GameResult.WHITE_WIN && cur.playerSide == PieceColor.WHITE) ||
            (termRes == GameResult.BLACK_WIN && cur.playerSide == PieceColor.BLACK)

        if (move.toUci() == expectedUci || isImmediateMate) {
            val nextStep = cur.stepIndex + 1
            val isComplete = isImmediateMate || nextStep >= puzzle.solutionUci.size

            when {
                isImmediateMate || (isComplete && termReason == com.example.chess.engine.TerminationReason.CHECKMATE) ->
                    soundPlayer.play(SoundEffect.CHECKMATE, soundEnabled)
                isComplete -> soundPlayer.play(SoundEffect.PUZZLE_SUCCESS, soundEnabled)
                isCheck -> soundPlayer.play(SoundEffect.CHECK, soundEnabled)
                move.isCastling -> soundPlayer.play(SoundEffect.CASTLE, soundEnabled)
                move.promotion != null -> soundPlayer.play(SoundEffect.PROMOTE, soundEnabled)
                isCapture -> soundPlayer.play(SoundEffect.CAPTURE, soundEnabled)
                else -> soundPlayer.play(SoundEffect.MOVE, soundEnabled)
            }

            _state.update {
                it.copy(
                    board = nextBoard,
                    stepIndex = nextStep,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    lastMoveFrom = move.from,
                    lastMoveTo = move.to,
                    hintFrom = null,
                    hintTo = null,
                    status = if (isComplete) PuzzleStatus.SOLVED else PuzzleStatus.SOLVING
                )
            }

            if (isComplete) {
                onPuzzleSolved(skipSound = true)
            } else {
                // Play forced opponent reply automatically
                viewModelScope.launch {
                    delay(350L)
                    val afterWait = _state.value
                    val replyUci = puzzle.solutionUci.getOrNull(afterWait.stepIndex) ?: return@launch
                    val replyMove = afterWait.board.generateLegalMoves().firstOrNull { it.toUci() == replyUci }
                    if (replyMove != null) {
                        val replyCapture = afterWait.board.pieceAt(replyMove.to) != null || replyMove.isEnPassant
                        val b2 = afterWait.board.makeMove(replyMove)
                        val replyCheck = b2.isInCheck(b2.sideToMove)
                        when {
                            replyCheck -> soundPlayer.play(SoundEffect.CHECK, soundEnabled)
                            replyCapture -> soundPlayer.play(SoundEffect.CAPTURE, soundEnabled)
                            else -> soundPlayer.play(SoundEffect.OPPONENT_MOVE, soundEnabled)
                        }
                        _state.update {
                            it.copy(
                                board = b2,
                                stepIndex = it.stepIndex + 1,
                                lastMoveFrom = replyMove.from,
                                lastMoveTo = replyMove.to
                            )
                        }
                    }
                }
            }
        } else {
            soundPlayer.play(SoundEffect.PUZZLE_FAIL, soundEnabled)
            _state.update {
                it.copy(
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    status = PuzzleStatus.WRONG_TRY,
                    streakRunEnded = it.isDailyStreakMode
                )
            }
            if (!hasRecordedAttemptForCurrent) {
                hasRecordedAttemptForCurrent = true
                viewModelScope.launch {
                    val delta = repository.recordPuzzleResult(
                        puzzleId = puzzle.id,
                        puzzleTitle = puzzle.title,
                        difficulty = puzzle.difficulty,
                        puzzleRating = puzzle.rating,
                        isDaily = cur.isDailyChallenge,
                        solved = false,
                        hintsUsed = cur.hintsUsed
                    )
                    _state.update { it.copy(ratingDelta = delta) }
                }
            }
        }
    }

    private fun onPuzzleSolved(skipSound: Boolean = false) {
        if (!skipSound) {
            soundPlayer.play(SoundEffect.PUZZLE_SUCCESS, soundEnabled)
        }
        val cur = _state.value
        if (cur.isDailyChallenge) {
            _dailySolvedEvents.tryEmit(LocalDate.now().toEpochDay())
        }
        if (cur.isDailyStreakMode) {
            val newStreakScore = cur.streakRunCurrent + 1
            _state.update { it.copy(streakRunCurrent = newStreakScore) }
            _streakRunScoreEvents.tryEmit(newStreakScore)
        }
        if (!hasRecordedAttemptForCurrent) {
            hasRecordedAttemptForCurrent = true
            viewModelScope.launch {
                val delta = repository.recordPuzzleResult(
                    puzzleId = cur.currentPuzzle.id,
                    puzzleTitle = cur.currentPuzzle.title,
                    difficulty = cur.currentPuzzle.difficulty,
                    puzzleRating = cur.currentPuzzle.rating,
                    isDaily = cur.isDailyChallenge,
                    solved = true,
                    hintsUsed = cur.hintsUsed
                )
                _state.update { it.copy(ratingDelta = delta) }
            }
        }
    }
}
