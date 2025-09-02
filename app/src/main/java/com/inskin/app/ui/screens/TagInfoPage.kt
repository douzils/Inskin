// TagInfoPage.kt
@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER")

package com.inskin.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.inskin.app.ClassicBlockInfo
import com.inskin.app.ClassicSectorInfo
import com.inskin.app.TagDetails
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
internal fun TagInfoPage(
    title: String,
    uid: String,
    total: Int,
    locked: Boolean,
    details: TagDetails?,
    canAskUnlock: Boolean = false,
    onAskUnlock: () -> Unit = {},
    onRequestGoUp: () -> Unit
) {
    val scroll = rememberScrollState()

    // ---------- Identification ----------
    val ident: List<Pair<String, String>> = remember(details, uid) {
        buildList {
            addKV("UID", details?.uidHex ?: uid)
            addKV("Longueur UID", details?.uidHex?.length?.let { "${it / 2} octets" })
            addKV("Technologies", details?.techList?.joinToString())
            addKV("Fabricant", details?.manufacturer)
            addKV("ATQA", details?.atqaHex)
            addKV("SAK", details?.sakHex)
            addKV("ATS / Historical", details?.atsHex ?: details?.historicalBytesHex)
            addKV("Hi-layer response", details?.hiLayerResponseHex)
            addKV("RF config", details?.rfConfig)
            addKV("Batch info", details?.batchInfo)
        }
    }

    // ---------- Système & mémoire ----------
    val system: List<Pair<String, String>> = remember(details, title, total) {
        buildList {
            addKV("Type de puce", details?.chipType ?: title)
            addKV("Mémoire totale (brut)", details?.totalMemoryBytes?.let { "$it B" } ?: "$total B")
            addKV("Layout", details?.memoryLayout)
            addKV("Version (GET_VERSION)", details?.versionHex)
            addKV("Capacité NDEF", details?.ndefCapacity?.takeIf { it > 0 }?.let { "$it B" })
            addKV("Taille utilisée NDEF", details?.usedBytes?.takeIf { it >= 0 }?.let { "$it B" })
        }
    }

    // ---------- Sécurité ----------
    val security: List<Pair<String, String>> = remember(details, locked) {
        buildList {
            details?.isNdefWritable?.let { add("NDEF inscriptible" to it.toYesNo()) }
            details?.canMakeReadOnly?.let { add("Peut devenir Read-Only" to it.toYesNo()) }
            add("Verrou global" to if (locked) "oui" else "non")
            addKV("Lock bits", details?.lockBitsHex)
            addKV("OTP bytes", details?.otpBytesHex)
            addKV("Compteurs (READ_CNT)", details?.countersHex)
            addKV("Signature ECC (READ_SIG)", details?.eccSignatureHex)
            details?.tearFlag?.let { add("Tear flag" to it.toYesNo()) }
        }
    }

    val ndefRecs = remember(details) { details?.ndefRecords.orEmpty() }

    // ---------- MIFARE Classic ----------
    val classicStats: List<Pair<String, String>> = remember(details) {
        buildList {
            details?.classicSectors?.let { secs -> addKV("Secteurs Classic", secs.size.toString()) }
            details?.classicReadableSectors?.let { addKV("Secteurs lisibles", it.toString()) }
            details?.classicDiscoveredKeys?.let { addKV("Clés découvertes", it.toString()) }
        }
    }

    // ---------- Données brutes ----------
    val rawRows: List<Pair<String, String>> = remember(details) {
        buildList {
            details?.rawReadableBytes?.takeIf { it > 0 }?.let {
                add("Octets lisibles (best-effort)" to it.toString())
            }
        }
    }
    val rawDump = details?.rawDumpFirstBytesHex
    val formattedDump = remember(rawDump) { rawDump?.takeIf { it.isNotBlank() }?.chunked(2)?.joinToString(" ") }

    // ---------- Avancé ----------
    val appsList = remember(details) { details?.applications.orEmpty() }
    val filesList = remember(details) { details?.files.orEmpty() }
    val advancedRows: List<Pair<String, String>> = remember(appsList, filesList) {
        buildList {
            if (appsList.isNotEmpty()) add("Applications (DESFire…)" to appsList.size.toString())
            if (filesList.isNotEmpty()) add("Fichiers" to filesList.size.toString())
        }
    }

    // ---------- états des pages ----------
    var showDumpPage by remember { mutableStateOf(false) }
    var showDecodedPage by remember { mutableStateOf(false) }
    var showClassicPage by remember { mutableStateOf(false) }

    // ---- CONTENU SCROLLABLE ----
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scroll)
    ) {
        Text("Informations détaillées", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        if (ident.isNotEmpty()) Section("Identification") { Rows(ident) }
        if (system.isNotEmpty()) Section("Système & Mémoire") { Rows(system) }

        if (security.isNotEmpty() || canAskUnlock) {
            Section("Sécurité") {
                Rows(security)
                if (canAskUnlock) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onAskUnlock) { Text("Saisir / mémoriser le mot de passe") }
                }
            }
        }

        if (ndefRecs.isNotEmpty()) {
            Section("NDEF") {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        ndefRecs.forEachIndexed { i, r ->
                            Text("[$i] ${r.type} :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Mono(r.value)
                            if (i < ndefRecs.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDecodedPage = true }) {
                        Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Ouvrir NDEF")
                    }
                }
            }
        }

        if (classicStats.isNotEmpty()) {
            Section("MIFARE Classic / Accès") {
                Rows(classicStats)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showClassicPage = true }) {
                        Icon(Icons.Filled.GridView, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Voir secteurs")
                    }
                }
            }
        }

        if (rawRows.isNotEmpty() || formattedDump != null) {
            Section("Données brutes") {
                Rows(rawRows)
                formattedDump?.let {
                    KV("Dump (début)", "")
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) { Mono(it) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDumpPage = true }) {
                        Icon(Icons.Filled.Hexagon, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Ouvrir dump complet")
                    }
                }
            }
        }

        if (advancedRows.isNotEmpty() || appsList.isNotEmpty() || filesList.isNotEmpty()) {
            Section("Avancé") {
                Rows(advancedRows)
                if (appsList.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Applications :", fontWeight = FontWeight.SemiBold)
                    Bulleted(appsList)
                }
                if (filesList.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Fichiers :", fontWeight = FontWeight.SemiBold)
                    Bulleted(filesList)
                }
            }
        }
    }

    // ---- DIALOGS HORS SCROLL ----
    if (showDumpPage) {
        FullscreenSheet(title = "Dump brut", onClose = { showDumpPage = false }) {
            RawDumpView(details)
        }
    }
    if (showDecodedPage) {
        FullscreenSheet(title = "NDEF décodé", onClose = { showDecodedPage = false }) {
            NdefRecordsView(ndefRecs)
        }
    }
    if (showClassicPage) {
        FullscreenSheet(title = "Secteurs MIFARE Classic", onClose = { showClassicPage = false }) {
            ClassicSectorsView(details?.classicSectors.orEmpty())
        }
    }
}

/* =============================== VUES PLEIN ÉCRAN =============================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenSheet(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    }
                )
            }
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { content() }
        }
    }
}

@Composable
private fun RawDumpView(details: TagDetails?) {
    val clip = LocalClipboardManager.current
    val full = remember(details) {
        val head = details?.rawDumpFirstBytesHex.orEmpty()
        val classic = details?.classicSectors.orEmpty()
            .flatMap { it.blocks }
            .mapNotNull { it.dataHex }
            .joinToString(separator = "") { it }
        (head + classic).ifBlank { "(aucune donnée brute disponible)" }
    }
    val pretty = remember(full) { full.chunked(2).chunked(16).joinToString("\n") { it.joinToString(" ") } }

    Column(Modifier.fillMaxSize()) {
        Text("Hexa (groupé par 16 octets)", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        ElevatedCard(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Mono(pretty)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { clip.setText(AnnotatedString(pretty)) }) { Text("Copier") }
        }
    }
}

@Composable
private fun NdefRecordsView(ndefRecs: List<com.inskin.app.NdefRecordInfo>) {
    if (ndefRecs.isEmpty()) {
        Text("Aucun enregistrement NDEF.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(ndefRecs) { r ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(r.type, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Mono(r.value)
                }
            }
        }
    }
}

@Composable
private fun ClassicSectorsView(sectors: List<ClassicSectorInfo>) {
    if (sectors.isEmpty()) {
        Text("Aucun secteur Classic décodé.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(sectors) { s ->
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val keyInfo = buildString {
                        append("Secteur ${s.sector}")
                        s.usedKeyType?.let { append(" • clé $it") }
                        s.usedKeyHex?.let { append(" $it") }
                    }
                    Text(keyInfo, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    s.blocks.forEach { b -> ClassicBlockRow(b) }
                }
            }
        }
    }
}

@Composable
private fun ClassicBlockRow(b: ClassicBlockInfo) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("Bloc ${b.indexInSector}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        val data = b.dataHex?.chunked(2)?.joinToString(" ") ?: "—"
        Mono(data)
    }
}

/* =============================== Helpers UI =============================== */

private fun MutableList<Pair<String, String>>.addKV(k: String, v: String?) {
    if (!v.isNullOrBlank()) add(k to v)
}

@Composable
private fun Section(
    title: String,
    initiallyOpen: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { open = !open }) {
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp)) { content() }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Rows(rows: List<Pair<String, String>>) {
    rows.forEach { (k, v) -> KV(k, v) }
}

@Composable
private fun KV(k: String, v: String) {
    val clip = LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$k :", fontSize = 16.sp)
        Text(v, fontSize = 16.sp, modifier = Modifier.clickable { clip.setText(AnnotatedString(v)) })
    }
}

@Composable
private fun Mono(text: String) {
    val clip = LocalClipboardManager.current
    Text(
        text,
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { clip.setText(AnnotatedString(text)) }
    )
}

@Composable
private fun Bulleted(lines: List<String>) {
    Column(Modifier.fillMaxWidth()) {
        lines.forEach { line: String -> Text("• $line", fontSize = 14.sp) }
    }
}

private fun Boolean.toYesNo() = if (this) "oui" else "non"
