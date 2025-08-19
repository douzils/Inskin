package com.inskin.app

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.inskin.app.ui.screens.ScanScreen

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val vm: NfcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            ScanScreen(vm)   // pas de CompositionLocal particulier : on “bypass” le scale via code
        }
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.startWaiting()
        nfcAdapter?.enableReaderMode(
            this,
            { tag -> onTagDiscovered(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            Bundle()
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action ?: return
        if (
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let { onTagDiscovered(it) }
        }
    }

    private fun onTagDiscovered(tag: Tag) {
        val uidHex = tag.id?.joinToString("") { b -> "%02X".format(b) } ?: ""
        runOnUiThread {
            vm.updateTag(uidHex)
            Toast.makeText(this, "Tag détecté", Toast.LENGTH_SHORT).show()
        }
    }
}
