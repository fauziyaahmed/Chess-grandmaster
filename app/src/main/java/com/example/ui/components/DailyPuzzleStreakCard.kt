package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.engine.DailyPuzzleStreakLogic
import com.example.data.preferences.UserSettings
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.EmeraldPrimary
import java.time.LocalDate

@Composable
fun DailyPuzzleStreakCard(
    settings: UserSettings,
    isDailyStreakActive: Boolean,
    streakRunCurrent: Int,
    onOpenTodaysDaily: () -> Unit,
    onStartDailyStreakRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val todayEpoch = today.toEpochDay()
    val effectiveStreak = DailyPuzzleStreakLogic.effectiveStreak(
        storedStreak = settings.dailyPuzzleStreak,
        lastEpochDay = settings.lastDailyPuzzleEpochDay,
        todayEpochDay = todayEpoch
    )
    val solvedToday = DailyPuzzleStreakLogic.isSolvedToday(
        lastEpochDay = settings.lastDailyPuzzleEpochDay,
        completedEpochDays = settings.dailyPuzzleCompletedEpochDays,
        todayEpochDay = todayEpoch
    )
    val weekDots = remember(settings.dailyPuzzleCompletedEpochDays, settings.lastDailyPuzzleEpochDay, todayEpoch) {
        DailyPuzzleStreakLogic.recentSevenDayDots(
            completedEpochDays = settings.dailyPuzzleCompletedEpochDays,
            lastEpochDay = settings.lastDailyPuzzleEpochDay,
            today = today
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_puzzle_streak_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF97316).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = "Daily Puzzle Streak",
                            tint = Color(0xFFF97316)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (effectiveStreak > 0) "$effectiveStreak-Day Daily Streak"
                            else "Daily Puzzle Streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (solvedToday) {
                                "Today's daily cleared! Best: ${settings.bestDailyPuzzleStreak}d · Best Run: ${settings.bestDailyStreakRun}"
                            } else {
                                "Solve today's puzzle or launch a Daily Streak run"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = if (solvedToday) EmeraldPrimary.copy(alpha = 0.16f) else ChampionshipGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (solvedToday) "Solved ✓" else "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (solvedToday) EmeraldPrimary else ChampionshipGold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            // 7-day calendar completion strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_streak_7day_strip"),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDots.forEach { dot ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        dot.isCompleted -> Color(0xFFF97316)
                                        dot.isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                )
                                .then(
                                    if (dot.isToday && !dot.isCompleted) {
                                        Modifier.border(1.5.dp, ChampionshipGold, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dot.isCompleted) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = dot.shortDayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dot.isToday) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (dot.isToday) "Today" else dot.shortDayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dot.isToday) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenTodaysDaily,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("puzzle_daily_btn")
                ) {
                    Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (solvedToday) "Daily Replay" else "Today's Daily")
                }
                Button(
                    onClick = onStartDailyStreakRun,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("puzzle_daily_streak_btn")
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isDailyStreakActive) "Streak: $streakRunCurrent" else "Daily Streak",
                        maxLines = 1
                    )
                }
            }
        }
    }
}
