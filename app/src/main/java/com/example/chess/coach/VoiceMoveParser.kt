package com.example.chess.coach

import com.example.chess.engine.ChessBoard
import com.example.chess.engine.GameResult
import com.example.chess.engine.Move
import com.example.chess.engine.Piece
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.Square
import com.example.chess.engine.TerminationReason

data class BlindLegalMoveOption(
    val move: Move,
    val san: String,
    val spokenLabel: String,
    val destAlgebraic: String,
    val isCapture: Boolean,
    val isCheck: Boolean
)

data class BlindMovablePiece(
    val square: Int,
    val squareAlgebraic: String,
    val piece: Piece,
    val pieceName: String,
    val legalOptions: List<BlindLegalMoveOption>
)

object VoiceMoveParser {

    private val NUMBER_WORDS = mapOf(
        "one" to "1", "two" to "2", "to" to "to", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8"
    )

    private val FILE_WORDS = mapOf(
        "alpha" to "a", "bravo" to "b", "charlie" to "c", "delta" to "d",
        "echo" to "e", "foxtrot" to "f", "golf" to "g", "hotel" to "h",
        "aye" to "a", "bee" to "b", "see" to "c", "sea" to "c", "dee" to "d",
        "gee" to "g", "aitch" to "h"
    )

    fun parseSpokenMove(rawInput: String, board: ChessBoard): Move? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        // 1. Direct SAN or UCI match
        board.parseSan(trimmed)?.let { return it }

        val lower = trimmed.lowercase()
            .replace("-", " ")
            .replace(",", " ")
            .replace(".", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val legal = board.generateLegalMoves()

        // 2. Spoken castling
        if (lower.contains("queenside") || lower.contains("queen side") ||
            lower.contains("long castle") || lower == "o o o" || lower == "0 0 0"
        ) {
            return legal.firstOrNull { it.isCastling && !it.castleKingside }
        }
        if (lower.contains("kingside") || lower.contains("king side") ||
            lower.contains("short castle") || lower.contains("castle") ||
            lower == "o o" || lower == "0 0"
        ) {
            return legal.firstOrNull { it.isCastling && it.castleKingside }
                ?: legal.firstOrNull { it.isCastling }
        }

        // 3. Tokenize spoken chess grammar: "<Piece>? <fromSquare?> (takes|to)? <destSquare> <promoPiece?>"
        val tokens = lower.split(" ").filter { it.isNotBlank() }
        var pieceType: PieceType? = null
        var promoType: PieceType? = null
        var sawPromoteKeyword = false
        val normalizedTokens = mutableListOf<String>()

        for (tok in tokens) {
            when (tok) {
                "king" -> if (sawPromoteKeyword) promoType = PieceType.KING else pieceType = PieceType.KING
                "queen" -> if (sawPromoteKeyword) promoType = PieceType.QUEEN else if (pieceType == null) pieceType = PieceType.QUEEN else promoType = PieceType.QUEEN
                "rook", "castle" -> if (sawPromoteKeyword) promoType = PieceType.ROOK else if (pieceType == null) pieceType = PieceType.ROOK else promoType = PieceType.ROOK
                "bishop" -> if (sawPromoteKeyword) promoType = PieceType.BISHOP else if (pieceType == null) pieceType = PieceType.BISHOP else promoType = PieceType.BISHOP
                "knight", "night", "horse" -> if (sawPromoteKeyword) promoType = PieceType.KNIGHT else if (pieceType == null) pieceType = PieceType.KNIGHT else promoType = PieceType.KNIGHT
                "pawn" -> if (pieceType == null) pieceType = PieceType.PAWN
                "promote", "promotion", "equals" -> sawPromoteKeyword = true
                "to", "on", "at", "takes", "take", "captures", "capture", "x", "move", "play" -> {
                    // structural connector
                }
                else -> {
                    val mappedFile = FILE_WORDS[tok]
                    val mappedNum = if (tok != "to") NUMBER_WORDS[tok] else null
                    normalizedTokens.add(mappedFile ?: mappedNum ?: tok)
                }
            }
        }

        // Reassemble spaced square coordinates like ["e", "4"] -> "e4"
        val squaresFound = mutableListOf<Int>()
        var idx = 0
        while (idx < normalizedTokens.size) {
            val t = normalizedTokens[idx]
            if (t.length == 2 && t[0] in 'a'..'h' && t[1] in '1'..'8') {
                squaresFound.add(Square.fromAlgebraic(t))
                idx++
            } else if (t.length == 1 && t[0] in 'a'..'h' && idx + 1 < normalizedTokens.size) {
                val next = normalizedTokens[idx + 1]
                if (next.length == 1 && next[0] in '1'..'8') {
                    squaresFound.add(Square.fromAlgebraic("$t$next"))
                    idx += 2
                    continue
                }
                idx++
            } else {
                idx++
            }
        }

        // Also match compact disambiguated / SAN-like token if squaresFound was empty
        if (squaresFound.isEmpty()) {
            val joined = normalizedTokens.joinToString("")
            board.parseSan(joined)?.let { return it }
            return null
        }

        val matching = if (squaresFound.size >= 2) {
            val fromSq = squaresFound[0]
            val toSq = squaresFound[1]
            legal.filter { it.from == fromSq && it.to == toSq }
        } else {
            val destSq = squaresFound.last()
            legal.filter { m ->
                val moving = board.pieceAt(m.from) ?: return@filter false
                m.to == destSq && (pieceType == null || moving.type == pieceType)
            }
        }

        if (matching.isEmpty()) return null
        if (promoType != null) {
            matching.firstOrNull { it.promotion == promoType }?.let { return it }
        }
        return matching.firstOrNull { it.promotion == null || it.promotion == PieceType.QUEEN }
            ?: matching.first()
    }

    fun pieceTypeWord(type: PieceType): String = when (type) {
        PieceType.KING -> "King"
        PieceType.QUEEN -> "Queen"
        PieceType.ROOK -> "Rook"
        PieceType.BISHOP -> "Bishop"
        PieceType.KNIGHT -> "Knight"
        PieceType.PAWN -> "Pawn"
    }

    fun colorWord(color: PieceColor): String =
        if (color == PieceColor.WHITE) "White" else "Black"

    fun describeMoveForBlind(boardBefore: ChessBoard, move: Move, boardAfter: ChessBoard): String {
        val mover = boardBefore.pieceAt(move.from)
        val moverSide = mover?.color ?: boardBefore.sideToMove
        val sideLabel = colorWord(moverSide)
        val (res, reason) = boardAfter.evaluateTermination()
        val inCheck = boardAfter.isInCheck(boardAfter.sideToMove)

        val core = when {
            move.isCastling && move.castleKingside -> "$sideLabel castles kingside"
            move.isCastling -> "$sideLabel castles queenside"
            else -> {
                val pName = mover?.let { pieceTypeWord(it.type) } ?: "Piece"
                val fromAlg = Square.toAlgebraic(move.from)
                val toAlg = Square.toAlgebraic(move.to)
                val captured = boardBefore.pieceAt(move.to)
                val capText = when {
                    move.isEnPassant -> " takes pawn on $toAlg en passant"
                    captured != null -> " takes ${pieceTypeWord(captured.type)} on $toAlg"
                    else -> " to $toAlg"
                }
                val promoText = move.promotion?.let { " and promotes to ${pieceTypeWord(it)}" } ?: ""
                "$sideLabel plays $pName $fromAlg$capText$promoText"
            }
        }

        val suffix = when {
            reason == TerminationReason.CHECKMATE -> ". Checkmate! ${if (res == GameResult.WHITE_WIN) "White wins!" else "Black wins!"}"
            reason == TerminationReason.STALEMATE -> ". Stalemate, game drawn."
            res == GameResult.DRAW -> ". Draw by ${reason.label}."
            inCheck -> ". Check!"
            else -> "."
        }
        return core + suffix
    }

    fun describeSquareForBlind(board: ChessBoard, sq: Int, viewerSide: PieceColor?): String {
        val alg = Square.toAlgebraic(sq)
        val piece = board.pieceAt(sq)
        if (piece == null) {
            return "Square $alg, empty."
        }
        val owner = if (viewerSide != null && piece.color == viewerSide) "Your" else colorWord(piece.color)
        val base = "Square $alg, $owner ${pieceTypeWord(piece.type)}"
        return if (piece.color == board.sideToMove) {
            val legalCount = board.generateLegalMovesFrom(sq).size
            "$base, $legalCount legal ${if (legalCount == 1) "move" else "moves"}."
        } else {
            "$base."
        }
    }

    fun describeBoardForBlind(board: ChessBoard, viewerSide: PieceColor?): String {
        val whiteList = mutableListOf<String>()
        val blackList = mutableListOf<String>()
        val order = listOf(PieceType.KING, PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN)
        for (type in order) {
            for (sq in 0..63) {
                val p = board.pieceAt(sq) ?: continue
                if (p.type != type) continue
                val label = "${pieceTypeWord(p.type)} on ${Square.toAlgebraic(sq)}"
                if (p.color == PieceColor.WHITE) whiteList.add(label) else blackList.add(label)
            }
        }
        val turnStr = "${colorWord(board.sideToMove)} to move."
        val checkStr = if (board.isInCheck(board.sideToMove)) " ${colorWord(board.sideToMove)} is in check." else ""
        val whitePrefix = if (viewerSide == PieceColor.WHITE) "Your White pieces" else "White pieces"
        val blackPrefix = if (viewerSide == PieceColor.BLACK) "Your Black pieces" else "Black pieces"
        return "$turnStr$checkStr $whitePrefix: ${whiteList.joinToString(", ")}. $blackPrefix: ${blackList.joinToString(", ")}."
    }

    fun movablePiecesForBlind(board: ChessBoard): List<BlindMovablePiece> {
        val allLegal = board.generateLegalMoves()
        val byFrom = allLegal.groupBy { it.from }
        val sortRank = mapOf(
            PieceType.KING to 0, PieceType.QUEEN to 1, PieceType.ROOK to 2,
            PieceType.BISHOP to 3, PieceType.KNIGHT to 4, PieceType.PAWN to 5
        )
        return byFrom.entries
            .mapNotNull { (sq, moves) ->
                val p = board.pieceAt(sq) ?: return@mapNotNull null
                val options = moves.map { m ->
                    val san = board.toSan(m, allLegal)
                    val after = board.makeMove(m)
                    val isCap = board.pieceAt(m.to) != null || m.isEnPassant
                    val isChk = after.isInCheck(after.sideToMove)
                    val destAlg = Square.toAlgebraic(m.to)
                    val spoken = buildString {
                        if (m.isCastling) {
                            append(if (m.castleKingside) "Castle kingside" else "Castle queenside")
                        } else {
                            append(pieceTypeWord(p.type))
                            if (isCap) append(" takes on $destAlg") else append(" to $destAlg")
                            m.promotion?.let { append(" promote ${pieceTypeWord(it)}") }
                        }
                        if (isChk) append(", check")
                    }
                    BlindLegalMoveOption(
                        move = m,
                        san = san,
                        spokenLabel = spoken,
                        destAlgebraic = destAlg,
                        isCapture = isCap,
                        isCheck = isChk
                    )
                }
                BlindMovablePiece(
                    square = sq,
                    squareAlgebraic = Square.toAlgebraic(sq),
                    piece = p,
                    pieceName = pieceTypeWord(p.type),
                    legalOptions = options
                )
            }
            .sortedWith(compareBy<BlindMovablePiece> { sortRank[it.piece.type] ?: 9 }.thenBy { it.square })
    }
}
