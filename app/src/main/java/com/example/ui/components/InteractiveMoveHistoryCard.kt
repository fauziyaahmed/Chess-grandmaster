package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.engine.PieceColor
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily

data class MoveNotationTurn(
    val moveNumber: Int,
    val whitePly: Int?,
    val whiteSan: String?,
    val blackPly: Int?,
    val blackSan: String?
)

fun buildNotationTurns(
    sanMoves: List<String>,
    startingSide: PieceColor = PieceColor.WHITE
): List<MoveNotationTurn> {
    if (sanMoves.isEmpty()) return emptyList()
    val turns = mutableListOf<MoveNotationTurn>()
    var idx = 0
    var moveNum = 1
    if (startingSide == PieceColor.BLACK) {
        turns += MoveNotationTurn(
            moveNumber = moveNum,
            whitePly = null,
            whiteSan = "...",
            blackPly = 1,
            blackSan = sanMoves[0]
        )
        idx = 1
        moveNum = 2
    }
    while (idx < sanMoves.size) {
        val wPly = idx + 1
        val wSan = sanMoves[idx]
        val bPly = if (idx + 1 < sanMoves.size) idx + 2 else null
        val bSan = sanMoves.getOrNull(idx + 1)
        turns += MoveNotationTurn(
            moveNumber = moveNum,
            whitePly = wPly,
            whiteSan = wSan,
            blackPly = bPly,
            blackSan = bSan
        )
        idx += 2
        moveNum++
    }
    return turns
}

@Composable
fun InteractiveMoveHistoryCard(
    sanMoves: List<String>,
    displayedPly: Int,
    onJumpToPly: (Int) -> Unit,
    modifier: Modifier = Modifier,
    startingSide: PieceColor = PieceColor.WHITE,
    emptyHint: String = "Tap a piece to make the opening move"
) {
    val totalPlies = sanMoves.size
    val clampedPly = displayedPly.coerceIn(0, totalPlies)
    val isReviewing = totalPlies > 0 && clampedPly < totalPlies
    val turns = remember(sanMoves, startingSide) { buildNotationTurns(sanMoves, startingSide) }
    var expandedSheet by remember { mutableStateOf(false) }

    val rowListState = rememberLazyListState()
    LaunchedEffect(clampedPly, turns.size, expandedSheet) {
        if (!expandedSheet && turns.isNotEmpty()) {
            val targetTurnIndex = if (clampedPly == 0) {
                0
            } else {
                val offset = if (startingSide == PieceColor.BLACK) clampedPly else (clampedPly - 1)
                (offset / 2 + 1).coerceIn(0, turns.size)
            }
            rowListState.animateScrollToItem(targetTurnIndex)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        border = if (isReviewing) BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.65f)) else null,
        modifier = modifier
            .fillMaxWidth()
            .testTag("move_history_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header + step navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = if (isReviewing) ChampionshipGold else EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isReviewing) {
                            val activeSan = sanMoves.getOrNull(clampedPly - 1) ?: "Start"
                            "Move $clampedPly/$totalPlies ($activeSan)"
                        } else {
                            "Moves"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isReviewing) ChampionshipGold else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("move_history_status_label")
                    )
                    if (isReviewing) {
                        Surface(
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onJumpToPly(totalPlies) }
                                .testTag("move_history_live_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "Live",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (totalPlies > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onJumpToPly(0) },
                            enabled = clampedPly > 0,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("move_history_start_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FirstPage,
                                contentDescription = "Starting Position",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onJumpToPly(clampedPly - 1) },
                            enabled = clampedPly > 0,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("move_history_prev_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Move",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onJumpToPly(clampedPly + 1) },
                            enabled = clampedPly < totalPlies,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("move_history_next_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Move",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onJumpToPly(totalPlies) },
                            enabled = clampedPly < totalPlies,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("move_history_end_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LastPage,
                                contentDescription = "Latest Move",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { expandedSheet = !expandedSheet },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("move_history_expand_btn")
                        ) {
                            Icon(
                                imageVector = if (expandedSheet) Icons.Filled.UnfoldLess else Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = if (expandedSheet) "Collapse Move Sheet" else "Expand Move Sheet",
                                tint = if (expandedSheet) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (sanMoves.isEmpty()) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            } else if (!expandedSheet) {
                // Horizontal Algebraic Move Tape (tap any move e4, Nf3, etc.)
                LazyRow(
                    state = rowListState,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("move_history_ribbon")
                ) {
                    item(key = "ply_0") {
                        MovePlyPill(
                            label = "Start",
                            isSelected = clampedPly == 0,
                            onClick = { onJumpToPly(0) },
                            testTag = "move_ply_0"
                        )
                    }
                    itemsIndexed(turns, key = { _, t -> t.moveNumber }) { _, turn ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${turn.moveNumber}.",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (turn.whitePly != null && turn.whiteSan != null) {
                                MovePlyPill(
                                    label = turn.whiteSan,
                                    isSelected = clampedPly == turn.whitePly,
                                    onClick = { onJumpToPly(turn.whitePly) },
                                    testTag = "move_ply_${turn.whitePly}"
                                )
                            } else if (turn.whiteSan != null) {
                                Text(
                                    text = turn.whiteSan,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (turn.blackPly != null && turn.blackSan != null) {
                                MovePlyPill(
                                    label = turn.blackSan,
                                    isSelected = clampedPly == turn.blackPly,
                                    onClick = { onJumpToPly(turn.blackPly) },
                                    testTag = "move_ply_${turn.blackPly}"
                                )
                            }
                        }
                    }
                }
            } else {
                // Two-column Algebraic Scoresheet (White | Black)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("move_history_scoresheet"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    turns.forEach { turn ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${turn.moveNumber}.",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(34.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (turn.whitePly != null && turn.whiteSan != null) {
                                    MovePlyPill(
                                        label = turn.whiteSan,
                                        isSelected = clampedPly == turn.whitePly,
                                        onClick = { onJumpToPly(turn.whitePly) },
                                        testTag = "move_ply_${turn.whitePly}",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = turn.whiteSan ?: "",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (turn.blackPly != null && turn.blackSan != null) {
                                    MovePlyPill(
                                        label = turn.blackSan,
                                        isSelected = clampedPly == turn.blackPly,
                                        onClick = { onJumpToPly(turn.blackPly) },
                                        testTag = "move_ply_${turn.blackPly}",
                                        modifier = Modifier.fillMaxWidth()
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
private fun MovePlyPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
