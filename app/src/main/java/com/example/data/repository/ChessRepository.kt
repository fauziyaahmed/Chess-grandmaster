package com.example.data.repository

import com.example.chess.engine.GameResult
import com.example.chess.engine.GameVariant
import com.example.chess.engine.PieceColor
import com.example.chess.engine.PuzzleDifficulty
import com.example.chess.engine.TimeControlCategory
import com.example.data.AchievementItem
import com.example.data.RatingCalculator
import com.example.data.local.AnnouncementEntity
import com.example.data.local.ChessDao
import com.example.data.local.ClubMemberEntity
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.ModerationReportEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.local.RatingSnapshotEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.TournamentMatchEntity
import com.example.data.local.TournamentParticipantEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.roundToInt

class ChessRepository(private val dao: ChessDao) {

    val profileFlow: Flow<PlayerProfileEntity?> = dao.observeProfile()
    val allMatchesFlow: Flow<List<MatchHistoryEntity>> = dao.observeAllMatches()
    val fischerMatchesFlow: Flow<List<MatchHistoryEntity>> = dao.observeFischerMatches()
    val ratingSnapshotsFlow: Flow<List<RatingSnapshotEntity>> = dao.observeRatingSnapshots()
    val puzzleAttemptsFlow: Flow<List<PuzzleAttemptEntity>> = dao.observePuzzleAttempts()
    val tournamentsFlow: Flow<List<TournamentEntity>> = dao.observeTournaments()
    val clubMembersFlow: Flow<List<ClubMemberEntity>> = dao.observeClubMembers()
    val reportsFlow: Flow<List<ModerationReportEntity>> = dao.observeReports()
    val announcementsFlow: Flow<List<AnnouncementEntity>> = dao.observeAnnouncements()

    fun observeTournamentParticipants(tournamentId: Long): Flow<List<TournamentParticipantEntity>> =
        dao.observeTournamentParticipants(tournamentId)

    fun observeTournamentMatches(tournamentId: Long): Flow<List<TournamentMatchEntity>> =
        dao.observeTournamentMatches(tournamentId)

    suspend fun getMatchById(matchId: Long): MatchHistoryEntity? = dao.getMatchById(matchId)

    suspend fun ensureSeeded() {
        val existing = dao.getProfileOnce()
        if (existing == null) {
            dao.upsertProfile(PlayerProfileEntity())
            val now = System.currentTimeMillis()
            listOf("BULLET", "BLITZ", "RAPID", "CLASSICAL", "FISCHER960", "PUZZLE").forEach { fmt ->
                dao.insertRatingSnapshot(RatingSnapshotEntity(formatKey = fmt, rating = 1200, timestampMs = now))
            }
        }
        val members = dao.observeClubMembers().first()
        if (members.isEmpty()) {
            dao.insertClubMembers(defaultClubRoster())
        }
        val tournaments = dao.observeTournaments().first()
        if (tournaments.isEmpty()) {
            seedDefaultTournaments()
        }
        val announcements = dao.observeAnnouncements().first()
        if (announcements.isEmpty()) {
            dao.insertAnnouncement(
                AnnouncementEntity(
                    title = "Grandmaster Autumn Swiss & Chess960 Championship",
                    body = "Welcome to Grandmaster! Compete in Standard & Fischer Random (Chess960) tournaments, solve daily tactical puzzles, and analyze every match with our deep alpha-beta engine.",
                    priority = "HIGH"
                )
            )
            dao.insertAnnouncement(
                AnnouncementEntity(
                    title = "Local Club Mode Active",
                    body = "Cloud multiplayer sync is currently offline. All ratings, match history, tournaments, PGN archives, and puzzles are persisted locally on your device.",
                    priority = "NORMAL"
                )
            )
        }
    }

    suspend fun updateProfileDetails(username: String, countryCode: String, bio: String) {
        val cur = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            cur.copy(
                username = username.trim().ifBlank { cur.username }.take(28),
                countryCode = countryCode.uppercase().take(2),
                bio = bio.trim().take(160)
            )
        )
    }

    suspend fun claimDailyLoginReward(): Int? {
        val cur = dao.getProfileOnce() ?: return null
        val todayEpoch = LocalDate.now().toEpochDay()
        if (cur.lastDailyClaimEpochDay == todayEpoch) return null
        val newStreak = if (cur.lastDailyClaimEpochDay == todayEpoch - 1L) cur.dailyLoginStreak + 1 else 1
        val rewardXp = 50 + (newStreak.coerceAtMost(7) * 15)
        dao.upsertProfile(
            cur.copy(
                xp = cur.xp + rewardXp,
                dailyLoginStreak = newStreak,
                lastDailyClaimEpochDay = todayEpoch
            )
        )
        return rewardXp
    }

    /**
     * Records a completed game, updates exact ratings & streaks, and returns the saved MatchHistoryEntity ID.
     */
    suspend fun recordCompletedMatch(
        whiteName: String,
        blackName: String,
        opponentRating: Int,
        playerSide: PieceColor?, // null = Pass & Play (both local)
        variant: GameVariant,
        category: TimeControlCategory,
        timeControlLabel: String,
        chess960Position: Int,
        result: GameResult,
        termination: String,
        openingEco: String,
        openingName: String,
        movesSan: List<String>,
        startFen: String,
        finalFen: String,
        durationSeconds: Long,
        accuracyPercent: Double,
        pgnText: String,
        isRated: Boolean = true
    ): Long {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val formatKey = if (variant == GameVariant.FISCHER960) "FISCHER960" else category.name

        val currentRating = when (formatKey) {
            "BULLET" -> profile.bulletRating
            "BLITZ" -> profile.blitzRating
            "RAPID" -> profile.rapidRating
            "CLASSICAL" -> profile.classicalRating
            else -> profile.fischerRating
        }
        val formatGames = when (formatKey) {
            "BULLET" -> profile.bulletWins + profile.bulletLosses + profile.bulletDraws
            "BLITZ" -> profile.blitzWins + profile.blitzLosses + profile.blitzDraws
            "RAPID" -> profile.rapidWins + profile.rapidLosses + profile.rapidDraws
            "CLASSICAL" -> profile.classicalWins + profile.classicalLosses + profile.classicalDraws
            else -> profile.fischerWins + profile.fischerLosses + profile.fischerDraws
        }

        val playerScore = when {
            result == GameResult.IN_PROGRESS -> null
            playerSide == null -> null
            result == GameResult.DRAW -> 0.5
            result == GameResult.WHITE_WIN && playerSide == PieceColor.WHITE -> 1.0
            result == GameResult.BLACK_WIN && playerSide == PieceColor.BLACK -> 1.0
            else -> 0.0
        }

        val newRating = if (isRated && playerScore != null) {
            RatingCalculator.calculateNewRating(currentRating, opponentRating, playerScore, formatGames)
        } else {
            currentRating
        }

        val whiteBefore = if (playerSide == PieceColor.WHITE) currentRating else opponentRating
        val whiteAfter = if (playerSide == PieceColor.WHITE) newRating else opponentRating
        val blackBefore = if (playerSide == PieceColor.BLACK) currentRating else opponentRating
        val blackAfter = if (playerSide == PieceColor.BLACK) newRating else opponentRating

        val matchId = dao.insertMatch(
            MatchHistoryEntity(
                whiteName = whiteName,
                blackName = blackName,
                whiteRatingBefore = whiteBefore,
                whiteRatingAfter = whiteAfter,
                blackRatingBefore = blackBefore,
                blackRatingAfter = blackAfter,
                playerSide = playerSide?.name ?: "BOTH",
                variant = variant.name,
                category = category.name,
                timeControlLabel = timeControlLabel,
                chess960Position = chess960Position,
                result = result.name,
                termination = termination,
                openingEco = openingEco,
                openingName = openingName,
                movesSanCsv = movesSan.joinToString("|"),
                startFen = startFen,
                finalFen = finalFen,
                moveCount = (movesSan.size + 1) / 2,
                durationSeconds = durationSeconds,
                accuracyPercent = accuracyPercent,
                pgnText = pgnText
            )
        )

        if (playerSide != null && playerScore != null) {
            val isWin = playerScore == 1.0
            val isLoss = playerScore == 0.0
            val isDraw = playerScore == 0.5
            val xpGain = when {
                isWin -> 45
                isDraw -> 25
                else -> 15
            }
            val nextWinStreak = if (isWin) profile.currentWinStreak + 1 else if (isLoss) 0 else profile.currentWinStreak
            val bestWinStreak = maxOf(profile.longestWinStreak, nextWinStreak)

            val updatedProfile = when (formatKey) {
                "BULLET" -> profile.copy(
                    bulletRating = newRating,
                    bulletPeak = maxOf(profile.bulletPeak, newRating),
                    bulletWins = profile.bulletWins + if (isWin) 1 else 0,
                    bulletLosses = profile.bulletLosses + if (isLoss) 1 else 0,
                    bulletDraws = profile.bulletDraws + if (isDraw) 1 else 0
                )
                "BLITZ" -> profile.copy(
                    blitzRating = newRating,
                    blitzPeak = maxOf(profile.blitzPeak, newRating),
                    blitzWins = profile.blitzWins + if (isWin) 1 else 0,
                    blitzLosses = profile.blitzLosses + if (isLoss) 1 else 0,
                    blitzDraws = profile.blitzDraws + if (isDraw) 1 else 0
                )
                "RAPID" -> profile.copy(
                    rapidRating = newRating,
                    rapidPeak = maxOf(profile.rapidPeak, newRating),
                    rapidWins = profile.rapidWins + if (isWin) 1 else 0,
                    rapidLosses = profile.rapidLosses + if (isLoss) 1 else 0,
                    rapidDraws = profile.rapidDraws + if (isDraw) 1 else 0
                )
                "CLASSICAL" -> profile.copy(
                    classicalRating = newRating,
                    classicalPeak = maxOf(profile.classicalPeak, newRating),
                    classicalWins = profile.classicalWins + if (isWin) 1 else 0,
                    classicalLosses = profile.classicalLosses + if (isLoss) 1 else 0,
                    classicalDraws = profile.classicalDraws + if (isDraw) 1 else 0
                )
                else -> profile.copy(
                    fischerRating = newRating,
                    fischerPeak = maxOf(profile.fischerPeak, newRating),
                    fischerWins = profile.fischerWins + if (isWin) 1 else 0,
                    fischerLosses = profile.fischerLosses + if (isLoss) 1 else 0,
                    fischerDraws = profile.fischerDraws + if (isDraw) 1 else 0
                )
            }.copy(
                xp = profile.xp + xpGain,
                totalPlayTimeSeconds = profile.totalPlayTimeSeconds + durationSeconds,
                currentWinStreak = nextWinStreak,
                longestWinStreak = bestWinStreak
            )
            dao.upsertProfile(updatedProfile)
            if (isRated) {
                dao.insertRatingSnapshot(RatingSnapshotEntity(formatKey = formatKey, rating = newRating))
            }
        } else {
            dao.upsertProfile(
                profile.copy(
                    xp = profile.xp + 20,
                    totalPlayTimeSeconds = profile.totalPlayTimeSeconds + durationSeconds
                )
            )
        }
        return matchId
    }

    suspend fun recordPuzzleResult(
        puzzleId: String,
        puzzleTitle: String,
        difficulty: PuzzleDifficulty,
        puzzleRating: Int,
        isDaily: Boolean,
        solved: Boolean,
        hintsUsed: Int
    ): Int {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val dateKey = LocalDate.now().toString()
        val score = if (solved) if (hintsUsed > 0) 0.7 else 1.0 else 0.0
        val newRating = RatingCalculator.calculateNewRating(
            playerRating = profile.puzzleRating,
            opponentRating = puzzleRating,
            score = score,
            gamesPlayed = profile.puzzlesAttempted
        )
        val nextStreak = if (solved && hintsUsed == 0) profile.puzzleStreak + 1 else if (!solved) 0 else profile.puzzleStreak
        val bonusXp = if (solved) (if (isDaily) 60 else 30) else 5

        dao.insertPuzzleAttempt(
            PuzzleAttemptEntity(
                puzzleId = puzzleId,
                puzzleTitle = puzzleTitle,
                difficulty = difficulty.label,
                dateKey = dateKey,
                isDailyChallenge = isDaily,
                solved = solved,
                hintsUsed = hintsUsed,
                ratingBefore = profile.puzzleRating,
                ratingAfter = newRating
            )
        )
        dao.upsertProfile(
            profile.copy(
                puzzleRating = newRating,
                puzzlePeak = maxOf(profile.puzzlePeak, newRating),
                puzzlesSolved = profile.puzzlesSolved + if (solved) 1 else 0,
                puzzlesAttempted = profile.puzzlesAttempted + 1,
                puzzleStreak = nextStreak,
                bestPuzzleStreak = maxOf(profile.bestPuzzleStreak, nextStreak),
                xp = profile.xp + bonusXp
            )
        )
        dao.insertRatingSnapshot(RatingSnapshotEntity(formatKey = "PUZZLE", rating = newRating))
        return newRating - profile.puzzleRating
    }

    // Tournaments
    suspend fun createTournament(
        title: String,
        systemType: String,
        variant: GameVariant,
        timeControlLabel: String,
        maxRounds: Int
    ): Long {
        val id = dao.insertTournament(
            TournamentEntity(
                title = title.trim().ifBlank { "Grandmaster Open" },
                systemType = systemType,
                variant = variant.name,
                timeControlLabel = timeControlLabel,
                maxRounds = maxRounds.coerceIn(3, 9),
                currentRound = 0,
                status = "UPCOMING",
                scheduleText = "Starts on registration",
                prizeText = "Championship Trophy + 250 XP",
                isRegistered = true
            )
        )
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val roster = dao.observeClubMembers().first().filter { !it.isBanned }.shuffled().take(7)
        val participants = mutableListOf(
            TournamentParticipantEntity(
                tournamentId = id,
                playerName = profile.username,
                countryCode = profile.countryCode,
                rating = if (variant == GameVariant.FISCHER960) profile.fischerRating else profile.rapidRating,
                isHuman = true
            )
        )
        roster.forEach { m ->
            participants.add(
                TournamentParticipantEntity(
                    tournamentId = id,
                    playerName = m.username,
                    countryCode = m.countryCode,
                    rating = if (variant == GameVariant.FISCHER960) m.fischerRating else m.rapidRating,
                    isHuman = false
                )
            )
        }
        dao.insertParticipants(participants)
        startOrAdvanceTournamentRound(id)
        return id
    }

    suspend fun toggleTournamentRegistration(tournamentId: Long) {
        val t = dao.getTournamentById(tournamentId) ?: return
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        if (t.isRegistered) {
            dao.removeHumanParticipant(tournamentId)
            dao.updateTournament(t.copy(isRegistered = false))
        } else {
            val variant = if (t.variant == GameVariant.FISCHER960.name) GameVariant.FISCHER960 else GameVariant.STANDARD
            val rating = if (variant == GameVariant.FISCHER960) profile.fischerRating else profile.rapidRating
            dao.insertParticipants(
                listOf(
                    TournamentParticipantEntity(
                        tournamentId = tournamentId,
                        playerName = profile.username,
                        countryCode = profile.countryCode,
                        rating = rating,
                        isHuman = true
                    )
                )
            )
            dao.updateTournament(t.copy(isRegistered = true))
            if (t.status == "UPCOMING" || t.currentRound == 0) {
                startOrAdvanceTournamentRound(tournamentId)
            }
        }
    }

    suspend fun startOrAdvanceTournamentRound(tournamentId: Long) {
        val t = dao.getTournamentById(tournamentId) ?: return
        if (t.currentRound >= t.maxRounds) {
            dao.updateTournament(t.copy(status = "COMPLETED"))
            return
        }
        val nextRound = t.currentRound + 1
        val participants = dao.getTournamentParticipantsOnce(tournamentId)
            .sortedWith(compareByDescending<TournamentParticipantEntity> { it.pointsTimesTwo }.thenByDescending { it.rating })
        val priorMatches = dao.getTournamentMatchesOnce(tournamentId)
        val pairedHistory = priorMatches.map { setOf(it.whitePlayer, it.blackPlayer) }.toSet()

        val unpaired = participants.toMutableList()
        val roundMatches = mutableListOf<TournamentMatchEntity>()
        while (unpaired.size >= 2) {
            val p1 = unpaired.removeAt(0)
            val opponentIdx = unpaired.indexOfFirst { p2 -> setOf(p1.playerName, p2.playerName) !in pairedHistory }
                .let { if (it >= 0) it else 0 }
            val p2 = unpaired.removeAt(opponentIdx)
            val whiteFirst = (nextRound + roundMatches.size) % 2 == 0
            val w = if (whiteFirst) p1 else p2
            val b = if (whiteFirst) p2 else p1
            roundMatches.add(
                TournamentMatchEntity(
                    tournamentId = tournamentId,
                    roundNumber = nextRound,
                    whitePlayer = w.playerName,
                    blackPlayer = b.playerName,
                    whiteRating = w.rating,
                    blackRating = b.rating,
                    result = "PENDING"
                )
            )
        }
        dao.insertTournamentMatches(roundMatches)
        dao.updateTournament(t.copy(currentRound = nextRound, status = "ACTIVE"))
    }

    suspend fun completeTournamentMatchAndSimulateRound(
        tournamentId: Long,
        humanMatchId: Long,
        humanResult: GameResult,
        matchHistoryId: Long?
    ) {
        val t = dao.getTournamentById(tournamentId) ?: return
        val matches = dao.getTournamentMatchesOnce(tournamentId).filter { it.roundNumber == t.currentRound }
        val participants = dao.getTournamentParticipantsOnce(tournamentId).associateBy { it.playerName }.toMutableMap()

        for (m in matches) {
            if (m.result != "PENDING") continue
            val finalRes = if (m.id == humanMatchId) {
                humanResult.name
            } else {
                // Rating-weighted realistic resolution for concurrent AI pairings in the same round
                val diff = (m.whiteRating - m.blackRating).toDouble()
                val expectedWhite = 1.0 / (1.0 + Math.pow(10.0, -diff / 400.0))
                val roll = Math.random()
                when {
                    roll < expectedWhite * 0.78 -> GameResult.WHITE_WIN.name
                    roll < expectedWhite * 0.78 + 0.22 -> GameResult.DRAW.name
                    else -> GameResult.BLACK_WIN.name
                }
            }
            dao.updateTournamentMatch(
                m.copy(
                    result = finalRes,
                    matchHistoryId = if (m.id == humanMatchId) matchHistoryId else m.matchHistoryId
                )
            )
            val w = participants[m.whitePlayer]
            val b = participants[m.blackPlayer]
            if (w != null && b != null) {
                val (wPts, bPts) = when (finalRes) {
                    GameResult.WHITE_WIN.name -> 2 to 0
                    GameResult.BLACK_WIN.name -> 0 to 2
                    else -> 1 to 1
                }
                val streakBonusW = if (t.systemType == "ARENA" && wPts == 2 && w.streak >= 2) 1 else 0
                val streakBonusB = if (t.systemType == "ARENA" && bPts == 2 && b.streak >= 2) 1 else 0
                participants[w.playerName] = w.copy(
                    pointsTimesTwo = w.pointsTimesTwo + wPts + streakBonusW,
                    wins = w.wins + if (wPts == 2) 1 else 0,
                    draws = w.draws + if (wPts == 1) 1 else 0,
                    losses = w.losses + if (wPts == 0) 1 else 0,
                    streak = if (wPts == 2) w.streak + 1 else 0
                )
                participants[b.playerName] = b.copy(
                    pointsTimesTwo = b.pointsTimesTwo + bPts + streakBonusB,
                    wins = b.wins + if (bPts == 2) 1 else 0,
                    draws = b.draws + if (bPts == 1) 1 else 0,
                    losses = b.losses + if (bPts == 0) 1 else 0,
                    streak = if (bPts == 2) b.streak + 1 else 0
                )
            }
        }

        // Recompute Buchholz tiebreaks
        val allTournamentMatches = dao.getTournamentMatchesOnce(tournamentId)
        for ((name, p) in participants) {
            val opponents = allTournamentMatches.filter { it.whitePlayer == name || it.blackPlayer == name }
                .map { if (it.whitePlayer == name) it.blackPlayer else it.whitePlayer }
            val buchholz = opponents.sumOf { opp -> participants[opp]?.pointsTimesTwo ?: 0 }
            dao.updateParticipant(p.copy(buchholzTimesTwo = buchholz))
        }

        if (t.currentRound >= t.maxRounds) {
            dao.updateTournament(t.copy(status = "COMPLETED"))
            val sorted = participants.values.sortedWith(
                compareByDescending<TournamentParticipantEntity> { it.pointsTimesTwo }
                    .thenByDescending { it.buchholzTimesTwo }
            )
            val profile = dao.getProfileOnce()
            if (profile != null) {
                val won = sorted.firstOrNull()?.isHuman == true
                dao.upsertProfile(
                    profile.copy(
                        tournamentsPlayed = profile.tournamentsPlayed + 1,
                        tournamentWins = profile.tournamentWins + if (won) 1 else 0,
                        xp = profile.xp + if (won) 250 else 80
                    )
                )
            }
        } else {
            startOrAdvanceTournamentRound(tournamentId)
        }
    }

    // Social & Admin
    suspend fun toggleFriend(memberId: Long) {
        val m = dao.getClubMemberById(memberId) ?: return
        dao.updateClubMember(
            m.copy(
                isFriend = !m.isFriend,
                isFriendRequestPending = false
            )
        )
    }

    suspend fun acceptFriendRequest(memberId: Long) {
        val m = dao.getClubMemberById(memberId) ?: return
        dao.updateClubMember(m.copy(isFriend = true, isFriendRequestPending = false))
    }

    suspend fun toggleFollow(memberId: Long) {
        val m = dao.getClubMemberById(memberId) ?: return
        dao.updateClubMember(m.copy(isFollowing = !m.isFollowing))
    }

    suspend fun toggleBanMember(memberId: Long) {
        val m = dao.getClubMemberById(memberId) ?: return
        dao.updateClubMember(m.copy(isBanned = !m.isBanned))
    }

    suspend fun submitReport(targetUsername: String, reason: String, details: String) {
        dao.insertReport(
            ModerationReportEntity(
                targetUsername = targetUsername.trim(),
                reason = reason,
                details = details.trim().take(300)
            )
        )
    }

    suspend fun updateReportStatus(report: ModerationReportEntity, newStatus: String) {
        dao.updateReport(report.copy(status = newStatus))
    }

    suspend fun publishAnnouncement(title: String, body: String, priority: String) {
        dao.insertAnnouncement(
            AnnouncementEntity(
                title = title.trim(),
                body = body.trim(),
                priority = priority
            )
        )
    }

    suspend fun deleteAnnouncement(id: Long) {
        dao.deleteAnnouncement(id)
    }

    fun computeAchievements(profile: PlayerProfileEntity): List<AchievementItem> = listOf(
        AchievementItem("ach_first", "First Victory", "Win your first rated or AI match", profile.totalWins >= 1, "${profile.totalWins.coerceAtMost(1)}/1"),
        AchievementItem("ach_streak3", "On Fire", "Reach a 3-game winning streak", profile.longestWinStreak >= 3, "${profile.longestWinStreak.coerceAtMost(3)}/3"),
        AchievementItem("ach_960", "Fischer Pioneer", "Complete 3 Chess960 matches", (profile.fischerWins + profile.fischerLosses + profile.fischerDraws) >= 3, "${(profile.fischerWins + profile.fischerLosses + profile.fischerDraws).coerceAtMost(3)}/3"),
        AchievementItem("ach_puz5", "Tactician", "Solve 5 chess puzzles", profile.puzzlesSolved >= 5, "${profile.puzzlesSolved.coerceAtMost(5)}/5"),
        AchievementItem("ach_puz_streak", "Sharp Eye", "Achieve a 5-puzzle solving streak", profile.bestPuzzleStreak >= 5, "${profile.bestPuzzleStreak.coerceAtMost(5)}/5"),
        AchievementItem("ach_1500", "Club Master", "Reach 1500 rating in any format", profile.highestRating >= 1500, "${profile.highestRating.coerceAtMost(1500)}/1500"),
        AchievementItem("ach_tourney", "Arena Contender", "Complete an official tournament", profile.tournamentsPlayed >= 1, "${profile.tournamentsPlayed.coerceAtMost(1)}/1"),
        AchievementItem("ach_ten", "Veteran Competitor", "Play 10 total chess games", profile.totalMatches >= 10, "${profile.totalMatches.coerceAtMost(10)}/10")
    )

    private suspend fun seedDefaultTournaments() {
        val t1 = dao.insertTournament(
            TournamentEntity(
                title = "World Rapid Swiss Cup",
                systemType = "SWISS",
                variant = GameVariant.STANDARD.name,
                timeControlLabel = "10+0",
                maxRounds = 5,
                currentRound = 0,
                status = "UPCOMING",
                scheduleText = "5 Rounds · Swiss System",
                prizeText = "Gold Crown + 250 XP",
                isRegistered = false
            )
        )
        val t2 = dao.insertTournament(
            TournamentEntity(
                title = "Fischer960 Blitz Arena",
                systemType = "ARENA",
                variant = GameVariant.FISCHER960.name,
                timeControlLabel = "3+2",
                maxRounds = 5,
                currentRound = 0,
                status = "UPCOMING",
                scheduleText = "5 Rounds · Arena Streak Scoring",
                prizeText = "960 Master Badge + 300 XP",
                isRegistered = false
            )
        )
        val roster = dao.observeClubMembers().first()
        dao.insertParticipants(
            roster.take(7).map { m ->
                TournamentParticipantEntity(
                    tournamentId = t1,
                    playerName = m.username,
                    countryCode = m.countryCode,
                    rating = m.rapidRating
                )
            }
        )
        dao.insertParticipants(
            roster.drop(2).take(7).map { m ->
                TournamentParticipantEntity(
                    tournamentId = t2,
                    playerName = m.username,
                    countryCode = m.countryCode,
                    rating = m.fischerRating
                )
            }
        )
    }

    private fun defaultClubRoster(): List<ClubMemberEntity> = listOf(
        ClubMemberEntity(username = "GM_Valkyrie", title = "GM", countryCode = "NO", bulletRating = 2580, blitzRating = 2610, rapidRating = 2595, classicalRating = 2620, fischerRating = 2540, puzzleRating = 2680, weeklyGain = 34, monthlyGain = 82, status = "ONLINE", isFollowing = true),
        ClubMemberEntity(username = "Aarav_Viswa", title = "IM", countryCode = "IN", bulletRating = 2420, blitzRating = 2465, rapidRating = 2490, classicalRating = 2450, fischerRating = 2410, puzzleRating = 2520, weeklyGain = 28, monthlyGain = 64, status = "IN_GAME", isFriend = true),
        ClubMemberEntity(username = "Hikaru_Tactics", title = "GM", countryCode = "US", bulletRating = 2640, blitzRating = 2620, rapidRating = 2530, classicalRating = 2490, fischerRating = 2585, puzzleRating = 2710, weeklyGain = 41, monthlyGain = 95, status = "ONLINE"),
        ClubMemberEntity(username = "Elena_Endgame", title = "FM", countryCode = "DE", bulletRating = 2190, blitzRating = 2240, rapidRating = 2295, classicalRating = 2310, fischerRating = 2210, puzzleRating = 2340, weeklyGain = 19, monthlyGain = 48, status = "ONLINE", isFriend = true),
        ClubMemberEntity(username = "Mateo_Gambit", title = "CM", countryCode = "ES", bulletRating = 1980, blitzRating = 2040, rapidRating = 2085, classicalRating = 2050, fischerRating = 2015, puzzleRating = 2140, weeklyGain = 22, monthlyGain = 55, status = "OFFLINE", isFriendRequestPending = true),
        ClubMemberEntity(username = "Sakura_K960", title = "FM", countryCode = "JP", bulletRating = 2260, blitzRating = 2310, rapidRating = 2280, classicalRating = 2250, fischerRating = 2445, puzzleRating = 2390, weeklyGain = 37, monthlyGain = 79, status = "ONLINE"),
        ClubMemberEntity(username = "Lucas_Blitz", title = "Class A", countryCode = "FR", bulletRating = 1890, blitzRating = 1940, rapidRating = 1860, classicalRating = 1820, fischerRating = 1875, puzzleRating = 1990, weeklyGain = 15, monthlyGain = 38, status = "IN_GAME", isFriendRequestPending = true),
        ClubMemberEntity(username = "Sofia_Sicilian", title = "IM", countryCode = "UA", bulletRating = 2350, blitzRating = 2390, rapidRating = 2430, classicalRating = 2460, fischerRating = 2370, puzzleRating = 2480, weeklyGain = 26, monthlyGain = 71, status = "ONLINE"),
        ClubMemberEntity(username = "Karim_Cairo", title = "Class A", countryCode = "EG", bulletRating = 1720, blitzRating = 1765, rapidRating = 1810, classicalRating = 1840, fischerRating = 1750, puzzleRating = 1880, weeklyGain = 31, monthlyGain = 62, status = "OFFLINE"),
        ClubMemberEntity(username = "Chloe_London", title = "Club", countryCode = "GB", bulletRating = 1480, blitzRating = 1540, rapidRating = 1595, classicalRating = 1610, fischerRating = 1520, puzzleRating = 1640, weeklyGain = 14, monthlyGain = 41, status = "ONLINE"),
        ClubMemberEntity(username = "Rafael_Rio", title = "CM", countryCode = "BR", bulletRating = 2050, blitzRating = 2090, rapidRating = 2120, classicalRating = 2145, fischerRating = 2070, puzzleRating = 2180, weeklyGain = 18, monthlyGain = 47, status = "OFFLINE"),
        ClubMemberEntity(username = "Minho_Seoul", title = "Club", countryCode = "KR", bulletRating = 1360, blitzRating = 1410, rapidRating = 1465, classicalRating = 1480, fischerRating = 1390, puzzleRating = 1510, weeklyGain = 29, monthlyGain = 53, status = "ONLINE")
    )
}
