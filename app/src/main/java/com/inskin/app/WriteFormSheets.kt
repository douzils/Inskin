package com.inskin.app

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.inskin.app.model.WriteItem
import com.inskin.app.model.WriteItemType

/**
 * Content of the second bottom sheet used to enter a WriteItem payload.
 */
@Composable
fun WriteItemForm(type: WriteItemType, onSubmit: (WriteItem) -> Unit) {
    when (type) {
        WriteItemType.TEXT -> TextItemForm { onSubmit(it) }
        WriteItemType.URL -> UrlItemForm { onSubmit(it) }
        else -> Column(Modifier.padding(16.dp)) { Text(stringResource(R.string.not_implemented)) }
    }
}

@Composable
private fun TextItemForm(onSubmit: (WriteItem.Text) -> Unit) {
    var text by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.write_text_hint)) },
            singleLine = true,
            isError = text.isBlank()
        )
        Button(
            onClick = { if (text.isNotBlank()) onSubmit(WriteItem.Text(text)) },
            enabled = text.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Composable
private fun UrlItemForm(onSubmit: (WriteItem.Url) -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                error = it.isNotBlank() && !Patterns.WEB_URL.matcher(it).matches()
            },
            label = { Text(stringResource(R.string.write_url_hint)) },
            singleLine = true,
            isError = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Button(
            onClick = { if (!error && url.isNotBlank()) onSubmit(WriteItem.Url(url)) },
            enabled = url.isNotBlank() && !error,
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.action_add)) }
    }
}

@Preview
@Composable
private fun PreviewTextForm() {
    WriteItemForm(WriteItemType.TEXT) { }
}

@Preview
@Composable
private fun PreviewUrlForm() {
    WriteItemForm(WriteItemType.URL) { }
}
