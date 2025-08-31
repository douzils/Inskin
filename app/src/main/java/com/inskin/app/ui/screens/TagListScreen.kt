@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inskin.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inskin.app.NfcViewModel
import com.inskin.app.SavedTag

@Composable
fun TagListScreen(
    vm: NfcViewModel,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    val tags by vm.history
    val favorites = tags.filter { it.favorite }.sortedBy { it.name.lowercase() }
    val others = tags.filter { !it.favorite }.sortedBy { it.name.lowercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f)) {
                if (favorites.isNotEmpty()) {
                    item {
                        Text("Favoris", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(12.dp))
                    }
                    items(favorites, key = { it.uidHex }) { tag ->
                        TagRow(tag, true,
                            onSelect = { onSelect(tag.uidHex) },
                            onToggleFav = { fav -> vm.toggleFavorite(tag.uidHex, fav) })
                    }
                }
                item {
                    Text("Tous", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(12.dp))
                }
                items(others, key = { it.uidHex }) { tag ->
                    TagRow(tag, false,
                        onSelect = { onSelect(tag.uidHex) },
                        onToggleFav = { fav -> vm.toggleFavorite(tag.uidHex, fav) })
                }
            }

            // Colonne alphabétique à droite
            LazyColumn(
                Modifier.width(24.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                ('A'..'Z').forEach { c ->
                    item {
                        Text(c.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(
    tag: SavedTag,
    isFav: Boolean,
    onSelect: () -> Unit,
    onToggleFav: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(tag.name) },
        supportingContent = { Text(tag.uidHex, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            IconButton(onClick = { onToggleFav(!isFav) }) {
                Icon(
                    if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favori",
                    tint = if (isFav) Color(0xFFFFC107) else Color.Gray
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    )
}
