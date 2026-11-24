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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chess.engine.GameVariant
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PuzzleLibrary
import com.example.chess.engine.TimeControl
import com.example.data.local.AnnouncementEntity
import com.example.data.local.ClubMemberEntity
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.ModerationReportEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.TournamentEntity
import com.example.ui.components.CountryFlag
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.GameSetupConfig
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayOpponentMode
import java.time.ZoneId
import java.util.TimeZone

@Composable
fun CompeteScreen(
    mainViewModel: MainViewModel,
    profile: PlayerProfileEntity,
    settings: com.example.data.preferences.UserSettings = com.example.data.preferences.UserSettings(),
    tournaments: List<TournamentEntity>,
    clubMembers: List<ClubMemberEntity>,
    reports: List<ModerationReportEntity>,
    announcements: List<AnnouncementEntity>,
    matches: List<MatchHistoryEntity>,
    onStartTournamentOrChallengeGame: (GameSetupConfig) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tournaments") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Rankings") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Social") })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Admin") })
        }

        when (tab) {
            0 -> TournamentsTab(
                mainViewModel = mainViewModel,
                profile = profile,
                tournaments = tournaments,
                onPlayTournamentMatch = onStartTournamentOrChallengeGame
            )
            1 -> RankingsTab(
                profile = profile,
                clubMembers = clubMembers,
                onChallengePlayer = { member ->
                    onStartTournamentOrChallengeGame(
                        GameSetupConfig(
                            opponentMode = PlayOpponentMode.VS_AI,
                            timeControl = settings.effectiveTimeControl(),
                            aiSkillLevel = skillForRating(member.rapidRating),
                            customOpponentName = member.username,
                            customOpponentRating = member.rapidRating,
                            coachDavidEnabled = settings.coachDavidDefault
                        )
                    )
                }
            )
            2 -> SocialTab(
                mainViewModel = mainViewModel,
                clubMembers = clubMembers,
                onChallengePlayer = { member ->
                    onStartTournamentOrChallengeGame(
                        GameSetupConfig(
                            opponentMode = PlayOpponentMode.VS_AI,
                            timeControl = settings.effectiveTimeControl(),
                            aiSkillLevel = skillForRating(member.rapidRating),
                            customOpponentName = member.username,
                            customOpponentRating = member.rapidRating,
                            coachDavidEnabled = settings.coachDavidDefault
                        )
                    )
                }
            )
            else -> AdminTab(
                mainViewModel = mainViewModel,
                clubMembers = clubMembers,
                reports = reports,
                announcements = announcements,
                tournaments = tournaments,
                matches = matches
            )
        }
    }
}

@Composable
private fun TournamentsTab(
    mainViewModel: MainViewModel,
    profile: PlayerProfileEntity,
    tournaments: List<TournamentEntity>,
    onPlayTournamentMatch: (GameSetupConfig) -> Unit
) {
    var selectedTournamentId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val selectedTournament = tournaments.firstOrNull { it.id == selectedTournamentId } ?: tournaments.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = ElectricBlue.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Swiss & Arena Tournaments", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Local Club Mode · Real Swiss pairings & Buchholz tiebreaks · Zone: ${ZoneId.systemDefault().id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_tournament_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Create")
                    }
                }
            }
        }

        items(tournaments) { t ->
            val isSelected = selectedTournament?.id == t.id
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { selectedTournamentId = t.id }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${t.systemType} · ${if (t.variant == GameVariant.FISCHER960.name) "Chess960" else "Standard"} · ${t.timeControlLabel} · Round ${t.currentRound}/${t.maxRounds}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Prize: ${t.prizeText}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampionshipGold
                            )
                        }
                        Surface(
                            color = when (t.status) {
                                "ACTIVE" -> EmeraldPrimary.copy(alpha = 0.2f)
                                "COMPLETED" -> ChampionshipGold.copy(alpha = 0.2f)
                                else -> ElectricBlue.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = t.status,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (t.status != "COMPLETED") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!t.isRegistered) {
                                Button(
                                    onClick = {
                                        selectedTournamentId = t.id
                                        mainViewModel.toggleTournamentRegistration(t.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) { Text("Register & Start") }
                            } else {
                                OutlinedButton(onClick = { mainViewModel.toggleTournamentRegistration(t.id) }) {
                                    Text("Withdraw")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTournament != null) {
            item {
                TournamentDetailSection(
                    tournament = selectedTournament,
                    profile = profile,
                    mainViewModel = mainViewModel,
                    onPlayRound = onPlayTournamentMatch
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateTournamentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, sys, variant, tc, rounds ->
                mainViewModel.createTournament(title, sys, variant, tc, rounds)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TournamentDetailSection(
    tournament: TournamentEntity,
    profile: PlayerProfileEntity,
    mainViewModel: MainViewModel,
    onPlayRound: (GameSetupConfig) -> Unit
) {
    val participants by mainViewModel.observeTournamentParticipants(tournament.id).collectAsStateWithLifecycle(emptyList())
    val matches by mainViewModel.observeTournamentMatches(tournament.id).collectAsStateWithLifecycle(emptyList())

    val humanPendingMatch = matches.firstOrNull {
        it.roundNumber == tournament.currentRound &&
            it.result == "PENDING" &&
            (it.whitePlayer == profile.username || it.blackPlayer == profile.username)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Standings · ${tournament.title}", style = MaterialTheme.typography.titleMedium)
                if (humanPendingMatch != null && tournament.status == "ACTIVE") {
                    Button(
                        onClick = {
                            val isWhite = humanPendingMatch.whitePlayer == profile.username
                            val oppName = if (isWhite) humanPendingMatch.blackPlayer else humanPendingMatch.whitePlayer
                            val oppRating = if (isWhite) humanPendingMatch.blackRating else humanPendingMatch.whiteRating
                            val tc = TimeControl.fromLabel(tournament.timeControlLabel)
                            val variant = if (tournament.variant == GameVariant.FISCHER960.name) GameVariant.FISCHER960 else GameVariant.STANDARD
                            onPlayRound(
                                GameSetupConfig(
                                    opponentMode = PlayOpponentMode.TOURNAMENT,
                                    variant = variant,
                                    timeControl = tc,
                                    aiSkillLevel = skillForRating(oppRating),
                                    chosenColor = if (isWhite) PieceColor.WHITE else PieceColor.BLACK,
                                    customOpponentName = oppName,
                                    customOpponentRating = oppRating,
                                    tournamentId = tournament.id,
                                    tournamentMatchId = humanPendingMatch.id
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.testTag("play_tournament_round_btn")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play Rd ${tournament.currentRound}")
                    }
                }
            }

            participants.forEachIndexed { idx, p ->
                val ptsStr = if (p.pointsTimesTwo % 2 == 0) "${p.pointsTimesTwo / 2}" else "${p.pointsTimesTwo / 2}.5"
                val bhStr = if (p.buchholzTimesTwo % 2 == 0) "${p.buchholzTimesTwo / 2}" else "${p.buchholzTimesTwo / 2}.5"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${idx + 1}.", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(24.dp))
                        Text("${CountryFlag.emoji(p.countryCode)} ${p.playerName}", style = MaterialTheme.typography.bodyLarge)
                        if (p.isHuman) {
                            Spacer(Modifier.width(6.dp))
                            Text("(You)", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                        }
                    }
                    Text(
                        text = "$ptsStr pts · BH $bhStr",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = ChampionshipGold
                    )
                }
            }

            if (matches.isNotEmpty()) {
                Text("Round ${tournament.currentRound} Pairings", style = MaterialTheme.typography.titleMedium)
                matches.filter { it.roundNumber == tournament.currentRound }.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${m.whitePlayer} vs ${m.blackPlayer}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = when (m.result) {
                                "WHITE_WIN" -> "1-0"
                                "BLACK_WIN" -> "0-1"
                                "DRAW" -> "½-½"
                                else -> "Live"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTournamentDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, GameVariant, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("Grandmaster Cup") }
    var system by remember { mutableStateOf("SWISS") }
    var variant by remember { mutableStateOf(GameVariant.STANDARD) }
    var tc by remember { mutableStateOf("5+0") }
    var rounds by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tournament") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = system == "SWISS", onClick = { system = "SWISS" }, label = { Text("Swiss") })
                    FilterChip(selected = system == "ARENA", onClick = { system = "ARENA" }, label = { Text("Arena") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = variant == GameVariant.STANDARD, onClick = { variant = GameVariant.STANDARD }, label = { Text("Standard") })
                    FilterChip(selected = variant == GameVariant.FISCHER960, onClick = { variant = GameVariant.FISCHER960 }, label = { Text("Chess960") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("3+2", "5+0", "10+0").forEach { preset ->
                        FilterChip(selected = tc == preset, onClick = { tc = preset }, label = { Text(preset) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 7).forEach { r ->
                        FilterChip(selected = rounds == r, onClick = { rounds = r }, label = { Text("$r Rds") })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(title, system, variant, tc, rounds) }) { Text("Launch") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RankingsTab(
    profile: PlayerProfileEntity,
    clubMembers: List<ClubMemberEntity>,
    onChallengePlayer: (ClubMemberEntity) -> Unit
) {
    var formatFilter by remember { mutableStateOf("RAPID") }
    var periodFilter by remember { mutableStateOf("ALL_TIME") }
    var countryFilter by remember { mutableStateOf("WORLD") }

    val availableCountries = remember(clubMembers) {
        listOf("WORLD") + clubMembers.map { it.countryCode }.distinct().sorted()
    }

    val filtered = remember(clubMembers, formatFilter, periodFilter, countryFilter) {
        clubMembers.filter { !it.isBanned }
            .filter { countryFilter == "WORLD" || it.countryCode == countryFilter }
            .sortedByDescending { m ->
                when (periodFilter) {
                    "WEEKLY" -> m.weeklyGain
                    "MONTHLY" -> m.monthlyGain
                    else -> when (formatFilter) {
                        "BULLET" -> m.bulletRating
                        "BLITZ" -> m.blitzRating
                        "CLASSICAL" -> m.classicalRating
                        "FISCHER960" -> m.fischerRating
                        "PUZZLES" -> m.puzzleRating
                        else -> m.rapidRating
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("BULLET", "BLITZ", "RAPID", "CLASSICAL", "FISCHER960", "PUZZLES")) { fmt ->
                    FilterChip(
                        selected = formatFilter == fmt,
                        onClick = { formatFilter = fmt },
                        label = { Text(fmt.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ALL_TIME" to "World Rating", "WEEKLY" to "Weekly Gain", "MONTHLY" to "Monthly Gain")) { (k, lbl) ->
                    FilterChip(selected = periodFilter == k, onClick = { periodFilter = k }, label = { Text(lbl) })
                }
                items(availableCountries) { code ->
                    FilterChip(
                        selected = countryFilter == code,
                        onClick = { countryFilter = code },
                        label = { Text(if (code == "WORLD") "Global" else "${CountryFlag.emoji(code)} $code") }
                    )
                }
            }
        }

        itemsIndexed(filtered) { idx, m ->
            val scoreVal = when (periodFilter) {
                "WEEKLY" -> "+${m.weeklyGain}"
                "MONTHLY" -> "+${m.monthlyGain}"
                else -> when (formatFilter) {
                    "BULLET" -> "${m.bulletRating}"
                    "BLITZ" -> "${m.blitzRating}"
                    "CLASSICAL" -> "${m.classicalRating}"
                    "FISCHER960" -> "${m.fischerRating}"
                    "PUZZLES" -> "${m.puzzleRating}"
                    else -> "${m.rapidRating}"
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (idx < 3) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(34.dp)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(color = ChampionshipGold.copy(alpha = 0.18f), shape = RoundedCornerShape(4.dp)) {
                                    Text(m.title, style = MaterialTheme.typography.labelSmall, color = ChampionshipGold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                                Text("${CountryFlag.emoji(m.countryCode)} ${m.username}", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "${m.status} · ${m.gamesPlayed} games",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = scoreVal,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = EmeraldPrimary
                        )
                        FilledTonalButton(onClick = { onChallengePlayer(m) }) {
                            Text("Play")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialTab(
    mainViewModel: MainViewModel,
    clubMembers: List<ClubMemberEntity>,
    onChallengePlayer: (ClubMemberEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<ClubMemberEntity?>(null) }

    val activeRoster = remember(clubMembers, search) {
        clubMembers.filter { !it.isBanned && (search.isBlank() || it.username.contains(search, ignoreCase = true)) }
    }
    val requests = remember(clubMembers) { clubMembers.filter { it.isFriendRequestPending && !it.isBanned } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search players") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (requests.isNotEmpty()) {
            item {
                Text("Friend Requests (${requests.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(requests) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${CountryFlag.emoji(req.countryCode)} ${req.username} (${req.rapidRating})", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { mainViewModel.acceptFriendRequest(req.id) }) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Accept")
                        }
                    }
                }
            }
        }

        item {
            Text("International Club Roster", style = MaterialTheme.typography.titleMedium)
        }
        items(activeRoster) { m ->
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (m.status) {
                                            "ONLINE" -> EmeraldPrimary
                                            "IN_GAME" -> ChampionshipGold
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${CountryFlag.emoji(m.countryCode)} ${m.username}", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Rapid ${m.rapidRating}", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(onClick = { onChallengePlayer(m) }) {
                            Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Challenge")
                        }
                        OutlinedButton(onClick = { mainViewModel.toggleFriend(m.id) }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (m.isFriend) "Friend" else "Add")
                        }
                        IconButton(onClick = { mainViewModel.toggleFollow(m.id) }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Follow Player",
                                tint = if (m.isFollowing) ChampionshipGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { reportTarget = m }) {
                            Icon(Icons.Filled.Report, contentDescription = "Report Player", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { target ->
        var reason by remember { mutableStateOf("Engine Assistance Suspicion") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report ${target.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Fair-play reports are reviewed cautiously alongside multiple games; no player is flagged automatically on single signals.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    listOf("Engine Assistance Suspicion", "Stalling / Sandbagging", "Abusive Chat").forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) })
                    }
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Details (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    mainViewModel.submitReport(target.username, reason, details)
                    reportTarget = null
                }) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminTab(
    mainViewModel: MainViewModel,
    clubMembers: List<ClubMemberEntity>,
    reports: List<ModerationReportEntity>,
    announcements: List<AnnouncementEntity>,
    tournaments: List<TournamentEntity>,
    matches: List<MatchHistoryEntity>
) {
    var annTitle by remember { mutableStateOf("") }
    var annBody by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = ChampionshipGold)
                Spacer(Modifier.width(8.dp))
                Text("Administrator & Moderation Console", style = MaterialTheme.typography.titleLarge)
            }
        }

        // System Health & Platform Stats
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("System Health & Metrics", style = MaterialTheme.typography.titleMedium)
                    Text("Database: Room SQLite (Healthy) · Daily Puzzles: ${PuzzleLibrary.ALL_PUZZLES.size} verified", style = MaterialTheme.typography.bodyMedium)
                    Text("Club Players: ${clubMembers.size} · Matches Saved: ${matches.size} · Tournaments: ${tournaments.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("Timezone: ${TimeZone.getDefault().id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Publish Announcement
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Publish Platform Announcement", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Headline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = annBody, onValueChange = { annBody = it }, label = { Text("Announcement body") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (annTitle.isNotBlank() && annBody.isNotBlank()) {
                                mainViewModel.publishAnnouncement(annTitle, annBody, "HIGH")
                                annTitle = ""
                                annBody = ""
                            }
                        }
                    ) { Text("Publish") }
                }
            }
        }

        if (announcements.isNotEmpty()) {
            item { Text("Active Announcements (${announcements.size})", style = MaterialTheme.typography.titleMedium) }
            items(announcements) { ann ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ann.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { mainViewModel.deleteAnnouncement(ann.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Announcement")
                        }
                    }
                }
            }
        }

        // Fair-Play Reports Queue
        item {
            Text("Fair-Play Moderation Reports (${reports.size})", style = MaterialTheme.typography.titleMedium)
            if (reports.isEmpty()) {
                Text("No pending reports.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(reports) { rep ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${rep.targetUsername} · ${rep.reason}", style = MaterialTheme.typography.titleMedium)
                        Text(rep.status, style = MaterialTheme.typography.labelSmall, color = ChampionshipGold)
                    }
                    if (rep.details.isNotBlank()) {
                        Text(rep.details, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (rep.status == "PENDING") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { mainViewModel.updateReportStatus(rep, "RESOLVED") }) { Text("Resolve") }
                            OutlinedButton(onClick = { mainViewModel.updateReportStatus(rep, "DISMISSED") }) { Text("Dismiss") }
                        }
                    }
                }
            }
        }

        // Account Moderation
        item {
            Text("Account Moderation", style = MaterialTheme.typography.titleMedium)
        }
        items(clubMembers) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${m.username} (${m.rapidRating})" + if (m.isBanned) " [SUSPENDED]" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (m.isBanned) CrimsonRed else MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(onClick = { mainViewModel.toggleBanMember(m.id) }) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (m.isBanned) "Restore" else "Suspend")
                    }
                }
            }
        }
    }
}

private fun skillForRating(rating: Int): Int = when {
    rating < 800 -> 1
    rating < 1050 -> 2
    rating < 1300 -> 3
    rating < 1550 -> 4
    rating < 1800 -> 5
    rating < 2050 -> 6
    rating < 2250 -> 7
    else -> 8
}
