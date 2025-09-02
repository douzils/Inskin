package com.inskin.app.tags
import com.inskin.app.tags.WriteReport
import android.nfc.tech.NfcA

object InspectorUtils {
    var emitLog: ((String) -> Unit)? = null
    var extraKeysProvider: (() -> List<ByteArray>)? = null
    fun extraKeysOrEmpty(): List<ByteArray> = extraKeysProvider?.invoke().orEmpty()
    var onKeyLearned: ((uidHex: String, sector: Int, type: String, keyHex: String) -> Unit)? = null

    fun ok(msg: String? = null): WriteReport = WriteReport(true, msg)
    fun fail(msg: String? = null): WriteReport = WriteReport(false, msg)
    // Helper sûr pour NfcA: connect → block(this) → close
    fun <T> withNfcA(a: NfcA, block: NfcA.() -> T?): T? =
        try {
            a.connect()
            val r = a.block()
            a.close()
            r
        } catch (_: Exception) {
            try { a.close() } catch (_: Exception) {}
            null
        }

    // Déduction simple du fabricant à partir du premier octet d’UID (Type 2/Ultralight)
    fun guessManufacturer(uid: ByteArray?): String? {
        val b = uid?.getOrNull(0)?.toInt()?.and(0xFF) ?: return null
        return when (b) {
            0x04, 0x07 -> "NXP"
            0x02, 0x44 -> "STMicroelectronics"
            0x28       -> "Infineon"
            0x08       -> "Inside Secure"
            0x03       -> "Hitachi"
            0x20       -> "Samsung"
            0x1F       -> "Sony"
            else       -> null
        }
    }

}
