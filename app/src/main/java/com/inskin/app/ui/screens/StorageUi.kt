package com.inskin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inskin.app.TagDetails
import java.util.Locale
import kotlin.math.max

/* ========= Types ========= */

data class StorageSlice(
    val id: String,      // "system", "user_used", "user_free"
    val bytes: Int,
    val color: Color,
    val label: String
)

/* ========= Couleurs par défaut ========= */
private val DefaultSystem = Color(0xFFB0BEC5)
private val DefaultUsed   = Color(0xFF2962FF)
private val DefaultFree   = DefaultUsed.copy(alpha = 0.20f)

/* ========= Helpers non-@Composable ========= */

private fun guessNdefCapacity(typeLabel: String, typeDetail: String?): Int? {
    val txt = (typeLabel + " " + (typeDetail ?: "")).uppercase(Locale.ROOT)
    return when {
        "NTAG213" in txt -> 144
        "NTAG215" in txt -> 504
        "NTAG216" in txt -> 888
        "ULTRALIGHT C" in txt -> 144
        "MIFARE CLASSIC 1K" in txt -> 716
        "MIFARE CLASSIC 4K" in txt -> 3436
        else -> null
    }
}

private fun chooseNdefCapacity(
    typeLabel: String,
    typeDetail: String?,
    details: TagDetails?,
    ndefCapFromAndroid: Int
): Int = details?.ndefCapacity ?: guessNdefCapacity(typeLabel, typeDetail) ?: ndefCapFromAndroid

private fun chooseTotalBytes(
    typeLabel: String,
    typeDetail: String?,
    details: TagDetails?,
    ndefCap: Int,
    usedNdef: Int
): Int {
    details?.totalMemoryBytes?.let { return it }
    val txt = (typeLabel + " " + (typeDetail ?: "")).uppercase(Locale.ROOT)
    return if ("CLASSIC" in txt) {
        when {
            "4K" in txt -> 3440
            else -> 716
        }
    } else {
        max(ndefCap + 64, usedNdef)
    }
}

/** Public pour réutilisation croisée. */
fun fmtBytes(n: Int): String {
    if (n < 0) return "—"
    if (n < 1024) return "$n B"
    val kb = n / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.2f MB", mb)
}

/** Retourne Pair(totalBarBytes, slices). */
fun buildStorageSlices(
    typeLabel: String,
    typeDetail: String?,
    details: TagDetails?,
    usedNdef: Int,
    ndefCapFromAndroid: Int,
    colorUsed: Color = DefaultUsed,
    colorFree: Color = DefaultFree,
    colorSystem: Color = DefaultSystem
): Pair<Int, List<StorageSlice>> {
    val ndefCap = chooseNdefCapacity(typeLabel, typeDetail, details, ndefCapFromAndroid)
    val total = chooseTotalBytes(typeLabel, typeDetail, details, ndefCap, usedNdef)

    val txt = (typeLabel + " " + (typeDetail ?: "")).uppercase(Locale.ROOT)
    val out = mutableListOf<StorageSlice>()

    if ("CLASSIC" in txt) {
        val system = (total - max(usedNdef, 0)).coerceAtLeast(0)
        val userUsed = usedNdef.coerceAtLeast(0)
        val userFree = (total - system - userUsed).coerceAtLeast(0)
        if (system > 0)   out += StorageSlice("system",    system,   colorSystem, "Système")
        if (userUsed > 0) out += StorageSlice("user_used", userUsed, colorUsed,   "Données")
        if (userFree > 0) out += StorageSlice("user_free", userFree, colorFree,   "Libre")
    } else {
        val system  = (total - ndefCap).coerceAtLeast(0)
        val used    = usedNdef.coerceIn(0, ndefCap)
        val free    = (ndefCap - used).coerceAtLeast(0)
        if (system > 0) out += StorageSlice("system",    system, colorSystem, "Système")
        if (used > 0)   out += StorageSlice("user_used", used,   colorUsed,   "Données")
        if (free > 0)   out += StorageSlice("user_free", free,   colorFree,   "Libre")
    }

    val barTotal = out.sumOf { it.bytes }.coerceAtLeast(total)
    return barTotal to out
}

/* ========= UI ========= */

@Composable
fun SegmentedUsageBar(
    totalBytes: Int,
    slices: List<StorageSlice>,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    rounded: Boolean = true,
) {
    val total = totalBytes.coerceAtLeast(1)
    val shape = if (rounded) RoundedCornerShape(10.dp) else RoundedCornerShape(0.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxSize()) {
            slices.forEachIndexed { i, s ->
                val fraction = s.bytes / total.toFloat()
                if (fraction > 0f) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .weight(fraction)
                            .background(s.color)
                    )
                }
                if (i < slices.lastIndex) {
                    Spacer(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
fun StorageLegendRow(
    slices: List<StorageSlice>,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slices.filter { it.bytes > 0 }.forEach { s ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorDot(color = s.color, size = 12.dp)
                Text(
                    text = "${s.label}: ${fmtBytes(s.bytes)}",
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
