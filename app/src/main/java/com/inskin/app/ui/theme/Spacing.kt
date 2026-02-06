package com.inskin.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Système d'espacement cohérent pour toute l'application
 */
object Spacing {
    // Espacements de base (multiples de 4)
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp

    // Espacements spécifiques
    val tiny: Dp = 2.dp
    val cardPadding: Dp = 16.dp
    val screenPadding: Dp = 20.dp
    val sectionPadding: Dp = 24.dp
    val buttonPadding: Dp = 16.dp

    // Espacements verticaux
    val verticalTiny: Dp = 4.dp
    val verticalSmall: Dp = 8.dp
    val verticalMedium: Dp = 16.dp
    val verticalLarge: Dp = 24.dp
    val verticalXLarge: Dp = 32.dp

    // Espacements horizontaux
    val horizontalTiny: Dp = 4.dp
    val horizontalSmall: Dp = 8.dp
    val horizontalMedium: Dp = 16.dp
    val horizontalLarge: Dp = 24.dp
    val horizontalXLarge: Dp = 32.dp
}

/**
 * Dimensions pour les composants
 */
object Dimensions {
    // Tailles d'icônes
    val iconTiny: Dp = 16.dp
    val iconSmall: Dp = 20.dp
    val iconMedium: Dp = 24.dp
    val iconLarge: Dp = 32.dp
    val iconXLarge: Dp = 48.dp
    val iconXXLarge: Dp = 64.dp

    // Tailles de boutons
    val buttonHeightSmall: Dp = 36.dp
    val buttonHeightMedium: Dp = 48.dp
    val buttonHeightLarge: Dp = 56.dp

    // Tailles de badges/chips
    val badgeSize: Dp = 80.dp
    val badgeSizeLarge: Dp = 120.dp
    val chipHeight: Dp = 32.dp

    // Barres de signal
    val signalBarWidth: Dp = 6.dp
    val signalBarMaxHeight: Dp = 32.dp

    // Éléments NFC
    val nfcCircleSize: Dp = 200.dp
    val nfcCircleSizeLarge: Dp = 280.dp

    // Cartes et conteneurs
    val cardMinHeight: Dp = 80.dp
    val cardMaxWidth: Dp = 600.dp

    // Séparateurs
    val dividerThickness: Dp = 1.dp
    val thickDivider: Dp = 2.dp
}

/**
 * Rayons de coins pour cohérence
 */
object CornerRadius {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    val round: Dp = 50.dp // Pour les pills/badges arrondis
}

/**
 * Élévations (shadows)
 */
object Elevation {
    val none: Dp = 0.dp
    val xs: Dp = 1.dp
    val sm: Dp = 2.dp
    val md: Dp = 4.dp
    val lg: Dp = 8.dp
    val xl: Dp = 12.dp
    val xxl: Dp = 16.dp
}

/**
 * Durées d'animation (en millisecondes)
 */
object AnimationDuration {
    const val instant = 100
    const val fast = 200
    const val medium = 300
    const val slow = 500
    const val verySlow = 1000
}
