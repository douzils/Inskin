package com.inskin.app.pcunlock

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel pour gérer les PC autorisés et le déverrouillage
 */
class PcUnlockViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val pcUnlockService = PcUnlockService(context)
    private val devicesFile = File(context.filesDir, "pc_devices.json")

    // Liste des PC enregistrés
    val registeredPcs = mutableStateListOf<PcDevice>()

    // PC actuellement connecté
    private val _connectedPc = MutableStateFlow<PcDevice?>(null)
    val connectedPc: StateFlow<PcDevice?> = _connectedPc

    // État de connexion Bluetooth
    val connectionState = pcUnlockService.connectionState

    // Dernier résultat de déverrouillage
    val lastUnlockResult = pcUnlockService.lastUnlockResult

    // Appareils Bluetooth disponibles
    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availableDevices: StateFlow<List<BluetoothDevice>> = _availableDevices

    // Déverrouillage automatique activé
    private val _autoUnlockEnabled = MutableStateFlow(false)
    val autoUnlockEnabled: StateFlow<Boolean> = _autoUnlockEnabled

    init {
        loadRegisteredPcs()
        refreshAvailableDevices()
    }

    /**
     * Vérifie si Bluetooth est disponible
     */
    fun isBluetoothAvailable(): Boolean {
        return pcUnlockService.isBluetoothAvailable()
    }

    /**
     * Rafraîchit la liste des appareils Bluetooth appairés
     */
    fun refreshAvailableDevices() {
        _availableDevices.value = pcUnlockService.getPairedDevices()
    }

    /**
     * Ajoute un nouveau PC
     */
    fun addPc(pc: PcDevice) {
        if (!registeredPcs.any { it.bluetoothAddress == pc.bluetoothAddress }) {
            registeredPcs.add(pc)
            saveRegisteredPcs()
        }
    }

    /**
     * Met à jour un PC existant
     */
    fun updatePc(oldPc: PcDevice, newPc: PcDevice) {
        val index = registeredPcs.indexOf(oldPc)
        if (index != -1) {
            registeredPcs[index] = newPc
            saveRegisteredPcs()
        }
    }

    /**
     * Supprime un PC
     */
    fun deletePc(pc: PcDevice) {
        registeredPcs.remove(pc)
        saveRegisteredPcs()

        if (_connectedPc.value == pc) {
            disconnectFromPc()
        }
    }

    /**
     * Active/désactive un PC
     */
    fun togglePcEnabled(pc: PcDevice) {
        updatePc(pc, pc.copy(isEnabled = !pc.isEnabled))
    }

    /**
     * Connecte à un PC
     */
    fun connectToPc(pc: PcDevice) {
        viewModelScope.launch {
            val result = pcUnlockService.connectToPc(pc)
            if (result.isSuccess) {
                _connectedPc.value = pc
            }
        }
    }

    /**
     * Déconnecte du PC actuel
     */
    fun disconnectFromPc() {
        pcUnlockService.disconnect()
        _connectedPc.value = null
    }

    /**
     * Envoie une commande de déverrouillage
     */
    fun unlockPc(pc: PcDevice, nfcUid: String = "") {
        viewModelScope.launch {
            val result = pcUnlockService.unlockPcWithNfc(pc, nfcUid)

            if (result.isSuccess) {
                // Mettre à jour les stats
                val updated = pc.copy(
                    lastUnlocked = System.currentTimeMillis(),
                    unlockCount = pc.unlockCount + 1
                )
                updatePc(pc, updated)
            }
        }
    }

    /**
     * Déverrouillage automatique après scan NFC
     */
    fun handleNfcScan(nfcUid: String) {
        if (!_autoUnlockEnabled.value) return

        // Chercher un PC activé et connecté
        val activePc = _connectedPc.value?.takeIf { it.isEnabled && it.requireNfcScan }
            ?: registeredPcs.firstOrNull { it.isEnabled && it.requireNfcScan }

        activePc?.let { pc ->
            unlockPc(pc, nfcUid)
        }
    }

    /**
     * Active/désactive le déverrouillage automatique
     */
    fun setAutoUnlockEnabled(enabled: Boolean) {
        _autoUnlockEnabled.value = enabled
    }

    /**
     * Envoie une commande manuelle
     */
    fun sendCommand(command: UnlockCommand) {
        viewModelScope.launch {
            pcUnlockService.sendUnlockCommand(command)
        }
    }

    /**
     * Sauvegarde les PC enregistrés
     */
    private fun saveRegisteredPcs() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonArray = JSONArray()

                registeredPcs.forEach { pc ->
                    val jsonObject = JSONObject().apply {
                        put("id", pc.id)
                        put("name", pc.name)
                        put("bluetoothAddress", pc.bluetoothAddress)
                        put("bluetoothName", pc.bluetoothName)
                        put("password", pc.password)
                        put("color", pc.color.value.toString())
                        put("isEnabled", pc.isEnabled)
                        put("requireNfcScan", pc.requireNfcScan)
                        put("autoConnect", pc.autoConnect)
                        put("createdAt", pc.createdAt)
                        pc.lastUnlocked?.let { put("lastUnlocked", it) }
                        put("unlockCount", pc.unlockCount)
                    }
                    jsonArray.put(jsonObject)
                }

                FileOutputStream(devicesFile).use { output ->
                    output.write(jsonArray.toString().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Charge les PC enregistrés
     */
    private fun loadRegisteredPcs() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!devicesFile.exists()) return@launch

                val json = FileInputStream(devicesFile).use { input ->
                    String(input.readBytes())
                }

                val jsonArray = JSONArray(json)
                registeredPcs.clear()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    val colorValue = try {
                        obj.getString("color").toULong()
                    } catch (e: Exception) {
                        0xFF00D9FFUL // Default electric blue
                    }

                    val pc = PcDevice(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        bluetoothAddress = obj.getString("bluetoothAddress"),
                        bluetoothName = obj.getString("bluetoothName"),
                        password = obj.optString("password", ""),
                        color = androidx.compose.ui.graphics.Color(colorValue),
                        isEnabled = obj.getBoolean("isEnabled"),
                        requireNfcScan = obj.optBoolean("requireNfcScan", true),
                        autoConnect = obj.optBoolean("autoConnect", false),
                        createdAt = obj.getLong("createdAt"),
                        lastUnlocked = if (obj.has("lastUnlocked")) obj.getLong("lastUnlocked") else null,
                        unlockCount = obj.optInt("unlockCount", 0)
                    )

                    registeredPcs.add(pc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pcUnlockService.cleanup()
    }
}
