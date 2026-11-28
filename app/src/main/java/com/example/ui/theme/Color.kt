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
        primaryPreview = Color(0xFF10B981),
        secondaryPreview = Color(0xFFF59E0B),
        backgroundPreview = Color(0xFF070B12),
        surfacePreview = Color(0xFF151E30)
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
        secondaryPreview = Color(0xFFF59E0B),
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
    ),
    ROSEWOOD_GOLD(
        "Rosewood Gold",
        Color(0xFFF5E6D3),
        Color(0xFF6B2D2B),
        Color(0x66F59E0B),
        Color(0x88FBBF24),
        Color(0x9910B981)
    ),
    SAKURA_BLOSSOM(
        "Sakura Blossom",
        Color(0xFFFFF1F5),
        Color(0xFF9D4E6E),
        Color(0x66EC4899),
        Color(0x88F472B6),
        Color(0x996366F1)
    ),
    CYBER_NEON_MATRIX(
        "Cyber Neon",
        Color(0xFF1E293B),
        Color(0xFF0B1120),
        Color(0x6622D3EE),
        Color(0x8806B6D4),
        Color(0x99F43F5E)
    ),
    NORDIC_BIRCH(
        "Nordic Birch",
        Color(0xFFF6EFE2),
        Color(0xFFA68B6B),
        Color(0x660284C7),
        Color(0x880D9488),
        Color(0x99F59E0B)
    ),
    AMETHYST_DUSK(
        "Amethyst Dusk",
        Color(0xFFF3E8FF),
        Color(0xFF5B3A82),
        Color(0x66A855F7),
        Color(0x88C084FC),
        Color(0x9922D3EE)
    ),
    DESERT_SANDSTONE(
        "Desert Sandstone",
        Color(0xFFF7EAD9),
        Color(0xFFB46B3D),
        Color(0x66F59E0B),
        Color(0x88EA580C),
        Color(0x990284C7)
    ),
    OCEAN_CORAL(
        "Ocean Coral",
        Color(0xFFECFEFF),
        Color(0xFF16697A),
        Color(0x6606B6D4),
        Color(0x882DD4BF),
        Color(0x99F97316)
    ),
    CARBON_MONOCHROME(
        "Carbon Platinum",
        Color(0xFFE5E7EB),
        Color(0xFF27272A),
        Color(0x6610B981),
        Color(0x883B82F6),
        Color(0x99F59E0B)
    )
}

enum class BoardPatternStyle(val displayName: String) {
    CLASSIC_FLAT("Clean Flat"),
    WOOD_BEVEL("Beveled Frame"),
    TOURNAMENT_BORDER("Gold Rim"),
    NEON_GRID("Neon Edge Grid")
}

enum class PieceStyle(val displayName: String) {
    CLASSIC_STAUNTON("Classic Staunton"),
    NEO_GEOMETRIC("Neo Geometric"),
    ROYAL_GOLD("Royal Gold & Ivory"),
    TOURNAMENT_CRYSTAL("Crystal Glass"),
    ROSEWOOD_MAPLE("Rosewood & Maple"),
    CYBER_NEON("Cyber Neon"),
    OBSIDIAN_PEARL("Obsidian & Pearl"),
    MINIMALIST_MODERN("Minimalist Bold")
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
