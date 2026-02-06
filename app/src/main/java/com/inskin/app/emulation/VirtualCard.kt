package com.inskin.app.emulation

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Représente une carte virtuelle pouvant être émulée
 */
data class VirtualCard(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uid: String,
    val type: CardType,
    val data: ByteArray,
    val color: Color,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val useCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualCard

        if (id != other.id) return false
        if (name != other.name) return false
        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + uid.hashCode()
        return result
    }
}

/**
 * Types de cartes supportés
 */
enum class CardType(val displayName: String, val color: Color) {
    NTAG213("NTAG213", Color(0xFF4CAF50)),
    NTAG215("NTAG215", Color(0xFF2196F3)),
    NTAG216("NTAG216", Color(0xFF9C27B0)),
    MIFARE_CLASSIC_1K("Mifare Classic 1K", Color(0xFFFF9800)),
    MIFARE_CLASSIC_4K("Mifare Classic 4K", Color(0xFFFF5722)),
    MIFARE_ULTRALIGHT("Mifare Ultralight", Color(0xFF00BCD4)),
    CUSTOM("Personnalisée", Color(0xFF607D8B))
}

/**
 * État de l'émulation
 */
sealed class EmulationState {
    object Idle : EmulationState()
    data class Active(val card: VirtualCard) : EmulationState()
    data class Error(val message: String) : EmulationState()
}
