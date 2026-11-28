package com.example.chess.engine

import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.preferences.UserSettings
import kotlin.math.roundToInt

data class SkillRadarMetric(
    val label: String,
    val score: Int, // 0..100
    val detail: String
)

data class ColorOutcomeStats(
    val games: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winPct: Float,
    val drawPct: Float,
    val lossPct: Float
)

data class LogicalPlayerAnalytics(
    val radarMetrics: List<SkillRadarMetric>,
    val whiteStats: ColorOutcomeStats,
    val blackStats: ColorOutcomeStats,
    val avgMovesPerGame: Int,
    val summaryHeadline: String
)

object LogicalAnalytics {

    fun computePlayerAnalytics(
        profile: PlayerProfileEntity,
        matches: List<MatchHistoryEntity>,
        puzzleAttempts: List<PuzzleAttemptEntity>,
        settings: UserSettings
    ): LogicalPlayerAnalytics {
        val completedMatches = matches.filter {
            it.result == "WHITE_WIN" || it.result == "BLACK_WIN" || it.result == "DRAW"
        }

        // White & Black color outcome split
        var wTotal = 0; var wWins = 0; var wDraws = 0; var wLosses = 0
        var bTotal = 0; var bWins = 0; var bDraws = 0; var bLosses = 0
        var totalMoves = 0
        var endgameWins = 0; var endgameGames = 0
        var accuracySum = 0.0; var accuracyCount = 0

        for (m in completedMatches) {
            totalMoves += m.moveCount
            if (m.accuracyPercent > 0.0) {
                accuracySum += m.accuracyPercent
                accuracyCount++
            }
            val playerWon = (m.playerSide == "WHITE" && m.result == "WHITE_WIN") ||
                (m.playerSide == "BLACK" && m.result == "BLACK_WIN")
            val isDraw = m.result == "DRAW"

            if (m.moveCount >= 28) {
                endgameGames++
                if (playerWon) endgameWins++
            }

            if (m.playerSide == "WHITE") {
                wTotal++
                when {
                    playerWon -> wWins++
                    isDraw -> wDraws++
                    else -> wLosses++
                }
            } else {
                bTotal++
                when {
                    playerWon -> bWins++
                    isDraw -> bDraws++
                    else -> bLosses++
                }
            }
        }

        val whiteStats = buildColorStats(wTotal, wWins, wDraws, wLosses)
        val blackStats = buildColorStats(bTotal, bWins, bDraws, bLosses)
        val avgMoves = if (completedMatches.isEmpty()) 0 else (totalMoves.toDouble() / completedMatches.size).roundToInt()

        // 6-Dimension Logical Radar Scores (0..100)
        val avgAcc = if (accuracyCount > 0) (accuracySum / accuracyCount) else 72.0
        val rapidNorm = ((profile.rapidRating - 800) / 14.0).coerceIn(20.0, 100.0)
        val openingScore = ((avgAcc * 0.55) + (rapidNorm * 0.45)).roundToInt().coerceIn(25, 99)

        val puzzleAcc = if (puzzleAttempts.isNotEmpty()) {
            (puzzleAttempts.count { it.solved } * 100.0) / puzzleAttempts.size
        } else if (profile.puzzlesAttempted > 0) {
            (profile.puzzlesSolved * 100.0) / profile.puzzlesAttempted
        } else 68.0
        val puzzleNorm = ((profile.puzzleRating - 800) / 14.0).coerceIn(20.0, 100.0)
        val tacticsScore = ((puzzleAcc * 0.55) + (puzzleNorm * 0.45)).roundToInt().coerceIn(25, 99)

        val endgameDrillStars = settings.endgameProgress.values.sumOf { it.stars }
        val maxEndgameStars = EndgameLibrary.ALL_DRILLS.size * 3
        val drillRatio = if (maxEndgameStars > 0) (endgameDrillStars * 100.0) / maxEndgameStars else 0.0
        val lateGameRate = if (endgameGames > 0) (endgameWins * 100.0) / endgameGames else profile.overallWinRate
        val endgameScore = if (endgameDrillStars > 0 || endgameGames > 0) {
            ((drillRatio * 0.5) + (lateGameRate * 0.5)).roundToInt().coerceIn(25, 99)
        } else {
            (rapidNorm * 0.75).roundToInt().coerceIn(25, 99)
        }

        val blitzNorm = ((maxOf(profile.blitzRating, profile.bulletRating) - 800) / 14.0).coerceIn(20.0, 100.0)
        val speedGames = profile.blitzWins + profile.blitzLosses + profile.blitzDraws +
            profile.bulletWins + profile.bulletLosses + profile.bulletDraws
        val speedWinRate = if (speedGames > 0) {
            ((profile.blitzWins + profile.bulletWins) * 100.0) / speedGames
        } else 55.0
        val speedScore = ((blitzNorm * 0.65) + (speedWinRate * 0.35)).roundToInt().coerceIn(25, 99)

        val consistencyBase = 40.0 +
            (profile.longestWinStreak * 5.5).coerceAtMost(35.0) +
            (settings.dailyPuzzleStreak * 4.0).coerceAtMost(24.0)
        val consistencyScore = consistencyBase.roundToInt().coerceIn(30, 99)

        val fischerNorm = ((profile.fischerRating - 800) / 14.0).coerceIn(20.0, 100.0)
        val fischerTotal = profile.fischerWins + profile.fischerLosses + profile.fischerDraws
        val fischerRate = if (fischerTotal > 0) (profile.fischerWins * 100.0) / fischerTotal else 55.0
        val fischerScore = ((fischerNorm * 0.65) + (fischerRate * 0.35)).roundToInt().coerceIn(25, 99)

        val radar = listOf(
            SkillRadarMetric("Opening", openingScore, "${avgAcc.roundToInt()}% acc"),
            SkillRadarMetric("Tactics", tacticsScore, "${profile.puzzleRating} Elo"),
            SkillRadarMetric("Endgame", endgameScore, "$endgameDrillStars★ drills"),
            SkillRadarMetric("Speed", speedScore, "${profile.blitzRating} Blitz"),
            SkillRadarMetric("Stability", consistencyScore, "${profile.longestWinStreak}w streak"),
            SkillRadarMetric("Chess960", fischerScore, "${profile.fischerRating} 960")
        )

        val strongest = radar.maxByOrNull { it.score }
        val headline = if (strongest != null) {
            "Peak Pillar: ${strongest.label} (${strongest.score}/100) · ${completedMatches.size} analyzed games"
        } else {
            "Complete matches & puzzles to expand your logical graph"
        }

        return LogicalPlayerAnalytics(
            radarMetrics = radar,
            whiteStats = whiteStats,
            blackStats = blackStats,
            avgMovesPerGame = avgMoves,
            summaryHeadline = headline
        )
    }

    private fun buildColorStats(total: Int, wins: Int, draws: Int, losses: Int): ColorOutcomeStats {
        if (total == 0) return ColorOutcomeStats(0, 0, 0, 0, 0f, 0f, 0f)
        val t = total.toFloat()
        return ColorOutcomeStats(
            games = total,
            wins = wins,
            draws = draws,
            losses = losses,
            winPct = (wins / t) * 100f,
            drawPct = (draws / t) * 100f,
            lossPct = (losses / t) * 100f
        )
    }
}
