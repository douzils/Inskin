@file:OptIn(ExperimentalMaterial3Api::class)

package com.inskin.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.runtime.collectAsState
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
    val snackbarHostState = remember { SnackbarHostState() }
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

    data class Action(val type: WriteItemType, val icon: ImageVector, val label: Int)
    val actions = listOf(
        Action(WriteItemType.TEXT, Icons.Filled.Article, R.string.action_text),
        Action(WriteItemType.URL, Icons.Filled.Link, R.string.action_url),
        Action(WriteItemType.WEB_SEARCH, Icons.Filled.Search, R.string.action_web_search),
        Action(WriteItemType.SOCIAL, Icons.Filled.Share, R.string.action_social),
        Action(WriteItemType.VIDEO, Icons.Filled.OndemandVideo, R.string.action_video),
        Action(WriteItemType.FILE, Icons.Filled.InsertDriveFile, R.string.action_file),
        Action(WriteItemType.APP, Icons.Filled.Apps, R.string.action_app),
        Action(WriteItemType.MAIL, Icons.Filled.Email, R.string.action_mail),
        Action(WriteItemType.CONTACT, Icons.Filled.Person, R.string.action_contact),
        Action(WriteItemType.PHONE, Icons.Filled.Phone, R.string.action_phone),
        Action(WriteItemType.SMS, Icons.Filled.Sms, R.string.action_sms),
        Action(WriteItemType.LOCATION, Icons.Filled.Place, R.string.action_location),
        Action(WriteItemType.CUSTOM_LOCATION, Icons.Filled.MyLocation, R.string.action_custom_location),
        Action(WriteItemType.ADDRESS, Icons.Filled.Map, R.string.action_address),
        Action(WriteItemType.DESTINATION, Icons.Filled.Navigation, R.string.action_destination),
        Action(WriteItemType.NEARBY_SEARCH, Icons.Filled.TravelExplore, R.string.action_nearby_search),
        Action(WriteItemType.STREET_VIEW, Icons.Filled.Streetview, R.string.action_street_view),
        Action(WriteItemType.EMERGENCY, Icons.Filled.Warning, R.string.action_emergency),
        Action(WriteItemType.CRYPTO, Icons.Filled.CurrencyBitcoin, R.string.action_crypto),
        Action(WriteItemType.BLUETOOTH, Icons.Filled.Bluetooth, R.string.action_bluetooth),
        Action(WriteItemType.WIFI, Icons.Filled.Wifi, R.string.action_wifi),
        Action(WriteItemType.CUSTOM_DATA, Icons.Filled.Code, R.string.action_custom_data),
        Action(WriteItemType.SETTINGS, Icons.Filled.Settings, R.string.action_settings),
        Action(WriteItemType.CONDITION, Icons.Filled.Rule, R.string.action_condition)
    )

    if (showTypeSheet) {
        ModalBottomSheet(onDismissRequest = { showTypeSheet = false }) {
            LazyColumn {
                items(actions) { action ->
                    ListItem(
                        leadingContent = { Icon(action.icon, contentDescription = null) },
                        headlineContent = { Text(stringResource(action.label)) },
                        modifier = Modifier.clickable {
                            showTypeSheet = false
                            formType = action.type
                        }
                    )
                }
            }
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
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(items) { index, item ->
                    ListItem(
                        leadingContent = {
                            val icon = when (item.type) {
                                WriteItemType.TEXT -> Icons.Filled.Article
                                WriteItemType.URL -> Icons.Filled.Link
                                WriteItemType.WEB_SEARCH -> Icons.Filled.Search
                                WriteItemType.SOCIAL -> Icons.Filled.Share
                                WriteItemType.VIDEO -> Icons.Filled.OndemandVideo
                                WriteItemType.FILE -> Icons.Filled.InsertDriveFile
                                WriteItemType.APP -> Icons.Filled.Apps
                                WriteItemType.MAIL -> Icons.Filled.Email
                                WriteItemType.CONTACT -> Icons.Filled.Person
                                WriteItemType.PHONE -> Icons.Filled.Phone
                                WriteItemType.SMS -> Icons.Filled.Sms
                                WriteItemType.LOCATION -> Icons.Filled.Place
                                WriteItemType.CUSTOM_LOCATION -> Icons.Filled.MyLocation
                                WriteItemType.ADDRESS -> Icons.Filled.Map
                                WriteItemType.DESTINATION -> Icons.Filled.Navigation
                                WriteItemType.NEARBY_SEARCH -> Icons.Filled.TravelExplore
                                WriteItemType.STREET_VIEW -> Icons.Filled.Streetview
                                WriteItemType.EMERGENCY -> Icons.Filled.Warning
                                WriteItemType.CRYPTO -> Icons.Filled.CurrencyBitcoin
                                WriteItemType.BLUETOOTH -> Icons.Filled.Bluetooth
                                WriteItemType.WIFI -> Icons.Filled.Wifi
                                WriteItemType.CUSTOM_DATA -> Icons.Filled.Code
                                WriteItemType.SETTINGS -> Icons.Filled.Settings
                                WriteItemType.CONDITION -> Icons.Filled.Rule
                            }
                            Icon(icon, contentDescription = null)
                        },
                        headlineContent = {
                            when (item) {
                                is WriteItem.Text -> Text(item.text)
                                is WriteItem.Url -> Text(item.url)
                                is WriteItem.Phone -> Text(item.number)
                                is WriteItem.Sms -> Text("${item.number} ${item.body.orEmpty()}")
                                is WriteItem.Mail -> Text(item.to)
                                is WriteItem.Wifi -> Text(item.ssid)
                                is WriteItem.Bluetooth -> Text(item.mac)
                                is WriteItem.Contact -> Text(item.name)
                                is WriteItem.Location -> Text("${item.lat},${item.lon}")
                                is WriteItem.Crypto -> Text(item.address)
                                is WriteItem.KeyValue -> Text("${item.key}=${item.value}")
                                is WriteItem.UriItem -> Text(item.uri)
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
                        val msg = result.fold({ context.getString(R.string.write_success) }, { it.message ?: "error" })
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
