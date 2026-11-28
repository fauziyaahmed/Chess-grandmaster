package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.GameResult
import com.example.chess.engine.Move
import com.example.chess.engine.PieceColor
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.ActiveGameState

private data class MoveOption(
    val move: Move,
    val uci: String,
    val san: String,
    val scoreHint: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionalMoveSheet(
    state: ActiveGameState,
    onAddRule: (oppUci: String, oppSan: String, replyUci: String, replySan: String) -> Unit,
    onPlayAndArmRule: (myUci: String, oppUci: String, oppSan: String, replyUci: String, replySan: String) -> Unit,
    onRemoveRule: (Long) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    val isWaitingOnOpponent = state.playerSide != null && state.board.sideToMove != state.playerSide

    var selectedMyMove by remember(state.board) { mutableStateOf<MoveOption?>(null) }
    var selectedOppMove by remember(state.board, selectedMyMove) { mutableStateOf<MoveOption?>(null) }
    var selectedReplyMove by remember(state.board, selectedOppMove) { mutableStateOf<MoveOption?>(null) }

    val myOptions = remember(state.board, isWaitingOnOpponent) {
        if (isWaitingOnOpponent || state.result != GameResult.IN_PROGRESS) emptyList()
        else sortedMoveOptions(state.board)
    }

    val oppBaseBoard = remember(state.board, isWaitingOnOpponent, selectedMyMove) {
        when {
            isWaitingOnOpponent -> state.board
            selectedMyMove != null -> state.board.makeMove(selectedMyMove!!.move)
            else -> null
        }
    }

    val oppOptions = remember(oppBaseBoard) {
        if (oppBaseBoard == null) emptyList() else sortedMoveOptions(oppBaseBoard)
    }

    val replyBaseBoard = remember(oppBaseBoard, selectedOppMove) {
        if (oppBaseBoard != null && selectedOppMove != null) {
            oppBaseBoard.makeMove(selectedOppMove!!.move)
        } else null
    }

    val replyOptions = remember(replyBaseBoard) {
        if (replyBaseBoard == null) emptyList() else sortedMoveOptions(replyBaseBoard)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("conditional_move_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AltRoute, contentDescription = null, tint = ChampionshipGold)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Conditional Moves", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (isWaitingOnOpponent) {
                            "Opponent's turn: If they play X → Auto-play Y"
                        } else {
                            "Pre-plan: Your move → If Opponent plays X → Auto-play Y"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close Conditional Moves")
            }
        }

        if (state.conditionalMoves.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.14f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Armed Conditional Lines (${state.conditionalMoves.size})", style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(onClick = onClearAll, modifier = Modifier.testTag("cond_clear_all_btn")) {
                            Text("Clear All")
                        }
                    }
                    state.conditionalMoves.forEach { rule ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "If ${rule.expectedOpponentSan}  →  Auto-play ${rule.responseSan}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveRule(rule.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove rule")
                            }
                        }
                    }
                }
            }
        }

        if (!isWaitingOnOpponent) {
            Text("Step 1: Pick Your Move First", style = MaterialTheme.typography.titleMedium, color = ElectricBlue)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                myOptions.take(18).forEachIndexed { idx, opt ->
                    FilterChip(
                        selected = selectedMyMove?.uci == opt.uci,
                        onClick = {
                            selectedMyMove = opt
                            selectedOppMove = null
                            selectedReplyMove = null
                        },
                        label = { Text(opt.san) },
                        modifier = Modifier.testTag("cond_my_move_$idx")
                    )
                }
            }
        }

        if (oppBaseBoard != null) {
            val stepNum = if (isWaitingOnOpponent) "Step 1" else "Step 2"
            Text("$stepNum: If Opponent Plays...", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)
            if (oppOptions.isEmpty()) {
                Text("No legal opponent replies in this branch.", style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    oppOptions.take(20).forEachIndexed { idx, opt ->
                        FilterChip(
                            selected = selectedOppMove?.uci == opt.uci,
                            onClick = {
                                selectedOppMove = opt
                                selectedReplyMove = null
                            },
                            label = { Text(opt.san) },
                            modifier = Modifier.testTag("cond_opp_move_$idx")
                        )
                    }
                }
            }
        }

        if (replyBaseBoard != null && selectedOppMove != null) {
            val stepNum = if (isWaitingOnOpponent) "Step 2" else "Step 3"
            Text(
                "$stepNum: Auto-Play Response (after ${selectedOppMove!!.san})",
                style = MaterialTheme.typography.titleMedium,
                color = EmeraldPrimary
            )
            if (replyOptions.isEmpty()) {
                Text("No legal replies after ${selectedOppMove!!.san}.", style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    replyOptions.take(20).forEachIndexed { idx, opt ->
                        FilterChip(
                            selected = selectedReplyMove?.uci == opt.uci,
                            onClick = { selectedReplyMove = opt },
                            label = { Text(opt.san) },
                            modifier = Modifier.testTag("cond_reply_move_$idx")
                        )
                    }
                }
            }
        }

        val readyToArm = selectedOppMove != null && selectedReplyMove != null &&
            (isWaitingOnOpponent || selectedMyMove != null)

        if (readyToArm) {
            val opp = selectedOppMove!!
            val reply = selectedReplyMove!!
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isWaitingOnOpponent) {
                        "Rule Preview: If ${opp.san} → Auto-play ${reply.san}"
                    } else {
                        "Line Preview: Play ${selectedMyMove!!.san} → If ${opp.san} → Auto-play ${reply.san}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Button(
                onClick = {
                    if (isWaitingOnOpponent) {
                        onAddRule(opp.uci, opp.san, reply.uci, reply.san)
                        selectedOppMove = null
                        selectedReplyMove = null
                    } else {
                        onPlayAndArmRule(selectedMyMove!!.uci, opp.uci, opp.san, reply.uci, reply.san)
                        onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("cond_arm_rule_btn")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isWaitingOnOpponent) "Arm Conditional Move" else "Play ${selectedMyMove!!.san} & Arm Rule")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun sortedMoveOptions(board: ChessBoard): List<MoveOption> {
    val legal = board.generateLegalMoves()
    return legal.map { move ->
        val next = board.makeMove(move)
        var score = 0
        if (next.isInCheck(next.sideToMove)) score += 400
        if (board.pieceAt(move.to) != null) score += 300
        if (move.isCastling) score += 200
        if (move.promotion != null) score += 350
        MoveOption(
            move = move,
            uci = move.toUci(),
            san = board.toSan(move, legal),
            scoreHint = score
        )
    }.sortedByDescending { it.scoreHint }
}
