package com.inskin.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbAttachReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mgr = ProxmarkLocator.get(context)
        val dev = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> mgr.onDeviceAttached(dev)
            UsbManager.ACTION_USB_DEVICE_DETACHED -> mgr.onDeviceDetached(dev)
            "com.inskin.app.USB_PERMISSION" -> {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                mgr.onPermission(dev, granted)
            }
        }
    }
}
