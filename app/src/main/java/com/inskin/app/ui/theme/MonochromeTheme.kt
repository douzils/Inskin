package com.inskin.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette monochrome pour le thème
 */
object MonochromeColors {
    // Noir/Gris/Blanc
    val black = Color(0xFF000000)
    val almostBlack = Color(0xFF0A0A0A)
    val darkGray1 = Color(0xFF1A1A1A)
    val darkGray2 = Color(0xFF2A2A2A)
    val darkGray3 = Color(0xFF3A3A3A)
    val gray1 = Color(0xFF4A4A4A)
    val gray2 = Color(0xFF6A6A6A)
    val gray3 = Color(0xFF8A8A8A)
    val lightGray1 = Color(0xFFAAAAAA)
    val lightGray2 = Color(0xFFCCCCCC)
    val lightGray3 = Color(0xFFE8E8E8)
    val almostWhite = Color(0xFFF5F5F5)
    val white = Color(0xFFFFFFFF)
}

/**
 * Couleurs d'accent disponibles
 */
enum class AccentColor(val color: Color, val displayName: String) {
    ELECTRIC_BLUE(Color(0xFF00D9FF), "Bleu Électrique"),
    NEON_GREEN(Color(0xFF00FF85), "Vert Néon"),
    HOT_PINK(Color(0xFFFF0080), "Rose Vif"),
    CYBER_PURPLE(Color(0xFFB026FF), "Violet Cyber"),
    LASER_RED(Color(0xFFFF0040), "Rouge Laser"),
    GOLDEN_YELLOW(Color(0xFFFFD700), "Jaune Doré"),
    MINT_GREEN(Color(0xFF00FFC8), "Vert Menthe"),
    ORANGE_BURST(Color(0xFFFF6B00), "Orange Éclatant"),
    ULTRA_VIOLET(Color(0xFF8B00FF), "Ultra Violet"),
    ICE_BLUE(Color(0xFF00E5FF), "Bleu Glacé");

    companion object {
        fun fromOrdinal(ordinal: Int): AccentColor {
            return entries.getOrNull(ordinal) ?: ELECTRIC_BLUE
        }
    }
}

/**
 * Extension pour obtenir la couleur d'accent actuelle
 */
fun AccentColor.withAlpha(alpha: Float): Color {
    return this.color.copy(alpha = alpha)
}
