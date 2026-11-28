package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.coach.BlindMovablePiece
import com.example.chess.coach.VoiceMoveParser
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.Move
import com.example.chess.engine.PieceColor
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlindChessPanel(
    board: ChessBoard,
    playerSide: PieceColor?,
    selectedSquare: Int?,
    lastSpokenAnnouncement: String,
    hidePiecesForBlindfold: Boolean,
    speechRate: Float,
    isInteractiveTurn: Boolean,
    onToggleHidePieces: () -> Unit,
    onChangeSpeechRate: (Float) -> Unit,
    onSpeakBoard: () -> Unit,
    onSpeakLastMove: () -> Unit,
    onSpeakLegalMoves: () -> Unit,
    onSpeakHint: () -> Unit,
    onSelectPieceSquare: (Int) -> Unit,
    onExecuteBlindMove: (Move) -> Unit,
    onSubmitSpokenOrTypedMove: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var commandText by remember { mutableStateOf("") }
    var voiceErrorHint by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                commandText = spoken
                val ok = onSubmitSpokenOrTypedMove(spoken)
                if (ok) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    commandText = ""
                    voiceErrorHint = null
                } else {
                    voiceErrorHint = "Could not match '$spoken' to a legal move."
                }
            }
        }
    }

    val movablePieces: List<BlindMovablePiece> = remember(board) {
        VoiceMoveParser.movablePiecesForBlind(board)
    }
    val selectedPieceGroup = remember(movablePieces, selectedSquare) {
        movablePieces.firstOrNull { it.square == selectedSquare }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("blind_chess_panel")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Accessibility Mode Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AccessibilityNew,
                            contentDescription = "Blind Audio Chess Mode",
                            tint = ElectricCyan
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Blind & Audio Chess",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Voice Narration · TalkBack Grid · Touch Move Picker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Spoken Output Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Latest audio announcement: $lastSpokenAnnouncement"
                    }
                    .testTag("blind_spoken_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = lastSpokenAnnouncement.ifBlank { "Tap Speak Board or choose a piece below to play hands-free." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Audio Readout Buttons (48dp+ accessible targets)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakBoard()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("blind_speak_board_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Speak Board")
                }
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakLastMove()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("blind_speak_last_move_btn")
                ) {
                    Text("Last Move")
                }
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakLegalMoves()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("blind_speak_legal_btn")
                ) {
                    Text("Legal Moves")
                }
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakHint()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("blind_speak_hint_btn")
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = ChampionshipGold)
                    Spacer(Modifier.width(4.dp))
                    Text("Audio Hint")
                }
            }

            // Pure Blindfold + Speech Speed Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (hidePiecesForBlindfold) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = if (hidePiecesForBlindfold) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Hide Pieces (Blindfold)", style = MaterialTheme.typography.labelLarge)
                }
                Switch(
                    checked = hidePiecesForBlindfold,
                    onCheckedChange = { onToggleHidePieces() },
                    modifier = Modifier.testTag("blind_hide_pieces_toggle")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Voice Speed:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0.8f to "Slow", 1.0f to "Normal", 1.25f to "Fast").forEach { (rate, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(speechRate - rate) < 0.08f,
                        onClick = { onChangeSpeechRate(rate) },
                        label = { Text(label) }
                    )
                }
            }

            // Voice / Spoken Move Command Entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = {
                        commandText = it
                        voiceErrorHint = null
                    },
                    label = { Text("Speak or type move (e.g. knight f3, e4)") },
                    singleLine = true,
                    enabled = isInteractiveTurn,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("blind_move_input")
                )
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your chess move, e.g. Knight f3 or Castle kingside")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: Throwable) {
                            voiceErrorHint = "Device speech recognizer unavailable — type move text instead."
                        }
                    },
                    enabled = isInteractiveTurn,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.16f))
                        .testTag("blind_mic_btn")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Speak Chess Move", tint = EmeraldPrimary)
                }
                Button(
                    onClick = {
                        val ok = onSubmitSpokenOrTypedMove(commandText)
                        if (ok) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            commandText = ""
                            voiceErrorHint = null
                        } else {
                            voiceErrorHint = "Move '$commandText' is invalid or illegal here."
                        }
                    },
                    enabled = isInteractiveTurn && commandText.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("blind_voice_submit_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Play Spoken Move", modifier = Modifier.size(18.dp))
                }
            }

            voiceErrorHint?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Accessible Two-Step Piece & Legal Move Selector (Zero-Vision Play)
            if (isInteractiveTurn && movablePieces.isNotEmpty()) {
                val activeLabel = if (playerSide != null) {
                    "Your Movable Pieces (${movablePieces.size})"
                } else {
                    "${VoiceMoveParser.colorWord(board.sideToMove)} Movable Pieces (${movablePieces.size})"
                }
                Text(activeLabel, style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("blind_piece_selector")
                ) {
                    items(movablePieces) { item ->
                        val isSel = item.square == selectedSquare
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectPieceSquare(item.square)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .semantics {
                                    contentDescription = "${item.pieceName} on ${item.squareAlgebraic}, ${item.legalOptions.size} legal moves"
                                }
                                .testTag("blind_piece_${item.squareAlgebraic}")
                        ) {
                            Text(
                                text = "${item.pieceName} ${item.squareAlgebraic} (${item.legalOptions.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                        }
                    }
                }

                if (selectedPieceGroup != null) {
                    Text(
                        text = "Moves for ${selectedPieceGroup.pieceName} on ${selectedPieceGroup.squareAlgebraic}:",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampionshipGold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("blind_legal_moves_row")
                    ) {
                        selectedPieceGroup.legalOptions.forEach { opt ->
                            FilledTonalButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onExecuteBlindMove(opt.move)
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .semantics {
                                        contentDescription = "Play ${opt.spokenLabel}"
                                    }
                                    .testTag("blind_move_${opt.move.toUci()}")
                            ) {
                                Text(
                                    text = buildString {
                                        append(opt.san)
                                        if (opt.isCapture) append(" · Cap")
                                        if (opt.isCheck) append(" · Check")
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
