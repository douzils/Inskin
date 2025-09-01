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

class ProxmarkManager(private val app: Context) {
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
                port = p
                devId = d.deviceId
                ProxmarkLocator.status.tryEmit(ProxmarkStatus.Ready)
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
            runCatching { port?.close() }
            port = null
            devId = null
        }
    }

    companion object {
        /** Action du broadcast de résultat de permission USB. */
        const val PERM_ACTION = "com.inskin.app.USB_PERMISSION"
    }
}
