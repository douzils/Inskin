package com.inskin.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Data about a scanned NFC tag. */
data class TagInfo(
    val type: String,
    val tech: String,
    val serial: String,
    val atqa: String,
    val sak: String,
    val format: String,
    val size: Int,
    val used: Int,
    val isWritable: Boolean,
    val isReadOnly: Boolean
)

/**
 * Screen used to read an NFC tag. Displays waiting instructions when no tag is
 * scanned and shows details once one is detected.
 */
@Composable
fun ReadScreen(tagInfo: TagInfo? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (tagInfo == null) {
            val transition = rememberInfiniteTransition(label = "waiting")
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    tween(1000, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "nfc_scale"
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("En attente d'un scan")
                Icon(
                    Icons.Filled.Nfc,
                    contentDescription = "NFC",
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(120.dp)
                        .scale(scale)
                )
            }
        } else {
            val scale by animateFloatAsState(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = 500),
                label = "nfc_pulse"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.Nfc,
                    contentDescription = "NFC",
                    modifier = Modifier
                        .size(96.dp)
                        .scale(scale)
                )
                Spacer(Modifier.height(16.dp))
                InfoRow("Type de Tag", tagInfo.type)
                InfoRow("Technologie disponible", tagInfo.tech)
                InfoRow("Numéro de Série", tagInfo.serial)
                InfoRow("ATQA", tagInfo.atqa)
                InfoRow("SAK", tagInfo.sak)
                InfoRow("Format de données", tagInfo.format)
                Spacer(Modifier.height(8.dp))
                MemoryBar(tagInfo.size, tagInfo.used)
                InfoRow("Taille", "${tagInfo.used}/${tagInfo.size} octets")
                InfoRow("Écriture Possible", if (tagInfo.isWritable) "Oui" else "Non")
                InfoRow("Lecture Seule possible", if (tagInfo.isReadOnly) "Oui" else "Non")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = "$label : ", fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}

@Composable
private fun MemoryBar(total: Int, used: Int) {
    val progress = if (total > 0) used.toFloat() / total else 0f
    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    )
}

@Preview
@Composable
fun ReadScreenPreview() {
    ReadScreen(
        tagInfo = TagInfo(
            type = "MIFARE Ultralight",
            tech = "NFC-A",
            serial = "04A224B12C34",
            atqa = "0x4400",
            sak = "0x00",
            format = "NDEF",
            size = 144,
            used = 32,
            isWritable = true,
            isReadOnly = false
        )
    )
}
