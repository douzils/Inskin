// tags/nfc/IsoDepInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.tech.IsoDep
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex
import kotlin.math.min
import com.inskin.app.tags.Channel

object IsoDepInspector : TagInspector {
    override fun supports(channel: Channel) =
        channel is Channel.Android && IsoDep.get(channel.tag) != null

    override fun readRaw(channel: Channel): ByteArray? {
        val d = IsoDep.get((channel as Channel.Android).tag) ?: return null
        // On retourne l’ATS/historical si dispo
        val ats = d.historicalBytes ?: d.hiLayerResponse
        return ats
    }

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val d = IsoDep.get(tag)
        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()
        val hist = d?.historicalBytes?.toHex()
        val hlr = d?.hiLayerResponse?.toHex()

        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            chipType = "Type 4 (ISO-DEP)",
            atsHex = hist,
            historicalBytesHex = hist,
            hiLayerResponseHex = hlr,
            rawReadableBytes = raw?.size,
            rawDumpFirstBytesHex = raw?.copyOfRange(0, min(32, raw.size))?.toHex()
        )
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport {
        val d = IsoDep.get((channel as Channel.Android).tag) ?: return InspectorUtils.fail("IsoDep requis")
        return runCatching {
            d.connect()
            ops.filterIsInstance<WriteOp.IsoDepApdu>().forEach { apdu -> d.transceive(apdu.apdu) }
            d.close()
            InspectorUtils.ok("APDU envoyées")
        }.getOrElse { InspectorUtils.fail(it.message ?: "échec IsoDep") }
    }
}
