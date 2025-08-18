package com.inskin.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun WriteScreen(vm: NfcViewModel) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val tagInfo by vm.tagInfo.collectAsState()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.padding(p).padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Texte à écrire") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val res = vm.writeTextNdef(text)
                    scope.launch {
                        snackbar.showSnackbar(res.fold({ "Écriture OK" }, { it.message ?: "Erreur" }))
                    }
                },
                enabled = text.isNotBlank() && tagInfo != null
            ) { Text("Write to tag") }
        }
    }
}

@Preview
@Composable
private fun PreviewWrite() {
    val ctx = LocalContext.current
    WriteScreen(vm = NfcViewModel(ctx.applicationContext as android.app.Application))
}
