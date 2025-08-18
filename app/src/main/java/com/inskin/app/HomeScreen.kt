package com.inskin.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onOpenRead: () -> Unit = {},
    onOpenWrite: () -> Unit = {},
    onOpenHistory: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Inskin", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenRead) { Text("Lire un tag") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenWrite) { Text("Écrire un tag") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenHistory) { Text("Historique") }
    }
}
