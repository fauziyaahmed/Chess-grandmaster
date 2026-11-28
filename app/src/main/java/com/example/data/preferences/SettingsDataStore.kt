package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.engine.TimeControl
import com.example.data.SoundPack
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.ArenaBackgroundPreset
import com.example.ui.theme.BoardPatternStyle
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.PieceStyle
import com.example.ui.theme.ThemeComparisonSetup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "grandmaster_settings")

enum class AccountMode(val displayName: String) {
    UNDECIDED("Choose Account"),
    GUEST("Guest Mode (Local Device)")
}

data class EndgameCompletionRecord(
    val stars: Int,
    val bestMoves: Int
)

data class UserSettings(
    val appTheme: AppThemeOption = AppThemeOption.GRANDMASTER_EMERALD,
    val isDarkTheme: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD,
    val boardPattern: BoardPatternStyle = BoardPatternStyle.CLASSIC_FLAT,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    val backgroundPreset: ArenaBackgroundPreset = ArenaBackgroundPreset.EMERALD_AURORA,
    val customGradientStartArgb: Long = 0xFF0F172AL,
    val customGradientEndArgb: Long = 0xFF065F46L,
    val backgroundDimAlpha: Float = 0.45f,
    val customBackgroundImagePath: String? = null,
    val showCoordinates: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundPack: SoundPack = SoundPack.CLASSIC_WOOD,
    val soundVolume: Float = 0.85f,
    val animateMoves: Boolean = true,
    val autoPromoteQueen: Boolean = false,
    val boardScale: Float = 1.0f,
    val languageLabel: String = "English (International)",
    val hideOnlineStatus: Boolean = false,
    val safeChatFilter: Boolean = true,
    val accountMode: AccountMode = AccountMode.UNDECIDED,
    val useCustomTimeDefault: Boolean = false,
    val customMinutes: Int = 10,
    val customSeconds: Int = 0,
    val customIncrementSec: Int = 5,
    val selectedTimeControlLabel: String = "10+0",
    val coachDavidDefault: Boolean = false,
    val coachVoiceEnabled: Boolean = true,
    val endgameProgress: Map<String, EndgameCompletionRecord> = emptyMap(),
    val dailyPuzzleStreak: Int = 0,
    val bestDailyPuzzleStreak: Int = 0,
    val lastDailyPuzzleEpochDay: Long = 0L,
    val dailyPuzzleCompletedEpochDays: Set<Long> = emptySet(),
    val bestDailyStreakRun: Int = 0
) {
    fun effectiveTimeControl(): TimeControl = if (useCustomTimeDefault) {
        TimeControl.custom(customMinutes, customSeconds, customIncrementSec)
    } else {
        TimeControl.fromLabel(selectedTimeControlLabel)
    }

    fun toComparisonSetup(ownerName: String, rating: Int): ThemeComparisonSetup = ThemeComparisonSetup(
        id = "my_current_setup",
        title = "My Setup",
        authorLabel = ownerName,
        ratingLabel = "$rating Rapid",
        appTheme = appTheme,
        boardTheme = boardTheme,
        pieceStyle = pieceStyle,
        backgroundPreset = backgroundPreset,
        customGradientStartArgb = customGradientStartArgb,
        customGradientEndArgb = customGradientEndArgb,
        customBackgroundImagePath = customBackgroundImagePath,
        backgroundDimAlpha = backgroundDimAlpha
    )
}

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val BOARD_PATTERN = stringPreferencesKey("board_pattern")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val BACKGROUND_PRESET = stringPreferencesKey("background_preset")
        val CUSTOM_GRADIENT_START = longPreferencesKey("custom_gradient_start")
        val CUSTOM_GRADIENT_END = longPreferencesKey("custom_gradient_end")
        val BACKGROUND_DIM_ALPHA = floatPreferencesKey("background_dim_alpha")
        val CUSTOM_BG_IMAGE_PATH = stringPreferencesKey("custom_bg_image_path")
        val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_PACK = stringPreferencesKey("sound_pack")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val ANIMATE_MOVES = booleanPreferencesKey("animate_moves")
        val AUTO_PROMOTE_QUEEN = booleanPreferencesKey("auto_promote_queen")
        val BOARD_SCALE = floatPreferencesKey("board_scale")
        val LANGUAGE_LABEL = stringPreferencesKey("language_label")
        val HIDE_ONLINE_STATUS = booleanPreferencesKey("hide_online_status")
        val SAFE_CHAT_FILTER = booleanPreferencesKey("safe_chat_filter")
        val ACCOUNT_MODE = stringPreferencesKey("account_mode")
        val USE_CUSTOM_TIME_DEFAULT = booleanPreferencesKey("use_custom_time_default")
        val CUSTOM_MINUTES = intPreferencesKey("custom_minutes")
        val CUSTOM_SECONDS = intPreferencesKey("custom_seconds")
        val CUSTOM_INCREMENT_SEC = intPreferencesKey("custom_increment_sec")
        val SELECTED_TC_LABEL = stringPreferencesKey("selected_tc_label")
        val COACH_DAVID_DEFAULT = booleanPreferencesKey("coach_david_default")
        val COACH_VOICE_ENABLED = booleanPreferencesKey("coach_voice_enabled")
        val ENDGAME_PROGRESS = stringPreferencesKey("endgame_progress")
        val DAILY_PUZZLE_STREAK = intPreferencesKey("daily_puzzle_streak")
        val BEST_DAILY_PUZZLE_STREAK = intPreferencesKey("best_daily_puzzle_streak")
        val LAST_DAILY_PUZZLE_EPOCH_DAY = longPreferencesKey("last_daily_puzzle_epoch_day")
        val DAILY_PUZZLE_HISTORY_DAYS = stringPreferencesKey("daily_puzzle_history_days")
        val BEST_DAILY_STREAK_RUN = intPreferencesKey("best_daily_streak_run")
    }

    private fun mapPrefs(prefs: Preferences): UserSettings {
        val legacyDark = prefs[Keys.IS_DARK_THEME] ?: true
        val resolvedAppTheme = AppThemeOption.entries.firstOrNull { it.name == prefs[Keys.APP_THEME] }
            ?: if (legacyDark) AppThemeOption.GRANDMASTER_EMERALD else AppThemeOption.NORDIC_FROST

        return UserSettings(
            appTheme = resolvedAppTheme,
            isDarkTheme = resolvedAppTheme.isDark,
            boardTheme = BoardTheme.entries.firstOrNull { it.name == prefs[Keys.BOARD_THEME] }
                ?: BoardTheme.TOURNAMENT_EMERALD,
            boardPattern = BoardPatternStyle.entries.firstOrNull { it.name == prefs[Keys.BOARD_PATTERN] }
                ?: BoardPatternStyle.CLASSIC_FLAT,
            pieceStyle = PieceStyle.entries.firstOrNull { it.name == prefs[Keys.PIECE_STYLE] }
                ?: PieceStyle.CLASSIC_STAUNTON,
            backgroundPreset = ArenaBackgroundPreset.entries.firstOrNull { it.name == prefs[Keys.BACKGROUND_PRESET] }
                ?: ArenaBackgroundPreset.EMERALD_AURORA,
            customGradientStartArgb = prefs[Keys.CUSTOM_GRADIENT_START] ?: 0xFF0F172AL,
            customGradientEndArgb = prefs[Keys.CUSTOM_GRADIENT_END] ?: 0xFF065F46L,
            backgroundDimAlpha = (prefs[Keys.BACKGROUND_DIM_ALPHA] ?: 0.45f).coerceIn(0.15f, 0.85f),
            customBackgroundImagePath = prefs[Keys.CUSTOM_BG_IMAGE_PATH]?.takeIf { it.isNotBlank() },
            showCoordinates = prefs[Keys.SHOW_COORDINATES] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            soundPack = SoundPack.entries.firstOrNull { it.name == prefs[Keys.SOUND_PACK] }
                ?: SoundPack.CLASSIC_WOOD,
            soundVolume = (prefs[Keys.SOUND_VOLUME] ?: 0.85f).coerceIn(0f, 1f),
            animateMoves = prefs[Keys.ANIMATE_MOVES] ?: true,
            autoPromoteQueen = prefs[Keys.AUTO_PROMOTE_QUEEN] ?: false,
            boardScale = (prefs[Keys.BOARD_SCALE] ?: 1.0f).coerceIn(0.85f, 1.0f),
            languageLabel = prefs[Keys.LANGUAGE_LABEL] ?: "English (International)",
            hideOnlineStatus = prefs[Keys.HIDE_ONLINE_STATUS] ?: false,
            safeChatFilter = prefs[Keys.SAFE_CHAT_FILTER] ?: true,
            accountMode = AccountMode.entries.firstOrNull { it.name == prefs[Keys.ACCOUNT_MODE] }
                ?: AccountMode.UNDECIDED,
            useCustomTimeDefault = prefs[Keys.USE_CUSTOM_TIME_DEFAULT] ?: false,
            customMinutes = (prefs[Keys.CUSTOM_MINUTES] ?: 10).coerceIn(0, 180),
            customSeconds = (prefs[Keys.CUSTOM_SECONDS] ?: 0).coerceIn(0, 59),
            customIncrementSec = (prefs[Keys.CUSTOM_INCREMENT_SEC] ?: 5).coerceIn(0, 60),
            selectedTimeControlLabel = prefs[Keys.SELECTED_TC_LABEL] ?: "10+0",
            coachDavidDefault = prefs[Keys.COACH_DAVID_DEFAULT] ?: false,
            coachVoiceEnabled = prefs[Keys.COACH_VOICE_ENABLED] ?: true,
            endgameProgress = decodeEndgameProgress(prefs[Keys.ENDGAME_PROGRESS]),
            dailyPuzzleStreak = (prefs[Keys.DAILY_PUZZLE_STREAK] ?: 0).coerceAtLeast(0),
            bestDailyPuzzleStreak = (prefs[Keys.BEST_DAILY_PUZZLE_STREAK] ?: 0).coerceAtLeast(0),
            lastDailyPuzzleEpochDay = (prefs[Keys.LAST_DAILY_PUZZLE_EPOCH_DAY] ?: 0L).coerceAtLeast(0L),
            dailyPuzzleCompletedEpochDays = decodeEpochDays(prefs[Keys.DAILY_PUZZLE_HISTORY_DAYS]),
            bestDailyStreakRun = (prefs[Keys.BEST_DAILY_STREAK_RUN] ?: 0).coerceAtLeast(0)
        )
    }

    private fun decodeEpochDays(raw: String?): Set<Long> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    private fun encodeEpochDays(days: Set<Long>): String =
        days.sorted().joinToString(",")

    private fun decodeEndgameProgress(raw: String?): Map<String, EndgameCompletionRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        val result = linkedMapOf<String, EndgameCompletionRecord>()
        raw.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 3) {
                val id = parts[0].trim()
                val stars = parts[1].toIntOrNull()?.coerceIn(1, 3) ?: return@forEach
                val moves = parts[2].toIntOrNull()?.coerceAtLeast(1) ?: return@forEach
                if (id.isNotBlank()) result[id] = EndgameCompletionRecord(stars, moves)
            }
        }
        return result
    }

    private fun encodeEndgameProgress(map: Map<String, EndgameCompletionRecord>): String =
        map.entries.joinToString(";") { (id, rec) -> "$id:${rec.stars}:${rec.bestMoves}" }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map(::mapPrefs)

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(mapPrefs(prefs))
            prefs[Keys.APP_THEME] = updated.appTheme.name
            prefs[Keys.IS_DARK_THEME] = updated.appTheme.isDark
            prefs[Keys.BOARD_THEME] = updated.boardTheme.name
            prefs[Keys.BOARD_PATTERN] = updated.boardPattern.name
            prefs[Keys.PIECE_STYLE] = updated.pieceStyle.name
            prefs[Keys.BACKGROUND_PRESET] = updated.backgroundPreset.name
            prefs[Keys.CUSTOM_GRADIENT_START] = updated.customGradientStartArgb
            prefs[Keys.CUSTOM_GRADIENT_END] = updated.customGradientEndArgb
            prefs[Keys.BACKGROUND_DIM_ALPHA] = updated.backgroundDimAlpha.coerceIn(0.15f, 0.85f)
            if (updated.customBackgroundImagePath.isNullOrBlank()) {
                prefs.remove(Keys.CUSTOM_BG_IMAGE_PATH)
            } else {
                prefs[Keys.CUSTOM_BG_IMAGE_PATH] = updated.customBackgroundImagePath
            }
            prefs[Keys.SHOW_COORDINATES] = updated.showCoordinates
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.SOUND_PACK] = updated.soundPack.name
            prefs[Keys.SOUND_VOLUME] = updated.soundVolume.coerceIn(0f, 1f)
            prefs[Keys.ANIMATE_MOVES] = updated.animateMoves
            prefs[Keys.AUTO_PROMOTE_QUEEN] = updated.autoPromoteQueen
            prefs[Keys.BOARD_SCALE] = updated.boardScale
            prefs[Keys.LANGUAGE_LABEL] = updated.languageLabel
            prefs[Keys.HIDE_ONLINE_STATUS] = updated.hideOnlineStatus
            prefs[Keys.SAFE_CHAT_FILTER] = updated.safeChatFilter
            prefs[Keys.ACCOUNT_MODE] = updated.accountMode.name
            prefs[Keys.USE_CUSTOM_TIME_DEFAULT] = updated.useCustomTimeDefault
            prefs[Keys.CUSTOM_MINUTES] = updated.customMinutes
            prefs[Keys.CUSTOM_SECONDS] = updated.customSeconds
            prefs[Keys.CUSTOM_INCREMENT_SEC] = updated.customIncrementSec
            prefs[Keys.SELECTED_TC_LABEL] = updated.selectedTimeControlLabel
            prefs[Keys.COACH_DAVID_DEFAULT] = updated.coachDavidDefault
            prefs[Keys.COACH_VOICE_ENABLED] = updated.coachVoiceEnabled
            prefs[Keys.ENDGAME_PROGRESS] = encodeEndgameProgress(updated.endgameProgress)
            prefs[Keys.DAILY_PUZZLE_STREAK] = updated.dailyPuzzleStreak.coerceAtLeast(0)
            prefs[Keys.BEST_DAILY_PUZZLE_STREAK] = updated.bestDailyPuzzleStreak.coerceAtLeast(0)
            prefs[Keys.LAST_DAILY_PUZZLE_EPOCH_DAY] = updated.lastDailyPuzzleEpochDay.coerceAtLeast(0L)
            prefs[Keys.DAILY_PUZZLE_HISTORY_DAYS] = encodeEpochDays(updated.dailyPuzzleCompletedEpochDays)
            prefs[Keys.BEST_DAILY_STREAK_RUN] = updated.bestDailyStreakRun.coerceAtLeast(0)
        }
    }
}
