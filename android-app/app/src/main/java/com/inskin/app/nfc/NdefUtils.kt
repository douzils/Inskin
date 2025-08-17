package com.inskin.app.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset
import java.util.Locale

/** Utility helpers for building and parsing NDEF records. */
object NdefUtils {
    data class ParsedRecord(val type: String, val payload: String)

    fun textRecord(text: String, locale: Locale = Locale.getDefault()): NdefRecord {
        val langBytes = locale.language.encodeToByteArray()
        val textBytes = text.encodeToByteArray()
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
    }

    fun uriRecord(uri: String): NdefRecord = NdefRecord.createUri(uri)

    fun telRecord(number: String): NdefRecord = uriRecord("tel:$number")

    fun smsRecord(number: String, body: String? = null): NdefRecord {
        val uri = if (body.isNullOrEmpty()) "sms:$number" else "sms:$number?body=" + Uri.encode(body)
        return uriRecord(uri)
    }

    fun mailRecord(address: String, subject: String? = null, body: String? = null): NdefRecord {
        val params = mutableListOf<String>()
        if (!subject.isNullOrEmpty()) params += "subject=" + Uri.encode(subject)
        if (!body.isNullOrEmpty()) params += "body=" + Uri.encode(body)
        val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return uriRecord("mailto:$address$query")
    }

    fun geoRecord(lat: Double, lon: Double): NdefRecord = uriRecord("geo:$lat,$lon")

    fun vcardRecord(vcard: String): NdefRecord =
        NdefRecord.createMime("text/vcard", vcard.toByteArray(Charset.forName("UTF-8")))

    fun rawRecord(payload: ByteArray, tnf: Short, type: ByteArray, id: ByteArray? = null): NdefRecord =
        NdefRecord(tnf, type, id ?: ByteArray(0), payload)

    fun parse(record: NdefRecord): ParsedRecord {
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            val status = record.payload[0].toInt()
            val langLength = status and 0x3F
            val text = record.payload.copyOfRange(1 + langLength, record.payload.size).toString(Charset.forName("UTF-8"))
            return ParsedRecord("TEXT", text)
        }
        record.toUri()?.let { uri ->
            val value = uri.toString()
            val type = when {
                value.startsWith("tel:") -> "TEL"
                value.startsWith("sms:") -> "SMS"
                value.startsWith("mailto:") -> "MAILTO"
                value.startsWith("geo:") -> "GEO"
                else -> "URI"
            }
            val payload = when (type) {
                "TEL" -> value.removePrefix("tel:")
                "SMS" -> value.removePrefix("sms:")
                "MAILTO" -> value.removePrefix("mailto:")
                "GEO" -> value.removePrefix("geo:")
                else -> value
            }
            return ParsedRecord(type, payload)
        }
        if (record.tnf == NdefRecord.TNF_MIME_MEDIA && String(record.type) == "text/vcard") {
            return ParsedRecord("VCARD", record.payload.toString(Charset.forName("UTF-8")))
        }
        val hex = record.payload.joinToString(separator = "") { String.format("%02X", it) }
        return ParsedRecord("RAW", hex)
    }

    fun estimateSize(records: List<NdefRecord>): Int =
        NdefMessage(records.toTypedArray()).toByteArray().size
}
