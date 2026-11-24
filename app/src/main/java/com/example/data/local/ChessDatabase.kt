package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerProfileEntity::class,
        MatchHistoryEntity::class,
        RatingSnapshotEntity::class,
        PuzzleAttemptEntity::class,
        TournamentEntity::class,
        TournamentParticipantEntity::class,
        TournamentMatchEntity::class,
        ClubMemberEntity::class,
        ModerationReportEntity::class,
        AnnouncementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getInstance(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "grandmaster_chess.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
