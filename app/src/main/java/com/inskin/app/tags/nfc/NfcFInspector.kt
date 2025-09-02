// tags/nfc/NfcFInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.tech.NfcF
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex


object NfcFInspector : TagInspector {
    override fun supports(channel: Channel) =
        channel is Channel.Android && NfcF.get(channel.tag) != null

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val f = NfcF.get(tag)
        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()
        val sys = f?.systemCode?.let { if (it.size >= 2) byteArrayOf(it[0], it[1]).toHex() else null }
        val man = f?.manufacturer?.toHex()
        val rf = listOfNotNull(sys?.let { "Sys:$it" }, man?.let { "Man:$it" }).joinToString(" • ").ifBlank { null }
        return TagDetails(uidHex = uidHex, techList = techs, chipType = "FeliCa", rfConfig = rf)
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport =
        InspectorUtils.fail("Écriture FeliCa non implémentée")
}
