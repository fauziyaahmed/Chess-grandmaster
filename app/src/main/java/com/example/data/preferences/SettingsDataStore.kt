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
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.ArenaBackgroundPreset
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

data class UserSettings(
    val appTheme: AppThemeOption = AppThemeOption.GRANDMASTER_EMERALD,
    val isDarkTheme: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    val backgroundPreset: ArenaBackgroundPreset = ArenaBackgroundPreset.EMERALD_AURORA,
    val customGradientStartArgb: Long = 0xFF0F172AL,
    val customGradientEndArgb: Long = 0xFF065F46L,
    val backgroundDimAlpha: Float = 0.45f,
    val customBackgroundImagePath: String? = null,
    val showCoordinates: Boolean = true,
    val soundEnabled: Boolean = true,
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
    val coachDavidDefault: Boolean = false
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
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val BACKGROUND_PRESET = stringPreferencesKey("background_preset")
        val CUSTOM_GRADIENT_START = longPreferencesKey("custom_gradient_start")
        val CUSTOM_GRADIENT_END = longPreferencesKey("custom_gradient_end")
        val BACKGROUND_DIM_ALPHA = floatPreferencesKey("background_dim_alpha")
        val CUSTOM_BG_IMAGE_PATH = stringPreferencesKey("custom_bg_image_path")
        val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
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
            coachDavidDefault = prefs[Keys.COACH_DAVID_DEFAULT] ?: false
        )
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map(::mapPrefs)

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(mapPrefs(prefs))
            prefs[Keys.APP_THEME] = updated.appTheme.name
            prefs[Keys.IS_DARK_THEME] = updated.appTheme.isDark
            prefs[Keys.BOARD_THEME] = updated.boardTheme.name
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
        }
    }
}
