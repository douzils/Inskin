package com.inskin.app

import android.app.Application
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
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

        val ndef = Ndef.get(tag)
        val uid = tag.id?.joinToString("") { "%02X".format(it) } ?: ""
        val type = ndef?.type ?: "Unknown"
        val techs = tag.techList.toList()
        val size = ndef?.maxSize ?: 0
        val used = ndef?.cachedNdefMessage?.toByteArray()?.size ?: 0
        val writable = ndef?.isWritable == true
        val readOnly = ndef?.isWritable == false
        val format = if (ndef != null) "NDEF" else "Inconnu"

        _tagInfo.value = TagInfo(
            uid = uid,
            type = type,
            techs = techs,
            atqa = null,
            sak = null,
            format = format,
            size = size,
            used = used,
            isWritable = writable,
            isReadOnly = readOnly
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
            ndef.close()
            throw IOException("Tag capacity ${ndef.maxSize} < ${bytes.size}")
        }
        if (!ndef.isWritable) {
            ndef.close()
            throw IOException("Tag is read-only")
        }
    }
}
