package com.inskin.app.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val scheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = DarkCircle,
    surface = Color.White,
    onSurface = Color(0xFF222222)
)
@Composable
fun InskinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
