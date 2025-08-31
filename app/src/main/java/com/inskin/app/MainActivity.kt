package com.inskin.app

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.inskin.app.ui.screens.ScanScreen
import com.inskin.app.ui.screens.WriteTagRoute
import com.inskin.app.ui.screens.TagListScreen

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private val vm: NfcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC non disponible sur cet appareil", Toast.LENGTH_LONG).show()
        }

        setContent {
            val nav = rememberNavController()
            MaterialTheme {
                NavHost(navController = nav, startDestination = "scan") {
                    composable("scan") {
                        ScanScreen(
                            vm = vm,
                            onOpenWrite = { nav.navigate("write") },
                            onOpenList = { nav.navigate("taglist") }
                        )
                    }
                    composable("write") {
                        WriteTagRoute(navController = nav, vm = vm)
                    }
                    composable("taglist") {
                        TagListScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onSelect = { uid ->
                                vm.selectFromHistory(uid)
                                nav.popBackStack() // retour à l'écran scan
                            }
                        )
                    }
                }
            }
        }

        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.startWaiting()

        val extras = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        nfcAdapter?.enableReaderMode(
            this,
            { tag -> onTagDiscovered(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            extras
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
        when (intent.action) {
            NfcAdapter.ACTION_TAG_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                val tag: Tag? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Parcelable>(NfcAdapter.EXTRA_TAG) as? Tag
                }
                tag?.let { onTagDiscovered(it) }
            }
        }
    }

    private fun onTagDiscovered(tag: Tag) {
        runOnUiThread {
            vm.updateTag(tag)
            Toast.makeText(this, "Tag détecté", Toast.LENGTH_SHORT).show()
        }
    }
}
