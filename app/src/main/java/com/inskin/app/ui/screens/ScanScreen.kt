// ScanScreen.kt
@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
package com.inskin.app.ui.screens

import com.inskin.app.NfcViewModel
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import com.inskin.app.R as AppR
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings

private const val L_FLOW = "ScanFlow"
private const val L_UI   = "ScanUI"

private fun stringStateMapSaver():
        Saver<SnapshotStateMap<String, String>, HashMap<String, String>> =
    Saver(
        save = { HashMap(it) },
        restore = { saved -> mutableStateMapOf<String, String>().apply { putAll(saved) } }
    )

@Composable
private fun ReadBusyDialog(onCancel: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) { ReadBusyOverlay(onCancel) }
}

@Composable
private fun ReadBusyOverlay(onCancel: () -> Unit = {}) {
    val logs = remember { mutableStateListOf<String>() }
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        val script = listOf(
            "Connexion au tag…","Lecture UID…","Négociation NFC…",
            "Lecture des TLVs…","Décodage NDEF…","Collecte des métadonnées…"
        )
        while (true) {
            logs += if (step < script.size) script[step++] else "Lecture des blocs mémoire…"
            kotlinx.coroutines.delay(350)
        }
    }
    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.96f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Box(Modifier.size(220.dp).clip(CircleShape).background(Color(0xFF444444)), contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(3) { Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("LECTURE EN COURS", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF444444))
            Spacer(Modifier.height(8.dp))
            val scroll = rememberScrollState()
            LaunchedEffect(logs.size) { scroll.animateScrollTo(scroll.maxValue + 200) }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Column(Modifier.verticalScroll(scroll)) {
                    logs.forEachIndexed { i, line ->
                        Text(
                            text = if (i == logs.lastIndex) "• $line" else "✓ $line",
                            fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Laissez le tag en place", fontSize = 16.sp, color = Color(0xFF444444))
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancel) { Text("Annuler") }
        }
    }
}

/* ---------- Busy latch ---------- */
@Composable
private fun rememberBusyLatch(minMs: Long = 450L): Pair<(Boolean) -> Unit, State<Boolean>> {
    val show = remember { mutableStateOf(false) }
    var latchJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val setBusy: (Boolean) -> Unit = { busy ->
        if (busy) {
            if (!show.value) show.value = true
            latchJob?.cancel()
            latchJob = scope.launch(Dispatchers.Main) { delay(minMs) }
        } else {
            val job = latchJob
            if (job == null || job.isCompleted) show.value = false
            else scope.launch(Dispatchers.Main) { job.join(); show.value = false }
        }
    }
    return setBusy to show
}

/* ------------------------------ ScanScreen ------------------------------ */
@Composable
fun ScanScreen(
    vm: NfcViewModel,
    onOpenWrite: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val homeScroll = rememberScrollState()
    var homeExpanded by rememberSaveable { mutableStateOf(false) }
    var homeAngle by rememberSaveable { mutableFloatStateOf(0f) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }

    val historyItems by vm.history
    val itemsSorted = remember(historyItems) { historyItems.sortedByDescending { it.savedAt } }
    val tag = vm.lastTag.value
    val details = vm.lastDetails.value

    val iconByUid = rememberSaveable(saver = Saver(
        save = { HashMap(it) }, restore = { saved -> mutableStateMapOf<String, String>().apply { putAll(saved) } }
    )) { mutableStateMapOf<String, String>() }
    val nameByUid = rememberSaveable(saver = Saver(
        save = { HashMap(it) }, restore = { saved -> mutableStateMapOf<String, String>().apply { putAll(saved) } }
    )) { mutableStateMapOf<String, String>() }

    if (vm.showAuthDialog.value) {
        UnlockDialog(
            busy = vm.authBusy.value,
            error = vm.authError.value,
            onDismiss = { vm.showAuthDialog.value = false },
            onSubmit = { pwd, remember -> vm.submitNtagPassword(pwd, remember) }
        )
    }

    val historyUi = remember(itemsSorted, iconByUid, nameByUid) {
        itemsSorted.map { h ->
            val uid = h.uidHex.uppercase()
            HistoryRowUi(
                uid = uid,
                name = nameByUid[uid] ?: h.name.ifBlank { "Tag" },
                form = iconByUid[uid]?.let { BadgeForm.valueOf(it) },
                lastScanMs = h.savedAt,
                lastEditMs = null,
                status = SyncState.WRITTEN
            )
        }
    }
    LaunchedEffect(tag) { historyOpen = false }

    val curUid = tag?.uidHex?.uppercase()
    val detUid = details?.uidHex?.uppercase()

    val (setBusy, busy) = rememberBusyLatch(minMs = 500L)
    val needBusyNow = tag != null && (details == null || detUid != curUid || vm.authBusy.value)
    LaunchedEffect(needBusyNow, curUid, detUid, vm.authBusy.value) { setBusy(needBusyNow) }

    Box(
        Modifier.fillMaxSize().pointerInput(historyOpen) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val w = this.size.width.toFloat()
                if (down.position.x > w * 0.92f) {
                    var dx = 0f
                    drag(down.id) { change: PointerInputChange ->
                        dx += change.positionChange().x
                        if (!historyOpen && dx < -40f) historyOpen = true
                    }
                }
            }
        }
    ) {
        if (tag == null) {
            // Accueil
            ScanIdleScreen(
                verticalOffset = 0.dp,
                belowCircleGap = 32.dp,
                textSpacer = 16.dp,
                onOpenList = onOpenList,
                onOpenSettings = onOpenSettings,
                onTapHistory = { historyOpen = true },
                scroll = homeScroll,
                expanded = homeExpanded,
                onExpandedChange = { homeExpanded = it },
                angle = homeAngle,
                onAngleChange = { homeAngle = it }
            )
        } else {
            // Page Tag détecté
            val uidStr = curUid!!
            val selectedForm = iconByUid[uidStr]?.let { BadgeForm.valueOf(it) }

            // Nom préféré : map → historique → valeur du tag
            val fromHistoryName = remember(itemsSorted, uidStr) {
                itemsSorted.firstOrNull { it.uidHex.equals(uidStr, ignoreCase = true) }?.name
            }
            val currentName = nameByUid[uidStr]
                ?: fromHistoryName
                ?: tag.name.ifBlank { "Tag" }

            TagDetectedScreen(
                title = tag.typeLabel,
                typeDetail = tag.typeDetail,
                uid = uidStr.chunked(2).joinToString(":"),
                name = currentName,
                used = tag.used,
                total = tag.total,
                locked = tag.locked,
                details = details,
                canAskUnlock = vm.canAskUnlock.value,
                onAskUnlock = { vm.askUnlock() },
                onOpenWrite = onOpenWrite,
                onOpenHistory = { historyOpen = true },   // <- ouvre l’overlay Historique
                onBack = { vm.startWaiting() },
                selectedForm = selectedForm,
                onPickForm = { form -> iconByUid[uidStr] = form.name },
                onRename = { newName -> nameByUid[uidStr] = newName }
            )

            // IMPORTANT : plus d’icône Historique ici pour éviter le doublon.
            // L’icône Historique est gérée dans TagHeaderPage (en haut-droite).
        }

        if (historyOpen) {
            HistoryFullScreen(
                rows = historyUi,
                onClose = { historyOpen = false },
                onSelect = { uid -> vm.selectFromHistory(uid); historyOpen = false }
            )
        }

        if (busy.value) {
            ReadBusyDialog(onCancel = {
                vm.startWaiting()
                setBusy(false)
            })
        }
    }
}

/* =========================== Accueil (idle) =========================== */
@Composable
private fun ScanIdleScreen(
    verticalOffset: Dp = 0.dp,
    belowCircleGap: Dp = 30.dp,
    textSpacer: Dp = 20.dp,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onTapHistory: () -> Unit,
    scroll: ScrollState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    angle: Float,
    onAngleChange: (Float) -> Unit
) {
    val sw = LocalConfiguration.current.screenWidthDp.dp
    val sh = LocalConfiguration.current.screenHeightDp.dp
    val disc = (sw * 0.62f).coerceAtMost(380.dp)

    // <-- Ajout : offset vertical réglable
    val circleOffsetY = verticalOffset

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // <-- Centre verticalement
        ) {
            Box(
                modifier = Modifier
                    .size(disc)
                    .offset(y = circleOffsetY), // <-- Offset appliqué ici
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(disc)
                        .clip(CircleShape)
                        .background(Color(0xFF202020)),
                    contentAlignment = Alignment.Center
                ) {
                    BreathingTag(
                        logoScale = 1.2f,   // occupe 92 % du cercle
                        logoOffsetY = (3.5).dp // petit décalage vers le haut
                    )
                }
            }

            Spacer(Modifier.height(belowCircleGap))
            Text(
                "SCANNEZ LE TAG",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3E3E3E)
            )
            Spacer(Modifier.height(textSpacer))
            TypingDots(dotSize = 10.dp, spacing = 10.dp)
        }

        // Boutons haut/bas (inchangés)
        IconButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) { Icon(Icons.Filled.AccountTree, null, tint = Color.Gray) }

        IconButton(
            onClick = onTapHistory,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) { Icon(Icons.Filled.History, null) }

        IconButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) { Icon(Icons.Filled.Memory, null, tint = Color.Gray) }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Settings, null) }
    }
}

@Composable
private fun BreathingTag(
    logoScale: Float = 0.9f,   // >0, peut dépasser 1
    logoOffsetY: Dp = 0.dp
) {
    val t by rememberTicker()
    val phase = (t % 1600L).toFloat() / 1600f
    val breathe = 1.0f + 0.02f * kotlin.math.sin(2f * kotlin.math.PI.toFloat() * phase)

    val base = maxOf(0.01f, minOf(1f, logoScale))     // facteur pour fillMaxSize
    val extra = logoScale / base                       // surplus appliqué via graphicsLayer

    Image(
        painter = painterResource(AppR.drawable.antenna_tag),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize(base)                         // occupe jusqu’à 100% du cercle
            .offset(y = logoOffsetY)
            .graphicsLayer {                           // agrandit au-delà de 100% si besoin
                scaleX = breathe * extra
                scaleY = breathe * extra
            },
        contentScale = ContentScale.Fit
    )
}



/* ==== helpers ==== */
@Composable
private fun rememberTicker(): State<Long> {
    val t = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                t.longValue += (now - last) / 1_000_000L
                last = now
            }
        }
    }
    return t
}

@Composable
fun TypingDots(dotSize: Dp = 10.dp, spacing: Dp = 10.dp) {
    val transition = rememberInfiniteTransition(label = "dots")
    val phases = List(3) { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, delayMillis = i * 150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$i"
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        phases.forEach { v ->
            Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .graphicsLayer { alpha = 0.4f + 0.6f * v.value }
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
private fun UnlockDialog(
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (pwdHex: String, remember: Boolean) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    var remember by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Déverrouiller le tag") },
        text = {
            Column {
                Text("NTAG21x / Ultralight protégé.\nSaisis le mot de passe (4 octets HEX).")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it.uppercase().filter { c -> c.isDigit() || c in 'A'..'F' } },
                    singleLine = true,
                    label = { Text("Mot de passe (ex: 11223344)") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remember, onCheckedChange = { remember = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Mémoriser pour ce tag")
                }
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Erreur : $error", color = Color(0xFFB00020))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(pwd, remember) }, enabled = !busy) {
                Text(if (busy) "…" else "Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!busy) onDismiss() }) {
                Text("Ignorer")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagDetectedScreen(
    title: String, typeDetail: String?, uid: String, name: String,
    used: Int, total: Int, locked: Boolean,
    details: com.inskin.app.TagDetails?, canAskUnlock: Boolean,
    onAskUnlock: () -> Unit,
    onOpenWrite: () -> Unit,
    onOpenHistory: () -> Unit,             // <- remplace onOpenList
    onBack: () -> Unit,
    selectedForm: BadgeForm?,
    onPickForm: (BadgeForm) -> Unit,
    onRename: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val snap = rememberSnapFlingBehavior(lazyListState = list)

    LazyColumn(state = list, flingBehavior = snap, modifier = Modifier.fillMaxSize()) {
        item("page_header") {
            Box(Modifier.fillParentMaxHeight().fillMaxWidth()) {
                TagHeaderPage(
                    title = title,
                    uid = uid,
                    name = name,
                    used = used,
                    total = total,
                    locked = locked,
                    typeDetail = typeDetail,
                    details = details,
                    onBack = onBack,
                    selectedForm = selectedForm,
                    onPickForm = onPickForm,
                    onRename = onRename,
                    onOpenHistory = onOpenHistory,          // <- utilise le bon callback
                    onOpenWrite = onOpenWrite,
                    onRequestGoDown = { scope.launch { list.animateScrollToItem(1) } }
                )
            }
        }
        item("page_info") {
            Box(Modifier.fillParentMaxHeight().fillMaxWidth()) {
                TagInfoPage(
                    title = title, uid = uid, total = total, locked = locked, details = details,
                    canAskUnlock = canAskUnlock, onAskUnlock = onAskUnlock,
                    onRequestGoUp = { scope.launch { list.animateScrollToItem(0) } }
                )
            }
        }
    }
}
