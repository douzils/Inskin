package com.inskin.app

import android.app.Application
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.lifecycle.AndroidViewModel
import com.inskin.app.model.WriteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * ViewModel keeping track of the last NFC tag discovered and providing helpers
 * to write NDEF messages.
 */
class NfcViewModel(application: Application) : AndroidViewModel(application) {
    private var lastTag: Tag? = null

    private val _tagInfo = MutableStateFlow<TagInfo?>(null)
    val tagInfo = _tagInfo.asStateFlow()

    fun updateFromTag(tag: Tag?) {
        if (tag == null) {
            _tagInfo.value = null
            return
        }

        lastTag = tag
        val uid = tag.id?.joinToString(separator = "") { b -> "%02X".format(b) }
        val techs = tag.techList?.toList().orEmpty()
        val ndef = Ndef.get(tag)
        val type = ndef?.type
        val size = ndef?.maxSize ?: 0
        val used = ndef?.cachedNdefMessage?.toByteArray()?.size ?: 0
        val writable = ndef?.isWritable == true
        val readonly = ndef?.isWritable == false
        val records = ndef?.cachedNdefMessage?.records?.map { record ->
            when {
                record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                    val payload = record.payload
                    val languageLength = payload[0].toInt() and 63
                    val text = payload.copyOfRange(1 + languageLength, payload.size)
                    text.toString(Charsets.UTF_8)
                }
                record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) -> {
                    record.toUri()?.toString() ?: ""
                }
                else -> ""
            }
        } ?: emptyList()
        _tagInfo.value = TagInfo(type, techs, uid, null, null, size, used, writable, readonly, records)
    }

    /** Build an [NdefMessage] from a list of [WriteItem]. */
    fun buildNdefMessage(items: List<WriteItem>): NdefMessage {
        val records = items.map { item ->
            when (item) {
                is WriteItem.Text -> NdefRecord.createTextRecord("", item.text)
                is WriteItem.Url -> NdefRecord.createUri(item.url)
                is WriteItem.UriItem -> NdefRecord.createUri(item.uri)
                is WriteItem.Phone -> NdefRecord.createUri("tel:${item.number}")
                is WriteItem.Sms -> {
                    val body = item.body?.let { "?body=" + Uri.encode(it) } ?: ""
                    NdefRecord.createUri("sms:${item.number}${body}")
                }
                is WriteItem.Mail -> {
                    val params = buildList {
                        item.subject?.let { add("subject=${Uri.encode(it)}") }
                        item.body?.let { add("body=${Uri.encode(it)}") }
                    }
                    val uri = buildString {
                        append("mailto:${item.to}")
                        if (params.isNotEmpty()) append("?").append(params.joinToString("&"))
                    }
                    NdefRecord.createUri(uri)
                }
                is WriteItem.Wifi -> {
                    val password = item.password ?: ""
                    NdefRecord.createUri("WIFI:S:${item.ssid};T:${item.security};P:${password};;")
                }
                is WriteItem.Bluetooth -> NdefRecord.createUri("BT:${item.mac}")
                is WriteItem.Contact -> {
                    val vcard = buildString {
                        appendLine("BEGIN:VCARD")
                        appendLine("VERSION:3.0")
                        appendLine("FN:${item.name}")
                        item.phone?.let { appendLine("TEL:$it") }
                        item.email?.let { appendLine("EMAIL:$it") }
                        appendLine("END:VCARD")
                    }
                    NdefRecord.createMime("text/vcard", vcard.toByteArray())
                }
                is WriteItem.Location -> NdefRecord.createUri("geo:${item.lat},${item.lon}")
                is WriteItem.Crypto -> {
                    val scheme = when (item.network?.lowercase()) {
                        "eth", "ethereum" -> "ethereum"
                        "btc", "bitcoin" -> "bitcoin"
                        else -> null
                    }
                    val uri = if (scheme != null) "$scheme:${item.address}" else item.address
                    NdefRecord.createUri(uri)
                }
                is WriteItem.KeyValue -> {
                    val json = Json.encodeToString(mapOf(item.key to item.value))
                    NdefRecord.createMime("application/x-inskin", json.toByteArray())
                }
            }
        }
        return NdefMessage(records.toTypedArray())
    }

    /** Write a list of items to the last seen tag. */
    fun writeItems(items: List<WriteItem>): Result<Unit> = runCatching {
        val tag = lastTag ?: error("No tag")
        val ndef = Ndef.get(tag) ?: error("Tag is not NDEF")
        ndef.connect()
        val message = buildNdefMessage(items)
        ensureCapacity(ndef, message)
        ndef.writeNdefMessage(message)
        ndef.close()
    }

    private fun ensureCapacity(ndef: Ndef, message: NdefMessage) {
        val bytes = message.toByteArray()
        if (ndef.maxSize < bytes.size) {
            ndef.close()
            throw IOException("Tag capacity ${'$'}{ndef.maxSize} < ${'$'}{bytes.size}")
        }
        if (!ndef.isWritable) {
            ndef.close()
            throw IOException("Tag is read-only")
        }
    }
}
