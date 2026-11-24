package com.example.chess.coach

import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.EngineEvaluation
import com.example.chess.engine.Move
import com.example.chess.engine.MoveClassification
import com.example.chess.engine.OpeningBook
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.Square
import kotlin.math.abs

enum class CoachQueryType(val label: String) {
    HINT_BEST_MOVE("Hint & Best Arrow"),
    ANALYZE_THREATS("Threat Check"),
    STRATEGIC_PLAN("Strategic Plan"),
    EXPLAIN_LAST_MOVE("Review Last Move")
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
    val cpLoss: Int = 0
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
 * Engine-backed Coach David implementation using real alpha-beta evaluations,
 * principal variations, hanging-piece detection, king-safety heuristics, and opening knowledge.
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

        val (headline, msg) = when {
            mateAlert != null && ((moverIsWhite && mateAlert < 0) || (!moverIsWhite && mateAlert > 0)) ->
                "Mate Alert!" to "Coach David: Watch out! $playedSan allows forced mate in ${abs(mateAlert)}. $betterMove kept your king safe."
            grade == MoveClassification.BLUNDER ->
                "Blunder ($playedSan)" to "Coach David: $playedSan drops ~$pawnDrop pawns. ${if (threats.isNotEmpty()) "${threats.first()} is vulnerable. " else ""}Better was $betterMove."
            grade == MoveClassification.MISTAKE ->
                "Mistake ($playedSan)" to "Coach David: $playedSan slips ~$pawnDrop pawns. $betterMove was stronger (${prevEval.pvSan.take(3).joinToString(" ")})."
            grade == MoveClassification.INACCURACY ->
                "Inaccuracy ($playedSan)" to "Coach David: Playable, though $betterMove kept tighter control (-$pawnDrop)."
            grade == MoveClassification.BOOK ->
                "Book Theory ($playedSan)" to "Coach David: Solid theoretical move! Continue developing your pieces and contesting the center."
            grade == MoveClassification.BEST ->
                "Top Move ($playedSan)" to "Coach David: Spot on! $playedSan matches the engine's #1 choice (${afterEval.formattedScore()})."
            else ->
                "Good Move ($playedSan)" to "Coach David: Sound choice with $playedSan. Position evaluates at ${afterEval.formattedScore()}."
        }

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
            cpLoss = cpLoss
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

        return when (queryType) {
            CoachQueryType.HINT_BEST_MOVE -> {
                val pieceName = bestMove?.let { currentBoard.pieceAt(it.from)?.type?.name?.lowercase() } ?: "piece"
                val captureNote = if (bestMove != null && currentBoard.pieceAt(bestMove.to) != null) "winning material with a capture" else "improving piece activity"
                CoachFeedback(
                    headline = "Coach David Suggests: $bestSan",
                    message = "Coach David: Move your $pieceName with $bestSan ($captureNote). Main line: ${pvText.ifBlank { bestSan }}.",
                    evalFormatted = eval.formattedScore(),
                    centipawnsWhite = eval.centipawnsWhite,
                    mateIn = eval.mateIn,
                    bestMoveSan = bestSan,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    pvLine = pvText
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
                        append("Your pieces are well defended. ")
                    }
                    if (oppHanging.isNotEmpty()) {
                        append("Targets on opponent's side: ${oppHanging.joinToString(", ")}.")
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
                    pvLine = pvText
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
                    pvLine = pvText
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
                    moveGrade = lastMoveGrade
                )
            }
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
        val totalNonKingPieces = (0..63).count {
            val p = board.pieceAt(it)
            p != null && p.type != PieceType.KING && p.type != PieceType.PAWN
        }
        val canCastle = if (side == PieceColor.WHITE) {
            board.whiteCastleKingsideRookFile >= 0 || board.whiteCastleQueensideRookFile >= 0
        } else {
            board.blackCastleKingsideRookFile >= 0 || board.blackCastleQueensideRookFile >= 0
        }
        val (eco, openName) = OpeningBook.identifyOpening(sanHistory, board.variant, board.chess960Position)

        return buildString {
            append("Coach David: ")
            when {
                sanHistory.size < 10 -> {
                    append("Opening ($eco $openName): Prioritize central control (e4/d4/c4) and rapid knight/bishop development. ")
                    if (canCastle) append("Look to castle soon to tuck your King away. ")
                }
                totalNonKingPieces <= 4 -> {
                    append("Endgame phase: Activate your King toward the center and push passed pawns! ")
                }
                else -> {
                    append("Middlegame phase: Coordinate rooks on open files and target weak squares. ")
                }
            }
            if (advForSide >= 2) {
                append("You are +$advForSide ahead in material — trade pieces cleanly and avoid unnecessary complications. ")
            } else if (advForSide <= -2) {
                append("You are down ${abs(advForSide)} in material — seek counterplay and keep queens/tactics alive. ")
            }
            if (eval.bestMoveSan.isNotBlank()) {
                append("Top continuation: ${eval.pvSan.take(3).joinToString(" ").ifBlank { eval.bestMoveSan }}.")
            }
        }
    }
}
