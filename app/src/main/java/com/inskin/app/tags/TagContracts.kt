// tags/TagContracts.kt
package com.inskin.app.tags

import android.nfc.Tag
import android.nfc.NdefRecord
import com.inskin.app.TagDetails

sealed class Channel {
    data class Android(val tag: Tag): Channel()
    data class Proxmark(val uidHex: String): Channel()
}

sealed class WriteOp {
    /** Écrit un message NDEF construit à partir de records Android. */
    data class Ndef(val records: List<NdefRecord>): WriteOp()
    /** NTAG/Ultralight: écriture d’une page de 4 octets. */
    data class RawPage(val page: Int, val data: ByteArray): WriteOp()
    /** MIFARE Classic: écriture d’un bloc de 16 octets (auth pré-requise côté app). */
    data class ClassicBlock(val block: Int, val data: ByteArray): WriteOp()
    /** Type 4: envoi d’une APDU brute. */
    data class IsoDepApdu(val apdu: ByteArray): WriteOp()
}

data class WriteReport(val ok: Boolean, val message: String? = null)
data class ReadResult(val details: TagDetails, val raw: ByteArray? = null, val notes: List<String> = emptyList())

interface TagInspector {
    fun supports(channel: Channel): Boolean
    fun readRaw(channel: Channel): ByteArray?
    fun decode(channel: Channel, raw: ByteArray?): TagDetails
    fun write(channel: Channel, ops: List<WriteOp>): WriteReport

    fun read(channel: Channel): ReadResult {
        val raw = readRaw(channel)
        val det = decode(channel, raw)
        return ReadResult(det, raw)
    }
}
