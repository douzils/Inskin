package com.inskin.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset
import java.util.Locale

object NdefUtils {
  data class NdefParsed(val type: String, val value: String)
  data class NdefBuild(val records: List<NdefRecord>)

  fun parse(msg: NdefMessage?): List<NdefParsed> {
    if (msg == null) return emptyList()
    return msg.records.mapNotNull { rec ->
      when {
        rec.tnf == NdefRecord.TNF_WELL_KNOWN && rec.type.contentEquals(NdefRecord.RTD_TEXT) ->
          NdefParsed("text", decodeText(rec))
        rec.tnf == NdefRecord.TNF_WELL_KNOWN && rec.type.contentEquals(NdefRecord.RTD_URI) ->
          NdefParsed("uri", decodeUri(rec))
        else -> NdefParsed("raw", rec.payload.joinToString("") { "%02X".format(it) })
      }
    }
  }

  fun build(build: NdefBuild): NdefMessage = NdefMessage(build.records.toTypedArray())

  fun textRecord(text: String, locale: Locale = Locale.getDefault()): NdefRecord {
    val langBytes = locale.language.toByteArray(Charsets.US_ASCII)
    val textBytes = text.toByteArray(Charsets.UTF_8)
    val payload = ByteArray(1 + langBytes.size + textBytes.size)
    payload[0] = langBytes.size.toByte()
    System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
    System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
    return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
  }

  fun uriRecord(uri: String): NdefRecord {
    val prefixMap = listOf(
      "", "http://www.", "https://www.", "http://", "https://"
    )
    val match = prefixMap.withIndex().maxByOrNull { if (uri.startsWith(it.value)) it.value.length else -1 }!!
    val prefixCode = match.index.toByte()
    val rest = uri.removePrefix(match.value).toByteArray(Charset.forName("UTF-8"))
    val payload = ByteArray(1 + rest.size)
    payload[0] = prefixCode
    System.arraycopy(rest, 0, payload, 1, rest.size)
    return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, ByteArray(0), payload)
  }

  private fun decodeText(record: NdefRecord): String {
    val payload = record.payload
    val langLen = payload[0].toInt() and 0x3F
    val textBytes = payload.copyOfRange(1 + langLen, payload.size)
    return String(textBytes, Charsets.UTF_8)
  }

  private fun decodeUri(record: NdefRecord): String {
    val prefixMap = arrayOf(
      "", "http://www.", "https://www.", "http://", "https://"
    )
    val payload = record.payload
    val prefix = prefixMap.getOrElse(payload[0].toInt()) { "" }
    val rest = String(payload.copyOfRange(1, payload.size), Charsets.UTF_8)
    return prefix + rest
  }
}
