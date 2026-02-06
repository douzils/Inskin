package com.inskin.app.pcunlock

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Appareil PC enregistré pour le déverrouillage
 */
data class PcDevice(
    val id: String,
    val name: String,
    val bluetoothAddress: String,
    val bluetoothName: String,
    val password: String = "", // Mot de passe Windows à envoyer
    val color: Color = Color(0xFF00D9FF),
    val isEnabled: Boolean = true,
    val requireNfcScan: Boolean = true, // Nécessite scan NFC pour débloquer
    val autoConnect: Boolean = false,
    val createdAt: Long,
    val lastUnlocked: Long? = null,
    val unlockCount: Int = 0
)

/**
 * Commandes envoyées au PC via Bluetooth
 */
sealed class UnlockCommand {
    data class Unlock(val password: String) : UnlockCommand()
    data class TypePassword(val password: String) : UnlockCommand()
    data object WakeUp : UnlockCommand()
    data object Lock : UnlockCommand()
    data class Custom(val command: String) : UnlockCommand()

    fun toBytes(): ByteArray {
        val prefix = when (this) {
            is Unlock -> "UNLOCK:"
            is TypePassword -> "TYPE:"
            is WakeUp -> "WAKE"
            is Lock -> "LOCK"
            is Custom -> "CUSTOM:"
        }

        val payload = when (this) {
            is Unlock -> password
            is TypePassword -> password
            is WakeUp -> ""
            is Lock -> ""
            is Custom -> command
        }

        return "$prefix$payload\n".toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromBytes(data: ByteArray): UnlockCommand? {
            val str = String(data, Charsets.UTF_8).trim()
            return when {
                str.startsWith("UNLOCK:") -> Unlock(str.removePrefix("UNLOCK:"))
                str.startsWith("TYPE:") -> TypePassword(str.removePrefix("TYPE:"))
                str == "WAKE" -> WakeUp
                str == "LOCK" -> Lock
                str.startsWith("CUSTOM:") -> Custom(str.removePrefix("CUSTOM:"))
                else -> null
            }
        }
    }
}

/**
 * État de la connexion Bluetooth
 */
sealed class BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState()
    data object Connecting : BluetoothConnectionState()
    data class Connected(val deviceName: String) : BluetoothConnectionState()
    data class Error(val message: String) : BluetoothConnectionState()
}

/**
 * Résultat d'une tentative de déverrouillage
 */
sealed class UnlockResult {
    data object Success : UnlockResult()
    data class Failed(val reason: String) : UnlockResult()
    data object NoPcConnected : UnlockResult()
    data object NoNfcScanned : UnlockResult()
}
