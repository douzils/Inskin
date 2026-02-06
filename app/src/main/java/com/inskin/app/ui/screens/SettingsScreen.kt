package com.inskin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inskin.app.ui.PreferencesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: PreferencesViewModel,
    onBack: () -> Unit = {},
    onOpenPcUnlock: () -> Unit = {}
) {
    val dark by vm.darkTheme.collectAsState()
    var nfcSoundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var autoEmulation by remember { mutableStateOf(false) }
    var advancedMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            // Apparence
            item {
                SettingsSectionHeader("Apparence", Icons.Filled.Palette)
            }

            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Filled.DarkMode,
                        title = "Thème sombre",
                        subtitle = if (dark) "Activé" else "Désactivé",
                        checked = dark,
                        onCheckedChange = { vm.toggleTheme() },
                        iconGradient = if (dark) {
                            listOf(Color(0xFF2D1B3D), Color(0xFF1A1A2E))
                        } else {
                            listOf(Color(0xFFFFA726), Color(0xFFFF6F00))
                        }
                    )
                }
            }

            // NFC & Émulation
            item {
                SettingsSectionHeader("NFC & Émulation", Icons.Filled.Nfc)
            }

            item {
                SettingsCard {
                    Column {
                        SettingsToggleItem(
                            icon = Icons.Filled.VolumeUp,
                            title = "Son NFC",
                            subtitle = "Émettre un son lors de la détection",
                            checked = nfcSoundEnabled,
                            onCheckedChange = { nfcSoundEnabled = it },
                            iconGradient = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsToggleItem(
                            icon = Icons.Filled.Vibration,
                            title = "Vibration",
                            subtitle = "Vibrer lors des interactions NFC",
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it },
                            iconGradient = listOf(Color(0xFF2196F3), Color(0xFF64B5F6))
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsToggleItem(
                            icon = Icons.Filled.PhoneAndroid,
                            title = "Émulation automatique",
                            subtitle = "Activer l'émulation au démarrage",
                            checked = autoEmulation,
                            onCheckedChange = { autoEmulation = it },
                            iconGradient = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8))
                        )
                    }
                }
            }

            // Déverrouillage PC
            item {
                SettingsSectionHeader("Déverrouillage PC", Icons.Filled.Computer)
            }

            item {
                SettingsCard {
                    SettingsClickItem(
                        icon = Icons.Filled.LockOpen,
                        title = "Déverrouillage PC",
                        subtitle = "Débloquer votre PC via NFC + Bluetooth",
                        iconGradient = listOf(Color(0xFF00D9FF), Color(0xFF0080FF)),
                        onClick = onOpenPcUnlock
                    )
                }
            }

            // Avancé
            item {
                SettingsSectionHeader("Avancé", Icons.Filled.Settings)
            }

            item {
                SettingsCard {
                    Column {
                        SettingsToggleItem(
                            icon = Icons.Filled.DeveloperMode,
                            title = "Mode avancé",
                            subtitle = "Activer les fonctionnalités avancées",
                            checked = advancedMode,
                            onCheckedChange = { advancedMode = it },
                            iconGradient = listOf(Color(0xFFFF5722), Color(0xFFFF7043))
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsClickItem(
                            icon = Icons.Filled.Key,
                            title = "Clés Mifare",
                            subtitle = "Gérer les clés d'authentification",
                            iconGradient = listOf(Color(0xFFFF9800), Color(0xFFFFB74D)),
                            onClick = { /* Gérer clés */ }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsClickItem(
                            icon = Icons.Filled.Storage,
                            title = "Gestion du stockage",
                            subtitle = "Nettoyer le cache et les données",
                            iconGradient = listOf(Color(0xFF607D8B), Color(0xFF78909C)),
                            onClick = { /* Storage */ }
                        )
                    }
                }
            }

            // À propos
            item {
                SettingsSectionHeader("À propos", Icons.Filled.Info)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF667EEA).copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Inskin NFC Manager",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Version 2.0.0",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "Application professionnelle pour la gestion de tags NFC et d'implants Dangerous Things. Avec support d'émulation HCE.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Divider()

                            SettingsClickItem(
                                icon = Icons.Filled.Update,
                                title = "Vérifier les mises à jour",
                                subtitle = "Dernière vérification : Aujourd'hui",
                                iconGradient = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)),
                                onClick = { /* Check updates */ },
                                compact = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconGradient: List<Color>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(colors = iconGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF667EEA),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconGradient: List<Color>,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 40.dp else 48.dp)
                    .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                    .background(
                        brush = Brush.linearGradient(colors = iconGradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = if (compact) 12.sp else 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
