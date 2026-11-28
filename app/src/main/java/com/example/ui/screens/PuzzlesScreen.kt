package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PuzzleDifficulty
import com.example.chess.engine.PuzzleLibrary
import com.example.data.local.ClubMemberEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChessBoardView
import com.example.ui.components.CountryFlag
import com.example.ui.components.DailyPuzzleStreakCard
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.PuzzleStatus
import com.example.ui.viewmodel.PuzzleViewModel

@Composable
fun PuzzlesScreen(
    puzzleViewModel: PuzzleViewModel,
    profile: PlayerProfileEntity,
    attempts: List<PuzzleAttemptEntity>,
    clubMembers: List<ClubMemberEntity>,
    settings: UserSettings,
    onOpenEndgameTrainer: () -> Unit = {}
) {
    val state by puzzleViewModel.state.collectAsStateWithLifecycle()
    var subTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(settings.soundEnabled, settings.soundVolume, settings.soundPack) {
        puzzleViewModel.syncSound(
            enabled = settings.soundEnabled,
            volume = settings.soundVolume,
            pack = settings.soundPack
        )
    }

    val accuracyPct = if (profile.puzzlesAttempted == 0) 100.0
    else (profile.puzzlesSolved * 100.0) / profile.puzzlesAttempted

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Tactics") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("History") })
            Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Leaderboard") })
        }

        when (subTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("puzzles_tactics_list"),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Puzzle Streak Card & Rating Stats
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DailyPuzzleStreakCard(
                                settings = settings,
                                isDailyStreakActive = state.isDailyStreakMode,
                                streakRunCurrent = state.streakRunCurrent,
                                onOpenTodaysDaily = puzzleViewModel::loadDailyPuzzle,
                                onStartDailyStreakRun = puzzleViewModel::startDailyStreakRun
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Puzzle Rating: ${profile.puzzleRating}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ChampionshipGold
                                    )
                                    Text(
                                        "Tactics Streak ${profile.puzzleStreak} · Acc ${"%.0f".format(accuracyPct)}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Specialized Endgame Trainer Shortcut Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ChampionshipGold.copy(alpha = 0.14f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenEndgameTrainer)
                                .testTag("puzzles_open_endgame_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.School, contentDescription = null, tint = ChampionshipGold)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Endgame Trainer", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
                                        Text(
                                            "Practice K+Q vs K, K+R vs K & 12 Checkmate Drills",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open Endgame Trainer", tint = ChampionshipGold)
                            }
                        }
                    }

                    // Difficulty Filter
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = state.selectedDifficultyFilter == null,
                                    onClick = { puzzleViewModel.setDifficultyFilter(null) },
                                    label = { Text("All Levels") }
                                )
                            }
                            items(PuzzleDifficulty.entries) { diff ->
                                FilterChip(
                                    selected = state.selectedDifficultyFilter == diff,
                                    onClick = { puzzleViewModel.setDifficultyFilter(diff) },
                                    label = { Text(diff.label) }
                                )
                            }
                        }
                    }

                    // Active Daily Streak Run Banner
                    if (state.isDailyStreakMode) {
                        item {
                            Surface(
                                color = if (state.streakRunEnded) CrimsonRed.copy(alpha = 0.16f)
                                else Color(0xFFF97316).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("daily_streak_active_banner")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = if (state.streakRunEnded) CrimsonRed else Color(0xFFF97316)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (state.streakRunEnded) {
                                                    "Streak Run Ended · Final Score: ${state.streakRunCurrent}"
                                                } else {
                                                    "Daily Streak Run · Score: ${state.streakRunCurrent}"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (state.streakRunEnded) CrimsonRed else Color(0xFFF97316)
                                            )
                                            Text(
                                                text = if (state.streakRunEnded) {
                                                    "One mistake ends the run. Tap Restart Streak to climb higher!"
                                                } else {
                                                    "Puzzle ${state.streakRunIndex + 1} of ${state.streakRunTotal.coerceAtLeast(1)} · No-mistake ladder"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Puzzle Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when {
                                        state.isDailyStreakMode -> "DAILY STREAK · ${state.currentPuzzle.title}"
                                        state.isDailyChallenge -> "DAILY CHALLENGE · ${state.currentPuzzle.title}"
                                        else -> state.currentPuzzle.title
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "${state.currentPuzzle.theme} · ${state.currentPuzzle.difficulty.label} (${state.currentPuzzle.rating}) · ${if (state.playerSide == PieceColor.WHITE) "White to Move" else "Black to Move"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Board
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            ChessBoardView(
                                board = state.board,
                                boardTheme = settings.boardTheme,
                                pieceStyle = settings.pieceStyle,
                                flipped = state.playerSide == PieceColor.BLACK,
                                showCoordinates = settings.showCoordinates,
                                selectedSquare = state.selectedSquare,
                                legalMovesForSelected = state.legalMovesForSelected,
                                lastMoveFrom = state.lastMoveFrom,
                                lastMoveTo = state.lastMoveTo,
                                hintFrom = state.hintFrom,
                                hintTo = state.hintTo,
                                interactive = state.status != PuzzleStatus.SOLVED,
                                onSquareTapped = puzzleViewModel::onSquareTapped,
                                modifier = Modifier
                                    .fillMaxWidth(settings.boardScale)
                                    .widthIn(max = 480.dp)
                            )
                        }
                    }

                    // Status & Actions
                    item {
                        if (state.status == PuzzleStatus.WRONG_TRY) {
                            Surface(
                                color = CrimsonRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Not the strongest move — look for forcing checks, captures, or threats!",
                                    color = CrimsonRed,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (state.showHintText && state.status != PuzzleStatus.SOLVED) {
                            Surface(
                                color = ChampionshipGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hint: ${state.currentPuzzle.hintText}",
                                    color = ChampionshipGold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (state.status == PuzzleStatus.SOLVED) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.16f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                                        Spacer(Modifier.width(8.dp))
                                        val deltaText = state.ratingDelta?.let { if (it >= 0) "+$it" else "$it" } ?: ""
                                        Text(
                                            text = "Puzzle Solved! $deltaText Rating",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = EmeraldPrimary
                                        )
                                    }
                                    Text(
                                        text = state.currentPuzzle.explanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = puzzleViewModel::requestHint,
                                enabled = state.status != PuzzleStatus.SOLVED && !state.streakRunEnded,
                                modifier = Modifier.weight(1f).testTag("puzzle_hint_btn")
                            ) {
                                Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hint")
                            }
                            FilledTonalButton(
                                onClick = puzzleViewModel::retryPuzzle,
                                modifier = Modifier.weight(1f).testTag("puzzle_retry_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (state.isDailyStreakMode) "Restart" else "Retry")
                            }
                            Button(
                                onClick = puzzleViewModel::nextTrainingPuzzle,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f).testTag("puzzle_next_btn")
                            ) {
                                Text(
                                    when {
                                        state.isDailyStreakMode && state.streakRunEnded -> "New Run"
                                        state.isDailyStreakMode -> "Next Streak"
                                        else -> "Next"
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Tactical Puzzle Catalog Quick Pick
                    item {
                        Text("Training Catalog (${PuzzleLibrary.ALL_PUZZLES.size} Puzzles)", style = MaterialTheme.typography.titleMedium)
                    }
                    items(PuzzleLibrary.ALL_PUZZLES) { p ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { puzzleViewModel.loadPuzzle(p, isDaily = false) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${p.difficulty.label} · ${p.theme}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${p.rating}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = ChampionshipGold
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (attempts.isEmpty()) {
                        item {
                            Text(
                                "Solve or attempt puzzles to record your accuracy and rating history.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(attempts) { att ->
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
                                    Text(att.puzzleTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${att.difficulty} · ${att.dateKey}" + if (att.isDailyChallenge) " · Daily" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val delta = att.ratingAfter - att.ratingBefore
                                Text(
                                    text = (if (att.solved) "Solved " else "Failed ") + (if (delta >= 0) "(+$delta)" else "($delta)"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (att.solved) EmeraldPrimary else CrimsonRed
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                val ranked = remember(clubMembers, profile.puzzleRating) {
                    (clubMembers.filter { !it.isBanned }.map {
                        Triple("${CountryFlag.emoji(it.countryCode)} ${it.username}", it.puzzleRating, "+${it.weeklyGain}")
                    } + Triple("${CountryFlag.emoji(profile.countryCode)} ${profile.username} (You)", profile.puzzleRating, "+${profile.puzzleStreak * 4}"))
                        .sortedByDescending { it.second }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ChampionshipGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Weekly & Monthly Puzzle Leaderboard", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    itemsIndexed(ranked) { idx, (name, rating, gain) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${idx + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(32.dp))
                                    Text(name, style = MaterialTheme.typography.titleMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(gain, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                                    Text(
                                        "$rating",
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = ChampionshipGold
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
