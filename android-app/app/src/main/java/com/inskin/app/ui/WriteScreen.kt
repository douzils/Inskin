package com.inskin.app.ui

import android.nfc.Tag
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inskin.app.nfc.NfcController
import com.inskin.app.nfc.NdefUtils

@Composable
fun WriteScreen(nfc: NfcController) {
  var mode by remember { mutableStateOf("TEXT") }
  var input by remember { mutableStateOf("") }
  var info by remember { mutableStateOf<String?>(null) }
  val lastTag by nfc.lastTag.collectAsState()
  var armed by remember { mutableStateOf(false) }

  Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Écriture NFC", style = MaterialTheme.typography.headlineSmall)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = mode=="TEXT", onClick = { mode="TEXT" }, label = { Text("Texte") })
      FilterChip(selected = mode=="URI",  onClick = { mode="URI"  }, label = { Text("URL") })
    }

    OutlinedTextField(value = input, onValueChange = { input = it },
      label = { Text(if (mode=="TEXT") "Texte à écrire" else "URL à écrire") },
      modifier = Modifier.fillMaxWidth())

    Text("Approchez un tag pour écrire. L’écriture remplace le contenu NDEF.")
    Button(onClick = { armed = true; info = "Armez: scannez un tag pour écrire." }) {
      Text("Armer l’écriture")
    }

    info?.let { Text(it) }
  }

  LaunchedEffect(lastTag, armed, mode, input) {
    val tag = lastTag ?: return@LaunchedEffect
    if (!armed) return@LaunchedEffect
    val record = if (mode=="TEXT") NdefUtils.textRecord(input) else NdefUtils.uriRecord(input)
    val err = nfc.writeNdef(tag, NdefUtils.NdefBuild(listOf(record)))
    info = err ?: "Écriture réussie."
    armed = false
  }
}
