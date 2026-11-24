package com.example.chess.engine

import kotlin.random.Random

/**
 * Official FIDE / Scharnagl Chess960 (Fischer Random Chess) position generator.
 * Position 518 is the standard classical starting position (RNBQKBNR).
 */
object Chess960 {
    const val STANDARD_POSITION_NUMBER = 518
    const val TOTAL_POSITIONS = 960

    private val KNIGHT_TABLE = arrayOf(
        0 to 1, 0 to 2, 0 to 3, 0 to 4,
        1 to 2, 1 to 3, 1 to 4,
        2 to 3, 2 to 4,
        3 to 4
    )

    fun backRankForPosition(positionNumber: Int): String {
        require(positionNumber in 0 until TOTAL_POSITIONS) {
            "Chess960 position number must be in 0..959, got $positionNumber"
        }
        val rank = CharArray(8) { ' ' }

        var n = positionNumber
        val b1 = n % 4
        n /= 4
        rank[b1 * 2 + 1] = 'B' // Light square bishop (b, d, f, h)

        val b2 = n % 4
        n /= 4
        rank[b2 * 2] = 'B'     // Dark square bishop (a, c, e, g)

        val q = n % 6
        n /= 6
        val emptyForQueen = rank.indices.filter { rank[it] == ' ' }
        rank[emptyForQueen[q]] = 'Q'

        val (kn1, kn2) = KNIGHT_TABLE[n]
        val emptyForKnights = rank.indices.filter { rank[it] == ' ' }
        rank[emptyForKnights[kn1]] = 'N'
        rank[emptyForKnights[kn2]] = 'N'

        val remaining = rank.indices.filter { rank[it] == ' ' }
        rank[remaining[0]] = 'R'
        rank[remaining[1]] = 'K'
        rank[remaining[2]] = 'R'

        return String(rank)
    }

    /**
     * Produces X-FEN / Shredder-FEN for any Chess960 position 0..959.
     */
    fun startingFen(positionNumber: Int): String {
        val whiteRank = backRankForPosition(positionNumber)
        val blackRank = whiteRank.lowercase()
        val rookFiles = whiteRank.indices.filter { whiteRank[it] == 'R' }
        val qRookChar = ('A'.code + rookFiles[0]).toChar()
        val kRookChar = ('A'.code + rookFiles[1]).toChar()
        val castling = if (positionNumber == STANDARD_POSITION_NUMBER) {
            "KQkq"
        } else {
            "$kRookChar$qRookChar${kRookChar.lowercaseChar()}${qRookChar.lowercaseChar()}"
        }
        return "$blackRank/pppppppp/8/8/8/8/PPPPPPPP/$whiteRank w $castling - 0 1"
    }

    fun randomPositionNumber(random: Random = Random): Int = random.nextInt(TOTAL_POSITIONS)

    /**
     * Infers the Chess960 position number from an 8-char uppercase back-rank string, if valid.
     */
    fun positionNumberFromBackRank(backRank: String): Int? {
        val upper = backRank.uppercase()
        if (upper.length != 8) return null
        for (i in 0 until TOTAL_POSITIONS) {
            if (backRankForPosition(i) == upper) return i
        }
        return null
    }
}
