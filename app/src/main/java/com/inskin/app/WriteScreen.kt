package com.inskin.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.launch

@Composable
fun WriteScreen(vm: NfcViewModel) {
    val snackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val tagInfo by vm.tagInfo.collectAsState()
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.write_text_hint)) })
        TextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.write_url_hint)) })
        Button(
            onClick = {
                val result = if (url.isNotBlank()) vm.writeUrlNdef(url) else vm.writeTextNdef(text)
                scope.launch {
                    val msg = result.fold({ stringResource(R.string.write_success) }, { it.message ?: "error" })
                    snackbarHostState.showSnackbar(msg)
                }
            },
            enabled = tagInfo != null
        ) { Text(stringResource(R.string.btn_write)) }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Preview
@Composable
private fun PreviewWrite() {
    val context = androidx.compose.ui.platform.LocalContext.current
    WriteScreen(vm = NfcViewModel(context.applicationContext as android.app.Application))
}
