package com.inskin.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

class UsbAttachReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pm = ProxmarkLocator.get(context)
        val action = intent.action ?: return

        fun extraDevice(): UsbDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }

        try {
            when (action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val d = extraDevice() ?: return
                    if (!ProxmarkLocator.isProxmark(d)) return
                    Log.d("UsbAttachReceiver", "ATTACHED v=${d.vendorId} p=${d.productId}")
                    ProxmarkLocator.status.tryEmit(ProxmarkStatus.Initializing)
                    pm.onDeviceAttached(d)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val d = extraDevice()
                    Log.d("UsbAttachReceiver", "DETACHED id=${d?.deviceId}")
                    pm.onDeviceDetached(d)
                }
                ProxmarkManager.PERM_ACTION -> {
                    val d = extraDevice()
                    val granted =
                        intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Log.d("UsbAttachReceiver", "PERMISSION granted=$granted id=${d?.deviceId}")
                    pm.onPermission(d, granted)
                }
            }
        } catch (t: Throwable) {
            Log.e("UsbAttachReceiver", "onReceive failed", t)
            ProxmarkLocator.status.tryEmit(ProxmarkStatus.Error)
        }
    }
}
