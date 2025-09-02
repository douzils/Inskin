// tags/nfc/NfcAInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex
import kotlin.math.min

object NfcAInspector : TagInspector {
    override fun supports(channel: Channel) =
        channel is Channel.Android &&
                NfcA.get(channel.tag) != null &&
                MifareClassic.get(channel.tag) == null // éviter Classic ici

    override fun readRaw(channel: Channel): ByteArray? {
        val tag = (channel as Channel.Android).tag
        val a = NfcA.get(tag) ?: return null
        return runCatching {
            a.connect()
            val ver = a.transceive(byteArrayOf(0x60))              // GET_VERSION
            val p0 = a.transceive(byteArrayOf(0x30, 0x00))        // READ 0..3
            val p4 = a.transceive(byteArrayOf(0x30, 0x04))        // READ 4..7
            a.close()
            ver + p0 + p4
        }.getOrNull()
    }

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val uid = tag.id ?: ByteArray(0)
        val uidHex = uid.toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()

        var totalBytes: Int? = null
        var versionHex: String? = null
        var memoryLayout: String? = null
        var lockBitsHex: String? = null
        var otpHex: String? = null

        if (raw != null) {
            if (raw.size >= 8) {
                versionHex = raw.copyOfRange(0, 8).toHex()
                val blocks = (raw[6].toInt() and 0xFF) + 1
                if (blocks in 16..4096) totalBytes = blocks * 4
            }
            if (raw.size >= 24) {
                val head = raw.copyOfRange(8, 24)                 // pages 0..7
                lockBitsHex = if (head.size >= 12) byteArrayOf(head[10], head[11]).toHex() else null
                val page3 = head.copyOfRange(12, 16)
                otpHex = if ((page3[0].toInt() and 0xFF) != 0xE1) page3.toHex() else null
                if (totalBytes == null) totalBytes = (head.size / 4) * 4
            }
            totalBytes?.let { memoryLayout = "${it/4} pages × 4B ($it B)" }
        }

        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            manufacturer = InspectorUtils.guessManufacturer(uid),
            chipType = "Type 2",
            totalMemoryBytes = totalBytes,
            memoryLayout = memoryLayout,
            versionHex = versionHex,
            lockBitsHex = lockBitsHex,
            otpBytesHex = otpHex,
            rawReadableBytes = raw?.size,
            rawDumpFirstBytesHex = raw?.copyOfRange(0, min(32, raw.size))?.toHex()
        )
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport {
        val tag = (channel as Channel.Android).tag
        val a = NfcA.get(tag) ?: return InspectorUtils.fail("NfcA absent")
        return runCatching {
            a.connect()
            ops.forEach { op ->
                when (op) {
                    is WriteOp.RawPage -> {
                        require(op.data.size == 4) { "Page = 4 octets" }
                        a.transceive(byteArrayOf(0xA2.toByte(), op.page.toByte()) + op.data)
                    }
                    else -> {} // NDEF → géré par NdefInspector
                }
            }
            a.close()
            InspectorUtils.ok("Écriture NfcA OK")
        }.getOrElse { InspectorUtils.fail(it.message ?: "échec NfcA") }
    }
}
