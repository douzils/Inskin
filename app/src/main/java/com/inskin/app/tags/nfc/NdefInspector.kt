// tags/nfc/NdefInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.inskin.app.NdefRecordInfo
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
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
        val recs = msg?.records?.map { r -> toInfo(r) } ?: emptyList()
        val used = raw?.size
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
            if (!ndef.isWritable) throw IllegalStateException("Tag non inscriptible")
            if (msg.toByteArray().size > ndef.maxSize) throw IllegalStateException("Taille NDEF > capacité")
            ndef.writeNdefMessage(msg)
            ndef.close()
            InspectorUtils.ok("NDEF écrit")
        }.getOrElse { InspectorUtils.fail(it.message ?: "échec écriture NDEF") }
    }

    private fun toInfo(r: NdefRecord): NdefRecordInfo {
        return when {
            r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(byteArrayOf(0x54)) -> {
                val p = r.payload
                if (p.isNotEmpty()) {
                    val langLen = p[0].toInt() and 0x1F
                    val cs = if ((p[0].toInt() and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
                    val text = String(p, 1 + langLen, p.size - (1 + langLen), cs)
                    NdefRecordInfo("Text", text)
                } else NdefRecordInfo("Text", "")
            }
            r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(byteArrayOf(0x55)) -> {
                val p = r.payload
                val uri = if (p.isNotEmpty()) {
                    val id = p[0].toInt() and 0xFF
                    val prefix = URI_PREFIX.getOrElse(id) { "" }
                    prefix + String(p, 1, p.size - 1, Charsets.UTF_8)
                } else ""
                NdefRecordInfo("URI", uri)
            }
            r.tnf == NdefRecord.TNF_MIME_MEDIA -> {
                val mime = runCatching { String(r.type, Charsets.US_ASCII) }.getOrNull() ?: "MIME"
                val v = runCatching { String(r.payload, Charsets.UTF_8) }.getOrNull() ?: r.payload.toHex(64)
                NdefRecordInfo(mime, v)
            }
            r.tnf == NdefRecord.TNF_EXTERNAL_TYPE -> {
                val t = runCatching { String(r.type, Charsets.US_ASCII) }.getOrNull() ?: "ext"
                NdefRecordInfo(t, r.payload.toHex(64))
            }
            else -> NdefRecordInfo("NDEF", r.payload.toHex(64))
        }
    }

    private val URI_PREFIX = arrayOf(
        "", "http://www.", "https://www.", "http://", "https://",
        "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://",
        "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:",
        "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:",
        "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
        "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:",
        "urn:nfc:"
    )
}
