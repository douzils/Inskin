// tags/rfid/RfidAnimalInspector.kt
package com.inskin.app.tags.rfid

import com.inskin.app.TagDetails
import com.inskin.app.tags.*

object RfidAnimalInspector : TagInspector {
    override fun supports(channel: Channel) = channel is Channel.Proxmark

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val uid = (channel as Channel.Proxmark).uidHex
        return TagDetails(uidHex = uid, techList = listOf("LF"), chipType = "ISO 11784/11785")
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture transpondeur animal non permise")
}
