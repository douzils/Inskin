package com.inskin.app.usb

import android.content.Context

object ProxmarkLocator {
    @Volatile private var inst: ProxmarkManager? = null
    fun get(ctx: Context): ProxmarkManager =
        inst ?: synchronized(this) {
            inst ?: ProxmarkManager(ctx.applicationContext).also { inst = it }
        }
}
