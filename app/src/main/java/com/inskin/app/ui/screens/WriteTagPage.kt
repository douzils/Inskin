@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@file:Suppress("SpellCheckingInspection")

package com.inskin.app.ui.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import com.inskin.app.R as AppR
import androidx.navigation.NavController
import com.inskin.app.NfcViewModel
import androidx.compose.ui.res.painterResource

/* ===== Modèles ===== */
enum class EntryKind {
    TEXT,
    URL,
    FILE_LINK,
    APP,
    CONTACT,
    EMAIL,
    SMS,
    CALL,
    LOCATION,
    ADDRESS,
    CRYPTO_WALLET,
    BLUETOOTH,
    WIFI,
    HOTSPOT,
    WAKE_ON_LAN,
    UDP,
    HTTP_REQUEST,
    HTTP_AUTH,
    VOLUME,
    ALARM,
    EVENT,
    MEDIA_CONTROL,
    RECORD,
    VIDEO,
    PHOTO,
    BRIGHTNESS,
    ROTATION,
    NOTIFICATION_LED,
    WALLPAPER,
    UNINSTALL_APP,
    KILL_APP,
    HOME_SCREEN,
    LOCK,
    UNLOCK,
    VOICE_ASSISTANT,
    CREATE_FOLDER,
    CREATE_FILE,
    MOVE_FOLDER,
    MOVE_FILE,
    DELETE_FILE,
    DELETE_FOLDER,
    TTS_FILE,
    COMPRESS,
    DECOMPRESS,
    TORCH,
    VIBRATE,
    SCREENSHOT,
    CONDITION
}

data class WriteEntry(
    val id: Long = Random.nextLong(),
    val kind: EntryKind,
    val value: String
)

/* ===== Page d’écriture ===== */
@Composable
fun WriteTagPage(
    tagName: String,
    form: BadgeForm?,
    locked: Boolean,
    onBack: () -> Unit,
    initialEntries: List<WriteEntry> = emptyList(),
    onStartWrite: (List<WriteEntry>, (Boolean) -> Unit) -> Unit,
    onDefinePassword: () -> Unit = {},
    onRemovePassword: () -> Unit = {}
) {
    val snackbarHost = remember { SnackbarHostState() }
    var writeResult by remember { mutableStateOf<Boolean?>(null) }

    // Affiche un message après l’écriture
    LaunchedEffect(writeResult) {
        writeResult?.let { ok ->
            snackbarHost.showSnackbar(if (ok) "Écriture réussie" else "Échec de l’écriture")
            writeResult = null
        }
    }

    // Liste réactive
    val entries = remember(initialEntries) {
        mutableStateListOf<WriteEntry>().apply { addAll(initialEntries) }
    }

    // Drag & drop
    var dragDy by remember { mutableFloatStateOf(0f) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }

    // UI state
    var showAdd by remember { mutableStateOf(false) }
    var showWaiting by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<WriteEntry?>(null) }

    // Décalage sous le header
    val topBarHeight = 56.dp
    val contentTopPadding = topBarHeight + 8.dp

    Box(Modifier.fillMaxSize()) {

        /* Top bar */
        Row(
            Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Retour",
                    tint = Color.Black
                )
            }

            Box(Modifier.weight(1f)) {
                Text(
                    text = tagName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .align(Alignment.CenterEnd)
                )
            }

            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF202020)),
                contentAlignment = Alignment.Center
            ) {
                if (form == null) {
                    Icon(
                        painter = painterResource(AppR.drawable.antenna_tag),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize(0.70f)
                    )
                } else {
                    Icon(
                        imageVector = form.icon,
                        contentDescription = form.label,
                        tint = form.tint,
                        modifier = Modifier.fillMaxSize(0.62f)
                    )
                }
            }
        }

        /* Message vide */
        if (entries.isEmpty()) {
            EmptyHint(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = contentTopPadding, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            )
        }

        /* Liste */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopPadding, bottom = 140.dp)
        ) {
            items(entries, key = { it.id }) { item ->
                val isDragging = draggingId == item.id
                EntryRow(
                    entry = item,
                    modifier = Modifier
                        .animateItem()
                        .shadow(if (isDragging) 6.dp else 0.dp, RoundedCornerShape(10.dp))
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp)
                        .onGloballyPositioned { coords ->
                            itemHeights[item.id] = coords.size.height
                        },
                    onDragStart = { draggingId = item.id; dragDy = 0f },
                    onDrag = { _, drag ->
                        dragDy += drag.y
                        val from = entries.indexOfFirst { it.id == item.id }
                        if (from == -1) return@EntryRow
                        val half = (itemHeights[item.id] ?: 0) / 2f

                        if (from < entries.lastIndex && dragDy > half) {
                            val moved = entries.removeAt(from)
                            entries.add(from + 1, moved)
                            dragDy -= (itemHeights[moved.id] ?: 0)
                        } else if (from > 0 && dragDy < -half) {
                            val moved = entries.removeAt(from)
                            entries.add(from - 1, moved)
                            dragDy += (itemHeights[moved.id] ?: 0)
                        }
                    },
                    onDragEnd = { draggingId = null; dragDy = 0f },
                    onRequestEdit = { editorTarget = it },
                    onRemove = {
                        val i = entries.indexOfFirst { e -> e.id == it.id }
                        if (i >= 0) entries.removeAt(i)
                    }
                )
            }
        }

        /* Boutons bas */
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            CircleButton(size = 68.dp, bg = Color(0xFF2B2323), onClick = { showAdd = !showAdd }) {
                Icon(Icons.Filled.Add, null, tint = Color(0xFFD7FFE6))
            }
            Spacer(Modifier.width(12.dp))
            CircleButton(
                size = 56.dp,
                bg = Color(0xFF2B2323),
                onClick = { if (locked) onRemovePassword() else onDefinePassword() }
            ) {
                Icon(if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = Color(0xFFD7FFE6))
            }
        }

        CircleButton(
            size = 180.dp,
            bg = Color(0xFF2B2323),
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-84).dp),
            onClick = {
                showWaiting = true
                onStartWrite(entries) { ok ->
                    showWaiting = false
                    writeResult = ok
                }
            }
        ) {
            Icon(Icons.Filled.ArrowUpward, null, modifier = Modifier.size(72.dp), tint = Color(0xFFD9FFE8))
        }

        /* Onglet d’ajout */
        AnimatedVisibility(
            visible = showAdd,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                AddChoiceRow(Icons.Filled.Link, "URL") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.URL, value = "")) }
                AddChoiceRow(Icons.Filled.TextFields, "Texte") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.TEXT, value = "")) }
                AddChoiceRow(Icons.Filled.Description, "Lien vers Fichier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.FILE_LINK, value = "")) }
                AddChoiceRow(Icons.Filled.Apps, "Application") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.APP, value = "")) }
                AddChoiceRow(Icons.Filled.Person, "Enregistre un contact") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.CONTACT, value = "")) }
                AddChoiceRow(Icons.Filled.Email, "Enregistre un Mail") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.EMAIL, value = "")) }
                AddChoiceRow(Icons.Filled.Message, "Envoie un SMS") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.SMS, value = "")) }
                AddChoiceRow(Icons.Filled.Phone, "Appelle") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.CALL, value = "")) }
                AddChoiceRow(Icons.Filled.Place, "Localisation") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.LOCATION, value = "")) }
                AddChoiceRow(Icons.Filled.Home, "Lien vers adresse") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.ADDRESS, value = "")) }
                AddChoiceRow(Icons.Filled.AccountBalance, "Adresse Portefeuile (Crypto)") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.CRYPTO_WALLET, value = "")) }
                AddChoiceRow(Icons.Filled.Bluetooth, "Ajoute un appareil Bluetooth") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.BLUETOOTH, value = "")) }
                AddChoiceRow(Icons.Filled.Wifi, "Ajoute un Wifi") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.WIFI, value = "")) }
                AddChoiceRow(Icons.Filled.WifiTethering, "Allume un Hotspot") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.HOTSPOT, value = "")) }
                AddChoiceRow(Icons.Filled.Computer, "Wake on Lan") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.WAKE_ON_LAN, value = "")) }
                AddChoiceRow(Icons.Filled.SettingsEthernet, "UDP") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.UDP, value = "")) }
                AddChoiceRow(Icons.Filled.Http, "Requête HTTP") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.HTTP_REQUEST, value = "")) }
                AddChoiceRow(Icons.Filled.VpnKey, "Authentification HTTP") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.HTTP_AUTH, value = "")) }
                AddChoiceRow(Icons.Filled.VolumeUp, "Volume") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.VOLUME, value = "")) }
                AddChoiceRow(Icons.Filled.Alarm, "Définit une alarme") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.ALARM, value = "")) }
                AddChoiceRow(Icons.Filled.Event, "Définit un événement") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.EVENT, value = "")) }
                AddChoiceRow(Icons.Filled.PlayArrow, "Contrôle de média") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.MEDIA_CONTROL, value = "")) }
                AddChoiceRow(Icons.Filled.Mic, "Lance un enregistrement") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.RECORD, value = "")) }
                AddChoiceRow(Icons.Filled.Videocam, "Lance une vidéo") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.VIDEO, value = "")) }
                AddChoiceRow(Icons.Filled.CameraAlt, "Prend une photo") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.PHOTO, value = "")) }
                AddChoiceRow(Icons.Filled.Brightness6, "Définit la luminosité") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.BRIGHTNESS, value = "")) }
                AddChoiceRow(Icons.Filled.ScreenRotation, "Rotation") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.ROTATION, value = "")) }
                AddChoiceRow(Icons.Filled.Notifications, "Voyant de notification") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.NOTIFICATION_LED, value = "")) }
                AddChoiceRow(Icons.Filled.Image, "Définir fond d’écran/verrouillage") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.WALLPAPER, value = "")) }
                AddChoiceRow(Icons.Filled.Delete, "Désinstalle une App") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.UNINSTALL_APP, value = "")) }
                AddChoiceRow(Icons.Filled.Block, "Tue une application") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.KILL_APP, value = "")) }
                AddChoiceRow(Icons.Filled.Home, "Écran d’accueil") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.HOME_SCREEN, value = "")) }
                AddChoiceRow(Icons.Filled.Lock, "Verrouille le téléphone") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.LOCK, value = "")) }
                AddChoiceRow(Icons.Filled.LockOpen, "Déverrouille le téléphone") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.UNLOCK, value = "")) }
                AddChoiceRow(Icons.Filled.KeyboardVoice, "Assistant vocal") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.VOICE_ASSISTANT, value = "")) }
                AddChoiceRow(Icons.Filled.CreateNewFolder, "Créer un dossier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.CREATE_FOLDER, value = "")) }
                AddChoiceRow(Icons.Filled.NoteAdd, "Créer un fichier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.CREATE_FILE, value = "")) }
                AddChoiceRow(Icons.Filled.DriveFileMove, "Déplace un dossier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.MOVE_FOLDER, value = "")) }
                AddChoiceRow(Icons.Filled.DriveFileMove, "Déplace un fichier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.MOVE_FILE, value = "")) }
                AddChoiceRow(Icons.Filled.Delete, "Supprime un fichier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.DELETE_FILE, value = "")) }
                AddChoiceRow(Icons.Filled.Folder, "Supprime un dossier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.DELETE_FOLDER, value = "")) }
                AddChoiceRow(Icons.Filled.RecordVoiceOver, "Lecture TTS d’un fichier") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.TTS_FILE, value = "")) }
                AddChoiceRow(Icons.Filled.Archive, "Compresser") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.COMPRESS, value = "")) }
                AddChoiceRow(Icons.Filled.Unarchive, "Décompresser") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.DECOMPRESS, value = "")) }
                AddChoiceRow(Icons.Filled.FlashOn, "Allume/éteint la lampe torche") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.TORCH, value = "")) }
                AddChoiceRow(Icons.Filled.Vibration, "Vibrer") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.VIBRATE, value = "")) }
                AddChoiceRow(Icons.Filled.Screenshot, "Capture d’écran") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.SCREENSHOT, value = "")) }
                AddChoiceRow(Icons.Filled.Help, "Condition") { showAdd = false; entries.add(WriteEntry(kind = EntryKind.CONDITION, value = "")) }
                Spacer(Modifier.height(8.dp))
            }
        }

        /* Overlay attente */
        AnimatedVisibility(visible = showWaiting, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF444444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            repeat(3) {
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("EN ATTENTE D’UN", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF444444))
                    Text("TAG …", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF444444))
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showWaiting = false }) { Text("Annuler") }
                }
            }
        }

        /* Pop-up édition */
        editorTarget?.let { target ->
            EditEntryDialog(
                entry = target,
                onDismiss = { editorTarget = null },
                onConfirm = { updated ->
                    val i = entries.indexOfFirst { it.id == updated.id }
                    if (i >= 0) entries[i] = updated
                    editorTarget = null
                }
            )
        }

        /* Snackbar */
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun WriteTagRoute(
    navController: NavController,
    vm: NfcViewModel,
    form: BadgeForm? = null
) {
    val last = vm.lastTag.value
    WriteTagPage(
        tagName = last?.name ?: "NFC Tag",
        form = form,
        locked = last?.locked == true,
        onBack = { navController.popBackStack() },
        onStartWrite = vm::queueWrite,   // ✅ écrit tout de suite ou attend un tag
        onDefinePassword = vm::askUnlock
    )
}

@Composable
private fun EntryRow(
    entry: WriteEntry,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit,
    onDrag: (change: PointerInputChange, drag: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onRequestEdit: (WriteEntry) -> Unit,
    onRemove: (WriteEntry) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Déplacer",
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(28.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, drag ->
                            change.consume()
                            onDrag(change, drag)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd
                    )
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val icon: ImageVector = when (entry.kind) {
            EntryKind.URL -> Icons.Filled.Link
            EntryKind.TEXT -> Icons.Filled.TextFields
            EntryKind.FILE_LINK -> Icons.Filled.Description
            EntryKind.APP -> Icons.Filled.Apps
            EntryKind.CONTACT -> Icons.Filled.Person
            EntryKind.EMAIL -> Icons.Filled.Email
            EntryKind.SMS -> Icons.Filled.Message
            EntryKind.CALL -> Icons.Filled.Phone
            EntryKind.LOCATION -> Icons.Filled.Place
            EntryKind.ADDRESS -> Icons.Filled.Home
            EntryKind.CRYPTO_WALLET -> Icons.Filled.AccountBalance
            EntryKind.BLUETOOTH -> Icons.Filled.Bluetooth
            EntryKind.WIFI -> Icons.Filled.Wifi
            EntryKind.HOTSPOT -> Icons.Filled.WifiTethering
            EntryKind.WAKE_ON_LAN -> Icons.Filled.Computer
            EntryKind.UDP -> Icons.Filled.SettingsEthernet
            EntryKind.HTTP_REQUEST -> Icons.Filled.Http
            EntryKind.HTTP_AUTH -> Icons.Filled.VpnKey
            EntryKind.VOLUME -> Icons.Filled.VolumeUp
            EntryKind.ALARM -> Icons.Filled.Alarm
            EntryKind.EVENT -> Icons.Filled.Event
            EntryKind.MEDIA_CONTROL -> Icons.Filled.PlayArrow
            EntryKind.RECORD -> Icons.Filled.Mic
            EntryKind.VIDEO -> Icons.Filled.Videocam
            EntryKind.PHOTO -> Icons.Filled.CameraAlt
            EntryKind.BRIGHTNESS -> Icons.Filled.Brightness6
            EntryKind.ROTATION -> Icons.Filled.ScreenRotation
            EntryKind.NOTIFICATION_LED -> Icons.Filled.Notifications
            EntryKind.WALLPAPER -> Icons.Filled.Image
            EntryKind.UNINSTALL_APP -> Icons.Filled.Delete
            EntryKind.KILL_APP -> Icons.Filled.Block
            EntryKind.HOME_SCREEN -> Icons.Filled.Home
            EntryKind.LOCK -> Icons.Filled.Lock
            EntryKind.UNLOCK -> Icons.Filled.LockOpen
            EntryKind.VOICE_ASSISTANT -> Icons.Filled.KeyboardVoice
            EntryKind.CREATE_FOLDER -> Icons.Filled.CreateNewFolder
            EntryKind.CREATE_FILE -> Icons.Filled.NoteAdd
            EntryKind.MOVE_FOLDER, EntryKind.MOVE_FILE -> Icons.Filled.DriveFileMove
            EntryKind.DELETE_FILE -> Icons.Filled.Delete
            EntryKind.DELETE_FOLDER -> Icons.Filled.Folder
            EntryKind.TTS_FILE -> Icons.Filled.RecordVoiceOver
            EntryKind.COMPRESS -> Icons.Filled.Archive
            EntryKind.DECOMPRESS -> Icons.Filled.Unarchive
            EntryKind.TORCH -> Icons.Filled.FlashOn
            EntryKind.VIBRATE -> Icons.Filled.Vibration
            EntryKind.SCREENSHOT -> Icons.Filled.Screenshot
            EntryKind.CONDITION -> Icons.Filled.Help
        }
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)

        Spacer(Modifier.width(10.dp))

        Text(
            text = entryLabel(entry),
            fontSize = 18.sp,
            modifier = Modifier
                .weight(1f)
                .clickable { onRequestEdit(entry) }
        )

        IconButton(onClick = { onRemove(entry) }) {
            Icon(Icons.Filled.Close, contentDescription = "Supprimer", tint = Color(0xFFD44A45))
        }
    }
}

private fun entryLabel(entry: WriteEntry): String {
    val placeholder = when (entry.kind) {
        EntryKind.URL -> "URL…"
        EntryKind.TEXT -> "Texte…"
        EntryKind.FILE_LINK -> "Chemin ou URI de fichier…"
        EntryKind.APP -> "Nom de package / activité…"
        EntryKind.CONTACT -> "vCard ou nom/numéro…"
        EntryKind.EMAIL -> "email@domaine.tld; sujet; corps"
        EntryKind.SMS -> "+33123456789; message"
        EntryKind.CALL -> "+33123456789"
        EntryKind.LOCATION -> "lat,lon ou geo:URI"
        EntryKind.ADDRESS -> "Adresse postale…"
        EntryKind.CRYPTO_WALLET -> "Adresse ou URI (bitcoin:…)"
            EntryKind.BLUETOOTH -> "MAC ou nom d’appareil…"
        EntryKind.WIFI -> "SSID; sécurité; mot de passe"
        EntryKind.HOTSPOT -> "SSID; mot de passe; bande"
        EntryKind.WAKE_ON_LAN -> "MAC; broadcast; port"
        EntryKind.UDP -> "host:port; payload"
        EntryKind.HTTP_REQUEST -> "GET https://example.com"
        EntryKind.HTTP_AUTH -> "user:pass@host"
        EntryKind.VOLUME -> "0-100 ou media/sonnerie"
        EntryKind.ALARM -> "HH:mm ; libellé"
        EntryKind.EVENT -> "Titre; début; fin; lieu"
        EntryKind.MEDIA_CONTROL -> "play/pause/next/prev"
        EntryKind.RECORD -> "audio/vidéo; durée"
        EntryKind.VIDEO -> "URI vidéo ou action"
        EntryKind.PHOTO -> "caméra avant/arrière"
        EntryKind.BRIGHTNESS -> "0-100"
        EntryKind.ROTATION -> "auto/on/off"
        EntryKind.NOTIFICATION_LED -> "on/off; couleur"
        EntryKind.WALLPAPER -> "URI image"
        EntryKind.UNINSTALL_APP -> "package"
        EntryKind.KILL_APP -> "package"
        EntryKind.HOME_SCREEN -> "Aller à l’accueil"
        EntryKind.LOCK -> "Verrouiller"
        EntryKind.UNLOCK -> "Déverrouiller"
        EntryKind.VOICE_ASSISTANT -> "Assistant"
        EntryKind.CREATE_FOLDER -> "Chemin dossier"
        EntryKind.CREATE_FILE -> "Chemin fichier"
        EntryKind.MOVE_FOLDER -> "src; dst"
        EntryKind.MOVE_FILE -> "src; dst"
        EntryKind.DELETE_FILE -> "Chemin fichier"
        EntryKind.DELETE_FOLDER -> "Chemin dossier"
        EntryKind.TTS_FILE -> "Chemin fichier texte"
        EntryKind.COMPRESS -> "src; zip"
        EntryKind.DECOMPRESS -> "zip; dest"
        EntryKind.TORCH -> "on/off"
        EntryKind.VIBRATE -> "durée ms"
        EntryKind.SCREENSHOT -> "capturer maintenant"
        EntryKind.CONDITION -> "expr. logique"
    }
    return entry.value.ifBlank { placeholder }
}

@Composable
private fun EditEntryDialog(
    entry: WriteEntry,
    onDismiss: () -> Unit,
    onConfirm: (WriteEntry) -> Unit
) {
    var value by remember(entry.id) { mutableStateOf(entry.value) }

    val label = when (entry.kind) {
        EntryKind.URL -> "https://…"
        EntryKind.TEXT -> "Saisir le texte"
        EntryKind.FILE_LINK -> "Chemin/URI de fichier"
        EntryKind.APP -> "package[/activity]"
        EntryKind.CONTACT -> "vCard ou champs"
        EntryKind.EMAIL -> "email; sujet; corps"
        EntryKind.SMS -> "+33…; message"
        EntryKind.CALL -> "+33…"
        EntryKind.LOCATION -> "lat,lon ou geo:…"
        EntryKind.ADDRESS -> "Adresse postale"
        EntryKind.CRYPTO_WALLET -> "Adresse/URI crypto"
        EntryKind.BLUETOOTH -> "MAC ou nom"
        EntryKind.WIFI -> "SSID; sécurité; mot de passe"
        EntryKind.HOTSPOT -> "SSID; mot de passe; bande"
        EntryKind.WAKE_ON_LAN -> "MAC; broadcast; port"
        EntryKind.UDP -> "host:port; payload"
        EntryKind.HTTP_REQUEST -> "Méthode et URL"
        EntryKind.HTTP_AUTH -> "user:pass@host"
        EntryKind.VOLUME -> "0-100 ou canal"
        EntryKind.ALARM -> "HH:mm ; libellé"
        EntryKind.EVENT -> "Titre; début; fin; lieu"
        EntryKind.MEDIA_CONTROL -> "play/pause/next/prev"
        EntryKind.RECORD -> "audio/vidéo; durée"
        EntryKind.VIDEO -> "URI vidéo"
        EntryKind.PHOTO -> "caméra avant/arrière"
        EntryKind.BRIGHTNESS -> "0-100"
        EntryKind.ROTATION -> "auto/on/off"
        EntryKind.NOTIFICATION_LED -> "on/off; couleur"
        EntryKind.WALLPAPER -> "URI image"
        EntryKind.UNINSTALL_APP -> "package"
        EntryKind.KILL_APP -> "package"
        EntryKind.HOME_SCREEN -> ""
        EntryKind.LOCK -> ""
        EntryKind.UNLOCK -> ""
        EntryKind.VOICE_ASSISTANT -> "commande optionnelle"
        EntryKind.CREATE_FOLDER -> "Chemin dossier"
        EntryKind.CREATE_FILE -> "Chemin fichier"
        EntryKind.MOVE_FOLDER -> "src; dst"
        EntryKind.MOVE_FILE -> "src; dst"
        EntryKind.DELETE_FILE -> "Chemin fichier"
        EntryKind.DELETE_FOLDER -> "Chemin dossier"
        EntryKind.TTS_FILE -> "Chemin fichier texte"
        EntryKind.COMPRESS -> "src; zip"
        EntryKind.DECOMPRESS -> "zip; dest"
        EntryKind.TORCH -> "on/off"
        EntryKind.VIBRATE -> "durée ms"
        EntryKind.SCREENSHOT -> ""
        EntryKind.CONDITION -> "ex: type==\"home\""
    }

    val title = when (entry.kind) {
        EntryKind.URL -> "URL"
        EntryKind.TEXT -> "Texte"
        EntryKind.FILE_LINK -> "Lien vers Fichier"
        EntryKind.APP -> "Application"
        EntryKind.CONTACT -> "Contact"
        EntryKind.EMAIL -> "Email"
        EntryKind.SMS -> "SMS"
        EntryKind.CALL -> "Appel"
        EntryKind.LOCATION -> "Localisation"
        EntryKind.ADDRESS -> "Adresse"
        EntryKind.CRYPTO_WALLET -> "Adresse Crypto"
        EntryKind.BLUETOOTH -> "Bluetooth"
        EntryKind.WIFI -> "Wi‑Fi"
        EntryKind.HOTSPOT -> "Hotspot"
        EntryKind.WAKE_ON_LAN -> "Wake on LAN"
        EntryKind.UDP -> "UDP"
        EntryKind.HTTP_REQUEST -> "Requête HTTP"
        EntryKind.HTTP_AUTH -> "Authentification HTTP"
        EntryKind.VOLUME -> "Volume"
        EntryKind.ALARM -> "Alarme"
        EntryKind.EVENT -> "Événement"
        EntryKind.MEDIA_CONTROL -> "Contrôle média"
        EntryKind.RECORD -> "Enregistrement"
        EntryKind.VIDEO -> "Vidéo"
        EntryKind.PHOTO -> "Photo"
        EntryKind.BRIGHTNESS -> "Luminosité"
        EntryKind.ROTATION -> "Rotation"
        EntryKind.NOTIFICATION_LED -> "Voyant de notification"
        EntryKind.WALLPAPER -> "Fond d’écran/Verrouillage"
        EntryKind.UNINSTALL_APP -> "Désinstaller App"
        EntryKind.KILL_APP -> "Tuer App"
        EntryKind.HOME_SCREEN -> "Écran d’accueil"
        EntryKind.LOCK -> "Verrouiller"
        EntryKind.UNLOCK -> "Déverrouiller"
        EntryKind.VOICE_ASSISTANT -> "Assistant vocal"
        EntryKind.CREATE_FOLDER -> "Créer dossier"
        EntryKind.CREATE_FILE -> "Créer fichier"
        EntryKind.MOVE_FOLDER -> "Déplacer dossier"
        EntryKind.MOVE_FILE -> "Déplacer fichier"
        EntryKind.DELETE_FILE -> "Supprimer fichier"
        EntryKind.DELETE_FOLDER -> "Supprimer dossier"
        EntryKind.TTS_FILE -> "Lire fichier (TTS)"
        EntryKind.COMPRESS -> "Compresser"
        EntryKind.DECOMPRESS -> "Décompresser"
        EntryKind.TORCH -> "Lampe torche"
        EntryKind.VIBRATE -> "Vibrer"
        EntryKind.SCREENSHOT -> "Capture d’écran"
        EntryKind.CONDITION -> "Condition"
    }

    val singleLine = when (entry.kind) {
        EntryKind.TEXT -> false
        else -> true
    }

    val ok = remember(value, entry.kind) { validate(entry.kind, value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = singleLine,
                    label = { Text(label) }
                )
                if (!ok && value.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Valeur invalide", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (ok) onConfirm(entry.copy(value = value)) }, enabled = ok) {
                Text("Valider")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun validate(kind: EntryKind, value: String): Boolean {
    if (value.isBlank()) return true // laisser saisir puis valider.
    return when (kind) {
        EntryKind.URL -> Patterns.WEB_URL.matcher(value).matches()
        EntryKind.EMAIL -> value.split(';').firstOrNull()?.let { Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches() } == true
        EntryKind.SMS, EntryKind.CALL -> value.split(';').firstOrNull()?.let { Patterns.PHONE.matcher(it.trim()).matches() } == true
        EntryKind.LOCATION -> value.startsWith("geo:") || value.split(',').let { it.size == 2 && it[0].trim().toDoubleOrNull()!=null && it[1].trim().toDoubleOrNull()!=null }
        EntryKind.WIFI -> value.contains(";")
        EntryKind.HTTP_REQUEST -> value.startsWith("GET ") || value.startsWith("POST ") || value.startsWith("PUT ") || value.startsWith("DELETE ")
        else -> true
    }
}

@Composable
private fun AddChoiceRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.TextFields, contentDescription = null)
            Text("Texte", fontWeight = FontWeight.SemiBold)
        }
        Text("Aucune donnée à écrire.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text("Ajoute des données avec le bouton “+”.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text("Commence par une entrée ci‑dessous.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun CircleButton(
    size: Dp = 56.dp,
    bg: Color = Color(0xFF2B2323),
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
