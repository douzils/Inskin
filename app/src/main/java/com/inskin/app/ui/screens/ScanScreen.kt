package com.inskin.app.ui.screens

import com.inskin.app.NfcViewModel
import com.inskin.app.R as AppR
import androidx.compose.runtime.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/* ================== ticker indépendant du scale système ================== */
@Composable
private fun rememberTicker(): State<Long> {
    val timeMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = (now - last) / 1_000_000L
                last = now
                timeMs.longValue += dt
            }
        }
    }
    return timeMs
}

@Composable
private fun rememberTimedProgress(runKey: Any?, durationMs: Int, startWhen: Boolean): Float {
    val ticker by rememberTicker()
    var startAt by remember(runKey) { mutableStateOf<Long?>(null) }
    if (startWhen && startAt == null) startAt = ticker
    if (!startWhen) startAt = null
    return if (startAt == null) 0f else ((ticker - startAt!!).toFloat() / durationMs).coerceIn(0f, 1f)
}

private fun smooth(p: Float): Float = (p * p * (3f - 2f * p)).coerceIn(0f, 1f)
private fun lerpDp(a: Dp, b: Dp, p: Float) = a + (b - a) * p

/* =================================== SCREEN =================================== */
@Composable
fun ScanScreen(vm: NfcViewModel) {
    val isWaiting by vm.isWaiting
    val tag = vm.lastTag.value

    if (!isWaiting && tag != null) {
        TagDetectedScreen(
            title = "NTAG",
            uid   = tag.uidHex.uppercase().chunked(2).joinToString(":"),
            name  = tag.name.ifBlank { "NextV2" },
            used  = 100,
            total = 888,
            locked = false,
            onEdit = {}
        )
    } else {
        ScanIdleScreen(verticalOffset = 0.dp)
    }
}

/* =========================== IDLE (SCAN) =========================== */
@Composable
private fun ScanIdleScreen(verticalOffset: Dp = 0.dp) {
    var showBubbles by remember { mutableStateOf(false) }

    val sw = LocalConfiguration.current.screenWidthDp.dp
    val disc = (sw * 0.62f).coerceAtMost(380.dp)
    val bubble = disc * 0.34f

    // respiration + offset
    val t by rememberTicker()
    val breathPhase = (t % 3200L).toFloat() / 3200f
    val breath = 1.25f * (1f + 0.015f * kotlin.math.sin(2f * PI.toFloat() * breathPhase))
    val shiftY = with(LocalDensity.current) { 5.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = verticalOffset),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // --- Zone du disque ---
            Box(modifier = Modifier.size(disc), contentAlignment = Alignment.Center) {

                // 1) RAIL placé *avant* le disque ⇒ les bulles sortent de derrière
                OptionsArcRail(
                    disc = disc,
                    bubble = bubble,
                    expanded = showBubbles,
                    centerOffsetY = 5.dp, // le même offset que le logo
                    actions = listOf(
                        Icons.Filled.Lock to {},
                        Icons.Filled.Wifi to {},
                        Icons.Filled.Share to {}
                    )
                )

                // 2) Disque + logo (masque visuel du rail)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(AppR.drawable.antenna_tag),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = breath; scaleY = breath
                                translationY = shiftY
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                // 3) Tap-catcher : rend *tout le disque* cliquable
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(2f)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { showBubbles = !showBubbles })
                        }
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "EN ATTENTE D'UN TAG",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3E3E3E),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            TypingDots(dotSize = 10.dp, spacing = 12.dp)
        }
    }
}

/* ====================== POINTS (fade + scale synchronisés) ===================== */
@Composable
fun TypingDots(
    dotSize: Dp = 60.dp,
    spacing: Dp = 70.dp,
    color: Color = Color(0xFFB3BEC6),
    periodMs: Int = 1000
) {
    val t by rememberTicker()
    val twoPi = 2f * PI.toFloat()
    val baseTheta = (t % periodMs).toFloat() / periodMs * twoPi
    val phaseStep = twoPi / 7f

    Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            val theta = baseTheta + i * phaseStep
            val pulse = 0.5f * (1f - cos(theta))   // 0..1..0
            val alpha = 0f + 0.9f * pulse
            val scale = 1.7f + 0.15f * pulse

            Box(
                Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/* ========================= TAG DETECTED UI ========================= */
@Composable
private fun TagDetectedScreen(
    title: String,
    uid: String,
    name: String,
    used: Int,
    total: Int,
    locked: Boolean,
    onEdit: () -> Unit
) {
    val pct = (used.toFloat() / max(total, 1)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(uid, fontSize = 18.sp, color = Color(0xFF373737))

        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onEdit,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE69245))
            ) { Text("Modifier", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.Black.copy(alpha = if (locked) 1f else 0.25f),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(Color(0xFF3E3E3E)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(AppR.drawable.antenna_tag),
                contentDescription = null,
                modifier = Modifier.height(210.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(name, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text("$used / $total bytes", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        UsageBar(progress = pct)
        Spacer(Modifier.height(28.dp))
        Text("Plus d’informations", fontSize = 22.sp)
        Text("⌄", fontSize = 48.sp, color = Color.Black.copy(alpha = 0.75f))
    }
}

/* ============================ REUSABLE UI =========================== */
@Composable
private fun UsageBar(progress: Float) {
    val bg = Color(0xFF5A5A5A)
    val fg = Color(0xFF48E17C)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(CircleShape)
            .background(bg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(CircleShape)
                .background(fg)
        )
    }
}

@Composable
private fun OptionBubble(
    size: Dp,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF424242))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.38f))
    }
}

/* ====================== CARROUSEL D’OPTIONS (arc supérieur) ====================== */
/*  Bidirectionnel (ouverture/fermeture) + rail scrollable.
    Le rail est dessiné avant le disque → le disque masque ce qui est “derrière”. */
@Composable
private fun OptionsArcRail(
    disc: Dp,
    bubble: Dp,
    expanded: Boolean,
    centerOffsetY: Dp = 0.dp,
    actions: List<Pair<ImageVector, () -> Unit>>
) {
    // --- contrôleur d'animation aller/retour ---
    val t by rememberTicker()
    val duration = 240f
    var dir by remember { mutableStateOf(0) }          // 1=open, -1=close, 0=idle
    var startAt by remember { mutableStateOf<Long?>(null) }
    if (expanded && dir != 1) { dir = 1; startAt = t }
    if (!expanded && dir != -1) { dir = -1; startAt = t }

    val prog = if (startAt == null) 0f else ((t - startAt!!) / duration).coerceIn(0f, 1f)
    val p = when (dir) {
        1 -> smooth(prog)         // 0→1
        -1 -> smooth(1f - prog)   // 1→0
        else -> 0f
    }

    // géométrie
    val R = disc / 2
    val centerX = R
    val centerY = R + centerOffsetY

    val gap = bubble * 0.60f + 12.dp            // éloigne bien les bulles
    val railRadius = R + gap

    // arc visible
    val startDeg = -200f
    val endDeg   =   20f
    val span     = endDeg - startDeg
    val n        = actions.size.coerceAtLeast(1)
    val step     = span / n

    // scroll → décalage angulaire
    var angleOffset by remember { mutableStateOf(0f) }
    val scroll = rememberScrollableState { dx ->
        angleOffset += dx * -0.10f
        0f
    }

    // Enveloppe alignée au disque (pas de clip ici)
    Box(
        modifier = Modifier
            .size(disc)
            .scrollable(scroll, Orientation.Horizontal),
        contentAlignment = Alignment.TopCenter
    ) {
        // Fenêtre haute + marge pour éviter les découpes
        Box(
            modifier = Modifier
                .width(disc)
                .height(disc / 2 + bubble)
                .offset(y = (-bubble / 2))
                .clipToBounds(),
            contentAlignment = Alignment.TopStart
        ) {
            actions.forEachIndexed { i, (icon, onClick) ->
                val deg = startDeg + i * step + angleOffset
                val th  = (deg / 180f) * PI.toFloat()

                val r = lerpDp(0.dp, railRadius, p)

                val x = centerX + r * cos(th) - bubble / 2
                val y = centerY + r * sin(th) - bubble / 2

                // garder uniquement la partie au-dessus
                val visible = if (sin(th) < -0.01f) 1f else 0f

                // fondu doux aux bords
                val edge =
                    when {
                        deg < startDeg + 14f -> ((deg - (startDeg - 8f)) / 22f).coerceIn(0f, 1f)
                        deg > endDeg   - 14f -> (((endDeg + 8f) - deg) / 22f).coerceIn(0f, 1f)
                        else -> 1f
                    }

                Box(
                    modifier = Modifier
                        .absoluteOffset(x, y)
                        .graphicsLayer {
                            alpha = (p * visible * edge).coerceAtLeast(0.001f)
                            scaleX = 0.88f + 0.12f * p
                            scaleY = scaleX
                            transformOrigin = TransformOrigin.Center
                        }
                ) {
                    OptionBubble(bubble, icon, onClick)
                }
            }

            // voiles de fondu aux bords
            EdgeFades(fadeWidth = bubble * 0.45f, bg = Color(0x00FFFFFF))
        }
    }
}

@Composable
private fun EdgeFades(
    fadeWidth: Dp = 28.dp,
    bg: Color = Color(0x00FFFFFF)
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(fadeWidth)
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(listOf(bg, bg.copy(alpha = 0f)))
                )
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(fadeWidth)
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(listOf(bg.copy(alpha = 0f), bg))
                )
        )
    }
}
