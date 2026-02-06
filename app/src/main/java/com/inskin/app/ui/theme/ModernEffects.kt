package com.inskin.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==================== GRADIENT MODIFIERS ====================

/**
 * Applique un dégradé vertical
 */
fun Modifier.verticalGradient(
    colorStops: List<Pair<Float, Color>>,
    shape: Shape = RoundedCornerShape(0.dp)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            *colorStops.toTypedArray()
        )
    )

/**
 * Applique un dégradé horizontal
 */
fun Modifier.horizontalGradient(
    colorStops: List<Pair<Float, Color>>,
    shape: Shape = RoundedCornerShape(0.dp)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.horizontalGradient(
            *colorStops.toTypedArray()
        )
    )

/**
 * Dégradé diagonal (haut-gauche vers bas-droite)
 */
fun Modifier.diagonalGradient(
    startColor: Color,
    endColor: Color,
    shape: Shape = RoundedCornerShape(0.dp)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            0.0f to startColor,
            1.0f to endColor
        )
    )

/**
 * Dégradé radial (du centre vers l'extérieur)
 */
fun Modifier.radialGradient(
    centerColor: Color,
    edgeColor: Color,
    shape: Shape = RoundedCornerShape(0.dp)
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.radialGradient(
            0.0f to centerColor,
            1.0f to edgeColor
        )
    )

// ==================== GLASSMORPHISM ====================

/**
 * Effet glassmorphism moderne avec transparence et flou
 */
fun Modifier.glassmorphic(
    backgroundColor: Color = Color.White.copy(alpha = 0.1f),
    shape: Shape = RoundedCornerShape(16.dp),
    blurRadius: Dp = 10.dp,
    borderColor: Color = Color.White.copy(alpha = 0.2f)
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .blur(blurRadius)

/**
 * Carte glass avec ombre douce
 */
fun Modifier.glassCard(
    isDark: Boolean = false,
    cornerRadius: Dp = 16.dp
): Modifier = this
    .shadow(
        elevation = 4.dp,
        shape = RoundedCornerShape(cornerRadius),
        spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.1f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        if (isDark) InskinColors.glassDark else InskinColors.glassLight
    )

// ==================== NEUMORPHISM ====================

/**
 * Effet neumorphic avec double ombre
 */
fun Modifier.neumorphic(
    lightShadowColor: Color = Color.White.copy(alpha = 0.7f),
    darkShadowColor: Color = Color.Black.copy(alpha = 0.25f),
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(elevation, shape, spotColor = darkShadowColor, ambientColor = lightShadowColor)
    .clip(shape)

// ==================== ENHANCED SHADOWS ====================

/**
 * Ombre colorée moderne
 */
fun Modifier.coloredShadow(
    color: Color,
    alpha: Float = 0.3f,
    elevation: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    spotColor = color.copy(alpha = alpha),
    ambientColor = color.copy(alpha = alpha * 0.5f)
)

/**
 * Ombre douce et diffuse
 */
fun Modifier.softShadow(
    elevation: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(16.dp)
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    spotColor = Color.Black.copy(alpha = 0.08f),
    ambientColor = Color.Black.copy(alpha = 0.04f)
)

// ==================== GRADIENT PRESETS ====================

object GradientPresets {
    val nfcSignal = listOf(
        0.0f to InskinColors.gradientNfcStart,
        1.0f to InskinColors.gradientNfcEnd
    )

    val primary = listOf(
        0.0f to InskinColors.gradientPrimaryStart,
        1.0f to InskinColors.gradientPrimaryEnd
    )

    val success = listOf(
        0.0f to InskinColors.gradientSuccessStart,
        1.0f to InskinColors.gradientSuccessEnd
    )

    val warning = listOf(
        0.0f to InskinColors.gradientWarningStart,
        1.0f to InskinColors.gradientWarningEnd
    )

    val ocean = listOf(
        0.0f to Color(0xFF2E3192),
        0.5f to Color(0xFF1BFFFF),
        1.0f to Color(0xFF00E4FF)
    )

    val sunset = listOf(
        0.0f to Color(0xFFFF6B6B),
        0.5f to Color(0xFFFFE66D),
        1.0f to Color(0xFF4ECDC4)
    )

    val space = listOf(
        0.0f to Color(0xFF000428),
        1.0f to Color(0xFF004E92)
    )
}

// ==================== HELPER COMPOSABLES ====================

/**
 * Container avec dégradé de fond
 */
@Composable
fun GradientBox(
    gradient: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.verticalGradient(gradient, shape)
    ) {
        content()
    }
}

/**
 * Card avec effet glass moderne
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.glassCard(isDark, cornerRadius)
    ) {
        content()
    }
}
