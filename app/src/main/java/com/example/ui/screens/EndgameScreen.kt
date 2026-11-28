package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.engine.EndgameCategory
import com.example.chess.engine.EndgameDifficulty
import com.example.chess.engine.EndgameLibrary
import com.example.chess.engine.PieceColor
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChessBoardView
import com.example.ui.components.InteractiveMoveHistoryCard
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EndgameStatus
import com.example.ui.viewmodel.EndgameViewModel

@Composable
fun EndgameScreen(
    endgameViewModel: EndgameViewModel,
    settings: UserSettings
) {
    val state by endgameViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(
        settings.soundEnabled,
        settings.soundVolume,
        settings.soundPack,
        settings.autoPromoteQueen,
        settings.coachVoiceEnabled
    ) {
        endgameViewModel.syncPreferences(
            sound = settings.soundEnabled,
            autoQueen = settings.autoPromoteQueen,
            coachVoice = settings.coachVoiceEnabled,
            soundVolume = settings.soundVolume,
            soundPack = settings.soundPack
        )
    }

    val filteredDrills = EndgameLibrary.drillsForCategory(state.selectedCategory)
    val totalPossibleStars = EndgameLibrary.ALL_DRILLS.size * 3
    val earnedTotalStars = settings.endgameProgress.values.sumOf { it.stars }
    val masteredCount = settings.endgameProgress.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("endgame_trainer_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mastery Summary Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ChampionshipGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.School, contentDescription = null, tint = ChampionshipGold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Endgame Mastery", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$masteredCount/${EndgameLibrary.ALL_DRILLS.size} Patterns Solved · Score $earnedTotalStars/$totalPossibleStars ★",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        color = if (state.mateInMoves != null) EmeraldPrimary.copy(alpha = 0.18f) else ChampionshipGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("endgame_eval_pill")
                    ) {
                        Text(
                            text = state.mateInMoves?.let { "Mate in $it" } ?: state.evalLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = if (state.mateInMoves != null) EmeraldPrimary else ChampionshipGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("endgame_category_row")
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { endgameViewModel.selectCategory(null) },
                        label = { Text("All Drills") },
                        modifier = Modifier.testTag("endgame_cat_all")
                    )
                }
                items(EndgameCategory.entries) { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { endgameViewModel.selectCategory(cat) },
                        label = { Text(cat.label) },
                        modifier = Modifier.testTag("endgame_cat_${cat.name}")
                    )
                }
            }
        }

        // Current Drill Title & Moves/Par Info
        item {
            val drill = state.currentDrill
            val prevRecord = settings.endgameProgress[drill.id]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ElectricBlue.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = drill.materialCode,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = difficultyAccent(drill.difficulty).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = drill.difficulty.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = difficultyAccent(drill.difficulty),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = drill.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${if (state.playerSide == PieceColor.WHITE) "White" else "Black"} to force checkmate · ${drill.techniqueName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Moves ${state.movesUsed} / Par ${drill.parMoves}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (state.movesUsed <= drill.parMoves) EmeraldPrimary else ChampionshipGold,
                        modifier = Modifier.testTag("endgame_moves_counter")
                    )
                    StarRow(stars = prevRecord?.stars ?: 0)
                }
            }
        }

        // Interactive Endgame Chessboard
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ChessBoardView(
                    board = state.displayedBoard,
                    boardTheme = settings.boardTheme,
                    pieceStyle = settings.pieceStyle,
                    flipped = state.playerSide == PieceColor.BLACK,
                    showCoordinates = settings.showCoordinates,
                    selectedSquare = if (state.isReviewingHistory) null else state.selectedSquare,
                    legalMovesForSelected = if (state.isReviewingHistory) emptyList() else state.legalMovesForSelected,
                    lastMoveFrom = state.displayedLastMoveFrom,
                    lastMoveTo = state.displayedLastMoveTo,
                    hintFrom = if (state.isReviewingHistory) null else state.hintFrom,
                    hintTo = if (state.isReviewingHistory) null else state.hintTo,
                    bestMoveFrom = if (state.isReviewingHistory) null else state.hintFrom,
                    bestMoveTo = if (state.isReviewingHistory) null else state.hintTo,
                    controlledSquares = if (!state.isReviewingHistory && state.showControlOverlay) state.controlledSquares else emptySet(),
                    boardPattern = settings.boardPattern,
                    interactive = state.status == EndgameStatus.PLAYING && !state.isDefenderThinking,
                    onSquareTapped = endgameViewModel::onSquareTapped,
                    modifier = Modifier
                        .fillMaxWidth(settings.boardScale)
                        .widthIn(max = 480.dp)
                )
            }
        }

        // Interactive Move History List
        item {
            InteractiveMoveHistoryCard(
                sanMoves = state.sanHistory,
                displayedPly = state.displayedPly,
                onJumpToPly = endgameViewModel::jumpToMovePly,
                startingSide = state.boardHistory.firstOrNull()?.sideToMove ?: PieceColor.WHITE,
                emptyHint = "Tap a piece to begin the endgame drill"
            )
        }

        // Outcome Banners (Win / Stalemate / Draw)
        if (state.status == EndgameStatus.CHECKMATED_WIN) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.16f)),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("endgame_win_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Checkmate Delivered!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldPrimary
                                )
                            }
                            StarRow(stars = state.earnedStars)
                        }
                        Text(
                            text = state.coachMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = endgameViewModel::retryDrill,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replay")
                            }
                            Button(
                                onClick = endgameViewModel::nextDrill,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("endgame_win_next_btn")
                            ) {
                                Text("Next Drill")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else if (state.status != EndgameStatus.PLAYING) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.16f)),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("endgame_fail_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = CrimsonRed)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (state.status == EndgameStatus.STALEMATE_FAIL) {
                                    "Stalemate — Careful!"
                                } else {
                                    "Draw — ${state.terminationReason.label}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = CrimsonRed
                            )
                        }
                        Text(
                            text = state.coachMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = endgameViewModel::undoLastTurn,
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("endgame_undo_fail_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Undo Move")
                            }
                            OutlinedButton(
                                onClick = endgameViewModel::retryDrill,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Restart")
                            }
                        }
                    }
                }
            }
        }

        // Training Controls Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = endgameViewModel::requestHint,
                        enabled = state.status == EndgameStatus.PLAYING && !state.isDefenderThinking && !state.isHintAnalyzing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("endgame_hint_btn")
                    ) {
                        if (state.isHintAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Hint")
                    }
                    FilledTonalButton(
                        onClick = endgameViewModel::toggleControlOverlay,
                        colors = if (state.showControlOverlay) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = ChampionshipGold.copy(alpha = 0.24f),
                                contentColor = ChampionshipGold
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("endgame_box_toggle_btn")
                    ) {
                        Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.showControlOverlay) "Box On" else "Box Off")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = endgameViewModel::undoLastTurn,
                        enabled = state.boardHistory.size > 1,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("endgame_undo_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                    OutlinedButton(
                        onClick = endgameViewModel::retryDrill,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("endgame_retry_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restart")
                    }
                    Button(
                        onClick = endgameViewModel::nextDrill,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("endgame_next_btn")
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Coach Strategy & Voice Tip Card
        if (state.status == EndgameStatus.PLAYING) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("endgame_coach_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Technique · ${state.currentDrill.techniqueName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldPrimary
                            )
                            IconButton(
                                onClick = {
                                    if (state.isVoiceSpeaking) endgameViewModel.stopVoice()
                                    else endgameViewModel.speakCurrentCoachTip()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("endgame_voice_btn")
                            ) {
                                Icon(
                                    imageVector = if (state.isVoiceSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (state.isVoiceSpeaking) "Stop Voice" else "Speak Tip",
                                    tint = ChampionshipGold
                                )
                            }
                        }
                        Text(
                            text = state.coachMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Endgame Drills Catalog
        item {
            Text(
                text = "Checkmate Patterns & Drills (${filteredDrills.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(filteredDrills, key = { it.id }) { drill ->
            val selected = drill.id == state.currentDrill.id
            val record = settings.endgameProgress[drill.id]
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        EmeraldPrimary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ),
                border = if (selected) BorderStroke(1.5.dp, EmeraldPrimary) else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { endgameViewModel.loadDrill(drill) }
                    .testTag("endgame_drill_card_${drill.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = ElectricBlue.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = drill.materialCode,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = drill.category.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(drill.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Par ${drill.parMoves} moves · ${drill.difficulty.label}" +
                                (record?.let { " · Best ${it.bestMoves}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StarRow(stars = record?.stars ?: 0)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (state.pendingPromotion != null) {
        PromotionPickerDialog(
            color = state.playerSide,
            pieceStyle = settings.pieceStyle,
            onSelect = endgameViewModel::confirmPromotion,
            onDismiss = endgameViewModel::cancelPromotion
        )
    }
}

@Composable
private fun StarRow(stars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..3) {
            Icon(
                imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i <= stars) ChampionshipGold else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun difficultyAccent(difficulty: EndgameDifficulty): Color = when (difficulty) {
    EndgameDifficulty.FUNDAMENTAL -> EmeraldPrimary
    EndgameDifficulty.INTERMEDIATE -> ChampionshipGold
    EndgameDifficulty.MASTER -> CrimsonRed
}
