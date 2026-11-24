package com.example.chess.engine

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: Char, val pieceValueCp: Int) {
    PAWN('P', 100),
    KNIGHT('N', 320),
    BISHOP('B', 330),
    ROOK('R', 500),
    QUEEN('Q', 900),
    KING('K', 20000);

    companion object {
        fun fromChar(c: Char): PieceType? = when (c.uppercaseChar()) {
            'P' -> PAWN
            'N' -> KNIGHT
            'B' -> BISHOP
            'R' -> ROOK
            'Q' -> QUEEN
            'K' -> KING
            else -> null
        }
    }
}

data class Piece(val color: PieceColor, val type: PieceType) {
    fun toFenChar(): Char = if (color == PieceColor.WHITE) type.symbol else type.symbol.lowercaseChar()

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val type = PieceType.fromChar(c) ?: return null
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return Piece(color, type)
        }
    }
}

/**
 * Square index 0..63 where 0 = a1, 7 = h1, 56 = a8, 63 = h8.
 */
object Square {
    fun of(file: Int, rank: Int): Int = rank * 8 + file
    fun fileOf(sq: Int): Int = sq and 7
    fun rankOf(sq: Int): Int = sq shr 3

    fun toAlgebraic(sq: Int): String {
        if (sq !in 0..63) return "-"
        val f = ('a'.code + fileOf(sq)).toChar()
        val r = ('1'.code + rankOf(sq)).toChar()
        return "$f$r"
    }

    fun fromAlgebraic(str: String): Int {
        if (str.length != 2) return -1
        val f = str[0].lowercaseChar() - 'a'
        val r = str[1] - '1'
        return if (f in 0..7 && r in 0..7) of(f, r) else -1
    }
}

data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val castleKingside: Boolean = false,
    val castleRookFrom: Int = -1,
    val castleRookTo: Int = -1,
    val isEnPassant: Boolean = false
) {
    fun toUci(): String {
        val promo = promotion?.symbol?.lowercaseChar()?.toString() ?: ""
        return "${Square.toAlgebraic(from)}${Square.toAlgebraic(to)}$promo"
    }
}

enum class GameVariant(val displayName: String) {
    STANDARD("Standard Chess"),
    FISCHER960("Fischer Chess 960")
}

enum class TimeControlCategory(val displayName: String) {
    BULLET("Bullet"),
    BLITZ("Blitz"),
    RAPID("Rapid"),
    CLASSICAL("Classical")
}

data class TimeControl(
    val label: String,
    val initialSeconds: Int,
    val incrementSeconds: Int,
    val category: TimeControlCategory
) {
    companion object {
        val PRESETS = listOf(
            TimeControl("1+0", 60, 0, TimeControlCategory.BULLET),
            TimeControl("2+1", 120, 1, TimeControlCategory.BULLET),
            TimeControl("3+0", 180, 0, TimeControlCategory.BLITZ),
            TimeControl("3+2", 180, 2, TimeControlCategory.BLITZ),
            TimeControl("5+0", 300, 0, TimeControlCategory.BLITZ),
            TimeControl("10+0", 600, 0, TimeControlCategory.RAPID),
            TimeControl("15+10", 900, 10, TimeControlCategory.RAPID),
            TimeControl("30+0", 1800, 0, TimeControlCategory.CLASSICAL)
        )

        fun categorize(initialSeconds: Int, incrementSeconds: Int): TimeControlCategory {
            val estTotal = initialSeconds + 40 * incrementSeconds
            return when {
                estTotal < 180 -> TimeControlCategory.BULLET
                estTotal < 600 -> TimeControlCategory.BLITZ
                estTotal < 1500 -> TimeControlCategory.RAPID
                else -> TimeControlCategory.CLASSICAL
            }
        }

        fun formatLabel(initialSeconds: Int, incrementSeconds: Int): String {
            val safeInit = initialSeconds.coerceAtLeast(15)
            val safeInc = incrementSeconds.coerceAtLeast(0)
            val mins = safeInit / 60
            val secs = safeInit % 60
            val baseStr = when {
                mins > 0 && secs == 0 -> "$mins"
                mins == 0 -> "0:$secs"
                else -> "$mins:${secs.toString().padStart(2, '0')}"
            }
            return "$baseStr+$safeInc"
        }

        fun custom(minutes: Int, seconds: Int = 0, incrementSeconds: Int = 0): TimeControl {
            val totalSeconds = (minutes.coerceIn(0, 180) * 60 + seconds.coerceIn(0, 59)).coerceAtLeast(15)
            val inc = incrementSeconds.coerceIn(0, 60)
            return TimeControl(
                label = formatLabel(totalSeconds, inc),
                initialSeconds = totalSeconds,
                incrementSeconds = inc,
                category = categorize(totalSeconds, inc)
            )
        }

        fun fromLabel(label: String): TimeControl {
            PRESETS.firstOrNull { it.label == label }?.let { return it }
            val parts = label.split("+")
            if (parts.size == 2) {
                val inc = parts[1].toIntOrNull() ?: 0
                val baseParts = parts[0].split(":")
                val initSec = if (baseParts.size == 2) {
                    (baseParts[0].toIntOrNull() ?: 0) * 60 + (baseParts[1].toIntOrNull() ?: 0)
                } else {
                    (parts[0].toIntOrNull() ?: 10) * 60
                }
                return TimeControl(
                    label = formatLabel(initSec, inc),
                    initialSeconds = initSec.coerceAtLeast(15),
                    incrementSeconds = inc.coerceIn(0, 60),
                    category = categorize(initSec.coerceAtLeast(15), inc.coerceIn(0, 60))
                )
            }
            return PRESETS[5]
        }
    }
}

enum class GameResult(val pgnScore: String, val displayLabel: String) {
    IN_PROGRESS("*", "In Progress"),
    WHITE_WIN("1-0", "White Wins"),
    BLACK_WIN("0-1", "Black Wins"),
    DRAW("1/2-1/2", "Draw")
}

enum class TerminationReason(val label: String) {
    NONE(""),
    CHECKMATE("Checkmate"),
    RESIGNATION("Resignation"),
    TIMEOUT("Time Expired"),
    STALEMATE("Stalemate"),
    INSUFFICIENT_MATERIAL("Insufficient Material"),
    THREEFOLD_REPETITION("Threefold Repetition"),
    FIFTY_MOVE_RULE("50-Move Rule"),
    AGREED_DRAW("Draw by Agreement")
}

enum class MoveClassification(val label: String, val symbol: String) {
    BOOK("Book", "B"),
    BRILLIANT("Brilliant", "!!"),
    BEST("Best", "*"),
    EXCELLENT("Excellent", "!"),
    GOOD("Good", ""),
    INACCURACY("Inaccuracy", "?!"),
    MISTAKE("Mistake", "?"),
    BLUNDER("Blunder", "??")
}
