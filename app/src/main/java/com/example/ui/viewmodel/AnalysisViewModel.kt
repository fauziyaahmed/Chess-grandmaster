package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.engine.AnalyzedMove
import com.example.chess.engine.Chess960
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.EngineEvaluation
import com.example.chess.engine.GameAnalysisSummary
import com.example.chess.engine.GameVariant
import com.example.chess.engine.Move
import com.example.chess.engine.OpeningBook
import com.example.chess.engine.OpeningExplorer
import com.example.chess.engine.OpeningExplorerPosition
import com.example.data.local.MatchHistoryEntity
import com.example.data.repository.ChessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnalysisUiState(
    val title: String = "Analysis Board",
    val variant: GameVariant = GameVariant.STANDARD,
    val chess960Pos: Int = Chess960.STANDARD_POSITION_NUMBER,
    val startFen: String = ChessBoard.STANDARD_FEN,
    val boards: List<ChessBoard> = listOf(ChessBoard.standard()),
    val sanMoves: List<String> = emptyList(),
    val moveSquares: List<Pair<Int, Int>> = emptyList(),
    val currentPly: Int = 0,
    val flipped: Boolean = false,
    val perspectiveSide: com.example.chess.engine.PieceColor = com.example.chess.engine.PieceColor.WHITE,
    val selectedSquare: Int? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val liveEval: EngineEvaluation? = null,
    val isEvaluating: Boolean = false,
    val fullAnalysis: GameAnalysisSummary? = null,
    val fullAnalysisProgress: Float? = null,
    val isAutoPlaying: Boolean = false,
    val openingEco: String = "A00",
    val openingName: String = "Starting Position",
    val loadedPgn: String = "",
    val explorerPosition: OpeningExplorerPosition? = null
)

class AnalysisViewModel(
    private val repository: ChessRepository
) : ViewModel() {

    private val engine = ChessEngine()
    private val _state = MutableStateFlow(AnalysisUiState())
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private var evalJob: Job? = null
    private var fullAnalysisJob: Job? = null
    private var autoPlayJob: Job? = null

    init {
        evaluateCurrentPosition()
    }

    fun loadMatchById(matchId: Long) {
        viewModelScope.launch {
            val match = repository.getMatchById(matchId) ?: return@launch
            loadMatch(match)
        }
    }

    fun loadMatch(match: MatchHistoryEntity) {
        val variant = if (match.variant == GameVariant.FISCHER960.name) GameVariant.FISCHER960 else GameVariant.STANDARD
        val sanList = if (match.movesSanCsv.isBlank()) emptyList() else match.movesSanCsv.split("|")
        val side = if (match.playerSide == com.example.chess.engine.PieceColor.BLACK.name) {
            com.example.chess.engine.PieceColor.BLACK
        } else {
            com.example.chess.engine.PieceColor.WHITE
        }
        loadGameSequence(
            title = "${match.whiteName} vs ${match.blackName}",
            startFen = match.startFen,
            sanMoves = sanList,
            variant = variant,
            chess960Pos = match.chess960Position,
            pgn = match.pgnText,
            perspectiveSide = side
        )
    }

    fun loadFen(fen: String, variant: GameVariant = GameVariant.STANDARD, chess960Pos: Int = Chess960.STANDARD_POSITION_NUMBER) {
        try {
            val board = ChessBoard.fromFen(fen, variant, chess960Pos)
            loadGameSequence(
                title = if (variant == GameVariant.FISCHER960) "Chess960 #$chess960Pos" else "Custom FEN",
                startFen = board.toFen(),
                sanMoves = emptyList(),
                variant = variant,
                chess960Pos = chess960Pos,
                pgn = ""
            )
        } catch (_: Exception) {
            // Ignore malformed FEN
        }
    }

    fun resetStandardBoard() {
        loadGameSequence(
            title = "Analysis Board",
            startFen = ChessBoard.STANDARD_FEN,
            sanMoves = emptyList(),
            variant = GameVariant.STANDARD,
            chess960Pos = Chess960.STANDARD_POSITION_NUMBER,
            pgn = ""
        )
    }

    private fun loadGameSequence(
        title: String,
        startFen: String,
        sanMoves: List<String>,
        variant: GameVariant,
        chess960Pos: Int,
        pgn: String,
        perspectiveSide: com.example.chess.engine.PieceColor = com.example.chess.engine.PieceColor.WHITE
    ) {
        fullAnalysisJob?.cancel()
        autoPlayJob?.cancel()

        val startBoard = ChessBoard.fromFen(startFen, variant, chess960Pos)
        val boards = mutableListOf(startBoard)
        val validSans = mutableListOf<String>()
        val squares = mutableListOf<Pair<Int, Int>>()
        var cur = startBoard
        for (san in sanMoves) {
            val m = cur.parseSan(san) ?: break
            validSans.add(cur.toSan(m))
            squares.add(m.from to m.to)
            cur = cur.makeMove(m)
            boards.add(cur)
        }
        val (eco, openName) = OpeningBook.identifyOpening(validSans, variant, chess960Pos)
        val currentBoard = boards.last()
        val explorer = OpeningExplorer.lookup(currentBoard, variant, chess960Pos)
        _state.value = AnalysisUiState(
            title = title,
            variant = variant,
            chess960Pos = chess960Pos,
            startFen = startFen,
            boards = boards,
            sanMoves = validSans,
            moveSquares = squares,
            currentPly = boards.lastIndex,
            flipped = perspectiveSide == com.example.chess.engine.PieceColor.BLACK,
            perspectiveSide = perspectiveSide,
            openingEco = if (explorer.eco != "A00" && validSans.isEmpty()) explorer.eco else eco,
            openingName = if (validSans.isEmpty()) openName else if (eco == "A00" && explorer.eco != "A00") explorer.openingName else openName,
            loadedPgn = pgn,
            explorerPosition = explorer
        )
        evaluateCurrentPosition()
    }

    fun goToPly(ply: Int) {
        val cur = _state.value
        val clamped = ply.coerceIn(0, cur.boards.lastIndex)
        val boardAtPly = cur.boards[clamped]
        val explorer = OpeningExplorer.lookup(boardAtPly, cur.variant, cur.chess960Pos)
        val (eco, openName) = OpeningBook.identifyOpening(cur.sanMoves.take(clamped), cur.variant, cur.chess960Pos)
        _state.update {
            it.copy(
                currentPly = clamped,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                openingEco = if (eco == "A00" && explorer.eco != "A00") explorer.eco else eco,
                openingName = if (clamped == 0) "Starting Position" else if (eco == "A00" && explorer.eco != "A00") explorer.openingName else openName,
                explorerPosition = explorer
            )
        }
        evaluateCurrentPosition()
    }

    fun toggleFlip() {
        _state.update { it.copy(flipped = !it.flipped) }
    }

    fun toggleAutoPlay() {
        val cur = _state.value
        if (cur.isAutoPlaying) {
            autoPlayJob?.cancel()
            _state.update { it.copy(isAutoPlaying = false) }
            return
        }
        autoPlayJob = viewModelScope.launch {
            if (_state.value.currentPly >= _state.value.boards.lastIndex) {
                goToPly(0)
            }
            _state.update { it.copy(isAutoPlaying = true) }
            while (_state.value.currentPly < _state.value.boards.lastIndex) {
                delay(750L)
                goToPly(_state.value.currentPly + 1)
            }
            _state.update { it.copy(isAutoPlaying = false) }
        }
    }

    fun onSquareTapped(sq: Int) {
        val cur = _state.value
        val board = cur.boards[cur.currentPly]
        val selected = cur.selectedSquare
        if (selected != null) {
            val move = cur.legalMovesForSelected.firstOrNull {
                it.to == sq || (it.isCastling && it.castleRookFrom == sq)
            }
            if (move != null) {
                applyAnalysisMove(cur, board, move)
                return
            }
        }
        val piece = board.pieceAt(sq)
        if (piece != null && piece.color == board.sideToMove) {
            _state.update {
                it.copy(selectedSquare = sq, legalMovesForSelected = board.generateLegalMovesFrom(sq))
            }
        } else {
            _state.update { it.copy(selectedSquare = null, legalMovesForSelected = emptyList()) }
        }
    }

    fun playExplorerMove(san: String) {
        val cur = _state.value
        val board = cur.boards[cur.currentPly]
        val move = board.parseSan(san) ?: return
        applyAnalysisMove(cur, board, move)
    }

    private fun applyAnalysisMove(cur: AnalysisUiState, board: ChessBoard, move: Move) {
        val san = board.toSan(move)
        val nextBoard = board.makeMove(move)
        val prefixBoards = cur.boards.subList(0, cur.currentPly + 1) + nextBoard
        val prefixSans = cur.sanMoves.subList(0, cur.currentPly) + san
        val prefixSquares = cur.moveSquares.subList(0, cur.currentPly) + (move.from to move.to)
        val (eco, openName) = OpeningBook.identifyOpening(prefixSans, cur.variant, cur.chess960Pos)
        val explorer = OpeningExplorer.lookup(nextBoard, cur.variant, cur.chess960Pos)
        _state.update {
            it.copy(
                boards = prefixBoards,
                sanMoves = prefixSans,
                moveSquares = prefixSquares,
                currentPly = prefixBoards.lastIndex,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                openingEco = if (eco == "A00" && explorer.eco != "A00") explorer.eco else eco,
                openingName = if (eco == "A00" && explorer.eco != "A00") explorer.openingName else openName,
                fullAnalysis = null,
                explorerPosition = explorer
            )
        }
        evaluateCurrentPosition()
    }

    private fun evaluateCurrentPosition() {
        evalJob?.cancel()
        val cur = _state.value
        val board = cur.boards[cur.currentPly]
        val explorer = cur.explorerPosition ?: OpeningExplorer.lookup(board, cur.variant, cur.chess960Pos)
        if (cur.explorerPosition == null) {
            _state.update { it.copy(explorerPosition = explorer) }
        }
        evalJob = viewModelScope.launch {
            _state.update { it.copy(isEvaluating = true) }
            val result = withContext(Dispatchers.Default) {
                engine.analyzePosition(board, maxDepth = 5, timeLimitMs = 650L)
            }
            _state.update { it.copy(liveEval = result, isEvaluating = false) }
        }
    }

    fun runFullGameAnalysis() {
        val cur = _state.value
        if (cur.sanMoves.isEmpty()) return
        fullAnalysisJob?.cancel()
        fullAnalysisJob = viewModelScope.launch {
            _state.update { it.copy(fullAnalysisProgress = 0.05f) }
            val summary = withContext(Dispatchers.Default) {
                engine.analyzeFullGame(
                    sanMoves = cur.sanMoves,
                    startFen = cur.startFen,
                    variant = cur.variant,
                    chess960Pos = cur.chess960Pos,
                    onProgress = { p -> _state.update { it.copy(fullAnalysisProgress = p) } }
                )
            }
            _state.update { it.copy(fullAnalysis = summary, fullAnalysisProgress = null) }
        }
    }

    fun currentAnalyzedMove(): AnalyzedMove? {
        val cur = _state.value
        val summary = cur.fullAnalysis ?: return null
        if (cur.currentPly == 0) return null
        return summary.analyzedMoves.getOrNull(cur.currentPly - 1)
    }

    override fun onCleared() {
        super.onCleared()
        evalJob?.cancel()
        fullAnalysisJob?.cancel()
        autoPlayJob?.cancel()
    }
}
