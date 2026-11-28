package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.engine.MoveClassification
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvaluationBar
import com.example.ui.components.LogicalEvalFlowGraph
import com.example.ui.components.OpeningExplorerCard
import com.example.ui.theme.BestGreen
import com.example.ui.theme.BlunderRed
import com.example.ui.theme.BrilliantCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExcellentGreen
import com.example.ui.theme.GoodNeutral
import com.example.ui.theme.InaccuracyYellow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MistakeOrange
import com.example.ui.viewmodel.AnalysisViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyzeScreen(
    analysisViewModel: AnalysisViewModel,
    settings: UserSettings
) {
    val state by analysisViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showFenDialog by remember { mutableStateOf(false) }

    val currentBoard = state.boards[state.currentPly]
    val lastSquares = if (state.currentPly > 0) state.moveSquares[state.currentPly - 1] else null
    val bestMove = state.liveEval?.bestMove
    val currentAnalyzed = analysisViewModel.currentAnalyzedMove()

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("analyze_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Eval Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${state.openingEco} ${state.openingName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = state.liveEval?.formattedScore() ?: "0.00",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Eval Bar + Board
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvaluationBar(
                    centipawnsWhite = state.liveEval?.centipawnsWhite ?: 0,
                    mateIn = state.liveEval?.mateIn,
                    flipped = state.flipped,
                    modifier = Modifier.width(18.dp).fillMaxHeight()
                )
                ChessBoardView(
                    board = currentBoard,
                    boardTheme = settings.boardTheme,
                    pieceStyle = settings.pieceStyle,
                    flipped = state.flipped,
                    showCoordinates = settings.showCoordinates,
                    selectedSquare = state.selectedSquare,
                    legalMovesForSelected = state.legalMovesForSelected,
                    lastMoveFrom = lastSquares?.first,
                    lastMoveTo = lastSquares?.second,
                    bestMoveFrom = bestMove?.from,
                    bestMoveTo = bestMove?.to,
                    interactive = true,
                    onSquareTapped = analysisViewModel::onSquareTapped,
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
            }
        }

        // Best Engine Line & Classification Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Best: ${state.liveEval?.bestMoveSan?.ifBlank { "-" } ?: "..."} (d${state.liveEval?.depth ?: 0})",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldPrimary
                        )
                        if (currentAnalyzed != null) {
                            val clsColor = classificationColor(currentAnalyzed.classification)
                            Surface(color = clsColor.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "${currentAnalyzed.playedSan}: ${currentAnalyzed.classification.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = clsColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    val pv = state.liveEval?.pvSan?.joinToString(" ") ?: ""
                    if (pv.isNotBlank()) {
                        Text(
                            text = "PV: $pv",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentAnalyzed != null && currentAnalyzed.bestSan != currentAnalyzed.playedSan) {
                        Text(
                            text = "Engine preferred ${currentAnalyzed.bestSan} (${currentAnalyzed.cpLoss} cp loss)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Opening Explorer (Master Games for Current Position)
        item {
            OpeningExplorerCard(
                explorer = state.explorerPosition,
                onPlayMove = analysisViewModel::playExplorerMove
            )
        }

        // Navigation Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { analysisViewModel.goToPly(0) }) {
                    Icon(Icons.Filled.FirstPage, contentDescription = "First Move")
                }
                IconButton(onClick = { analysisViewModel.goToPly(state.currentPly - 1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Move")
                }
                IconButton(onClick = analysisViewModel::toggleAutoPlay) {
                    Icon(
                        if (state.isAutoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Auto Replay"
                    )
                }
                IconButton(onClick = { analysisViewModel.goToPly(state.currentPly + 1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Move")
                }
                IconButton(onClick = { analysisViewModel.goToPly(state.boards.lastIndex) }) {
                    Icon(Icons.Filled.LastPage, contentDescription = "Last Move")
                }
                IconButton(onClick = analysisViewModel::toggleFlip) {
                    Icon(Icons.Filled.Cached, contentDescription = "Flip Board")
                }
            }
        }

        // Engine Full Review Action & Tools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = analysisViewModel::runFullGameAnalysis,
                    enabled = state.sanMoves.isNotEmpty() && state.fullAnalysisProgress == null,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.weight(1f).testTag("run_full_analysis_btn")
                ) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Game Review")
                }
                FilledTonalButton(onClick = { showFenDialog = true }) {
                    Text("Load FEN")
                }
                FilledTonalButton(onClick = analysisViewModel::resetStandardBoard) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset Board", modifier = Modifier.size(16.dp))
                }
            }

            state.fullAnalysisProgress?.let { prog ->
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { prog }, modifier = Modifier.fillMaxWidth())
                Text("Evaluating moves with alpha-beta engine...", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Full Analysis Report
        state.fullAnalysis?.let { summary ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Game Accuracy Report", style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("White Accuracy", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${"%.1f".format(summary.whiteAccuracy)}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = EmeraldPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Black Accuracy", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${"%.1f".format(summary.blackAccuracy)}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF60A5FA)
                                )
                            }
                        }

                        // Logical Eval Flow graph across plies (tap any point to jump to that ply)
                        if (summary.analyzedMoves.isNotEmpty()) {
                            LogicalEvalFlowGraph(
                                analyzedMoves = summary.analyzedMoves,
                                currentPly = state.currentPly,
                                onSelectPly = analysisViewModel::goToPly
                            )
                        }

                        // Classification summary chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                MoveClassification.BRILLIANT,
                                MoveClassification.BEST,
                                MoveClassification.INACCURACY,
                                MoveClassification.MISTAKE,
                                MoveClassification.BLUNDER
                            ).forEach { cls ->
                                val w = summary.whiteCounts[cls] ?: 0
                                val b = summary.blackCounts[cls] ?: 0
                                if (w + b > 0) {
                                    Surface(
                                        color = classificationColor(cls).copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${cls.label}: W$w / B$b",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = classificationColor(cls),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clickable Algebraic Move List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Move List", style = MaterialTheme.typography.titleMedium)
                        Row {
                            IconButton(
                                onClick = {
                                    val fen = currentBoard.toFen()
                                    clipboard.setText(AnnotatedString(fen))
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy FEN", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val pgnOrFen = state.loadedPgn.ifBlank { currentBoard.toFen() }
                                    shareText(context, pgnOrFen)
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share PGN or FEN", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (state.sanMoves.isEmpty()) {
                        Text(
                            "Tap pieces on the board to explore variations, or open any match from Profile > Match History.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.sanMoves.forEachIndexed { idx, san ->
                                val ply = idx + 1
                                val isCurrent = ply == state.currentPly
                                val prefix = if (idx % 2 == 0) "${idx / 2 + 1}." else ""
                                val cls = state.fullAnalysis?.analyzedMoves?.getOrNull(idx)?.classification
                                val badge = cls?.symbol?.takeIf { it.isNotEmpty() && cls != MoveClassification.GOOD } ?: ""
                                Surface(
                                    color = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { analysisViewModel.goToPly(ply) }
                                ) {
                                    Text(
                                        text = "$prefix$san$badge",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFenDialog) {
        var fenText by remember { mutableStateOf(currentBoard.toFen()) }
        AlertDialog(
            onDismissRequest = { showFenDialog = false },
            title = { Text("Load FEN Position") },
            text = {
                OutlinedTextField(
                    value = fenText,
                    onValueChange = { fenText = it },
                    label = { Text("FEN String") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    analysisViewModel.loadFen(fenText)
                    showFenDialog = false
                }) { Text("Load") }
            },
            dismissButton = {
                TextButton(onClick = { showFenDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun classificationColor(cls: MoveClassification): Color = when (cls) {
    MoveClassification.BRILLIANT -> BrilliantCyan
    MoveClassification.BEST -> BestGreen
    MoveClassification.EXCELLENT -> ExcellentGreen
    MoveClassification.GOOD, MoveClassification.BOOK -> GoodNeutral
    MoveClassification.INACCURACY -> InaccuracyYellow
    MoveClassification.MISTAKE -> MistakeOrange
    MoveClassification.BLUNDER -> BlunderRed
}
