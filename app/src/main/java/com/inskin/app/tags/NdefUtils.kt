package com.inskin.app.tags

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.inskin.app.NdefRecordInfo
import com.inskin.app.toHex

fun NdefMessage.toRecordInfos(): List<NdefRecordInfo> =
    records.map { it.toInfo() }

fun NdefRecord.toInfo(): NdefRecordInfo {
    val typeStr = try { type?.toString(Charsets.UTF_8) } catch (_: Exception) { null }
    val idStr = id?.toHex()
    val payloadHex = payload?.toHex()

    var text: String? = null
    var lang: String? = null
    var uri: String? = null
    var mime: String? = null

    when (tnf) {
        NdefRecord.TNF_WELL_KNOWN -> {
            if (NdefRecord.RTD_TEXT.contentEquals(type) && payload != null && payload.isNotEmpty()) {
                val langLen = payload[0].toInt() and 0x3F
                if (payload.size > 1 + langLen) {
                    lang = payload.copyOfRange(1, 1 + langLen).toString(Charsets.UTF_8)
                    text = payload.copyOfRange(1 + langLen, payload.size).toString(Charsets.UTF_8)
                }
            }
            if (NdefRecord.RTD_URI.contentEquals(type) && payload != null && payload.isNotEmpty()) {
                val idx = payload[0].toInt() and 0xFF
                val prefix = if (idx in uriPrefixes.indices) uriPrefixes[idx] else ""
                uri = prefix + payload.copyOfRange(1, payload.size).toString(Charsets.UTF_8)
            }
        }
        NdefRecord.TNF_MIME_MEDIA -> {
            mime = typeStr
            text = payload?.toString(Charsets.UTF_8)
        }
    }

    return NdefRecordInfo(
        tnf = this.tnf.toInt(),
        type = typeStr,
        payloadHex = payloadHex,
        idHex = idStr,
        text = text,
        lang = lang,
        uri = uri,
        mimeType = mime,
        sizeBytes = payload?.size,
        value = text ?: uri ?: payloadHex
    )
}

private val uriPrefixes = arrayOf(
    "", "http://www.", "https://www.", "http://", "https://",
    "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
    "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
    "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
    "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
    "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
    "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:",
    "urn:nfc:"
)
