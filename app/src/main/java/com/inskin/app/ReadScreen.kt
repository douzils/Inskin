package com.inskin.app

import android.content.Intent
import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val NEXTV2_UID = "NEXTV2"
private val Context.tagMetaStore by preferencesDataStore("tag_meta")

@Composable
fun ReadScreen(tagInfoFlow: StateFlow<TagInfo?>) {
    val tagInfo = tagInfoFlow.collectAsState().value
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val dataStore = context.tagMetaStore
    var note by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }
    LaunchedEffect(tagInfo?.uid) {
        val uid = tagInfo?.uid ?: return@LaunchedEffect
        val prefs = dataStore.data.first()
        note = prefs[stringPreferencesKey("note_$uid")] ?: ""
        icon = prefs[stringPreferencesKey("icon_$uid")] ?: ""
    }
    if (tagInfo == null) {
        val transition = rememberInfiniteTransition(label = "nfc")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
            label = "alpha"
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
            val image = if (tagInfo.uid == NEXTV2_UID) R.drawable.ic_implant_nextv2 else null
            image?.let { Image(painterResource(it), contentDescription = null, modifier = Modifier.size(96.dp)) }
            if (icon.isNotBlank()) {
                Text(icon, style = MaterialTheme.typography.displayMedium)
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.tag_note_hint)) })
            OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text(stringResource(R.string.tag_icon_hint)) })
            Button(onClick = {
                tagInfo.uid?.let { uid ->
                    scope.launch {
                        dataStore.edit { prefs ->
                            prefs[stringPreferencesKey("note_$uid")] = note
                            prefs[stringPreferencesKey("icon_$uid")] = icon
                        }
                    }
                }
            }) { Text(stringResource(R.string.save_note)) }
            Spacer(Modifier.height(16.dp))
            Text("UID: ${'$'}{tagInfo.uid}")
            Text("Type: ${'$'}{tagInfo.type}")
            Text("Size: ${'$'}{tagInfo.size}/${'$'}{tagInfo.used}")
            tagInfo.records.forEach { Text(it) }
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = { clipboard.setText(AnnotatedString(tagInfo.uid ?: "")) }) {
                    Text(stringResource(R.string.copy_uid))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, tagInfo?.uid ?: "")
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    }
                ) {
                    Text(stringResource(R.string.share))
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
    val info = TagInfo("NDEF", listOf("android.nfc.tech.Ndef"), "DEADBEEF", null, null, 1024, 128, true, false, emptyList())
    val flow = MutableStateFlow<TagInfo?>(info)
    ReadScreen(flow)
}
