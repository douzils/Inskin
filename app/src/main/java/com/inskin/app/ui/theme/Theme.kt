package com.inskin.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==================== LIGHT THEME COLORS ====================

private val md_theme_light_primary = Color(0xFF0061A4)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFD1E4FF)
private val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
private val md_theme_light_secondary = Color(0xFF535F70)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFD7E3F7)
private val md_theme_light_onSecondaryContainer = Color(0xFF101C2B)
private val md_theme_light_tertiary = Color(0xFF6B5778)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFF2DAFF)
private val md_theme_light_onTertiaryContainer = Color(0xFF251431)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_background = Color(0xFFFDFCFF)
private val md_theme_light_onBackground = Color(0xFF1A1C1E)
private val md_theme_light_surface = Color(0xFFFDFCFF)
private val md_theme_light_onSurface = Color(0xFF1A1C1E)
private val md_theme_light_surfaceVariant = Color(0xFFDFE2EB)
private val md_theme_light_onSurfaceVariant = Color(0xFF43474E)
private val md_theme_light_outline = Color(0xFF73777F)
private val md_theme_light_inverseOnSurface = Color(0xFFF1F0F4)
private val md_theme_light_inverseSurface = Color(0xFF2F3033)
private val md_theme_light_inversePrimary = Color(0xFF9ECAFF)
private val md_theme_light_surfaceTint = Color(0xFF0061A4)

// ==================== DARK THEME COLORS ====================

private val md_theme_dark_primary = Color(0xFF9ECAFF)
private val md_theme_dark_onPrimary = Color(0xFF003258)
private val md_theme_dark_primaryContainer = Color(0xFF00497D)
private val md_theme_dark_onPrimaryContainer = Color(0xFFD1E4FF)
private val md_theme_dark_secondary = Color(0xFFBBC7DB)
private val md_theme_dark_onSecondary = Color(0xFF253140)
private val md_theme_dark_secondaryContainer = Color(0xFF3C4858)
private val md_theme_dark_onSecondaryContainer = Color(0xFFD7E3F7)
private val md_theme_dark_tertiary = Color(0xFFD6BEE4)
private val md_theme_dark_onTertiary = Color(0xFF3B2948)
private val md_theme_dark_tertiaryContainer = Color(0xFF523F5F)
private val md_theme_dark_onTertiaryContainer = Color(0xFFF2DAFF)
private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_theme_dark_background = Color(0xFF1A1C1E)
private val md_theme_dark_onBackground = Color(0xFFE2E2E6)
private val md_theme_dark_surface = Color(0xFF1A1C1E)
private val md_theme_dark_onSurface = Color(0xFFE2E2E6)
private val md_theme_dark_surfaceVariant = Color(0xFF43474E)
private val md_theme_dark_onSurfaceVariant = Color(0xFFC3C7CF)
private val md_theme_dark_outline = Color(0xFF8D9199)
private val md_theme_dark_inverseOnSurface = Color(0xFF1A1C1E)
private val md_theme_dark_inverseSurface = Color(0xFFE2E2E6)
private val md_theme_dark_inversePrimary = Color(0xFF0061A4)
private val md_theme_dark_surfaceTint = Color(0xFF9ECAFF)

// ==================== CUSTOM SEMANTIC COLORS ====================

object InskinColors {
    // Status Colors
    val success = Color(0xFF34C759)
    val successLight = Color(0xFF81C784)
    val successDark = Color(0xFF2E7D32)

    val warning = Color(0xFFFFB020)
    val warningLight = Color(0xFFFFD54F)
    val warningDark = Color(0xFFF57C00)

    val error = Color(0xFFFF3B30)
    val errorLight = Color(0xFFE57373)
    val errorDark = Color(0xFFC62828)

    val info = Color(0xFF007AFF)
    val infoLight = Color(0xFF64B5F6)
    val infoDark = Color(0xFF1976D2)

    // NFC Signal Strength
    val signalWeak = Color(0xFFFF5722)
    val signalMedium = Color(0xFFFFC107)
    val signalStrong = Color(0xFF4CAF50)
    val signalExcellent = Color(0xFF00BCD4)

    // Health Status (Implants)
    val healthExcellent = Color(0xFF4CAF50)
    val healthGood = Color(0xFF8BC34A)
    val healthFair = Color(0xFFFFC107)
    val healthWarning = Color(0xFFFF9800)
    val healthCritical = Color(0xFFFF5722)

    // Badge Forms
    val badgeCard = Color(0xFF4FC3F7)
    val badgeKeyfob = Color(0xFFFFB74D)
    val badgeImplant = Color(0xFF81C784)
    val badgeRing = Color(0xFFFFD700)
    val badgePhone = Color(0xFF607D8B)
    val badgeSticker = Color(0xFFBA68C8)
    val badgeWatch = Color(0xFFA1887F)

    // Implant Specific
    val implantXNT = Color(0xFF4CAF50)
    val implantXM1 = Color(0xFF2196F3)
    val implantXEM = Color(0xFFFF9800)
    val implantXAC = Color(0xFF9C27B0)
    val implantNExT = Color(0xFF00BCD4)
    val implantFlexNT = Color(0xFF8BC34A)
    val implantVivoKey = Color(0xFFE91E63)

    // Gradients (Start -> End)
    val gradientPrimaryStart = Color(0xFF0061A4)
    val gradientPrimaryEnd = Color(0xFF00B4DB)

    val gradientSuccessStart = Color(0xFF34C759)
    val gradientSuccessEnd = Color(0xFF00C851)

    val gradientWarningStart = Color(0xFFFFB020)
    val gradientWarningEnd = Color(0xFFFF6B6B)

    val gradientNfcStart = Color(0xFF667EEA)
    val gradientNfcEnd = Color(0xFF764BA2)

    // Surface overlays for glassmorphism
    val glassLight = Color(0xCCFFFFFF)
    val glassDark = Color(0xCC2A2A2E)
}

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
)

@Composable
fun InskinTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
