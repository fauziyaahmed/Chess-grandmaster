package com.example.chess.engine

data class OpeningEntry(
    val eco: String,
    val name: String,
    val moves: List<String>
)

object OpeningBook {
    private val OPENINGS = listOf(
        OpeningEntry("B90", "Sicilian Defense: Najdorf Variation", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6")),
        OpeningEntry("B70", "Sicilian Defense: Dragon Variation", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "g6")),
        OpeningEntry("B33", "Sicilian Defense: Sveshnikov Variation", listOf("e4", "c5", "Nf3", "Nc6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "e5")),
        OpeningEntry("B23", "Sicilian Defense: Closed Sicilian", listOf("e4", "c5", "Nc3")),
        OpeningEntry("B22", "Sicilian Defense: Alapin Variation", listOf("e4", "c5", "c3")),
        OpeningEntry("B20", "Sicilian Defense", listOf("e4", "c5")),
        OpeningEntry("C65", "Ruy Lopez: Berlin Defense", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "Nf6")),
        OpeningEntry("C78", "Ruy Lopez: Morphy Defense", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O")),
        OpeningEntry("C60", "Ruy Lopez (Spanish Game)", listOf("e4", "e5", "Nf3", "Nc6", "Bb5")),
        OpeningEntry("C54", "Italian Game: Giuoco Piano", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3")),
        OpeningEntry("C51", "Italian Game: Evans Gambit", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "b4")),
        OpeningEntry("C55", "Italian Game: Two Knights Defense", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6")),
        OpeningEntry("C50", "Italian Game", listOf("e4", "e5", "Nf3", "Nc6", "Bc4")),
        OpeningEntry("C45", "Scotch Game", listOf("e4", "e5", "Nf3", "Nc6", "d4")),
        OpeningEntry("C42", "Petrov's Defense", listOf("e4", "e5", "Nf3", "Nf6")),
        OpeningEntry("C30", "King's Gambit", listOf("e4", "e5", "f4")),
        OpeningEntry("C25", "Vienna Game", listOf("e4", "e5", "Nc3")),
        OpeningEntry("C20", "King's Pawn Game", listOf("e4", "e5")),
        OpeningEntry("C11", "French Defense: Classical Variation", listOf("e4", "e6", "d4", "d5", "Nc3", "Nf6")),
        OpeningEntry("C02", "French Defense: Advance Variation", listOf("e4", "e6", "d4", "d5", "e5")),
        OpeningEntry("C00", "French Defense", listOf("e4", "e6")),
        OpeningEntry("B12", "Caro-Kann Defense: Advance Variation", listOf("e4", "c6", "d4", "d5", "e5")),
        OpeningEntry("B10", "Caro-Kann Defense", listOf("e4", "c6")),
        OpeningEntry("B01", "Scandinavian Defense", listOf("e4", "d5")),
        OpeningEntry("B07", "Pirc Defense", listOf("e4", "d6", "d4", "Nf6")),
        OpeningEntry("B02", "Alekhine's Defense", listOf("e4", "Nf6")),
        OpeningEntry("D37", "Queen's Gambit Declined", listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6")),
        OpeningEntry("D10", "Slav Defense", listOf("d4", "d5", "c4", "c6")),
        OpeningEntry("D20", "Queen's Gambit Accepted", listOf("d4", "d5", "c4", "dxc4")),
        OpeningEntry("D06", "Queen's Gambit", listOf("d4", "d5", "c4")),
        OpeningEntry("D02", "London System", listOf("d4", "d5", "Nf3", "Nf6", "Bf4")),
        OpeningEntry("D02", "London System", listOf("d4", "Nf6", "Bf4")),
        OpeningEntry("E20", "Nimzo-Indian Defense", listOf("d4", "Nf6", "c4", "e6", "Nc3", "Bb4")),
        OpeningEntry("E60", "King's Indian Defense", listOf("d4", "Nf6", "c4", "g6")),
        OpeningEntry("D85", "Grunfeld Defense", listOf("d4", "Nf6", "c4", "g6", "Nc3", "d5")),
        OpeningEntry("A57", "Benko Gambit", listOf("d4", "Nf6", "c4", "c5", "d5", "b5")),
        OpeningEntry("A80", "Dutch Defense", listOf("d4", "f5")),
        OpeningEntry("A40", "Queen's Pawn Opening", listOf("d4")),
        OpeningEntry("E00", "Catalan Opening", listOf("d4", "Nf6", "c4", "e6", "g3")),
        OpeningEntry("A20", "English Opening: Reversed Sicilian", listOf("c4", "e5")),
        OpeningEntry("A10", "English Opening", listOf("c4")),
        OpeningEntry("A06", "Reti Opening", listOf("Nf3", "d5", "c4")),
        OpeningEntry("A04", "Zukertort Opening", listOf("Nf3")),
        OpeningEntry("A00", "King's Fianchetto Opening", listOf("g3")),
        OpeningEntry("B00", "King's Pawn Opening", listOf("e4"))
    ).sortedByDescending { it.moves.size }

    fun identifyOpening(sanMoves: List<String>, variant: GameVariant, chess960Position: Int): Pair<String, String> {
        if (variant == GameVariant.FISCHER960 && chess960Position != Chess960.STANDARD_POSITION_NUMBER) {
            return "960" to "Fischer Random #$chess960Position"
        }
        if (sanMoves.isEmpty()) return "A00" to "Starting Position"
        val cleanMoves = sanMoves.map { it.trimEnd('+', '#') }
        for (entry in OPENINGS) {
            if (cleanMoves.size >= entry.moves.size && cleanMoves.subList(0, entry.moves.size) == entry.moves) {
                return entry.eco to entry.name
            }
        }
        return "A00" to "Irregular Opening"
    }

    fun isBookPly(sanMoves: List<String>, plyIndex: Int, variant: GameVariant): Boolean {
        if (variant == GameVariant.FISCHER960) return false
        if (plyIndex >= 10 || plyIndex >= sanMoves.size) return false
        val prefix = sanMoves.subList(0, plyIndex + 1).map { it.trimEnd('+', '#') }
        return OPENINGS.any { entry ->
            entry.moves.size >= prefix.size && entry.moves.subList(0, prefix.size) == prefix
        }
    }
}
