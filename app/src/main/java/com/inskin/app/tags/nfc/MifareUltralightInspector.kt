// tags/nfc/MifareUltralightInspector.kt
@file:Suppress("SpellCheckingInspection")

package com.inskin.app.tags.nfc

import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import com.inskin.app.NdefRecordInfo
import com.inskin.app.TagDetails
import com.inskin.app.tags.*
import com.inskin.app.toHex
import kotlin.math.min

/**
 * Type 2 / NTAG / Ultralight:
 * - GET_VERSION (0x60) -> 8 bytes
 * - READ (0x30 addr) -> 16 bytes (4 pages)
 * - FAST_READ (0x3A start end) -> (end-start+1)*4 bytes
 * - READ_CNT (0x39 idx) -> 3 bytes (selon puce)
 * - READ_SIG (0x3C 0x00) -> 32 bytes (selon puce)
 * - WRITE (0xA2 page data[4])
 */
object MifareUltralightInspector : TagInspector {

    /* -------------------------------- Entry points -------------------------------- */

    override fun supports(channel: Channel): Boolean =
        channel is Channel.Android && MifareUltralight.get(channel.tag) != null

    /**
     * Objectif: fournir un "head" brut pour l’onglet Dump.
     * Stratégie sûre: GET_VERSION + FAST_READ 0..0x10 (17 pages ≈ 68B).
     * On fallback en READ si FAST_READ échoue.
     */
    override fun readRaw(channel: Channel): ByteArray? {
        val tag = (channel as Channel.Android).tag
        val a = NfcA.get(tag) ?: return null
        return InspectorUtils.withNfcA(a) {
            InspectorUtils.emitLog?.invoke("Ultralight: GET_VERSION…")
            val ver = safeXcv(byteArrayOf(0x60)) ?: ByteArray(0)

            val head: ByteArray? =
                safeXcv(byteArrayOf(0x3A, 0x00, 0x10)) ?: run {
                    InspectorUtils.emitLog?.invoke("FAST_READ indisponible, fallback READ(0x30)…")
                    val parts = ArrayList<Byte>()
                    for (p in 0..0x10 step 4) {
                        val r = safeXcv(byteArrayOf(0x30, p.toByte())) ?: break
                        parts.addAll(r.toList())
                    }
                    val arr = parts.toByteArray()
                    if (arr.isEmpty()) null else arr
                }

            if (head != null) ver + head else ver
        }
    }


    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val uid = tag.id ?: ByteArray(0)
        val uidHex = uid.toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()

        val a = NfcA.get(tag)
        val atqaHex = a?.atqa?.toHex()
        val sakHex = a?.sak?.toInt()?.and(0xFF)?.let { "%02X".format(it) }

        // -------- Version + taille mémoire
        var versionHex: String? = null
        var totalBytes: Int? = null
        var memoryLayout: String? = null

        // Si raw commence par la réponse GET_VERSION, la garder
        if (raw != null && raw.size >= 8) {
            versionHex = raw.copyOfRange(0, 8).toHex()
            val blocks = (raw[6].toInt() and 0xFF) + 1
            if (blocks in 16..4096) {
                totalBytes = blocks * 4
                memoryLayout = "$blocks pages × 4B (${blocks * 4} B)"
            }
        } else {
            a?.let {
                InspectorUtils.withNfcA(it) {
                    val v = safeXcv(byteArrayOf(0x60))
                    if (v != null && v.size >= 8) {
                        versionHex = v.copyOfRange(0, 8).toHex()
                        val blocks = (v[6].toInt() and 0xFF) + 1
                        if (blocks in 16..4096) {
                            totalBytes = blocks * 4
                            memoryLayout = "$blocks pages × 4B (${blocks * 4} B)"
                        }
                    }
                }
            }
        }


        // -------- CC / Lock / OTP depuis les premières pages
        // raw sans la tête version
        val head = raw?.drop(8)?.toByteArray() ?: ByteArray(0)
        // Pages 0..3 = 16B si disponibles
        val p0to3 = head.take(16).toByteArray()
        val lockBitsHex = if (p0to3.size >= 12) p0to3.copyOfRange(10, 12).toHex() else null
        val page3 = if (p0to3.size >= 16) p0to3.copyOfRange(12, 16) else null
        // CC si byte0==0xE1
        var ndefCapacity: Int? = null
        var otpBytesHex: String? = null
        if (page3 != null && page3.isNotEmpty()) {
            if ((page3[0].toInt() and 0xFF) == 0xE1) {
                // Capability Container: [E1][ver][size*8][access]
                ndefCapacity = (page3[2].toInt() and 0xFF) * 8
            } else {
                // Ultralight (non NTAG): OTP présents ici
                otpBytesHex = page3.toHex()
            }
        }

        // -------- Lecture NDEF via android.nfc.tech.Ndef
        var ndefRecords: List<NdefRecordInfo> = emptyList()
        var isNdefWritable: Boolean? = null
        var canMakeReadOnly: Boolean? = null
        var usedBytes: Int? = null

        Ndef.get(tag)?.let { n ->
            try {
                n.connect()
                isNdefWritable = n.isWritable
                if ((ndefCapacity ?: 0) <= 0) ndefCapacity = n.maxSize
                canMakeReadOnly = n.canMakeReadOnly()


                val msg = n.cachedNdefMessage ?: n.ndefMessage
                if (msg != null) {
                    usedBytes = msg.toByteArray().size
                    ndefRecords = msg.records.map { r -> decodeNdefRecord(r) }
                }
            } catch (_: Exception) {
                // ignore
            } finally {
                try { n.close() } catch (_: Exception) {}
            }
        }

        // -------- READ_CNT et READ_SIG best-effort
        var countersHex: String? = null
        var eccSignatureHex: String? = null
        a?.let {
            InspectorUtils.withNfcA(it) {
                // READ_CNT (0x39 idx). Les index varient selon la puce; on tente 0..2.
                val cnts = mutableListOf<String>()
                for (idx in 0..2) {
                    val c = safeXcv(byteArrayOf(0x39, idx.toByte()))
                    if (c != null && c.size >= 3) cnts.add("C$idx=${c.copyOfRange(0, 3).toHex()}")
                }
                if (cnts.isNotEmpty()) countersHex = cnts.joinToString(",")

                // READ_SIG (0x3C 0x00) -> 32B sur NTAG21x
                val sig = safeXcv(byteArrayOf(0x3C, 0x00))
                if (sig != null && sig.size >= 32) eccSignatureHex = sig.copyOfRange(0, 32).toHex()
            }
        }

        // -------- Remplissage TagDetails
        val rawReadableBytes = raw?.size
        val rawDumpFirstBytesHex = raw?.copyOfRange(0, min(64, raw.size))?.toHex()

        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            atqaHex = atqaHex,
            sakHex = sakHex,
            manufacturer = InspectorUtils.guessManufacturer(uid),
            chipType = "MIFARE Ultralight/NTAG",
            totalMemoryBytes = totalBytes,
            memoryLayout = memoryLayout,
            versionHex = versionHex,
            isNdefWritable = isNdefWritable,
            canMakeReadOnly = canMakeReadOnly,
            ndefCapacity = ndefCapacity,
            ndefRecords = ndefRecords,
            usedBytes = usedBytes,
            lockBitsHex = lockBitsHex,
            otpBytesHex = otpBytesHex,
            countersHex = countersHex,
            eccSignatureHex = eccSignatureHex,
            rawReadableBytes = rawReadableBytes,
            rawDumpFirstBytesHex = rawDumpFirstBytesHex
        )
    }

    /**
     * Ecrit:
     * - WriteOp.Ndef: via Ndef ou NdefFormatable si vide.
     * - WriteOp.RawPage: WRITE (0xA2).
     */
    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport {
        val tag = (channel as Channel.Android).tag
        // 1) Gestion NDEF si demandé
        ops.filterIsInstance<WriteOp.Ndef>().firstOrNull()?.let { op ->
            val n = Ndef.get(tag)
            if (n != null) {
                return runCatching {
                    n.connect()
                    val msg = android.nfc.NdefMessage(op.records.toTypedArray())
                    n.writeNdefMessage(msg)
                    n.close()
                    InspectorUtils.ok("NDEF écrit (${msg.toByteArray().size} octets)")
                }.getOrElse { return InspectorUtils.fail("NDEF: ${it.message}") }
            } else {
                val fmt = NdefFormatable.get(tag)
                if (fmt != null) {
                    return runCatching {
                        fmt.connect()
                        val msg = android.nfc.NdefMessage(op.records.toTypedArray())
                        fmt.format(msg)
                        fmt.close()
                        InspectorUtils.ok("Formatage+NDEF OK (${msg.toByteArray().size} octets)")
                    }.getOrElse { return InspectorUtils.fail("Formatage: ${it.message}") }
                }
            }
            return InspectorUtils.fail("NDEF non supporté par ce tag")
        }

        // 2) Ecritures brutes de pages
        val a = NfcA.get(tag) ?: return InspectorUtils.fail("NfcA requis")
        return runCatching {
            a.connect()
            ops.forEach { op ->
                when (op) {
                    is WriteOp.RawPage -> {
                        require(op.data.size == 4) { "Page = 4 octets" }
                        InspectorUtils.emitLog?.invoke("WRITE page ${op.page} = ${op.data.toHex()}")
                        a.transceive(byteArrayOf(0xA2.toByte(), op.page.toByte()) + op.data)
                    }
                    else -> Unit
                }
            }
            a.close()
            InspectorUtils.ok("Écriture Ultralight OK")
        }.getOrElse { InspectorUtils.fail(it.message ?: "Échec Ultralight") }
    }

    /* -------------------------------- Helpers -------------------------------- */

    private fun NfcA.safeXcv(cmd: ByteArray): ByteArray? =
        try { transceive(cmd) } catch (_: Exception) { null }

    private fun decodeNdefRecord(r: android.nfc.NdefRecord): NdefRecordInfo {
        return when {
            r.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(
                android.nfc.NdefRecord.RTD_URI
            ) -> {
                val s = decodeUri(r.payload)
                NdefRecordInfo("URI", s)
            }
            r.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(
                android.nfc.NdefRecord.RTD_TEXT
            ) -> {
                val s = decodeText(r.payload)
                NdefRecordInfo("Text", s)
            }
            r.tnf == android.nfc.NdefRecord.TNF_MIME_MEDIA -> {
                val mime = try { String(r.type, Charsets.US_ASCII) } catch (_: Exception) { "mime" }
                val data = r.payload.toHex()
                NdefRecordInfo("MIME:$mime", data)
            }
            else -> {
                val kind = "TNF${r.tnf}:${try { String(r.type, Charsets.US_ASCII) } catch (_: Exception) { r.type.toHex() }}"
                NdefRecordInfo(kind, r.payload.toHex())
            }
        }
    }

    private fun decodeText(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val status = payload[0].toInt() and 0xFF
        val langLen = status and 0x3F
        val enc = if ((status and 0x80) != 0) Charsets.UTF_16 else Charsets.UTF_8
        val text = payload.copyOfRange(1 + langLen, payload.size).toString(enc)
        return text
    }

    private val URI_PREFIX = arrayOf(
        "", "http://www.", "https://www.", "http://", "https://",
        "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://",
        "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:",
        "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:",
        "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
        "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:",
        "urn:nfc:"
    )

    private fun decodeUri(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefixIdx = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIX.getOrNull(prefixIdx) ?: ""
        val rest = try { String(payload, 1, payload.size - 1, Charsets.UTF_8) } catch (_: Exception) { "" }
        return prefix + rest
    }
}
