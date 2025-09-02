@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inskin.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.inskin.app.NfcViewModel

@Composable
fun TagListScreen(
    vm: NfcViewModel,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sorted = vm.history.sortedByDescending { it.savedAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(sorted, key = { it.uidHex }) { t ->
                TagRow(uid = t.uidHex, time = t.savedAt) { onSelect(t.uidHex) }
            }
        }
    }
}

@Composable
private fun TagRow(
    uid: String,
    time: Long,
    onSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(uid) },
        supportingContent = { Text(time.toString()) },
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onSelect)
    )
}
