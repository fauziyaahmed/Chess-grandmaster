package com.example.chess.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class EngineEvaluation(
    val centipawnsWhite: Int,
    val mateIn: Int?, // Positive = White mates in N, Negative = Black mates in N
    val bestMove: Move?,
    val bestMoveSan: String,
    val pvSan: List<String>,
    val depth: Int,
    val nodes: Long
) {
    fun formattedScore(): String {
        if (mateIn != null) {
            return if (mateIn > 0) "M$mateIn" else "-M${abs(mateIn)}"
        }
        val pawns = centipawnsWhite / 100.0
        val sign = if (pawns > 0.05) "+" else ""
        return "$sign${"%.2f".format(pawns)}"
    }

    fun whiteWinProbability(): Float {
        if (mateIn != null) return if (mateIn > 0) 0.99f else 0.01f
        val clamped = centipawnsWhite.coerceIn(-1500, 1500)
        val p = 1.0 / (1.0 + exp(-0.00368208 * clamped))
        return p.toFloat().coerceIn(0.01f, 0.99f)
    }
}

data class AnalyzedMove(
    val plyIndex: Int,
    val moveNumber: Int,
    val isWhite: Boolean,
    val playedSan: String,
    val bestSan: String,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val mateBefore: Int?,
    val mateAfter: Int?,
    val cpLoss: Int,
    val classification: MoveClassification,
    val fenAfter: String
)

data class GameAnalysisSummary(
    val analyzedMoves: List<AnalyzedMove>,
    val whiteAccuracy: Double,
    val blackAccuracy: Double,
    val openingEco: String,
    val openingName: String,
    val whiteCounts: Map<MoveClassification, Int>,
    val blackCounts: Map<MoveClassification, Int>
)

/**
 * Pure-Kotlin high-strength chess engine using Iterative Deepening, Alpha-Beta Pruning,
 * Quiescence Search, Transposition Table, Killer Moves, MVV-LVA, and Tapered PeSTO PST Evaluation.
 */
class ChessEngine {

    private companion object {
        const val INF = 1_000_000
        const val MATE_SCORE = 900_000
        const val TT_SIZE = 1 shl 16 // 65536 slots
        const val TT_MASK = TT_SIZE - 1

        const val FLAG_EXACT = 0
        const val FLAG_LOWER = 1
        const val FLAG_UPPER = 2
    }

    private data class TTEntry(
        val key: Long,
        val depth: Int,
        val score: Int,
        val flag: Int,
        val bestMoveUci: String?
    )

    private val tt = arrayOfNulls<TTEntry>(TT_SIZE)
    private val killerMoves = Array(64) { arrayOfNulls<String>(2) }
    private var nodesVisited = 0L
    private var deadlineMs = Long.MAX_VALUE
    private var aborted = false

    // Zobrist tables
    private val zobristPiece = Array(12) { LongArray(64) }
    private val zobristSide: Long

    init {
        val rng = Random(20261123L)
        for (p in 0 until 12) {
            for (sq in 0 until 64) {
                zobristPiece[p][sq] = rng.nextLong()
            }
        }
        zobristSide = rng.nextLong()
    }

    private fun hashBoard(board: ChessBoard): Long {
        var h = 0L
        for (sq in 0..63) {
            val p = board.squares[sq] ?: continue
            val idx = pieceIndex(p)
            h = h xor zobristPiece[idx][sq]
        }
        if (board.sideToMove == PieceColor.BLACK) h = h xor zobristSide
        if (board.enPassantSquare in 0..63) h = h xor (board.enPassantSquare.toLong() * 0x9E3779B97F4A7C15UL.toLong())
        h = h xor (board.whiteCastleKingsideRookFile.toLong() shl 1)
        h = h xor (board.whiteCastleQueensideRookFile.toLong() shl 5)
        h = h xor (board.blackCastleKingsideRookFile.toLong() shl 9)
        h = h xor (board.blackCastleQueensideRookFile.toLong() shl 13)
        return h
    }

    private fun pieceIndex(p: Piece): Int {
        val base = when (p.type) {
            PieceType.PAWN -> 0
            PieceType.KNIGHT -> 1
            PieceType.BISHOP -> 2
            PieceType.ROOK -> 3
            PieceType.QUEEN -> 4
            PieceType.KING -> 5
        }
        return if (p.color == PieceColor.WHITE) base else base + 6
    }

    /**
     * Selects a move calibrated to Elo skill level (1..8):
     * 1 (~650), 2 (~900), 3 (~1150), 4 (~1400), 5 (~1650), 6 (~1900), 7 (~2100), 8 (~2300+).
     */
    fun chooseMoveForSkill(board: ChessBoard, skillLevel: Int, timeLimitMs: Long = 900L): Move? {
        val legal = board.generateLegalMoves()
        if (legal.isEmpty()) return null
        if (legal.size == 1) return legal[0]

        val clamped = skillLevel.coerceIn(1, 8)
        val (depth, budgetMs, noiseCp) = when (clamped) {
            1 -> Triple(1, 120L, 260)
            2 -> Triple(2, 200L, 160)
            3 -> Triple(2, 300L, 95)
            4 -> Triple(3, 450L, 50)
            5 -> Triple(3, 650L, 20)
            6 -> Triple(4, 900L, 6)
            7 -> Triple(5, 1300L, 0)
            else -> Triple(6, timeLimitMs.coerceAtLeast(1600L), 0)
        }

        if (noiseCp > 0) {
            // Evaluate all legal moves at shallow depth and pick weighted/noisy top choice
            val scored = legal.map { move ->
                val next = board.makeMoveUnchecked(move)
                val score = -negamaxRootEval(next, (depth - 1).coerceAtLeast(0))
                val jitter = Random.nextInt(-noiseCp, noiseCp + 1)
                move to (score + jitter)
            }.sortedByDescending { it.second }
            return scored.first().first
        }

        val eval = analyzePosition(board, maxDepth = depth, timeLimitMs = budgetMs)
        return eval.bestMove ?: legal.first()
    }

    private fun negamaxRootEval(board: ChessBoard, depth: Int): Int {
        if (depth <= 0) return quiescence(board, -INF, INF, 0)
        val legal = orderMoves(board, board.generateLegalMoves(), null, 0)
        if (legal.isEmpty()) {
            return if (board.isInCheck(board.sideToMove)) -MATE_SCORE else 0
        }
        var best = -INF
        for (m in legal) {
            val sc = -negamaxRootEval(board.makeMoveUnchecked(m), depth - 1)
            if (sc > best) best = sc
        }
        return best
    }

    fun analyzePosition(
        board: ChessBoard,
        maxDepth: Int = 5,
        timeLimitMs: Long = 1000L
    ): EngineEvaluation {
        val legal = board.generateLegalMoves()
        if (legal.isEmpty()) {
            val isMate = board.isInCheck(board.sideToMove)
            val whiteScore = if (!isMate) 0 else if (board.sideToMove == PieceColor.WHITE) -MATE_SCORE else MATE_SCORE
            val mateIn = if (!isMate) null else if (board.sideToMove == PieceColor.WHITE) -1 else 1
            return EngineEvaluation(
                centipawnsWhite = whiteScore,
                mateIn = mateIn,
                bestMove = null,
                bestMoveSan = "",
                pvSan = emptyList(),
                depth = 0,
                nodes = 1
            )
        }

        nodesVisited = 0L
        aborted = false
        deadlineMs = System.currentTimeMillis() + timeLimitMs
        for (i in killerMoves.indices) {
            killerMoves[i][0] = null
            killerMoves[i][1] = null
        }

        var bestMove: Move = legal[0]
        var bestScoreSide = evaluateSideToMove(board)
        var reachedDepth = 0

        for (depth in 1..maxDepth) {
            val (score, move) = searchRoot(board, depth, bestMove)
            if (aborted && depth > 1) break
            if (move != null) {
                bestMove = move
                bestScoreSide = score
                reachedDepth = depth
            }
            if (abs(score) > MATE_SCORE - 100) break
            if (System.currentTimeMillis() >= deadlineMs) break
        }

        val whiteCp = if (board.sideToMove == PieceColor.WHITE) bestScoreSide else -bestScoreSide
        val mateIn = if (abs(whiteCp) > MATE_SCORE - 200) {
            val pliesToMate = (MATE_SCORE - abs(whiteCp)).coerceAtLeast(1)
            val movesToMate = (pliesToMate + 1) / 2
            if (whiteCp > 0) movesToMate else -movesToMate
        } else null

        val pvSan = extractPvSan(board, bestMove, reachedDepth.coerceAtMost(5))
        val bestSan = board.toSan(bestMove, legal)

        return EngineEvaluation(
            centipawnsWhite = whiteCp.coerceIn(-25000, 25000),
            mateIn = mateIn,
            bestMove = bestMove,
            bestMoveSan = bestSan,
            pvSan = pvSan,
            depth = reachedDepth,
            nodes = nodesVisited
        )
    }

    private fun searchRoot(board: ChessBoard, depth: Int, pvHint: Move?): Pair<Int, Move?> {
        val legal = orderMoves(board, board.generateLegalMoves(), pvHint?.toUci(), 0)
        var alpha = -INF
        val beta = INF
        var bestScore = -INF
        var bestMove: Move? = null

        for (move in legal) {
            val next = board.makeMove(move)
            val score = -alphaBeta(next, depth - 1, -beta, -alpha, 1)
            if (aborted) return bestScore to bestMove
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) alpha = score
        }
        return bestScore to bestMove
    }

    private fun alphaBeta(board: ChessBoard, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        if ((nodesVisited and 2047L) == 0L && System.currentTimeMillis() >= deadlineMs) {
            aborted = true
            return 0
        }
        nodesVisited++

        if (ply > 0 && (board.isThreefoldRepetition() || board.halfmoveClock >= 100 || board.isInsufficientMaterial())) {
            return 0
        }

        var alpha = alphaIn
        val key = hashBoard(board)
        val ttIdx = (key and TT_MASK.toLong()).toInt()
        val ttEntry = tt[ttIdx]
        var ttMoveUci: String? = null
        if (ttEntry != null && ttEntry.key == key) {
            ttMoveUci = ttEntry.bestMoveUci
            if (ttEntry.depth >= depth) {
                when (ttEntry.flag) {
                    FLAG_EXACT -> return ttEntry.score
                    FLAG_LOWER -> alpha = max(alpha, ttEntry.score)
                    FLAG_UPPER -> if (ttEntry.score <= alpha) return ttEntry.score
                }
                if (alpha >= beta) return ttEntry.score
            }
        }

        if (depth <= 0) {
            return quiescence(board, alpha, beta, ply)
        }

        val legal = orderMoves(board, board.generateLegalMoves(), ttMoveUci, ply)
        if (legal.isEmpty()) {
            return if (board.isInCheck(board.sideToMove)) -MATE_SCORE + ply else 0
        }

        var bestScore = -INF
        var bestMoveUci: String? = null
        val origAlpha = alpha

        for (move in legal) {
            val next = board.makeMoveUnchecked(move)
            val score = -alphaBeta(next, depth - 1, -beta, -alpha, ply + 1)
            if (aborted) return 0

            if (score > bestScore) {
                bestScore = score
                bestMoveUci = move.toUci()
            }
            if (score > alpha) {
                alpha = score
            }
            if (alpha >= beta) {
                if (board.pieceAt(move.to) == null && ply < 64) {
                    val uci = move.toUci()
                    if (killerMoves[ply][0] != uci) {
                        killerMoves[ply][1] = killerMoves[ply][0]
                        killerMoves[ply][0] = uci
                    }
                }
                break
            }
        }

        val flag = when {
            bestScore <= origAlpha -> FLAG_UPPER
            bestScore >= beta -> FLAG_LOWER
            else -> FLAG_EXACT
        }
        tt[ttIdx] = TTEntry(key, depth, bestScore, flag, bestMoveUci)
        return bestScore
    }

    private fun quiescence(board: ChessBoard, alphaIn: Int, beta: Int, ply: Int): Int {
        nodesVisited++
        val standPat = evaluateSideToMove(board)
        if (standPat >= beta) return beta
        var alpha = max(alphaIn, standPat)
        if (ply >= 10) return alpha

        val captures = board.generateLegalMoves().filter { move ->
            board.pieceAt(move.to) != null || move.isEnPassant || move.promotion != null
        }
        val ordered = orderMoves(board, captures, null, ply)
        for (move in ordered) {
            val next = board.makeMoveUnchecked(move)
            val score = -quiescence(next, -beta, -alpha, ply + 1)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderMoves(
        board: ChessBoard,
        moves: List<Move>,
        ttMoveUci: String?,
        ply: Int
    ): List<Move> {
        if (moves.size <= 1) return moves
        val k0 = if (ply < 64) killerMoves[ply][0] else null
        val k1 = if (ply < 64) killerMoves[ply][1] else null

        return moves.sortedByDescending { move ->
            val uci = move.toUci()
            when {
                uci == ttMoveUci -> 100_000
                move.promotion == PieceType.QUEEN -> 90_000
                board.pieceAt(move.to) != null -> {
                    val victim = board.pieceAt(move.to)!!.type.pieceValueCp
                    val attacker = board.pieceAt(move.from)?.type?.pieceValueCp ?: 100
                    50_000 + victim * 10 - attacker
                }
                move.isEnPassant -> 50_900
                uci == k0 -> 40_000
                uci == k1 -> 39_000
                move.isCastling -> 20_000
                else -> 0
            }
        }
    }

    private fun extractPvSan(board: ChessBoard, firstMove: Move, maxLen: Int): List<String> {
        val result = mutableListOf<String>()
        var cur = board
        var m: Move? = firstMove
        val visited = HashSet<Long>()
        repeat(maxLen) {
            val move = m ?: return result
            val legal = cur.generateLegalMoves()
            if (move !in legal) return result
            result.add(cur.toSan(move, legal))
            cur = cur.makeMoveUnchecked(move)
            val h = hashBoard(cur)
            if (!visited.add(h)) return result
            val ttEntry = tt[(h and TT_MASK.toLong()).toInt()]
            val nextUci = ttEntry?.bestMoveUci ?: return result
            m = cur.generateLegalMoves().firstOrNull { it.toUci() == nextUci }
        }
        return result
    }

    /**
     * Tapered Midgame + Endgame evaluation from side-to-move's perspective (in centipawns).
     */
    private fun evaluateSideToMove(board: ChessBoard): Int {
        var mgWhite = 0
        var egWhite = 0
        var mgBlack = 0
        var egBlack = 0
        var gamePhase = 0
        var whiteBishops = 0
        var blackBishops = 0
        var whiteNonKingCount = 0
        var blackNonKingCount = 0
        var whiteKingSq = -1
        var blackKingSq = -1

        val whitePawnFiles = IntArray(8)
        val blackPawnFiles = IntArray(8)

        for (sq in 0..63) {
            val p = board.squares[sq] ?: continue
            val f = Square.fileOf(sq)
            val r = Square.rankOf(sq)
            val pstSq = if (p.color == PieceColor.WHITE) (7 - r) * 8 + f else r * 8 + f

            val mgVal: Int
            val egVal: Int
            when (p.type) {
                PieceType.PAWN -> {
                    mgVal = 82 + MG_PAWN[pstSq]
                    egVal = 94 + EG_PAWN[pstSq]
                    if (p.color == PieceColor.WHITE) whitePawnFiles[f]++ else blackPawnFiles[f]++
                }
                PieceType.KNIGHT -> {
                    mgVal = 337 + MG_KNIGHT[pstSq]
                    egVal = 281 + EG_KNIGHT[pstSq]
                    gamePhase += 1
                }
                PieceType.BISHOP -> {
                    mgVal = 365 + MG_BISHOP[pstSq]
                    egVal = 297 + EG_BISHOP[pstSq]
                    gamePhase += 1
                    if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                }
                PieceType.ROOK -> {
                    mgVal = 477 + MG_ROOK[pstSq]
                    egVal = 512 + EG_ROOK[pstSq]
                    gamePhase += 2
                }
                PieceType.QUEEN -> {
                    mgVal = 1025 + MG_QUEEN[pstSq]
                    egVal = 936 + EG_QUEEN[pstSq]
                    gamePhase += 4
                }
                PieceType.KING -> {
                    mgVal = MG_KING[pstSq]
                    egVal = EG_KING[pstSq]
                }
            }

            if (p.color == PieceColor.WHITE) {
                mgWhite += mgVal
                egWhite += egVal
                if (p.type == PieceType.KING) whiteKingSq = sq else whiteNonKingCount++
            } else {
                mgBlack += mgVal
                egBlack += egVal
                if (p.type == PieceType.KING) blackKingSq = sq else blackNonKingCount++
            }
        }

        // Bishop pair bonus
        if (whiteBishops >= 2) { mgWhite += 30; egWhite += 45 }
        if (blackBishops >= 2) { mgBlack += 30; egBlack += 45 }

        // Doubled & passed pawns + Rook on open/semi-open file
        for (f in 0..7) {
            if (whitePawnFiles[f] > 1) { mgWhite -= 12 * (whitePawnFiles[f] - 1); egWhite -= 18 * (whitePawnFiles[f] - 1) }
            if (blackPawnFiles[f] > 1) { mgBlack -= 12 * (blackPawnFiles[f] - 1); egBlack -= 18 * (blackPawnFiles[f] - 1) }
        }
        for (sq in 0..63) {
            val p = board.squares[sq] ?: continue
            val f = Square.fileOf(sq)
            val r = Square.rankOf(sq)
            if (p.type == PieceType.ROOK) {
                val ownPawns = if (p.color == PieceColor.WHITE) whitePawnFiles[f] else blackPawnFiles[f]
                val enemyPawns = if (p.color == PieceColor.WHITE) blackPawnFiles[f] else whitePawnFiles[f]
                if (ownPawns == 0) {
                    val bonus = if (enemyPawns == 0) 24 else 12
                    if (p.color == PieceColor.WHITE) { mgWhite += bonus; egWhite += bonus }
                    else { mgBlack += bonus; egBlack += bonus }
                }
            } else if (p.type == PieceType.PAWN) {
                val isPassed = if (p.color == PieceColor.WHITE) {
                    (max(0, f - 1)..min(7, f + 1)).all { bf ->
                        (r + 1..7).none { br ->
                            board.squares[Square.of(bf, br)] == Piece(PieceColor.BLACK, PieceType.PAWN)
                        }
                    }
                } else {
                    (max(0, f - 1)..min(7, f + 1)).all { wf ->
                        (0 until r).none { wr ->
                            board.squares[Square.of(wf, wr)] == Piece(PieceColor.WHITE, PieceType.PAWN)
                        }
                    }
                }
                if (isPassed) {
                    val advance = if (p.color == PieceColor.WHITE) r else 7 - r
                    val bonus = advance * advance * 3
                    if (p.color == PieceColor.WHITE) { mgWhite += bonus / 2; egWhite += bonus }
                    else { mgBlack += bonus / 2; egBlack += bonus }
                }
            }
        }

        // Lone-King Endgame Mop-Up: drive defending king to edge/corner & bring attacking king close
        if (whiteKingSq in 0..63 && blackKingSq in 0..63) {
            if (blackNonKingCount == 0 && whiteNonKingCount > 0 && egWhite - egBlack >= 250) {
                val wkF = Square.fileOf(whiteKingSq)
                val wkR = Square.rankOf(whiteKingSq)
                val bkF = Square.fileOf(blackKingSq)
                val bkR = Square.rankOf(blackKingSq)
                val centerDist = abs(bkF - 3.5) + abs(bkR - 3.5)
                val kingProximity = 14 - (abs(wkF - bkF) + abs(wkR - bkR))
                egWhite += (centerDist * 22.0 + kingProximity * 10.0).toInt()
            } else if (whiteNonKingCount == 0 && blackNonKingCount > 0 && egBlack - egWhite >= 250) {
                val wkF = Square.fileOf(whiteKingSq)
                val wkR = Square.rankOf(whiteKingSq)
                val bkF = Square.fileOf(blackKingSq)
                val bkR = Square.rankOf(blackKingSq)
                val centerDist = abs(wkF - 3.5) + abs(wkR - 3.5)
                val kingProximity = 14 - (abs(wkF - bkF) + abs(wkR - bkR))
                egBlack += (centerDist * 22.0 + kingProximity * 10.0).toInt()
            }
        }

        val mgPhase = gamePhase.coerceAtMost(24)
        val egPhase = 24 - mgPhase
        val mgDiff = mgWhite - mgBlack
        val egDiff = egWhite - egBlack
        val whiteScore = (mgDiff * mgPhase + egDiff * egPhase) / 24
        val tempo = 10
        return if (board.sideToMove == PieceColor.WHITE) whiteScore + tempo else -whiteScore + tempo
    }

    /**
     * Evaluates every move of a completed game accurately using the search engine.
     */
    fun analyzeFullGame(
        sanMoves: List<String>,
        startFen: String,
        variant: GameVariant,
        chess960Pos: Int,
        onProgress: (Float) -> Unit = {}
    ): GameAnalysisSummary {
        var board = ChessBoard.fromFen(startFen, variant, chess960Pos)
        val analyzed = mutableListOf<AnalyzedMove>()
        val whiteAccSamples = mutableListOf<Double>()
        val blackAccSamples = mutableListOf<Double>()
        val whiteCounts = MoveClassification.entries.associateWith { 0 }.toMutableMap()
        val blackCounts = MoveClassification.entries.associateWith { 0 }.toMutableMap()

        var prevEval = analyzePosition(board, maxDepth = 3, timeLimitMs = 140L)
        val total = sanMoves.size.coerceAtLeast(1)

        for ((idx, san) in sanMoves.withIndex()) {
            onProgress((idx + 1).toFloat() / total)
            val isWhite = board.sideToMove == PieceColor.WHITE
            val moveNumber = board.fullmoveNumber
            val legal = board.generateLegalMoves()
            val playedMove = board.parseSan(san) ?: break

            val isBook = OpeningBook.isBookPly(sanMoves, idx, variant)
            val nextBoard = board.makeMove(playedMove)
            val afterEval = analyzePosition(nextBoard, maxDepth = 3, timeLimitMs = 140L)

            val bestCpFromMover = if (isWhite) prevEval.centipawnsWhite else -prevEval.centipawnsWhite
            val afterCpFromMover = if (isWhite) afterEval.centipawnsWhite else -afterEval.centipawnsWhite
            val cpLoss = (bestCpFromMover - afterCpFromMover).coerceAtLeast(0)

            // Win probability accuracy per move (Lichess-inspired logistic formula)
            val wpBefore = winProbFromSide(prevEval, isWhite)
            val wpAfter = winProbFromSide(afterEval, isWhite)
            val wpLoss = (wpBefore - wpAfter).coerceAtLeast(0.0)
            val moveAcc = (103.1668 * exp(-0.04354 * wpLoss * 100.0) - 3.1669).coerceIn(0.0, 100.0)

            val isSacrifice = board.pieceAt(playedMove.from)?.type != PieceType.PAWN &&
                board.pieceAt(playedMove.to) == null &&
                nextBoard.isSquareAttacked(playedMove.to, board.sideToMove.opposite()) &&
                cpLoss <= 25 &&
                afterCpFromMover >= -50

            val classification = when {
                isBook -> MoveClassification.BOOK
                playedMove.toUci() == prevEval.bestMove?.toUci() && isSacrifice && wpBefore < 0.85 -> MoveClassification.BRILLIANT
                playedMove.toUci() == prevEval.bestMove?.toUci() || cpLoss <= 10 -> MoveClassification.BEST
                cpLoss <= 25 -> MoveClassification.EXCELLENT
                cpLoss <= 55 -> MoveClassification.GOOD
                cpLoss <= 115 -> MoveClassification.INACCURACY
                cpLoss <= 250 -> MoveClassification.MISTAKE
                else -> MoveClassification.BLUNDER
            }

            if (isWhite) {
                whiteAccSamples.add(moveAcc)
                whiteCounts[classification] = (whiteCounts[classification] ?: 0) + 1
            } else {
                blackAccSamples.add(moveAcc)
                blackCounts[classification] = (blackCounts[classification] ?: 0) + 1
            }

            analyzed.add(
                AnalyzedMove(
                    plyIndex = idx,
                    moveNumber = moveNumber,
                    isWhite = isWhite,
                    playedSan = san,
                    bestSan = prevEval.bestMoveSan.ifBlank { san },
                    evalBeforeCp = prevEval.centipawnsWhite,
                    evalAfterCp = afterEval.centipawnsWhite,
                    mateBefore = prevEval.mateIn,
                    mateAfter = afterEval.mateIn,
                    cpLoss = cpLoss,
                    classification = classification,
                    fenAfter = nextBoard.toFen()
                )
            )

            board = nextBoard
            prevEval = afterEval
        }

        val (eco, openingName) = OpeningBook.identifyOpening(sanMoves, variant, chess960Pos)
        return GameAnalysisSummary(
            analyzedMoves = analyzed,
            whiteAccuracy = if (whiteAccSamples.isEmpty()) 100.0 else whiteAccSamples.average(),
            blackAccuracy = if (blackAccSamples.isEmpty()) 100.0 else blackAccSamples.average(),
            openingEco = eco,
            openingName = openingName,
            whiteCounts = whiteCounts,
            blackCounts = blackCounts
        )
    }

    private fun winProbFromSide(eval: EngineEvaluation, whitePerspective: Boolean): Double {
        val wpWhite = eval.whiteWinProbability().toDouble()
        return if (whitePerspective) wpWhite else (1.0 - wpWhite)
    }

    // PeSTO-style Midgame & Endgame Piece-Square Tables (a8=0 .. h1=63)
    private val MG_PAWN = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        98, 134, 61, 95, 68, 126, 34, -11,
        -6, 7, 26, 31, 65, 56, 25, -20,
        -14, 13, 6, 21, 23, 12, 17, -23,
        -27, -2, -5, 12, 17, 6, 10, -25,
        -26, -4, -4, -10, 3, 3, 33, -12,
        -35, -1, -20, -23, -15, 24, 38, -22,
        0, 0, 0, 0, 0, 0, 0, 0
    )
    private val EG_PAWN = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        178, 173, 158, 134, 147, 132, 165, 187,
        94, 100, 85, 67, 56, 53, 82, 84,
        32, 24, 13, 5, -2, 4, 17, 17,
        13, 9, -3, -7, -7, -8, 3, -1,
        4, 7, -6, 1, 0, -5, -1, -8,
        13, 8, 8, 10, 13, 0, 2, -7,
        0, 0, 0, 0, 0, 0, 0, 0
    )
    private val MG_KNIGHT = intArrayOf(
        -167, -89, -34, -49, 61, -97, -15, -107,
        -73, -41, 72, 36, 23, 62, 7, -17,
        -47, 60, 37, 65, 84, 129, 73, 44,
        -9, 17, 19, 53, 37, 69, 18, 22,
        -13, 4, 16, 13, 28, 19, 21, -8,
        -23, -9, 12, 10, 19, 17, 25, -16,
        -29, -53, -12, -3, -1, 18, -14, -19,
        -105, -21, -58, -33, -17, -28, -19, -23
    )
    private val EG_KNIGHT = intArrayOf(
        -58, -38, -13, -28, -31, -27, -63, -99,
        -25, -8, -25, -2, -9, -25, -24, -52,
        -24, -20, 10, 9, -1, -9, -19, -41,
        -17, 3, 22, 22, 22, 11, 8, -18,
        -18, -6, 16, 25, 16, 17, 4, -18,
        -23, -3, -1, 15, 10, -3, -20, -22,
        -42, -20, -10, -5, -2, -20, -23, -44,
        -29, -51, -23, -15, -22, -18, -50, -64
    )
    private val MG_BISHOP = intArrayOf(
        -29, 4, -82, -37, -25, -42, 7, -8,
        -26, 16, -18, -13, 30, 59, 18, -47,
        -16, 37, 43, 40, 35, 50, 37, -2,
        -4, 5, 19, 50, 37, 37, 7, -2,
        -6, 13, 13, 26, 34, 12, 10, 4,
        0, 15, 15, 15, 14, 27, 18, 10,
        4, 15, 16, 0, 7, 21, 33, 1,
        -33, -3, -14, -21, -13, -12, -39, -21
    )
    private val EG_BISHOP = intArrayOf(
        -14, -21, -11, -8, -7, -9, -17, -24,
        -8, -4, 7, -12, -3, -13, -4, -14,
        2, -8, 0, -1, -2, 6, 0, 4,
        -3, 9, 12, 9, 14, 10, 3, 2,
        -6, 3, 13, 19, 7, 10, -3, -9,
        -12, -3, 8, 10, 13, 3, -7, -15,
        -14, -18, -7, -1, 4, -9, -15, -27,
        -23, -9, -23, -5, -9, -16, -5, -17
    )
    private val MG_ROOK = intArrayOf(
        32, 42, 32, 51, 63, 9, 31, 43,
        27, 32, 58, 62, 80, 67, 26, 44,
        -5, 19, 26, 36, 17, 45, 61, 16,
        -24, -11, 7, 26, 24, 35, -8, -20,
        -36, -26, -12, -1, 9, -7, 6, -23,
        -45, -25, -16, -17, 3, 0, -5, -33,
        -44, -16, -20, -9, -1, 11, -6, -71,
        -19, -13, 1, 17, 16, 7, -37, -26
    )
    private val EG_ROOK = intArrayOf(
        13, 10, 18, 15, 12, 12, 8, 5,
        11, 13, 13, 11, -3, 3, 8, 3,
        7, 7, 7, 5, 4, -3, -5, -3,
        4, 3, 13, 1, 2, 1, -1, 2,
        3, 5, 8, 4, -5, -6, -8, -11,
        -4, 0, -5, -1, -7, -12, -8, -16,
        -6, -6, 0, 2, -9, -9, -11, -3,
        -9, 2, 3, -1, -5, -13, 4, -20
    )
    private val MG_QUEEN = intArrayOf(
        -28, 0, 29, 12, 59, 44, 43, 45,
        -24, -39, -5, 1, -16, 57, 28, 54,
        -13, -17, 7, 8, 29, 56, 47, 57,
        -27, -27, -16, -16, -1, 17, -2, 1,
        -9, -26, -9, -10, -2, -4, 3, -3,
        -14, 2, -11, -2, -5, 2, 14, 5,
        -35, -8, 11, 2, 8, 15, -3, 1,
        -1, -18, -9, 10, -15, -25, -31, -50
    )
    private val EG_QUEEN = intArrayOf(
        -9, 22, 22, 27, 27, 19, 10, 20,
        -17, 20, 32, 41, 58, 25, 30, 0,
        -20, 6, 9, 49, 47, 35, 19, 9,
        3, 22, 24, 45, 57, 40, 57, 36,
        -18, 28, 19, 47, 31, 34, 39, 23,
        -16, -27, 15, 6, 9, 17, 10, 5,
        -22, -23, -30, -16, -16, -23, -36, -32,
        -33, -28, -22, -43, -5, -32, -20, -41
    )
    private val MG_KING = intArrayOf(
        -65, 23, 16, -15, -56, -34, 2, 13,
        29, -1, -20, -7, -8, -4, -38, -29,
        -9, 24, 2, -16, -20, 6, 22, -22,
        -17, -20, -12, -27, -30, -25, -14, -36,
        -49, -1, -27, -39, -46, -44, -33, -51,
        -14, -14, -22, -46, -44, -30, -15, -27,
        1, 7, -8, -64, -43, -16, 9, 8,
        -15, 36, 12, -54, 8, -28, 24, 14
    )
    private val EG_KING = intArrayOf(
        -74, -35, -18, -18, -11, 15, 4, -17,
        -12, 17, 14, 17, 17, 38, 23, 11,
        10, 17, 23, 15, 20, 45, 44, 13,
        -8, 22, 24, 27, 26, 33, 26, 3,
        -18, -4, 21, 24, 27, 23, 9, -11,
        -19, -3, 11, 21, 23, 16, 7, -9,
        -27, -11, 4, 13, 14, 4, -5, -17,
        -53, -33, -21, -11, -28, -14, -24, -43
    )
}
