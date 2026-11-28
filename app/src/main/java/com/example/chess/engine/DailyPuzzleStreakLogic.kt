package com.example.chess.engine

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DailyStreakCompletionUpdate(
    val streak: Int,
    val bestStreak: Int,
    val lastEpochDay: Long,
    val completedEpochDays: Set<Long>,
    val incrementedToday: Boolean
)

data class StreakDayDot(
    val epochDay: Long,
    val shortDayLabel: String,
    val isToday: Boolean,
    val isCompleted: Boolean
)

object DailyPuzzleStreakLogic {

    fun effectiveStreak(
        storedStreak: Int,
        lastEpochDay: Long,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): Int {
        if (storedStreak <= 0 || lastEpochDay <= 0L) return 0
        return if (lastEpochDay == todayEpochDay || lastEpochDay == todayEpochDay - 1L) {
            storedStreak
        } else {
            0
        }
    }

    fun isSolvedToday(
        lastEpochDay: Long,
        completedEpochDays: Set<Long>,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): Boolean = lastEpochDay == todayEpochDay || todayEpochDay in completedEpochDays

    fun recordCompletion(
        storedStreak: Int,
        bestStreak: Int,
        lastEpochDay: Long,
        completedEpochDays: Set<Long>,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): DailyStreakCompletionUpdate {
        val alreadySolvedToday = isSolvedToday(lastEpochDay, completedEpochDays, todayEpochDay)
        val nextStreak = when {
            alreadySolvedToday -> storedStreak.coerceAtLeast(1)
            lastEpochDay == todayEpochDay - 1L -> storedStreak.coerceAtLeast(0) + 1
            else -> 1
        }
        val nextBest = maxOf(bestStreak, nextStreak)
        val trimmedDays = (completedEpochDays + todayEpochDay)
            .filter { it >= todayEpochDay - 30L }
            .toSet()

        return DailyStreakCompletionUpdate(
            streak = nextStreak,
            bestStreak = nextBest,
            lastEpochDay = maxOf(lastEpochDay, todayEpochDay),
            completedEpochDays = trimmedDays,
            incrementedToday = !alreadySolvedToday
        )
    }

    fun recentSevenDayDots(
        completedEpochDays: Set<Long>,
        lastEpochDay: Long,
        today: LocalDate = LocalDate.now()
    ): List<StreakDayDot> {
        val todayEpoch = today.toEpochDay()
        val effectiveCompleted = if (lastEpochDay > 0L) completedEpochDays + lastEpochDay else completedEpochDays
        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val ep = date.toEpochDay()
            StreakDayDot(
                epochDay = ep,
                shortDayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                isToday = ep == todayEpoch,
                isCompleted = ep in effectiveCompleted
            )
        }
    }
}
