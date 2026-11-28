package com.example.chess.coach

import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.EngineEvaluation
import com.example.chess.engine.GameVariant
import com.example.chess.engine.Move
import com.example.chess.engine.MoveClassification
import com.example.chess.engine.OpeningBook
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.Square
import kotlin.math.abs

enum class CoachQueryType(val label: String) {
    HINT_BEST_MOVE("Hint & Arrow"),
    CANDIDATE_LINES("Top 2 Moves"),
    ANALYZE_THREATS("Threats"),
    STRATEGIC_PLAN("Plan"),
    OPENING_ENDGAME_LESSON("Lesson"),
    EXPLAIN_LAST_MOVE("Review Move")
}

data class CoachFeedback(
    val headline: String,
    val message: String,
    val evalFormatted: String,
    val centipawnsWhite: Int,
    val mateIn: Int?,
    val bestMoveSan: String,
    val bestMoveFrom: Int? = null,
    val bestMoveTo: Int? = null,
    val pvLine: String = "",
    val moveGrade: MoveClassification? = null,
    val cpLoss: Int = 0,
    val candidateMoves: List<String> = emptyList(),
    val tacticalTag: String? = null,
    val phaseTitle: String = "Opening",
    val shouldWarnMistake: Boolean = false
)

interface CoachRepository {
    suspend fun evaluateMoveFeedback(
        boardBefore: ChessBoard,
        boardAfter: ChessBoard,
        playedMove: Move,
        playedSan: String,
        sanHistoryAfter: List<String>,
        playerColor: PieceColor?
    ): CoachFeedback

    suspend fun answerCoachQuery(
        queryType: CoachQueryType,
        currentBoard: ChessBoard,
        sanHistory: List<String>,
        lastPlayedSan: String?,
        lastEvalBefore: EngineEvaluation?,
        lastMoveGrade: MoveClassification?
    ): CoachFeedback
}

/**
 * Enhanced Engine-backed Coach David with multi-candidate comparison, tactical motif detection,
 * blunder rescue coaching, phase lessons, and voice-friendly explanations.
 */
class EngineCoachDavidRepository(
    private val engine: ChessEngine = ChessEngine()
) : CoachRepository {

    override suspend fun evaluateMoveFeedback(
        boardBefore: ChessBoard,
        boardAfter: ChessBoard,
        playedMove: Move,
        playedSan: String,
        sanHistoryAfter: List<String>,
        playerColor: PieceColor?
    ): CoachFeedback {
        val moverIsWhite = boardBefore.sideToMove == PieceColor.WHITE
        val prevEval = engine.analyzePosition(boardBefore, maxDepth = 3, timeLimitMs = 220L)
        val afterEval = engine.analyzePosition(boardAfter, maxDepth = 3, timeLimitMs = 220L)

        val bestBeforeFromMover = if (moverIsWhite) prevEval.centipawnsWhite else -prevEval.centipawnsWhite
        val afterFromMover = if (moverIsWhite) afterEval.centipawnsWhite else -afterEval.centipawnsWhite
        val cpLoss = (bestBeforeFromMover - afterFromMover).coerceAtLeast(0)

        val isBook = OpeningBook.isBookPly(sanHistoryAfter, sanHistoryAfter.lastIndex, boardBefore.variant)
        val isBest = playedMove.toUci() == prevEval.bestMove?.toUci() || cpLoss <= 12
        val grade = when {
            isBook -> MoveClassification.BOOK
            isBest && cpLoss == 0 && (boardBefore.pieceAt(playedMove.to) != null || afterEval.mateIn != null) ->
                MoveClassification.BRILLIANT
            isBest -> MoveClassification.BEST
            cpLoss <= 28 -> MoveClassification.EXCELLENT
            cpLoss <= 60 -> MoveClassification.GOOD
            cpLoss <= 120 -> MoveClassification.INACCURACY
            cpLoss <= 250 -> MoveClassification.MISTAKE
            else -> MoveClassification.BLUNDER
        }

        val pawnDrop = "%.1f".format(cpLoss / 100.0)
        val betterMove = prevEval.bestMoveSan.ifBlank { playedSan }
        val threats = findHangingPieces(boardAfter, boardBefore.sideToMove)
        val mateAlert = afterEval.mateIn
        val motif = detectTacticalMotif(boardBefore, playedMove, boardAfter)
        val phase = detectPhaseName(boardAfter, sanHistoryAfter)
        val shouldWarn = grade == MoveClassification.BLUNDER || grade == MoveClassification.MISTAKE

        val (headline, msg) = when {
            mateAlert != null && ((moverIsWhite && mateAlert < 0) || (!moverIsWhite && mateAlert > 0)) ->
                "Mate Alert!" to "Coach David: Watch out! $playedSan allows forced mate in ${abs(mateAlert)}. $betterMove kept your king safe. Tap Undo & Fix to try again!"
            grade == MoveClassification.BLUNDER ->
                "Blunder ($playedSan)" to "Coach David: Careful! $playedSan drops about $pawnDrop pawns. ${if (threats.isNotEmpty()) "${threats.first()} is left undefended. " else ""}Playing $betterMove kept the advantage."
            grade == MoveClassification.MISTAKE ->
                "Mistake ($playedSan)" to "Coach David: $playedSan slips about $pawnDrop pawns. $betterMove was stronger with the continuation ${prevEval.pvSan.take(3).joinToString(" ")}."
            grade == MoveClassification.INACCURACY ->
                "Inaccuracy ($playedSan)" to "Coach David: $playedSan is playable, though $betterMove kept tighter control."
            grade == MoveClassification.BRILLIANT ->
                "Sharp Strike ($playedSan)" to "Coach David: Excellent tactical move! $playedSan finds the strongest line (${afterEval.formattedScore()})."
            grade == MoveClassification.BOOK ->
                "Book Theory ($playedSan)" to "Coach David: Solid theoretical move in the $phase! Keep developing minor pieces and securing king safety."
            grade == MoveClassification.BEST ->
                "Top Move ($playedSan)" to "Coach David: Spot on! $playedSan matches the engine's number one choice (${afterEval.formattedScore()})."
            else ->
                "Good Move ($playedSan)" to "Coach David: Sound move with $playedSan. Position evaluates at ${afterEval.formattedScore()}."
        }

        val candidates = buildCandidateSummaries(boardAfter, limit = 2)

        return CoachFeedback(
            headline = headline,
            message = msg,
            evalFormatted = afterEval.formattedScore(),
            centipawnsWhite = afterEval.centipawnsWhite,
            mateIn = afterEval.mateIn,
            bestMoveSan = afterEval.bestMoveSan,
            bestMoveFrom = if (cpLoss > 80) prevEval.bestMove?.from else afterEval.bestMove?.from,
            bestMoveTo = if (cpLoss > 80) prevEval.bestMove?.to else afterEval.bestMove?.to,
            pvLine = afterEval.pvSan.joinToString(" "),
            moveGrade = grade,
            cpLoss = cpLoss,
            candidateMoves = candidates,
            tacticalTag = motif,
            phaseTitle = phase,
            shouldWarnMistake = shouldWarn
        )
    }

    override suspend fun answerCoachQuery(
        queryType: CoachQueryType,
        currentBoard: ChessBoard,
        sanHistory: List<String>,
        lastPlayedSan: String?,
        lastEvalBefore: EngineEvaluation?,
        lastMoveGrade: MoveClassification?
    ): CoachFeedback {
        val eval = engine.analyzePosition(currentBoard, maxDepth = 4, timeLimitMs = 450L)
        val side = currentBoard.sideToMove
        val sideLabel = if (side == PieceColor.WHITE) "White" else "Black"
        val bestMove = eval.bestMove
        val bestSan = eval.bestMoveSan.ifBlank { "-" }
        val pvText = eval.pvSan.joinToString(" ")
        val phase = detectPhaseName(currentBoard, sanHistory)
        val candidates = buildCandidateSummaries(currentBoard, limit = 2)
        val motif = bestMove?.let {
            val nextB = currentBoard.makeMove(it)
            detectTacticalMotif(currentBoard, it, nextB)
        }

        return when (queryType) {
            CoachQueryType.HINT_BEST_MOVE -> {
                val pieceName = bestMove?.let { currentBoard.pieceAt(it.from)?.type?.name?.lowercase() } ?: "piece"
                val captureNote = if (bestMove != null && currentBoard.pieceAt(bestMove.to) != null) "winning material with a capture" else "improving piece activity"
                val tagNote = if (motif != null) " ($motif)" else ""
                CoachFeedback(
                    headline = "Coach David Suggests: $bestSan",
                    message = "Coach David: Move your $pieceName with $bestSan$tagNote, $captureNote. Main line: ${pvText.ifBlank { bestSan }}.",
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText,
                    candidateMoves = candidates,
                    tacticalTag = motif,
                    phaseTitle = phase
                )
            }

            CoachQueryType.CANDIDATE_LINES -> {
                val lineDesc = if (candidates.isEmpty()) "Only $bestSan is available." else candidates.joinToString(" vs ")
                CoachFeedback(
                    headline = "Top 2 Candidate Moves",
                    message = "Coach David: Compare the top two plans: $lineDesc. Best choice is $bestSan.",
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText,
                    candidateMoves = candidates,
                    tacticalTag = motif,
                    phaseTitle = phase
                )
            }

            CoachQueryType.ANALYZE_THREATS -> {
                val inCheck = currentBoard.isInCheck(side)
                val ownHanging = findHangingPieces(currentBoard, side)
                val oppHanging = findHangingPieces(currentBoard, side.opposite())
                val threatMsg = buildString {
                    append("Coach David: ")
                    if (inCheck) append("Your King is in CHECK — resolve it first! ")
                    if (eval.mateIn != null) {
                        val mates = eval.mateIn
                        append(if ((side == PieceColor.WHITE && mates > 0) || (side == PieceColor.BLACK && mates < 0))
                            "You have a forced mate in ${abs(mates)}! "
                        else "Warning: Opponent threatens forced mate in ${abs(mates)}! ")
                    }
                    if (ownHanging.isNotEmpty()) {
                        append("Undefended $sideLabel pieces: ${ownHanging.joinToString(", ")}. ")
                    } else {
                        append("All your pieces are defended. ")
                    }
                    if (oppHanging.isNotEmpty()) {
                        append("Attack targets on opponent's side: ${oppHanging.joinToString(", ")}.")
                    }
                }
                CoachFeedback(
                    headline = "Tactical Threat Scan",
                    message = threatMsg.trim(),
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText,
                    candidateMoves = candidates,
                    tacticalTag = if (ownHanging.isNotEmpty()) "Hanging Piece Alert" else motif,
                    phaseTitle = phase
                )
            }

            CoachQueryType.STRATEGIC_PLAN -> {
                val plan = buildStrategicPlan(currentBoard, sanHistory, eval)
                CoachFeedback(
                    headline = "Coach David's Plan ($sideLabel)",
                    message = plan,
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText,
                    candidateMoves = candidates,
                    tacticalTag = motif,
                    phaseTitle = phase
                )
            }

            CoachQueryType.OPENING_ENDGAME_LESSON -> {
                val lesson = buildPhaseLesson(currentBoard, sanHistory, bestSan)
                CoachFeedback(
                    headline = "$phase Masterclass",
                    message = lesson,
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText,
                    candidateMoves = candidates,
                    tacticalTag = "$phase Lesson",
                    phaseTitle = phase
                )
            }

            CoachQueryType.EXPLAIN_LAST_MOVE -> {
                val msg = if (lastPlayedSan == null) {
                    "Coach David: Make your first move and I'll break down the opening ideas and engine evaluation!"
                } else {
                    val gradeStr = lastMoveGrade?.label ?: "Evaluated"
                    "Coach David: Your last move $lastPlayedSan was rated $gradeStr. Current evaluation is ${eval.formattedScore()}; next strongest idea is $bestSan."
                }
                CoachFeedback(
                    headline = lastPlayedSan?.let { "Review: $it" } ?: "Opening Guidance",
                    message = msg,
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText,
                    moveGrade = lastMoveGrade,
                    candidateMoves = candidates,
                    tacticalTag = motif,
                    phaseTitle = phase
                )
            }
        }
    }

    private fun buildCandidateSummaries(board: ChessBoard, limit: Int): List<String> {
        val legal = board.generateLegalMoves()
        if (legal.isEmpty()) return emptyList()
        val moverIsWhite = board.sideToMove == PieceColor.WHITE

        // Quick 2-ply candidate ranking across top captures/checks/active moves
        val prioritized = legal.sortedByDescending { m ->
            var s = 0
            if (board.pieceAt(m.to) != null) s += 500
            if (m.promotion != null) s += 400
            if (m.isCastling) s += 200
            val next = board.makeMove(m)
            if (next.isInCheck(next.sideToMove)) s += 300
            s
        }.take(8)

        val scored = prioritized.map { m ->
            val next = board.makeMove(m)
            val eval = engine.analyzePosition(next, maxDepth = 2, timeLimitMs = 60L)
            val cpMover = if (moverIsWhite) eval.centipawnsWhite else -eval.centipawnsWhite
            val san = board.toSan(m, legal)
            val label = when {
                eval.mateIn != null -> "$san (#${abs(eval.mateIn)})"
                else -> {
                    val scoreWhite = eval.formattedScore()
                    "$san ($scoreWhite)"
                }
            }
            cpMover to label
        }.sortedByDescending { it.first }

        return scored.take(limit).mapIndexed { idx, pair -> "${idx + 1}. ${pair.second}" }
    }

    private fun detectTacticalMotif(boardBefore: ChessBoard, move: Move, boardAfter: ChessBoard): String? {
        if (boardAfter.isInCheck(boardAfter.sideToMove)) {
            val oppKing = boardAfter.findKing(boardAfter.sideToMove)
            val movedPiece = boardAfter.pieceAt(move.to)
            if (movedPiece?.type == PieceType.KNIGHT && oppKing >= 0) {
                return "Knight Check"
            }
            return "Check Threat"
        }
        if (move.promotion != null) return "Pawn Promotion"
        if (move.isCastling) return "King Safety Castle"
        val movedPiece = boardAfter.pieceAt(move.to) ?: return null
        if (movedPiece.type == PieceType.KNIGHT || movedPiece.type == PieceType.BISHOP || movedPiece.type == PieceType.QUEEN) {
            val highTargets = boardAfter.generateLegalMovesFrom(move.to).count { m ->
                val target = boardAfter.pieceAt(m.to)
                target != null && target.color != movedPiece.color && target.type != PieceType.PAWN
            }
            if (highTargets >= 2) return "Double Attack / Fork"
        }
        if (boardBefore.pieceAt(move.to) != null) return "Material Capture"
        return null
    }

    private fun detectPhaseName(board: ChessBoard, sanHistory: List<String>): String {
        val totalNonKingPieces = (0..63).count {
            val p = board.pieceAt(it)
            p != null && p.type != PieceType.KING && p.type != PieceType.PAWN
        }
        return when {
            sanHistory.size < 10 -> "Opening"
            totalNonKingPieces <= 4 -> "Endgame"
            else -> "Middlegame"
        }
    }

    private fun findHangingPieces(board: ChessBoard, color: PieceColor): List<String> {
        val result = mutableListOf<String>()
        val enemy = color.opposite()
        for (sq in 0..63) {
            val p = board.pieceAt(sq) ?: continue
            if (p.color != color || p.type == PieceType.KING || p.type == PieceType.PAWN) continue
            val attacked = board.isSquareAttacked(sq, enemy)
            val defended = board.isSquareAttacked(sq, color)
            if (attacked && !defended) {
                result.add("${p.type.name.lowercase().replaceFirstChar { it.uppercase() }} on ${Square.toAlgebraic(sq)}")
            }
        }
        return result.take(3)
    }

    private fun buildStrategicPlan(
        board: ChessBoard,
        sanHistory: List<String>,
        eval: EngineEvaluation
    ): String {
        val side = board.sideToMove
        val stats = board.materialStats()
        val advForSide = if (side == PieceColor.WHITE) stats.whiteAdvantage else -stats.whiteAdvantage
        val phase = detectPhaseName(board, sanHistory)
        val canCastle = if (side == PieceColor.WHITE) {
            board.whiteCastleKingsideRookFile >= 0 || board.whiteCastleQueensideRookFile >= 0
        } else {
            board.blackCastleKingsideRookFile >= 0 || board.blackCastleQueensideRookFile >= 0
        }
        val (eco, openName) = OpeningBook.identifyOpening(sanHistory, board.variant, board.chess960Position)

        return buildString {
            append("Coach David: ")
            when (phase) {
                "Opening" -> {
                    append("Opening ($eco $openName): Prioritize central control (e4, d4, c4) and rapid knight and bishop development. ")
                    if (canCastle) append("Look to castle soon to protect your King. ")
                }
                "Endgame" -> {
                    append("Endgame phase: Activate your King toward the center and advance passed pawns with rook support! ")
                }
                else -> {
                    append("Middlegame phase: Coordinate rooks on open files, connect your pieces, and target weak squares. ")
                }
            }
            if (advForSide >= 2) {
                append("You are up +$advForSide in material — simplify by trading pieces cleanly. ")
            } else if (advForSide <= -2) {
                append("You are down ${abs(advForSide)} in material — create active counterplay against the opponent's King. ")
            }
            if (eval.bestMoveSan.isNotBlank()) {
                append("Top continuation: ${eval.pvSan.take(3).joinToString(" ").ifBlank { eval.bestMoveSan }}.")
            }
        }
    }

    private fun buildPhaseLesson(board: ChessBoard, sanHistory: List<String>, bestSan: String): String {
        val phase = detectPhaseName(board, sanHistory)
        val is960 = board.variant == GameVariant.FISCHER960
        return when {
            is960 && phase == "Opening" ->
                "Coach David: In Fischer Chess 960 Position #${board.chess960Position}, check which pawns are undefended on move 1 and follow the castling direction arrows to tuck your King onto g or c cleanly. Best idea here is $bestSan."
            phase == "Opening" ->
                "Coach David: Opening rule of thumb: don't move the same piece twice without a concrete tactic, fight for the center, and castle early. Try $bestSan next."
            phase == "Middlegame" ->
                "Coach David: Middlegame principle: look at your least active piece and improve it toward the center or kingside. Notice checks, captures, and threats before moving — consider $bestSan."
            else ->
                "Coach David: Endgame checklist: King becomes a fighting piece! Push outside passed pawns and place rooks behind passed pawns. Best technique here is $bestSan."
        }
    }
}
