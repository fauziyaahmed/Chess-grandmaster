package com.example

import com.example.chess.engine.Chess960
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameResult
import com.example.chess.engine.GameVariant
import com.example.chess.engine.MoveClassification
import com.example.chess.engine.OpeningBook
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.chess.engine.PuzzleLibrary
import com.example.chess.engine.Square
import com.example.chess.engine.TerminationReason
import com.example.data.RatingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChessEngineUnitTest {

    @Test
    fun `chess960 generates all 960 unique valid starting positions`() {
        val seen = HashSet<String>()
        for (pos in 0 until Chess960.TOTAL_POSITIONS) {
            val rank = Chess960.backRankForPosition(pos)
            assertEquals(8, rank.length)
            assertTrue("Duplicate rank at $pos: $rank", seen.add(rank))

            // Verify piece counts
            assertEquals(1, rank.count { it == 'K' })
            assertEquals(1, rank.count { it == 'Q' })
            assertEquals(2, rank.count { it == 'R' })
            assertEquals(2, rank.count { it == 'B' })
            assertEquals(2, rank.count { it == 'N' })

            // Verify opposite-colored bishops
            val bishopFiles = rank.indices.filter { rank[it] == 'B' }
            assertEquals(1, (bishopFiles[0] + bishopFiles[1]) % 2)

            // Verify King between the two Rooks
            val kFile = rank.indexOf('K')
            val rFiles = rank.indices.filter { rank[it] == 'R' }
            assertTrue("King must sit between rooks at $pos ($rank)", kFile > rFiles[0] && kFile < rFiles[1])

            // Verify reverse lookup
            assertEquals(pos, Chess960.positionNumberFromBackRank(rank))
        }
        assertEquals(960, seen.size)
        assertEquals("RNBQKBNR", Chess960.backRankForPosition(Chess960.STANDARD_POSITION_NUMBER))
    }

    @Test
    fun `chess960 castling handles king on g1 and rook swap positions`() {
        // Construct a position where White King is on f1 and Kingside Rook is on g1
        // After O-O, King goes to g1 and Rook goes to f1 (they swap places!)
        val board = ChessBoard.fromFen(
            "4k3/8/8/8/8/8/8/5KR1 w G - 0 1",
            variant = GameVariant.FISCHER960,
            chess960Position = 0
        )
        val castleMove = board.generateLegalMoves().firstOrNull { it.isCastling && it.castleKingside }
        assertNotNull("Expected legal Chess960 O-O swap move", castleMove)
        assertEquals("O-O", board.toSan(castleMove!!))

        val after = board.makeMove(castleMove)
        assertEquals(PieceType.KING, after.pieceAt(Square.fromAlgebraic("g1"))?.type)
        assertEquals(PieceType.ROOK, after.pieceAt(Square.fromAlgebraic("f1"))?.type)
    }

    @Test
    fun `chess960 castling works when king is already on c1 queenside destination`() {
        // King on c1, Rook on b1: after O-O-O King ends on c1 and Rook on d1
        val board = ChessBoard.fromFen(
            "4k3/8/8/8/8/8/8/1RK5 w B - 0 1",
            variant = GameVariant.FISCHER960,
            chess960Position = 12
        )
        val castleMove = board.generateLegalMoves().firstOrNull { it.isCastling && !it.castleKingside }
        assertNotNull("Expected legal Chess960 O-O-O when king is on c1", castleMove)
        val after = board.makeMove(castleMove!!)
        assertEquals(PieceType.KING, after.pieceAt(Square.fromAlgebraic("c1"))?.type)
        assertEquals(PieceType.ROOK, after.pieceAt(Square.fromAlgebraic("d1"))?.type)
    }

    @Test
    fun `standard chess detects checkmate stalemate en passant and promotion`() {
        // Fool's mate
        var b = ChessBoard.standard()
        for (san in listOf("f3", "e5", "g4", "Qh4#")) {
            val m = b.parseSan(san)
            assertNotNull("Move $san should parse cleanly", m)
            b = b.makeMove(m!!)
        }
        val (res, reason) = b.evaluateTermination()
        assertEquals(GameResult.BLACK_WIN, res)
        assertEquals(TerminationReason.CHECKMATE, reason)

        // Stalemate: Black King h8, White Queen g6, White King f6
        val stale = ChessBoard.fromFen("7k/8/5KQ1/8/8/8/8/8 b - - 0 1")
        val (sRes, sReason) = stale.evaluateTermination()
        assertEquals(GameResult.DRAW, sRes)
        assertEquals(TerminationReason.STALEMATE, sReason)

        // En passant capture
        val epStart = ChessBoard.fromFen("4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1")
        val afterDouble = epStart.makeMove(epStart.parseSan("d5")!!)
        val epMove = afterDouble.generateLegalMoves().firstOrNull { it.isEnPassant }
        assertNotNull("En passant move should be generated", epMove)
        val afterEp = afterDouble.makeMove(epMove!!)
        assertEquals(PieceType.PAWN, afterEp.pieceAt(Square.fromAlgebraic("d6"))?.type)
        assertEquals(null, afterEp.pieceAt(Square.fromAlgebraic("d5")))

        // Promotion generates 4 piece choices
        val promoBoard = ChessBoard.fromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
        val promos = promoBoard.generateLegalMovesFrom(Square.fromAlgebraic("a7"))
            .filter { it.to == Square.fromAlgebraic("a8") }
        assertEquals(4, promos.size)
        assertEquals(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT),
            promos.mapNotNull { it.promotion }.toSet()
        )
    }

    @Test
    fun `insufficient material and fifty move rule terminate as draw`() {
        val kvsk = ChessBoard.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        assertEquals(GameResult.DRAW to TerminationReason.INSUFFICIENT_MATERIAL, kvsk.evaluateTermination())

        val kbsk = ChessBoard.fromFen("4k3/8/8/8/8/2B5/8/4K3 w - - 0 1")
        assertEquals(GameResult.DRAW to TerminationReason.INSUFFICIENT_MATERIAL, kbsk.evaluateTermination())

        val fiftyMove = ChessBoard.fromFen("4k3/8/8/8/8/8/4Q3/4K3 w - - 100 60")
        assertEquals(GameResult.DRAW to TerminationReason.FIFTY_MOVE_RULE, fiftyMove.evaluateTermination())
    }

    @Test
    fun `all puzzles in PuzzleLibrary have legal solutions ending in mate or clear tactical advantage`() {
        val engine = ChessEngine()
        val ids = HashSet<String>()
        for (puzzle in PuzzleLibrary.ALL_PUZZLES) {
            assertTrue("Duplicate puzzle id ${puzzle.id}", ids.add(puzzle.id))
            assertTrue("Puzzle ${puzzle.id} solution must not be empty", puzzle.solutionUci.isNotEmpty())

            var board = ChessBoard.fromFen(puzzle.fen)
            val solverColor = board.sideToMove
            assertFalse("Initial puzzle FEN must not already be over (${puzzle.id})",
                board.evaluateTermination().first != GameResult.IN_PROGRESS
            )

            for ((idx, uci) in puzzle.solutionUci.withIndex()) {
                val legal = board.generateLegalMoves()
                val move = legal.firstOrNull { it.toUci() == uci }
                assertNotNull(
                    "Puzzle ${puzzle.id} step $idx move '$uci' was NOT legal in FEN ${board.toFen()}",
                    move
                )
                board = board.makeMove(move!!)
            }

            val (term, _) = board.evaluateTermination()
            if (term == GameResult.IN_PROGRESS) {
                // Non-mate tactical puzzle (fork, pin, skewer, underpromotion): verify decisive advantage for solver
                val eval = engine.analyzePosition(board, maxDepth = 3, timeLimitMs = 300L)
                val solverCp = if (solverColor == PieceColor.WHITE) eval.centipawnsWhite else -eval.centipawnsWhite
                assertTrue(
                    "Puzzle ${puzzle.id} (${puzzle.title}) did not achieve winning advantage: solverCp=$solverCp",
                    solverCp >= 200
                )
            } else {
                val expectedWin = if (solverColor == PieceColor.WHITE) GameResult.WHITE_WIN else GameResult.BLACK_WIN
                assertEquals(
                    "Puzzle ${puzzle.id} (${puzzle.title}) expected $expectedWin at end of mate line",
                    expectedWin,
                    term
                )
            }
        }
    }

    @Test
    fun `daily puzzle rotates without immediate repeats`() {
        val d1 = PuzzleLibrary.puzzleForDate(LocalDate.of(2026, 11, 23))
        val d2 = PuzzleLibrary.puzzleForDate(LocalDate.of(2026, 11, 24))
        val d3 = PuzzleLibrary.puzzleForDate(LocalDate.of(2026, 11, 25))
        assertTrue(d1.id != d2.id && d2.id != d3.id)
        assertEquals(d1.id, PuzzleLibrary.puzzleForDate(LocalDate.of(2026, 11, 23)).id)
    }

    @Test
    fun `engine finds mate in one and classifies blunders accurately`() {
        val engine = ChessEngine()
        val mateBoard = ChessBoard.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1")
        val eval = engine.analyzePosition(mateBoard, maxDepth = 4, timeLimitMs = 500L)
        assertEquals("d1d8", eval.bestMove?.toUci())
        assertNotNull("Expected mate score", eval.mateIn)

        // Analyze game where White hangs Queen in 1 move (e4 e5 Qh5 Nc6 Qxf7+)
        val summary = engine.analyzeFullGame(
            sanMoves = listOf("e4", "e5", "Qh5", "Nc6", "Qxf7+"),
            startFen = ChessBoard.STANDARD_FEN,
            variant = GameVariant.STANDARD,
            chess960Pos = Chess960.STANDARD_POSITION_NUMBER
        )
        assertEquals(5, summary.analyzedMoves.size)
        val lastMove = summary.analyzedMoves.last()
        assertTrue(
            "Qxf7+ without support hangs the Queen and should be Mistake or Blunder, was ${lastMove.classification}",
            lastMove.classification == MoveClassification.BLUNDER || lastMove.classification == MoveClassification.MISTAKE
        )
    }

    @Test
    fun `opening book and rating calculator work accurately`() {
        val (eco, name) = OpeningBook.identifyOpening(
            listOf("e4", "c5", "Nf3", "d6", "d4"),
            GameVariant.STANDARD,
            Chess960.STANDARD_POSITION_NUMBER
        )
        assertTrue(eco.startsWith("B"))
        assertTrue(name.contains("Sicilian", ignoreCase = true))

        val rWin = RatingCalculator.calculateNewRating(1200, 1400, 1.0, gamesPlayed = 5)
        assertTrue("Beating higher rated opponent must raise rating significantly: $rWin", rWin > 1225)
        val rLoss = RatingCalculator.calculateNewRating(1200, 1200, 0.0, gamesPlayed = 50)
        assertEquals(1190, rLoss)

        val pgn = ChessBoard.buildPgn(
            event = "Test Event",
            whiteName = "Alice",
            blackName = "Bob",
            whiteElo = 1500,
            blackElo = 1480,
            result = GameResult.WHITE_WIN,
            variant = GameVariant.STANDARD,
            startFen = ChessBoard.STANDARD_FEN,
            sanMoves = listOf("e4", "e5", "Qh5", "Nc6", "Bc4", "Nf6", "Qxf7#"),
            dateStr = "2026.11.23",
            opening = "C20 Scholar's Mate"
        )
        assertTrue(pgn.contains("[White \"Alice\"]"))
        assertTrue(pgn.contains("1. e4 e5 2. Qh5 Nc6"))
        assertTrue(pgn.endsWith("1-0"))
    }

    @Test
    fun `custom time controls format parse and categorize accurately`() {
        val bullet = com.example.chess.engine.TimeControl.custom(minutes = 0, seconds = 45, incrementSeconds = 2)
        assertEquals("0:45+2", bullet.label)
        assertEquals(45, bullet.initialSeconds)
        assertEquals(2, bullet.incrementSeconds)
        assertEquals(com.example.chess.engine.TimeControlCategory.BULLET, bullet.category)

        val rapid = com.example.chess.engine.TimeControl.custom(minutes = 7, seconds = 30, incrementSeconds = 5)
        assertEquals("7:30+5", rapid.label)
        assertEquals(450, rapid.initialSeconds)
        assertEquals(com.example.chess.engine.TimeControlCategory.RAPID, rapid.category)

        val parsed = com.example.chess.engine.TimeControl.fromLabel("12+8")
        assertEquals(720, parsed.initialSeconds)
        assertEquals(8, parsed.incrementSeconds)
        assertEquals(com.example.chess.engine.TimeControlCategory.RAPID, parsed.category)
    }

    @Test
    fun `coach david repository grades blunders and suggests best move hints`() = kotlinx.coroutines.runBlocking {
        val coach = com.example.chess.coach.EngineCoachDavidRepository()
        // Position where White can mate on d8 (Rd1->d8#) or blunder by playing Qa4+? Let's test Qxf7+ hanging Queen
        var board = ChessBoard.standard()
        for (san in listOf("e4", "e5", "Qh5", "Nc6")) {
            board = board.makeMove(board.parseSan(san)!!)
        }
        val blunderMove = board.parseSan("Qxf7+")!!
        val afterBlunder = board.makeMove(blunderMove)

        val feedback = coach.evaluateMoveFeedback(
            boardBefore = board,
            boardAfter = afterBlunder,
            playedMove = blunderMove,
            playedSan = "Qxf7+",
            sanHistoryAfter = listOf("e4", "e5", "Qh5", "Nc6", "Qxf7+"),
            playerColor = PieceColor.WHITE
        )
        assertTrue(
            "Coach David should detect blunder/mistake on hanging queen: ${feedback.moveGrade}",
            feedback.moveGrade == MoveClassification.BLUNDER || feedback.moveGrade == MoveClassification.MISTAKE
        )
        assertTrue(feedback.message.contains("Coach David"))

        val mateBoard = ChessBoard.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1")
        val hint = coach.answerCoachQuery(
            queryType = com.example.chess.coach.CoachQueryType.HINT_BEST_MOVE,
            currentBoard = mateBoard,
            sanHistory = emptyList(),
            lastPlayedSan = null,
            lastEvalBefore = null,
            lastMoveGrade = null
        )
        assertEquals("Rd8#", hint.bestMoveSan)
        assertEquals(Square.fromAlgebraic("d1"), hint.bestMoveFrom)
        assertEquals(Square.fromAlgebraic("d8"), hint.bestMoveTo)
    }
}
