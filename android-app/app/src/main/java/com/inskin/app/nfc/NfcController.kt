package com.inskin.app.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NfcController(private val activity: Activity) {
  private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

  private val _lastRead = MutableStateFlow<NdefResult?>(null)
  val lastRead = _lastRead.asStateFlow()

  private val _status = MutableStateFlow(Status())
  val status = _status.asStateFlow()

  private val _lastTag = MutableStateFlow<Tag?>(null)
  val lastTag = _lastTag.asStateFlow()

  data class Status(val nfcAvailable: Boolean = true, val readerEnabled: Boolean = false)
  data class NdefResult(val uid: String, val tech: List<String>, val records: List<NdefUtils.NdefParsed>)

  fun enableReaderMode() {
    if (adapter == null) { _status.value = Status(nfcAvailable=false); return }
    val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    adapter.enableReaderMode(activity, { tag -> onTag(tag) }, flags, Bundle())
    _status.value = Status(nfcAvailable=true, readerEnabled=true)
  }

  fun disableReaderMode() {
    adapter?.disableReaderMode(activity)
    _status.value = _status.value.copy(readerEnabled=false)
  }

  private fun onTag(tag: Tag) {
    val uid = tag.id?.joinToString("") { "%02X".format(it) } ?: "UNKNOWN"
    val techs = tag.techList.toList()
    val ndef = Ndef.get(tag)
    val parsed = try {
      val msg = ndef?.cachedNdefMessage ?: run {
        ndef?.connect()
        val m = ndef?.ndefMessage
        ndef?.close()
        m
      }
      NdefUtils.parse(msg)
    } catch (_: Exception) { emptyList() }
    _lastTag.value = tag
    _lastRead.value = NdefResult(uid, techs, parsed)
  }

  /** Écrit une charge NDEF complète sur le tag courant. Retourne null si ok ou message d’erreur. */
  fun writeNdef(tag: Tag, payload: NdefUtils.NdefBuild): String? {
    return try {
      val ndef = Ndef.get(tag) ?: return "Tag non NDEF"
      ndef.connect()
      val msg = NdefUtils.build(payload)
      if (!ndef.isWritable) return "Tag en lecture seule"
      if (ndef.maxSize < msg.toByteArray().size) return "Payload trop grand pour ce tag"
      ndef.writeNdefMessage(msg)
      ndef.close()
      null
    } catch (e: Exception) {
      e.message ?: "Écriture échouée"
    }
  }
}
