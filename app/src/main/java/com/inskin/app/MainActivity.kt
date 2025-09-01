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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inskin.app.ui.screens.ScanScreen
import com.inskin.app.ui.screens.WriteTagRoute
import com.inskin.app.ui.screens.TagListScreen
import com.inskin.app.usb.ProxmarkStatus

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
            val backstack by nav.currentBackStackEntryAsState()
            val route = backstack?.destination?.route

            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
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
                                    nav.popBackStack()
                                }
                            )
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

/** Bulle noire + icône Proxmark + badge statut. */
@Composable
fun ProxmarkBubble(
    status: ProxmarkStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 56.dp
) {
    Box(
        modifier
            .size(diameter)
            .shadow(6.dp, CircleShape, clip = false)
            .background(Color.Black, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Icône Proxmark minimaliste
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val c = center
            val minDim = kotlin.math.min(size.width, size.height)

            drawCircle(color = Color.White, radius = minDim * 0.12f)

            listOf(0.28f, 0.42f, 0.56f).forEach { r ->
                drawCircle(
                    color = Color.White,
                    radius = minDim * r,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            drawLine(
                color = Color.Black,
                start = c + Offset(0f, -minDim * 0.56f),
                end = c + Offset(0f, -minDim * 0.28f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        val badgeColor = when (status) {
            ProxmarkStatus.Ready -> Color(0xFF22C55E)
            ProxmarkStatus.Initializing -> Color(0xFFF59E0B)
            ProxmarkStatus.NotPresent -> Color(0xFF9CA3AF)
            ProxmarkStatus.NoAccess, ProxmarkStatus.Error -> Color(0xFFEF4444)
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-4).dp, y = (-4).dp)
                .size(14.dp)
                .shadow(2.dp, CircleShape)
                .background(badgeColor, CircleShape)
        )
    }
}
