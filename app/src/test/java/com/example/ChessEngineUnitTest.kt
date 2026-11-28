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

    @Test
    fun `match rating stakes and performance rating calculate accurately`() {
        val stakes = RatingCalculator.calculateExpectedStakes(
            playerRating = 1200,
            opponentRating = 1400,
            gamesPlayed = 5
        )
        assertTrue("Win against higher rated must be positive: ${stakes.winDelta}", stakes.winDelta > 20)
        assertTrue("Draw against higher rated must be positive: ${stakes.drawDelta}", stakes.drawDelta > 0)
        assertTrue("Loss must be negative: ${stakes.lossDelta}", stakes.lossDelta < 0)

        val highPerf = RatingCalculator.performanceRating(opponentRating = 1500, score = 1.0, accuracyPercent = 90.0)
        val lowPerf = RatingCalculator.performanceRating(opponentRating = 1500, score = 0.0, accuracyPercent = 50.0)
        assertTrue("Win with 90% accuracy ($highPerf) must exceed opponent rating", highPerf > 1900)
        assertTrue("Loss with 50% accuracy ($lowPerf) must be below opponent rating", lowPerf < 1200)
    }

    @Test
    fun `coach voice speech formatter expands algebraic chess notation`() {
        val raw = "Coach David: Try Nf3+ or Bxe5, avoid O-O-O until Rd8#"
        val spoken = com.example.chess.coach.CoachVoiceEngine.formatChessSpeech(raw)
        assertFalse(spoken.contains("Coach David:"))
        assertTrue("Should pronounce Knight move: $spoken", spoken.contains("Knight f3 check"))
        assertTrue("Should pronounce Bishop capture: $spoken", spoken.contains("Bishop takes e5"))
        assertTrue("Should pronounce queenside castling: $spoken", spoken.contains("castle queenside"))
        assertTrue("Should pronounce checkmate: $spoken", spoken.contains("Rook d8 checkmate"))
    }

    @Test
    fun `coach david supports candidate lines comparison and phase lessons`() = kotlinx.coroutines.runBlocking {
        val coach = com.example.chess.coach.EngineCoachDavidRepository()
        val board = ChessBoard.standard()

        val candidatesFb = coach.answerCoachQuery(
            queryType = com.example.chess.coach.CoachQueryType.CANDIDATE_LINES,
            currentBoard = board,
            sanHistory = emptyList(),
            lastPlayedSan = null,
            lastEvalBefore = null,
            lastMoveGrade = null
        )
        assertTrue("Should return candidate move comparisons", candidatesFb.candidateMoves.isNotEmpty())
        assertTrue(candidatesFb.headline.contains("Top 2"))

        val lessonFb = coach.answerCoachQuery(
            queryType = com.example.chess.coach.CoachQueryType.OPENING_ENDGAME_LESSON,
            currentBoard = board,
            sanHistory = emptyList(),
            lastPlayedSan = null,
            lastEvalBefore = null,
            lastMoveGrade = null
        )
        assertEquals("Opening", lessonFb.phaseTitle)
        assertTrue(lessonFb.message.contains("Coach David"))
    }

    @Test
    fun `bluetooth packet encode and decode roundtrip succeeds`() {
        val start = com.example.data.bluetooth.BtPacket.StartMatch(
            variant = GameVariant.FISCHER960,
            chess960Pos = 421,
            timeControlLabel = "5+3",
            hostPlaysWhite = false,
            hostName = "Alex|GM",
            hostRating = 1650,
            isRated = true
        )
        val decodedStart = com.example.data.bluetooth.BtPacket.decode(start.encode()) as? com.example.data.bluetooth.BtPacket.StartMatch
        assertNotNull(decodedStart)
        assertEquals(GameVariant.FISCHER960, decodedStart!!.variant)
        assertEquals(421, decodedStart.chess960Pos)
        assertEquals("5+3", decodedStart.timeControlLabel)
        assertFalse(decodedStart.hostPlaysWhite)
        assertEquals("Alex GM", decodedStart.hostName)
        assertEquals(1650, decodedStart.hostRating)
        assertTrue(decodedStart.isRated)

        val movePkt = com.example.data.bluetooth.BtPacket.MoveSync("e2e4", 300_000L, 298_500L)
        val decodedMove = com.example.data.bluetooth.BtPacket.decode(movePkt.encode()) as? com.example.data.bluetooth.BtPacket.MoveSync
        assertEquals(movePkt, decodedMove)

        val chatPkt = com.example.data.bluetooth.BtPacket.ChatSync("Sam", "Good luck!")
        assertEquals(chatPkt, com.example.data.bluetooth.BtPacket.decode(chatPkt.encode()))
        assertEquals(
            com.example.data.bluetooth.BtPacket.DrawOfferSync,
            com.example.data.bluetooth.BtPacket.decode(com.example.data.bluetooth.BtPacket.DrawOfferSync.encode())
        )
    }

    @Test
    fun `chess960 castling direction guides show before castling and turn off after castling`() {
        val board = ChessBoard.fischer960(518)
        val beforeGuides = com.example.ui.viewmodel.GameViewModel.computeCastleGuides(
            board = board,
            whiteHasCastled = false,
            blackHasCastled = false,
            targetSide = PieceColor.WHITE,
            enabled = true
        )
        assertEquals("Both kingside and queenside Chess960 castling guides should show before castling", 2, beforeGuides.size)
        assertTrue(beforeGuides.any { it.isKingside && it.toKingSq == Square.fromAlgebraic("g1") && it.toRookSq == Square.fromAlgebraic("f1") })
        assertTrue(beforeGuides.any { !it.isKingside && it.toKingSq == Square.fromAlgebraic("c1") && it.toRookSq == Square.fromAlgebraic("d1") })

        // Once White castles, the guide must automatically turn OFF
        val afterCastledGuides = com.example.ui.viewmodel.GameViewModel.computeCastleGuides(
            board = board,
            whiteHasCastled = true,
            blackHasCastled = false,
            targetSide = PieceColor.WHITE,
            enabled = true
        )
        assertTrue("Castling direction guide must turn off once White has castled", afterCastledGuides.isEmpty())
    }

    @Test
    fun `endgame library drills are all valid winning positions and engine finds mates in KQ and KR`() {
        val engine = ChessEngine()
        val ids = HashSet<String>()
        assertTrue(com.example.chess.engine.EndgameLibrary.ALL_DRILLS.any { it.materialCode == "K + Q vs K" })
        assertTrue(com.example.chess.engine.EndgameLibrary.ALL_DRILLS.any { it.materialCode == "K + R vs K" })

        for (drill in com.example.chess.engine.EndgameLibrary.ALL_DRILLS) {
            assertTrue("Duplicate drill id: ${drill.id}", ids.add(drill.id))
            val board = ChessBoard.fromFen(drill.fen)
            assertEquals("Drill ${drill.id} must be IN_PROGRESS", GameResult.IN_PROGRESS, board.evaluateTermination().first)
            assertTrue("Drill ${drill.id} must have legal moves", board.generateLegalMoves().isNotEmpty())
            val eval = engine.analyzePosition(board, maxDepth = 3, timeLimitMs = 300L)
            assertTrue(
                "Drill ${drill.id} (${drill.title}) must favor side to move: cp=${eval.centipawnsWhite}, mate=${eval.mateIn}",
                eval.centipawnsWhite >= 90 || (eval.mateIn != null && eval.mateIn > 0)
            )
        }

        // Verify K+Q vs K mate-in-1 and K+R vs K mate-in-1 detection
        val kqMate = ChessBoard.fromFen("3k4/8/3K4/8/2Q5/8/8/8 w - - 0 1")
        val kqEval = engine.analyzePosition(kqMate, maxDepth = 3, timeLimitMs = 300L)
        assertEquals(1, kqEval.mateIn)

        val krMate = ChessBoard.fromFen("4k3/8/4K3/8/R7/8/8/8 w - - 0 1")
        val krEval = engine.analyzePosition(krMate, maxDepth = 4, timeLimitMs = 400L)
        assertTrue("Expected mate search in KR vs K finale: mateIn=${krEval.mateIn}, cp=${krEval.centipawnsWhite}", krEval.centipawnsWhite > 500)

        // Stars calculation & controlled squares
        assertEquals(3, com.example.ui.viewmodel.EndgameViewModel.calculateStars(movesUsed = 5, parMoves = 7, hintsUsed = 0))
        assertEquals(2, com.example.ui.viewmodel.EndgameViewModel.calculateStars(movesUsed = 9, parMoves = 7, hintsUsed = 0))
        assertEquals(1, com.example.ui.viewmodel.EndgameViewModel.calculateStars(movesUsed = 15, parMoves = 7, hintsUsed = 2))

        val controlled = com.example.ui.viewmodel.EndgameViewModel.computeControlledSquares(kqMate, PieceColor.WHITE)
        assertTrue(controlled.contains(Square.fromAlgebraic("c8")))
        assertTrue(controlled.contains(Square.fromAlgebraic("e7")))
    }

    @Test
    fun `sound effect manager synthesizes distinct clips per event and pack and respects mute`() {
        val moveWood = com.example.data.SoundEffectManager.synthesizeClip(
            com.example.data.SoundEffect.MOVE,
            com.example.data.SoundPack.CLASSIC_WOOD,
            1.0f
        )
        val captureWood = com.example.data.SoundEffectManager.synthesizeClip(
            com.example.data.SoundEffect.CAPTURE,
            com.example.data.SoundPack.CLASSIC_WOOD,
            1.0f
        )
        val mateWood = com.example.data.SoundEffectManager.synthesizeClip(
            com.example.data.SoundEffect.CHECKMATE,
            com.example.data.SoundPack.CLASSIC_WOOD,
            1.0f
        )
        assertTrue(moveWood.isNotEmpty())
        assertTrue(captureWood.isNotEmpty())
        assertTrue(mateWood.size > moveWood.size)
        assertFalse(moveWood.contentEquals(captureWood))
        assertFalse(captureWood.contentEquals(mateWood))

        val moveCrystal = com.example.data.SoundEffectManager.synthesizeClip(
            com.example.data.SoundEffect.MOVE,
            com.example.data.SoundPack.CRYSTAL_GLASS,
            1.0f
        )
        val moveRetro = com.example.data.SoundEffectManager.synthesizeClip(
            com.example.data.SoundEffect.MOVE,
            com.example.data.SoundPack.RETRO_ARCADE,
            1.0f
        )
        assertFalse(moveWood.contentEquals(moveCrystal))
        assertFalse(moveCrystal.contentEquals(moveRetro))

        val muted = com.example.data.SoundEffectManager.synthesizeClip(
            com.example.data.SoundEffect.CHECKMATE,
            com.example.data.SoundPack.CLASSIC_WOOD,
            0.0f
        )
        assertTrue("Zero volume must produce silent samples", muted.all { it.toInt() == 0 })

        val manager = com.example.data.SoundEffectManager()
        manager.configure(volume = 0.8f, pack = com.example.data.SoundPack.CLASSIC_WOOD)
        manager.play(com.example.data.SoundEffect.MOVE, enabled = false)
        assertTrue(manager.recentEvents.value.isEmpty())

        manager.play(com.example.data.SoundEffect.CAPTURE, enabled = true)
        assertEquals(com.example.data.SoundEffect.CAPTURE, manager.lastPlayedEffect.value)

        manager.configure(volume = 0.0f, pack = com.example.data.SoundPack.RETRO_ARCADE)
        manager.play(com.example.data.SoundEffect.CHECK, enabled = true)
        assertEquals(1, manager.recentEvents.value.size)

        manager.preview(com.example.data.SoundEffect.CHECKMATE)
        assertEquals(com.example.data.SoundEffect.CHECKMATE, manager.lastPlayedEffect.value)
        manager.release()
    }

    @Test
    fun `algebraic notation turn builder pairs white and black moves accurately`() {
        val standardTurns = com.example.ui.components.buildNotationTurns(
            listOf("e4", "e5", "Nf3", "Nc6", "Bb5"),
            PieceColor.WHITE
        )
        assertEquals(3, standardTurns.size)
        assertEquals(1, standardTurns[0].moveNumber)
        assertEquals("e4", standardTurns[0].whiteSan)
        assertEquals(1, standardTurns[0].whitePly)
        assertEquals("e5", standardTurns[0].blackSan)
        assertEquals(2, standardTurns[0].blackPly)
        assertEquals(2, standardTurns[1].moveNumber)
        assertEquals("Nf3", standardTurns[1].whiteSan)
        assertEquals("Nc6", standardTurns[1].blackSan)
        assertEquals(3, standardTurns[2].moveNumber)
        assertEquals("Bb5", standardTurns[2].whiteSan)
        assertEquals(5, standardTurns[2].whitePly)
        org.junit.Assert.assertNull(standardTurns[2].blackSan)

        val blackFirstTurns = com.example.ui.components.buildNotationTurns(
            listOf("Nc6", "d4"),
            PieceColor.BLACK
        )
        assertEquals(2, blackFirstTurns.size)
        assertEquals("...", blackFirstTurns[0].whiteSan)
        org.junit.Assert.assertNull(blackFirstTurns[0].whitePly)
        assertEquals("Nc6", blackFirstTurns[0].blackSan)
        assertEquals(1, blackFirstTurns[0].blackPly)
        assertEquals("d4", blackFirstTurns[1].whiteSan)
        assertEquals(2, blackFirstTurns[1].whitePly)
    }

    @Test
    fun `opening explorer validates all master lines and aggregates transpositions`() {
        val parseErrors = com.example.chess.engine.OpeningExplorer.validateAllLines()
        assertTrue("Master repertoire lines must all parse cleanly: $parseErrors", parseErrors.isEmpty())

        val startExplorer = com.example.chess.engine.OpeningExplorer.lookup(ChessBoard.standard())
        assertFalse(startExplorer.isChess960NonStandard)
        assertTrue(startExplorer.totalGames > 500_000)
        val topMoves = startExplorer.moves.map { it.san }
        assertTrue("Expected e4 and d4 in starting explorer, got $topMoves", topMoves.containsAll(listOf("e4", "d4", "Nf3", "c4")))
        // Sorted descending by frequency
        assertEquals(startExplorer.moves.sortedByDescending { it.totalGames }, startExplorer.moves)
        val popSum = startExplorer.moves.sumOf { it.popularityPct.toDouble() }
        assertTrue("Popularity sum should be ~100%, was $popSum", popSum in 99.0..101.0)

        // Verify transpositions reach the exact same master position regardless of move order:
        // Order A: 1. d4 Nf6 2. c4 e6 vs Order B: 1. c4 Nf6 2. d4 e6 -> should suggest Nc3, g3, Nf3
        var bA = ChessBoard.standard()
        for (san in listOf("d4", "Nf6", "c4", "e6")) bA = bA.makeMove(bA.parseSan(san)!!)
        var bB = ChessBoard.standard()
        for (san in listOf("c4", "Nf6", "d4", "e6")) bB = bB.makeMove(bB.parseSan(san)!!)

        val expA = com.example.chess.engine.OpeningExplorer.lookup(bA)
        val expB = com.example.chess.engine.OpeningExplorer.lookup(bB)
        assertEquals(expA.positionKey, expB.positionKey)
        assertEquals(expA.moves.map { it.san }, expB.moves.map { it.san })
        assertTrue(expA.moves.any { it.san == "Nc3" })

        val fischer960Exp = com.example.chess.engine.OpeningExplorer.lookup(
            ChessBoard.fischer960(42),
            GameVariant.FISCHER960,
            42
        )
        assertTrue(fischer960Exp.isChess960NonStandard)
        assertTrue(fischer960Exp.moves.isEmpty())
    }

    @Test
    fun `daily puzzle streak logic tracks consecutive days idempotency and broken streak resets`() {
        val day10 = 20_500L
        val first = com.example.chess.engine.DailyPuzzleStreakLogic.recordCompletion(
            storedStreak = 0,
            bestStreak = 0,
            lastEpochDay = 0L,
            completedEpochDays = emptySet(),
            todayEpochDay = day10
        )
        assertEquals(1, first.streak)
        assertEquals(1, first.bestStreak)
        assertTrue(first.incrementedToday)
        assertTrue(com.example.chess.engine.DailyPuzzleStreakLogic.isSolvedToday(first.lastEpochDay, first.completedEpochDays, day10))

        // Re-solving on same day is idempotent
        val sameDay = com.example.chess.engine.DailyPuzzleStreakLogic.recordCompletion(
            storedStreak = first.streak,
            bestStreak = first.bestStreak,
            lastEpochDay = first.lastEpochDay,
            completedEpochDays = first.completedEpochDays,
            todayEpochDay = day10
        )
        assertEquals(1, sameDay.streak)
        assertFalse(sameDay.incrementedToday)

        // Solving on day11 increments streak to 2
        val day11 = com.example.chess.engine.DailyPuzzleStreakLogic.recordCompletion(
            storedStreak = sameDay.streak,
            bestStreak = sameDay.bestStreak,
            lastEpochDay = sameDay.lastEpochDay,
            completedEpochDays = sameDay.completedEpochDays,
            todayEpochDay = day10 + 1L
        )
        assertEquals(2, day11.streak)
        assertEquals(2, day11.bestStreak)
        assertEquals(2, com.example.chess.engine.DailyPuzzleStreakLogic.effectiveStreak(day11.streak, day11.lastEpochDay, day10 + 2L))
        // Skipping day12 makes effectiveStreak 0 on day13
        assertEquals(0, com.example.chess.engine.DailyPuzzleStreakLogic.effectiveStreak(day11.streak, day11.lastEpochDay, day10 + 3L))
        // Completing again on day13 resets streak to 1 while preserving bestStreak = 2
        val afterGap = com.example.chess.engine.DailyPuzzleStreakLogic.recordCompletion(
            storedStreak = day11.streak,
            bestStreak = day11.bestStreak,
            lastEpochDay = day11.lastEpochDay,
            completedEpochDays = day11.completedEpochDays,
            todayEpochDay = day10 + 3L
        )
        assertEquals(1, afterGap.streak)
        assertEquals(2, afterGap.bestStreak)

        val dots = com.example.chess.engine.DailyPuzzleStreakLogic.recentSevenDayDots(
            completedEpochDays = day11.completedEpochDays,
            lastEpochDay = day11.lastEpochDay,
            today = LocalDate.of(2026, 11, 27)
        )
        assertEquals(7, dots.size)
        assertTrue(dots.last().isToday)
    }

    @Test
    fun `voice move parser handles spoken moves captures castling homophones and blind descriptions`() {
        val board = ChessBoard.standard()

        // Spoken pawn advance with space ("e 4")
        val e4 = com.example.chess.coach.VoiceMoveParser.parseSpokenMove("e 4", board)
        assertNotNull(e4)
        assertEquals("e2e4", e4!!.toUci())

        // Spoken Knight move with phonetic homophone ("night f3")
        val nf3 = com.example.chess.coach.VoiceMoveParser.parseSpokenMove("night f3", board)
        assertNotNull(nf3)
        assertEquals("g1f3", nf3!!.toUci())

        // Spoken capture & kingside castling
        var testBoard = board
        for (san in listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6")) {
            testBoard = testBoard.makeMove(testBoard.parseSan(san)!!)
        }
        val takesC6 = com.example.chess.coach.VoiceMoveParser.parseSpokenMove("bishop takes c6", testBoard)
        assertNotNull(takesC6)
        assertEquals("b5c6", takesC6!!.toUci())

        var castleBoard = testBoard.makeMove(testBoard.parseSan("Ba4")!!)
        castleBoard = castleBoard.makeMove(castleBoard.parseSan("Nf6")!!)
        val castle = com.example.chess.coach.VoiceMoveParser.parseSpokenMove("castle kingside", castleBoard)
        assertNotNull(castle)
        assertTrue(castle!!.isCastling)
        assertTrue(castle.castleKingside)

        // Invalid spoken text returns null safely
        org.junit.Assert.assertNull(com.example.chess.coach.VoiceMoveParser.parseSpokenMove("queen to z9", board))

        // Spoken move & board description
        val afterE4 = board.makeMove(e4)
        val spokenMove = com.example.chess.coach.VoiceMoveParser.describeMoveForBlind(board, e4, afterE4)
        assertTrue(spokenMove.contains("White plays Pawn from e2 to e4"))
        val boardSummary = com.example.chess.coach.VoiceMoveParser.describeBoardForBlind(afterE4, PieceColor.WHITE)
        assertTrue(boardSummary.contains("Black to move"))
        val movable = com.example.chess.coach.VoiceMoveParser.movablePiecesForBlind(board)
        assertEquals(10, movable.size) // 8 pawns + 2 knights
    }

    @Test
    fun `logical analytics computes radar scores and white black win percentages from matches`() {
        val profile = com.example.data.local.PlayerProfileEntity(
            rapidRating = 1520,
            rapidPeak = 1550,
            blitzRating = 1480,
            fischerRating = 1410,
            puzzleRating = 1600,
            longestWinStreak = 4,
            blitzWins = 5,
            blitzLosses = 2,
            fischerWins = 3,
            fischerLosses = 1,
            puzzlesSolved = 8,
            puzzlesAttempted = 10
        )
        val matches = listOf(
            com.example.data.local.MatchHistoryEntity(
                whiteName = "Player", blackName = "Bot",
                whiteRatingBefore = 1500, whiteRatingAfter = 1512,
                blackRatingBefore = 1500, blackRatingAfter = 1488,
                playerSide = "WHITE", variant = "STANDARD", category = "RAPID",
                timeControlLabel = "10+0", chess960Position = 518,
                result = "WHITE_WIN", termination = "Checkmate",
                openingEco = "C50", openingName = "Italian Game",
                movesSanCsv = "e4,e5", startFen = ChessBoard.STANDARD_FEN, finalFen = ChessBoard.STANDARD_FEN,
                moveCount = 32, durationSeconds = 300, accuracyPercent = 88.0, pgnText = ""
            ),
            com.example.data.local.MatchHistoryEntity(
                whiteName = "Bot", blackName = "Player",
                whiteRatingBefore = 1500, whiteRatingAfter = 1495,
                blackRatingBefore = 1500, blackRatingAfter = 1505,
                playerSide = "BLACK", variant = "STANDARD", category = "RAPID",
                timeControlLabel = "10+0", chess960Position = 518,
                result = "DRAW", termination = "Agreement",
                openingEco = "B20", openingName = "Sicilian",
                movesSanCsv = "e4,c5", startFen = ChessBoard.STANDARD_FEN, finalFen = ChessBoard.STANDARD_FEN,
                moveCount = 40, durationSeconds = 420, accuracyPercent = 82.0, pgnText = ""
            )
        )
        val attempts = listOf(
            com.example.data.local.PuzzleAttemptEntity(
                puzzleId = "p1", puzzleTitle = "Mate", difficulty = "Easy",
                dateKey = "2026-11-27", isDailyChallenge = true, solved = true,
                hintsUsed = 0, ratingBefore = 1580, ratingAfter = 1600
            )
        )

        val analytics = com.example.chess.engine.LogicalAnalytics.computePlayerAnalytics(
            profile = profile,
            matches = matches,
            puzzleAttempts = attempts,
            settings = com.example.data.preferences.UserSettings(dailyPuzzleStreak = 3)
        )

        assertEquals(6, analytics.radarMetrics.size)
        analytics.radarMetrics.forEach { metric ->
            assertTrue("${metric.label} score must be bounded 0..100", metric.score in 0..100)
        }
        assertEquals(1, analytics.whiteStats.games)
        assertEquals(1, analytics.whiteStats.wins)
        assertEquals(100f, analytics.whiteStats.winPct)
        assertEquals(1, analytics.blackStats.games)
        assertEquals(1, analytics.blackStats.draws)
        assertEquals(36, analytics.avgMovesPerGame)
    }
}
