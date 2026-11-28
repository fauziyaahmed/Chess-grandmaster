package com.example.chess.engine

import java.time.LocalDate

enum class PuzzleDifficulty(val label: String, val ratingRange: IntRange) {
    BEGINNER("Beginner", 800..1150),
    INTERMEDIATE("Intermediate", 1151..1550),
    ADVANCED("Advanced", 1551..1950),
    MASTER("Master", 1951..2400)
}

data class ChessPuzzle(
    val id: String,
    val title: String,
    val difficulty: PuzzleDifficulty,
    val rating: Int,
    val theme: String,
    val fen: String,
    /**
     * Sequence of UCI moves starting from [fen].
     * Even indices (0, 2, 4...) are player's winning moves; odd indices (1, 3...) are forced opponent replies.
     */
    val solutionUci: List<String>,
    val hintText: String,
    val explanation: String
)

object PuzzleLibrary {
    val ALL_PUZZLES: List<ChessPuzzle> = listOf(
        // --- BEGINNER (Mate in 1 & Elementary Tactics) ---
        ChessPuzzle(
            id = "puz_001",
            title = "Back Rank Thunder",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 850,
            theme = "Mate in 1",
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d1d8"),
            hintText = "Look at Black's undefended 8th rank.",
            explanation = "1. Rd8# delivers an immediate back-rank checkmate because the f7, g7, and h7 pawns block the Black king's escape."
        ),
        ChessPuzzle(
            id = "puz_002",
            title = "Queen's Final Kiss",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 900,
            theme = "Mate in 1",
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            solutionUci = listOf("h5f7"),
            hintText = "Which square near the Black king is attacked by both your queen and bishop?",
            explanation = "Scholar's Mate! 1. Qxf7# is checkmate as the f7 square is supported by the bishop on c4."
        ),
        ChessPuzzle(
            id = "puz_003",
            title = "Royal Knight Fork",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 960,
            theme = "Fork",
            fen = "1q6/4k3/8/8/3N4/8/1P6/4K3 w - - 0 1",
            solutionUci = listOf("d4c6", "e7d6", "c6b8"),
            hintText = "Find a square where your knight attacks both the Black king and queen at once.",
            explanation = "1. Nc6+! delivers a Royal Fork against the king on e7 and the queen on b8, winning the queen next move."
        ),
        ChessPuzzle(
            id = "puz_004",
            title = "Family Fork on c7",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 1020,
            theme = "Fork",
            fen = "r3k3/8/8/3N4/8/8/1P6/4K3 w - - 0 1",
            solutionUci = listOf("d5c7", "e8d7", "c7a8"),
            hintText = "Attack the king and rook simultaneously with your knight.",
            explanation = "1. Nc7+ forks the Black king on e8 and the rook on a8, winning the rook cleanly."
        ),
        ChessPuzzle(
            id = "puz_005",
            title = "Arabian Mate",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 1080,
            theme = "Mate in 1",
            fen = "7k/8/5N2/6R1/8/8/8/6K1 w - - 0 1",
            solutionUci = listOf("g5g8"),
            hintText = "Your knight on f6 covers h7 and supports the g8 square.",
            explanation = "1. Rg8# is the classic Arabian Mate: the knight on f6 takes away h7 and protects the rook on g8."
        ),
        ChessPuzzle(
            id = "puz_006",
            title = "Bishop Skewer",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 1120,
            theme = "Skewer",
            fen = "8/8/3k3B/8/1r6/8/1P6/6K1 w - - 0 1",
            solutionUci = listOf("h6f8", "d6e6", "f8b4"),
            hintText = "Use your bishop to give check and skewer the Black rook behind the king.",
            explanation = "1. Bf8+! skewers the Black king on d6 against the rook on b4, winning the rook cleanly."
        ),
        ChessPuzzle(
            id = "puz_007",
            title = "Absolute File Pin",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 1140,
            theme = "Pin",
            fen = "4k3/8/4q3/8/8/8/4R3/4K3 w - - 0 1",
            solutionUci = listOf("e2e6"),
            hintText = "The Black queen is pinned to the king—capture it!",
            explanation = "1. Rxe6+ wins the Black queen because it is pinned along the e-file."
        ),
        ChessPuzzle(
            id = "puz_021",
            title = "Deadly Battery on g7",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 880,
            theme = "Mate in 1",
            fen = "7k/6pp/8/8/8/2Q5/1B6/6K1 w - - 0 1",
            solutionUci = listOf("c3g7"),
            hintText = "Your queen and bishop form a battery aimed directly at g7.",
            explanation = "1. Qxg7# delivers checkmate supported by the bishop on b2."
        ),
        ChessPuzzle(
            id = "puz_025",
            title = "Dovetail Checkmate",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 940,
            theme = "Mate in 1",
            fen = "8/2prp3/2pk4/8/3P4/8/7Q/4K3 w - - 0 1",
            solutionUci = listOf("h2e5"),
            hintText = "Bring your queen directly next to the Black king on a square defended by your d4 pawn.",
            explanation = "1. Qe5# is checkmate: Black's own pieces block d7, c7, e7, and c6, while your queen and pawn cover every remaining square."
        ),
        ChessPuzzle(
            id = "puz_027",
            title = "Deadly Diagonal Pin",
            difficulty = PuzzleDifficulty.BEGINNER,
            rating = 1050,
            theme = "Pin",
            fen = "7k/6pp/5q2/B7/8/8/8/2R3K1 w - - 0 1",
            solutionUci = listOf("a5c3", "f6c3", "c1c3"),
            hintText = "Move your bishop onto the a1-h8 diagonal to pin the Black queen to her king.",
            explanation = "1. Bc3! pins the queen on f6 to the h8 king, winning the queen for a bishop."
        ),

        // --- INTERMEDIATE (Mate in 2, Discovered Attacks, Double Attacks) ---
        ChessPuzzle(
            id = "puz_008",
            title = "Smothered Checkmate",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1280,
            theme = "Mate in 2",
            fen = "5r1k/6pp/7N/8/2Q5/8/8/6K1 w - - 0 1",
            solutionUci = listOf("c4g8", "f8g8", "h6f7"),
            hintText = "Force the rook onto g8 so the Black king has zero escape squares.",
            explanation = "1. Qg8+! Rxg8 forces Black to block their own escape square, allowing 2. Nf7# smothered mate."
        ),
        ChessPuzzle(
            id = "puz_009",
            title = "Anastasia's Sacrifice",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1350,
            theme = "Mate in 2",
            fen = "7k/4N1pp/8/7Q/8/8/4R3/6K1 w - - 0 1",
            solutionUci = listOf("h5h7", "h8h7", "e2h2"),
            hintText = "Draw the king onto h7 with a queen sacrifice, then swing your rook to the h-file.",
            explanation = "1. Qxh7+! Kxh7 2. Rh2# checkmates because the knight on e7 guards g8 and g6."
        ),
        ChessPuzzle(
            id = "puz_010",
            title = "Battery Breach",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1380,
            theme = "Mate in 2",
            fen = "6k1/5ppp/8/8/8/1B6/5Q2/6K1 w - - 0 1",
            solutionUci = listOf("f2f7", "g8h8", "f7f8"),
            hintText = "Attack f7 with the queen supported by the b3 bishop.",
            explanation = "1. Qxf7+ Kh8 2. Qf8# (or Qg8#) finishes the game cleanly."
        ),
        ChessPuzzle(
            id = "puz_011",
            title = "Discovered Check Harvest",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1420,
            theme = "Discovered Attack",
            fen = "3k4/8/8/q7/3B4/8/3R4/4K3 w - - 0 1",
            solutionUci = listOf("d4b6", "d8c8", "b6a5"),
            hintText = "Move your bishop to attack the queen while unleashing a discovered check from your rook.",
            explanation = "1. Bb6+! reveals check from Rd2 while simultaneously attacking the Black queen on a5, winning it next move."
        ),
        ChessPuzzle(
            id = "puz_012",
            title = "Overloaded Back-Rank",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1460,
            theme = "Mate in 2",
            fen = "3r2k1/4qppp/8/8/8/8/3Q1PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d2d8", "e7d8", "d1d8"),
            hintText = "Black's queen is overloaded defending the d8 rook and the back rank.",
            explanation = "1. Qxd8+! forces 1... Qxd8 (or 1... Qf8 2. Qxf8#), allowing 2. Rxd8# back-rank checkmate."
        ),
        ChessPuzzle(
            id = "puz_013",
            title = "Boden's Crisscross Mate",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1520,
            theme = "Mate in 2",
            fen = "2kr4/pp1n4/2p5/8/5B2/2Q5/PP6/5BK1 w - - 0 1",
            solutionUci = listOf("c3c6", "b7c6", "f1a6"),
            hintText = "Sacrifice your queen to clear the c6 square for a deadly crisscross bishop mate.",
            explanation = "1. Qxc6+! bxc6 2. Ba6# is Boden's Mate, utilizing two bishops on intersecting diagonals."
        ),
        ChessPuzzle(
            id = "puz_022",
            title = "Knight Fork on c7",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1220,
            theme = "Fork",
            fen = "q3k3/8/8/3N4/8/8/1P6/4K3 w - - 0 1",
            solutionUci = listOf("d5c7", "e8d7", "c7a8"),
            hintText = "Fork the king and queen on an undefended square!",
            explanation = "1. Nc7+ forks the Black king on e8 and queen on a8, winning the queen next move."
        ),
        ChessPuzzle(
            id = "puz_023",
            title = "E-File Deflection",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1320,
            theme = "Mate in 2",
            fen = "2r3k1/5ppp/8/8/8/4Q3/5PPP/4R1K1 w - - 0 1",
            solutionUci = listOf("e3e8", "c8e8", "e1e8"),
            hintText = "Sacrifice your queen on e8 to deflect the Black rook from defending the back rank.",
            explanation = "1. Qe8+! Rxe8 2. Rxe8# delivers an unstoppable back-rank checkmate."
        ),
        ChessPuzzle(
            id = "puz_024",
            title = "Petrov's Queen Pin",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1480,
            theme = "Pin",
            fen = "rnbqkb1r/ppp2ppp/8/4N3/4n3/8/PPPP1PPP/RNBQKB1R w KQkq - 0 5",
            solutionUci = listOf("d1e2", "e4f6", "e5c6"),
            hintText = "Pin the Black knight on e4 to the Black king!",
            explanation = "1. Qe2! pins the e4 knight against the king on e8. After 1... Nf6, 2. Nc6+ discovers check and wins the Black queen!"
        ),
        ChessPuzzle(
            id = "puz_028",
            title = "H-File Queen Sacrifice",
            difficulty = PuzzleDifficulty.INTERMEDIATE,
            rating = 1390,
            theme = "Mate in 2",
            fen = "6rk/5ppp/8/5P1Q/8/5R2/8/6K1 w - - 0 1",
            solutionUci = listOf("h5h7", "h8h7", "f3h3"),
            hintText = "Blast open the h-file with a queen sacrifice—your f5 pawn seals the escape route!",
            explanation = "1. Qxh7+! Kxh7 2. Rh3# delivers checkmate as Black's own pieces block g8 and g7 while the f5 pawn covers g6."
        ),

        // --- ADVANCED (Mate in 3, Sacrifices, Deep Tactics) ---
        ChessPuzzle(
            id = "puz_014",
            title = "Légal's Trap Finale",
            difficulty = PuzzleDifficulty.ADVANCED,
            rating = 1620,
            theme = "Mate in 2",
            fen = "r2qkbnr/ppp2ppp/2np4/4N3/2B1P1b1/2N5/PPPP1PPP/R1BQK2R w KQkq - 0 5",
            solutionUci = listOf("c4f7", "e8e7", "c3d5"),
            hintText = "Ignore the pin on your queen—strike f7 with your bishop!",
            explanation = "1. Bxf7+! Ke7 2. Nd5# is the famous Légal's Mate with two knights and a bishop."
        ),
        ChessPuzzle(
            id = "puz_015",
            title = "Opera House Checkmate",
            difficulty = PuzzleDifficulty.ADVANCED,
            rating = 1680,
            theme = "Mate in 2",
            fen = "4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("b3b8", "d7b8", "d1d8"),
            hintText = "Sacrifice your queen on b8 to deflect the d7 knight from guarding d8.",
            explanation = "1. Qb8+! Nxb8 2. Rd8# is Paul Morphy's iconic Opera Game finale."
        ),
        ChessPuzzle(
            id = "puz_016",
            title = "Damiano's Rook Sacrifice",
            difficulty = PuzzleDifficulty.ADVANCED,
            rating = 1760,
            theme = "Mate in 3",
            fen = "5rk1/5pp1/6P1/8/6Q1/8/8/6KR w - - 0 1",
            solutionUci = listOf("h1h8", "g8h8", "g4h4", "h8g8", "h4h7"),
            hintText = "Decoy the Black king into the corner with a rook sacrifice on h8.",
            explanation = "1. Rh8+! Kxh8 forces the king to h8, allowing 2. Qh4+ Kg8 3. Qh7# supported by the g6 pawn."
        ),
        ChessPuzzle(
            id = "puz_017",
            title = "Windmill Queen Trap",
            difficulty = PuzzleDifficulty.ADVANCED,
            rating = 1850,
            theme = "Discovered Attack",
            fen = "5nk1/6pp/1q6/8/8/8/1B4R1/7K w - - 0 1",
            solutionUci = listOf("g2g7", "g8h8", "g7b7", "h8g8", "b7b6"),
            hintText = "Capture on g7 with check, then unleash a devastating discovered check along the long diagonal.",
            explanation = "1. Rxg7+ Kh8 2. Rb7+! delivers discovered check from the b2 bishop while attacking the Black queen on b6, winning it cleanly."
        ),
        ChessPuzzle(
            id = "puz_018",
            title = "H-File Queen Skewer",
            difficulty = PuzzleDifficulty.ADVANCED,
            rating = 1890,
            theme = "Skewer",
            fen = "8/5ppk/8/8/7q/2N5/PP3PK1/3R4 w - - 0 1",
            solutionUci = listOf("d1h1", "h4h1", "g2h1"),
            hintText = "Swing your rook to the h-file to skewer the Black queen against her king.",
            explanation = "1. Rh1! pins and skewers the queen on h4 to the h7 king, winning the queen for the rook."
        ),
        ChessPuzzle(
            id = "puz_026",
            title = "Clearance Sacrifice Mate in 3",
            difficulty = PuzzleDifficulty.ADVANCED,
            rating = 1790,
            theme = "Mate in 3",
            fen = "5rkb/5ppp/8/8/4R3/7Q/2B2PPP/6K1 w - - 0 1",
            solutionUci = listOf("h3h7", "g8h7", "e4h4", "h7g8", "c2h7"),
            hintText = "Sacrifice your queen on h7, then swing the rook to h4 to clear the c2-h7 diagonal for your bishop!",
            explanation = "1. Qxh7+! Kxh7 2. Rh4+ clears the c2-h7 diagonal while checking the king, forcing 2... Kg8 3. Bh7#!"
        ),

        // --- MASTER (2000+ Deep Combinations & Mates in 3+) ---
        ChessPuzzle(
            id = "puz_019",
            title = "Philidor's Legacy",
            difficulty = PuzzleDifficulty.MASTER,
            rating = 2120,
            theme = "Mate in 4",
            fen = "5r1k/6pp/8/6N1/2Q5/8/8/6K1 w - - 0 1",
            solutionUci = listOf("g5f7", "h8g8", "f7h6", "g8h8", "c4g8", "f8g8", "h6f7"),
            hintText = "Start with a knight check, follow with a double check, and finish with a queen sacrifice.",
            explanation = "1. Nf7+ Kg8 2. Nh6+ Kh8 3. Qg8+! Rxg8 4. Nf7# — Philidor's immortal 4-move Smothered Mate."
        ),
        ChessPuzzle(
            id = "puz_020",
            title = "Knight Underpromotion Rescue",
            difficulty = PuzzleDifficulty.MASTER,
            rating = 2280,
            theme = "Sacrifice",
            fen = "8/3q1P2/6k1/8/8/8/6PP/7K w - - 0 1",
            solutionUci = listOf("f7f8n", "g6f5", "f8d7"),
            hintText = "Promoting to a Queen allows 1... Qd1+ and 2. Qf1 Qxf1#! Find the only promotion that saves the game and wins.",
            explanation = "1. f8=N+! Underpromoting to a knight delivers check while forking the Black queen on d7—whereas promoting to a queen, rook, or bishop loses immediately to ...Qd1+!"
        )
    )

    fun puzzleForDate(date: LocalDate = LocalDate.now()): ChessPuzzle {
        val daySeed = (date.year * 372 + date.dayOfYear)
        val idx = ((daySeed % ALL_PUZZLES.size) + ALL_PUZZLES.size) % ALL_PUZZLES.size
        return ALL_PUZZLES[idx]
    }

    /**
     * Progressive tactical ladder for Daily Puzzle Streak mode:
     * starts with today's Daily Challenge (warm-up) then ramps cleanly by rating.
     */
    fun streakLadderForDate(date: LocalDate = LocalDate.now()): List<ChessPuzzle> {
        val daily = puzzleForDate(date)
        val dayOffset = (date.dayOfYear % 3)
        val remaining = ALL_PUZZLES
            .filter { it.id != daily.id }
            .sortedWith(compareBy<ChessPuzzle> { it.difficulty.ordinal }.thenBy { (it.rating + dayOffset * 17) })
        return listOf(daily) + remaining
    }
}
