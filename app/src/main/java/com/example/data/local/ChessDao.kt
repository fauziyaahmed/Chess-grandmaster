package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChessDao {

    // Profile
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    // Match History
    @Query("SELECT * FROM match_history ORDER BY timestampMs DESC")
    fun observeAllMatches(): Flow<List<MatchHistoryEntity>>

    @Query("SELECT * FROM match_history WHERE variant = 'FISCHER960' ORDER BY timestampMs DESC")
    fun observeFischerMatches(): Flow<List<MatchHistoryEntity>>

    @Query("SELECT * FROM match_history WHERE id = :matchId")
    suspend fun getMatchById(matchId: Long): MatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchHistoryEntity): Long

    // Rating Snapshots
    @Query("SELECT * FROM rating_snapshots ORDER BY timestampMs ASC")
    fun observeRatingSnapshots(): Flow<List<RatingSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRatingSnapshot(snapshot: RatingSnapshotEntity)

    // Puzzle Attempts
    @Query("SELECT * FROM puzzle_attempts ORDER BY timestampMs DESC")
    fun observePuzzleAttempts(): Flow<List<PuzzleAttemptEntity>>

    @Query("SELECT * FROM puzzle_attempts WHERE isDailyChallenge = 1 AND dateKey = :dateKey AND solved = 1 LIMIT 1")
    suspend fun getDailySolvedForDate(dateKey: String): PuzzleAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzleAttempt(attempt: PuzzleAttemptEntity)

    // Tournaments
    @Query("SELECT * FROM tournaments ORDER BY createdAtMs DESC")
    fun observeTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: Long): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId ORDER BY pointsTimesTwo DESC, buchholzTimesTwo DESC, rating DESC")
    fun observeTournamentParticipants(tournamentId: Long): Flow<List<TournamentParticipantEntity>>

    @Query("SELECT * FROM tournament_participants WHERE tournamentId = :tournamentId ORDER BY pointsTimesTwo DESC, buchholzTimesTwo DESC, rating DESC")
    suspend fun getTournamentParticipantsOnce(tournamentId: Long): List<TournamentParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<TournamentParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: TournamentParticipantEntity)

    @Query("DELETE FROM tournament_participants WHERE tournamentId = :tournamentId AND isHuman = 1")
    suspend fun removeHumanParticipant(tournamentId: Long)

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId ORDER BY roundNumber DESC, id ASC")
    fun observeTournamentMatches(tournamentId: Long): Flow<List<TournamentMatchEntity>>

    @Query("SELECT * FROM tournament_matches WHERE tournamentId = :tournamentId")
    suspend fun getTournamentMatchesOnce(tournamentId: Long): List<TournamentMatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournamentMatches(matches: List<TournamentMatchEntity>)

    @Update
    suspend fun updateTournamentMatch(match: TournamentMatchEntity)

    // Club Members / Rankings / Social
    @Query("SELECT * FROM club_members ORDER BY rapidRating DESC")
    fun observeClubMembers(): Flow<List<ClubMemberEntity>>

    @Query("SELECT * FROM club_members WHERE id = :id")
    suspend fun getClubMemberById(id: Long): ClubMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClubMembers(members: List<ClubMemberEntity>)

    @Update
    suspend fun updateClubMember(member: ClubMemberEntity)

    // Moderation Reports
    @Query("SELECT * FROM moderation_reports ORDER BY timestampMs DESC")
    fun observeReports(): Flow<List<ModerationReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ModerationReportEntity)

    @Update
    suspend fun updateReport(report: ModerationReportEntity)

    // Announcements
    @Query("SELECT * FROM announcements ORDER BY createdAtMs DESC")
    fun observeAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: Long)
}
