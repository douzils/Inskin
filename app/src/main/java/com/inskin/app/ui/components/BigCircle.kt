package com.inskin.app.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inskin.app.ui.theme.DarkCircle

@Composable
fun BigCircle(size: Dp = 320.dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(DarkCircle),
        contentAlignment = Alignment.Center,
        content = content
    )
}
