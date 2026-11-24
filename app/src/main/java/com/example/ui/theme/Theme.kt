package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GrandmasterEmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF031A10),
    primaryContainer = Color(0xFF0B3B2B),
    onPrimaryContainer = EmeraldLight,
    secondary = ChampionshipGold,
    onSecondary = Color(0xFF1F1200),
    secondaryContainer = Color(0xFF3A2807),
    onSecondaryContainer = GoldSoft,
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF142B52),
    onTertiaryContainer = Color(0xFF93C5FD),
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = DeepNavy,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceSlate,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceSlate,
    surfaceContainerHigh = SurfaceElevated,
    outline = BorderSubtle,
    error = CrimsonRed,
    onError = Color.White
)

private val MidnightRoyalColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFF041432),
    primaryContainer = Color(0xFF172B54),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF04242C),
    secondaryContainer = Color(0xFF0E3747),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = ChampionshipGold,
    onTertiary = Color(0xFF1F1200),
    tertiaryContainer = Color(0xFF3B2809),
    onTertiaryContainer = GoldSoft,
    background = Color(0xFF060A17),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0D1426),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF141E36),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF141E36),
    surfaceContainerHigh = Color(0xFF1D2A4A),
    outline = Color(0xFF2D3E63),
    error = CrimsonRed,
    onError = Color.White
)

private val CrimsonGrandPrixColorScheme = darkColorScheme(
    primary = Color(0xFFF43F5E),
    onPrimary = Color(0xFF2B050C),
    primaryContainer = Color(0xFF4C111E),
    onPrimaryContainer = Color(0xFFFECDD3),
    secondary = ChampionshipGold,
    onSecondary = Color(0xFF1F1200),
    secondaryContainer = Color(0xFF3D2908),
    onSecondaryContainer = GoldSoft,
    tertiary = ElectricCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF0E3545),
    onTertiaryContainer = Color(0xFFA5F3FC),
    background = Color(0xFF12070B),
    onBackground = Color(0xFFFFF1F2),
    surface = Color(0xFF1C0C12),
    onSurface = Color(0xFFFFF1F2),
    surfaceVariant = Color(0xFF28131A),
    onSurfaceVariant = Color(0xFFD6A8B2),
    surfaceContainer = Color(0xFF28131A),
    surfaceContainerHigh = Color(0xFF381B25),
    outline = Color(0xFF4F2734),
    error = Color(0xFFFB7185),
    onError = Color(0xFF2B050C)
)

private val ImperialGoldColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF261600),
    primaryContainer = Color(0xFF422C05),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = EmeraldLight,
    onSecondary = Color(0xFF032014),
    secondaryContainer = Color(0xFF0A3826),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF281100),
    tertiaryContainer = Color(0xFF452007),
    onTertiaryContainer = Color(0xFFFED7AA),
    background = Color(0xFF100C05),
    onBackground = Color(0xFFFEF9EE),
    surface = Color(0xFF1A140A),
    onSurface = Color(0xFFFEF9EE),
    surfaceVariant = Color(0xFF261E10),
    onSurfaceVariant = Color(0xFFCBBBA0),
    surfaceContainer = Color(0xFF261E10),
    surfaceContainerHigh = Color(0xFF342916),
    outline = Color(0xFF4C3C22),
    error = CrimsonRed,
    onError = Color.White
)

private val NordicFrostLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF1E3A8A),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val WarmIvoryLightColorScheme = lightColorScheme(
    primary = Color(0xFF9A3412),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFF047857),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFFAF6EE),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFEFE6D5),
    onSurfaceVariant = Color(0xFF57534E),
    surfaceContainer = Color(0xFFFFFCF5),
    surfaceContainerHigh = Color(0xFFEFE6D5),
    outline = Color(0xFFD6CBB8),
    error = Color(0xFFDC2626),
    onError = Color.White
)

fun colorSchemeForTheme(appTheme: AppThemeOption, darkTheme: Boolean = appTheme.isDark): ColorScheme {
    val resolved = when {
        !darkTheme && appTheme.isDark -> AppThemeOption.NORDIC_FROST
        darkTheme && !appTheme.isDark -> AppThemeOption.GRANDMASTER_EMERALD
        else -> appTheme
    }
    return when (resolved) {
        AppThemeOption.GRANDMASTER_EMERALD -> GrandmasterEmeraldColorScheme
        AppThemeOption.MIDNIGHT_ROYAL -> MidnightRoyalColorScheme
        AppThemeOption.CRIMSON_GRAND_PRIX -> CrimsonGrandPrixColorScheme
        AppThemeOption.IMPERIAL_GOLD -> ImperialGoldColorScheme
        AppThemeOption.NORDIC_FROST -> NordicFrostLightColorScheme
        AppThemeOption.WARM_IVORY -> WarmIvoryLightColorScheme
    }
}

@Composable
fun GrandmasterTheme(
    appTheme: AppThemeOption = AppThemeOption.GRANDMASTER_EMERALD,
    darkTheme: Boolean = appTheme.isDark,
    content: @Composable () -> Unit
) {
    val colorScheme = colorSchemeForTheme(appTheme = appTheme, darkTheme = darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
