package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.chess.engine.Chess960
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.GameResult
import com.example.chess.engine.GameVariant
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PuzzleDifficulty
import com.example.chess.engine.TimeControlCategory
import com.example.data.local.ChessDatabase
import com.example.data.local.PlayerProfileEntity
import com.example.data.repository.ChessRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.GrandmasterTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: ChessDatabase
    private lateinit var repository: ChessRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ChessDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ChessRepository(db.chessDao())
    }

    @After
    fun tearDown() {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        db.close()
    }

    @Test
    fun `app name matches Grandmaster branding`() {
        assertEquals("Grandmaster", context.getString(R.string.app_name))
    }

    @Test
    fun `repository seeds data and persists matches across formats`() = runBlocking {
        repository.ensureSeeded()

        val initialProfile = repository.profileFlow.first()!!
        assertEquals(1200, initialProfile.rapidRating)
        assertEquals(1200, initialProfile.fischerRating)

        // Record a Rapid win
        val rapidId = repository.recordCompletedMatch(
            whiteName = initialProfile.username,
            blackName = "Grandmaster AI Lv.4",
            opponentRating = 1400,
            playerSide = PieceColor.WHITE,
            variant = GameVariant.STANDARD,
            category = TimeControlCategory.RAPID,
            timeControlLabel = "10+0",
            chess960Position = 518,
            result = GameResult.WHITE_WIN,
            termination = "Checkmate",
            openingEco = "C20",
            openingName = "King's Pawn Game",
            movesSan = listOf("e4", "e5", "Qh5", "Nc6", "Bc4", "Nf6", "Qxf7#"),
            startFen = ChessBoard.STANDARD_FEN,
            finalFen = ChessBoard.STANDARD_FEN,
            durationSeconds = 95,
            accuracyPercent = 91.5,
            pgnText = "[Event \"Test\"]\n\n1. e4 e5 1-0"
        )
        assertTrue(rapidId > 0)

        // Record a Fischer Chess960 win
        val pos = 42
        val fischerId = repository.recordCompletedMatch(
            whiteName = initialProfile.username,
            blackName = "Fischer Bot",
            opponentRating = 1350,
            playerSide = PieceColor.WHITE,
            variant = GameVariant.FISCHER960,
            category = TimeControlCategory.BLITZ,
            timeControlLabel = "5+3",
            chess960Position = pos,
            result = GameResult.WHITE_WIN,
            termination = "Resignation",
            openingEco = "960",
            openingName = "Fischer960 #$pos",
            movesSan = listOf("e4", "e5"),
            startFen = Chess960.startingFen(pos),
            finalFen = Chess960.startingFen(pos),
            durationSeconds = 120,
            accuracyPercent = 84.0,
            pgnText = "[Variant \"Chess960\"]\n\n1. e4 e5 1-0"
        )
        assertTrue(fischerId > rapidId)

        val updated = repository.profileFlow.first()!!
        assertTrue("Rapid rating should increase after win", updated.rapidRating > 1200)
        assertTrue("Fischer rating should increase after Chess960 win", updated.fischerRating > 1200)
        assertEquals(2, updated.currentWinStreak)
        assertEquals(1, updated.rapidWins)
        assertEquals(1, updated.fischerWins)

        val allMatches = repository.allMatchesFlow.first()
        assertEquals(2, allMatches.size)
        val fischerOnly = repository.fischerMatchesFlow.first()
        assertEquals(1, fischerOnly.size)
        assertEquals(42, fischerOnly[0].chess960Position)
        assertNotNull(repository.getMatchById(rapidId))
    }

    @Test
    fun `puzzle results and swiss tournament progression work end to end`() = runBlocking {
        repository.ensureSeeded()

        val delta = repository.recordPuzzleResult(
            puzzleId = "puz_001",
            puzzleTitle = "Back Rank Thunder",
            difficulty = PuzzleDifficulty.BEGINNER,
            puzzleRating = 850,
            isDaily = true,
            solved = true,
            hintsUsed = 0
        )
        assertTrue(delta > 0)

        val p = repository.profileFlow.first()!!
        assertEquals(1, p.puzzlesSolved)
        assertEquals(1, p.puzzleStreak)
        assertTrue(p.puzzleRating > 1200)

        val tournaments = repository.tournamentsFlow.first()
        assertTrue(tournaments.isNotEmpty())
        val firstT = tournaments.first()
        repository.startOrAdvanceTournamentRound(firstT.id)
        val matches = repository.observeTournamentMatches(firstT.id).first()
        assertTrue("Tournament pairings must be generated for round 1", matches.isNotEmpty())
    }

    @Test
    fun `home screen renders primary interactive entry points`() {
        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                HomeScreen(
                    profile = PlayerProfileEntity(),
                    recentMatches = emptyList(),
                    announcements = emptyList(),
                    dailyRewardBannerXp = null,
                    onClaimDailyReward = {},
                    onDismissRewardBanner = {},
                    onStartGame = {},
                    onOpenFischerHub = {},
                    onOpenDailyPuzzle = {},
                    onOpenMatchAnalysis = {},
                    onOpenCompete = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_quick_play_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("custom_time_chip"))
        composeRule.onNodeWithTag("custom_time_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("coach_david_toggle"))
        composeRule.onNodeWithTag("coach_david_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_open_theme_studio_btn"))
        composeRule.onNodeWithTag("home_open_theme_studio_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("home_compare_themes_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("mode_bluetooth_friend"))
        composeRule.onNodeWithTag("mode_bluetooth_friend").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("mode_endgame_trainer"))
        composeRule.onNodeWithTag("mode_endgame_trainer").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("mode_fischer960"))
        composeRule.onNodeWithTag("mode_fischer960").assertIsDisplayed()
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_daily_puzzle_card"))
        composeRule.onNodeWithTag("home_daily_puzzle_card").assertIsDisplayed()
    }

    @Test
    fun `early saved matches persist to history without distorting player ratings`() = runBlocking {
        repository.ensureSeeded()
        val before = repository.profileFlow.first()!!

        val savedId = repository.recordCompletedMatch(
            whiteName = before.username,
            blackName = "Grandmaster AI Lv.4",
            opponentRating = 1400,
            playerSide = PieceColor.WHITE,
            variant = GameVariant.STANDARD,
            category = TimeControlCategory.RAPID,
            timeControlLabel = "7:30+5",
            chess960Position = 518,
            result = GameResult.IN_PROGRESS,
            termination = "Saved Early",
            openingEco = "C20",
            openingName = "King's Pawn Game",
            movesSan = listOf("e4", "e5"),
            startFen = ChessBoard.STANDARD_FEN,
            finalFen = ChessBoard.STANDARD_FEN,
            durationSeconds = 30,
            accuracyPercent = 0.0,
            pgnText = "[Event \"Saved\"]\n\n1. e4 e5 *",
            isRated = false
        )
        assertTrue(savedId > 0)

        val after = repository.profileFlow.first()!!
        assertEquals(before.rapidRating, after.rapidRating)
        assertEquals(0, after.totalMatches)
        val allMatches = repository.allMatchesFlow.first()
        assertEquals(1, allMatches.size)
        assertEquals("7:30+5", allMatches.first().timeControlLabel)
        assertEquals("Saved Early", allMatches.first().termination)
    }

    @Test
    fun `account choice dialog renders Google login and Guest options`() {
        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.components.AccountChoiceDialog(
                    currentUsername = "GuestPlayer",
                    googleNotice = null,
                    onGoogleSignIn = {},
                    onContinueAsGuest = {},
                    onDismissNotice = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_login_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("continue_as_guest_btn").assertIsDisplayed()
    }

    @Test
    fun `theme studio renders themes custom background picker and side by side compare`() {
        var appliedSetupTitle: String? = null
        composeRule.setContent {
            val settings = com.example.data.preferences.UserSettings()
            GrandmasterTheme(appTheme = settings.appTheme, darkTheme = settings.appTheme.isDark) {
                com.example.ui.components.ThemeStudioSheetContent(
                    settings = settings,
                    ownerName = "PlayerOne",
                    ownerRating = 1450,
                    initialTab = 0,
                    onSelectAppTheme = {},
                    onSelectBoardTheme = {},
                    onSelectBoardPattern = {},
                    onSelectPieceStyle = {},
                    onSelectBackgroundPreset = {},
                    onUpdateCustomGradient = { _, _ -> },
                    onUpdateBackgroundDim = {},
                    onPickCustomImageUri = {},
                    onClearCustomImage = {},
                    onApplyComparisonSetup = { appliedSetupTitle = it.title },
                    onClose = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_theme_card_GRANDMASTER_EMERALD").assertIsDisplayed()
        composeRule.onNodeWithTag("pick_custom_bg_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("board_pattern_row").performScrollTo()
        composeRule.onNodeWithTag("board_pattern_row").performScrollToNode(hasTestTag("board_pattern_chip_TOURNAMENT_BORDER"))
        composeRule.onNodeWithTag("board_pattern_chip_TOURNAMENT_BORDER").assertIsDisplayed()

        // Switch to Compare With Others tab
        composeRule.onNodeWithTag("theme_compare_tab").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("compare_arena_row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("apply_compare_b_btn").performScrollTo().performClick()
        assertNotNull("Applying setup B should invoke onApplyComparisonSetup", appliedSetupTitle)
    }

    @Test
    fun `theme options 16 boards 4 patterns 8 piece sets and comparison presets are valid`() {
        assertEquals(6, com.example.ui.theme.AppThemeOption.entries.size)
        assertEquals(16, com.example.ui.theme.BoardTheme.entries.size)
        assertEquals(4, com.example.ui.theme.BoardPatternStyle.entries.size)
        assertEquals(8, com.example.ui.theme.PieceStyle.entries.size)
        assertEquals(10, com.example.ui.theme.ArenaBackgroundPreset.entries.size)
        assertTrue(com.example.ui.theme.COMMUNITY_THEME_PRESETS.size >= 5)

        com.example.ui.theme.AppThemeOption.entries.forEach { theme ->
            val scheme = com.example.ui.theme.colorSchemeForTheme(theme)
            assertNotNull(scheme.primary)
            assertNotNull(scheme.background)
        }

        val settings = com.example.data.preferences.UserSettings(
            appTheme = com.example.ui.theme.AppThemeOption.CRIMSON_GRAND_PRIX,
            boardTheme = com.example.ui.theme.BoardTheme.ROSEWOOD_GOLD,
            boardPattern = com.example.ui.theme.BoardPatternStyle.TOURNAMENT_BORDER,
            pieceStyle = com.example.ui.theme.PieceStyle.OBSIDIAN_PEARL,
            backgroundPreset = com.example.ui.theme.ArenaBackgroundPreset.COSMIC_NEBULA
        )
        val setup = settings.toComparisonSetup("AlexGM", 1850)
        assertEquals("AlexGM", setup.authorLabel)
        assertEquals(com.example.ui.theme.AppThemeOption.CRIMSON_GRAND_PRIX, setup.appTheme)
        assertEquals(com.example.ui.theme.ArenaBackgroundPreset.COSMIC_NEBULA, setup.backgroundPreset)
    }

    @Test
    fun `conditional move executes automatically and match rating updates on finish`() = runBlocking {
        repository.ensureSeeded()
        val vm = com.example.ui.viewmodel.GameViewModel(repository, com.example.data.ChessSoundPlayer())

        // Start a Pass & Play conditional-testable match OR Bluetooth-style fixed white match
        vm.startNewGame(
            com.example.ui.viewmodel.GameSetupConfig(
                opponentMode = com.example.ui.viewmodel.PlayOpponentMode.BLUETOOTH_FRIEND,
                chosenColor = PieceColor.WHITE,
                isRated = true
            )
        )
        repeat(40) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(25))
            val s = vm.state.value
            if (s.config.opponentMode == com.example.ui.viewmodel.PlayOpponentMode.BLUETOOTH_FRIEND && s.preMatchStakes != null) {
                return@repeat
            }
            Thread.sleep(15)
        }
        assertEquals(com.example.ui.viewmodel.PlayOpponentMode.BLUETOOTH_FRIEND, vm.state.value.config.opponentMode)
        assertNotNull("Rated match should display pre-match Elo stakes", vm.state.value.preMatchStakes)

        // Play White 1.e4 & arm conditional: If Black plays e7e5 (e5) -> Auto-play g1f3 (Nf3)
        vm.playMoveWithConditional(
            myMoveUci = "e2e4",
            expectedOpponentUci = "e7e5",
            expectedOpponentSan = "e5",
            responseUci = "g1f3",
            responseSan = "Nf3"
        )
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(listOf("e4"), vm.state.value.sanHistory)
        assertEquals(1, vm.state.value.conditionalMoves.size)

        // Simulate Black playing e7e5 over Bluetooth and verify Nf3 auto-executes conditionally
        vm.onIncomingBluetoothPacketForTest(com.example.data.bluetooth.BtPacket.MoveSync("e7e5", 599_000L, 598_000L))
        repeat(20) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(40))
            if (vm.state.value.sanHistory.size >= 3) return@repeat
            Thread.sleep(15)
        }
        assertEquals(listOf("e4", "e5", "Nf3"), vm.state.value.sanHistory)
        assertTrue(vm.state.value.conditionalMoves.isEmpty())

        vm.resign()
        repeat(40) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(30))
            if (vm.state.value.whiteRatingDelta != null && vm.state.value.postGamePerfRating != null) return@repeat
            Thread.sleep(20)
        }
        assertTrue("Resignation loss should compute negative whiteRatingDelta", (vm.state.value.whiteRatingDelta ?: 0) < 0)
        assertNotNull("Post-game performance rating should be calculated", vm.state.value.postGamePerfRating)
    }

    @Test
    fun `main activity cold start initializes UserSettings and ViewModel cleanly`() {
        val defaultSettings = com.example.data.preferences.UserSettings()
        assertEquals(com.example.ui.theme.AppThemeOption.GRANDMASTER_EMERALD, defaultSettings.appTheme)
        assertTrue(com.example.ui.theme.COMMUNITY_THEME_PRESETS.isNotEmpty())

        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }

    @Test
    fun `endgame trainer screen renders categories drills and checkmate completes with 3 stars`() {
        val vm = com.example.ui.viewmodel.EndgameViewModel(com.example.data.ChessSoundPlayer())
        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.screens.EndgameScreen(
                    endgameViewModel = vm,
                    settings = com.example.data.preferences.UserSettings()
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("endgame_trainer_list").assertIsDisplayed()
        composeRule.onNodeWithTag("endgame_cat_BASIC_MATES").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("endgame_trainer_list").performScrollToNode(hasTestTag("endgame_box_toggle_btn"))
        composeRule.onNodeWithTag("endgame_box_toggle_btn").assertIsDisplayed()

        // Load K+Q vs K corner edge drill ("3k4/8/3K4/8/2Q5/8/8/8 w - - 0 1") and deliver Qg8# checkmate!
        val cornerDrill = com.example.chess.engine.EndgameLibrary.ALL_DRILLS.first { it.id == "eg_kq_corner" }
        vm.loadDrill(cornerDrill)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("endgame_trainer_list").performScrollToNode(hasTestTag("square_c4"))
        composeRule.onNodeWithTag("square_c4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("endgame_trainer_list").performScrollToNode(hasTestTag("square_g8"))
        composeRule.onNodeWithTag("square_g8").performClick()
        composeRule.waitForIdle()

        assertEquals(com.example.ui.viewmodel.EndgameStatus.CHECKMATED_WIN, vm.state.value.status)
        assertEquals(3, vm.state.value.earnedStars)
        composeRule.onNodeWithTag("endgame_trainer_list").performScrollToNode(hasTestTag("endgame_win_card"))
        composeRule.onNodeWithTag("endgame_win_card").assertIsDisplayed()
    }

    @Test
    fun `sound effect manager triggers distinct audio clips for move capture and checkmate`() = runBlocking {
        repository.ensureSeeded()
        val soundMgr = com.example.data.SoundEffectManager()
        soundMgr.attachContext(context)
        val vm = com.example.ui.viewmodel.GameViewModel(repository, soundMgr)
        vm.syncPreferences(
            sound = true,
            autoQueen = true,
            voiceEnabled = false,
            volume = 0.9f,
            pack = com.example.data.SoundPack.CRYSTAL_GLASS
        )
        vm.startNewGame(
            com.example.ui.viewmodel.GameSetupConfig(
                opponentMode = com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY,
                isRated = false
            )
        )
        repeat(30) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(20))
            if (vm.state.value.config.opponentMode == com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY) return@repeat
            Thread.sleep(10)
        }
        soundMgr.clearRecordedEvents()

        fun playSquares(fromAlg: String, toAlg: String) {
            vm.onSquareTapped(com.example.chess.engine.Square.fromAlgebraic(fromAlg))
            vm.onSquareTapped(com.example.chess.engine.Square.fromAlgebraic(toAlg))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }

        // 1. e4 e5 2. d4 exd4 (CAPTURE) 3. Bc4 Nc6 4. Qh5 Nf6 5. Qxf7# (CHECKMATE)
        playSquares("e2", "e4")
        playSquares("e7", "e5")
        playSquares("d2", "d4")
        playSquares("e5", "d4")
        playSquares("f1", "c4")
        playSquares("b8", "c6")
        playSquares("d1", "h5")
        playSquares("g8", "f6")
        playSquares("h5", "f7")

        assertEquals(GameResult.WHITE_WIN, vm.state.value.result)
        val events = soundMgr.recentEvents.value
        assertTrue("Expected MOVE clip in $events", events.contains(com.example.data.SoundEffect.MOVE))
        assertTrue("Expected CAPTURE clip in $events", events.contains(com.example.data.SoundEffect.CAPTURE))
        assertEquals(
            "Checkmate must trigger CHECKMATE without overlapping GAME_END",
            com.example.data.SoundEffect.CHECKMATE,
            events.lastOrNull()
        )
        assertEquals(1, events.count { it == com.example.data.SoundEffect.CHECKMATE })
        soundMgr.release()
    }

    @Test
    fun `move history list displays algebraic notation and tapping moves navigates game state`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val vm = com.example.ui.viewmodel.GameViewModel(repository, com.example.data.SoundEffectManager())
        vm.startNewGame(
            com.example.ui.viewmodel.GameSetupConfig(
                opponentMode = com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY,
                isRated = false
            )
        )
        repeat(30) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(20))
            if (vm.state.value.config.opponentMode == com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY) return@repeat
            Thread.sleep(10)
        }

        fun playSquares(fromAlg: String, toAlg: String) {
            vm.onSquareTapped(com.example.chess.engine.Square.fromAlgebraic(fromAlg))
            vm.onSquareTapped(com.example.chess.engine.Square.fromAlgebraic(toAlg))
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }

        playSquares("e2", "e4")
        playSquares("e7", "e5")
        playSquares("g1", "f3")
        assertEquals(listOf("e4", "e5", "Nf3"), vm.state.value.sanHistory)

        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.screens.PlayScreen(
                    gameViewModel = vm,
                    settings = com.example.data.preferences.UserSettings(),
                    onAnalyzeMatch = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("move_history_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("move_ply_1").assertIsDisplayed()
        composeRule.onNodeWithTag("move_ply_2").assertIsDisplayed()
        composeRule.onNodeWithTag("move_ply_3").assertIsDisplayed()

        // Tap ply 1 (e4): board rewinds to after 1. e4
        composeRule.onNodeWithTag("move_ply_1").performClick()
        composeRule.waitForIdle()
        assertEquals(1, vm.state.value.displayedPly)
        assertTrue(vm.state.value.isReviewingHistory)
        assertNotNull(vm.state.value.displayedBoard.pieceAt(com.example.chess.engine.Square.fromAlgebraic("e4")))
        assertEquals(null, vm.state.value.displayedBoard.pieceAt(com.example.chess.engine.Square.fromAlgebraic("f3")))
        composeRule.onNodeWithTag("move_history_live_btn").assertIsDisplayed()

        // Tap ply 2 (e5): board advances to after 1... e5
        composeRule.onNodeWithTag("move_ply_2").performClick()
        composeRule.waitForIdle()
        assertEquals(2, vm.state.value.displayedPly)
        assertNotNull(vm.state.value.displayedBoard.pieceAt(com.example.chess.engine.Square.fromAlgebraic("e5")))

        // Tap Start (ply 0): initial board
        composeRule.onNodeWithTag("move_ply_0").performClick()
        composeRule.waitForIdle()
        assertEquals(0, vm.state.value.displayedPly)
        assertEquals(ChessBoard.STANDARD_FEN, vm.state.value.displayedBoard.toFen())

        // Tap Live button: snaps back to latest state after 2. Nf3
        composeRule.onNodeWithTag("move_history_live_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(3, vm.state.value.displayedPly)
        assertTrue(!vm.state.value.isReviewingHistory)
        assertNotNull(vm.state.value.displayedBoard.pieceAt(com.example.chess.engine.Square.fromAlgebraic("f3")))

        // Expand into 2-column algebraic scoresheet view
        composeRule.onNodeWithTag("move_history_expand_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("move_history_scoresheet").assertIsDisplayed()
    }

    @Test
    fun `opening explorer displays master moves in analysis mode and tapping plays move`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val analysisVm = com.example.ui.viewmodel.AnalysisViewModel(repository)

        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.screens.AnalyzeScreen(
                    analysisViewModel = analysisVm,
                    settings = com.example.data.preferences.UserSettings()
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("analyze_list")
            .performScrollToNode(hasTestTag("opening_explorer_card"))
        composeRule.onNodeWithTag("opening_explorer_card").assertIsDisplayed()
        composeRule.onNodeWithTag("explorer_move_e4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("explorer_move_d4").performScrollTo().assertIsDisplayed()

        // Tap 1. e4 in Opening Explorer -> updates board and refreshes replies (c5, e5, e6, c6)
        composeRule.onNodeWithTag("explorer_move_e4").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("e4"), analysisVm.state.value.sanMoves)
        assertEquals(1, analysisVm.state.value.currentPly)
        val replies = analysisVm.state.value.explorerPosition?.moves?.map { it.san }.orEmpty()
        assertTrue("Expected Sicilian c5 and e5 after 1. e4, got $replies", replies.containsAll(listOf("c5", "e5")))
        composeRule.onNodeWithTag("explorer_move_c5").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `daily puzzle streak card launches streak run and increments score when solved`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val vmStore = androidx.lifecycle.ViewModelStore()
        val puzzleVm = com.example.ui.viewmodel.PuzzleViewModel(repository, com.example.data.SoundEffectManager())
        vmStore.put("puzzle", puzzleVm)
        val solvedEpochs = mutableListOf<Long>()
        val streakScores = mutableListOf<Int>()
        val dailyJob = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            puzzleVm.dailySolvedEvents.collect { solvedEpochs.add(it) }
        }
        val scoreJob = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            puzzleVm.streakRunScoreEvents.collect { streakScores.add(it) }
        }

        try {
            composeRule.setContent {
                GrandmasterTheme(darkTheme = true) {
                    com.example.ui.screens.PuzzlesScreen(
                        puzzleViewModel = puzzleVm,
                        profile = PlayerProfileEntity(),
                        attempts = emptyList(),
                        clubMembers = emptyList(),
                        settings = com.example.data.preferences.UserSettings()
                    )
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("daily_puzzle_streak_card").assertIsDisplayed()
            composeRule.onNodeWithTag("daily_streak_7day_strip").assertIsDisplayed()
            composeRule.onNodeWithTag("puzzle_daily_streak_btn").performClick()
            composeRule.waitForIdle()

            assertTrue(puzzleVm.state.value.isDailyStreakMode)
            composeRule.onNodeWithTag("puzzles_tactics_list")
                .performScrollToNode(hasTestTag("daily_streak_active_banner"))
            composeRule.onNodeWithTag("daily_streak_active_banner").assertIsDisplayed()

            // Play out solution steps of the active streak puzzle (deterministic mate/tactic)
            var guard = 0
            while (puzzleVm.state.value.status != com.example.ui.viewmodel.PuzzleStatus.SOLVED && guard++ < 12) {
                val st = puzzleVm.state.value
                if (st.board.sideToMove == st.playerSide) {
                    val nextUci = st.currentPuzzle.solutionUci[st.stepIndex]
                    val fromSq = com.example.chess.engine.Square.fromAlgebraic(nextUci.substring(0, 2))
                    val toSq = com.example.chess.engine.Square.fromAlgebraic(nextUci.substring(2, 4))
                    puzzleVm.onSquareTapped(fromSq)
                    puzzleVm.onSquareTapped(toSq)
                }
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(450))
            }
            composeRule.waitForIdle()

            assertEquals(com.example.ui.viewmodel.PuzzleStatus.SOLVED, puzzleVm.state.value.status)
            assertEquals(1, puzzleVm.state.value.streakRunCurrent)
            assertTrue("Expected daily solved epoch emitted", solvedEpochs.isNotEmpty())
            assertEquals(listOf(1), streakScores)

            // Drain asynchronous puzzle result write before @After closes the in-memory DB
            repeat(40) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (repository.puzzleAttemptsFlow.first().isNotEmpty()) return@repeat
                Thread.sleep(15)
            }
        } finally {
            dailyJob.cancel()
            scoreJob.cancel()
            vmStore.clear()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `logical graphs render radar color outcomes and progression in profile dashboard`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val dataStore = com.example.data.preferences.SettingsDataStore(context)
        val vmStore = androidx.lifecycle.ViewModelStore()
        val mainVm = com.example.ui.viewmodel.MainViewModel(repository, dataStore)
        vmStore.put("main", mainVm)
        try {
            mainVm.openProfileTab(0)
            val profile = PlayerProfileEntity(
                username = "GraphGM",
                rapidRating = 1540,
                blitzRating = 1490,
                puzzleRating = 1620,
                longestWinStreak = 5
            )
            val snapshots = listOf(
                com.example.data.local.RatingSnapshotEntity(formatKey = "RAPID", rating = 1450),
                com.example.data.local.RatingSnapshotEntity(formatKey = "RAPID", rating = 1540)
            )
            val sampleMatches = listOf(
                com.example.data.local.MatchHistoryEntity(
                    whiteName = "GraphGM", blackName = "Engine",
                    whiteRatingBefore = 1500, whiteRatingAfter = 1540,
                    blackRatingBefore = 1500, blackRatingAfter = 1460,
                    playerSide = "WHITE", variant = "STANDARD", category = "RAPID",
                    timeControlLabel = "10+0", chess960Position = 518,
                    result = "WHITE_WIN", termination = "Checkmate",
                    openingEco = "C50", openingName = "Italian",
                    movesSanCsv = "e4,e5", startFen = ChessBoard.STANDARD_FEN, finalFen = ChessBoard.STANDARD_FEN,
                    moveCount = 30, durationSeconds = 250, accuracyPercent = 91.0, pgnText = ""
                )
            )

            composeRule.setContent {
                GrandmasterTheme(darkTheme = true) {
                    com.example.ui.screens.ProfileScreen(
                        mainViewModel = mainVm,
                        profile = profile,
                        matches = sampleMatches,
                        ratingSnapshots = snapshots,
                        achievements = emptyList(),
                        settings = com.example.data.preferences.UserSettings(dailyPuzzleStreak = 2),
                        onOpenMatchAnalysis = {}
                    )
                }
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("profile_dashboard_list")
                .performScrollToNode(hasTestTag("logical_progression_graph"))
            composeRule.onNodeWithTag("logical_progression_graph").assertIsDisplayed()

            composeRule.onNodeWithTag("profile_dashboard_list")
                .performScrollToNode(hasTestTag("logical_radar_graph"))
            composeRule.onNodeWithTag("logical_radar_graph").assertIsDisplayed()

            composeRule.onNodeWithTag("profile_dashboard_list")
                .performScrollToNode(hasTestTag("logical_color_outcome_graph"))
            composeRule.onNodeWithTag("logical_color_outcome_graph").assertIsDisplayed()
        } finally {
            vmStore.clear()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `blind audio chess mode plays spoken moves via piece selector and command bar`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val vm = com.example.ui.viewmodel.GameViewModel(repository, com.example.data.SoundEffectManager())
        vm.startNewGame(
            com.example.ui.viewmodel.GameSetupConfig(
                opponentMode = com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY,
                isBlindMode = true
            )
        )
        repeat(60) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            if (vm.state.value.config.isBlindMode &&
                vm.state.value.config.opponentMode == com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY
            ) return@repeat
            Thread.sleep(10)
        }

        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.screens.PlayScreen(
                    gameViewModel = vm,
                    settings = com.example.data.preferences.UserSettings(),
                    onAnalyzeMatch = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("blind_chess_panel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("blind_spoken_banner").assertIsDisplayed()

        // Step 1 & 2: Play 1. e4 through accessible piece selector & legal move buttons
        composeRule.onNodeWithTag("blind_piece_selector")
            .performScrollTo()
            .performScrollToNode(hasTestTag("blind_piece_e2"))
        composeRule.onNodeWithTag("blind_piece_e2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blind_move_e2e4").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("e4"), vm.state.value.sanHistory)

        // Spoken/Typed natural command for Black reply 1... c5
        composeRule.onNodeWithTag("blind_move_input").performScrollTo().performTextInput("c5")
        composeRule.onNodeWithTag("blind_voice_submit_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("e4", "c5"), vm.state.value.sanHistory)

        // Toggle Blindfold hide pieces switch
        composeRule.onNodeWithTag("blind_hide_pieces_toggle").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(vm.state.value.blindfoldHidePieces)
    }

    @Test
    fun `tactical themes filter switches daily puzzles between pin fork skewer and discovery`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val puzzleVm = com.example.ui.viewmodel.PuzzleViewModel(repository, com.example.data.SoundEffectManager())

        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.screens.PuzzlesScreen(
                    puzzleViewModel = puzzleVm,
                    profile = PlayerProfileEntity(),
                    attempts = emptyList(),
                    clubMembers = emptyList(),
                    settings = com.example.data.preferences.UserSettings()
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("puzzles_tactics_list")
            .performScrollToNode(hasTestTag("tactical_themes_filter_row"))
        composeRule.onNodeWithTag("tactical_themes_filter_row").assertIsDisplayed()

        // Filter by SKEWER and verify Skewer puzzle loads
        composeRule.onNodeWithTag("tactical_themes_filter_row")
            .performScrollToNode(hasTestTag("theme_filter_SKEWER"))
        composeRule.onNodeWithTag("theme_filter_SKEWER").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.chess.engine.TacticalThemeFilter.SKEWER, puzzleVm.state.value.selectedThemeFilter)
        assertTrue(puzzleVm.state.value.currentPuzzle.theme.contains("Skewer", ignoreCase = true))

        // Filter by DISCOVERY and verify Discovery puzzle loads
        composeRule.onNodeWithTag("tactical_themes_filter_row")
            .performScrollToNode(hasTestTag("theme_filter_DISCOVERY"))
        composeRule.onNodeWithTag("theme_filter_DISCOVERY").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.chess.engine.TacticalThemeFilter.DISCOVERY, puzzleVm.state.value.selectedThemeFilter)
        assertTrue(
            com.example.chess.engine.TacticalThemeFilter.DISCOVERY.matches(puzzleVm.state.value.currentPuzzle)
        )
    }

    @Test
    fun `play screen provides labeled undo draw cond analysis controls and live eval graph`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val vmStore = androidx.lifecycle.ViewModelStore()
        val vm = com.example.ui.viewmodel.GameViewModel(repository, com.example.data.SoundEffectManager())
        vmStore.put("game", vm)
        try {
            vm.startNewGame(
                com.example.ui.viewmodel.GameSetupConfig(
                    opponentMode = com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY
                )
            )
            repeat(40) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (vm.state.value.config.opponentMode == com.example.ui.viewmodel.PlayOpponentMode.PASS_AND_PLAY) return@repeat
                Thread.sleep(10)
            }

            composeRule.setContent {
                GrandmasterTheme(darkTheme = true) {
                    com.example.ui.screens.PlayScreen(
                        gameViewModel = vm,
                        settings = com.example.data.preferences.UserSettings(),
                        onAnalyzeMatch = {},
                        playerRapidRating = 1350
                    )
                }
            }
            composeRule.waitForIdle()

            // Banners show White/Black names cleanly
            composeRule.onNodeWithTag("player_banner_top").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("player_banner_bottom").performScrollTo().assertIsDisplayed()

            // Make a move 1. e4
            vm.onSquareTapped(com.example.chess.engine.Square.of(4, 1)) // e2
            vm.onSquareTapped(com.example.chess.engine.Square.of(4, 3)) // e4
            composeRule.waitForIdle()
            assertEquals(listOf("e4"), vm.state.value.sanHistory)

            // Toggle Live Analysis ON while playing
            composeRule.onNodeWithTag("game_controls_bar")
                .performScrollTo()
                .performScrollToNode(hasTestTag("play_live_analysis_btn"))
            composeRule.onNodeWithTag("play_live_analysis_btn").performClick()
            repeat(60) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (vm.state.value.liveAnalyzedMoves.isNotEmpty()) return@repeat
                Thread.sleep(15)
            }
            composeRule.waitForIdle()
            assertTrue(vm.state.value.isLiveAnalysisEnabled)
            composeRule.onNodeWithTag("live_play_analysis_card").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("logical_eval_flow_graph").performScrollTo().assertIsDisplayed()

            // Test Undo button below board
            composeRule.onNodeWithTag("game_controls_bar")
                .performScrollTo()
                .performScrollToNode(hasTestTag("game_takeback_btn"))
            composeRule.onNodeWithTag("game_takeback_btn").performClick()
            composeRule.waitForIdle()
            assertTrue(vm.state.value.sanHistory.isEmpty())

            // Test Draw button in Pass & Play
            composeRule.onNodeWithTag("game_controls_bar")
                .performScrollToNode(hasTestTag("game_draw_btn"))
            composeRule.onNodeWithTag("game_draw_btn").performClick()
            composeRule.waitForIdle()
            assertEquals(com.example.chess.engine.GameResult.DRAW, vm.state.value.result)

            // Drain asynchronous match persistence before @After closes the in-memory DB
            repeat(40) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (repository.allMatchesFlow.first().any { it.result == "DRAW" }) return@repeat
                Thread.sleep(15)
            }
        } finally {
            vmStore.clear()
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `home screen elo booster updates opponent elo and win loss probabilities`() = runBlocking<Unit> {
        repository.ensureSeeded()
        val profile = repository.profileFlow.let {
            var p = com.example.data.local.PlayerProfileEntity()
            runBlocking {
                repository.adjustPlayerFormatRating("RAPID", 100)
            }
            p.copy(rapidRating = 1300)
        }

        composeRule.setContent {
            GrandmasterTheme(darkTheme = true) {
                com.example.ui.screens.HomeScreen(
                    profile = profile,
                    settings = com.example.data.preferences.UserSettings(),
                    recentMatches = emptyList(),
                    announcements = emptyList(),
                    dailyRewardBannerXp = null,
                    onClaimDailyReward = {},
                    onDismissRewardBanner = {},
                    onStartGame = {},
                    onOpenFischerHub = {},
                    onOpenDailyPuzzle = {},
                    onOpenMatchAnalysis = {},
                    onOpenCompete = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("home_elo_selector"))
        composeRule.onNodeWithText("Opponent Elo: 1400").assertIsDisplayed()
        composeRule.onNodeWithTag("home_elo_forecast_bar").assertIsDisplayed()

        // Increase opponent Elo by +50 twice -> 1500
        composeRule.onNodeWithTag("home_elo_increase_btn").performClick()
        composeRule.onNodeWithTag("home_elo_increase_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Opponent Elo: 1500").assertIsDisplayed()
    }
}
