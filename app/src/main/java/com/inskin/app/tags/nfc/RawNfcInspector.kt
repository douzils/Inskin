// tags/nfc/RawNfcInspector.kt
package com.inskin.app.tags.nfc

import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex

/** Fallback minimal pour tout tag Android non géré par un inspecteur dédié. */
object RawNfcInspector : TagInspector {
    override fun supports(channel: Channel) = channel is Channel.Android

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()

        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            chipType = "NFC Tag"
        )
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture RAW générique non supportée")
}
