@file:OptIn(ExperimentalMaterial3Api::class)
package com.inskin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast


// --- modèle UI (utilise toutes les valeurs de SyncState) ---
enum class SyncState { WRITTEN, PENDING, ERROR }


@Composable
fun HistoryRow(item: HistoryRowUi, onClick: () -> Unit) {
    val tint = item.form?.tint ?: MaterialTheme.colorScheme.primary
    val icon = item.form?.icon ?: Icons.Filled.Memory
    val statusColor = when (item.status) {
        SyncState.WRITTEN -> Color(0xFF34C759)          // vert
        SyncState.PENDING -> Color(0xFFFFA000)          // orange
        SyncState.ERROR   -> MaterialTheme.colorScheme.error // rouge
    }

    ListItem(
        leadingContent = {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
        },
        headlineContent = { Text(item.name, maxLines = 1) },
        supportingContent = {
            Column {
                Text(item.uid, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(
                    "Dernier scan: ${formatDate(item.lastScanMs)}  •  Modifié: ${formatDate(item.lastEditMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = { ColorDot(statusColor, size = 12.dp) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    )
}

data class HistoryRowUi(
    val uid: String,
    val name: String,
    val form: BadgeForm? = null,
    val lastScanMs: Long? = null,
    val lastEditMs: Long? = null,
    val status: SyncState = SyncState.PENDING
)


private fun formatDate(ms: Long?): String {
    if (ms == null || ms <= 0L) return "—"
    return try {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        sdf.format(java.util.Date(ms))
    } catch (_: Exception) { "—" }
}

@Composable
fun TagHeaderPageTitle(
    title: String,
    uid: String,
    typeDetail: String?,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
) {
    val hAlign = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = hAlign
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable {
                clip.setText(AnnotatedString(title))
                Toast.makeText(ctx, "Type copié", Toast.LENGTH_SHORT).show()
            }
        )
        if (!typeDetail.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = typeDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    clip.setText(AnnotatedString(typeDetail))
                    Toast.makeText(ctx, "Type copié", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = uid,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable {
                clip.setText(AnnotatedString(uid))
                Toast.makeText(ctx, "UID copié", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/* ---------------------- */
/*  Petits composants UI  */
/* ---------------------- */

@Composable
fun StackedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFullScreen(
    rows: List<HistoryRowUi>,          // <- renommé
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Historique") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Retour",
                                tint = Color.Black
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                items(rows, key = { it.uid }) { row ->
                    HistoryRow(item = row) { onSelect(row.uid) }
                    HorizontalDivider()
                }
            }

        }
    }
}
