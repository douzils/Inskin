// tags/nfc/NfcVInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.tech.NfcV
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex
import kotlin.math.min

object NfcVInspector : TagInspector {
    override fun supports(channel: Channel) =
        channel is Channel.Android && NfcV.get(channel.tag) != null

    override fun readRaw(channel: Channel): ByteArray? {
        val tag = (channel as Channel.Android).tag
        val v = NfcV.get(tag) ?: return null
        return runCatching {
            v.connect()
            val uidLe = (tag.id ?: ByteArray(0)).reversedArray()
            val resp = v.transceive(byteArrayOf(0x22, 0x2B) + uidLe) // Get System Info
            v.close()
            resp
        }.getOrNull()
    }

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()
        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            chipType = "ISO15693",
            rfConfig = raw?.copyOfRange(0, min(32, raw.size))?.toHex(),
            rawReadableBytes = raw?.size,
            rawDumpFirstBytesHex = raw?.copyOfRange(0, min(32, raw.size))?.toHex()
        )
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture NfcV dépend du produit")
}
