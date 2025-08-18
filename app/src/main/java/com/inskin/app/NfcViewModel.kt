package com.inskin.app

import android.app.Application
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class NfcViewModel(application: Application) : AndroidViewModel(application) {
    private var lastTag: Tag? = null

    private val _tagInfo = MutableStateFlow<TagInfo?>(null)
    val tagInfo = _tagInfo.asStateFlow()

    fun updateFromTag(tag: Tag) {
        lastTag = tag

        // Techs/UID
        val techs = tag.techList?.toList().orEmpty()
        val uid = tag.id?.joinToString("") { b -> "%02X".format(b) }

        // ATQA/SAK si NfcA dispo
        var atqaHex: String? = null
        var sakHex: String? = null
        NfcA.get(tag)?.let { nfcA ->
            try {
                atqaHex = nfcA.atqa.joinToString("") { "%02X".format(it) }
                sakHex = "%02X".format(nfcA.sak)
            } catch (_: Throwable) { }
        }

        // NDEF / capacité / utilisé
        var type: String? = null
        var size = 0
        var used = 0
        var writable = false
        var readonly = false

        Ndef.get(tag)?.let { ndef ->
            type = ndef.type
            try {
                ndef.connect()
                size = ndef.maxSize
                used = ndef.cachedNdefMessage?.toByteArray()?.size ?: 0
                writable = ndef.isWritable
                readonly = !ndef.isWritable
            } catch (_: Throwable) {
            } finally {
                try { ndef.close() } catch (_: Throwable) {}
            }
        } ?: run {
            // Pas d’NDEF → tenter de déduire la capacité d’un Type 2 (Ultralight/NTAG)
            MifareUltralight.get(tag)?.let { mu ->
                type = "Type 2 (Mifare Ultralight/NTAG)"
                try {
                    mu.connect()
                    // CC à la page 3 : [E1, 10, size(×8B), 00]
                    val cc = mu.readPages(3) // 16 octets (pages 3..6)
                    val capField = cc[2].toInt() and 0xFF
                    size = capField * 8

                    // Lire TLV (début data page 4) pour estimer la longueur NDEF
                    val block = mu.readPages(4)
                    val tlv = block[0].toInt() and 0xFF
                    if (tlv == 0x03) {
                        val len = block[1].toInt() and 0xFF
                        used = if (len == 0xFF) {
                            ((block[2].toInt() and 0xFF) shl 8) or (block[3].toInt() and 0xFF)
                        } else len
                    }
                } catch (_: Throwable) {
                } finally {
                    try { mu.close() } catch (_: Throwable) {}
                }
            }
        }

        _tagInfo.value = TagInfo(
            type = type,
            techs = techs,
            uid = uid,
            atqa = atqaHex,
            sak = sakHex,
            size = size,
            used = used,
            writable = writable,
            readonly = readonly,
            records = emptyList()
        )
    }

    fun writeTextNdef(text: String): Result<Unit> = runCatching {
        val tag = lastTag ?: error("No tag")
        val ndef = Ndef.get(tag) ?: error("Tag is not NDEF")
        ndef.connect()
        val rec = NdefRecord.createTextRecord("", text)
        val msg = NdefMessage(arrayOf(rec))
        ensureCapacity(ndef, msg)
        ndef.writeNdefMessage(msg)
        ndef.close()
    }

    fun writeUrlNdef(url: String): Result<Unit> = runCatching {
        val tag = lastTag ?: error("No tag")
        val ndef = Ndef.get(tag) ?: error("Tag is not NDEF")
        ndef.connect()
        val rec = NdefRecord.createUri(url)
        val msg = NdefMessage(arrayOf(rec))
        ensureCapacity(ndef, msg)
        ndef.writeNdefMessage(msg)
        ndef.close()
    }

    private fun ensureCapacity(ndef: Ndef, message: NdefMessage) {
        val bytes = message.toByteArray()
        if (ndef.maxSize < bytes.size) {
            try { ndef.close() } catch (_: Throwable) {}
            throw IOException("Tag capacity ${ndef.maxSize} < ${bytes.size}")
        }
        if (!ndef.isWritable) {
            try { ndef.close() } catch (_: Throwable) {}
            throw IOException("Tag is read-only")
        }
    }
}
