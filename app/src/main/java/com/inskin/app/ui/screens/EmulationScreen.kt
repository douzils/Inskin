package com.inskin.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inskin.app.emulation.EmulationState
import com.inskin.app.emulation.EmulationViewModel
import com.inskin.app.emulation.VirtualCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulationScreen(
    onBack: () -> Unit = {},
    viewModel: EmulationViewModel = viewModel()
) {
    val emulationState by viewModel.emulationState.collectAsState()
    val virtualCards = viewModel.virtualCards

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Émulation NFC") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Ajouter carte */ }) {
                        Icon(Icons.Filled.Add, "Ajouter")
                    }
                }
            )
        },
        floatingActionButton = {
            if (emulationState is EmulationState.Active) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.stopEmulation() },
                    containerColor = Color(0xFFFF5722),
                    icon = { Icon(Icons.Filled.Stop, null) },
                    text = { Text("Arrêter l'émulation") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statut d'émulation
            item {
                EmulationStatusCard(emulationState, viewModel.totalEmulations)
            }

            // Cartes virtuelles
            item {
                Text(
                    text = "Mes Cartes Virtuelles",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (virtualCards.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                items(virtualCards, key = { it.id }) { card ->
                    VirtualCardItem(
                        card = card,
                        isActive = card.isActive,
                        onActivate = { viewModel.startEmulation(card) },
                        onDelete = { viewModel.deleteVirtualCard(card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmulationStatusCard(state: EmulationState, totalEmulations: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = when (state) {
                        is EmulationState.Active -> Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                        )
                        is EmulationState.Error -> Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFFF7043))
                        )
                        else -> Brush.horizontalGradient(
                            colors = listOf(Color(0xFF607D8B), Color(0xFF78909C))
                        )
                    }
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icône animée
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (state is EmulationState.Active) 1.2f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (state) {
                                is EmulationState.Active -> Icons.Filled.Nfc
                                is EmulationState.Error -> Icons.Filled.Error
                                else -> Icons.Filled.PhoneAndroid
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Text(
                            text = when (state) {
                                is EmulationState.Active -> "Émulation Active"
                                is EmulationState.Error -> "Erreur"
                                else -> "Prêt à émuler"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (state) {
                                is EmulationState.Active -> state.card.name
                                is EmulationState.Error -> state.message
                                else -> "$totalEmulations émulations effectuées"
                            },
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualCardItem(
    card: VirtualCard,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (!isActive) onActivate else { {} }),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 12.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) card.color.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Indicateur actif
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ACTIF",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône de carte
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    card.color,
                                    card.color.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "UID: ${card.uid}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = card.type.displayName,
                        fontSize = 12.sp,
                        color = card.color
                    )
                }

                // Bouton supprimer
                if (!isActive) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Aucune carte virtuelle",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Scannez un tag NFC pour créer votre première carte virtuelle",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
