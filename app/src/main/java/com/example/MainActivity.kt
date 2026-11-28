package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.example.chess.coach.CoachVoiceEngine
import com.example.chess.engine.GameVariant
import com.example.data.ChessSoundPlayer
import com.example.data.bluetooth.BluetoothChessManager
import com.example.data.local.ChessDatabase
import com.example.data.preferences.AccountMode
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.ChessRepository
import com.example.ui.components.AccountChoiceDialog
import com.example.ui.components.ArenaBackgroundLayer
import com.example.ui.components.BluetoothLobbySheet
import com.example.ui.components.ThemeStudioSheetContent
import com.example.ui.screens.AnalyzeScreen
import com.example.ui.screens.CompeteScreen
import com.example.ui.screens.EndgameScreen
import com.example.ui.screens.FischerChessScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.theme.ArenaBackgroundPreset
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.GrandmasterTheme
import com.example.ui.viewmodel.AnalysisViewModel
import com.example.ui.viewmodel.EndgameViewModel
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PuzzleViewModel

object AppRoutes {
    const val HOME = "home"
    const val PLAY = "play"
    const val FISCHER_960 = "fischer960"
    const val PUZZLES = "puzzles"
    const val ENDGAME = "endgame"
    const val COMPETE = "compete"
    const val ANALYZE = "analyze"
    const val PROFILE = "profile"
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PRIMARY_NAV_ITEMS = listOf(
    NavDestinationItem(AppRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavDestinationItem(AppRoutes.PLAY, "Play", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "nav_play"),
    NavDestinationItem(AppRoutes.FISCHER_960, "960", Icons.Filled.Casino, Icons.Outlined.Casino, "nav_960"),
    NavDestinationItem(AppRoutes.PUZZLES, "Puzzles", Icons.Filled.Extension, Icons.Outlined.Extension, "nav_puzzles"),
    NavDestinationItem(AppRoutes.COMPETE, "Compete", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_compete")
)

private val ALL_RAIL_ITEMS = PRIMARY_NAV_ITEMS + listOf(
    NavDestinationItem(AppRoutes.ENDGAME, "Endgames", Icons.Filled.School, Icons.Outlined.School, "nav_endgame"),
    NavDestinationItem(AppRoutes.ANALYZE, "Analyze", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_analyze"),
    NavDestinationItem(AppRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {

    private val soundManager by lazy { ChessSoundPlayer() }

    private val viewModelFactory by lazy {
        val db = ChessDatabase.getInstance(applicationContext)
        val repository = ChessRepository(db.chessDao())
        val settingsStore = SettingsDataStore(applicationContext)

        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(MainViewModel::class.java) ->
                        MainViewModel(repository, settingsStore) as T
                    modelClass.isAssignableFrom(GameViewModel::class.java) ->
                        GameViewModel(repository, soundManager) as T
                    modelClass.isAssignableFrom(PuzzleViewModel::class.java) ->
                        PuzzleViewModel(repository, soundManager) as T
                    modelClass.isAssignableFrom(EndgameViewModel::class.java) ->
                        EndgameViewModel(soundManager) as T
                    modelClass.isAssignableFrom(AnalysisViewModel::class.java) ->
                        AnalysisViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    private var bluetoothManager: BluetoothChessManager? = null
    private var coachVoiceEngine: CoachVoiceEngine? = null

    private val mainViewModel: MainViewModel by viewModels { viewModelFactory }
    private val gameViewModel: GameViewModel by viewModels { viewModelFactory }
    private val puzzleViewModel: PuzzleViewModel by viewModels { viewModelFactory }
    private val endgameViewModel: EndgameViewModel by viewModels { viewModelFactory }
    private val analysisViewModel: AnalysisViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        soundManager.attachContext(applicationContext)
        val bt = BluetoothChessManager(applicationContext).also { bluetoothManager = it }
        val voice = CoachVoiceEngine(applicationContext).also { coachVoiceEngine = it }
        gameViewModel.attachPlatformServices(bt, voice)
        endgameViewModel.attachVoiceEngine(voice)
        setContent {
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            GrandmasterTheme(
                appTheme = settings.appTheme,
                darkTheme = settings.appTheme.isDark
            ) {
                GrandmasterAppShell(
                    mainViewModel = mainViewModel,
                    gameViewModel = gameViewModel,
                    puzzleViewModel = puzzleViewModel,
                    endgameViewModel = endgameViewModel,
                    analysisViewModel = analysisViewModel,
                    bluetoothManager = bt,
                    soundManager = soundManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager?.release()
        coachVoiceEngine?.shutdown()
        soundManager.release()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandmasterAppShell(
    mainViewModel: MainViewModel,
    gameViewModel: GameViewModel,
    puzzleViewModel: PuzzleViewModel,
    endgameViewModel: EndgameViewModel? = null,
    analysisViewModel: AnalysisViewModel,
    bluetoothManager: BluetoothChessManager? = null,
    soundManager: ChessSoundPlayer? = null
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.HOME

    val profile by mainViewModel.profile.collectAsStateWithLifecycle()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val allMatches by mainViewModel.allMatches.collectAsStateWithLifecycle()
    val fischerMatches by mainViewModel.fischerMatches.collectAsStateWithLifecycle()
    val ratingSnapshots by mainViewModel.ratingSnapshots.collectAsStateWithLifecycle()
    val puzzleAttempts by mainViewModel.puzzleAttempts.collectAsStateWithLifecycle()
    val tournaments by mainViewModel.tournaments.collectAsStateWithLifecycle()
    val clubMembers by mainViewModel.clubMembers.collectAsStateWithLifecycle()
    val reports by mainViewModel.reports.collectAsStateWithLifecycle()
    val announcements by mainViewModel.announcements.collectAsStateWithLifecycle()
    val dailyRewardBannerXp by mainViewModel.dailyRewardBannerXp.collectAsStateWithLifecycle()
    val googleAuthNotice by mainViewModel.googleAuthNotice.collectAsStateWithLifecycle()
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val resolvedBtManager = bluetoothManager ?: remember(context) { BluetoothChessManager(context) }
    val resolvedSoundMgr = soundManager ?: remember { ChessSoundPlayer() }
    val resolvedEndgameVm = endgameViewModel ?: remember { EndgameViewModel(resolvedSoundMgr) }
    LaunchedEffect(resolvedBtManager) {
        gameViewModel.attachPlatformServices(resolvedBtManager, null)
    }
    LaunchedEffect(settings.soundVolume, settings.soundPack) {
        resolvedSoundMgr.configure(settings.soundVolume, settings.soundPack)
    }
    LaunchedEffect(resolvedEndgameVm) {
        resolvedEndgameVm.completionEvents.collect { evt ->
            mainViewModel.recordEndgameCompletion(evt.drillId, evt.stars, evt.movesUsed)
        }
    }
    LaunchedEffect(puzzleViewModel) {
        puzzleViewModel.dailySolvedEvents.collect { epochDay ->
            mainViewModel.recordDailyPuzzleCompleted(epochDay)
        }
    }
    LaunchedEffect(puzzleViewModel) {
        puzzleViewModel.streakRunScoreEvents.collect { score ->
            mainViewModel.recordDailyStreakRun(score)
        }
    }

    var showThemeStudioSheet by remember { mutableStateOf(false) }
    var showBluetoothLobbySheet by remember { mutableStateOf(false) }
    var themeStudioInitialTab by remember { mutableIntStateOf(0) }
    val openThemeStudio: (Int) -> Unit = { tabIdx ->
        themeStudioInitialTab = tabIdx.coerceIn(0, 1)
        showThemeStudioSheet = true
    }

    val achievements = remember(profile) { mainViewModel.achievementsFor(profile) }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openHistory: () -> Unit = {
        mainViewModel.openProfileTab(1)
        navigateTo(AppRoutes.PROFILE)
    }

    val routeTitle = when (currentRoute) {
        AppRoutes.HOME -> stringResource(R.string.app_name)
        AppRoutes.PLAY -> "Play Chess"
        AppRoutes.FISCHER_960 -> "Fischer Chess 960"
        AppRoutes.PUZZLES -> "Daily Puzzles"
        AppRoutes.ENDGAME -> "Endgame Trainer"
        AppRoutes.COMPETE -> "Club & Tournaments"
        AppRoutes.ANALYZE -> "Engine Analysis"
        AppRoutes.PROFILE -> "Player Profile"
        else -> stringResource(R.string.app_name)
    }

    val isSecondaryRoute = currentRoute == AppRoutes.ANALYZE ||
        currentRoute == AppRoutes.PROFILE ||
        currentRoute == AppRoutes.ENDGAME

    val isCustomBackgroundActive = settings.backgroundPreset != ArenaBackgroundPreset.SOLID_THEME
    val chromeSurfaceColor = if (isCustomBackgroundActive) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ArenaBackgroundLayer(
            appTheme = settings.appTheme,
            backgroundPreset = settings.backgroundPreset,
            customGradientStartArgb = settings.customGradientStartArgb,
            customGradientEndArgb = settings.customGradientEndArgb,
            customBackgroundImagePath = settings.customBackgroundImagePath,
            backgroundDimAlpha = settings.backgroundDimAlpha,
            modifier = Modifier.fillMaxSize()
        )
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = chromeSurfaceColor
                ) {
                    Spacer(Modifier.size(12.dp))
                    ALL_RAIL_ITEMS.forEach { item ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = if (isCustomBackgroundActive) Color.Transparent else MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = chromeSurfaceColor,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        navigationIcon = {
                            if (!useRail && isSecondaryRoute) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.testTag("top_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate Back"
                                    )
                                }
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = routeTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${profile.rapidRating}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ChampionshipGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { openThemeStudio(0) },
                                modifier = Modifier.testTag("top_theme_studio_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = "Themes, Backgrounds & Compare",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!useRail) {
                                IconButton(
                                    onClick = { navigateTo(AppRoutes.ANALYZE) },
                                    modifier = Modifier.testTag("top_analyze_button")
                                ) {
                                    Icon(
                                        imageVector = if (currentRoute == AppRoutes.ANALYZE) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                        contentDescription = "Engine Analysis",
                                        tint = if (currentRoute == AppRoutes.ANALYZE) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { navigateTo(AppRoutes.PROFILE) },
                                    modifier = Modifier.testTag("top_profile_button")
                                ) {
                                    Icon(
                                        imageVector = if (currentRoute == AppRoutes.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Player Profile and History",
                                        tint = if (currentRoute == AppRoutes.PROFILE) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = chromeSurfaceColor
                        ) {
                            PRIMARY_NAV_ITEMS.forEach { item ->
                                val selected = backStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.HOME,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(AppRoutes.HOME) {
                            HomeScreen(
                                profile = profile,
                                settings = settings,
                                recentMatches = allMatches,
                                announcements = announcements,
                                dailyRewardBannerXp = dailyRewardBannerXp,
                                onClaimDailyReward = { mainViewModel.claimDailyLoginReward() },
                                onDismissRewardBanner = { mainViewModel.dismissDailyRewardBanner() },
                                onStartGame = { config ->
                                    gameViewModel.startNewGame(config)
                                    navigateTo(AppRoutes.PLAY)
                                },
                                onOpenFischerHub = { navigateTo(AppRoutes.FISCHER_960) },
                                onOpenDailyPuzzle = {
                                    puzzleViewModel.loadDailyPuzzle()
                                    navigateTo(AppRoutes.PUZZLES)
                                },
                                onOpenDailyStreak = {
                                    puzzleViewModel.startDailyStreakRun()
                                    navigateTo(AppRoutes.PUZZLES)
                                },
                                onOpenMatchAnalysis = { matchId ->
                                    analysisViewModel.loadMatchById(matchId)
                                    navigateTo(AppRoutes.ANALYZE)
                                },
                                onOpenCompete = { navigateTo(AppRoutes.COMPETE) },
                                onOpenHistory = openHistory,
                                onOpenAccountChoice = { mainViewModel.reopenAccountChoice() },
                                onOpenThemeStudio = { tabIdx -> openThemeStudio(tabIdx) },
                                onOpenBluetoothLobby = { showBluetoothLobbySheet = true },
                                onOpenEndgameTrainer = { navigateTo(AppRoutes.ENDGAME) },
                                onOpenLogicalGraphs = {
                                    mainViewModel.openProfileTab(0)
                                    navigateTo(AppRoutes.PROFILE)
                                },
                                onSaveTimeControlPreference = mainViewModel::saveTimeControlPreference,
                                onSetCoachDavidDefault = mainViewModel::setCoachDavidDefault
                            )
                        }

                        composable(AppRoutes.PLAY) {
                            PlayScreen(
                                gameViewModel = gameViewModel,
                                settings = settings,
                                onAnalyzeMatch = { matchId ->
                                    analysisViewModel.loadMatchById(matchId)
                                    navigateTo(AppRoutes.ANALYZE)
                                },
                                onOpenHistory = openHistory,
                                onOpenThemeStudio = { openThemeStudio(0) },
                                onOpenBluetoothLobby = { showBluetoothLobbySheet = true },
                                onToggleCoachVoice = mainViewModel::setCoachVoiceEnabled,
                                onSaveTimeControlPreference = mainViewModel::saveTimeControlPreference,
                                onSetCoachDavidDefault = mainViewModel::setCoachDavidDefault,
                                playerRapidRating = profile.rapidRating,
                                onRenamePlayer = mainViewModel::updateUsername
                            )
                        }

                        composable(AppRoutes.FISCHER_960) {
                            FischerChessScreen(
                                profile = profile,
                                fischerMatches = fischerMatches,
                                clubMembers = clubMembers,
                                settings = settings,
                                onStartFischerGame = { config ->
                                    gameViewModel.startNewGame(config)
                                    navigateTo(AppRoutes.PLAY)
                                },
                                onAnalyzePositionFen = { fen, pos ->
                                    analysisViewModel.loadFen(fen, GameVariant.FISCHER960, pos)
                                    navigateTo(AppRoutes.ANALYZE)
                                },
                                onOpenMatchAnalysis = { matchId ->
                                    analysisViewModel.loadMatchById(matchId)
                                    navigateTo(AppRoutes.ANALYZE)
                                },
                                onOpenBluetoothLobby = { showBluetoothLobbySheet = true },
                                onSaveTimeControlPreference = mainViewModel::saveTimeControlPreference,
                                onSetCoachDavidDefault = mainViewModel::setCoachDavidDefault
                            )
                        }

                        composable(AppRoutes.PUZZLES) {
                            PuzzlesScreen(
                                puzzleViewModel = puzzleViewModel,
                                profile = profile,
                                attempts = puzzleAttempts,
                                clubMembers = clubMembers,
                                settings = settings,
                                onOpenEndgameTrainer = { navigateTo(AppRoutes.ENDGAME) }
                            )
                        }

                        composable(AppRoutes.ENDGAME) {
                            EndgameScreen(
                                endgameViewModel = resolvedEndgameVm,
                                settings = settings
                            )
                        }

                        composable(AppRoutes.COMPETE) {
                            CompeteScreen(
                                mainViewModel = mainViewModel,
                                profile = profile,
                                settings = settings,
                                tournaments = tournaments,
                                clubMembers = clubMembers,
                                reports = reports,
                                announcements = announcements,
                                matches = allMatches,
                                onStartTournamentOrChallengeGame = { config ->
                                    gameViewModel.startNewGame(config)
                                    navigateTo(AppRoutes.PLAY)
                                }
                            )
                        }

                        composable(AppRoutes.ANALYZE) {
                            AnalyzeScreen(
                                analysisViewModel = analysisViewModel,
                                settings = settings
                            )
                        }

                        composable(AppRoutes.PROFILE) {
                            ProfileScreen(
                                mainViewModel = mainViewModel,
                                profile = profile,
                                matches = allMatches,
                                ratingSnapshots = ratingSnapshots,
                                achievements = achievements,
                                settings = settings,
                                onOpenMatchAnalysis = { matchId ->
                                    analysisViewModel.loadMatchById(matchId)
                                    navigateTo(AppRoutes.ANALYZE)
                                },
                                onOpenThemeStudio = { tabIdx -> openThemeStudio(tabIdx) },
                                onPreviewSound = { eff -> resolvedSoundMgr.preview(eff) },
                                puzzleAttempts = puzzleAttempts
                            )
                        }
                    }
                }
            }
        }
    }

    if (showThemeStudioSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeStudioSheet = false }) {
            ThemeStudioSheetContent(
                settings = settings,
                ownerName = profile.username,
                ownerRating = profile.rapidRating,
                initialTab = themeStudioInitialTab,
                onSelectAppTheme = mainViewModel::selectAppTheme,
                onSelectBoardTheme = mainViewModel::selectBoardTheme,
                onSelectBoardPattern = mainViewModel::selectBoardPattern,
                onSelectPieceStyle = mainViewModel::selectPieceStyle,
                onSelectBackgroundPreset = mainViewModel::selectBackgroundPreset,
                onUpdateCustomGradient = mainViewModel::updateCustomGradient,
                onUpdateBackgroundDim = mainViewModel::updateBackgroundDim,
                onPickCustomImageUri = { uri -> mainViewModel.saveCustomBackgroundFromUri(context, uri) },
                onClearCustomImage = { mainViewModel.clearCustomBackgroundImage(context) },
                onApplyComparisonSetup = mainViewModel::applyComparisonSetup,
                onClose = { showThemeStudioSheet = false }
            )
        }
    }

    if (showBluetoothLobbySheet) {
        ModalBottomSheet(onDismissRequest = { showBluetoothLobbySheet = false }) {
            BluetoothLobbySheet(
                bluetoothManager = resolvedBtManager,
                connectionState = gameState.btConnectionState,
                statusText = gameState.btStatusText,
                connectedPeer = gameState.btPeerName,
                devices = gameState.btDevices,
                settings = settings,
                onRefreshDevices = gameViewModel::refreshBluetoothDevices,
                onStartDiscovery = gameViewModel::startBluetoothDiscovery,
                onHostMatch = { variant, pos, tc, hostColor, isRated, coachEnabled ->
                    gameViewModel.hostBluetoothMatch(
                        variant = variant,
                        chess960Pos = pos,
                        timeControl = tc,
                        hostColor = hostColor,
                        isRated = isRated,
                        coachEnabled = coachEnabled
                    ) {
                        showBluetoothLobbySheet = false
                        navigateTo(AppRoutes.PLAY)
                    }
                },
                onConnectDevice = { address, coachEnabled ->
                    gameViewModel.connectBluetoothFriend(address, coachEnabled) {
                        showBluetoothLobbySheet = false
                        navigateTo(AppRoutes.PLAY)
                    }
                },
                onDisconnect = gameViewModel::disconnectBluetooth,
                onClose = { showBluetoothLobbySheet = false }
            )
        }
    }

    if (settings.accountMode == AccountMode.UNDECIDED) {
        AccountChoiceDialog(
            currentUsername = profile.username,
            googleNotice = googleAuthNotice,
            onGoogleSignIn = { mainViewModel.requestGoogleSignIn() },
            onContinueAsGuest = { guestName -> mainViewModel.continueAsGuest(guestName) },
            onDismissNotice = { mainViewModel.dismissGoogleSignInNotice() }
        )
    }
}
