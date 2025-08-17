package com.inskin.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.inskin.app.model.WriteItem
import com.inskin.app.model.WriteItemType
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.writeDataStore by preferencesDataStore("write_items")
private val WRITE_ITEMS_KEY = stringPreferencesKey("write_items_json")

/**
 * Main screen for creating a list of [WriteItem] and writing them to a tag.
 */
@Composable
fun WriteScreen(vm: NfcViewModel) {
    val context = LocalContext.current
    val dataStore = context.writeDataStore
    val scope = rememberCoroutineScope()
    val snackbarHostState: SnackbarHostState = rememberSnackbarHostState()
    var items by remember { mutableStateOf<List<WriteItem>>(emptyList()) }
    val json = Json

    LaunchedEffect(Unit) {
        val stored = dataStore.data.first()[WRITE_ITEMS_KEY]
        stored?.let { items = json.decodeFromString(it) }
    }
    LaunchedEffect(items) {
        dataStore.edit { it[WRITE_ITEMS_KEY] = json.encodeToString(items) }
    }

    val tagInfo by vm.tagInfo.collectAsState()
    var showTypeSheet by remember { mutableStateOf(false) }
    var formType by remember { mutableStateOf<WriteItemType?>(null) }

    if (showTypeSheet) {
        ModalBottomSheet(onDismissRequest = { showTypeSheet = false }) {
            ListItem(
                leadingContent = { Icon(Icons.Filled.Article, contentDescription = null) },
                headlineText = { Text(stringResource(R.string.action_text)) },
                modifier = Modifier.clickable {
                    showTypeSheet = false
                    formType = WriteItemType.TEXT
                }
            )
            ListItem(
                leadingContent = { Icon(Icons.Filled.Link, contentDescription = null) },
                headlineText = { Text(stringResource(R.string.action_url)) },
                modifier = Modifier.clickable {
                    showTypeSheet = false
                    formType = WriteItemType.URL
                }
            )
        }
    }

    if (formType != null) {
        ModalBottomSheet(onDismissRequest = { formType = null }) {
            WriteItemForm(formType!!) { item ->
                items = items + item
                formType = null
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showTypeSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.write_add_item))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f, true)) {
                itemsIndexed(items) { index, item ->
                    ListItem(
                        leadingContent = {
                            when (item.type) {
                                WriteItemType.TEXT -> Icon(Icons.Filled.Article, contentDescription = null)
                                WriteItemType.URL -> Icon(Icons.Filled.Link, contentDescription = null)
                                else -> Icon(Icons.Filled.Article, contentDescription = null)
                            }
                        },
                        headlineText = {
                            when (item) {
                                is WriteItem.Text -> Text(item.text)
                                is WriteItem.Url -> Text(item.url)
                                else -> Text(item.type.name)
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                items = items.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                            }
                        }
                    )
                }
            }
            Button(
                onClick = {
                    val result = vm.writeItems(items)
                    scope.launch {
                        val msg = result.fold({ stringResource(R.string.write_success) }, { it.message ?: "error" })
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                enabled = tagInfo != null && items.isNotEmpty(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.btn_write)) }
        }
    }
}

@Preview
@Composable
private fun PreviewWrite() {
    val context = LocalContext.current
    WriteScreen(vm = NfcViewModel(context.applicationContext as android.app.Application))
}
