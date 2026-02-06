package com.inskin.app.pcunlock

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Service de communication Bluetooth avec le PC pour déverrouillage
 */
class PcUnlockService(private val context: Context) {

    companion object {
        private const val TAG = "PcUnlockService"
        // UUID standard pour SPP (Serial Port Profile)
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(
        BluetoothConnectionState.Disconnected
    )
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState

    private val _lastUnlockResult = MutableStateFlow<UnlockResult?>(null)
    val lastUnlockResult: StateFlow<UnlockResult?> = _lastUnlockResult

    /**
     * Vérifie si Bluetooth est disponible et activé
     */
    @SuppressLint("MissingPermission")
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    /**
     * Vérifie si on a les permissions Bluetooth
     */
    fun hasBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Liste les appareils Bluetooth appairés
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) {
            return emptyList()
        }

        return try {
            bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting paired devices", e)
            emptyList()
        }
    }

    /**
     * Connecte à un PC via Bluetooth
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToPc(device: PcDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) {
                return@withContext Result.failure(SecurityException("Missing Bluetooth permissions"))
            }

            _connectionState.value = BluetoothConnectionState.Connecting

            // Récupérer le device Bluetooth
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.bluetoothAddress)
                ?: return@withContext Result.failure(Exception("Bluetooth device not found"))

            // Créer socket
            bluetoothSocket = try {
                bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: SecurityException) {
                return@withContext Result.failure(e)
            }

            // Annuler la découverte pour améliorer les performances
            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                // Ignore
            }

            // Connecter
            try {
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                _connectionState.value = BluetoothConnectionState.Connected(device.name)

                // Démarrer l'écoute des messages du PC
                startListening()

                Result.success(Unit)
            } catch (e: IOException) {
                disconnect()
                _connectionState.value = BluetoothConnectionState.Error("Échec de connexion: ${e.message}")
                Result.failure(e)
            }

        } catch (e: Exception) {
            _connectionState.value = BluetoothConnectionState.Error(e.message ?: "Erreur inconnue")
            Result.failure(e)
        }
    }

    /**
     * Déconnecte du PC
     */
    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing Bluetooth connection", e)
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            _connectionState.value = BluetoothConnectionState.Disconnected
        }
    }

    /**
     * Vérifie si connecté à un PC
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Envoie une commande de déverrouillage au PC
     */
    suspend fun sendUnlockCommand(command: UnlockCommand): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                _lastUnlockResult.value = UnlockResult.NoPcConnected
                return@withContext Result.failure(Exception("Not connected to PC"))
            }

            val bytes = command.toBytes()
            outputStream?.write(bytes)
            outputStream?.flush()

            Log.d(TAG, "Sent command: ${String(bytes, Charsets.UTF_8).trim()}")

            _lastUnlockResult.value = UnlockResult.Success
            Result.success(Unit)

        } catch (e: IOException) {
            Log.e(TAG, "Error sending command", e)
            _lastUnlockResult.value = UnlockResult.Failed(e.message ?: "IO Error")
            Result.failure(e)
        }
    }

    /**
     * Écoute les messages du PC
     */
    private fun startListening() {
        scope.launch {
            val buffer = ByteArray(1024)
            while (isConnected()) {
                try {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val message = String(buffer, 0, bytes, Charsets.UTF_8)
                        Log.d(TAG, "Received from PC: $message")
                        handlePcMessage(message)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading from PC", e)
                    break
                }
            }
        }
    }

    /**
     * Traite les messages reçus du PC
     */
    private fun handlePcMessage(message: String) {
        when {
            message.contains("OK") -> {
                _lastUnlockResult.value = UnlockResult.Success
            }
            message.contains("ERROR") -> {
                _lastUnlockResult.value = UnlockResult.Failed(message)
            }
            message.contains("UNLOCKED") -> {
                _lastUnlockResult.value = UnlockResult.Success
            }
        }
    }

    /**
     * Déverrouille le PC automatiquement après scan NFC
     */
    suspend fun unlockPcWithNfc(device: PcDevice, nfcUid: String): Result<Unit> {
        if (!device.isEnabled) {
            return Result.failure(Exception("Device is disabled"))
        }

        if (device.requireNfcScan && nfcUid.isEmpty()) {
            _lastUnlockResult.value = UnlockResult.NoNfcScanned
            return Result.failure(Exception("NFC scan required"))
        }

        if (!isConnected()) {
            // Tenter de se connecter
            connectToPc(device).getOrElse {
                return Result.failure(it)
            }
        }

        // Envoyer la commande de déverrouillage
        val command = if (device.password.isNotEmpty()) {
            UnlockCommand.TypePassword(device.password)
        } else {
            UnlockCommand.WakeUp
        }

        return sendUnlockCommand(command)
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}
