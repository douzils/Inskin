// TagInfoPage.kt
@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER")

package com.inskin.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inskin.app.TagDetails
import androidx.compose.foundation.lazy.rememberLazyListState


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
    // Réduit le jank: état scroll mémorisé
    val listState = rememberLazyListState()

    val ident = remember(details, uid) {
        buildList {
            addKV("UID", details?.uidHex ?: uid)
            addKV("Technologies", details?.techList?.joinToString())
            addKV("Fabricant", details?.manufacturer)
            addKV("ATQA", details?.atqaHex)
            addKV("SAK", details?.sakHex)
            addKV("Batch info", details?.batchInfo)
        }
    }
    val radio = remember(details) {
        buildList {
            addKV("ATS / Historical", details?.atsHex ?: details?.historicalBytesHex)
            addKV("Hi-layer response", details?.hiLayerResponseHex)
            addKV("RF config", details?.rfConfig)
        }
    }
    val system = remember(details, title, total) {
        buildList {
            addKV("Type de puce", details?.chipType ?: title)
            addKV("Mémoire totale (brut)", details?.totalMemoryBytes?.let { "$it B" } ?: "$total B")
            addKV("Layout", details?.memoryLayout)
            addKV("Version (GET_VERSION)", details?.versionHex)
        }
    }
    val security = remember(details, locked) {
        buildList {
            details?.isNdefWritable?.let { add("NDEF writable" to it.toYesNo()) }
            details?.canMakeReadOnly?.let { add("Peut devenir Read-Only" to it.toYesNo()) }
            add("Verrou global" to if (locked) "oui" else "non")
            addKV("Lock bits", details?.lockBitsHex)
            addKV("OTP bytes", details?.otpBytesHex)
            addKV("Compteurs (READ_CNT)", details?.countersHex)
            addKV("Signature ECC (READ_SIG)", details?.eccSignatureHex)
            details?.tearFlag?.let { add("Tear flag" to it.toYesNo()) }
        }
    }
    val ndefRows = remember(details) {
        buildList {
            details?.ndefCapacity?.takeIf { it > 0 }?.let { add("Capacité NDEF" to "$it B") }
            details?.usedBytes?.takeIf { it >= 0 }?.let { add("Taille utilisée" to "$it B") }
        }
    }
    val ndefRecs = remember(details) { details?.ndefRecords.orEmpty() }

    val rawRows = remember(details) {
        buildList {
            details?.rawReadableBytes?.takeIf { it > 0 }?.let { add("Octets lisibles (best-effort)" to it.toString()) }
        }
    }
    val rawDump = details?.rawDumpFirstBytesHex
    val formattedDump = remember(rawDump) { rawDump?.takeIf { it.isNotBlank() }?.chunked(2)?.joinToString(" ") }

    val appsList = remember(details) { details?.applications.orEmpty() }
    val filesList = remember(details) { details?.files.orEmpty() }
    val advancedRows = remember(appsList, filesList) {
        buildList {
            if (appsList.isNotEmpty()) add("Applications (DESFire…)" to appsList.size.toString())
            if (filesList.isNotEmpty()) add("Fichiers" to filesList.size.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        HeaderRow()

        if (ident.isNotEmpty()) Section("Identification") { Rows(ident) }
        if (radio.isNotEmpty()) Section("Radio / Protocole") { Rows(radio) }
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

        if (ndefRows.isNotEmpty() || ndefRecs.isNotEmpty()) {
            Section("NDEF") {
                Rows(ndefRows)
                if (ndefRecs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            ndefRecs.forEachIndexed { i, r ->
                                Text("[$i] ${r.type} :", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Mono(r.value)
                                if (i < ndefRecs.lastIndex) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        if (rawRows.isNotEmpty() || formattedDump != null) {
            Section("Données brutes") {
                Rows(rawRows)
                formattedDump?.let {
                    KV("Dump (début)", "")
                    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Mono(it) } }
                }
            }
        }

        if (advancedRows.isNotEmpty()) {
            Section("Avancé") {
                Rows(advancedRows)
                if (appsList.isNotEmpty()) Bulleted(appsList)
                if (filesList.isNotEmpty()) Bulleted(filesList)
            }
        }
    }
}

private fun MutableList<Pair<String, String>>.addKV(k: String, v: String?) {
    if (!v.isNullOrBlank()) add(k to v)
}

@Composable
private fun HeaderRow() {
    Text("Informations détaillées", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
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
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
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
        Text(
            v,
            fontSize = 16.sp,
            modifier = Modifier.clickable { clip.setText(AnnotatedString(v)) }
        )
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
        lines.forEach { line -> Text("• $line", fontSize = 14.sp) }
    }
}

private fun Boolean.toYesNo() = if (this) "oui" else "non"
