package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.engine.GameVariant
import com.example.data.AchievementItem
import com.example.data.local.AnnouncementEntity
import com.example.data.local.ClubMemberEntity
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.ModerationReportEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.PuzzleAttemptEntity
import com.example.data.local.RatingSnapshotEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.TournamentMatchEntity
import com.example.data.local.TournamentParticipantEntity
import com.example.data.preferences.AccountMode
import com.example.data.preferences.SettingsDataStore
import com.example.data.preferences.UserSettings
import com.example.data.repository.ChessRepository
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.ArenaBackgroundPreset
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.PieceStyle
import com.example.ui.theme.ThemeComparisonSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(
    private val repository: ChessRepository,
    private val settingsStore: SettingsDataStore
) : ViewModel() {

    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .combine(MutableStateFlow(Unit)) { p, _ -> p ?: PlayerProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfileEntity())

    val settings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val allMatches: StateFlow<List<MatchHistoryEntity>> = repository.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fischerMatches: StateFlow<List<MatchHistoryEntity>> = repository.fischerMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ratingSnapshots: StateFlow<List<RatingSnapshotEntity>> = repository.ratingSnapshotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val puzzleAttempts: StateFlow<List<PuzzleAttemptEntity>> = repository.puzzleAttemptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tournaments: StateFlow<List<TournamentEntity>> = repository.tournamentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clubMembers: StateFlow<List<ClubMemberEntity>> = repository.clubMembersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ModerationReportEntity>> = repository.reportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = repository.announcementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailyRewardBannerXp = MutableStateFlow<Int?>(null)
    val dailyRewardBannerXp: StateFlow<Int?> = _dailyRewardBannerXp.asStateFlow()

    private val _profileTabIndex = MutableStateFlow(0)
    val profileTabIndex: StateFlow<Int> = _profileTabIndex.asStateFlow()

    private val _googleAuthNotice = MutableStateFlow<String?>(null)
    val googleAuthNotice: StateFlow<String?> = _googleAuthNotice.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun openProfileTab(index: Int) {
        _profileTabIndex.value = index.coerceIn(0, 2)
    }

    fun continueAsGuest(customUsername: String? = null) {
        viewModelScope.launch {
            if (!customUsername.isNullOrBlank()) {
                val cur = profile.value
                repository.updateProfileDetails(customUsername.trim(), cur.countryCode, cur.bio)
            }
            settingsStore.updateSettings { it.copy(accountMode = AccountMode.GUEST) }
            _googleAuthNotice.value = null
        }
    }

    fun requestGoogleSignIn() {
        _googleAuthNotice.value =
            "Google Sign-In & Cloud Sync are unavailable because Firebase Authentication setup was declined for this build. Select \"Continue as Guest\" to play all modes, train with Coach David, and save every game locally on your device."
    }

    fun dismissGoogleSignInNotice() {
        _googleAuthNotice.value = null
    }

    fun reopenAccountChoice() {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(accountMode = AccountMode.UNDECIDED) }
        }
    }

    fun saveTimeControlPreference(
        useCustom: Boolean,
        presetLabel: String,
        customMinutes: Int,
        customSeconds: Int,
        customIncrementSec: Int
    ) {
        viewModelScope.launch {
            settingsStore.updateSettings {
                it.copy(
                    useCustomTimeDefault = useCustom,
                    selectedTimeControlLabel = presetLabel,
                    customMinutes = customMinutes.coerceIn(0, 180),
                    customSeconds = customSeconds.coerceIn(0, 59),
                    customIncrementSec = customIncrementSec.coerceIn(0, 60)
                )
            }
        }
    }

    fun setCoachDavidDefault(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(coachDavidDefault = enabled) }
        }
    }

    fun selectAppTheme(theme: AppThemeOption) {
        viewModelScope.launch {
            settingsStore.updateSettings {
                it.copy(appTheme = theme, isDarkTheme = theme.isDark)
            }
        }
    }

    fun selectBoardTheme(boardTheme: BoardTheme) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(boardTheme = boardTheme) }
        }
    }

    fun selectPieceStyle(pieceStyle: PieceStyle) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(pieceStyle = pieceStyle) }
        }
    }

    fun selectBackgroundPreset(preset: ArenaBackgroundPreset) {
        viewModelScope.launch {
            settingsStore.updateSettings { it.copy(backgroundPreset = preset) }
        }
    }

    fun updateCustomGradient(startArgb: Long, endArgb: Long) {
        viewModelScope.launch {
            settingsStore.updateSettings {
                it.copy(
                    backgroundPreset = ArenaBackgroundPreset.CUSTOM_GRADIENT,
                    customGradientStartArgb = startArgb,
                    customGradientEndArgb = endArgb
                )
            }
        }
    }

    fun updateBackgroundDim(alpha: Float) {
        viewModelScope.launch {
            settingsStore.updateSettings {
                it.copy(backgroundDimAlpha = alpha.coerceIn(0.15f, 0.85f))
            }
        }
    }

    fun saveCustomBackgroundFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val outDir = context.filesDir
                    outDir.listFiles { _, name -> name.startsWith("custom_arena_bg_") }
                        ?.forEach { it.delete() }

                    val targetFile = File(outDir, "custom_arena_bg_${System.currentTimeMillis()}.jpg")
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                    var sampleSize = 1
                    while (maxDim / sampleSize > 1600) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOpts)
                    }
                    if (bitmap != null) {
                        FileOutputStream(targetFile).use { fos ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                        }
                        bitmap.recycle()
                        targetFile.absolutePath
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                            targetFile.absolutePath
                        }
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (!savedPath.isNullOrBlank()) {
                settingsStore.updateSettings {
                    it.copy(
                        backgroundPreset = ArenaBackgroundPreset.CUSTOM_IMAGE,
                        customBackgroundImagePath = savedPath
                    )
                }
            }
        }
    }

    fun clearCustomBackgroundImage(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.filesDir.listFiles { _, name -> name.startsWith("custom_arena_bg_") }
                    ?.forEach { it.delete() }
            }
            settingsStore.updateSettings { s ->
                s.copy(
                    customBackgroundImagePath = null,
                    backgroundPreset = if (s.backgroundPreset == ArenaBackgroundPreset.CUSTOM_IMAGE) {
                        ArenaBackgroundPreset.EMERALD_AURORA
                    } else s.backgroundPreset
                )
            }
        }
    }

    fun applyComparisonSetup(setup: ThemeComparisonSetup) {
        viewModelScope.launch {
            settingsStore.updateSettings { cur ->
                cur.copy(
                    appTheme = setup.appTheme,
                    isDarkTheme = setup.appTheme.isDark,
                    boardTheme = setup.boardTheme,
                    pieceStyle = setup.pieceStyle,
                    backgroundPreset = if (setup.backgroundPreset == ArenaBackgroundPreset.CUSTOM_IMAGE &&
                        setup.customBackgroundImagePath.isNullOrBlank() &&
                        cur.customBackgroundImagePath.isNullOrBlank()
                    ) ArenaBackgroundPreset.EMERALD_AURORA else setup.backgroundPreset,
                    customGradientStartArgb = setup.customGradientStartArgb,
                    customGradientEndArgb = setup.customGradientEndArgb,
                    customBackgroundImagePath = setup.customBackgroundImagePath ?: cur.customBackgroundImagePath,
                    backgroundDimAlpha = setup.backgroundDimAlpha
                )
            }
        }
    }

    fun achievementsFor(p: PlayerProfileEntity): List<AchievementItem> =
        repository.computeAchievements(p)

    fun observeTournamentParticipants(tournamentId: Long): Flow<List<TournamentParticipantEntity>> =
        repository.observeTournamentParticipants(tournamentId)

    fun observeTournamentMatches(tournamentId: Long): Flow<List<TournamentMatchEntity>> =
        repository.observeTournamentMatches(tournamentId)

    fun claimDailyLoginReward() {
        viewModelScope.launch {
            val xp = repository.claimDailyLoginReward()
            if (xp != null) {
                _dailyRewardBannerXp.value = xp
            }
        }
    }

    fun dismissDailyRewardBanner() {
        _dailyRewardBannerXp.value = null
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsStore.updateSettings(transform)
        }
    }

    fun updateProfile(username: String, countryCode: String, bio: String) {
        viewModelScope.launch {
            repository.updateProfileDetails(username, countryCode, bio)
        }
    }

    fun createTournament(
        title: String,
        systemType: String,
        variant: GameVariant,
        timeControlLabel: String,
        maxRounds: Int
    ) {
        viewModelScope.launch {
            repository.createTournament(title, systemType, variant, timeControlLabel, maxRounds)
        }
    }

    fun toggleTournamentRegistration(tournamentId: Long) {
        viewModelScope.launch {
            repository.toggleTournamentRegistration(tournamentId)
        }
    }

    fun advanceTournamentRound(tournamentId: Long) {
        viewModelScope.launch {
            repository.startOrAdvanceTournamentRound(tournamentId)
        }
    }

    fun toggleFriend(memberId: Long) {
        viewModelScope.launch { repository.toggleFriend(memberId) }
    }

    fun acceptFriendRequest(memberId: Long) {
        viewModelScope.launch { repository.acceptFriendRequest(memberId) }
    }

    fun toggleFollow(memberId: Long) {
        viewModelScope.launch { repository.toggleFollow(memberId) }
    }

    fun toggleBanMember(memberId: Long) {
        viewModelScope.launch { repository.toggleBanMember(memberId) }
    }

    fun submitReport(targetUsername: String, reason: String, details: String) {
        viewModelScope.launch { repository.submitReport(targetUsername, reason, details) }
    }

    fun updateReportStatus(report: ModerationReportEntity, status: String) {
        viewModelScope.launch { repository.updateReportStatus(report, status) }
    }

    fun publishAnnouncement(title: String, body: String, priority: String) {
        viewModelScope.launch { repository.publishAnnouncement(title, body, priority) }
    }

    fun deleteAnnouncement(id: Long) {
        viewModelScope.launch { repository.deleteAnnouncement(id) }
    }
}
