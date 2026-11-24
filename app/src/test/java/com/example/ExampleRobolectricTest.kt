package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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

        // Switch to Compare With Others tab
        composeRule.onNodeWithTag("theme_compare_tab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("compare_arena_row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("apply_compare_b_btn").performScrollTo().performClick()
        assertNotNull("Applying setup B should invoke onApplyComparisonSetup", appliedSetupTitle)
    }

    @Test
    fun `theme options boards backgrounds and community comparison presets are valid`() {
        assertEquals(6, com.example.ui.theme.AppThemeOption.entries.size)
        assertEquals(8, com.example.ui.theme.BoardTheme.entries.size)
        assertEquals(4, com.example.ui.theme.PieceStyle.entries.size)
        assertEquals(10, com.example.ui.theme.ArenaBackgroundPreset.entries.size)
        assertTrue(com.example.ui.theme.COMMUNITY_THEME_PRESETS.size >= 5)

        com.example.ui.theme.AppThemeOption.entries.forEach { theme ->
            val scheme = com.example.ui.theme.colorSchemeForTheme(theme)
            assertNotNull(scheme.primary)
            assertNotNull(scheme.background)
        }

        val settings = com.example.data.preferences.UserSettings(
            appTheme = com.example.ui.theme.AppThemeOption.CRIMSON_GRAND_PRIX,
            boardTheme = com.example.ui.theme.BoardTheme.TOURNAMENT_CRIMSON,
            pieceStyle = com.example.ui.theme.PieceStyle.TOURNAMENT_CRYSTAL,
            backgroundPreset = com.example.ui.theme.ArenaBackgroundPreset.COSMIC_NEBULA
        )
        val setup = settings.toComparisonSetup("AlexGM", 1850)
        assertEquals("AlexGM", setup.authorLabel)
        assertEquals(com.example.ui.theme.AppThemeOption.CRIMSON_GRAND_PRIX, setup.appTheme)
        assertEquals(com.example.ui.theme.ArenaBackgroundPreset.COSMIC_NEBULA, setup.backgroundPreset)
    }
}
