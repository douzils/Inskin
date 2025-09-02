// tags/rfid/RawRfidInspector.kt
package com.inskin.app.tags.rfid

import com.inskin.app.TagDetails
import com.inskin.app.tags.*

object RawRfidInspector : TagInspector {
    override fun supports(channel: Channel) = channel is Channel.Proxmark

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails =
        TagDetails(uidHex = (channel as Channel.Proxmark).uidHex, techList = listOf("RFID"))

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Pas d’écriture RAW générique")
}
