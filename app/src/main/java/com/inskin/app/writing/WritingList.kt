package com.inskin.app.writing

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Représente une liste d'écriture NFC
 */
data class WritingList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val actions: List<NFCAction>,
    val icon: String = "edit",
    val color: Color = Color(0xFF00D9FF),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val useCount: Int = 0,
    val isFavorite: Boolean = false
)

/**
 * Action NFC à écrire
 */
sealed class NFCAction {
    abstract val id: String
    abstract val name: String
    abstract val enabled: Boolean

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Texte",
        override val enabled: Boolean = true,
        val content: String
    ) : NFCAction()

    data class URL(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "URL",
        override val enabled: Boolean = true,
        val url: String
    ) : NFCAction()

    data class WiFi(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "WiFi",
        override val enabled: Boolean = true,
        val ssid: String,
        val password: String,
        val encryption: String = "WPA2"
    ) : NFCAction()

    data class VolumeControl(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Volume",
        override val enabled: Boolean = true,
        val level: Int, // 0-100
        val type: VolumeType = VolumeType.MEDIA
    ) : NFCAction()

    data class LaunchApp(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Lancer App",
        override val enabled: Boolean = true,
        val packageName: String,
        val appName: String
    ) : NFCAction()

    data class PhoneCall(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Appel",
        override val enabled: Boolean = true,
        val phoneNumber: String
    ) : NFCAction()

    data class SMS(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "SMS",
        override val enabled: Boolean = true,
        val phoneNumber: String,
        val message: String
    ) : NFCAction()

    data class Email(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Email",
        override val enabled: Boolean = true,
        val to: String,
        val subject: String = "",
        val body: String = ""
    ) : NFCAction()

    data class Bluetooth(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Bluetooth",
        override val enabled: Boolean = true,
        val action: BluetoothAction = BluetoothAction.TOGGLE,
        val deviceAddress: String? = null
    ) : NFCAction()

    data class Custom(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Personnalisé",
        override val enabled: Boolean = true,
        val data: ByteArray
    ) : NFCAction() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Custom
            if (id != other.id) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}

enum class VolumeType {
    MEDIA, RINGTONE, NOTIFICATION, ALARM
}

enum class BluetoothAction {
    TOGGLE, ENABLE, DISABLE, CONNECT
}

/**
 * Presets prédéfinis
 */
object WritingPresets {
    val volumeMute = WritingList(
        name = "Réduire Volume",
        description = "Met le volume média à 0%",
        actions = listOf(
            NFCAction.VolumeControl(
                level = 0,
                type = VolumeType.MEDIA
            )
        ),
        icon = "volume_off",
        color = Color(0xFFFF0080)
    )

    val volumeMax = WritingList(
        name = "Volume Max",
        description = "Met le volume média à 100%",
        actions = listOf(
            NFCAction.VolumeControl(
                level = 100,
                type = VolumeType.MEDIA
            )
        ),
        icon = "volume_up",
        color = Color(0xFF00FF85)
    )

    val silentMode = WritingList(
        name = "Mode Silencieux",
        description = "Met tous les volumes à 0",
        actions = listOf(
            NFCAction.VolumeControl(level = 0, type = VolumeType.MEDIA),
            NFCAction.VolumeControl(level = 0, type = VolumeType.RINGTONE),
            NFCAction.VolumeControl(level = 0, type = VolumeType.NOTIFICATION)
        ),
        icon = "notifications_off",
        color = Color(0xFFB026FF)
    )

    val homeWiFi = WritingList(
        name = "WiFi Maison",
        description = "Se connecte au WiFi de la maison",
        actions = listOf(
            NFCAction.WiFi(
                ssid = "MonWiFi",
                password = "",
                encryption = "WPA2"
            )
        ),
        icon = "wifi",
        color = Color(0xFF00D9FF)
    )

    val callEmergency = WritingList(
        name = "Appel d'Urgence",
        description = "Appelle un numéro d'urgence",
        actions = listOf(
            NFCAction.PhoneCall(phoneNumber = "112")
        ),
        icon = "emergency",
        color = Color(0xFFFF0040)
    )

    val businessCard = WritingList(
        name = "Carte de Visite",
        description = "Partage vos informations de contact",
        actions = listOf(
            NFCAction.Text(content = "John Doe\n+33 6 12 34 56 78\njohn@example.com")
        ),
        icon = "badge",
        color = Color(0xFFFFD700)
    )

    fun getAllPresets(): List<WritingList> = listOf(
        volumeMute,
        volumeMax,
        silentMode,
        homeWiFi,
        callEmergency,
        businessCard
    )
}
