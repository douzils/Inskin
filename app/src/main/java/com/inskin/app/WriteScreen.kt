package com.inskin.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun WriteScreen(vm: NfcViewModel = viewModel()) {
    // évite rememberSnackbarHostState (non résolu chez toi)
    val snack = remember { SnackbarHostState() }
    var text by rememberSaveable { mutableStateOf("") }
    var url  by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Écrire du texte NDEF")
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Texte") }
            )
            Button(onClick = {
                scope.launch {
                    val msg = vm.writeTextNdef(text)
                        .fold({ "Texte écrit sur le tag ✅" }, { "Erreur: ${it.message ?: "inconnue"}" })
                    snack.showSnackbar(msg)
                }
            }) { Text("Écrire le texte") }

            Spacer(Modifier.height(8.dp))

            Text("Écrire une URL NDEF")
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                label = { Text("https://…") }
            )
            Button(onClick = {
                scope.launch {
                    val msg = vm.writeUrlNdef(url)
                        .fold({ "URL écrite sur le tag ✅" }, { "Erreur: ${it.message ?: "inconnue"}" })
                    snack.showSnackbar(msg)
                }
            }) { Text("Écrire l’URL") }
        }
    }
}

/* Pas de @Preview ici pour éviter l’extension d’une classe finale */
