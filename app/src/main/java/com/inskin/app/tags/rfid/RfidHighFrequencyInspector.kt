// tags/rfid/RfidHighFrequencyInspector.kt
package com.inskin.app.tags.rfid

import com.inskin.app.TagDetails
import com.inskin.app.tags.*

object RfidHighFrequencyInspector : TagInspector {
    override fun supports(channel: Channel) = channel is Channel.Proxmark

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val uid = (channel as Channel.Proxmark).uidHex
        return TagDetails(uidHex = uid, techList = listOf("HF"), chipType = "HF 13.56MHz")
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture HF via Proxmark à implémenter")
}
