package com.inskin.app.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inskin.app.nfc.NfcController

@Composable
fun ReadScreen(nfc: NfcController) {
  val status by nfc.status.collectAsState()
  val last by nfc.lastRead.collectAsState()

  Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Lecture NFC", style = MaterialTheme.typography.headlineSmall)
    AssistChip(onClick = {}, label = { Text(if (status.readerEnabled) "Lecture continue active" else "Lecture inactive") })
    if (!status.nfcAvailable) Text("NFC non disponible sur cet appareil.")

    if (last == null) {
      Text("Approchez un badge ou implant pour lire…")
    } else {
      Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("UID: ${last!!.uid}")
          Text("Tech: ${last!!.tech.joinToString()}")
          last!!.records.forEachIndexed { i, r ->
            Text("[$i] ${r.type.uppercase()}: ${r.value}")
          }
        }
      }
    }
  }
}
