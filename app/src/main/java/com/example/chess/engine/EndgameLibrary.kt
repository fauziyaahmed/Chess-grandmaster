package com.example.chess.engine

enum class EndgameCategory(val label: String) {
    BASIC_MATES("Basic Mates"),
    MINOR_PIECES("Bishop & Knight"),
    PAWN_ENDGAMES("Pawn & Promotion"),
    MATING_NETS("Mating Patterns")
}

enum class EndgameDifficulty(val label: String) {
    FUNDAMENTAL("Fundamental"),
    INTERMEDIATE("Intermediate"),
    MASTER("Master")
}

data class EndgameDrill(
    val id: String,
    val title: String,
    val materialCode: String,
    val category: EndgameCategory,
    val difficulty: EndgameDifficulty,
    val fen: String,
    val parMoves: Int,
    val techniqueName: String,
    val strategyGuide: String,
    val stalemateWarning: String
)

object EndgameLibrary {

    val ALL_DRILLS: List<EndgameDrill> = listOf(
        EndgameDrill(
            id = "eg_kq_box",
            title = "Queen Checkmate (Box Method)",
            materialCode = "K + Q vs K",
            category = EndgameCategory.BASIC_MATES,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "4k3/8/8/8/3Q4/8/4K3/8 w - - 0 1",
            parMoves = 7,
            techniqueName = "Shrink the Queen's Box",
            strategyGuide = "Keep your Queen a knight's jump away from the enemy King to fence it toward the edge, then bring your King in to support the final mate.",
            stalemateWarning = "Never confine the lone King to 1 corner square without leaving it a legal move while your King walks closer!"
        ),
        EndgameDrill(
            id = "eg_kq_corner",
            title = "Queen Edge Finish",
            materialCode = "K + Q vs K",
            category = EndgameCategory.BASIC_MATES,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "3k4/8/3K4/8/2Q5/8/8/8 w - - 0 1",
            parMoves = 3,
            techniqueName = "Opposition + Kiss of Death",
            strategyGuide = "Black's King is trapped on the 8th rank. Align your King in opposition or deliver mate directly on a guarded square along the back rank.",
            stalemateWarning = "Watch out for moves like Qc7 or Qb7 if they remove the King's last escape square without giving check!"
        ),
        EndgameDrill(
            id = "eg_kr_box",
            title = "Rook Checkmate (Cutting Off)",
            materialCode = "K + R vs K",
            category = EndgameCategory.BASIC_MATES,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "4k3/8/8/8/8/8/4K3/R7 w - - 0 1",
            parMoves = 11,
            techniqueName = "Cut Off Rank & Win Opposition",
            strategyGuide = "Slice the board in half with your Rook first, walk your King up to face the enemy King directly (opposition), and only then check to push the King back a rank.",
            stalemateWarning = "In the corner, make sure the enemy King still has a side-step square before delivering the final Rook check."
        ),
        EndgameDrill(
            id = "eg_kr_edge",
            title = "Rook Mate (Opposition Finale)",
            materialCode = "K + R vs K",
            category = EndgameCategory.BASIC_MATES,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "4k3/8/4K3/8/R7/8/8/8 w - - 0 1",
            parMoves = 4,
            techniqueName = "Force the Losing King to Give Way",
            strategyGuide = "Your King already holds direct opposition on e6! Slide your Rook to a waiting square or check along the 8th rank when Black's King steps opposite yours.",
            stalemateWarning = "Do not rush your Rook onto the 7th rank without a spare escape square for Black's King."
        ),
        EndgameDrill(
            id = "eg_krr_ladder",
            title = "Two Rooks Roller (Ladder Mate)",
            materialCode = "K + 2R vs K",
            category = EndgameCategory.BASIC_MATES,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "3k4/8/8/8/8/1R6/R7/4K3 w - - 0 1",
            parMoves = 4,
            techniqueName = "Alternating Rook Staircase",
            strategyGuide = "Alternate checks rank by rank with both Rooks so one guards the cutoff rank while the other delivers check — pushing the King up to the 8th rank.",
            stalemateWarning = "Keep the two Rooks on separate ranks so the enemy King cannot capture an undefended Rook."
        ),
        EndgameDrill(
            id = "eg_kbb_corner",
            title = "Two Bishops Checkmate",
            materialCode = "K + 2B vs K",
            category = EndgameCategory.MINOR_PIECES,
            difficulty = EndgameDifficulty.INTERMEDIATE,
            fen = "3k4/8/3K4/8/2BB4/8/8/8 w - - 0 1",
            parMoves = 9,
            techniqueName = "Dual-Diagonal Corner Squeeze",
            strategyGuide = "Place both Bishops side-by-side to seal off two adjacent diagonals, driving Black's King into either corner with your King assisting.",
            stalemateWarning = "Be careful not to take away all corner flight squares before checking with the opposite-color Bishop."
        ),
        EndgameDrill(
            id = "eg_kbn_w_corner",
            title = "Bishop & Knight (Corner Finish)",
            materialCode = "K + B + N vs K",
            category = EndgameCategory.MINOR_PIECES,
            difficulty = EndgameDifficulty.MASTER,
            fen = "5k2/8/5K2/8/4BN2/8/8/8 w - - 0 1",
            parMoves = 8,
            techniqueName = "Drive to the Matching Bishop Corner (h8)",
            strategyGuide = "Your Bishop runs on light squares, so h8 is the mating corner! Coordinate King, Knight, and light-squared Bishop to seal f8 and force Black toward h8.",
            stalemateWarning = "The defending King will try to escape toward a8 (the dark-squared safe corner) — guard f7/e8 carefully."
        ),
        EndgameDrill(
            id = "eg_kp_opposition",
            title = "King & Pawn Opposition + Mate",
            materialCode = "K + P vs K",
            category = EndgameCategory.PAWN_ENDGAMES,
            difficulty = EndgameDifficulty.INTERMEDIATE,
            fen = "4k3/8/8/4P3/4K3/8/8/8 w - - 0 1",
            parMoves = 10,
            techniqueName = "Lead with the King, Promote & Mate",
            strategyGuide = "Do NOT push e6 immediately! Step forward with your King to seize the key squares in front of the pawn, outflank Black's King, queen the pawn, and finish the mate.",
            stalemateWarning = "When escorting a pawn to the 7th rank, check that your Queen promotion won't instantly stalemate the cornered King!"
        ),
        EndgameDrill(
            id = "eg_kp_outside",
            title = "Key Squares & Wing Breakthrough",
            materialCode = "K + 2P vs K + P",
            category = EndgameCategory.PAWN_ENDGAMES,
            difficulty = EndgameDifficulty.INTERMEDIATE,
            fen = "8/5pk1/8/6P1/4K2P/8/8/8 w - - 0 1",
            parMoves = 11,
            techniqueName = "Create a Protected Passed Pawn",
            strategyGuide = "Push your wing pawns and shoulder Black's King out with Kf5/Kg4 to crown a Queen and convert the endgame cleanly.",
            stalemateWarning = "After promoting, remember the Box Method to avoid accidental stalemate against a single remaining pawn or lone King."
        ),
        EndgameDrill(
            id = "eg_arabian_mate",
            title = "Arabian Mate Pattern",
            materialCode = "K + R + N vs K",
            category = EndgameCategory.MATING_NETS,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "7k/8/8/6K1/5N2/8/1R6/8 w - - 0 1",
            parMoves = 4,
            techniqueName = "Knight + Rook Edge Coordination",
            strategyGuide = "Place your Knight on f6 (or h6) to guard the corner escape while sealing the edge with your Rook for the classic Arabian Mate!",
            stalemateWarning = "Leave Black at least one corner oscillation square until your Knight or Rook delivers the final check."
        ),
        EndgameDrill(
            id = "eg_backrank_rook",
            title = "Back-Rank Mating Net",
            materialCode = "K + Q + R vs K + R + 3P",
            category = EndgameCategory.MATING_NETS,
            difficulty = EndgameDifficulty.FUNDAMENTAL,
            fen = "5rk1/5ppp/8/8/8/8/4QPPP/4R1K1 w - - 0 1",
            parMoves = 2,
            techniqueName = "Deflection & Back-Rank Strike",
            strategyGuide = "Black's King is entombed behind its own pawns on g8. Exploit the overloaded defender on f8 to crash through on e8!",
            stalemateWarning = "Force the exchange in the right move order so your Rook remains active on the 8th rank."
        ),
        EndgameDrill(
            id = "eg_queen_vs_rook",
            title = "Queen vs. Rook Conversion",
            materialCode = "K + Q vs K + R",
            category = EndgameCategory.MATING_NETS,
            difficulty = EndgameDifficulty.MASTER,
            fen = "6k1/8/6K1/4Q3/8/8/8/5r2 w - - 0 1",
            parMoves = 8,
            techniqueName = "Double-Attack & Skewer Checks",
            strategyGuide = "With Black's Rook separated from its King, use sharp Queen checks to fork King and Rook, or weave an immediate back-rank mating net!",
            stalemateWarning = "Watch for Rook stalemate tricks where Black sacrifices its Rook with check to force a draw!"
        )
    )

    fun drillsForCategory(category: EndgameCategory?): List<EndgameDrill> =
        if (category == null) ALL_DRILLS else ALL_DRILLS.filter { it.category == category }
}
