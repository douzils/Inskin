package com.inskin.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inskin.app.ui.theme.AccentColor
import com.inskin.app.ui.theme.MonochromeColors
import com.inskin.app.writing.WritingList
import com.inskin.app.writing.WritingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingListsScreen(
    accentColor: AccentColor = AccentColor.ELECTRIC_BLUE,
    onBack: () -> Unit = {},
    viewModel: WritingListViewModel = viewModel()
) {
    var showPresets by remember { mutableStateOf(false) }

    val customLists = viewModel.writingLists
    val presets = viewModel.availablePresets

    Scaffold(
        containerColor = MonochromeColors.almostBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Listes d'Écriture",
                        color = MonochromeColors.white
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            null,
                            tint = MonochromeColors.white
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPresets = !showPresets }) {
                        Icon(
                            if (showPresets) Icons.Filled.LibraryBooks else Icons.Filled.AddCircle,
                            null,
                            tint = accentColor.color
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MonochromeColors.darkGray1
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Créer nouvelle liste */ },
                containerColor = accentColor.color,
                contentColor = MonochromeColors.black
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Créer une liste")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Toggle Presets / Mes Listes
            item {
                SegmentedControl(
                    items = listOf("Mes Listes", "Presets"),
                    selectedIndex = if (showPresets) 1 else 0,
                    onItemSelected = { showPresets = it == 1 },
                    accentColor = accentColor
                )
            }

            // Stats
            if (!showPresets && customLists.isNotEmpty()) {
                item {
                    StatsCard(
                        totalLists = customLists.size,
                        totalWrites = viewModel.totalWrites,
                        accentColor = accentColor
                    )
                }
            }

            // Section titre
            item {
                Text(
                    text = if (showPresets) "Presets Disponibles" else "Mes Listes Personnalisées",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeColors.white,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Listes
            val listsToShow = if (showPresets) presets else customLists

            if (listsToShow.isEmpty()) {
                item {
                    EmptyStateWritingLists(
                        isPresets = showPresets,
                        accentColor = accentColor
                    )
                }
            } else {
                items(listsToShow, key = { it.id }) { list ->
                    WritingListItem(
                        list = list,
                        accentColor = accentColor,
                        isPreset = showPresets,
                        onClick = { /* Ouvrir détails */ },
                        onClone = if (showPresets) {
                            { viewModel.clonePreset(list) }
                        } else null,
                        onFavorite = if (!showPresets) {
                            { viewModel.toggleFavorite(list) }
                        } else null,
                        onDelete = if (!showPresets) {
                            { viewModel.deleteWritingList(list) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    accentColor: AccentColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MonochromeColors.darkGray2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) accentColor.color
                        else Color.Transparent
                    )
                    .clickable { onItemSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MonochromeColors.black else MonochromeColors.lightGray2
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    totalLists: Int,
    totalWrites: Int,
    accentColor: AccentColor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MonochromeColors.darkGray2
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(
                value = totalLists.toString(),
                label = "Listes",
                accentColor = accentColor
            )
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp),
                color = MonochromeColors.gray2
            )
            StatItem(
                value = totalWrites.toString(),
                label = "Écritures",
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, accentColor: AccentColor) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor.color
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = MonochromeColors.lightGray2
        )
    }
}

@Composable
private fun WritingListItem(
    list: WritingList,
    accentColor: AccentColor,
    isPreset: Boolean,
    onClick: () -> Unit,
    onClone: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MonochromeColors.darkGray2
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Icône
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit, // TODO: icon dynamique
                            contentDescription = null,
                            tint = accentColor.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = list.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MonochromeColors.white
                            )
                            if (list.isFavorite) {
                                Icon(
                                    Icons.Filled.Star,
                                    null,
                                    tint = accentColor.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = list.description,
                            fontSize = 13.sp,
                            color = MonochromeColors.lightGray2
                        )

                        Text(
                            text = "${list.actions.size} actions",
                            fontSize = 11.sp,
                            color = MonochromeColors.gray3,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    onClone?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                null,
                                tint = accentColor.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    onFavorite?.let {
                        IconButton(onClick = it) {
                            Icon(
                                if (list.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                null,
                                tint = accentColor.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Filled.Delete,
                                null,
                                tint = MonochromeColors.lightGray2,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Badge de type preset
            if (isPreset) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.color.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRESET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.color
                    )
                }
            }

            // Stats d'utilisation
            if (!isPreset && list.useCount > 0) {
                Text(
                    text = "Utilisé ${list.useCount} fois",
                    fontSize = 11.sp,
                    color = MonochromeColors.gray3,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateWritingLists(
    isPresets: Boolean,
    accentColor: AccentColor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MonochromeColors.darkGray2
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isPresets) Icons.Filled.LibraryBooks else Icons.Filled.Edit,
                contentDescription = null,
                tint = MonochromeColors.gray2,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = if (isPresets) "Aucun preset disponible" else "Aucune liste personnalisée",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MonochromeColors.lightGray2
            )
            Text(
                text = if (isPresets)
                    "Les presets seront chargés automatiquement"
                else
                    "Appuyez sur + pour créer votre première liste",
                fontSize = 13.sp,
                color = MonochromeColors.gray3
            )
        }
    }
}
