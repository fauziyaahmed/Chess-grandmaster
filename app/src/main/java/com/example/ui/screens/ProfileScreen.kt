package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.engine.GameVariant
import com.example.chess.engine.LogicalAnalytics
import com.example.chess.engine.Piece
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PieceType
import com.example.data.AchievementItem
import com.example.data.RatingCalculator
import com.example.data.SoundEffect
import com.example.data.SoundPack
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.local.RatingSnapshotEntity
import com.example.data.preferences.AccountMode
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChessPieceIcon
import com.example.ui.components.CountryFlag
import com.example.ui.components.LogicalColorOutcomeCard
import com.example.ui.components.LogicalRatingProgressionCard
import com.example.ui.components.LogicalSkillRadarCard
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.ArenaBackgroundPreset
import com.example.ui.theme.BoardPatternStyle
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceStyle
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    mainViewModel: MainViewModel,
    profile: PlayerProfileEntity,
    matches: List<MatchHistoryEntity>,
    ratingSnapshots: List<RatingSnapshotEntity>,
    achievements: List<AchievementItem>,
    settings: UserSettings,
    onOpenMatchAnalysis: (Long) -> Unit,
    onOpenThemeStudio: (Int) -> Unit = {},
    onPreviewSound: (SoundEffect) -> Unit = {},
    puzzleAttempts: List<PuzzleAttemptEntity> = emptyList()
) {
    val tab by mainViewModel.profileTabIndex.collectAsStateWithLifecycle()
    var showEditProfile by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { mainViewModel.openProfileTab(0) }, text = { Text("Dashboard") })
            Tab(
                selected = tab == 1,
                onClick = { mainViewModel.openProfileTab(1) },
                text = { Text("History (${matches.size})") }
            )
            Tab(selected = tab == 2, onClick = { mainViewModel.openProfileTab(2) }, text = { Text("Settings") })
        }

        when (tab) {
            0 -> DashboardTab(
                profile = profile,
                settings = settings,
                matches = matches,
                puzzleAttempts = puzzleAttempts,
                ratingSnapshots = ratingSnapshots,
                achievements = achievements,
                onEditProfile = { showEditProfile = true },
                onSwitchAccount = { mainViewModel.reopenAccountChoice() }
            )
            1 -> MatchHistoryTab(
                matches = matches,
                onOpenMatchAnalysis = onOpenMatchAnalysis
            )
            else -> SettingsTab(
                mainViewModel = mainViewModel,
                settings = settings,
                onOpenThemeStudio = onOpenThemeStudio,
                onPreviewSound = onPreviewSound
            )
        }
    }

    if (showEditProfile) {
        var name by remember { mutableStateOf(profile.username) }
        var country by remember { mutableStateOf(profile.countryCode) }
        var bio by remember { mutableStateOf(profile.bio) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Customize Player Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })
                    Text("Country Flag", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CountryFlag.SUPPORTED_COUNTRIES) { (code, _) ->
                            FilterChip(
                                selected = country == code,
                                onClick = { country = code },
                                label = { Text("${CountryFlag.emoji(code)} $code") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    mainViewModel.updateProfile(name, country, bio)
                    showEditProfile = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DashboardTab(
    profile: PlayerProfileEntity,
    settings: UserSettings,
    matches: List<MatchHistoryEntity>,
    puzzleAttempts: List<PuzzleAttemptEntity>,
    ratingSnapshots: List<RatingSnapshotEntity>,
    achievements: List<AchievementItem>,
    onEditProfile: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    var selectedChartFormat by remember { mutableStateOf("RAPID") }
    val level = RatingCalculator.levelForXp(profile.xp)
    val (xpInLevel, xpReq) = RatingCalculator.xpProgress(profile.xp)
    val title = RatingCalculator.chessTitleForPeak(profile.highestRating)

    val chartValues = remember(ratingSnapshots, selectedChartFormat) {
        ratingSnapshots.filter { it.formatKey == selectedChartFormat }.map { it.rating }
    }
    val analytics = remember(profile, matches, puzzleAttempts, settings) {
        LogicalAnalytics.computePlayerAnalytics(profile, matches, puzzleAttempts, settings)
    }
    val avgDurationSec = if (profile.totalMatches == 0) 0L else profile.totalPlayTimeSeconds / profile.totalMatches

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Player Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = CountryFlag.emoji(profile.countryCode),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(profile.username, style = MaterialTheme.typography.titleLarge)
                                    Surface(color = ChampionshipGold.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                        Text(title, style = MaterialTheme.typography.labelSmall, color = ChampionshipGold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(profile.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = onEditProfile, modifier = Modifier.testTag("edit_profile_btn")) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                        }
                    }

                    Text("Level $level · $xpInLevel / $xpReq XP", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                    LinearProgressIndicator(
                        progress = { (xpInLevel.toFloat() / xpReq.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Account Session Card (Guest / Google Sign-In options)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Account Status", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = settings.accountMode.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onSwitchAccount,
                        modifier = Modifier.testTag("profile_account_options_btn")
                    ) {
                        Text("Options")
                    }
                }
            }
        }

        // Complete Career Statistics
        item {
            Text("Career Statistics", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricBlock("Matches", "${profile.totalMatches}")
                        MetricBlock("W / L / D", "${profile.totalWins}/${profile.totalLosses}/${profile.totalDraws}")
                        MetricBlock("Win Rate", "${"%.1f".format(profile.overallWinRate)}%")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricBlock("Peak Rating", "${profile.highestRating}")
                        MetricBlock("Win Streak", "${profile.currentWinStreak} (Best ${profile.longestWinStreak})")
                        MetricBlock("Avg / Total", "${CountryFlag.formatDuration(avgDurationSec)} / ${profile.totalPlayTimeSeconds / 60}m")
                    }
                }
            }
        }

        // Format Ratings Grid
        item {
            Text("Ratings by Format", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatStatRow("Bullet", profile.bulletRating, profile.bulletPeak, profile.bulletWins, profile.bulletLosses, profile.bulletDraws)
                FormatStatRow("Blitz", profile.blitzRating, profile.blitzPeak, profile.blitzWins, profile.blitzLosses, profile.blitzDraws)
                FormatStatRow("Rapid", profile.rapidRating, profile.rapidPeak, profile.rapidWins, profile.rapidLosses, profile.rapidDraws)
                FormatStatRow("Classical", profile.classicalRating, profile.classicalPeak, profile.classicalWins, profile.classicalLosses, profile.classicalDraws)
                FormatStatRow("Fischer960", profile.fischerRating, profile.fischerPeak, profile.fischerWins, profile.fischerLosses, profile.fischerDraws)
                FormatStatRow("Puzzles", profile.puzzleRating, profile.puzzlePeak, profile.puzzlesSolved, (profile.puzzlesAttempted - profile.puzzlesSolved).coerceAtLeast(0), 0)
            }
        }

        // Rating History Graph
        item {
            Text("Rating Progression", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("BULLET", "BLITZ", "RAPID", "CLASSICAL", "FISCHER960", "PUZZLE")) { fmt ->
                    FilterChip(
                        selected = selectedChartFormat == fmt,
                        onClick = { selectedChartFormat = fmt },
                        label = { Text(fmt.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LogicalRatingProgressionCard(
                formatLabel = selectedChartFormat,
                values = chartValues
            )
        }

        // Logical Chess Intelligence Graphs
        item {
            Text("Logical Performance Graphs", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LogicalSkillRadarCard(analytics = analytics)
                LogicalColorOutcomeCard(analytics = analytics)
            }
        }

        // Achievements
        item {
            Text("Skill Achievements (${achievements.count { it.unlocked }}/${achievements.size})", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                achievements.forEach { ach ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (ach.unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (ach.unlocked) Icons.Filled.Verified else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (ach.unlocked) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(ach.title, style = MaterialTheme.typography.titleMedium)
                                    Text(ach.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(ach.progressText, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormatStatRow(name: String, rating: Int, peak: Int, w: Int, l: Int, d: Int) {
    val total = w + l + d
    val prov = if (RatingCalculator.isProvisional(total)) "?" else ""
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("Peak $peak · W:$w L:$l D:$d", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "$rating$prov",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = EmeraldPrimary
            )
        }
    }
}

@Composable
private fun MatchHistoryTab(
    matches: List<MatchHistoryEntity>,
    onOpenMatchAnalysis: (Long) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var selectedMatch by remember { mutableStateOf<MatchHistoryEntity?>(null) }

    val filtered = remember(matches, query, filter) {
        matches.filter { m ->
            val matchesQuery = query.isBlank() ||
                m.whiteName.contains(query, ignoreCase = true) ||
                m.blackName.contains(query, ignoreCase = true) ||
                m.openingName.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "STANDARD" -> m.variant == GameVariant.STANDARD.name
                "FISCHER960" -> m.variant == GameVariant.FISCHER960.name
                "WINS" -> (m.playerSide == "WHITE" && m.result == "WHITE_WIN") || (m.playerSide == "BLACK" && m.result == "BLACK_WIN")
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${matches.size} games permanently saved in your PGN archive",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search opponent or opening") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All", "STANDARD" to "Standard", "FISCHER960" to "Chess960", "WINS" to "Wins").forEach { (k, lbl) ->
                    FilterChip(selected = filter == k, onClick = { filter = k }, label = { Text(lbl) })
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    "No matching games found in your local PGN archive.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(filtered) { m ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { selectedMatch = m }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${m.whiteName} (${m.whiteRatingBefore}→${m.whiteRatingAfter}) vs ${m.blackName} (${m.blackRatingBefore}→${m.blackRatingAfter})",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (m.result) {
                                "WHITE_WIN" -> "1-0"
                                "BLACK_WIN" -> "0-1"
                                "DRAW" -> "½-½"
                                else -> "*"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampionshipGold
                        )
                    }
                    Text(
                        "${m.variant} · ${m.timeControlLabel} · ${m.moveCount} moves · ${CountryFlag.formatDuration(m.durationSeconds)} · ${m.termination}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${m.openingEco} ${m.openingName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }

    selectedMatch?.let { m ->
        AlertDialog(
            onDismissRequest = { selectedMatch = null },
            title = { Text("${m.whiteName} vs ${m.blackName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Result: ${m.result} (${m.termination})")
                    Text("Format: ${m.variant} · ${m.timeControlLabel} · ${m.moveCount} moves")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = m.pgnText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedMatch = null
                    onOpenMatchAnalysis(m.id)
                }) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Replay & Analyze")
                }
            },
            dismissButton = {
                Row {
                    IconButton(onClick = { clipboard.setText(AnnotatedString(m.pgnText)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy PGN")
                    }
                    IconButton(onClick = { shareText(context, m.pgnText) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share PGN")
                    }
                    TextButton(onClick = { selectedMatch = null }) { Text("Close") }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsTab(
    mainViewModel: MainViewModel,
    settings: UserSettings,
    onOpenThemeStudio: (Int) -> Unit,
    onPreviewSound: (SoundEffect) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account & Coaching Defaults", style = MaterialTheme.typography.titleLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Session Mode", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                settings.accountMode.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = { mainViewModel.reopenAccountChoice() }) {
                            Text("Google / Guest")
                        }
                    }
                    SettingSwitchRow("Train with Coach David by Default", settings.coachDavidDefault) {
                        mainViewModel.setCoachDavidDefault(it)
                    }
                    SettingSwitchRow("Coach David Voice (Speak Advice Aloud)", settings.coachVoiceEnabled) {
                        mainViewModel.setCoachVoiceEnabled(it)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Themes, Backgrounds & Compare", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Pick from ${AppThemeOption.entries.size} app themes, ${ArenaBackgroundPreset.entries.size} arena backgrounds (including custom photo & gradient), or compare setups side-by-side.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenThemeStudio(0) },
                            modifier = Modifier.weight(1f).testTag("profile_open_theme_studio_btn")
                        ) {
                            Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Studio & Custom BG")
                        }
                        FilledTonalButton(
                            onClick = { onOpenThemeStudio(1) },
                            modifier = Modifier.weight(1f).testTag("profile_compare_themes_btn")
                        ) {
                            Icon(Icons.Filled.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compare")
                        }
                    }

                    Text("App Theme", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppThemeOption.entries) { appTheme ->
                            FilterChip(
                                selected = settings.appTheme == appTheme,
                                onClick = { mainViewModel.selectAppTheme(appTheme) },
                                label = { Text(appTheme.displayName) },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(appTheme.primaryPreview)
                                    )
                                }
                            )
                        }
                    }

                    Text("Arena Background", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ArenaBackgroundPreset.entries) { preset ->
                            FilterChip(
                                selected = settings.backgroundPreset == preset,
                                onClick = {
                                    if (preset == ArenaBackgroundPreset.CUSTOM_IMAGE && settings.customBackgroundImagePath.isNullOrBlank()) {
                                        onOpenThemeStudio(0)
                                    } else {
                                        mainViewModel.selectBackgroundPreset(preset)
                                    }
                                },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }

                    SettingSwitchRow("Show Board Coordinates", settings.showCoordinates) {
                        mainViewModel.updateSettings { s -> s.copy(showCoordinates = it) }
                    }
                    SettingSwitchRow("Sound Effects", settings.soundEnabled) {
                        mainViewModel.updateSettings { s -> s.copy(soundEnabled = it) }
                    }
                    if (settings.soundEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sound Pack · ${settings.soundPack.displayName}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = settings.soundPack.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SoundPack.entries) { pack ->
                                    FilterChip(
                                        selected = settings.soundPack == pack,
                                        onClick = { mainViewModel.selectSoundPack(pack) },
                                        label = { Text(pack.displayName) },
                                        modifier = Modifier.testTag("sound_pack_${pack.name}")
                                    )
                                }
                            }
                            Text(
                                text = "Master Volume: ${(settings.soundVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = settings.soundVolume,
                                onValueChange = { v -> mainViewModel.updateSoundVolume(v) },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.testTag("settings_sound_volume_slider")
                            )
                            Text(
                                text = "Preview Chess Audio Clips",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    SoundEffect.MOVE to "Move",
                                    SoundEffect.CAPTURE to "Capture",
                                    SoundEffect.CASTLE to "Castle",
                                    SoundEffect.CHECK to "Check",
                                    SoundEffect.CHECKMATE to "Checkmate"
                                ).forEach { (eff, label) ->
                                    AssistChip(
                                        onClick = { onPreviewSound(eff) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("preview_sound_${eff.name}")
                                    )
                                }
                            }
                        }
                    }
                    SettingSwitchRow("Move Animations", settings.animateMoves) {
                        mainViewModel.updateSettings { s -> s.copy(animateMoves = it) }
                    }
                    SettingSwitchRow("Auto-Promote to Queen", settings.autoPromoteQueen) {
                        mainViewModel.updateSettings { s -> s.copy(autoPromoteQueen = it) }
                    }
                    Text("Board Size: ${(settings.boardScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.boardScale,
                        onValueChange = { v -> mainViewModel.updateSettings { s -> s.copy(boardScale = v) } },
                        valueRange = 0.85f..1.0f
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Board Theme (${BoardTheme.entries.size} Combinations)", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BoardTheme.entries) { bt ->
                            FilterChip(
                                selected = settings.boardTheme == bt,
                                onClick = { mainViewModel.updateSettings { s -> s.copy(boardTheme = bt) } },
                                label = { Text(bt.displayName) },
                                leadingIcon = {
                                    Row {
                                        Box(Modifier.size(10.dp).background(bt.lightSquare))
                                        Box(Modifier.size(10.dp).background(bt.darkSquare))
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Board Frame Pattern", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BoardPatternStyle.entries) { pat ->
                            FilterChip(
                                selected = settings.boardPattern == pat,
                                onClick = { mainViewModel.selectBoardPattern(pat) },
                                label = { Text(pat.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("Piece Style (${PieceStyle.entries.size} Sets)", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PieceStyle.entries) { ps ->
                            FilterChip(
                                selected = settings.pieceStyle == ps,
                                onClick = { mainViewModel.updateSettings { s -> s.copy(pieceStyle = ps) } },
                                label = { Text(ps.displayName) },
                                leadingIcon = {
                                    ChessPieceIcon(
                                        piece = Piece(PieceColor.WHITE, PieceType.QUEEN),
                                        style = ps,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("International & Privacy", style = MaterialTheme.typography.titleLarge)
                    SettingSwitchRow("Hide Online Status", settings.hideOnlineStatus) {
                        mainViewModel.updateSettings { s -> s.copy(hideOnlineStatus = it) }
                    }
                    SettingSwitchRow("Safe Chat Filter", settings.safeChatFilter) {
                        mainViewModel.updateSettings { s -> s.copy(safeChatFilter = it) }
                    }
                    Text("Interface Language", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("English (International)", "Español", "Français", "Deutsch", "日本語")) { lang ->
                            FilterChip(
                                selected = settings.languageLabel == lang,
                                onClick = { mainViewModel.updateSettings { s -> s.copy(languageLabel = lang) } },
                                label = { Text(lang) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
