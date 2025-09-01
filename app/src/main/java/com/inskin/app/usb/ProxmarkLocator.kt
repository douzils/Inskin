package com.inskin.app.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import kotlinx.coroutines.flow.MutableSharedFlow

/** Etat du Proxmark pour l’UI. */
enum class ProxmarkStatus {
    NotPresent,        // non branché
    Initializing,      // demande de permission / ouverture
    Ready,             // prêt
    NoAccess,          // permission refusée
    Error              // erreur d’ouverture/IO
}

/** Point d’accès global : status + singleton du manager. */
object ProxmarkLocator {
    /** Flux que l’UI observe (replay=1 pour le dernier état). */
    val status = MutableSharedFlow<ProxmarkStatus>(replay = 1)

    @Volatile private var mgr: ProxmarkManager? = null

    fun get(context: Context): ProxmarkManager {
        val cur = mgr
        if (cur != null) return cur
        synchronized(this) {
            val again = mgr
            if (again != null) return again
            val created = ProxmarkManager(context.applicationContext)
            mgr = created
            return created
        }
    }

    /** Heuristique simple pour reconnaître le Proxmark3. */
    fun isProxmark(d: UsbDevice?): Boolean {
        if (d == null) return false
        val pn = d.productName ?: ""
        val mn = d.manufacturerName ?: ""
        if (pn.contains("proxmark", true) || mn.contains("proxmark", true)) return true
        // quelques couples connus (selon firmwares/câbles CDC ACM)
        val pair = d.vendorId to d.productId
        return pair in setOf(
            0x2D2D to 0x504D, // “PM3”
            0x9AC4 to 0x4B8F  // générique CDC (exemples)
        )
    }
}
