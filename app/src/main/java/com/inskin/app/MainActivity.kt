package com.inskin.app

import android.nfc.Ndef
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Main entry point for the application. While the activity is in the
 * foreground we enable NFC reader mode so that scanned tags are routed to the
 * app instead of launching their own action (for example opening a URL in the
 * browser). Information extracted from the tag is stored in [tagInfo] and is
 * displayed by composables.
 */
class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null

    // Holds information about the last scanned tag
    private var tagInfo by mutableStateOf<TagInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            InskinTheme {
                // Pass the latest tag info down to the navigation graph
                InskinNav(tagInfo)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable reader mode so tags are delivered directly to this activity
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

    /**
     * Called whenever a tag is discovered while reader mode is enabled.
     * Extracts information about the tag and updates [tagInfo] so the UI can
     * display it.
     */
    override fun onTagDiscovered(tag: Tag) {
        val id = tag.id.joinToString("") { String.format("%02X", it) }
        val techList = tag.techList.map { it.substringAfterLast('.') }

        val nfcA = NfcA.get(tag)
        val atqa = nfcA?.atqa?.joinToString("") { String.format("%02X", it) } ?: "N/A"
        val sak = nfcA?.sak?.let { String.format("0x%02X", it) } ?: "N/A"

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

