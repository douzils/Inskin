// tags/rfid/RfidUhfInspector.kt
package com.inskin.app.tags.rfid

import com.inskin.app.TagDetails
import com.inskin.app.tags.*

object RfidUhfInspector : TagInspector {
    override fun supports(channel: Channel) = channel is Channel.Proxmark

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val uid = (channel as Channel.Proxmark).uidHex
        return TagDetails(uidHex = uid, techList = listOf("UHF"), chipType = "EPC Gen2")
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture UHF à implémenter")
}
