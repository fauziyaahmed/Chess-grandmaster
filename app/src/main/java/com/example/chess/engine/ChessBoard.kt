package com.example.chess.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Full-featured, immutable-friendly chess state machine supporting Standard Chess & Chess960.
 */
class ChessBoard private constructor(
    val squares: Array<Piece?>,
    val sideToMove: PieceColor,
    val whiteCastleKingsideRookFile: Int,
    val whiteCastleQueensideRookFile: Int,
    val blackCastleKingsideRookFile: Int,
    val blackCastleQueensideRookFile: Int,
    val enPassantSquare: Int,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
    val variant: GameVariant,
    val chess960Position: Int,
    val repetitionCounts: Map<String, Int>
) {

    fun pieceAt(sq: Int): Piece? = if (sq in 0..63) squares[sq] else null

    fun copy(): ChessBoard = ChessBoard(
        squares = squares.copyOf(),
        sideToMove = sideToMove,
        whiteCastleKingsideRookFile = whiteCastleKingsideRookFile,
        whiteCastleQueensideRookFile = whiteCastleQueensideRookFile,
        blackCastleKingsideRookFile = blackCastleKingsideRookFile,
        blackCastleQueensideRookFile = blackCastleQueensideRookFile,
        enPassantSquare = enPassantSquare,
        halfmoveClock = halfmoveClock,
        fullmoveNumber = fullmoveNumber,
        variant = variant,
        chess960Position = chess960Position,
        repetitionCounts = HashMap(repetitionCounts)
    )

    fun positionKey(): String {
        val sb = StringBuilder(72)
        for (sq in 0..63) {
            val p = squares[sq]
            sb.append(p?.toFenChar() ?: '.')
        }
        sb.append(if (sideToMove == PieceColor.WHITE) 'w' else 'b')
        sb.append(whiteCastleKingsideRookFile)
        sb.append(whiteCastleQueensideRookFile)
        sb.append(blackCastleKingsideRookFile)
        sb.append(blackCastleQueensideRookFile)
        sb.append(enPassantSquare)
        return sb.toString()
    }

    fun findKing(color: PieceColor): Int {
        for (sq in 0..63) {
            val p = squares[sq]
            if (p != null && p.color == color && p.type == PieceType.KING) return sq
        }
        return -1
    }

    fun isInCheck(color: PieceColor = sideToMove): Boolean {
        val kingSq = findKing(color)
        return kingSq >= 0 && isSquareAttacked(kingSq, color.opposite())
    }

    fun isSquareAttacked(sq: Int, byColor: PieceColor): Boolean {
        val f = Square.fileOf(sq)
        val r = Square.rankOf(sq)

        // Pawn attacks
        val pawnRank = if (byColor == PieceColor.WHITE) r - 1 else r + 1
        if (pawnRank in 0..7) {
            for (df in intArrayOf(-1, 1)) {
                val pf = f + df
                if (pf in 0..7) {
                    val p = squares[Square.of(pf, pawnRank)]
                    if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // Knight attacks
        for (d in KNIGHT_OFFSETS) {
            val nf = f + d[0]
            val nr = r + d[1]
            if (nf in 0..7 && nr in 0..7) {
                val p = squares[Square.of(nf, nr)]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King attacks
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val kf = f + df
                val kr = r + dr
                if (kf in 0..7 && kr in 0..7) {
                    val p = squares[Square.of(kf, kr)]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        // Orthogonal (Rook / Queen)
        for (dir in ORTHO_DIRS) {
            var cf = f + dir[0]
            var cr = r + dir[1]
            while (cf in 0..7 && cr in 0..7) {
                val p = squares[Square.of(cf, cr)]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                cf += dir[0]
                cr += dir[1]
            }
        }

        // Diagonal (Bishop / Queen)
        for (dir in DIAG_DIRS) {
            var cf = f + dir[0]
            var cr = r + dir[1]
            while (cf in 0..7 && cr in 0..7) {
                val p = squares[Square.of(cf, cr)]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                cf += dir[0]
                cr += dir[1]
            }
        }

        return false
    }

    fun generateLegalMoves(): List<Move> {
        val pseudo = generatePseudoLegalMoves()
        val legal = ArrayList<Move>(pseudo.size)
        for (m in pseudo) {
            if (m.isCastling) {
                // Castling already verifies check/path constraints
                legal.add(m)
            } else {
                val next = makeMoveUnchecked(m)
                if (!next.isInCheck(sideToMove)) {
                    legal.add(m)
                }
            }
        }
        return legal
    }

    fun generateLegalMovesFrom(sq: Int): List<Move> =
        generateLegalMoves().filter { it.from == sq }

    private fun generatePseudoLegalMoves(): List<Move> {
        val moves = ArrayList<Move>(48)
        val color = sideToMove
        val enemy = color.opposite()

        for (sq in 0..63) {
            val piece = squares[sq] ?: continue
            if (piece.color != color) continue
            val f = Square.fileOf(sq)
            val r = Square.rankOf(sq)

            when (piece.type) {
                PieceType.PAWN -> {
                    val dir = if (color == PieceColor.WHITE) 1 else -1
                    val startRank = if (color == PieceColor.WHITE) 1 else 6
                    val promoRank = if (color == PieceColor.WHITE) 7 else 0
                    val oneStepRank = r + dir
                    if (oneStepRank in 0..7) {
                        val oneSq = Square.of(f, oneStepRank)
                        if (squares[oneSq] == null) {
                            if (oneStepRank == promoRank) {
                                for (promo in PROMOTION_TYPES) {
                                    moves.add(Move(sq, oneSq, promotion = promo))
                                }
                            } else {
                                moves.add(Move(sq, oneSq))
                                if (r == startRank) {
                                    val twoSq = Square.of(f, r + 2 * dir)
                                    if (squares[twoSq] == null) {
                                        moves.add(Move(sq, twoSq))
                                    }
                                }
                            }
                        }
                        for (df in intArrayOf(-1, 1)) {
                            val cf = f + df
                            if (cf in 0..7) {
                                val capSq = Square.of(cf, oneStepRank)
                                val target = squares[capSq]
                                if (target != null && target.color == enemy) {
                                    if (oneStepRank == promoRank) {
                                        for (promo in PROMOTION_TYPES) {
                                            moves.add(Move(sq, capSq, promotion = promo))
                                        }
                                    } else {
                                        moves.add(Move(sq, capSq))
                                    }
                                } else if (capSq == enPassantSquare) {
                                    moves.add(Move(sq, capSq, isEnPassant = true))
                                }
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    for (d in KNIGHT_OFFSETS) {
                        val nf = f + d[0]
                        val nr = r + d[1]
                        if (nf in 0..7 && nr in 0..7) {
                            val to = Square.of(nf, nr)
                            val t = squares[to]
                            if (t == null || t.color == enemy) moves.add(Move(sq, to))
                        }
                    }
                }

                PieceType.BISHOP -> addSlidingMoves(sq, f, r, DIAG_DIRS, enemy, moves)
                PieceType.ROOK -> addSlidingMoves(sq, f, r, ORTHO_DIRS, enemy, moves)
                PieceType.QUEEN -> {
                    addSlidingMoves(sq, f, r, DIAG_DIRS, enemy, moves)
                    addSlidingMoves(sq, f, r, ORTHO_DIRS, enemy, moves)
                }

                PieceType.KING -> {
                    for (df in -1..1) {
                        for (dr in -1..1) {
                            if (df == 0 && dr == 0) continue
                            val nf = f + df
                            val nr = r + dr
                            if (nf in 0..7 && nr in 0..7) {
                                val to = Square.of(nf, nr)
                                val t = squares[to]
                                if (t == null || t.color == enemy) moves.add(Move(sq, to))
                            }
                        }
                    }
                    // Castling (Standard & Chess960)
                    addCastlingMoves(sq, f, r, color, moves)
                }
            }
        }
        return moves
    }

    private fun addSlidingMoves(
        fromSq: Int,
        f: Int,
        r: Int,
        dirs: Array<IntArray>,
        enemy: PieceColor,
        out: MutableList<Move>
    ) {
        for (dir in dirs) {
            var cf = f + dir[0]
            var cr = r + dir[1]
            while (cf in 0..7 && cr in 0..7) {
                val to = Square.of(cf, cr)
                val p = squares[to]
                if (p == null) {
                    out.add(Move(fromSq, to))
                } else {
                    if (p.color == enemy) out.add(Move(fromSq, to))
                    break
                }
                cf += dir[0]
                cr += dir[1]
            }
        }
    }

    private fun addCastlingMoves(
        kingSq: Int,
        kingFile: Int,
        kingRank: Int,
        color: PieceColor,
        out: MutableList<Move>
    ) {
        val homeRank = if (color == PieceColor.WHITE) 0 else 7
        if (kingRank != homeRank) return
        val enemy = color.opposite()
        if (isSquareAttacked(kingSq, enemy)) return

        val ksRookFile = if (color == PieceColor.WHITE) whiteCastleKingsideRookFile else blackCastleKingsideRookFile
        val qsRookFile = if (color == PieceColor.WHITE) whiteCastleQueensideRookFile else blackCastleQueensideRookFile

        if (ksRookFile in 0..7) {
            tryAddCastle(kingSq, kingFile, homeRank, ksRookFile, 6, 5, true, color, enemy, out)
        }
        if (qsRookFile in 0..7) {
            tryAddCastle(kingSq, kingFile, homeRank, qsRookFile, 2, 3, false, color, enemy, out)
        }
    }

    private fun tryAddCastle(
        kingSq: Int,
        kingFile: Int,
        homeRank: Int,
        rookFile: Int,
        kingTargetFile: Int,
        rookTargetFile: Int,
        isKingside: Boolean,
        color: PieceColor,
        enemy: PieceColor,
        out: MutableList<Move>
    ) {
        val rookSq = Square.of(rookFile, homeRank)
        val rookPiece = squares[rookSq]
        if (rookPiece == null || rookPiece.color != color || rookPiece.type != PieceType.ROOK) return

        // All squares traversed by King and Rook must be vacant (ignoring the King and Rook themselves)
        val minClear = minOf(kingFile, rookFile, kingTargetFile, rookTargetFile)
        val maxClear = maxOf(kingFile, rookFile, kingTargetFile, rookTargetFile)
        for (file in minClear..maxClear) {
            if (file == kingFile || file == rookFile) continue
            val inKingRange = file in min(kingFile, kingTargetFile)..max(kingFile, kingTargetFile)
            val inRookRange = file in min(rookFile, rookTargetFile)..max(rookFile, rookTargetFile)
            if ((inKingRange || inRookRange) && squares[Square.of(file, homeRank)] != null) {
                return
            }
        }

        // King's path (including destination) must not be under attack
        val kStart = min(kingFile, kingTargetFile)
        val kEnd = max(kingFile, kingTargetFile)
        for (file in kStart..kEnd) {
            val checkSq = Square.of(file, homeRank)
            if (checkSq != kingSq && isSquareAttacked(checkSq, enemy)) return
        }

        val kingToSq = Square.of(kingTargetFile, homeRank)
        val rookToSq = Square.of(rookTargetFile, homeRank)
        out.add(
            Move(
                from = kingSq,
                to = kingToSq,
                isCastling = true,
                castleKingside = isKingside,
                castleRookFrom = rookSq,
                castleRookTo = rookToSq
            )
        )
    }

    fun makeMove(move: Move): ChessBoard {
        val next = makeMoveUnchecked(move)
        val key = next.positionKey()
        val updatedCounts = HashMap(next.repetitionCounts)
        updatedCounts[key] = (updatedCounts[key] ?: 0) + 1
        return ChessBoard(
            squares = next.squares,
            sideToMove = next.sideToMove,
            whiteCastleKingsideRookFile = next.whiteCastleKingsideRookFile,
            whiteCastleQueensideRookFile = next.whiteCastleQueensideRookFile,
            blackCastleKingsideRookFile = next.blackCastleKingsideRookFile,
            blackCastleQueensideRookFile = next.blackCastleQueensideRookFile,
            enPassantSquare = next.enPassantSquare,
            halfmoveClock = next.halfmoveClock,
            fullmoveNumber = next.fullmoveNumber,
            variant = variant,
            chess960Position = chess960Position,
            repetitionCounts = updatedCounts
        )
    }

    internal fun makeMoveUnchecked(move: Move): ChessBoard {
        val nextSquares = squares.copyOf()
        val moving = nextSquares[move.from]
            ?: error("No piece on from square ${Square.toAlgebraic(move.from)}")
        val captured = nextSquares[move.to]

        var wKs = whiteCastleKingsideRookFile
        var wQs = whiteCastleQueensideRookFile
        var bKs = blackCastleKingsideRookFile
        var bQs = blackCastleQueensideRookFile

        // Revoke castling rights if king moves or rook moves/is captured
        if (moving.type == PieceType.KING) {
            if (moving.color == PieceColor.WHITE) {
                wKs = -1; wQs = -1
            } else {
                bKs = -1; bQs = -1
            }
        }
        if (moving.type == PieceType.ROOK) {
            val fromRank = Square.rankOf(move.from)
            val fromFile = Square.fileOf(move.from)
            if (moving.color == PieceColor.WHITE && fromRank == 0) {
                if (fromFile == wKs) wKs = -1
                if (fromFile == wQs) wQs = -1
            } else if (moving.color == PieceColor.BLACK && fromRank == 7) {
                if (fromFile == bKs) bKs = -1
                if (fromFile == bQs) bQs = -1
            }
        }
        // Check captured rook on home rank
        val toRank = Square.rankOf(move.to)
        val toFile = Square.fileOf(move.to)
        if (toRank == 0) {
            if (toFile == wKs) wKs = -1
            if (toFile == wQs) wQs = -1
        } else if (toRank == 7) {
            if (toFile == bKs) bKs = -1
            if (toFile == bQs) bQs = -1
        }

        if (move.isCastling) {
            nextSquares[move.from] = null
            nextSquares[move.castleRookFrom] = null
            nextSquares[move.to] = moving
            nextSquares[move.castleRookTo] = Piece(moving.color, PieceType.ROOK)
        } else {
            nextSquares[move.from] = null
            if (move.isEnPassant) {
                val epCapRank = if (moving.color == PieceColor.WHITE) toRank - 1 else toRank + 1
                nextSquares[Square.of(toFile, epCapRank)] = null
            }
            nextSquares[move.to] = if (move.promotion != null) {
                Piece(moving.color, move.promotion)
            } else {
                moving
            }
        }

        val newEp = if (moving.type == PieceType.PAWN && abs(Square.rankOf(move.to) - Square.rankOf(move.from)) == 2) {
            val midRank = (Square.rankOf(move.to) + Square.rankOf(move.from)) / 2
            Square.of(Square.fileOf(move.from), midRank)
        } else {
            -1
        }

        val isResetHalfmove = moving.type == PieceType.PAWN || captured != null || move.isEnPassant
        val newHalfmove = if (isResetHalfmove) 0 else halfmoveClock + 1
        val newFullmove = if (sideToMove == PieceColor.BLACK) fullmoveNumber + 1 else fullmoveNumber

        return ChessBoard(
            squares = nextSquares,
            sideToMove = sideToMove.opposite(),
            whiteCastleKingsideRookFile = wKs,
            whiteCastleQueensideRookFile = wQs,
            blackCastleKingsideRookFile = bKs,
            blackCastleQueensideRookFile = bQs,
            enPassantSquare = newEp,
            halfmoveClock = newHalfmove,
            fullmoveNumber = newFullmove,
            variant = variant,
            chess960Position = chess960Position,
            repetitionCounts = repetitionCounts
        )
    }

    fun isInsufficientMaterial(): Boolean {
        val nonKingPieces = mutableListOf<Pair<Int, Piece>>()
        for (sq in 0..63) {
            val p = squares[sq] ?: continue
            if (p.type != PieceType.KING) nonKingPieces.add(sq to p)
        }
        if (nonKingPieces.isEmpty()) return true
        if (nonKingPieces.size == 1) {
            val type = nonKingPieces[0].second.type
            return type == PieceType.BISHOP || type == PieceType.KNIGHT
        }
        if (nonKingPieces.size == 2) {
            val (sq1, p1) = nonKingPieces[0]
            val (sq2, p2) = nonKingPieces[1]
            if (p1.type == PieceType.BISHOP && p2.type == PieceType.BISHOP && p1.color != p2.color) {
                val c1 = (Square.fileOf(sq1) + Square.rankOf(sq1)) and 1
                val c2 = (Square.fileOf(sq2) + Square.rankOf(sq2)) and 1
                return c1 == c2
            }
        }
        return false
    }

    fun isThreefoldRepetition(): Boolean =
        (repetitionCounts[positionKey()] ?: 0) >= 3

    fun evaluateTermination(legalMoves: List<Move> = generateLegalMoves()): Pair<GameResult, TerminationReason> {
        if (legalMoves.isEmpty()) {
            return if (isInCheck(sideToMove)) {
                val result = if (sideToMove == PieceColor.WHITE) GameResult.BLACK_WIN else GameResult.WHITE_WIN
                result to TerminationReason.CHECKMATE
            } else {
                GameResult.DRAW to TerminationReason.STALEMATE
            }
        }
        if (halfmoveClock >= 100) return GameResult.DRAW to TerminationReason.FIFTY_MOVE_RULE
        if (isThreefoldRepetition()) return GameResult.DRAW to TerminationReason.THREEFOLD_REPETITION
        if (isInsufficientMaterial()) return GameResult.DRAW to TerminationReason.INSUFFICIENT_MATERIAL
        return GameResult.IN_PROGRESS to TerminationReason.NONE
    }

    fun toSan(move: Move, legalMoves: List<Move> = generateLegalMoves()): String {
        val moving = pieceAt(move.from) ?: return move.toUci()
        val after = makeMoveUnchecked(move)
        val opponentLegal = after.generateLegalMoves()
        val inCheck = after.isInCheck(after.sideToMove)
        val suffix = when {
            inCheck && opponentLegal.isEmpty() -> "#"
            inCheck -> "+"
            else -> ""
        }

        if (move.isCastling) {
            return (if (move.castleKingside) "O-O" else "O-O-O") + suffix
        }

        val isCapture = pieceAt(move.to) != null || move.isEnPassant
        val destStr = Square.toAlgebraic(move.to)

        if (moving.type == PieceType.PAWN) {
            val prefix = if (isCapture) "${('a'.code + Square.fileOf(move.from)).toChar()}x" else ""
            val promo = if (move.promotion != null) "=${move.promotion.symbol}" else ""
            return "$prefix$destStr$promo$suffix"
        }

        val sameTypeConflicts = legalMoves.filter {
            it.to == move.to && it.from != move.from && pieceAt(it.from) == moving
        }
        val disambig = when {
            sameTypeConflicts.isEmpty() -> ""
            sameTypeConflicts.none { Square.fileOf(it.from) == Square.fileOf(move.from) } ->
                ('a'.code + Square.fileOf(move.from)).toChar().toString()
            sameTypeConflicts.none { Square.rankOf(it.from) == Square.rankOf(move.from) } ->
                ('1'.code + Square.rankOf(move.from)).toChar().toString()
            else -> Square.toAlgebraic(move.from)
        }
        val capStr = if (isCapture) "x" else ""
        return "${moving.type.symbol}$disambig$capStr$destStr$suffix"
    }

    fun parseSan(sanInput: String): Move? {
        val clean = sanInput.trim().trimEnd('+', '#', '!', '?')
        val legal = generateLegalMoves()
        if (clean == "O-O" || clean == "0-0") {
            return legal.firstOrNull { it.isCastling && it.castleKingside }
        }
        if (clean == "O-O-O" || clean == "0-0-0") {
            return legal.firstOrNull { it.isCastling && !it.castleKingside }
        }
        for (m in legal) {
            val candidate = toSan(m, legal).trimEnd('+', '#')
            if (candidate == clean) return m
        }
        // Fallback: UCI match (e.g., e2e4)
        for (m in legal) {
            if (m.toUci() == sanInput.trim().lowercase()) return m
        }
        return null
    }

    fun toFen(shredder: Boolean = variant == GameVariant.FISCHER960): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = squares[Square.of(file, rank)]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.toFenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(if (sideToMove == PieceColor.WHITE) " w " else " b ")

        val castleSb = StringBuilder()
        if (shredder) {
            if (whiteCastleKingsideRookFile in 0..7) castleSb.append(('A'.code + whiteCastleKingsideRookFile).toChar())
            if (whiteCastleQueensideRookFile in 0..7) castleSb.append(('A'.code + whiteCastleQueensideRookFile).toChar())
            if (blackCastleKingsideRookFile in 0..7) castleSb.append(('a'.code + blackCastleKingsideRookFile).toChar())
            if (blackCastleQueensideRookFile in 0..7) castleSb.append(('a'.code + blackCastleQueensideRookFile).toChar())
        } else {
            if (whiteCastleKingsideRookFile >= 0) castleSb.append('K')
            if (whiteCastleQueensideRookFile >= 0) castleSb.append('Q')
            if (blackCastleKingsideRookFile >= 0) castleSb.append('k')
            if (blackCastleQueensideRookFile >= 0) castleSb.append('q')
        }
        sb.append(if (castleSb.isEmpty()) "-" else castleSb.toString())
        sb.append(' ')
        sb.append(if (enPassantSquare in 0..63) Square.toAlgebraic(enPassantSquare) else "-")
        sb.append(" $halfmoveClock $fullmoveNumber")
        return sb.toString()
    }

    /**
     * Computes captured pieces from initial material & material imbalance in pawns.
     */
    fun materialStats(): MaterialInfo {
        val counts = mutableMapOf<Piece, Int>()
        for (sq in 0..63) {
            val p = squares[sq] ?: continue
            counts[p] = (counts[p] ?: 0) + 1
        }
        val whiteVal =
            (counts[Piece(PieceColor.WHITE, PieceType.PAWN)] ?: 0) * 1 +
            (counts[Piece(PieceColor.WHITE, PieceType.KNIGHT)] ?: 0) * 3 +
            (counts[Piece(PieceColor.WHITE, PieceType.BISHOP)] ?: 0) * 3 +
            (counts[Piece(PieceColor.WHITE, PieceType.ROOK)] ?: 0) * 5 +
            (counts[Piece(PieceColor.WHITE, PieceType.QUEEN)] ?: 0) * 9
        val blackVal =
            (counts[Piece(PieceColor.BLACK, PieceType.PAWN)] ?: 0) * 1 +
            (counts[Piece(PieceColor.BLACK, PieceType.KNIGHT)] ?: 0) * 3 +
            (counts[Piece(PieceColor.BLACK, PieceType.BISHOP)] ?: 0) * 3 +
            (counts[Piece(PieceColor.BLACK, PieceType.ROOK)] ?: 0) * 5 +
            (counts[Piece(PieceColor.BLACK, PieceType.QUEEN)] ?: 0) * 9

        val standardTotals = mapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val capturedByWhite = mutableListOf<PieceType>()
        val capturedByBlack = mutableListOf<PieceType>()
        for ((type, maxCount) in standardTotals) {
            val blackMissing = (maxCount - (counts[Piece(PieceColor.BLACK, type)] ?: 0)).coerceAtLeast(0)
            repeat(blackMissing) { capturedByWhite.add(type) }
            val whiteMissing = (maxCount - (counts[Piece(PieceColor.WHITE, type)] ?: 0)).coerceAtLeast(0)
            repeat(whiteMissing) { capturedByBlack.add(type) }
        }
        return MaterialInfo(
            whiteAdvantage = whiteVal - blackVal,
            capturedByWhite = capturedByWhite,
            capturedByBlack = capturedByBlack
        )
    }

    data class MaterialInfo(
        val whiteAdvantage: Int,
        val capturedByWhite: List<PieceType>,
        val capturedByBlack: List<PieceType>
    )

    companion object {
        const val STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        private val PROMOTION_TYPES = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
        private val KNIGHT_OFFSETS = arrayOf(
            intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
            intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
        )
        private val ORTHO_DIRS = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)
        )
        private val DIAG_DIRS = arrayOf(
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
        )

        fun standard(): ChessBoard = fromFen(STANDARD_FEN, GameVariant.STANDARD, Chess960.STANDARD_POSITION_NUMBER)

        fun fischer960(positionNumber: Int): ChessBoard =
            fromFen(Chess960.startingFen(positionNumber), GameVariant.FISCHER960, positionNumber)

        fun fromFen(
            fen: String,
            variant: GameVariant = GameVariant.STANDARD,
            chess960Position: Int = Chess960.STANDARD_POSITION_NUMBER
        ): ChessBoard {
            val parts = fen.trim().split(Regex("\\s+"))
            require(parts.size >= 4) { "Invalid FEN: $fen" }

            val squares = arrayOfNulls<Piece>(64)
            val ranks = parts[0].split('/')
            require(ranks.size == 8) { "Invalid FEN ranks: $fen" }

            for (fenRankIdx in 0..7) {
                val rank = 7 - fenRankIdx
                var file = 0
                for (ch in ranks[fenRankIdx]) {
                    if (ch.isDigit()) {
                        file += ch - '0'
                    } else {
                        val piece = Piece.fromFenChar(ch)
                        if (piece != null && file in 0..7) {
                            squares[Square.of(file, rank)] = piece
                        }
                        file++
                    }
                }
            }

            val side = if (parts[1].lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE

            // Find king files for castling resolution
            var whiteKingFile = 4
            var blackKingFile = 4
            for (f in 0..7) {
                if (squares[Square.of(f, 0)] == Piece(PieceColor.WHITE, PieceType.KING)) whiteKingFile = f
                if (squares[Square.of(f, 7)] == Piece(PieceColor.BLACK, PieceType.KING)) blackKingFile = f
            }

            var wKs = -1; var wQs = -1; var bKs = -1; var bQs = -1
            val castleStr = parts[2]
            if (castleStr != "-") {
                for (ch in castleStr) {
                    when (ch) {
                        'K' -> {
                            for (f in 7 downTo (whiteKingFile + 1)) {
                                if (squares[Square.of(f, 0)] == Piece(PieceColor.WHITE, PieceType.ROOK)) {
                                    wKs = f; break
                                }
                            }
                        }
                        'Q' -> {
                            for (f in 0 until whiteKingFile) {
                                if (squares[Square.of(f, 0)] == Piece(PieceColor.WHITE, PieceType.ROOK)) {
                                    wQs = f; break
                                }
                            }
                        }
                        'k' -> {
                            for (f in 7 downTo (blackKingFile + 1)) {
                                if (squares[Square.of(f, 7)] == Piece(PieceColor.BLACK, PieceType.ROOK)) {
                                    bKs = f; break
                                }
                            }
                        }
                        'q' -> {
                            for (f in 0 until blackKingFile) {
                                if (squares[Square.of(f, 7)] == Piece(PieceColor.BLACK, PieceType.ROOK)) {
                                    bQs = f; break
                                }
                            }
                        }
                        in 'A'..'H' -> {
                            val rf = ch - 'A'
                            if (rf > whiteKingFile) wKs = rf else if (rf < whiteKingFile) wQs = rf
                        }
                        in 'a'..'h' -> {
                            val rf = ch - 'a'
                            if (rf > blackKingFile) bKs = rf else if (rf < blackKingFile) bQs = rf
                        }
                    }
                }
            }

            val epSq = if (parts[3] == "-") -1 else Square.fromAlgebraic(parts[3])
            val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

            val initial = ChessBoard(
                squares = squares,
                sideToMove = side,
                whiteCastleKingsideRookFile = wKs,
                whiteCastleQueensideRookFile = wQs,
                blackCastleKingsideRookFile = bKs,
                blackCastleQueensideRookFile = bQs,
                enPassantSquare = epSq,
                halfmoveClock = halfmove,
                fullmoveNumber = fullmove,
                variant = variant,
                chess960Position = chess960Position,
                repetitionCounts = emptyMap()
            )
            val key = initial.positionKey()
            return ChessBoard(
                squares = squares,
                sideToMove = side,
                whiteCastleKingsideRookFile = wKs,
                whiteCastleQueensideRookFile = wQs,
                blackCastleKingsideRookFile = bKs,
                blackCastleQueensideRookFile = bQs,
                enPassantSquare = epSq,
                halfmoveClock = halfmove,
                fullmoveNumber = fullmove,
                variant = variant,
                chess960Position = chess960Position,
                repetitionCounts = mapOf(key to 1)
            )
        }

        fun buildPgn(
            event: String,
            whiteName: String,
            blackName: String,
            whiteElo: Int,
            blackElo: Int,
            result: GameResult,
            variant: GameVariant,
            startFen: String,
            sanMoves: List<String>,
            dateStr: String,
            opening: String
        ): String {
            val sb = StringBuilder()
            sb.appendLine("[Event \"$event\"]")
            sb.appendLine("[Site \"Grandmaster Arena\"]")
            sb.appendLine("[Date \"$dateStr\"]")
            sb.appendLine("[White \"$whiteName\"]")
            sb.appendLine("[Black \"$blackName\"]")
            sb.appendLine("[Result \"${result.pgnScore}\"]")
            sb.appendLine("[WhiteElo \"$whiteElo\"]")
            sb.appendLine("[BlackElo \"$blackElo\"]")
            if (variant == GameVariant.FISCHER960) {
                sb.appendLine("[Variant \"Chess960\"]")
                sb.appendLine("[FEN \"$startFen\"]")
                sb.appendLine("[SetUp \"1\"]")
            }
            if (opening.isNotBlank()) {
                sb.appendLine("[Opening \"$opening\"]")
            }
            sb.appendLine()
            val moveTokens = mutableListOf<String>()
            var idx = 0
            while (idx < sanMoves.size) {
                val moveNum = idx / 2 + 1
                val w = sanMoves[idx]
                val b = sanMoves.getOrNull(idx + 1)
                if (b != null) moveTokens.add("$moveNum. $w $b")
                else moveTokens.add("$moveNum. $w")
                idx += 2
            }
            moveTokens.add(result.pgnScore)
            sb.append(moveTokens.joinToString(" "))
            return sb.toString()
        }
    }
}
