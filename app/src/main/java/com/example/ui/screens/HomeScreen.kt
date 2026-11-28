package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.chess.engine.DailyPuzzleStreakLogic
import com.example.chess.engine.GameVariant
import com.example.chess.engine.PuzzleLibrary
import com.example.chess.engine.TimeControl
import com.example.data.RatingCalculator
import com.example.data.local.AnnouncementEntity
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.preferences.AccountMode
import com.example.data.preferences.UserSettings
import com.example.ui.components.CoachDavidOptionToggle
import com.example.ui.components.CountryFlag
import com.example.ui.components.TimeControlPickerSection
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PieceStyle
import com.example.ui.viewmodel.GameSetupConfig
import com.example.ui.viewmodel.PlayOpponentMode
import java.time.LocalDate

@Composable
fun HomeScreen(
    profile: PlayerProfileEntity,
    settings: UserSettings = UserSettings(),
    recentMatches: List<MatchHistoryEntity>,
    announcements: List<AnnouncementEntity>,
    dailyRewardBannerXp: Int?,
    onClaimDailyReward: () -> Unit,
    onDismissRewardBanner: () -> Unit,
    onStartGame: (GameSetupConfig) -> Unit,
    onOpenFischerHub: () -> Unit,
    onOpenDailyPuzzle: () -> Unit,
    onOpenDailyStreak: () -> Unit = onOpenDailyPuzzle,
    onOpenMatchAnalysis: (Long) -> Unit,
    onOpenCompete: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenAccountChoice: () -> Unit = {},
    onOpenThemeStudio: (Int) -> Unit = {},
    onOpenBluetoothLobby: () -> Unit = {},
    onOpenEndgameTrainer: () -> Unit = {},
    onOpenLogicalGraphs: () -> Unit = {},
    onSaveTimeControlPreference: (Boolean, String, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onSetCoachDavidDefault: (Boolean) -> Unit = {}
) {
    val dailyPuzzle = PuzzleLibrary.puzzleForDate(LocalDate.now())
    val todayClaimed = profile.lastDailyClaimEpochDay == LocalDate.now().toEpochDay()
    val level = RatingCalculator.levelForXp(profile.xp)

    var useCustomTime by remember(settings.useCustomTimeDefault) { mutableStateOf(settings.useCustomTimeDefault) }
    var selectedPreset by remember(settings.selectedTimeControlLabel) {
        mutableStateOf(TimeControl.fromLabel(settings.selectedTimeControlLabel))
    }
    var customMinutes by remember(settings.customMinutes) { mutableIntStateOf(settings.customMinutes) }
    var customSeconds by remember(settings.customSeconds) { mutableIntStateOf(settings.customSeconds) }
    var customInc by remember(settings.customIncrementSec) { mutableIntStateOf(settings.customIncrementSec) }
    var coachDavidEnabled by remember(settings.coachDavidDefault) { mutableStateOf(settings.coachDavidDefault) }

    val activeTimeControl = remember(useCustomTime, selectedPreset, customMinutes, customSeconds, customInc) {
        if (useCustomTime) TimeControl.custom(customMinutes, customSeconds, customInc) else selectedPreset
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_chess_hero_banner),
                    contentDescription = "Grandmaster Arena Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x55070B12),
                                    Color(0xDD070B12)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable(onClick = onOpenAccountChoice)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AccountCircle,
                                    contentDescription = "Account Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${CountryFlag.emoji(profile.countryCode)} ${profile.username} · ${if (settings.accountMode == AccountMode.GUEST) "Guest" else "Account"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            color = ChampionshipGold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "Lv.$level · ${profile.rapidRating}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ChampionshipGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "GRANDMASTER",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "International Chess · Chess960 · Coach David",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onStartGame(
                                        GameSetupConfig(
                                            opponentMode = PlayOpponentMode.VS_AI,
                                            variant = GameVariant.STANDARD,
                                            timeControl = activeTimeControl,
                                            coachDavidEnabled = coachDavidEnabled
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.testTag("home_quick_play_btn")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Quick Play ${activeTimeControl.label}")
                            }
                            FilledTonalButton(
                                onClick = onOpenFischerHub,
                                modifier = Modifier.testTag("home_fischer_btn")
                            ) {
                                Text("Chess960")
                            }
                        }
                    }
                }
            }
        }

        // Custom Timing & Coach David Match Builder
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeControlPickerSection(
                        selectedPreset = selectedPreset,
                        useCustomTime = useCustomTime,
                        customMinutes = customMinutes,
                        customSeconds = customSeconds,
                        customIncrementSec = customInc,
                        onSelectPreset = { tc ->
                            useCustomTime = false
                            selectedPreset = tc
                            onSaveTimeControlPreference(false, tc.label, customMinutes, customSeconds, customInc)
                        },
                        onSelectCustomMode = {
                            useCustomTime = true
                            onSaveTimeControlPreference(true, selectedPreset.label, customMinutes, customSeconds, customInc)
                        },
                        onCustomChanged = { m, s, inc ->
                            customMinutes = m
                            customSeconds = s
                            customInc = inc
                            useCustomTime = true
                            onSaveTimeControlPreference(true, selectedPreset.label, m, s, inc)
                        }
                    )

                    CoachDavidOptionToggle(
                        enabled = coachDavidEnabled,
                        onToggle = { checked ->
                            coachDavidEnabled = checked
                            onSetCoachDavidDefault(checked)
                        }
                    )
                }
            }
        }

        // Theme, Custom Background & Compare With Others Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Themes, Backgrounds & Compare", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${settings.appTheme.displayName} · ${settings.backgroundPreset.displayName} · ${settings.boardTheme.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpenThemeStudio(0) },
                            modifier = Modifier.weight(1f).testTag("home_open_theme_studio_btn")
                        ) {
                            Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Themes & BG")
                        }
                        FilledTonalButton(
                            onClick = { onOpenThemeStudio(1) },
                            modifier = Modifier.weight(1f).testTag("home_compare_themes_btn")
                        ) {
                            Icon(Icons.Filled.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compare")
                        }
                    }
                }
            }
        }

        // Daily Login Reward
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ChampionshipGold.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ChampionshipGold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (dailyRewardBannerXp != null) "Claimed +$dailyRewardBannerXp XP!"
                                else "Daily Training Streak: ${profile.dailyLoginStreak}d",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (todayClaimed) "Today's XP bonus collected · ${profile.xp} total XP"
                                else "Tap to collect today's streak XP bonus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!todayClaimed) {
                        Button(
                            onClick = onClaimDailyReward,
                            modifier = Modifier.testTag("claim_daily_reward_btn")
                        ) {
                            Text("Claim")
                        }
                    } else if (dailyRewardBannerXp != null) {
                        FilledTonalButton(onClick = onDismissRewardBanner) {
                            Text("Nice")
                        }
                    }
                }
            }
        }

        // Play Mode Cards Grid
        item {
            Text("Arena Modes", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArenaModeTile(
                        title = "Play vs AI",
                        subtitle = "${activeTimeControl.label} · ${if (coachDavidEnabled) "Coach David On" else "8 Elo Levels"}",
                        icon = Icons.Filled.SmartToy,
                        accent = EmeraldPrimary,
                        modifier = Modifier.weight(1f).testTag("mode_vs_ai"),
                        onClick = {
                            onStartGame(
                                GameSetupConfig(
                                    opponentMode = PlayOpponentMode.VS_AI,
                                    timeControl = activeTimeControl,
                                    coachDavidEnabled = coachDavidEnabled
                                )
                            )
                        }
                    )
                    ArenaModeTile(
                        title = "Fischer 960",
                        subtitle = "All 960 Starting Grids",
                        icon = Icons.Filled.Casino,
                        accent = ChampionshipGold,
                        modifier = Modifier.weight(1f).testTag("mode_fischer960"),
                        onClick = onOpenFischerHub
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArenaModeTile(
                        title = "Bluetooth Friend",
                        subtitle = "Wireless Nearby Match",
                        icon = Icons.Filled.Bluetooth,
                        accent = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f).testTag("mode_bluetooth_friend"),
                        onClick = onOpenBluetoothLobby
                    )
                    ArenaModeTile(
                        title = "Pass & Play",
                        subtitle = "${activeTimeControl.label} · Same Device",
                        icon = Icons.Filled.Groups,
                        accent = ElectricBlue,
                        modifier = Modifier.weight(1f).testTag("mode_pass_play"),
                        onClick = {
                            onStartGame(
                                GameSetupConfig(
                                    opponentMode = PlayOpponentMode.PASS_AND_PLAY,
                                    timeControl = activeTimeControl,
                                    coachDavidEnabled = coachDavidEnabled
                                )
                            )
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArenaModeTile(
                        title = "Endgame Trainer",
                        subtitle = "K+Q, K+R & Mate Patterns",
                        icon = Icons.Filled.School,
                        accent = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f).testTag("mode_endgame_trainer"),
                        onClick = onOpenEndgameTrainer
                    )
                    ArenaModeTile(
                        title = "Spectate Clash",
                        subtitle = "Watch 2250 AI Engines · ${BoardTheme.entries.size} Boards & ${PieceStyle.entries.size} Pieces",
                        icon = Icons.Filled.Visibility,
                        accent = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f).testTag("mode_spectate"),
                        onClick = {
                            onStartGame(
                                GameSetupConfig(
                                    opponentMode = PlayOpponentMode.SPECTATOR,
                                    timeControl = activeTimeControl,
                                    coachDavidEnabled = coachDavidEnabled
                                )
                            )
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArenaModeTile(
                        title = "Blind Audio Chess",
                        subtitle = "Voice, Haptics & TalkBack",
                        icon = Icons.Filled.AccessibilityNew,
                        accent = Color(0xFF22D3EE),
                        modifier = Modifier.weight(1f).testTag("mode_blind_chess"),
                        onClick = {
                            onStartGame(
                                GameSetupConfig(
                                    opponentMode = PlayOpponentMode.VS_AI,
                                    timeControl = activeTimeControl,
                                    coachDavidEnabled = true,
                                    isBlindMode = true
                                )
                            )
                        }
                    )
                    ArenaModeTile(
                        title = "Logical Graphs",
                        subtitle = "Radar, Outcomes & Flow",
                        icon = Icons.Filled.Hub,
                        accent = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f).testTag("mode_logical_graphs"),
                        onClick = onOpenLogicalGraphs
                    )
                }
            }
        }

        // Daily Chess Puzzle & Streak Card
        item {
            val todayEpoch = LocalDate.now().toEpochDay()
            val dailyStreakDays = DailyPuzzleStreakLogic.effectiveStreak(
                storedStreak = settings.dailyPuzzleStreak,
                lastEpochDay = settings.lastDailyPuzzleEpochDay,
                todayEpochDay = todayEpoch
            )
            val solvedDailyToday = DailyPuzzleStreakLogic.isSolvedToday(
                lastEpochDay = settings.lastDailyPuzzleEpochDay,
                completedEpochDays = settings.dailyPuzzleCompletedEpochDays,
                todayEpochDay = todayEpoch
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDailyPuzzle)
                    .testTag("home_daily_puzzle_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Extension, contentDescription = null, tint = EmeraldPrimary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DAILY PUZZLE · ${dailyPuzzle.difficulty.label.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = dailyPuzzle.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${dailyPuzzle.theme} · Rated ${dailyPuzzle.rating} · ${if (solvedDailyToday) "Cleared Today ✓" else "Daily Streak ${dailyStreakDays}d"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Daily Puzzle")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onOpenDailyPuzzle,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (solvedDailyToday) "Replay Daily" else "Solve Daily")
                        }
                        Button(
                            onClick = onOpenDailyStreak,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                            modifier = Modifier.weight(1f).testTag("home_daily_streak_btn")
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Daily Streak (${dailyStreakDays}d)")
                        }
                    }
                }
            }
        }

        // Announcements & Local Club Notice
        if (announcements.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Platform Bulletins", style = MaterialTheme.typography.titleLarge)
                    FilledTonalButton(onClick = onOpenCompete) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tournaments")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    announcements.take(2).forEach { ann ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = ChampionshipGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(ann.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        ann.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Matches & Full History Shortcut
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Matches (${recentMatches.size})", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.testTag("home_view_all_history_btn")
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("All History")
                }
            }
            Spacer(Modifier.height(6.dp))
            if (recentMatches.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "No matches completed yet. Every finished or saved game is automatically stored in your permanent PGN Match History.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentMatches.take(5).forEach { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMatchAnalysis(m.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${m.whiteName} vs ${m.blackName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${m.variant} · ${m.timeControlLabel} · ${m.openingName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when (m.result) {
                                            "WHITE_WIN" -> "1-0"
                                            "BLACK_WIN" -> "0-1"
                                            "DRAW" -> "½-½"
                                            else -> "*"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArenaModeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
