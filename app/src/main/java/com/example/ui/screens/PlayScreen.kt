package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.coach.CoachQueryType
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.GameResult
import com.example.chess.engine.GameVariant
import com.example.chess.engine.MoveClassification
import com.example.chess.engine.Piece
import com.example.chess.engine.PieceColor
import com.example.chess.engine.TimeControl
import com.example.data.RatingCalculator
import com.example.data.preferences.UserSettings
import com.example.ui.components.BlindChessPanel
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceIcon
import com.example.ui.components.CoachDavidOptionToggle
import com.example.ui.components.ConditionalMoveSheet
import com.example.ui.components.CountryFlag
import com.example.ui.components.InteractiveMoveHistoryCard
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.components.TimeControlPickerSection
import com.example.ui.theme.BestGreen
import com.example.ui.theme.BlunderRed
import com.example.ui.theme.BrilliantCyan
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExcellentGreen
import com.example.ui.theme.GoodNeutral
import com.example.ui.theme.InaccuracyYellow
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MistakeOrange
import com.example.ui.viewmodel.ActiveGameState
import com.example.ui.viewmodel.GameSetupConfig
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.PlayOpponentMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    gameViewModel: GameViewModel,
    settings: UserSettings,
    onAnalyzeMatch: (Long) -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenThemeStudio: () -> Unit = {},
    onOpenBluetoothLobby: () -> Unit = {},
    onToggleCoachVoice: (Boolean) -> Unit = {},
    onSaveTimeControlPreference: (Boolean, String, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onSetCoachDavidDefault: (Boolean) -> Unit = {}
) {
    val state by gameViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSetupSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showResignConfirm by remember { mutableStateOf(false) }
    var showConditionalSheet by remember { mutableStateOf(false) }

    LaunchedEffect(
        settings.soundEnabled,
        settings.soundVolume,
        settings.soundPack,
        settings.autoPromoteQueen,
        settings.coachVoiceEnabled
    ) {
        gameViewModel.syncPreferences(
            sound = settings.soundEnabled,
            autoQueen = settings.autoPromoteQueen,
            voiceEnabled = settings.coachVoiceEnabled,
            volume = settings.soundVolume,
            pack = settings.soundPack
        )
    }

    val displayedBoard = state.displayedBoard
    val materialInfo = remember(displayedBoard) { displayedBoard.materialStats() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerBannerRow(
                        name = if (state.flipped) state.whiteName else state.blackName,
                        rating = if (state.flipped) state.whiteRating else state.blackRating,
                        timeMs = if (state.flipped) state.whiteTimeMs else state.blackTimeMs,
                        isActiveTurn = state.result == GameResult.IN_PROGRESS && !state.isReviewingHistory &&
                            displayedBoard.sideToMove == (if (state.flipped) PieceColor.WHITE else PieceColor.BLACK),
                        capturedPieces = if (state.flipped) materialInfo.capturedByWhite else materialInfo.capturedByBlack,
                        pieceColor = if (state.flipped) PieceColor.BLACK else PieceColor.WHITE,
                        materialAdv = if (state.flipped) materialInfo.whiteAdvantage else -materialInfo.whiteAdvantage,
                        settings = settings
                    )
                    Spacer(Modifier.height(6.dp))
                    ChessBoardView(
                        board = displayedBoard,
                        boardTheme = settings.boardTheme,
                        pieceStyle = settings.pieceStyle,
                        flipped = state.flipped,
                        showCoordinates = settings.showCoordinates,
                        selectedSquare = if (state.isReviewingHistory) null else state.selectedSquare,
                        legalMovesForSelected = if (state.isReviewingHistory) emptyList() else state.legalMovesForSelected,
                        lastMoveFrom = state.displayedLastMoveFrom,
                        lastMoveTo = state.displayedLastMoveTo,
                        bestMoveFrom = if (!state.isReviewingHistory && state.showCoachArrow) state.coachArrowFrom else null,
                        bestMoveTo = if (!state.isReviewingHistory && state.showCoachArrow) state.coachArrowTo else null,
                        castleGuides = if (state.isReviewingHistory) emptyList() else state.castleGuides,
                        boardPattern = settings.boardPattern,
                        interactive = state.result == GameResult.IN_PROGRESS,
                        hidePiecesForBlindfold = state.blindfoldHidePieces,
                        onSquareTapped = gameViewModel::onSquareTapped,
                        modifier = Modifier.fillMaxWidth(settings.boardScale)
                    )
                    Spacer(Modifier.height(6.dp))
                    PlayerBannerRow(
                        name = if (state.flipped) state.blackName else state.whiteName,
                        rating = if (state.flipped) state.blackRating else state.whiteRating,
                        timeMs = if (state.flipped) state.blackTimeMs else state.whiteTimeMs,
                        isActiveTurn = state.result == GameResult.IN_PROGRESS && !state.isReviewingHistory &&
                            displayedBoard.sideToMove == (if (state.flipped) PieceColor.BLACK else PieceColor.WHITE),
                        capturedPieces = if (state.flipped) materialInfo.capturedByBlack else materialInfo.capturedByWhite,
                        pieceColor = if (state.flipped) PieceColor.WHITE else PieceColor.BLACK,
                        materialAdv = if (state.flipped) -materialInfo.whiteAdvantage else materialInfo.whiteAdvantage,
                        settings = settings
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameHeaderBar(
                        state = state,
                        onOpenSetup = { showSetupSheet = true },
                        onToggleCoach = gameViewModel::toggleCoachDavid,
                        onToggleBlind = gameViewModel::toggleBlindMode,
                        onOpenHistory = onOpenHistory,
                        onOpenThemeStudio = onOpenThemeStudio
                    )
                    MatchInfoPillsBar(
                        state = state,
                        onToggleCastleGuide = gameViewModel::toggleCastleGuide,
                        onOpenBluetoothLobby = onOpenBluetoothLobby,
                        onRespondDrawOffer = gameViewModel::respondToDrawOffer,
                        onOpenConditional = { showConditionalSheet = true }
                    )
                    AnimatedVisibility(visible = state.config.isBlindMode) {
                        BlindChessPanel(
                            board = displayedBoard,
                            playerSide = state.playerSide,
                            selectedSquare = state.selectedSquare,
                            lastSpokenAnnouncement = state.blindAnnouncement,
                            hidePiecesForBlindfold = state.blindfoldHidePieces,
                            speechRate = state.blindSpeechRate,
                            isInteractiveTurn = state.result == GameResult.IN_PROGRESS && !state.isReviewingHistory &&
                                (state.playerSide == null || displayedBoard.sideToMove == state.playerSide),
                            onToggleHidePieces = gameViewModel::toggleBlindfoldHidePieces,
                            onChangeSpeechRate = gameViewModel::setBlindSpeechRate,
                            onSpeakBoard = gameViewModel::speakBoardSummary,
                            onSpeakLastMove = gameViewModel::speakLastMoveBlind,
                            onSpeakLegalMoves = gameViewModel::speakLegalMovesBlind,
                            onSpeakHint = gameViewModel::speakBlindHint,
                            onSelectPieceSquare = gameViewModel::onSquareTapped,
                            onExecuteBlindMove = gameViewModel::executeBlindMove,
                            onSubmitSpokenOrTypedMove = gameViewModel::submitSpokenOrTypedMove
                        )
                    }
                    InteractiveMoveHistoryCard(
                        sanMoves = state.sanHistory,
                        displayedPly = state.displayedPly,
                        onJumpToPly = gameViewModel::jumpToMovePly
                    )
                    GameControlsBar(
                        state = state,
                        onResign = { showResignConfirm = true },
                        onDraw = gameViewModel::offerDraw,
                        onTakeback = gameViewModel::requestTakeback,
                        onConditional = { showConditionalSheet = true },
                        onFlip = gameViewModel::flipBoard,
                        onChat = { showChatSheet = true },
                        onSaveNow = gameViewModel::saveGameToHistoryNow,
                        onRematch = { gameViewModel.startNewGame(state.config) }
                    )
                    AnimatedVisibility(visible = state.config.coachDavidEnabled) {
                        CoachDavidPanelCard(
                            state = state,
                            voiceEnabled = settings.coachVoiceEnabled,
                            onToggleVoice = { onToggleCoachVoice(!settings.coachVoiceEnabled) },
                            onSpeakVoice = {
                                if (state.isVoiceSpeaking) gameViewModel.stopCoachVoice()
                                else gameViewModel.speakCurrentCoachMessage()
                            },
                            onUndoMistake = gameViewModel::undoMistakeAndRetry,
                            onQuery = gameViewModel::askCoachDavid,
                            onHideArrow = gameViewModel::hideCoachArrow,
                            onDisableCoach = gameViewModel::toggleCoachDavid
                        )
                    }
                    if (state.result != GameResult.IN_PROGRESS) {
                        PostGameCard(
                            state = state,
                            onRematch = { gameViewModel.startNewGame(state.config) },
                            onAnalyze = { state.savedMatchId?.let(onAnalyzeMatch) },
                            onOpenHistory = onOpenHistory,
                            onSharePgn = { shareText(context, buildQuickPgn(state)) }
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameHeaderBar(
                    state = state,
                    onOpenSetup = { showSetupSheet = true },
                    onToggleCoach = gameViewModel::toggleCoachDavid,
                    onToggleBlind = gameViewModel::toggleBlindMode,
                    onOpenHistory = onOpenHistory,
                    onOpenThemeStudio = onOpenThemeStudio
                )

                MatchInfoPillsBar(
                    state = state,
                    onToggleCastleGuide = gameViewModel::toggleCastleGuide,
                    onOpenBluetoothLobby = onOpenBluetoothLobby,
                    onRespondDrawOffer = gameViewModel::respondToDrawOffer,
                    onOpenConditional = { showConditionalSheet = true }
                )

                if (state.isAiThinking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                }

                PlayerBannerRow(
                    name = if (state.flipped) state.whiteName else state.blackName,
                    rating = if (state.flipped) state.whiteRating else state.blackRating,
                    timeMs = if (state.flipped) state.whiteTimeMs else state.blackTimeMs,
                    isActiveTurn = state.result == GameResult.IN_PROGRESS && !state.isReviewingHistory &&
                        displayedBoard.sideToMove == (if (state.flipped) PieceColor.WHITE else PieceColor.BLACK),
                    capturedPieces = if (state.flipped) materialInfo.capturedByWhite else materialInfo.capturedByBlack,
                    pieceColor = if (state.flipped) PieceColor.BLACK else PieceColor.WHITE,
                    materialAdv = if (state.flipped) materialInfo.whiteAdvantage else -materialInfo.whiteAdvantage,
                    settings = settings
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ChessBoardView(
                        board = displayedBoard,
                        boardTheme = settings.boardTheme,
                        pieceStyle = settings.pieceStyle,
                        flipped = state.flipped,
                        showCoordinates = settings.showCoordinates,
                        selectedSquare = if (state.isReviewingHistory) null else state.selectedSquare,
                        legalMovesForSelected = if (state.isReviewingHistory) emptyList() else state.legalMovesForSelected,
                        lastMoveFrom = state.displayedLastMoveFrom,
                        lastMoveTo = state.displayedLastMoveTo,
                        bestMoveFrom = if (!state.isReviewingHistory && state.showCoachArrow) state.coachArrowFrom else null,
                        bestMoveTo = if (!state.isReviewingHistory && state.showCoachArrow) state.coachArrowTo else null,
                        castleGuides = if (state.isReviewingHistory) emptyList() else state.castleGuides,
                        boardPattern = settings.boardPattern,
                        interactive = state.result == GameResult.IN_PROGRESS,
                        hidePiecesForBlindfold = state.blindfoldHidePieces,
                        onSquareTapped = gameViewModel::onSquareTapped,
                        modifier = Modifier
                            .fillMaxWidth(settings.boardScale)
                            .widthIn(max = 520.dp)
                    )
                }

                PlayerBannerRow(
                    name = if (state.flipped) state.blackName else state.whiteName,
                    rating = if (state.flipped) state.blackRating else state.whiteRating,
                    timeMs = if (state.flipped) state.blackTimeMs else state.whiteTimeMs,
                    isActiveTurn = state.result == GameResult.IN_PROGRESS && !state.isReviewingHistory &&
                        displayedBoard.sideToMove == (if (state.flipped) PieceColor.BLACK else PieceColor.WHITE),
                    capturedPieces = if (state.flipped) materialInfo.capturedByBlack else materialInfo.capturedByWhite,
                    pieceColor = if (state.flipped) PieceColor.WHITE else PieceColor.BLACK,
                    materialAdv = if (state.flipped) -materialInfo.whiteAdvantage else materialInfo.whiteAdvantage,
                    settings = settings
                )

                if (state.statusMessage.isNotBlank()) {
                    Text(
                        text = state.statusMessage,
                        style = MaterialTheme.typography.labelLarge,
                        color = ChampionshipGold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                AnimatedVisibility(visible = state.config.isBlindMode) {
                    BlindChessPanel(
                        board = displayedBoard,
                        playerSide = state.playerSide,
                        selectedSquare = state.selectedSquare,
                        lastSpokenAnnouncement = state.blindAnnouncement,
                        hidePiecesForBlindfold = state.blindfoldHidePieces,
                        speechRate = state.blindSpeechRate,
                        isInteractiveTurn = state.result == GameResult.IN_PROGRESS && !state.isReviewingHistory &&
                            (state.playerSide == null || displayedBoard.sideToMove == state.playerSide),
                        onToggleHidePieces = gameViewModel::toggleBlindfoldHidePieces,
                        onChangeSpeechRate = gameViewModel::setBlindSpeechRate,
                        onSpeakBoard = gameViewModel::speakBoardSummary,
                        onSpeakLastMove = gameViewModel::speakLastMoveBlind,
                        onSpeakLegalMoves = gameViewModel::speakLegalMovesBlind,
                        onSpeakHint = gameViewModel::speakBlindHint,
                        onSelectPieceSquare = gameViewModel::onSquareTapped,
                        onExecuteBlindMove = gameViewModel::executeBlindMove,
                        onSubmitSpokenOrTypedMove = gameViewModel::submitSpokenOrTypedMove
                    )
                }

                InteractiveMoveHistoryCard(
                    sanMoves = state.sanHistory,
                    displayedPly = state.displayedPly,
                    onJumpToPly = gameViewModel::jumpToMovePly
                )

                GameControlsBar(
                    state = state,
                    onResign = { showResignConfirm = true },
                    onDraw = gameViewModel::offerDraw,
                    onTakeback = gameViewModel::requestTakeback,
                    onConditional = { showConditionalSheet = true },
                    onFlip = gameViewModel::flipBoard,
                    onChat = { showChatSheet = true },
                    onSaveNow = gameViewModel::saveGameToHistoryNow,
                    onRematch = { gameViewModel.startNewGame(state.config) }
                )

                AnimatedVisibility(visible = state.config.coachDavidEnabled) {
                    CoachDavidPanelCard(
                        state = state,
                        voiceEnabled = settings.coachVoiceEnabled,
                        onToggleVoice = { onToggleCoachVoice(!settings.coachVoiceEnabled) },
                        onSpeakVoice = {
                            if (state.isVoiceSpeaking) gameViewModel.stopCoachVoice()
                            else gameViewModel.speakCurrentCoachMessage()
                        },
                        onUndoMistake = gameViewModel::undoMistakeAndRetry,
                        onQuery = gameViewModel::askCoachDavid,
                        onHideArrow = gameViewModel::hideCoachArrow,
                        onDisableCoach = gameViewModel::toggleCoachDavid
                    )
                }

                if (state.result != GameResult.IN_PROGRESS) {
                    PostGameCard(
                        state = state,
                        onRematch = { gameViewModel.startNewGame(state.config) },
                        onAnalyze = { state.savedMatchId?.let(onAnalyzeMatch) },
                        onOpenHistory = onOpenHistory,
                        onSharePgn = { shareText(context, buildQuickPgn(state)) }
                    )
                }
            }
        }
    }

    if (state.pendingPromotion != null) {
        PromotionPickerDialog(
            color = state.board.sideToMove,
            pieceStyle = settings.pieceStyle,
            onSelect = gameViewModel::confirmPromotion,
            onDismiss = gameViewModel::cancelPromotion
        )
    }

    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("Resign Match?") },
            text = { Text("Resigning will award the victory to your opponent and save the match to your History.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResignConfirm = false
                        gameViewModel.resign()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) { Text("Resign") }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showSetupSheet) {
        ModalBottomSheet(onDismissRequest = { showSetupSheet = false }) {
            NewGameSetupSheet(
                currentConfig = state.config,
                settings = settings,
                onStart = { cfg ->
                    showSetupSheet = false
                    onSetCoachDavidDefault(cfg.coachDavidEnabled)
                    gameViewModel.startNewGame(cfg)
                },
                onOpenBluetoothLobby = {
                    showSetupSheet = false
                    onOpenBluetoothLobby()
                },
                onSaveTimeControlPreference = onSaveTimeControlPreference
            )
        }
    }

    if (showConditionalSheet) {
        ModalBottomSheet(onDismissRequest = { showConditionalSheet = false }) {
            ConditionalMoveSheet(
                state = state,
                onAddRule = gameViewModel::addConditionalMove,
                onPlayAndArmRule = gameViewModel::playMoveWithConditional,
                onRemoveRule = gameViewModel::removeConditionalMove,
                onClearAll = gameViewModel::clearConditionalMoves,
                onClose = { showConditionalSheet = false }
            )
        }
    }

    if (showChatSheet) {
        ModalBottomSheet(onDismissRequest = { showChatSheet = false }) {
            GameChatSheet(
                state = state,
                safeFilterEnabled = settings.safeChatFilter,
                onSend = { txt -> gameViewModel.sendChatMessage(txt, settings.safeChatFilter) },
                onClose = { showChatSheet = false }
            )
        }
    }
}

@Composable
private fun GameHeaderBar(
    state: ActiveGameState,
    onOpenSetup: () -> Unit,
    onToggleCoach: () -> Unit,
    onToggleBlind: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenThemeStudio: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val variantBadge = if (state.config.variant == GameVariant.FISCHER960)
                "Chess960 #${state.chess960Pos}" else state.config.timeControl.category.displayName
            Text(
                text = "$variantBadge · ${state.config.timeControl.label}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${state.openingEco} ${state.openingName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleBlind,
                modifier = Modifier.testTag("play_toggle_blind_btn")
            ) {
                Icon(
                    Icons.Filled.AccessibilityNew,
                    contentDescription = "Toggle Blind Audio Chess",
                    tint = if (state.config.isBlindMode) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onOpenThemeStudio,
                modifier = Modifier.testTag("play_theme_studio_btn")
            ) {
                Icon(Icons.Filled.Palette, contentDescription = "Themes & Backgrounds")
            }
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier.testTag("play_history_btn")
            ) {
                Icon(Icons.Filled.History, contentDescription = "View Match History")
            }
            FilledTonalButton(
                onClick = onToggleCoach,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (state.config.coachDavidEnabled) ElectricBlue.copy(alpha = 0.24f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("play_coach_david_btn")
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = "Coach David",
                    tint = if (state.config.coachDavidEnabled) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("David")
            }
            FilledTonalButton(
                onClick = onOpenSetup,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("open_game_setup_btn")
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Setup")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchInfoPillsBar(
    state: ActiveGameState,
    onToggleCastleGuide: () -> Unit,
    onOpenBluetoothLobby: () -> Unit,
    onRespondDrawOffer: (Boolean) -> Unit,
    onOpenConditional: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val stakes = state.preMatchStakes
            if (stakes != null) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("match_rating_stakes_bar")
                ) {
                    Text(
                        text = "Rated · Win +${stakes.winDelta} / Draw ${signedInt(stakes.drawDelta)} / Loss ${stakes.lossDelta}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (!state.config.isRated) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("match_rating_stakes_bar")
                ) {
                    Text(
                        text = "Casual Unrated Match",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (state.config.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) {
                Surface(
                    color = ElectricBlue.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onOpenBluetoothLobby).testTag("bt_match_status_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "BT: ${state.btConnectionState.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricBlue
                        )
                    }
                }
            }

            if (state.conditionalMoves.isNotEmpty()) {
                Surface(
                    color = ChampionshipGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onOpenConditional).testTag("conditional_active_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.AltRoute, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${state.conditionalMoves.size} Conditional Armed",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampionshipGold
                        )
                    }
                }
            }
        }

        if (state.config.variant == GameVariant.FISCHER960 && state.result == GameResult.IN_PROGRESS) {
            val targetSide = state.playerSide ?: state.board.sideToMove
            val hasCastled = if (targetSide == PieceColor.WHITE) state.whiteHasCastled else state.blackHasCastled
            Surface(
                color = if (!hasCastled) ChampionshipGold.copy(alpha = 0.14f) else EmeraldPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("fischer_castle_guide_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!hasCastled) {
                            "Castling Guide: Gold King → g/c · Cyan Rook → f/d (Auto-off after castle)"
                        } else {
                            "Castled ✓ Chess960 castling direction turned off"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!hasCastled) ChampionshipGold else EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!hasCastled) {
                        IconButton(
                            onClick = onToggleCastleGuide,
                            modifier = Modifier.size(26.dp).testTag("fischer_castle_toggle_chip")
                        ) {
                            Icon(
                                imageVector = if (state.showCastleGuide) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle Chess960 Castling Direction",
                                tint = ChampionshipGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        state.conditionalStatusNotice?.let { notice ->
            Text(
                text = notice,
                style = MaterialTheme.typography.labelSmall,
                color = ChampionshipGold
            )
        }

        if (state.drawOfferedByOpponent && state.result == GameResult.IN_PROGRESS) {
            Surface(
                color = ElectricBlue.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Friend offered a draw", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onRespondDrawOffer(true) },
                            modifier = Modifier.testTag("bt_accept_draw_btn")
                        ) { Text("Accept") }
                        OutlinedButton(
                            onClick = { onRespondDrawOffer(false) },
                            modifier = Modifier.testTag("bt_decline_draw_btn")
                        ) { Text("Decline") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoachDavidPanelCard(
    state: ActiveGameState,
    voiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onSpeakVoice: () -> Unit,
    onUndoMistake: () -> Unit,
    onQuery: (CoachQueryType) -> Unit,
    onHideArrow: () -> Unit,
    onDisableCoach: () -> Unit
) {
    val fb = state.coachFeedback
    Card(
        colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.13f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("coach_david_panel")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = fb?.headline ?: "Coach David",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (voiceEnabled) "Voice + Engine Coaching Active" else "Engine Coaching (Muted)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onSpeakVoice,
                        modifier = Modifier.size(32.dp).testTag("coach_speak_btn")
                    ) {
                        Icon(
                            imageVector = if (state.isVoiceSpeaking) Icons.Filled.StopCircle else Icons.Filled.RecordVoiceOver,
                            contentDescription = if (state.isVoiceSpeaking) "Stop Coach Voice" else "Speak Coach Message",
                            tint = if (state.isVoiceSpeaking) ChampionshipGold else ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleVoice,
                        modifier = Modifier.size(32.dp).testTag("coach_voice_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (voiceEnabled) "Mute Coach Voice" else "Enable Coach Voice",
                            tint = if (voiceEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (fb != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = fb.evalFormatted,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    fb?.moveGrade?.let { grade ->
                        val c = coachClassificationColor(grade)
                        Surface(color = c.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = grade.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = c,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDisableCoach, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Coach David", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (fb != null && (fb.phaseTitle.isNotBlank() || fb.tacticalTag != null)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = fb.phaseTitle,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    fb.tacticalTag?.let { tag ->
                        Surface(color = ChampionshipGold.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampionshipGold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (state.isCoachAnalyzing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }

            Text(
                text = fb?.message ?: "Tap Hint, Top 2 Moves, Lesson, or Plan below for Coach David's spoken & written breakdown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (fb != null && fb.candidateMoves.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    fb.candidateMoves.forEach { cand ->
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = cand,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (fb?.shouldWarnMistake == true &&
                state.result == GameResult.IN_PROGRESS &&
                state.config.opponentMode != PlayOpponentMode.TOURNAMENT &&
                state.config.opponentMode != PlayOpponentMode.BLUETOOTH_FRIEND
            ) {
                FilledTonalButton(
                    onClick = onUndoMistake,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MistakeOrange.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().testTag("coach_undo_mistake_btn")
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MistakeOrange)
                    Spacer(Modifier.width(6.dp))
                    Text("Undo Blunder & Fix Move", color = MistakeOrange)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = { onQuery(CoachQueryType.HINT_BEST_MOVE) },
                    leadingIcon = { Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Hint & Arrow") },
                    modifier = Modifier.testTag("coach_hint_btn")
                )
                AssistChip(
                    onClick = { onQuery(CoachQueryType.CANDIDATE_LINES) },
                    label = { Text("Top 2 Moves") },
                    modifier = Modifier.testTag("coach_candidates_btn")
                )
                AssistChip(
                    onClick = { onQuery(CoachQueryType.ANALYZE_THREATS) },
                    label = { Text("Threats") },
                    modifier = Modifier.testTag("coach_threats_btn")
                )
                AssistChip(
                    onClick = { onQuery(CoachQueryType.STRATEGIC_PLAN) },
                    label = { Text("Plan") },
                    modifier = Modifier.testTag("coach_plan_btn")
                )
                AssistChip(
                    onClick = { onQuery(CoachQueryType.OPENING_ENDGAME_LESSON) },
                    label = { Text("Lesson") },
                    modifier = Modifier.testTag("coach_lesson_btn")
                )
                AssistChip(
                    onClick = { onQuery(CoachQueryType.EXPLAIN_LAST_MOVE) },
                    label = { Text("Last Move") },
                    modifier = Modifier.testTag("coach_review_btn")
                )
                if (state.showCoachArrow) {
                    AssistChip(
                        onClick = onHideArrow,
                        label = { Text("Hide Arrow") }
                    )
                }
            }
        }
    }
}

private fun coachClassificationColor(cls: MoveClassification): Color = when (cls) {
    MoveClassification.BRILLIANT -> BrilliantCyan
    MoveClassification.BEST -> BestGreen
    MoveClassification.EXCELLENT -> ExcellentGreen
    MoveClassification.GOOD, MoveClassification.BOOK -> GoodNeutral
    MoveClassification.INACCURACY -> InaccuracyYellow
    MoveClassification.MISTAKE -> MistakeOrange
    MoveClassification.BLUNDER -> BlunderRed
}

@Composable
private fun PlayerBannerRow(
    name: String,
    rating: Int,
    timeMs: Long,
    isActiveTurn: Boolean,
    capturedPieces: List<com.example.chess.engine.PieceType>,
    pieceColor: PieceColor,
    materialAdv: Int,
    settings: UserSettings
) {
    val isLowTime = timeMs < 20_000L && isActiveTurn
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveTurn) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "($rating)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (capturedPieces.isNotEmpty() || materialAdv > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        capturedPieces.take(8).forEach { pt ->
                            ChessPieceIcon(
                                piece = Piece(pieceColor, pt),
                                style = settings.pieceStyle,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (materialAdv > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "+$materialAdv",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
            Surface(
                color = when {
                    isLowTime -> CrimsonRed
                    isActiveTurn -> EmeraldPrimary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = CountryFlag.formatClockMs(timeMs),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (isActiveTurn || isLowTime) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}


@Composable
private fun GameControlsBar(
    state: ActiveGameState,
    onResign: () -> Unit,
    onDraw: () -> Unit,
    onTakeback: () -> Unit,
    onConditional: () -> Unit,
    onFlip: () -> Unit,
    onChat: () -> Unit,
    onSaveNow: () -> Unit,
    onRematch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (state.result == GameResult.IN_PROGRESS) {
            IconButton(onClick = onTakeback, modifier = Modifier.testTag("game_takeback_btn")) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Takeback")
            }
            if (state.config.opponentMode != PlayOpponentMode.SPECTATOR) {
                IconButton(onClick = onConditional, modifier = Modifier.testTag("conditional_move_btn")) {
                    Icon(
                        Icons.Filled.AltRoute,
                        contentDescription = "Conditional Move",
                        tint = if (state.conditionalMoves.isNotEmpty()) ChampionshipGold else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = onDraw, modifier = Modifier.testTag("game_draw_btn")) {
                Icon(Icons.Filled.Handshake, contentDescription = "Offer Draw")
            }
            IconButton(onClick = onResign, modifier = Modifier.testTag("game_resign_btn")) {
                Icon(Icons.Filled.Flag, contentDescription = "Resign")
            }
            if (state.sanHistory.isNotEmpty() && state.savedMatchId == null) {
                IconButton(onClick = onSaveNow, modifier = Modifier.testTag("game_save_now_btn")) {
                    Icon(Icons.Filled.BookmarkAdded, contentDescription = "Save Game to History")
                }
            }
        } else {
            IconButton(onClick = onRematch, modifier = Modifier.testTag("game_rematch_icon_btn")) {
                Icon(Icons.Filled.Refresh, contentDescription = "Rematch")
            }
        }
        IconButton(onClick = onFlip, modifier = Modifier.testTag("game_flip_btn")) {
            Icon(Icons.Filled.Cached, contentDescription = "Flip Board")
        }
        IconButton(onClick = onChat, modifier = Modifier.testTag("game_chat_btn")) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Match Chat")
        }
    }
}

@Composable
private fun PostGameCard(
    state: ActiveGameState,
    onRematch: () -> Unit,
    onAnalyze: () -> Unit,
    onOpenHistory: () -> Unit,
    onSharePgn: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.result.displayLabel} (${state.termination.label})",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChampionshipGold
                )
                if (state.savedMatchId != null) {
                    Surface(color = EmeraldPrimary.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text(
                text = "${state.sanHistory.size} plies · ${state.openingEco} ${state.openingName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Match Rating & Performance Summary
            if (state.whiteRatingDelta != null || state.postGamePerfRating != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("postgame_rating_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Match Rating & Performance", style = MaterialTheme.typography.titleSmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val wDelta = state.whiteRatingDelta ?: 0
                            val bDelta = state.blackRatingDelta ?: 0
                            Column {
                                Text(state.whiteName, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${state.whiteRating + wDelta} (${signedInt(wDelta)})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (wDelta >= 0) BestGreen else CrimsonRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(state.blackName, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${state.blackRating + bDelta} (${signedInt(bDelta)})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (bDelta >= 0) BestGreen else CrimsonRed
                                )
                            }
                        }
                        if (state.postGamePerfRating != null || state.postGameAccuracy != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Accuracy: ${"%.1f".format(state.postGameAccuracy ?: 0.0)}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Performance: ${state.postGamePerfRating ?: "-"}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = ChampionshipGold
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRematch,
                    modifier = Modifier.weight(1f).testTag("postgame_rematch_btn")
                ) {
                    Text("Rematch")
                }
                FilledTonalButton(
                    onClick = onAnalyze,
                    enabled = state.savedMatchId != null,
                    modifier = Modifier.weight(1f).testTag("postgame_analyze_btn")
                ) {
                    Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Analyze")
                }
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Filled.History, contentDescription = "View History")
                }
                IconButton(onClick = onSharePgn) {
                    Icon(Icons.Filled.Share, contentDescription = "Share PGN")
                }
            }
        }
    }
}

private fun signedInt(v: Int): String = if (v >= 0) "+$v" else "$v"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewGameSetupSheet(
    currentConfig: GameSetupConfig,
    settings: UserSettings,
    onStart: (GameSetupConfig) -> Unit,
    onOpenBluetoothLobby: () -> Unit,
    onSaveTimeControlPreference: (Boolean, String, Int, Int, Int) -> Unit
) {
    var mode by remember { mutableStateOf(if (currentConfig.opponentMode == PlayOpponentMode.BLUETOOTH_FRIEND) PlayOpponentMode.VS_AI else currentConfig.opponentMode) }
    var variant by remember { mutableStateOf(currentConfig.variant) }
    var selectedPreset by remember { mutableStateOf(currentConfig.timeControl) }
    var useCustomTime by remember {
        mutableStateOf(TimeControl.PRESETS.none { it.label == currentConfig.timeControl.label })
    }
    var customMinutes by remember {
        mutableIntStateOf((currentConfig.timeControl.initialSeconds / 60).coerceIn(0, 180))
    }
    var customSeconds by remember {
        mutableIntStateOf((currentConfig.timeControl.initialSeconds % 60).coerceIn(0, 59))
    }
    var customIncrement by remember {
        mutableIntStateOf(currentConfig.timeControl.incrementSeconds.coerceIn(0, 60))
    }
    var aiLevel by remember { mutableIntStateOf(currentConfig.aiSkillLevel) }
    var chosenColor by remember { mutableStateOf(currentConfig.chosenColor) }
    var customPosText by remember { mutableStateOf("") }
    var coachDavidEnabled by remember { mutableStateOf(currentConfig.coachDavidEnabled || settings.coachDavidDefault) }
    var isRated by remember { mutableStateOf(currentConfig.isRated) }
    var isBlindMode by remember { mutableStateOf(currentConfig.isBlindMode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Configure Match", style = MaterialTheme.typography.headlineMedium)

        Text("Opponent", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(PlayOpponentMode.VS_AI, PlayOpponentMode.PASS_AND_PLAY, PlayOpponentMode.SPECTATOR).forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.label) }
                )
            }
            FilledTonalButton(onClick = onOpenBluetoothLobby, modifier = Modifier.testTag("setup_bluetooth_btn")) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Bluetooth Friend")
            }
        }

        Text("Variant", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameVariant.entries.forEach { v ->
                FilterChip(
                    selected = variant == v,
                    onClick = { variant = v },
                    label = { Text(v.displayName) }
                )
            }
        }

        if (variant == GameVariant.FISCHER960) {
            OutlinedTextField(
                value = customPosText,
                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) customPosText = it },
                label = { Text("Chess960 Position # (0–959, blank = random)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TimeControlPickerSection(
            selectedPreset = selectedPreset,
            useCustomTime = useCustomTime,
            customMinutes = customMinutes,
            customSeconds = customSeconds,
            customIncrementSec = customIncrement,
            onSelectPreset = { tc ->
                useCustomTime = false
                selectedPreset = tc
            },
            onSelectCustomMode = { useCustomTime = true },
            onCustomChanged = { m, s, inc ->
                customMinutes = m
                customSeconds = s
                customIncrement = inc
                useCustomTime = true
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rated Match (Updates Elo Rating)", style = MaterialTheme.typography.titleSmall)
            Switch(checked = isRated, onCheckedChange = { isRated = it }, modifier = Modifier.testTag("setup_rated_switch"))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Blind & Audio Accessibility Mode", style = MaterialTheme.typography.titleSmall)
            Switch(checked = isBlindMode, onCheckedChange = { isBlindMode = it }, modifier = Modifier.testTag("setup_blind_switch"))
        }

        CoachDavidOptionToggle(
            enabled = coachDavidEnabled,
            onToggle = { coachDavidEnabled = it }
        )

        if (mode == PlayOpponentMode.VS_AI) {
            Text("AI Strength: Level $aiLevel (${RatingCalculator.eloForSkillLevel(aiLevel)} Elo)", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = aiLevel.toFloat(),
                onValueChange = { aiLevel = it.roundToInt().coerceIn(1, 8) },
                valueRange = 1f..8f,
                steps = 6
            )

            Text("Play As", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = chosenColor == PieceColor.WHITE, onClick = { chosenColor = PieceColor.WHITE }, label = { Text("White") })
                FilterChip(selected = chosenColor == null, onClick = { chosenColor = null }, label = { Text("Random") })
                FilterChip(selected = chosenColor == PieceColor.BLACK, onClick = { chosenColor = PieceColor.BLACK }, label = { Text("Black") })
            }
        }

        Button(
            onClick = {
                val tc = if (useCustomTime) {
                    TimeControl.custom(customMinutes, customSeconds, customIncrement)
                } else selectedPreset
                onSaveTimeControlPreference(useCustomTime, selectedPreset.label, customMinutes, customSeconds, customIncrement)
                val pos = customPosText.toIntOrNull()?.coerceIn(0, 959)
                onStart(
                    GameSetupConfig(
                        opponentMode = mode,
                        variant = variant,
                        timeControl = tc,
                        aiSkillLevel = aiLevel,
                        chosenColor = chosenColor,
                        chess960Position = pos,
                        coachDavidEnabled = coachDavidEnabled,
                        isRated = isRated,
                        isBlindMode = isBlindMode
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("start_configured_game_btn")
        ) {
            Text("Start Match")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GameChatSheet(
    state: ActiveGameState,
    safeFilterEnabled: Boolean,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Match Chat", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (safeFilterEnabled) "Safety filter active" else "Safety filter off",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close Chat") }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Good luck!", "Nice move!", "Well played!", "Thanks for the game!")) { phrase ->
                FilledTonalButton(onClick = { onSend(phrase) }) { Text(phrase) }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.chatMessages) { msg ->
                Surface(
                    color = if (msg.isSelf) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${msg.sender}: ${msg.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Send message...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun buildQuickPgn(state: ActiveGameState): String =
    ChessBoard.buildPgn(
        event = "Grandmaster Arena",
        whiteName = state.whiteName,
        blackName = state.blackName,
        whiteElo = state.whiteRating,
        blackElo = state.blackRating,
        result = state.result,
        variant = state.config.variant,
        startFen = state.startFen,
        sanMoves = state.sanHistory,
        dateStr = "2026.11.23",
        opening = "${state.openingEco} ${state.openingName}"
    )

internal fun shareText(context: android.content.Context, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share Chess Record"))
}
