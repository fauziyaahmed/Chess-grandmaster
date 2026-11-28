package com.example.chess.engine

import kotlin.math.roundToInt

data class ExplorerMoveStat(
    val san: String,
    val uci: String,
    val totalGames: Int,
    val popularityPct: Float,
    val whiteWinPct: Float,
    val drawPct: Float,
    val blackWinPct: Float,
    val avgRating: Int,
    val resultingEco: String,
    val resultingOpeningName: String
)

data class OpeningExplorerPosition(
    val positionKey: String,
    val eco: String,
    val openingName: String,
    val totalGames: Int,
    val moves: List<ExplorerMoveStat>,
    val isChess960NonStandard: Boolean = false
)

object OpeningExplorer {

    private data class MasterLine(
        val eco: String,
        val name: String,
        val moves: List<String>,
        val games: Int,
        val whitePct: Int,
        val drawPct: Int,
        val blackPct: Int,
        val avgRating: Int
    )

    private class MutableEdgeAccumulator(
        val san: String,
        val uci: String,
        val targetKey: String
    ) {
        var totalGames: Long = 0L
        var whiteGames: Long = 0L
        var drawGames: Long = 0L
        var blackGames: Long = 0L
        var ratingWeightedSum: Long = 0L
        var resultingEco: String = "A00"
        var resultingName: String = "Opening Theory"
    }

    private class MutableNode {
        var eco: String = "A00"
        var name: String = "Master Opening Tree"
        var maxDepthWeight: Int = -1
        val edges = linkedMapOf<String, MutableEdgeAccumulator>()
    }

    private fun normalizedKey(board: ChessBoard): String {
        val parts = board.toFen(shredder = false).split(" ")
        return parts.take(4).joinToString(" ")
    }

    private val MASTER_LINES: List<MasterLine> = listOf(
        // --- 1. e4 OPENINGS ---
        // Sicilian Defense (B20-B99)
        MasterLine("B90", "Sicilian: Najdorf (6. Bg5)", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6", "Bg5", "e6", "f4"), 46200, 39, 32, 29, 2690),
        MasterLine("B90", "Sicilian: Najdorf English Attack", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6", "Be3", "e5", "Nb3", "Be6"), 41800, 38, 33, 29, 2685),
        MasterLine("B92", "Sicilian: Najdorf Opocensky (6. Be2)", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6", "Be2", "e5", "Nb3"), 28400, 36, 36, 28, 2655),
        MasterLine("B90", "Sicilian: Najdorf Adams Attack (6. h3)", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6", "h3", "e5"), 18900, 39, 33, 28, 2675),
        MasterLine("B80", "Sicilian: Scheveningen", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "e6", "Be2", "a6"), 21500, 37, 34, 29, 2630),
        MasterLine("B76", "Sicilian: Dragon Yugoslav Attack", listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "g6", "Be3", "Bg7", "f3", "O-O", "Qd2"), 24600, 41, 29, 30, 2645),
        MasterLine("B50", "Sicilian: Moscow Variation (3. Bb5+)", listOf("e4", "c5", "Nf3", "d6", "Bb5+", "Bd7", "Bxd7+", "Qxd7"), 19200, 35, 39, 26, 2650),
        MasterLine("B33", "Sicilian: Sveshnikov Variation", listOf("e4", "c5", "Nf3", "Nc6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "e5", "Ndb5", "d6", "Bg5"), 33400, 37, 35, 28, 2680),
        MasterLine("B30", "Sicilian: Rossolimo (3. Bb5)", listOf("e4", "c5", "Nf3", "Nc6", "Bb5", "g6", "O-O", "Bg7"), 29800, 37, 37, 26, 2670),
        MasterLine("B48", "Sicilian: Taimanov Variation", listOf("e4", "c5", "Nf3", "e6", "d4", "cxd4", "Nxd4", "Nc6", "Nc3", "Qc7", "Be3", "a6"), 27100, 37, 33, 30, 2640),
        MasterLine("B42", "Sicilian: Kan Variation", listOf("e4", "c5", "Nf3", "e6", "d4", "cxd4", "Nxd4", "a6", "Bd3"), 18400, 36, 35, 29, 2620),
        MasterLine("B23", "Sicilian: Closed Grand Prix", listOf("e4", "c5", "Nc3", "Nc6", "g3", "g6", "Bg2", "Bg7", "d3"), 19600, 35, 34, 31, 2595),
        MasterLine("B22", "Sicilian: Alapin Variation (2. c3)", listOf("e4", "c5", "c3", "Nf6", "e5", "Nd5", "d4", "cxd4"), 21200, 36, 36, 28, 2610),

        // Ruy Lopez / Spanish Game (C60-C99)
        MasterLine("C65", "Ruy Lopez: Berlin Defense", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "Nf6", "O-O", "Nxe4", "d4", "Nd6", "Bxc6", "dxc6"), 44500, 32, 46, 22, 2720),
        MasterLine("C67", "Ruy Lopez: Berlin Anti-Berlin (4. d3)", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "Nf6", "d3", "Bc5", "c3", "O-O"), 26400, 35, 40, 25, 2700),
        MasterLine("C88", "Ruy Lopez: Morphy Closed (5. O-O)", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7", "Re1", "b5", "Bb3", "d6", "c3", "O-O"), 48200, 37, 38, 25, 2685),
        MasterLine("C89", "Ruy Lopez: Marshall Attack", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7", "Re1", "b5", "Bb3", "O-O", "c3", "d5"), 23800, 31, 44, 25, 2705),
        MasterLine("C78", "Ruy Lopez: Arkhangelsk Defense", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "b5", "Bb3", "Bb7"), 15400, 37, 35, 28, 2660),
        MasterLine("C68", "Ruy Lopez: Exchange Variation", listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Bxc6", "dxc6", "O-O"), 16800, 35, 39, 26, 2630),

        // Italian Game, Scotch & Petrov (C40-C59)
        MasterLine("C54", "Italian Game: Giuoco Pianissimo", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3", "Nf6", "d3", "d6", "O-O", "a6"), 39600, 36, 39, 25, 2690),
        MasterLine("C50", "Italian Game: 4. d3 Modern", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "d3", "Nf6", "Nc3"), 18200, 35, 37, 28, 2650),
        MasterLine("C55", "Italian Game: Two Knights (4. d3)", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6", "d3", "Be7", "O-O", "O-O"), 22900, 36, 37, 27, 2665),
        MasterLine("C57", "Italian Game: Fried Liver / Ulvestad", listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Nf6", "Ng5", "d5", "exd5", "Na5"), 11200, 38, 32, 30, 2590),
        MasterLine("C45", "Scotch Game: Main Line", listOf("e4", "e5", "Nf3", "Nc6", "d4", "exd4", "Nxd4", "Nf6", "Nxc6", "bxc6"), 21700, 37, 36, 27, 2655),
        MasterLine("C42", "Petrov's Defense: Classical", listOf("e4", "e5", "Nf3", "Nf6", "Nxe5", "d6", "Nf3", "Nxe4", "d4", "d5", "Bd3"), 28600, 31, 47, 22, 2695),

        // French Defense (C00-C19)
        MasterLine("C18", "French: Winawer Variation", listOf("e4", "e6", "d4", "d5", "Nc3", "Bb4", "e5", "c5", "a3", "Bxc3+", "bxc3"), 25400, 38, 32, 30, 2645),
        MasterLine("C11", "French: Classical Steinitz", listOf("e4", "e6", "d4", "d5", "Nc3", "Nf6", "e5", "Nfd7", "f4", "c5", "Nf3", "Nc6"), 23800, 39, 33, 28, 2650),
        MasterLine("C07", "French: Tarrasch Open (3...c5)", listOf("e4", "e6", "d4", "d5", "Nd2", "c5", "exd5", "exd5", "Ngf3", "Nc6"), 19500, 36, 38, 26, 2635),
        MasterLine("C02", "French: Advance Variation", listOf("e4", "e6", "d4", "d5", "e5", "c5", "c3", "Nc6", "Nf3", "Qb6"), 20400, 37, 34, 29, 2615),

        // Caro-Kann Defense (B10-B19)
        MasterLine("B12", "Caro-Kann: Advance Short Variation", listOf("e4", "c6", "d4", "d5", "e5", "Bf5", "Nf3", "e6", "Be2", "c5"), 29400, 38, 35, 27, 2660),
        MasterLine("B18", "Caro-Kann: Classical Main Line", listOf("e4", "c6", "d4", "d5", "Nc3", "dxe4", "Nxe4", "Bf5", "Ng3", "Bg6", "h4", "h6"), 21600, 37, 37, 26, 2655),
        MasterLine("B13", "Caro-Kann: Panov Attack", listOf("e4", "c6", "d4", "d5", "exd5", "cxd5", "c4", "Nf6", "Nc3", "Nc6"), 16300, 36, 37, 27, 2625),

        // Other 1. e4 Responses
        MasterLine("B01", "Scandinavian Defense: 3...Qd6", listOf("e4", "d5", "exd5", "Qxd5", "Nc3", "Qd6", "d4", "Nf6"), 15400, 39, 34, 27, 2600),
        MasterLine("B07", "Pirc Defense: Austrian Attack", listOf("e4", "d6", "d4", "Nf6", "Nc3", "g6", "f4", "Bg7", "Nf3", "O-O"), 16200, 40, 30, 30, 2620),
        MasterLine("B04", "Alekhine's Defense: Modern", listOf("e4", "Nf6", "e5", "Nd5", "d4", "d6", "Nf3"), 10400, 39, 33, 28, 2585),

        // --- 1. d4 OPENINGS ---
        // Queen's Gambit Declined & Slav (D10-D69)
        MasterLine("D37", "QGD: Main Line (5. Bf4 / Bg5)", listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "Bg5", "Be7", "e3", "O-O", "Nf3"), 41200, 37, 40, 23, 2685),
        MasterLine("D38", "QGD: Ragozin Defense (4...Bb4)", listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "Nf3", "Bb4", "Bg5", "h6"), 29800, 35, 42, 23, 2705),
        MasterLine("D35", "QGD: Exchange Variation", listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "cxd5", "exd5", "Bg5", "c6", "e3"), 32400, 38, 38, 24, 2675),
        MasterLine("D45", "Semi-Slav Defense (5. e3)", listOf("d4", "d5", "c4", "c6", "Nf3", "Nf6", "Nc3", "e6", "e3", "Nbd7", "Bd3", "dxc4", "Bxc4"), 34100, 36, 39, 25, 2680),
        MasterLine("D15", "Slav Defense: Main Line", listOf("d4", "d5", "c4", "c6", "Nf3", "Nf6", "Nc3", "dxc4", "a4", "Bf5", "e3", "e6"), 27600, 36, 40, 24, 2665),
        MasterLine("D27", "Queen's Gambit Accepted: Classical", listOf("d4", "d5", "c4", "dxc4", "Nf3", "Nf6", "e3", "e6", "Bxc4", "c5"), 21400, 35, 41, 24, 2655),

        // London System (D02 / A45)
        MasterLine("D02", "London System: 1...d5", listOf("d4", "d5", "Bf4", "Nf6", "e3", "c5", "c3", "Nc6", "Nd2"), 24800, 36, 37, 27, 2645),
        MasterLine("A45", "London System: 1...Nf6", listOf("d4", "Nf6", "Bf4", "d5", "e3", "c5", "c3", "Nc6", "Nd2"), 21500, 36, 37, 27, 2650),

        // Nimzo-Indian, Queen's Indian & Catalan (E00-E59)
        MasterLine("E32", "Nimzo-Indian: Classical (4. Qc2)", listOf("d4", "Nf6", "c4", "e6", "Nc3", "Bb4", "Qc2", "O-O", "a3", "Bxc3+", "Qxc3"), 31200, 36, 40, 24, 2685),
        MasterLine("E54", "Nimzo-Indian: Rubinstein (4. e3)", listOf("d4", "Nf6", "c4", "e6", "Nc3", "Bb4", "e3", "O-O", "Bd3", "d5", "Nf3", "c5"), 36500, 35, 41, 24, 2690),
        MasterLine("E20", "Nimzo-Indian: f3 / Sämisch", listOf("d4", "Nf6", "c4", "e6", "Nc3", "Bb4", "f3", "d5", "a3", "Bxc3+", "bxc3"), 18400, 38, 35, 27, 2670),
        MasterLine("E15", "Queen's Indian Defense", listOf("d4", "Nf6", "c4", "e6", "Nf3", "b6", "g3", "Ba6", "b3", "Bb4+"), 24900, 34, 43, 23, 2675),
        MasterLine("E04", "Catalan Opening: Open Catalan", listOf("d4", "Nf6", "c4", "e6", "g3", "d5", "Bg2", "dxc4", "Nf3", "Be7", "O-O", "O-O"), 38700, 38, 40, 22, 2710),
        MasterLine("E06", "Catalan Opening: Closed Catalan", listOf("d4", "Nf6", "c4", "e6", "g3", "d5", "Bg2", "Be7", "Nf3", "O-O", "O-O"), 31800, 36, 42, 22, 2700),

        // King's Indian, Grünfeld & Benoni (E60-E99, D80-D99, A60-A89)
        MasterLine("E97", "King's Indian: Mar del Plata", listOf("d4", "Nf6", "c4", "g6", "Nc3", "Bg7", "e4", "d6", "Nf3", "O-O", "Be2", "e5", "O-O", "Nc6"), 33600, 39, 31, 30, 2665),
        MasterLine("E81", "King's Indian: Sämisch (5. f3)", listOf("d4", "Nf6", "c4", "g6", "Nc3", "Bg7", "e4", "d6", "f3", "O-O", "Be3"), 17900, 38, 33, 29, 2645),
        MasterLine("D85", "Grünfeld Defense: Exchange Variation", listOf("d4", "Nf6", "c4", "g6", "Nc3", "d5", "cxd5", "Nxd5", "e4", "Nxc3", "bxc3", "Bg7"), 35400, 36, 39, 25, 2705),
        MasterLine("D97", "Grünfeld Defense: Russian System", listOf("d4", "Nf6", "c4", "g6", "Nc3", "d5", "Nf3", "Bg7", "Qb3", "dxc4", "Qxc4"), 21800, 35, 41, 24, 2690),
        MasterLine("A70", "Modern Benoni: Classical", listOf("d4", "Nf6", "c4", "c5", "d5", "e6", "Nc3", "exd5", "cxd5", "d6", "e4", "g6"), 16500, 41, 31, 28, 2625),
        MasterLine("A87", "Dutch Defense: Leningrad", listOf("d4", "f5", "g3", "Nf6", "Bg2", "g6", "Nf3", "Bg7", "O-O", "O-O"), 14200, 40, 32, 28, 2605),

        // --- 1. c4 & 1. Nf3 & 1. g3 FLANK OPENINGS ---
        // Note: Several transpositions naturally cross-link with 1. d4 lines above!
        MasterLine("A28", "English: Four Knights System", listOf("c4", "e5", "Nc3", "Nf6", "Nf3", "Nc6", "e3", "Bb4"), 29500, 36, 40, 24, 2680),
        MasterLine("A21", "English: Reversed Sicilian (1...e5)", listOf("c4", "e5", "Nc3", "Nc6", "g3", "g6", "Bg2", "Bg7", "d3", "d6"), 26400, 37, 36, 27, 2665),
        MasterLine("A34", "English: Symmetrical Variation", listOf("c4", "c5", "Nc3", "Nc6", "g3", "g6", "Bg2", "Bg7", "Nf3", "Nf6"), 28900, 35, 41, 24, 2675),
        MasterLine("A16", "English: Anglo-Indian (transposes to d4/c4)", listOf("c4", "Nf6", "Nc3", "e6", "d4", "Bb4", "Qc2"), 19200, 36, 40, 24, 2680),
        MasterLine("A15", "English: Anglo-Grünfeld", listOf("c4", "Nf6", "Nc3", "g6", "d4", "d5"), 15800, 36, 39, 25, 2695),
        MasterLine("A06", "Réti Opening: 1. Nf3 d5 2. c4", listOf("Nf3", "d5", "c4", "d4", "b4"), 21400, 36, 38, 26, 2670),
        MasterLine("A09", "Réti Opening: Advance (2...dxc4)", listOf("Nf3", "d5", "c4", "e6", "g3", "Nf6", "Bg2", "Be7", "O-O", "O-O", "d4"), 27600, 36, 42, 22, 2695),
        MasterLine("A05", "King's Indian Attack / Réti 1...Nf6", listOf("Nf3", "Nf6", "c4", "e6", "g3", "d5", "Bg2", "Be7"), 23100, 36, 41, 23, 2685),
        MasterLine("A00", "King's Fianchetto (1. g3)", listOf("g3", "d5", "Bg2", "Nf6", "Nf3", "c5", "O-O", "Nc6"), 14600, 35, 38, 27, 2635)
    )

    private val POSITION_GRAPH: Map<String, MutableNode> by lazy { buildGraph() }

    private fun buildGraph(): Map<String, MutableNode> {
        val nodes = linkedMapOf<String, MutableNode>()
        val startBoard = ChessBoard.standard()
        val rootKey = normalizedKey(startBoard)
        nodes[rootKey] = MutableNode().apply {
            eco = "A00"
            name = "Starting Position"
            maxDepthWeight = 0
        }

        for (line in MASTER_LINES) {
            var board = startBoard
            var key = rootKey
            val depth = line.moves.size
            for ((plyIdx, rawSan) in line.moves.withIndex()) {
                val move = board.parseSan(rawSan) ?: break
                val canonicalSan = board.toSan(move).trimEnd('+', '#')
                val nextBoard = board.makeMove(move)
                val nextKey = normalizedKey(nextBoard)

                val parentNode = nodes.getOrPut(key) { MutableNode() }
                val nextNode = nodes.getOrPut(nextKey) { MutableNode() }

                // Prefer the deepest/most specific name when naming nodes reached later in the line
                if (plyIdx >= 0 && nextNode.maxDepthWeight <= depth) {
                    nextNode.eco = line.eco
                    nextNode.name = line.name
                    nextNode.maxDepthWeight = depth
                }

                val edge = parentNode.edges.getOrPut(canonicalSan) {
                    MutableEdgeAccumulator(
                        san = canonicalSan,
                        uci = move.toUci(),
                        targetKey = nextKey
                    )
                }
                val g = line.games.toLong()
                val wGames = (g * line.whitePct) / 100L
                val dGames = (g * line.drawPct) / 100L
                val bGames = (g - wGames - dGames).coerceAtLeast(0L)
                edge.totalGames += g
                edge.whiteGames += wGames
                edge.drawGames += dGames
                edge.blackGames += bGames
                edge.ratingWeightedSum += g * line.avgRating.toLong()
                edge.resultingEco = nextNode.eco
                edge.resultingName = nextNode.name

                board = nextBoard
                key = nextKey
            }
        }
        return nodes
    }

    fun lookup(
        board: ChessBoard,
        variant: GameVariant = GameVariant.STANDARD,
        chess960Pos: Int = Chess960.STANDARD_POSITION_NUMBER
    ): OpeningExplorerPosition {
        val key = normalizedKey(board)
        if (variant == GameVariant.FISCHER960 && chess960Pos != Chess960.STANDARD_POSITION_NUMBER) {
            return OpeningExplorerPosition(
                positionKey = key,
                eco = "960",
                openingName = "Fischer Random #$chess960Pos",
                totalGames = 0,
                moves = emptyList(),
                isChess960NonStandard = true
            )
        }

        val node = POSITION_GRAPH[key]
        if (node == null || node.edges.isEmpty()) {
            return OpeningExplorerPosition(
                positionKey = key,
                eco = node?.eco ?: "A00",
                openingName = node?.name ?: "Out of Master Book",
                totalGames = 0,
                moves = emptyList(),
                isChess960NonStandard = false
            )
        }

        val legalMoves = board.generateLegalMoves()
        val legalSanMap = legalMoves.associateBy { board.toSan(it, legalMoves).trimEnd('+', '#') }

        val validEdges = node.edges.values.filter { legalSanMap.containsKey(it.san) }
        val totalPositionGames = validEdges.sumOf { it.totalGames }.coerceAtLeast(1L)

        val stats = validEdges
            .sortedByDescending { it.totalGames }
            .map { edge ->
                val g = edge.totalGames.coerceAtLeast(1L).toDouble()
                val wPct = ((edge.whiteGames / g) * 100.0).toFloat()
                val dPct = ((edge.drawGames / g) * 100.0).toFloat()
                val bPct = (100f - wPct - dPct).coerceAtLeast(0f)
                val targetNode = POSITION_GRAPH[edge.targetKey]
                ExplorerMoveStat(
                    san = edge.san,
                    uci = edge.uci,
                    totalGames = edge.totalGames.toInt(),
                    popularityPct = ((edge.totalGames.toDouble() / totalPositionGames.toDouble()) * 100.0).toFloat(),
                    whiteWinPct = wPct,
                    drawPct = dPct,
                    blackWinPct = bPct,
                    avgRating = (edge.ratingWeightedSum / edge.totalGames.coerceAtLeast(1L)).toInt(),
                    resultingEco = targetNode?.eco ?: edge.resultingEco,
                    resultingOpeningName = targetNode?.name ?: edge.resultingName
                )
            }

        return OpeningExplorerPosition(
            positionKey = key,
            eco = node.eco,
            openingName = node.name,
            totalGames = validEdges.sumOf { it.totalGames }.toInt(),
            moves = stats,
            isChess960NonStandard = false
        )
    }

    /** Validation helper used by unit tests to guarantee every scripted line parses legally. */
    internal fun validateAllLines(): List<String> {
        val failures = mutableListOf<String>()
        for (line in MASTER_LINES) {
            var board = ChessBoard.standard()
            for ((idx, san) in line.moves.withIndex()) {
                val m = board.parseSan(san)
                if (m == null) {
                    failures.add("${line.eco} ${line.name}: illegal move #$idx '$san'")
                    break
                }
                board = board.makeMove(m)
            }
        }
        return failures
    }

    fun formatGameCount(count: Int): String = when {
        count >= 1_000_000 -> "${((count / 100_000.0).roundToInt() / 10.0)}M"
        count >= 1_000 -> "${(count / 1_000.0).roundToInt()}k"
        else -> count.toString()
    }
}
