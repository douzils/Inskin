package com.inskin.app

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun ReadScreen(tagInfoFlow: StateFlow<TagInfo?>) {
    val tagInfo by tagInfoFlow.collectAsState()
    if (tagInfo == null) WaitingUi() else DetailsUi(tagInfo!!)
}

@Composable
private fun WaitingUi() {
    val alpha by rememberInfiniteTransition(label = "nfc")
        .animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
            label = "alpha"
        )
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Nfc, contentDescription = "NFC", modifier = Modifier.size(96.dp), tint = LocalContentColor.current.copy(alpha))
        Spacer(Modifier.height(12.dp))
        Text("Approchez un badge NFC…")
    }
}

@Composable
private fun DetailsUi(tagInfo: TagInfo) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("UID : ${tagInfo.serial}")
        Text("Type : ${tagInfo.type}")
        Text("Tech : ${tagInfo.tech}")
        Text("ATQA : ${tagInfo.atqa ?: "—"}")
        Text("SAK : ${tagInfo.sak ?: "—"}")
        Text("Format : ${tagInfo.format}")
        Spacer(Modifier.height(8.dp))
        Text("Taille : ${tagInfo.used}/${tagInfo.size} octets")
        Text("Écriture possible : ${if (tagInfo.isWritable) "Oui" else "Non"}")
        Text("Lecture seule : ${if (tagInfo.isReadOnly) "Oui" else "Non"}")

        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { clipboard.setText(AnnotatedString(tagInfo.serial)) }) {
                Text("Copier l’UID")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, tagInfo.serial)
                }
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Text("Partager")
            }
        }
    }
}

/* PREVIEWS (facultatif) */
@Composable
@Preview
private fun PreviewWaiting() {
    ReadScreen(MutableStateFlow(null))
}

@Composable
@Preview
private fun PreviewWithTag() {
    val info = TagInfo(
        uid = "DE:AD:BE:EF",
        type = "NDEF",
        techs = listOf("android.nfc.tech.Ndef", "android.nfc.tech.NfcA"),
        atqa = "0x4400",
        sak = "0x00",
        format = "NDEF",
        size = 144,
        used = 32,
        isWritable = true,
        isReadOnly = false
    )
    ReadScreen(MutableStateFlow(info))
}
