package com.inskin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.inskin.app.nav.InskinNav
import com.inskin.app.nfc.NfcController

class MainActivity : ComponentActivity() {
  private lateinit var nfc: NfcController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    nfc = NfcController(this)

    setContent { InskinNav(nfc = nfc) }
  }

  override fun onResume() {
    super.onResume()
    nfc.enableReaderMode()
  }
  override fun onPause() {
    nfc.disableReaderMode()
    super.onPause()
  }
}
