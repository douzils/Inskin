package com.inskin.app.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inskin.app.pcunlock.*
import com.inskin.app.ui.theme.AccentColor
import com.inskin.app.ui.theme.MonochromeColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcUnlockScreen(
    accentColor: AccentColor = AccentColor.ELECTRIC_BLUE,
    onBack: () -> Unit = {},
    viewModel: PcUnlockViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPc by remember { mutableStateOf<PcDevice?>(null) }

    val registeredPcs = viewModel.registeredPcs
    val connectedPc by viewModel.connectedPc.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val autoUnlockEnabled by viewModel.autoUnlockEnabled.collectAsState()

    Scaffold(
        containerColor = MonochromeColors.almostBlack,
        topBar = {
            TopAppBar(
                title = { Text("Déverrouillage PC", color = MonochromeColors.white) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = MonochromeColors.white)
                    }
                },
                actions = {
                    // Indicateur de connexion
                    AnimatedVisibility(visible = connectedPc != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Connecté",
                                fontSize = 12.sp,
                                color = accentColor.color
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MonochromeColors.darkGray1
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = accentColor.color,
                contentColor = MonochromeColors.black
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter un PC")
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
            // Toggle déverrouillage auto
            item {
                AutoUnlockToggleCard(
                    enabled = autoUnlockEnabled,
                    onToggle = { viewModel.setAutoUnlockEnabled(it) },
                    accentColor = accentColor
                )
            }

            // État de connexion Bluetooth
            item {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    connectedPc = connectedPc,
                    accentColor = accentColor,
                    onDisconnect = { viewModel.disconnectFromPc() }
                )
            }

            // Stats
            if (registeredPcs.isNotEmpty()) {
                item {
                    PcStatsCard(
                        totalPcs = registeredPcs.size,
                        totalUnlocks = registeredPcs.sumOf { it.unlockCount },
                        accentColor = accentColor
                    )
                }
            }

            // Section titre
            item {
                Text(
                    text = "PC Enregistrés",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeColors.white,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Liste des PC
            if (registeredPcs.isEmpty()) {
                item {
                    EmptyStatePcList(accentColor = accentColor)
                }
            } else {
                items(registeredPcs, key = { it.id }) { pc ->
                    PcDeviceCard(
                        pc = pc,
                        isConnected = connectedPc == pc,
                        accentColor = accentColor,
                        onClick = { selectedPc = pc },
                        onConnect = { viewModel.connectToPc(pc) },
                        onDisconnect = { viewModel.disconnectFromPc() },
                        onToggleEnabled = { viewModel.togglePcEnabled(pc) },
                        onUnlock = { viewModel.unlockPc(pc) },
                        onDelete = { viewModel.deletePc(pc) }
                    )
                }
            }
        }
    }

    // Dialog ajout PC
    if (showAddDialog) {
        AddPcDialog(
            availableDevices = viewModel.availableDevices.collectAsState().value,
            accentColor = accentColor,
            onDismiss = { showAddDialog = false },
            onAdd = { pc ->
                viewModel.addPc(pc)
                showAddDialog = false
            },
            onRefreshDevices = { viewModel.refreshAvailableDevices() }
        )
    }

    // Dialog détails PC
    selectedPc?.let { pc ->
        PcDetailsDialog(
            pc = pc,
            accentColor = accentColor,
            onDismiss = { selectedPc = null },
            onSave = { updated ->
                viewModel.updatePc(pc, updated)
                selectedPc = null
            }
        )
    }
}

@Composable
private fun AutoUnlockToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: AccentColor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MonochromeColors.darkGray2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Nfc,
                    null,
                    tint = accentColor.color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Déverrouillage Auto NFC",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MonochromeColors.white
                    )
                    Text(
                        text = "Débloquer au scan d'implant",
                        fontSize = 13.sp,
                        color = MonochromeColors.lightGray2
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor.color,
                    checkedTrackColor = accentColor.color.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BluetoothConnectionState,
    connectedPc: PcDevice?,
    accentColor: AccentColor,
    onDisconnect: () -> Unit
) {
    val (statusText, statusColor, icon) = when (connectionState) {
        is BluetoothConnectionState.Disconnected ->
            Triple("Déconnecté", MonochromeColors.gray3, Icons.Filled.BluetoothDisabled)
        is BluetoothConnectionState.Connecting ->
            Triple("Connexion...", accentColor.color, Icons.Filled.Bluetooth)
        is BluetoothConnectionState.Connected ->
            Triple("Connecté à ${connectionState.deviceName}", accentColor.color, Icons.Filled.BluetoothConnected)
        is BluetoothConnectionState.Error ->
            Triple("Erreur: ${connectionState.message}", Color(0xFFFF0080), Icons.Filled.Error)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MonochromeColors.darkGray2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = statusColor, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = statusColor
                )
            }

            if (connectedPc != null) {
                TextButton(onClick = onDisconnect) {
                    Text("Déconnecter", color = accentColor.color)
                }
            }
        }
    }
}

@Composable
private fun PcStatsCard(
    totalPcs: Int,
    totalUnlocks: Int,
    accentColor: AccentColor
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MonochromeColors.darkGray2),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalPcs.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.color
                )
                Text("PC", fontSize = 13.sp, color = MonochromeColors.lightGray2)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MonochromeColors.gray2)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalUnlocks.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.color
                )
                Text("Déverrouillages", fontSize = 13.sp, color = MonochromeColors.lightGray2)
            }
        }
    }
}

@Composable
private fun PcDeviceCard(
    pc: PcDevice,
    isConnected: Boolean,
    accentColor: AccentColor,
    onClick: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleEnabled: () -> Unit,
    onUnlock: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                accentColor.color.copy(alpha = 0.1f)
            else
                MonochromeColors.darkGray2
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Icône PC
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (pc.isEnabled) accentColor.color.copy(alpha = 0.2f)
                                else MonochromeColors.gray2.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Computer,
                            null,
                            tint = if (pc.isEnabled) accentColor.color else MonochromeColors.gray3,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pc.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pc.isEnabled) MonochromeColors.white else MonochromeColors.gray3
                            )
                            if (isConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.color)
                                )
                            }
                        }

                        Text(
                            text = pc.bluetoothName,
                            fontSize = 13.sp,
                            color = MonochromeColors.lightGray2
                        )

                        if (pc.lastUnlocked != null) {
                            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(pc.lastUnlocked))
                            Text(
                                text = "Dernier: $date",
                                fontSize = 11.sp,
                                color = MonochromeColors.gray3,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Actions
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = pc.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.size(48.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor.color,
                            checkedTrackColor = accentColor.color.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Boutons d'action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isConnected) {
                    Button(
                        onClick = onUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.color),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Déverrouiller", color = MonochromeColors.black)
                    }
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentColor.color
                        )
                    ) {
                        Text("Déconnecter")
                    }
                } else if (pc.isEnabled) {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.color),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Bluetooth, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Connecter", color = MonochromeColors.black)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null, tint = MonochromeColors.lightGray2)
                }
            }

            // Badges
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pc.requireNfcScan) {
                    InfoBadge(text = "NFC requis", accentColor = accentColor)
                }
                if (pc.unlockCount > 0) {
                    InfoBadge(text = "${pc.unlockCount} utilisations", accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String, accentColor: AccentColor) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor.color
        )
    }
}

@Composable
private fun EmptyStatePcList(accentColor: AccentColor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MonochromeColors.darkGray2),
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
                Icons.Filled.Computer,
                null,
                tint = MonochromeColors.gray2,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Aucun PC enregistré",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MonochromeColors.lightGray2
            )
            Text(
                text = "Appuyez sur + pour ajouter un PC",
                fontSize = 13.sp,
                color = MonochromeColors.gray3
            )
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPcDialog(
    availableDevices: List<BluetoothDevice>,
    accentColor: AccentColor,
    onDismiss: () -> Unit,
    onAdd: (PcDevice) -> Unit,
    onRefreshDevices: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var requireNfc by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MonochromeColors.darkGray1
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MonochromeColors.darkGray1),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ajouter un PC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeColors.white
                )

                // Nom
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du PC", color = MonochromeColors.lightGray2) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = accentColor.color,
                        unfocusedBorderColor = MonochromeColors.gray2,
                        focusedTextColor = MonochromeColors.white,
                        unfocusedTextColor = MonochromeColors.lightGray2
                    )
                )

                // Sélection appareil BT
                Text(
                    text = "Appareil Bluetooth",
                    fontSize = 14.sp,
                    color = MonochromeColors.lightGray2
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MonochromeColors.darkGray2)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (availableDevices.isEmpty()) {
                        Text(
                            text = "Aucun appareil appairé",
                            fontSize = 13.sp,
                            color = MonochromeColors.gray3,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        availableDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedDevice = device }
                                    .background(
                                        if (selectedDevice == device)
                                            accentColor.color.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDevice == device,
                                    onClick = { selectedDevice = device },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor.color
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = device.name ?: device.address,
                                    fontSize = 14.sp,
                                    color = MonochromeColors.white
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Actualiser", color = accentColor.color)
                    }
                }

                // Mot de passe (optionnel)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe Windows (optionnel)", color = MonochromeColors.lightGray2) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                null,
                                tint = MonochromeColors.lightGray2
                            )
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = accentColor.color,
                        unfocusedBorderColor = MonochromeColors.gray2,
                        focusedTextColor = MonochromeColors.white,
                        unfocusedTextColor = MonochromeColors.lightGray2
                    )
                )

                // Option NFC
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { requireNfc = !requireNfc }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nécessite scan NFC",
                        fontSize = 14.sp,
                        color = MonochromeColors.white
                    )
                    Switch(
                        checked = requireNfc,
                        onCheckedChange = { requireNfc = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor.color,
                            checkedTrackColor = accentColor.color.copy(alpha = 0.5f)
                        )
                    )
                }

                // Boutons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MonochromeColors.lightGray2
                        )
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            selectedDevice?.let { device ->
                                val pc = PcDevice(
                                    name = name.ifEmpty { device.name ?: "PC" },
                                    bluetoothAddress = device.address,
                                    bluetoothName = device.name ?: device.address,
                                    password = password,
                                    requireNfcScan = requireNfc,
                                    color = accentColor.color
                                )
                                onAdd(pc)
                            }
                        },
                        enabled = selectedDevice != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.color,
                            contentColor = MonochromeColors.black
                        )
                    ) {
                        Text("Ajouter")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PcDetailsDialog(
    pc: PcDevice,
    accentColor: AccentColor,
    onDismiss: () -> Unit,
    onSave: (PcDevice) -> Unit
) {
    var name by remember { mutableStateOf(pc.name) }
    var password by remember { mutableStateOf(pc.password) }
    var requireNfc by remember { mutableStateOf(pc.requireNfcScan) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MonochromeColors.darkGray1
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MonochromeColors.darkGray1),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configuration PC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MonochromeColors.white
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom", color = MonochromeColors.lightGray2) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = accentColor.color,
                        unfocusedBorderColor = MonochromeColors.gray2,
                        focusedTextColor = MonochromeColors.white,
                        unfocusedTextColor = MonochromeColors.lightGray2
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe", color = MonochromeColors.lightGray2) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                null,
                                tint = MonochromeColors.lightGray2
                            )
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = accentColor.color,
                        unfocusedBorderColor = MonochromeColors.gray2,
                        focusedTextColor = MonochromeColors.white,
                        unfocusedTextColor = MonochromeColors.lightGray2
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nécessite scan NFC", fontSize = 14.sp, color = MonochromeColors.white)
                    Switch(
                        checked = requireNfc,
                        onCheckedChange = { requireNfc = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor.color,
                            checkedTrackColor = accentColor.color.copy(alpha = 0.5f)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MonochromeColors.lightGray2
                        )
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            onSave(pc.copy(
                                name = name,
                                password = password,
                                requireNfcScan = requireNfc
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.color,
                            contentColor = MonochromeColors.black
                        )
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}
