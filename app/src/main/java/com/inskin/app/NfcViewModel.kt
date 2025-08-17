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

/**
 * ViewModel keeping track of the last NFC tag discovered and providing helpers
 * to write NDEF messages.
 */
class NfcViewModel(application: Application) : AndroidViewModel(application) {
    private var lastTag: Tag? = null

    private val _tagInfo = MutableStateFlow<TagInfo?>(null)
    val tagInfo = _tagInfo.asStateFlow()

    fun updateFromTag(tag: Tag) {
        lastTag = tag
        val ndef = Ndef.get(tag)
        val type = ndef?.type
        val size = ndef?.maxSize ?: 0
        val used = ndef?.cachedNdefMessage?.toByteArray()?.size ?: 0
        val writable = ndef?.isWritable == true
        val readonly = ndef?.isWritable == false
        val uid = tag.id?.joinToString(separator = "") { b -> "%02X".format(b) }
        val techs = tag.techList.toList()
        _tagInfo.value = TagInfo(type, techs, uid, null, null, size, used, writable, readonly)
    }

    fun writeTextNdef(text: String): Result<Unit> = runCatching {
        val tag = lastTag ?: error("No tag")
        val ndef = Ndef.get(tag) ?: error("Tag is not NDEF")
        ndef.connect()
        val record = NdefRecord.createTextRecord("", text)
        val message = NdefMessage(arrayOf(record))
        ensureCapacity(ndef, message)
        ndef.writeNdefMessage(message)
        ndef.close()
    }

    fun writeUrlNdef(url: String): Result<Unit> = runCatching {
        val tag = lastTag ?: error("No tag")
        val ndef = Ndef.get(tag) ?: error("Tag is not NDEF")
        ndef.connect()
        val record = NdefRecord.createUri(url)
        val message = NdefMessage(arrayOf(record))
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
