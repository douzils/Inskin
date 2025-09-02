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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inskin.app.ui.PreferencesViewModel
import com.inskin.app.ui.screens.ScanScreen
import com.inskin.app.ui.screens.SettingsScreen
import com.inskin.app.ui.screens.TagListScreen
import com.inskin.app.ui.screens.WriteTagRoute
import com.inskin.app.ui.theme.InskinTheme
import com.inskin.app.usb.ProxmarkLocator
import com.inskin.app.usb.ProxmarkStatus
import com.inskin.app.tags.InspectorUtils
import com.inskin.app.tags.nfc.KeysRepository
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var vm: NfcViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Injecte le dictionnaire MCT une seule fois
        if (InspectorUtils.extraKeysProvider == null) {
            val repo = KeysRepository(applicationContext)
            InspectorUtils.extraKeysProvider = { repo.loadAllFromAssets() }
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        vm = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NfcViewModel::class.java]

        val prefsVm = ViewModelProvider(this)[PreferencesViewModel::class.java]

        setContent {
            val nav = rememberNavController()
            val backstack by nav.currentBackStackEntryAsState()
            val dark by prefsVm.darkTheme.collectAsState()

            InskinTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize()) {
                    NavHost(navController = nav, startDestination = "scan") {
                        composable("scan") {
                            ScanScreen(
                                vm = vm,
                                onOpenWrite = { nav.navigate("write") },
                                onOpenList  = { nav.navigate("taglist") },
                                onOpenSettings = { nav.navigate("settings") }
                            )
                        }
                        composable("write")   { WriteTagRoute(nav, vm) }
                        composable("taglist") {
                            TagListScreen(vm, onBack = { nav.popBackStack() }) { uid ->
                                vm.selectFromHistory(uid); nav.popBackStack()
                            }
                        }
                        composable("settings") {
                            SettingsScreen(vm = prefsVm, onBack = { nav.popBackStack() })
                        }
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

        val pm = ProxmarkLocator.get(this)
        if (vm.pm3Status.value == ProxmarkStatus.Ready) pm.startAutoRead()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        ProxmarkLocator.get(this).stopAutoRead()
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
