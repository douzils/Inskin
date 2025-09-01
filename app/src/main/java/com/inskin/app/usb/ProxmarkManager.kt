package com.inskin.app.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*

class ProxmarkManager(private val app: Context) {
    private val usb get() = app.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var port: UsbSerialPort? = null

    private val permission = "com.inskin.app.USB_PERMISSION"
    private fun permIntent(): PendingIntent =
        PendingIntent.getBroadcast(app, 0, Intent(permission), PendingIntent.FLAG_IMMUTABLE)

    fun isProxmark(d: UsbDevice): Boolean =
        UsbSerialProber.getDefaultProber().probeDevice(d) != null

    fun onDeviceAttached(d: UsbDevice) {
        if (!isProxmark(d)) return
        if (!usb.hasPermission(d)) { usb.requestPermission(d, permIntent()); return }
        open(d)
    }
    fun onPermission(d: UsbDevice, granted: Boolean) { if (granted) open(d) }
    fun onDeviceDetached(d: UsbDevice) { if (port?.driver?.device?.deviceId == d.deviceId) close() }

    private fun open(d: UsbDevice) {
        scope.launch {
            runCatching {
                val driver = UsbSerialProber.getDefaultProber().probeDevice(d) ?: return@launch
                val conn = usb.openDevice(d) ?: return@launch
                val p = driver.ports.first()
                p.open(conn)
                p.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                port = p
            }.onFailure { _ -> close() }
        }
    }
    fun close() { scope.launch { runCatching { port?.close() }; port = null } }
}
