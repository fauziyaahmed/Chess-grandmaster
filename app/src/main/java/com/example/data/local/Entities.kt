package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "GrandmasterPlayer",
    val countryCode: String = "US",
    val bio: String = "Tactical strategist & Chess960 competitor",
    val avatarColorIndex: Int = 0,
    val xp: Int = 120,
    val dailyLoginStreak: Int = 1,
    val lastDailyClaimEpochDay: Long = 0L,
    val totalPlayTimeSeconds: Long = 0L,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
    // Format ratings & counters
    val bulletRating: Int = 1200,
    val bulletPeak: Int = 1200,
    val bulletWins: Int = 0,
    val bulletLosses: Int = 0,
    val bulletDraws: Int = 0,
    val blitzRating: Int = 1200,
    val blitzPeak: Int = 1200,
    val blitzWins: Int = 0,
    val blitzLosses: Int = 0,
    val blitzDraws: Int = 0,
    val rapidRating: Int = 1200,
    val rapidPeak: Int = 1200,
    val rapidWins: Int = 0,
    val rapidLosses: Int = 0,
    val rapidDraws: Int = 0,
    val classicalRating: Int = 1200,
    val classicalPeak: Int = 1200,
    val classicalWins: Int = 0,
    val classicalLosses: Int = 0,
    val classicalDraws: Int = 0,
    val fischerRating: Int = 1200,
    val fischerPeak: Int = 1200,
    val fischerWins: Int = 0,
    val fischerLosses: Int = 0,
    val fischerDraws: Int = 0,
    // Puzzle stats
    val puzzleRating: Int = 1200,
    val puzzlePeak: Int = 1200,
    val puzzlesSolved: Int = 0,
    val puzzlesAttempted: Int = 0,
    val puzzleStreak: Int = 0,
    val bestPuzzleStreak: Int = 0,
    val tournamentsPlayed: Int = 0,
    val tournamentWins: Int = 0
) {
    val totalWins: Int get() = bulletWins + blitzWins + rapidWins + classicalWins + fischerWins
    val totalLosses: Int get() = bulletLosses + blitzLosses + rapidLosses + classicalLosses + fischerLosses
    val totalDraws: Int get() = bulletDraws + blitzDraws + rapidDraws + classicalDraws + fischerDraws
    val totalMatches: Int get() = totalWins + totalLosses + totalDraws
    val overallWinRate: Double get() = if (totalMatches == 0) 0.0 else (totalWins * 100.0) / totalMatches
    val highestRating: Int get() = maxOf(bulletPeak, blitzPeak, rapidPeak, classicalPeak, fischerPeak)
    val primaryRating: Int get() = maxOf(rapidRating, blitzRating, bulletRating, classicalRating)
}

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val whiteName: String,
    val blackName: String,
    val whiteRatingBefore: Int,
    val whiteRatingAfter: Int,
    val blackRatingBefore: Int,
    val blackRatingAfter: Int,
    val playerSide: String, // "WHITE", "BLACK", "BOTH" (Pass & Play)
    val variant: String, // "STANDARD", "FISCHER960"
    val category: String, // "BULLET", "BLITZ", "RAPID", "CLASSICAL"
    val timeControlLabel: String,
    val chess960Position: Int,
    val result: String, // "WHITE_WIN", "BLACK_WIN", "DRAW"
    val termination: String,
    val openingEco: String,
    val openingName: String,
    val movesSanCsv: String,
    val startFen: String,
    val finalFen: String,
    val moveCount: Int,
    val durationSeconds: Long,
    val accuracyPercent: Double,
    val pgnText: String,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "rating_snapshots")
data class RatingSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val formatKey: String, // "BULLET", "BLITZ", "RAPID", "CLASSICAL", "FISCHER960", "PUZZLE"
    val rating: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "puzzle_attempts")
data class PuzzleAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val puzzleId: String,
    val puzzleTitle: String,
    val difficulty: String,
    val dateKey: String,
    val isDailyChallenge: Boolean,
    val solved: Boolean,
    val hintsUsed: Int,
    val ratingBefore: Int,
    val ratingAfter: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val systemType: String, // "SWISS", "ARENA"
    val variant: String, // "STANDARD", "FISCHER960"
    val timeControlLabel: String,
    val maxRounds: Int,
    val currentRound: Int,
    val status: String, // "UPCOMING", "ACTIVE", "COMPLETED"
    val scheduleText: String,
    val prizeText: String,
    val isRegistered: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "tournament_participants")
data class TournamentParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val playerName: String,
    val countryCode: String,
    val rating: Int,
    val pointsTimesTwo: Int = 0, // Store 0.5 pt as 1 to avoid float precision issues
    val buchholzTimesTwo: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val streak: Int = 0,
    val isHuman: Boolean = false
)

@Entity(tableName = "tournament_matches")
data class TournamentMatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tournamentId: Long,
    val roundNumber: Int,
    val whitePlayer: String,
    val blackPlayer: String,
    val whiteRating: Int,
    val blackRating: Int,
    val result: String, // "PENDING", "WHITE_WIN", "BLACK_WIN", "DRAW"
    val matchHistoryId: Long? = null
)

@Entity(tableName = "club_members")
data class ClubMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val username: String,
    val title: String,
    val countryCode: String,
    val bulletRating: Int,
    val blitzRating: Int,
    val rapidRating: Int,
    val classicalRating: Int,
    val fischerRating: Int,
    val puzzleRating: Int,
    val weeklyGain: Int,
    val monthlyGain: Int,
    val status: String, // "ONLINE", "IN_GAME", "OFFLINE"
    val isFriend: Boolean = false,
    val isFriendRequestPending: Boolean = false,
    val isFollowing: Boolean = false,
    val isBanned: Boolean = false,
    val gamesPlayed: Int = 120
)

@Entity(tableName = "moderation_reports")
data class ModerationReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val targetUsername: String,
    val reason: String,
    val details: String,
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val priority: String = "NORMAL",
    val createdAtMs: Long = System.currentTimeMillis()
)
