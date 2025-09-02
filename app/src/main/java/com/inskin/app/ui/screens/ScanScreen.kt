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
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.inskin.app.usb.ProxmarkStatus
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.viewmodel.compose.viewModel


private const val L_FLOW = "ScanFlow"
private const val L_UI   = "ScanUI"

private fun stringStateMapSaver():
        Saver<SnapshotStateMap<String, String>, HashMap<String, String>> =
    Saver(
        save = { HashMap(it) },
        restore = { saved -> mutableStateMapOf<String, String>().apply { putAll(saved) } }
    )

@Composable
private fun ReadBusyDialog(
    onCancel: () -> Unit,
    phoneLevel: Int,
    logs: List<String>           // <- ajouté
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) { ReadBusyOverlay(onCancel = onCancel, phoneLevel = phoneLevel, logs = logs) }
}

@Composable
private fun ReadBusyOverlay(
    onCancel: () -> Unit = {},
    phoneLevel: Int,
    logs: List<String>           // <- ajouté
) {
    // ⚠️ SUPPRIMER tout le LaunchedEffect qui fabriquait des logs factices
    Box(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            /* ... le visuel du cercle + SignalBars reste identique ... */

            val scroll = rememberScrollState()
            LaunchedEffect(logs.size) { scroll.animateScrollTo(scroll.maxValue + 200) }
            Box(
                Modifier.fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Column(Modifier.verticalScroll(scroll)) {
                    logs.forEachIndexed { i, line ->
                        Text(
                            text = if (i == logs.lastIndex) "• $line" else "✓ $line",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
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
    val status = vm.pm3Status.value

    val homeScroll = rememberScrollState()
    var homeExpanded by rememberSaveable { mutableStateOf(false) }
    var homeAngle by rememberSaveable { mutableFloatStateOf(0f) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }

    val itemsSorted = vm.history.sortedByDescending { it.savedAt }
    val tag = vm.lastTag.value
    val details = vm.lastDetails.value

    val iconByUid = rememberSaveable(
        saver = Saver(
        save = { HashMap(it) },
        restore = { saved -> mutableStateMapOf<String, String>().apply { putAll(saved) } }
    )) { mutableStateMapOf<String, String>() }
    val nameByUid = rememberSaveable(
        saver = Saver(
            save = { HashMap(it) },
        restore = { saved -> mutableStateMapOf<String, String>().apply { putAll(saved) } }
    )) { mutableStateMapOf<String, String>() }

    if (vm.showAuthDialog.value) {
        UnlockDialog(
            busy = vm.authBusy.value,
            error = vm.authError.value,
            onDismiss = { vm.showAuthDialog.value = false },
            onSubmit = { pwd, remember -> vm.submitNtagPassword(pwd, remember) }
        )
    }

    val historyUi = itemsSorted.map { h ->
        val uid = h.uidHex.uppercase()
        val formKey = iconByUid[uid] ?: h.form
        HistoryRowUi(
            uid = uid,
            name = nameByUid[uid] ?: h.name.ifBlank { "Tag" },
            form = formKey?.let { BadgeForm.valueOf(it) },
            lastScanMs = h.savedAt,
            lastEditMs = null,
            status = SyncState.WRITTEN
        )
    }

    LaunchedEffect(tag) { historyOpen = false }

    val curUid = tag?.uidHex?.uppercase()
    LaunchedEffect(curUid) {
        val uid = curUid ?: return@LaunchedEffect
        if (iconByUid[uid] == null) {
            vm.getFormFor(uid)?.let { iconByUid[uid] = it }
        }
    }

    val detUid = details?.uidHex?.uppercase()

    val (setBusy, busy) = rememberBusyLatch(minMs = 500L)
    val needBusyNow = vm.readingNow.value && tag != null &&
            (details == null || detUid != curUid || vm.authBusy.value)
    LaunchedEffect(needBusyNow, curUid, detUid, vm.authBusy.value) {
        setBusy(needBusyNow)
    }




    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(historyOpen) {
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
                showBubble = (status == ProxmarkStatus.Ready) && !historyOpen,
                status = status,
                onOpenList = onOpenList,
                onOpenSettings = onOpenSettings,
                onTapHistory = { historyOpen = true },
                scroll = homeScroll,
                expanded = homeExpanded,
                onExpandedChange = { homeExpanded = it },
                angle = homeAngle,
                onAngleChange = { homeAngle = it },
                phoneLevel = vm.phoneSignalLevel.value    // <—
            )



        } else {
            // Page Tag détecté
            val uidStr = curUid!!
            val selectedForm =
                iconByUid[uidStr]?.let { BadgeForm.valueOf(it) }
                    ?: itemsSorted.firstOrNull { it.uidHex.equals(uidStr, true) }
                        ?.form?.let { BadgeForm.valueOf(it) }

            val fromHistoryName = remember(itemsSorted, uidStr) {
                itemsSorted.firstOrNull { it.uidHex.equals(uidStr, ignoreCase = true) }?.name
            }
            val currentName = nameByUid[uidStr]
                ?: fromHistoryName
                ?: tag.name.ifBlank { "Tag" }

            TagDetectedScreen(
                title = tag.typeLabel,
                typeDetail = tag.typeDetail,
                uid = uidStr,
                name = currentName,
                used = tag.used,
                total = tag.total,
                locked = tag.locked,
                details = details,
                canAskUnlock = vm.canAskUnlock.value,
                onAskUnlock = { vm.askUnlock() },
                onOpenWrite = onOpenWrite,
                onOpenHistory = { historyOpen = true },
                onBack = { vm.startWaiting() },
                selectedForm = selectedForm,
                onPickForm = { form ->
                    iconByUid[uidStr] = form.name   // UI
                    vm.setTagForm(uidStr, form.name) // DB
                },
                onRename = { newName ->
                    nameByUid[uidStr] = newName
                    vm.renameTag(uidStr, newName)
                }
            )

        }

        if (historyOpen) {
            HistoryFullScreen(
                rows = historyUi,
                onClose = { historyOpen = false },
                onSelect = { uid ->
                    vm.selectFromHistory(uid)
                    historyOpen = false
                }
            )
        }

        if (busy.value) {
            ReadBusyDialog(
                onCancel = { vm.startWaiting(); setBusy(false) },
                phoneLevel = vm.phoneSignalLevel.value,
                logs = vm.liveLogs                  // <- ici
            )
        }


    }
}

@Composable
fun SignalBars(
    level: Int,                // 0..4
    barCount: Int = 4,
    offsetX: Dp = 0.dp,        // nouvel offset X
    offsetY: Dp = 0.dp,        // nouvel offset Y
    width: Dp = 18.dp,         // largeur totale du groupe
    height: Dp = 14.dp,        // hauteur totale du groupe
    modifier: Modifier = Modifier
) {
    val lv = level.coerceIn(0, barCount)
    Row(
        modifier = modifier
            .offset(offsetX, offsetY)
            .size(width = width, height = height),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { i ->
            val on = i < lv
            Box(
                Modifier
                    .padding(end = 1.dp)
                    .width(3.dp)
                    .fillMaxHeight((i + 1) / barCount.toFloat())
                    .background(if (on) Color.White else Color(0xFF4B5563), shape = CircleShape)
            )
        }
    }
}


/* =========================== Accueil (idle) =========================== */
@Composable
private fun ScanIdleScreen(
    verticalOffset: Dp = 0.dp,
    belowCircleGap: Dp = 30.dp,
    textSpacer: Dp = 20.dp,
    showBubble: Boolean,
    status: ProxmarkStatus,
    phoneLevel: Int,
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
    val disc = (sw * 0.62f).coerceAtMost(380.dp)
    val circleOffsetY = verticalOffset

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(disc)
                    .offset(y = circleOffsetY),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(disc)
                        .offset(y = circleOffsetY),
                    contentAlignment = Alignment.Center
                ) {
                    // CERCLE (clippé)
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(Color(0xFF202020)),
                        contentAlignment = Alignment.Center
                    ) {
                        BreathingTag(logoScale = 1.2f, logoOffsetY = 3.5.dp)
                    }

                    // BARRES AU-DESSUS, NON CLIPPÉES
                    SignalBars(
                        level   = phoneLevel,
                        width   = 30.dp,
                        height  = 20.dp,
                        offsetX = (-0).dp,   // vers l’intérieur
                        offsetY = 0.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .zIndex(1f)
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
            TypingDots(dotSize = 15.dp, spacing = 20.dp)
        }

        // Overlay centré sous les points, n’impacte pas la mise en page
        if (showBubble && status == ProxmarkStatus.Ready) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                ProxmarkFab(
                    status = status,
                    modifier = Modifier.offset(y = 300.dp)
                )
            }
        }

        // Boutons haut/bas
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
    val phases = List(4) { i ->
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
private fun ProxmarkFab(
    status: ProxmarkStatus,
    onClick: () -> Unit = {},
    diameter: Dp = 56.dp,
    level: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(diameter)
            .shadow(6.dp, CircleShape, clip = false)
            .background(Color.Black, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Icône centrale Proxmark
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val minDim = size.minDimension
            drawCircle(Color.White, radius = minDim * 0.12f)
            listOf(0.28f, 0.42f, 0.56f).forEach { r ->
                drawCircle(
                    Color.White,
                    radius = minDim * r,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
            drawLine(
                color = Color.Black,
                start = center + Offset(0f, -minDim * 0.56f),
                end   = center + Offset(0f, -minDim * 0.28f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        // Badge statut
        val badge = when (status) {
            ProxmarkStatus.Ready -> Color(0xFF22C55E)
            ProxmarkStatus.Initializing -> Color(0xFFF59E0B)
            ProxmarkStatus.NotPresent -> Color(0xFF9CA3AF)
            ProxmarkStatus.NoAccess, ProxmarkStatus.Error -> Color(0xFFEF4444)
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-4).dp, y = (-4).dp)
                .size(14.dp)
                .shadow(2.dp, CircleShape)
                .background(badge, CircleShape)
        )

        // Indicateur signal en haut-gauche
        SignalBars(
            level = level.coerceIn(0, 4),
            offsetX = 45.dp,
            offsetY = 42.dp,
            width = 18.dp,
            height = 14.dp,
            modifier = Modifier.align(Alignment.TopStart)
        )
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
    title: String,
    typeDetail: String?,
    uid: String,
    name: String,
    used: Int,
    total: Int,
    locked: Boolean,
    details: com.inskin.app.TagDetails?,
    canAskUnlock: Boolean,
    onAskUnlock: () -> Unit,
    onOpenWrite: () -> Unit,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
    selectedForm: BadgeForm?,
    onPickForm: (BadgeForm) -> Unit,
    onRename: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val snap = rememberSnapFlingBehavior(lazyListState = list)

    LazyColumn(state = list, flingBehavior = snap, modifier = Modifier.fillMaxSize()) {

        // ⚠️ enlève le named param "key =" si ta version de Compose ne le supporte pas
        item {
            Box(Modifier.fillParentMaxHeight().fillMaxWidth()) {
                TagHeaderPage(
                    title = title,
                    uid = uid.chunked(2).joinToString(":"), // si tu veux l’affichage AA:BB:..
                    name = name,
                    used = used,
                    total = total,
                    locked = locked,
                    typeDetail = typeDetail,
                    details = details,
                    onBack = onBack,
                    selectedForm = selectedForm,
                    onPickForm = onPickForm,    // pass-through
                    onRename = onRename,        // pass-through
                    onOpenHistory = onOpenHistory,
                    onOpenWrite = onOpenWrite,
                    onRequestGoDown = { scope.launch { list.animateScrollToItem(1) } }
                )
            }
        }

        item {
            Box(Modifier.fillParentMaxHeight().fillMaxWidth()) {
                TagInfoPage(
                    title = title,
                    uid = uid,
                    total = total,
                    locked = locked,
                    details = details,
                    canAskUnlock = canAskUnlock,
                    onAskUnlock = onAskUnlock,
                    onRequestGoUp = { scope.launch { list.animateScrollToItem(0) } }
                )
            }
        }
    }
}
