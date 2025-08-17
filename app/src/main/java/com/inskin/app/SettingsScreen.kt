package com.inskin.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var dark by remember { mutableStateOf(true) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.tab_settings))
        Switch(checked = dark, onCheckedChange = { dark = it })
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:inskin@example.com"))
            context.startActivity(intent)
        }) { Text(stringResource(R.string.send_feedback)) }
    }
}

@Preview
@Composable
private fun PreviewSettings() {
    SettingsScreen()
}
