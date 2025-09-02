// tags/NfcRfidInspectorRouter.kt
package com.inskin.app.tags

import com.inskin.app.tags.nfc.*
import com.inskin.app.tags.rfid.*

object NfcRfidInspectorRouter {
    private val inspectors: List<TagInspector> = listOf(
        // NFC
        MifareClassicInspector,
        MifareUltralightInspector,
        IsoDepInspector,
        NfcAInspector,
        NfcBInspector,
        NfcFInspector,
        NfcVInspector,
        NdefInspector,
        RawNfcInspector,
        // RFID
        RfidLowFrequencyInspector,
        RfidHighFrequencyInspector,
        RfidUhfInspector,
        RfidAnimalInspector,
        RawRfidInspector
    )

    fun firstSupporting(channel: Channel): TagInspector =
        inspectors.firstOrNull { it.supports(channel) } ?: RawNfcInspector
}
