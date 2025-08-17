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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ReadScreen(tagInfoFlow: StateFlow<TagInfo?>) {
    val tagInfo by tagInfoFlow.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    if (tagInfo == null) {
        val transition = rememberInfiniteTransition(label = "nfc")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Nfc, contentDescription = "nfc", modifier = Modifier.size(96.dp), tint = LocalContentColor.current.copy(alpha))
            Spacer(Modifier.height(16.dp))
            Text(text = stringResource(R.string.waiting_scan))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("UID: ${'$'}{tagInfo.uid}")
            Text("Type: ${'$'}{tagInfo.type}")
            Text("Size: ${'$'}{tagInfo.size}/${'$'}{tagInfo.used}")
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = { clipboard.setText(AnnotatedString(tagInfo.uid ?: "")) }) {
                    Text(stringResource(R.string.copy_uid))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, tagInfo.uid)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }) {
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewWaiting() {
    val flow = MutableStateFlow<TagInfo?>(null)
    ReadScreen(flow)
}

@Preview
@Composable
private fun PreviewWithTag() {
    val info = TagInfo("NDEF", listOf("android.nfc.tech.Ndef"), "DEADBEEF", null, null, 1024, 128, true, false)
    val flow = MutableStateFlow<TagInfo?>(info)
    ReadScreen(flow)
}
