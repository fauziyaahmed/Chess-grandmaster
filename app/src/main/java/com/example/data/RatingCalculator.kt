package com.example.data

import kotlin.math.pow
import kotlin.math.roundToInt

object RatingCalculator {

    fun calculateNewRating(
        playerRating: Int,
        opponentRating: Int,
        score: Double, // 1.0 = win, 0.5 = draw, 0.0 = loss
        gamesPlayed: Int
    ): Int {
        val k = when {
            gamesPlayed < 30 -> 40.0 // Provisional fast calibration
            playerRating >= 2400 -> 10.0
            else -> 20.0
        }
        val expected = 1.0 / (1.0 + 10.0.pow((opponentRating - playerRating) / 400.0))
        val delta = (k * (score - expected)).roundToInt()
        return (playerRating + delta).coerceIn(100, 3500)
    }

    fun isProvisional(gamesPlayed: Int): Boolean = gamesPlayed < 10

    fun levelForXp(xp: Int): Int {
        var lvl = 1
        var req = 100
        var remaining = xp.coerceAtLeast(0)
        while (remaining >= req) {
            remaining -= req
            lvl++
            req = 100 + (lvl - 1) * 60
        }
        return lvl
    }

    fun xpProgress(xp: Int): Pair<Int, Int> {
        var lvl = 1
        var req = 100
        var remaining = xp.coerceAtLeast(0)
        while (remaining >= req) {
            remaining -= req
            lvl++
            req = 100 + (lvl - 1) * 60
        }
        return remaining to req
    }

    fun chessTitleForPeak(peakRating: Int): String = when {
        peakRating >= 2500 -> "GM"
        peakRating >= 2400 -> "IM"
        peakRating >= 2300 -> "FM"
        peakRating >= 2100 -> "CM"
        peakRating >= 1800 -> "Class A"
        peakRating >= 1500 -> "Club"
        else -> "Challenger"
    }

    fun eloForSkillLevel(skillLevel: Int): Int = when (skillLevel.coerceIn(1, 8)) {
        1 -> 650
        2 -> 900
        3 -> 1150
        4 -> 1400
        5 -> 1650
        6 -> 1900
        7 -> 2100
        else -> 2300
    }

    fun calculateExpectedStakes(
        playerRating: Int,
        opponentRating: Int,
        gamesPlayed: Int
    ): MatchStakes {
        val winNew = calculateNewRating(playerRating, opponentRating, 1.0, gamesPlayed)
        val drawNew = calculateNewRating(playerRating, opponentRating, 0.5, gamesPlayed)
        val lossNew = calculateNewRating(playerRating, opponentRating, 0.0, gamesPlayed)
        return MatchStakes(
            winDelta = winNew - playerRating,
            drawDelta = drawNew - playerRating,
            lossDelta = lossNew - playerRating
        )
    }

    fun performanceRating(
        opponentRating: Int,
        score: Double,
        accuracyPercent: Double
    ): Int {
        val outcomeShift = when {
            score >= 0.99 -> 400
            score <= 0.01 -> -400
            else -> 0
        }
        val accAdjust = ((accuracyPercent.coerceIn(30.0, 99.0) - 70.0) * 3.5).roundToInt()
        return (opponentRating + outcomeShift + accAdjust).coerceIn(400, 3200)
    }
}

data class MatchStakes(
    val winDelta: Int,
    val drawDelta: Int,
    val lossDelta: Int
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val progressText: String
)
