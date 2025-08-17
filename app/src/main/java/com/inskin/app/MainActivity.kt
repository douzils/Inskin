package com.inskin.app

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Assure-toi que ces imports pointent bien vers tes fichiers :
import com.inskin.app.InskinTheme
import com.inskin.app.InskinNav

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null

    // Dernier tag scanné (observable par Compose)
    private var tagInfo by mutableStateOf<TagInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            InskinTheme {
                // InskinNav doit accepter un paramètre optionnel: tagInfo: TagInfo? = null
                InskinNav(tagInfo)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val id = tag.id.joinToString("") { "%02X".format(it) }
        val techList = tag.techList.map { it.substringAfterLast('.') }

        val nfcA = NfcA.get(tag)
        val atqa = nfcA?.atqa?.joinToString("") { "%02X".format(it) } ?: "N/A"
        val sak  = nfcA?.sak?.let { "0x%02X".format(it) } ?: "N/A"

        val ndef = Ndef.get(tag)
        val format = if (ndef != null) "NDEF" else "Inconnu"
        val size = ndef?.maxSize ?: 0
        val used = ndef?.cachedNdefMessage?.toByteArray()?.size ?: 0
        val isWritable = ndef?.isWritable ?: false
        val isReadOnly = ndef?.isWritable?.not() ?: false

        val info = TagInfo(
            type = techList.firstOrNull() ?: "Unknown",
            tech = techList.joinToString(", "),
            serial = id,
            atqa = atqa,
            sak = sak,
            format = format,
            size = size,
            used = used,
            isWritable = isWritable,
            isReadOnly = isReadOnly
        )

        runOnUiThread { tagInfo = info }
    }
}
