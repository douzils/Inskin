package com.inskin.app.tags.nfc

import android.nfc.NdefMessage
import android.nfc.tech.Ndef
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.tags.toRecordInfos
import com.inskin.app.toHex
import kotlin.math.min

object NdefInspector : TagInspector {

    override fun supports(channel: Channel) =
        channel is Channel.Android && Ndef.get(channel.tag) != null

    override fun readRaw(channel: Channel): ByteArray? {
        val tag = (channel as Channel.Android).tag
        val ndef = Ndef.get(tag) ?: return null
        return runCatching {
            ndef.connect()
            val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage
            val raw = msg?.toByteArray()
            ndef.close()
            raw
        }.getOrNull()
    }

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val ndef = Ndef.get(tag)

        val msg = runCatching { ndef?.cachedNdefMessage ?: ndef?.ndefMessage }.getOrNull()
        val recs = msg?.toRecordInfos() ?: emptyList()
        val used = msg?.toByteArray()?.size ?: raw?.size
        val cap = runCatching { ndef?.maxSize }.getOrNull()
        val writable = runCatching { ndef?.isWritable }.getOrNull()

        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()

        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            chipType = "NDEF",
            ndefCapacity = cap,
            usedBytes = used,
            isNdefWritable = writable,
            ndefRecords = recs,
            rawReadableBytes = raw?.size,
            rawDumpFirstBytesHex = raw?.copyOfRange(0, min(32, raw.size))?.toHex()
        )
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport {
        val tag = (channel as Channel.Android).tag
        val ndef = Ndef.get(tag) ?: return InspectorUtils.fail("NDEF absent")
        val toWrite = ops.filterIsInstance<WriteOp.Ndef>().firstOrNull()
            ?: return InspectorUtils.fail("Aucun WriteOp.Ndef")
        val msg = NdefMessage(toWrite.records.toTypedArray())
        return runCatching {
            ndef.connect()
            if (!ndef.isWritable) error("Tag non inscriptible")
            if (msg.toByteArray().size > ndef.maxSize) error("Taille NDEF > capacité")
            ndef.writeNdefMessage(msg)
            ndef.close()
            InspectorUtils.ok("NDEF écrit")
        }.getOrElse { InspectorUtils.fail(it.message) }
    }
}
