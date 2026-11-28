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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.chess.engine.Chess960
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.GameVariant
import com.example.chess.engine.TimeControl
import com.example.data.local.ClubMemberEntity
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChessBoardView
import com.example.ui.components.CoachDavidOptionToggle
import com.example.ui.components.CountryFlag
import com.example.ui.components.TimeControlPickerSection
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.GameSetupConfig
import com.example.ui.viewmodel.PlayOpponentMode
import kotlin.math.roundToInt

@Composable
fun FischerChessScreen(
    profile: PlayerProfileEntity,
    fischerMatches: List<MatchHistoryEntity>,
    clubMembers: List<ClubMemberEntity>,
    settings: UserSettings,
    onStartFischerGame: (GameSetupConfig) -> Unit,
    onAnalyzePositionFen: (String, Int) -> Unit,
    onOpenMatchAnalysis: (Long) -> Unit,
    onOpenBluetoothLobby: () -> Unit = {},
    onSaveTimeControlPreference: (Boolean, String, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onSetCoachDavidDefault: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var positionNumber by remember { mutableIntStateOf(Chess960.randomPositionNumber()) }
    var posInput by remember { mutableStateOf(positionNumber.toString()) }

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

    val previewBoard = remember(positionNumber) { ChessBoard.fischer960(positionNumber) }
    val backRank = remember(positionNumber) { Chess960.backRankForPosition(positionNumber) }
    val fen = remember(positionNumber) { Chess960.startingFen(positionNumber) }

    val total960 = profile.fischerWins + profile.fischerLosses + profile.fischerDraws
    val leaderboard960 = remember(clubMembers) {
        clubMembers.filter { !it.isBanned }.sortedByDescending { it.fischerRating }.take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fischer Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_fischer_banner),
                    contentDescription = "Fischer Chess 960 Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x44070B12), Color(0xE6070B12))))
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = ElectricBlue.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "OFFICIAL CHESS960 · SCHARNAGL 0–959",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Column {
                        Text("FISCHER CHESS", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text(
                            text = "960 starting setups · Official castling rules · Dedicated Rating ${profile.fischerRating}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // Dedicated 960 Stats Strip
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatMini("960 Rating", "${profile.fischerRating}", ChampionshipGold)
                    StatMini("Peak", "${profile.fischerPeak}", EmeraldPrimary)
                    StatMini("Matches", "$total960", MaterialTheme.colorScheme.onSurface)
                    StatMini("W / L / D", "${profile.fischerWins}/${profile.fischerLosses}/${profile.fischerDraws}", ElectricBlue)
                }
            }
        }

        // Interactive 960 Position Explorer + Custom Timing & Coach David
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Position #$positionNumber", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Back Rank: $backRank",
                                style = MaterialTheme.typography.labelMedium,
                                color = ChampionshipGold
                            )
                        }
                        Row {
                            FilledTonalButton(
                                onClick = {
                                    val r = Chess960.randomPositionNumber()
                                    positionNumber = r
                                    posInput = r.toString()
                                },
                                modifier = Modifier.testTag("fischer_random_btn")
                            ) {
                                Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Random")
                            }
                            IconButton(
                                onClick = {
                                    shareText(
                                        context,
                                        "Fischer Chess (Chess960) Position #$positionNumber\nBack Rank: $backRank\nFEN: $fen"
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share Chess960 Position")
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ChessBoardView(
                            board = previewBoard,
                            boardTheme = settings.boardTheme,
                            pieceStyle = settings.pieceStyle,
                            flipped = false,
                            showCoordinates = settings.showCoordinates,
                            selectedSquare = null,
                            legalMovesForSelected = emptyList(),
                            lastMoveFrom = null,
                            lastMoveTo = null,
                            boardPattern = settings.boardPattern,
                            interactive = false,
                            modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Slider(
                            value = positionNumber.toFloat(),
                            onValueChange = {
                                val v = it.roundToInt().coerceIn(0, 959)
                                positionNumber = v
                                posInput = v.toString()
                            },
                            valueRange = 0f..959f,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = posInput,
                            onValueChange = { raw ->
                                if (raw.length <= 3 && raw.all(Char::isDigit)) {
                                    posInput = raw
                                    raw.toIntOrNull()?.let { num ->
                                        if (num in 0..959) positionNumber = num
                                    }
                                }
                            },
                            label = { Text("#") },
                            singleLine = true,
                            modifier = Modifier.width(84.dp)
                        )
                    }

                    Text(
                        text = "X-FEN: $fen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onStartFischerGame(
                                    GameSetupConfig(
                                        opponentMode = PlayOpponentMode.VS_AI,
                                        variant = GameVariant.FISCHER960,
                                        timeControl = activeTimeControl,
                                        chess960Position = positionNumber,
                                        coachDavidEnabled = coachDavidEnabled
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("fischer_play_ai_btn")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play vs AI")
                        }
                        FilledTonalButton(
                            onClick = {
                                onStartFischerGame(
                                    GameSetupConfig(
                                        opponentMode = PlayOpponentMode.PASS_AND_PLAY,
                                        variant = GameVariant.FISCHER960,
                                        timeControl = activeTimeControl,
                                        chess960Position = positionNumber,
                                        coachDavidEnabled = coachDavidEnabled
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("vs Friend")
                        }
                        IconButton(onClick = onOpenBluetoothLobby) {
                            Icon(Icons.Filled.Bluetooth, contentDescription = "Play Chess960 via Bluetooth")
                        }
                        IconButton(onClick = { onAnalyzePositionFen(fen, positionNumber) }) {
                            Icon(Icons.Filled.Analytics, contentDescription = "Analyze 960 Position")
                        }
                    }
                }
            }
        }

        // Dedicated Fischer Chess Leaderboard
        item {
            Text("Fischer Chess Leaderboard", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leaderboard960.forEachIndexed { idx, m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (idx < 3) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                Text("${CountryFlag.emoji(m.countryCode)} ", style = MaterialTheme.typography.bodyLarge)
                                Text(m.username, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "${m.fischerRating}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // Fischer960 Match History
        item {
            Text("Your Chess960 Match History", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (fischerMatches.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "No Chess960 games played yet. Launch Position #$positionNumber above to record your first match!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        itemsIndexed(fischerMatches.take(10)) { _, m ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { onOpenMatchAnalysis(m.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${m.whiteName} vs ${m.blackName}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pos #${m.chess960Position} · ${m.timeControlLabel} · ${m.moveCount} moves",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = when (m.result) {
                            "WHITE_WIN" -> "1-0"
                            "BLACK_WIN" -> "0-1"
                            "DRAW" -> "½-½"
                            else -> "*"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
