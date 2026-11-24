package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Luxury Dark Palette
val ObsidianBlack = Color(0xFF070B12)
val DeepNavy = Color(0xFF0D1422)
val SurfaceSlate = Color(0xFF151E30)
val SurfaceElevated = Color(0xFF1D283D)
val BorderSubtle = Color(0xFF28354D)

// Accents
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldLight = Color(0xFF34D399)
val EmeraldDark = Color(0xFF059669)
val ChampionshipGold = Color(0xFFF59E0B)
val GoldSoft = Color(0xFFFBBF24)
val ElectricBlue = Color(0xFF3B82F6)
val ElectricCyan = Color(0xFF06B6D4)
val CrimsonRed = Color(0xFFEF4444)

// Text
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)

// Light Palette
val LightBackground = Color(0xFFF3F6FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE6ECF5)
val LightBorder = Color(0xFFCBD5E1)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// Classification Colors
val BrilliantCyan = Color(0xFF22D3EE)
val BestGreen = Color(0xFF22C55E)
val ExcellentGreen = Color(0xFF84CC16)
val GoodNeutral = Color(0xFF94A3B8)
val InaccuracyYellow = Color(0xFFFACC15)
val MistakeOrange = Color(0xFFFB923C)
val BlunderRed = Color(0xFFEF4444)

enum class AppThemeOption(
    val displayName: String,
    val isDark: Boolean,
    val primaryPreview: Color,
    val secondaryPreview: Color,
    val backgroundPreview: Color,
    val surfacePreview: Color
) {
    GRANDMASTER_EMERALD(
        displayName = "Grandmaster Emerald",
        isDark = true,
        primaryPreview = EmeraldPrimary,
        secondaryPreview = ChampionshipGold,
        backgroundPreview = ObsidianBlack,
        surfacePreview = SurfaceSlate
    ),
    MIDNIGHT_ROYAL(
        displayName = "Midnight Royal",
        isDark = true,
        primaryPreview = Color(0xFF3B82F6),
        secondaryPreview = Color(0xFF22D3EE),
        backgroundPreview = Color(0xFF060A17),
        surfacePreview = Color(0xFF141E36)
    ),
    CRIMSON_GRAND_PRIX(
        displayName = "Crimson Grand Prix",
        isDark = true,
        primaryPreview = Color(0xFFF43F5E),
        secondaryPreview = ChampionshipGold,
        backgroundPreview = Color(0xFF12070B),
        surfacePreview = Color(0xFF28131A)
    ),
    IMPERIAL_GOLD(
        displayName = "Imperial Gold",
        isDark = true,
        primaryPreview = Color(0xFFF59E0B),
        secondaryPreview = Color(0xFF34D399),
        backgroundPreview = Color(0xFF100C05),
        surfacePreview = Color(0xFF261E10)
    ),
    NORDIC_FROST(
        displayName = "Nordic Frost",
        isDark = false,
        primaryPreview = Color(0xFF0284C7),
        secondaryPreview = Color(0xFF0D9488),
        backgroundPreview = Color(0xFFF1F5F9),
        surfacePreview = Color(0xFFFFFFFF)
    ),
    WARM_IVORY(
        displayName = "Warm Ivory",
        isDark = false,
        primaryPreview = Color(0xFF9A3412),
        secondaryPreview = Color(0xFF047857),
        backgroundPreview = Color(0xFFFAF6EE),
        surfacePreview = Color(0xFFFFFCF5)
    )
}

enum class BoardTheme(
    val displayName: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val highlightFrom: Color,
    val highlightTo: Color,
    val selectedSquare: Color
) {
    TOURNAMENT_EMERALD(
        "Tournament Emerald",
        Color(0xFFEEEED2),
        Color(0xFF4A7C59),
        Color(0x66F59E0B),
        Color(0x88FBBF24),
        Color(0x993B82F6)
    ),
    CLASSIC_WALNUT(
        "Classic Walnut",
        Color(0xFFF0D9B5),
        Color(0xFFB58863),
        Color(0x66F59E0B),
        Color(0x88FBBF24),
        Color(0x9910B981)
    ),
    MIDNIGHT_NAVY(
        "Midnight Navy",
        Color(0xFFCBD5E1),
        Color(0xFF334155),
        Color(0x663B82F6),
        Color(0x8806B6D4),
        Color(0x99F59E0B)
    ),
    OBSIDIAN_SLATE(
        "Obsidian Slate",
        Color(0xFF9CA3AF),
        Color(0xFF1F2937),
        Color(0x6610B981),
        Color(0x8834D399),
        Color(0x99F59E0B)
    ),
    ROYAL_MARBLE(
        "Royal Marble",
        Color(0xFFF5F3EE),
        Color(0xFF5B6B7C),
        Color(0x66F59E0B),
        Color(0x883B82F6),
        Color(0x9910B981)
    ),
    TOURNAMENT_CRIMSON(
        "Tournament Crimson",
        Color(0xFFF8ECE8),
        Color(0xFF993B4B),
        Color(0x66F59E0B),
        Color(0x88F43F5E),
        Color(0x993B82F6)
    ),
    JADE_CRYSTAL(
        "Jade Crystal",
        Color(0xFFE2F5EE),
        Color(0xFF1E7B64),
        Color(0x6610B981),
        Color(0x8822D3EE),
        Color(0x99F59E0B)
    ),
    SAPPHIRE_ICE(
        "Sapphire Ice",
        Color(0xFFEBF4FF),
        Color(0xFF2F5D99),
        Color(0x663B82F6),
        Color(0x8806B6D4),
        Color(0x99F59E0B)
    )
}

enum class PieceStyle(val displayName: String) {
    CLASSIC_STAUNTON("Classic Staunton"),
    NEO_GEOMETRIC("Neo Geometric"),
    ROYAL_GOLD("Royal Gold & Ivory"),
    TOURNAMENT_CRYSTAL("Crystal Glass")
}

enum class ArenaBackgroundPreset(
    val displayName: String,
    val categoryLabel: String
) {
    SOLID_THEME("Solid Theme", "Minimal"),
    EMERALD_AURORA("Emerald Aurora", "Dynamic"),
    COSMIC_NEBULA("Cosmic Nebula", "Dynamic"),
    ROYAL_GOLD_DAMASK("Royal Gold Lattice", "Luxury"),
    CYBER_GRID("Cyber Arena Grid", "Esports"),
    GRANDMASTER_ARENA("Grandmaster Hall", "Artwork"),
    MARBLE_HALL("Marble Sanctuary", "Artwork"),
    MAHOGANY_CLUB("Mahogany Club", "Artwork"),
    CUSTOM_GRADIENT("Custom Gradient", "Custom"),
    CUSTOM_IMAGE("Custom Photo", "Custom")
}

val CUSTOM_GRADIENT_SWATCHES: List<Long> = listOf(
    0xFF0F172AL,
    0xFF065F46L,
    0xFF1E1B4BL,
    0xFF4C1D95L,
    0xFF7F1D1DL,
    0xFF78350FL,
    0xFF0E7490L,
    0xFF111827L,
    0xFFBE185DL,
    0xFFD1FAE5L,
    0xFFFEF3C7L,
    0xFFE0F2FEL
)

data class ThemeComparisonSetup(
    val id: String,
    val title: String,
    val authorLabel: String,
    val ratingLabel: String,
    val appTheme: AppThemeOption,
    val boardTheme: BoardTheme,
    val pieceStyle: PieceStyle,
    val backgroundPreset: ArenaBackgroundPreset,
    val customGradientStartArgb: Long = 0xFF0F172AL,
    val customGradientEndArgb: Long = 0xFF065F46L,
    val customBackgroundImagePath: String? = null,
    val backgroundDimAlpha: Float = 0.45f
)

val COMMUNITY_THEME_PRESETS: List<ThemeComparisonSetup> = listOf(
    ThemeComparisonSetup(
        id = "gm_valkyrie",
        title = "Nordic Marble Pro",
        authorLabel = "GM_Valkyrie (NO)",
        ratingLabel = "2540 Blitz",
        appTheme = AppThemeOption.MIDNIGHT_ROYAL,
        boardTheme = BoardTheme.ROYAL_MARBLE,
        pieceStyle = PieceStyle.CLASSIC_STAUNTON,
        backgroundPreset = ArenaBackgroundPreset.MARBLE_HALL
    ),
    ThemeComparisonSetup(
        id = "hikaru_tactics",
        title = "Crimson Speed Arena",
        authorLabel = "Hikaru_Tactics (US)",
        ratingLabel = "2610 Bullet",
        appTheme = AppThemeOption.CRIMSON_GRAND_PRIX,
        boardTheme = BoardTheme.TOURNAMENT_CRIMSON,
        pieceStyle = PieceStyle.NEO_GEOMETRIC,
        backgroundPreset = ArenaBackgroundPreset.CYBER_GRID
    ),
    ThemeComparisonSetup(
        id = "sakura_k960",
        title = "Nebula 960 Crystal",
        authorLabel = "Sakura_K960 (JP)",
        ratingLabel = "2420 Chess960",
        appTheme = AppThemeOption.MIDNIGHT_ROYAL,
        boardTheme = BoardTheme.SAPPHIRE_ICE,
        pieceStyle = PieceStyle.TOURNAMENT_CRYSTAL,
        backgroundPreset = ArenaBackgroundPreset.COSMIC_NEBULA
    ),
    ThemeComparisonSetup(
        id = "aarav_viswa",
        title = "Imperial Wood Study",
        authorLabel = "Aarav_Viswa (IN)",
        ratingLabel = "2485 Classical",
        appTheme = AppThemeOption.IMPERIAL_GOLD,
        boardTheme = BoardTheme.CLASSIC_WALNUT,
        pieceStyle = PieceStyle.ROYAL_GOLD,
        backgroundPreset = ArenaBackgroundPreset.MAHOGANY_CLUB
    ),
    ThemeComparisonSetup(
        id = "elena_endgame",
        title = "Daylight Frost Club",
        authorLabel = "Elena_Endgame (FR)",
        ratingLabel = "2390 Rapid",
        appTheme = AppThemeOption.NORDIC_FROST,
        boardTheme = BoardTheme.JADE_CRYSTAL,
        pieceStyle = PieceStyle.CLASSIC_STAUNTON,
        backgroundPreset = ArenaBackgroundPreset.CUSTOM_GRADIENT,
        customGradientStartArgb = 0xFFE0F2FEL,
        customGradientEndArgb = 0xFFD1FAE5L,
        backgroundDimAlpha = 0.20f
    )
)
