// TagHeaderPage.kt
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@file:Suppress("SpellCheckingInspection")

package com.inskin.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.inskin.app.TagDetails
import com.inskin.app.R as AppR

@Composable
fun TagHeaderPage(
    title: String,
    uid: String,
    name: String,
    used: Int,
    total: Int,
    locked: Boolean,
    typeDetail: String?,
    details: TagDetails?,
    onBack: () -> Unit,
    selectedForm: BadgeForm?,
    onPickForm: (BadgeForm) -> Unit,   // icône choisie -> sauvegarder via VM
    onRename: (String) -> Unit,        // nouveau nom -> sauvegarder via VM
    onOpenHistory: () -> Unit = {},
    onOpenWrite: () -> Unit = {},
    onRequestGoDown: () -> Unit = {}
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(name) { mutableStateOf(name) }
    var showFormDialog by remember { mutableStateOf(false) }

    val sw = LocalConfiguration.current.screenWidthDp.dp
    val sh = LocalConfiguration.current.screenHeightDp.dp
    val disc = remember(sw, sh) { (minOf(sw, sh) * 0.58f).coerceIn(128.dp, 360.dp) }
    val circleY = (-84).dp

    val writeLocked = details?.isNdefWritable == false
    val readLocked  = locked
    val fullyOpen   = !readLocked && !writeLocked
    val fullyLocked = readLocked && writeLocked

    Box(Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Retour", tint = Color.Black)
            }
            IconButton(onClick = onOpenHistory) {
                Icon(Icons.Filled.History, contentDescription = "Historique", tint = Color.Black)
            }
        }

        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            TagHeaderPageTitle(title = title, uid = uid, typeDetail = typeDetail, centered = true)
            Spacer(Modifier.height(6.dp))

            val topBoxHeight = disc * 2.05f
            Box(
                Modifier.fillMaxWidth().height(topBoxHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(disc)
                        .offset(y = disc + circleY)
                        .zIndex(8f)
                        .clip(CircleShape)
                        .background(Color(0xFF3E3E3E))
                        .combinedClickable(
                            onClick = { /* tap = rien */ },
                            onLongClick = { showFormDialog = true } // ouvrir le sélecteur d’icône
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedForm == null) {
                        Image(
                            painter = painterResource(AppR.drawable.antenna_tag),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.78f),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = selectedForm.icon,
                            contentDescription = selectedForm.label,
                            tint = selectedForm.tint,
                            modifier = Modifier.fillMaxSize(0.55f)
                        )
                    }

                    val ndefCount = details?.ndefRecords?.size ?: 0
                    if (ndefCount > 0) {
                        StackedCounter(
                            count = ndefCount,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        )
                    }
                }

                // Indicateur de verrouillage
                Box(
                    modifier = Modifier.size(disc).offset(y = disc + circleY).zIndex(9f),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier.offset(x = 10.dp, y = (-10).dp)
                            .size(36.dp).clip(CircleShape)
                            .background(if (fullyOpen) Color(0xFF2ECC71) else Color(0xCC000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (fullyOpen) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        if (!fullyOpen) {
                            val label = when {
                                fullyLocked -> "RW"
                                readLocked  -> "R"
                                writeLocked -> "W"
                                else -> ""
                            }
                            if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = (-3).dp, y = (-2).dp)
                                )
                            }
                        }
                    }
                }
            }

            // Nom + barre de stockage
            Column(
                Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .padding(horizontal = 20.dp)
            ) {
                if (!editing) {
                    Text(
                        text = name,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .combinedClickable(onClick = { editing = true })
                    )
                } else {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .widthIn(min = 200.dp, max = 320.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                onRename(draft.trim().ifBlank { name })  // sauvegarde
                                editing = false
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF34C759))
                            }
                        }
                    )
                }

                Spacer(Modifier.height(60.dp))

                val usedFromDetails = details?.usedBytes ?: used
                val (barTotalBytes, slices) = buildStorageSlices(
                    typeLabel = title,
                    typeDetail = typeDetail,
                    details = details,
                    usedNdef = usedFromDetails,
                    ndefCapFromAndroid = details?.ndefCapacity ?: 0
                )

                val userCapBytes = if (slices.any { it.id == "system" })
                    barTotalBytes - slices.first { it.id == "system" }.bytes
                else barTotalBytes
                val userUsedBytes = slices.firstOrNull { it.id == "user_used" }?.bytes ?: 0
                val usedLabel = if (usedFromDetails < 0) "—" else fmtBytes(userUsedBytes)

                Text(
                    text = "Stockage utilisé : $usedLabel / ${fmtBytes(userCapBytes)}",
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                SegmentedUsageBar(
                    totalBytes = barTotalBytes,
                    slices = slices,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 6.dp)
                )

                StorageLegendRow(
                    slices = slices,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(96.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onRequestGoDown)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Plus d’informations", fontSize = 18.sp, color = Color(0xFF2A2A2A))
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Color.Black.copy(alpha = 0.75f), modifier = Modifier.size(28.dp))
        }
    }

    if (showFormDialog) {
        FormPickerDialog(
            selected = selectedForm,
            onDismiss = { showFormDialog = false },
            onPick = { picked ->
                onPickForm(picked)  // sauvegarde
                showFormDialog = false
            }
        )
    }
}

@Composable
fun FormPickerDialog(
    selected: BadgeForm?,
    onDismiss: () -> Unit,
    onPick: (BadgeForm) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forme du tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeForm.entries.forEach { form ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .combinedClickable(onClick = { onPick(form) })
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(form.tint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(form.icon, contentDescription = null, tint = form.tint) }
                        Text(form.label, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (form == selected) Icons.Filled.Check else Icons.Filled.Circle,
                            contentDescription = null,
                            tint = if (form == selected) Color(0xFF34C759) else Color.Transparent
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
