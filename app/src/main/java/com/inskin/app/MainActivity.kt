package com.inskin.app
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import com.inskin.app.ui.theme.InskinTheme
import com.inskin.app.ui.screens.ScanScreen

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var nfc: NfcAdapter? = null
    private lateinit var vm: NfcViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(this)[NfcViewModel::class.java]
        nfc = NfcAdapter.getDefaultAdapter(this)
        setContent { InskinTheme { Surface { ScanScreen(vm) } } }
    }

    override fun onResume() {
        super.onResume()
        nfc?.enableReaderMode(
            this, this,
            NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }
    override fun onPause() { super.onPause(); nfc?.disableReaderMode(this) }
    override fun onTagDiscovered(tag: Tag) { vm.handleTag(tag) }
}
