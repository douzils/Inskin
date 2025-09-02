// tags/nfc/NfcBInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.tech.NfcB
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex

object NfcBInspector : TagInspector {
    override fun supports(channel: Channel) =
        channel is Channel.Android && NfcB.get(channel.tag) != null

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val b = NfcB.get(tag)
        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()
        val app = b?.applicationData?.toHex()
        val proto = b?.protocolInfo?.toHex()
        val rf = listOfNotNull(app?.let { "App:$it" }, proto?.let { "Proto:$it" }).joinToString(" • ").ifBlank { null }
        return TagDetails(uidHex = uidHex, techList = techs, chipType = "NfcB", rfConfig = rf)
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture NfcB non standard")
}
