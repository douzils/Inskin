package com.inskin.app.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.regex.Pattern
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import java.nio.charset.StandardCharsets

class ProxmarkManager(private val app: Context) {


    // RSSI proxmark 0..4 (replay=1 pour dernier niveau)
    private val _rssi = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 8)
    val rssi = _rssi.asSharedFlow()

    // Regex RSSI (selon sorties client)
    private val RE_RSSI = Pattern.compile("(?i)\\bRSSI\\b\\s*[:=]\\s*(-?\\d+)")

    private var readerJob: Job? = null
    private val writerMutex = Mutex()

    private val _detections = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val detections = _detections.asSharedFlow()

    private val RE_HEX_PAIR = Pattern.compile("([0-9A-Fa-f]{2})")
    private val RE_UID_GENERIC = Pattern.compile(
        "(?i)\\b(UID|NFCID|Card UID|Tag UID)\\b[^0-9A-Fa-f]*((?:[0-9A-Fa-f]{2}[ :]){3,10}[0-9A-Fa-f]{2})"
    )


    private val usb = app.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var port: UsbSerialPort? = null
    private var devId: Int? = null

    private val TAG = "ProxmarkMgr"

    /** PendingIntent EXPLICITE (FLAG_IMMUTABLE) –> Android 14+ OK */
    private fun permIntent(): PendingIntent {
        val i = Intent(app, UsbAttachReceiver::class.java).setAction(PERM_ACTION)
        // Était: FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(app, 0, i, PendingIntent.FLAG_MUTABLE)
    }

    /** A appeler au démarrage pour détecter si un PM3 est déjà branché. */
    fun scanExisting() {
        val dev = usb.deviceList.values.firstOrNull { ProxmarkLocator.isProxmark(it) }
        if (dev == null) {
            ProxmarkLocator.status.tryEmit(ProxmarkStatus.NotPresent)
            return
        }
        if (usb.hasPermission(dev)) open(dev) else requestPermission(dev)
    }

    fun onDeviceAttached(d: UsbDevice) {
        if (!ProxmarkLocator.isProxmark(d)) return
        requestPermission(d)
    }

    fun onDeviceDetached(d: UsbDevice?) {
        if (d != null && devId != null && d.deviceId != devId) return
        close()
        ProxmarkLocator.status.tryEmit(ProxmarkStatus.NotPresent)
    }

    fun onPermission(d: UsbDevice?, granted: Boolean) {
        if (d == null) return
        if (!granted) {
            Log.d(TAG, "permission denied for id=${d.deviceId}")
            ProxmarkLocator.status.tryEmit(ProxmarkStatus.NoAccess)
            return
        }
        open(d)
    }

    private fun requestPermission(d: UsbDevice) {
        ProxmarkLocator.status.tryEmit(ProxmarkStatus.Initializing)
        usb.requestPermission(d, permIntent())
    }

    private fun open(d: UsbDevice) {
        scope.launch {
            try {
                Log.d(TAG, "open id=${d.deviceId}")
                val driver = UsbSerialProber.getDefaultProber().probeDevice(d)
                    ?: run {
                        Log.w(TAG, "no driver – probably no access")
                        ProxmarkLocator.status.tryEmit(ProxmarkStatus.NoAccess)
                        return@launch
                    }

                val conn = usb.openDevice(d)
                    ?: run {
                        Log.w(TAG, "openDevice returned null – access?")
                        ProxmarkLocator.status.tryEmit(ProxmarkStatus.NoAccess)
                        return@launch
                    }

                val p = driver.ports.firstOrNull()
                    ?: run {
                        Log.e(TAG, "no serial ports on driver")
                        ProxmarkLocator.status.tryEmit(ProxmarkStatus.Error)
                        return@launch
                    }

                p.open(conn)
                p.setParameters(
                    115200,
                    8,
                    UsbSerialPort.STOPBITS_1,
                    UsbSerialPort.PARITY_NONE
                )
                // Active les lignes de contrôle
                p.dtr = true
                p.rts = true
                // purge initiale
                runCatching { p.read(ByteArray(4096), 50) }

                port = p
                devId = d.deviceId
                ProxmarkLocator.status.tryEmit(ProxmarkStatus.Ready)
                startAutoRead()
                Log.d(TAG, "ready")
            } catch (t: Throwable) {
                Log.e(TAG, "open failed", t)
                close()
                ProxmarkLocator.status.tryEmit(ProxmarkStatus.Error)
            }
        }
    }

    fun close() {
        scope.launch {
            stopAutoRead()
            runCatching { port?.close() }
            port = null
            devId = null
        }
    }

    fun startAutoRead() {
        if (port == null) return
        if (readerJob?.isActive == true) return
        readerJob = scope.launch {
            try {
                while (true) {
                    // Commandes optimisées pour les implants
                    sendCmd("hf search");     readLoopAndEmit(3000); kotlinx.coroutines.delay(150)
                    sendCmd("hf 14a reader"); readLoopAndEmit(3000); kotlinx.coroutines.delay(150)
                    sendCmd("hf mfu info");   readLoopAndEmit(3000); kotlinx.coroutines.delay(150)
                    sendCmd("hf 14a info");   readLoopAndEmit(3000); kotlinx.coroutines.delay(150)
                    // Commandes spécifiques pour implants
                    sendCmd("hf 14a raw -s -c 60"); readLoopAndEmit(2000); kotlinx.coroutines.delay(150) // GET_VERSION pour NTAG
                    // LF pour xEM et NExT (moins fréquent car plus lent)
                    // sendCmd("lf search");  readLoopAndEmit(4000); kotlinx.coroutines.delay(500)
                    kotlinx.coroutines.delay(300)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "reader loop crashed", t)
            }
        }
    }

    /**
     * Mode de lecture spécialisé pour les implants
     * Optimisé pour détecter les implants Dangerous Things
     */
    fun startImplantMode() {
        if (port == null) return
        if (readerJob?.isActive == true) return
        readerJob = scope.launch {
            try {
                while (true) {
                    // Scan HF pour xNT, xM1, xAC, VivoKey
                    sendCmd("hf search");     readLoopAndEmit(3000); kotlinx.coroutines.delay(200)
                    sendCmd("hf 14a info");   readLoopAndEmit(3000); kotlinx.coroutines.delay(200)
                    sendCmd("hf mfu info");   readLoopAndEmit(3000); kotlinx.coroutines.delay(200)
                    // Test Mifare Classic pour xM1
                    sendCmd("hf mf info");    readLoopAndEmit(3000); kotlinx.coroutines.delay(200)
                    // Scan LF pour xEM et partie LF du NExT
                    sendCmd("lf search");     readLoopAndEmit(4000); kotlinx.coroutines.delay(500)
                    sendCmd("lf em 410x reader"); readLoopAndEmit(2000); kotlinx.coroutines.delay(300)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "implant mode crashed", t)
            }
        }
    }

    /**
     * Lit un tag spécifique avec des commandes optimisées pour les implants
     */
    suspend fun readImplantDetails(): String? {
        val p = port ?: return null
        val result = StringBuilder()

        try {
            // Commandes de diagnostic détaillé
            sendCmd("hf 14a info -v")
            val info = readLoopOutput(3000)
            result.append(info)

            sendCmd("hf mfu info")
            val mfuInfo = readLoopOutput(3000)
            result.append("\n").append(mfuInfo)

            return result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read implant details", e)
            return null
        }
    }

    /**
     * Lit la sortie complète sans émettre d'événements
     */
    private fun readLoopOutput(timeoutMs: Long): String {
        val p = port ?: return ""
        val deadline = System.currentTimeMillis() + timeoutMs
        val buf = ByteArray(4096)
        val result = StringBuilder()

        while (System.currentTimeMillis() < deadline) {
            val n = try {
                p.read(buf, 250)
            } catch (t: Throwable) {
                break
            }
            if (n > 0) {
                result.append(String(buf, 0, n, StandardCharsets.US_ASCII))
            }
        }

        return result.toString()
    }

    fun stopAutoRead() {
        readerJob?.cancel()
        readerJob = null
    }

    private suspend fun sendCmd(cmd: String) {
        val p = port ?: return
        val line = (cmd.trim() + "\n").toByteArray(StandardCharsets.US_ASCII)
        writerMutex.lock()
        try {
            p.write(line, 500) // timeout écriture ms
        } finally {
            writerMutex.unlock()
        }
    }

    /** Lit quelques trames, parse les UIDs et RSSI, émet sur les flows. */
    private fun readLoopAndEmit(timeoutMs: Long = 1200L) {
        val p = port ?: return
        val deadline = System.currentTimeMillis() + timeoutMs
        val buf = ByteArray(4096)
        val acc = StringBuilder()

        while (System.currentTimeMillis() < deadline) {
            val n = try {
                p.read(buf, 250) // timeout lecture ms
            } catch (t: Throwable) {
                Log.d(TAG, "read error/timeout", t)
                break
            }
            if (n <= 0) continue

            acc.append(String(buf, 0, n, StandardCharsets.US_ASCII))

            // UID
            val m = RE_UID_GENERIC.matcher(acc)
            while (m.find()) {
                val raw = m.group(2) ?: continue
                val hex = normalizeHex(raw)
                if (hex.length >= 8) {
                    ProxmarkLocator.status.tryEmit(ProxmarkStatus.Ready)
                    _detections.tryEmit(hex)
                }
            }
            // RSSI -> 0..4
            val mr = RE_RSSI.matcher(acc)
            while (mr.find()) {
                val v = mr.group(1)?.toIntOrNull() ?: continue
                val lvl = when {
                    v >= -30 -> 4
                    v >= -45 -> 3
                    v >= -60 -> 2
                    v >= -75 -> 1
                    else     -> 0
                }
                _rssi.tryEmit(lvl)
            }

            val s = acc.toString()
            if (s.contains("pm3 -->") || s.contains("No tag", true) || s.contains("not found", true)) {
                break
            }
        }
    }

    /** Lit quelques trames, parse les UIDs, émet sur _detections. */

    private fun normalizeHex(s: String): String {
        val m = RE_HEX_PAIR.matcher(s)
        val out = StringBuilder()
        while (m.find()) out.append(m.group(1)!!.uppercase())
        return out.toString()
    }

    companion object {
        /** Action du broadcast de résultat de permission USB. */
        const val PERM_ACTION = "com.inskin.app.USB_PERMISSION"
    }
}
