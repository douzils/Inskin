package com.inskin.app

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.*
import kotlin.math.min

/* ======================= Modèles ======================= */
data class NdefRecordInfo(val type: String, val value: String)

/* ---- MIFARE Classic : structures détaillées ---- */
data class ClassicBlockInfo(
    val sector: Int,
    val indexInSector: Int,      // 0..(blockCountInSector-1)
    val absBlock: Int,           // numéro absolu
    val dataHex: String?,        // null si non lisible
    val isValue: Boolean?,       // true si value block valide
    val value: Int?,             // valeur si value block
    val addr: Int?,              // adresse stockée dans le value block
    val access: String?          // Cx bits décodés pour ce bloc si trailer lisible
)

data class ClassicTrailer(
    val keyA: String?,           // Key A si trailer lisible
    val accessBitsHex: String?,  // 3 octets d’accès bruts (bytes 6..8)
    val accessDecoded: String,   // lecture humaine des C-bits pour B0..B2 + trailer
    val keyB: String?            // Key B si trailer lisible
)

data class ClassicSectorInfo(
    val sector: Int,
    val usedKeyType: String?,    // "KeyA" / "KeyB" si auth ok
    val usedKeyHex: String?,     // clé qui a permis la lecture
    val blockCount: Int,
    val blocks: List<ClassicBlockInfo>,
    val trailer: ClassicTrailer?
)

data class TagDetails(
    val uidHex: String,
    val techList: List<String>,

    // Identification
    val atqaHex: String? = null,
    val sakHex: String? = null,
    val manufacturer: String? = null,
    val batchInfo: String? = null,

    // Système
    val chipType: String? = null,
    val totalMemoryBytes: Int? = null,
    val memoryLayout: String? = null,
    val versionHex: String? = null,
    val atsHex: String? = null,
    val historicalBytesHex: String? = null,
    val hiLayerResponseHex: String? = null,

    // Sécurité
    val lockBitsHex: String? = null,
    val otpBytesHex: String? = null,
    val isNdefWritable: Boolean? = null,
    val canMakeReadOnly: Boolean? = null,

    // Données NDEF
    val ndefCapacity: Int? = null,
    val ndefRecords: List<NdefRecordInfo> = emptyList(),
    val usedBytes: Int? = null,

    // Données brutes (best-effort)
    val rawReadableBytes: Int? = null,
    val rawDumpFirstBytesHex: String? = null,

    // Avancé
    val countersHex: String? = null,
    val tearFlag: Boolean? = null,
    val eccSignatureHex: String? = null,
    val rfConfig: String? = null,

    // (Cartes sécurisées type DESFire : placeholders)
    val applications: List<String>? = null,
    val files: List<String>? = null,

    // ------ Ajouts MIFARE Classic ------
    val classicSectors: List<ClassicSectorInfo>? = null, // dump complet lisible
    val classicReadableSectors: Int? = null,             // compte lisibles
    val classicDiscoveredKeys: Int? = null               // nb de secteurs ouverts
)

/* ======================================================= */

@Suppress("SpellCheckingInspection")
object NfcInspector {

    fun inspect(tag: Tag): TagDetails {
        val uid = tag.id ?: ByteArray(0)
        val uidHex = uid.toHex()
        val nfca = NfcA.get(tag)
        val nfcb = NfcB.get(tag)
        val nfcf = NfcF.get(tag)
        val nfcv = NfcV.get(tag)
        val iso  = IsoDep.get(tag)
        val mu   = MifareUltralight.get(tag)
        val mfc  = MifareClassic.get(tag)
        val ndef = Ndef.get(tag)

        val techList = tag.techList.map { it.substringAfterLast('.') }.distinct()

        /* ---------- Identification ---------- */
        val atqaHex: String? = nfca?.atqa?.let { a ->
            if (a.size >= 2) byteArrayOf(a[0], a[1]).toHex() else null
        }
        val sakHex: String? = nfca?.let { byteArrayOf(it.sak.toByte()).toHex() }
        val manufacturer = guessManufacturer(uid)

        /* ---------- Système ---------- */
        var chipType: String? = null
        var totalBytes: Int? = null
        var memoryLayout: String? = null
        var versionHex: String? = null
        var atsHex: String? = null
        var historicalHex: String? = null
        var hiLayerHex: String? = null

        // Valeurs optionnelles Type 2 (Ultralight/NTAG)
        var head: ByteArray? = null
        var lockBitsHex: String? = null
        var otpHex: String? = null
        var ndefCapFromCC: Int? = null
        var countersHex: String? = null
        var eccSigHex: String? = null

        // --- Type 2 / NTAG via NfcA
        if (nfca != null && mfc == null) {
            val (ver, all, cnt, sig) = safeWith(nfca) {
                val v  = transceive(byteArrayOf(0x60))              // GET_VERSION
                val p0 = transceive(byteArrayOf(0x30, 0x00))        // READ 0..3
                val p4 = transceive(byteArrayOf(0x30, 0x04))        // READ 4..7
                val allHead = p0 + p4
                val c  = runCatching { transceive(byteArrayOf(0x39, 0x00)) }.getOrNull() // READ_CNT
                val s  = runCatching { transceive(byteArrayOf(0x3C, 0x00)) }.getOrNull() // READ_SIG
                Quad(v, allHead, c, s)
            } ?: Quad(null, null, null, null)

            if (ver != null && ver.size >= 8) {
                versionHex = ver.toHex()
                val blocks = (ver[6].toInt() and 0xFF) + 1
                if (blocks in 16..4096) totalBytes = blocks * 4
                chipType = when (ver[1].toInt() and 0xFF) {
                    0x04       -> "NTAG213/215/216"
                    0x0B, 0x0C -> "MIFARE Ultralight EV1"
                    else       -> "Type 2"
                }
            }

            head = all
            lockBitsHex = head?.let { if (it.size >= 12) byteArrayOf(it[10], it[11]).toHex() else null }
            val ccOrOtp = head?.let {
                if (it.size >= 16) {
                    val page3 = byteArrayOf(it[12], it[13], it[14], it[15])
                    if ((page3[0].toInt() and 0xFF) == 0xE1) {
                        ndefCapFromCC = (page3[2].toInt() and 0xFF) * 8
                        null
                    } else {
                        page3.toHex()
                    }
                } else null
            }
            otpHex = ccOrOtp

            if (totalBytes == null && head != null) totalBytes = (head.size / 4) * 4
            totalBytes?.let { memoryLayout = "${it / 4} pages × 4B ($it B)" }

            countersHex = cnt?.takeIf { it.size >= 3 }?.copyOfRange(0, 3)?.toHex()
            eccSigHex   = sig?.takeIf { it.size >= 32 }?.copyOfRange(sig.size - 32, sig.size)?.toHex()
        }

        // --- MIFARE Classic : taille + layout
        var classicDump: List<ClassicSectorInfo>? = null
        var classicReadable = 0
        var classicOpened = 0

        mfc?.let { c ->
            chipType = when (c.size) {
                1024 -> "MIFARE Classic 1K"
                4096 -> "MIFARE Classic 4K"
                else -> "MIFARE Classic"
            }
            totalBytes = c.size
            memoryLayout = "${c.sectorCount} secteurs • ${c.blockCount} blocs • 16B/bloc (${c.size}B)"

            val dump = dumpClassic(c)
            if (dump.isNotEmpty()) {
                classicDump = dump
                classicReadable = dump.count { it.blocks.any { b -> b.dataHex != null } }
                classicOpened = dump.count { it.usedKeyHex != null }
            }
        }

        // --- ISO-DEP (Type 4)
        iso?.let { d ->
            chipType = chipType ?: "Type 4 (ISO-DEP)"
            historicalHex = d.historicalBytes?.toHex()
            hiLayerHex = d.hiLayerResponse?.toHex()
            atsHex = historicalHex
        }

        return buildTagDetails(
            uidHex = uidHex,
            techList = techList,
            atqaHex = atqaHex,
            sakHex = sakHex,
            manufacturer = manufacturer,

            chipType = chipType,
            totalBytes = totalBytes,
            memoryLayout = memoryLayout,
            versionHex = versionHex,
            atsHex = atsHex,
            historicalHex = historicalHex,
            hiLayerHex = hiLayerHex,

            lockBitsHex = lockBitsHex,
            otpHex = otpHex,

            ndefCapacityFromCC = ndefCapFromCC,
            head = head,
            countersHex = countersHex,
            eccSigHex = eccSigHex,
            rfConfig = rfInfo(nfcb, nfcf, nfcv),

            mfc = mfc,
            mu  = mu,
            ndef = ndef,

            classicDump = classicDump,
            classicReadable = if (mfc != null) classicReadable else null,
            classicOpened = if (mfc != null) classicOpened else null
        )
    }

    /* ---------------- Construction finale ---------------- */

    private fun buildTagDetails(
        uidHex: String,
        techList: List<String>,
        atqaHex: String?,
        sakHex: String?,
        manufacturer: String?,

        chipType: String?,
        totalBytes: Int?,
        memoryLayout: String?,
        versionHex: String?,
        atsHex: String?,
        historicalHex: String?,
        hiLayerHex: String?,

        lockBitsHex: String?,
        otpHex: String?,

        ndefCapacityFromCC: Int?,
        head: ByteArray?,
        countersHex: String?,
        eccSigHex: String?,
        rfConfig: String?,

        mfc: MifareClassic?,
        mu: MifareUltralight?,
        ndef: Ndef?,

        classicDump: List<ClassicSectorInfo>?,
        classicReadable: Int?,
        classicOpened: Int?
    ): TagDetails {

        val chipTypeFinal = chipType ?: mu?.let {
            when (it.type) {
                MifareUltralight.TYPE_ULTRALIGHT   -> "MIFARE Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> null
            }
        }

        // Lecture NDEF best-effort
        var ndefCapacity: Int? = null
        var isWritable: Boolean? = null
        var canRO: Boolean? = null
        var ndefRecords: List<NdefRecordInfo> = emptyList()
        var usedBytes: Int? = null

        if (ndef != null) {
            runCatching { ndef.connect() }
            ndefCapacity = ndef.maxSize
            isWritable = ndef.isWritable
            canRO = runCatching { ndef.canMakeReadOnly() }.getOrNull()
            val msg = ndef.cachedNdefMessage ?: runCatching { ndef.ndefMessage }.getOrNull()
            if (msg != null) {
                usedBytes = msg.toByteArray().size
                ndefRecords = parseNdef(msg)
            }
            runCatching { ndef.close() }
        }
        if (ndefCapacityFromCC != null) ndefCapacity = ndefCapacityFromCC

        return TagDetails(
            uidHex = uidHex,
            techList = techList,

            atqaHex = atqaHex,
            sakHex = sakHex,
            manufacturer = manufacturer,

            chipType = chipTypeFinal,
            totalMemoryBytes = totalBytes,
            memoryLayout = memoryLayout ?: mfc?.let { "${it.sectorCount} secteurs • ${it.blockCount} blocs • 16B/bloc (${it.size}B)" },
            versionHex = versionHex,
            atsHex = atsHex,
            historicalBytesHex = historicalHex,
            hiLayerResponseHex = hiLayerHex,

            lockBitsHex = lockBitsHex,
            otpBytesHex = otpHex,
            isNdefWritable = isWritable,
            canMakeReadOnly = canRO,

            ndefCapacity = ndefCapacity,
            ndefRecords = ndefRecords,
            usedBytes = usedBytes,

            rawReadableBytes = head?.size,
            rawDumpFirstBytesHex = head?.copyOfRange(0, min(32, head.size))?.toHex(),

            countersHex = countersHex,
            tearFlag = null,
            eccSignatureHex = eccSigHex,
            rfConfig = rfConfig,

            applications = emptyList(),
            files = emptyList(),

            classicSectors = classicDump,
            classicReadableSectors = classicReadable,
            classicDiscoveredKeys = classicOpened
        )
    }

    /* ---------------- NDEF parsing ---------------- */

    private fun parseNdef(msg: NdefMessage): List<NdefRecordInfo> =
        msg.records.map { r ->
            if (r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(byteArrayOf(0x54))) {
                val p = r.payload
                if (p.isNotEmpty()) {
                    val langLen = p[0].toInt() and 0x1F
                    val cs = if ((p[0].toInt() and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
                    val text = String(p, 1 + langLen, p.size - (1 + langLen), cs)
                    return@map NdefRecordInfo("Text", text)
                }
            }
            if (r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(byteArrayOf(0x55))) {
                val p = r.payload
                val uri = if (p.isNotEmpty()) uriFromRtdU(p) else ""
                return@map NdefRecordInfo("URI", uri)
            }
            if (r.tnf == NdefRecord.TNF_MIME_MEDIA) {
                val mime = runCatching { String(r.type, Charsets.US_ASCII) }.getOrNull() ?: "MIME"
                val v = runCatching { String(r.payload, Charsets.UTF_8) }.getOrNull() ?: r.payload.toHex(64)
                return@map NdefRecordInfo(mime, v)
            }
            if (r.tnf == NdefRecord.TNF_EXTERNAL_TYPE) {
                val t = runCatching { String(r.type, Charsets.US_ASCII) }.getOrNull() ?: "ext"
                val v = r.payload.toHex(64)
                return@map NdefRecordInfo(t, v)
            }
            NdefRecordInfo("NDEF", r.payload.toHex(64))
        }

    /* ---------------- Helpers généraux ---------------- */

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

    private fun uriFromRtdU(payload: ByteArray): String {
        val id = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIX.getOrElse(id) { "" }
        val rest = runCatching { String(payload, 1, payload.size - 1, Charsets.UTF_8) }.getOrNull() ?: ""
        return prefix + rest
    }

    private fun guessManufacturer(uid: ByteArray): String? =
        if (uid.isEmpty()) null else when (uid[0].toInt() and 0xFF) {
            0x04 -> "NXP"
            0x02 -> "STMicro"
            0x07 -> "Texas Instruments"
            else -> null
        }

    /** Petit conteneur pour safeWith(NfcA) */
    private data class Quad<A, B, C, D>(val a: A?, val b: B?, val c: C?, val d: D?)

    /** Exécute un bloc sur NfcA en gérant connect/close proprement. */
    private inline fun <T> safeWith(a: NfcA, block: NfcA.() -> T): T? =
        runCatching { a.connect(); val r = a.block(); a.close(); r }
            .getOrElse { runCatching { a.close() }; null }

    /** Regroupe infos RF pour NfcB/F/V (best-effort). */
    private fun rfInfo(nfcb: NfcB?, nfcf: NfcF?, nfcv: NfcV?): String? {
        val b = nfcb?.let {
            val parts = mutableListOf<String>()
            it.applicationData?.toHex()?.let { v -> parts.add("NfcB App:$v") }
            it.protocolInfo?.toHex()?.let { v -> parts.add("Proto:$v") }
            parts.joinToString(" • ").ifBlank { null }
        }
        val f = nfcf?.let {
            val parts = mutableListOf<String>()
            val sys = it.systemCode?.let { sc -> if (sc.size >= 2) byteArrayOf(sc[0], sc[1]).toHex() else null }
            val man = it.manufacturer?.toHex()
            sys?.let { v -> parts.add("Sys:$v") }
            man?.let { v -> parts.add("Man:$v") }
            parts.joinToString(" • ").ifBlank { null }
        }
        val v = nfcv?.let {
            runCatching {
                it.connect()
                val uidLe = (it.tag.id ?: ByteArray(0)).reversedArray() // NfcV adresse en LE
                val resp = it.transceive(byteArrayOf(0x22, 0x2B) + uidLe) // Get System Info
                it.close()
                val chunk = if (resp.size > 32) resp.copyOfRange(0, 32) else resp
                "SysInfo:${chunk.toHex()}".ifBlank { null }
            }.getOrNull()
        }
        val parts = listOfNotNull(b, f, v)
        return parts.joinToString(" • ").ifBlank { null }
    }

    /* ===================== MIFARE Classic : dump complet ===================== */

    // Liste de clés courantes (Key A/B)
    private val commonKeys: List<ByteArray> = listOf(
        byteArrayOf(0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte()),
        byteArrayOf(0xA0.toByte(),0xA1.toByte(),0xA2.toByte(),0xA3.toByte(),0xA4.toByte(),0xA5.toByte()),
        byteArrayOf(0xD3.toByte(),0xF7.toByte(),0xD3.toByte(),0xF7.toByte(),0xD3.toByte(),0xF7.toByte()),
        byteArrayOf(0,0,0,0,0,0),
        byteArrayOf(0xB0.toByte(),0xB1.toByte(),0xB2.toByte(),0xB3.toByte(),0xB4.toByte(),0xB5.toByte()),
        byteArrayOf(0x4D,0x3A,0x99.toByte(),0xC3.toByte(),0x51,0xDD.toByte()),
        byteArrayOf(0x1A,0x2B,0x3C,0x4D,0x5E,0x6F)
    )

    private fun dumpClassic(mc: MifareClassic): List<ClassicSectorInfo> {
        val out = ArrayList<ClassicSectorInfo>()
        runCatching { mc.connect() }.onFailure { return emptyList() }
        mc.timeout = 1000 // ms, ajuste si besoin
        mc.use {
            val secCount = mc.sectorCount
            for (s in 0 until secCount) {
                var usedKey: ByteArray? = null
                var usedType: String? = null
                for (k in commonKeys) {
                    val aOk = runCatching { mc.authenticateSectorWithKeyA(s, k) }.getOrNull() == true
                    if (aOk) { usedKey = k; usedType = "KeyA"; break }
                    val bOk = runCatching { mc.authenticateSectorWithKeyB(s, k) }.getOrNull() == true
                    if (bOk) { usedKey = k; usedType = "KeyB"; break }
                }

                val first = mc.sectorToBlock(s)
                val count = mc.getBlockCountInSector(s)
                val blocks = ArrayList<ClassicBlockInfo>()
                var trailer: ClassicTrailer? = null

                if (usedKey != null) {
                    for (i in 0 until count) {
                        val abs = first + i
                        val raw = runCatching { mc.readBlock(abs) }.getOrNull()   // TagLost catché ici aussi
                        if (i == count - 1) {
                            trailer = raw?.let { parseTrailer(it) }
                        } else {
                            val vb = raw?.let { detectValueBlock(it) }
                            val access = trailer?.let { perBlockAccess(it, i) }
                            blocks += ClassicBlockInfo(
                                sector = s,
                                indexInSector = i,
                                absBlock = abs,
                                dataHex = raw?.toHex(),
                                isValue = vb?.first,
                                value = vb?.second,
                                addr = vb?.third,
                                access = access
                            )
                        }
                    }
                } else {
                    for (i in 0 until count) {
                        val abs = first + i
                        blocks += ClassicBlockInfo(
                            sector = s,
                            indexInSector = i,
                            absBlock = abs,
                            dataHex = null,
                            isValue = null,
                            value = null,
                            addr = null,
                            access = null
                        )
                    }
                }

                out += ClassicSectorInfo(
                    sector = s,
                    usedKeyType = usedType,
                    usedKeyHex = usedKey?.toHex(),
                    blockCount = count,
                    blocks = blocks,
                    trailer = trailer
                )
            }
        }
        return out
    }

    /** Détecte un value block selon le format MIFARE (val, ~val, val, addr, ~addr, addr, ~addr). */
    private fun detectValueBlock(b: ByteArray): Triple<Boolean, Int, Int>? {
        if (b.size != 16) return null
        val v0 = toIntLe(b, 0)
        val nv = toIntLe(b, 4)
        val v2 = toIntLe(b, 8)
        val a0 = b[12].toInt() and 0xFF
        val na = b[13].toInt() and 0xFF
        val a2 = b[14].toInt() and 0xFF
        val na2 = b[15].toInt() and 0xFF

        val valOk = (v0 == v2) && (nv == v0.inv())
        val addrOk = (a0 == a2) && (na == a0.inv() and 0xFF) && (na2 == na)
        val isValue = valOk && addrOk
        return Triple(isValue, if (isValue) v0 else 0, if (isValue) a0 else 0)
    }

    private fun toIntLe(b: ByteArray, off: Int): Int {
        val x = (b[off].toInt() and 0xFF) or
                ((b[off+1].toInt() and 0xFF) shl 8) or
                ((b[off+2].toInt() and 0xFF) shl 16) or
                ((b[off+3].toInt() and 0xFF) shl 24)
        return x
    }

    /** Trailer: KeyA(0..5), Access(6..8), GP(9), KeyB(10..15). */
    private fun parseTrailer(t: ByteArray): ClassicTrailer {
        if (t.size != 16) return ClassicTrailer(null, null, "indéterminé", null)
        val keyA = t.copyOfRange(0, 6).toHex()
        val acc = t.copyOfRange(6, 9)
        val keyB = t.copyOfRange(10, 16).toHex()
        val accHex = acc.toHex()

        val decoded = decodeAccessBits(acc)
        return ClassicTrailer(
            keyA = keyA,
            accessBitsHex = accHex,
            accessDecoded = decoded,
            keyB = keyB
        )
    }

    /**
     * Décodage C1/C2/C3 pour B0..B3 selon NXP datasheet.
     * Retour lisible du type: "B0: C1C2C3=... (rwx), B1: ..., B2: ..., TR: ..."
     */
    private fun decodeAccessBits(acc: ByteArray): String {
        if (acc.size < 3) return "invalide"
        // Bits agencés en nibbles: voir MF1S50yy datasheet section 8.6.2
        val c1 = ((acc[1].toInt() shr 4) and 0x0F)
        val c2 = (acc[2].toInt() and 0x0F)
        val c3 = ((acc[2].toInt() shr 4) and 0x0F)
        // Cx pour B3,B2,B1,B0 dans l’ordre des nibbles
        val trip = (0..3).map { i ->
            val b = when (i) {
                0 -> 3 // B3
                1 -> 2 // B2
                2 -> 1 // B1
                else -> 0 // B0
            }
            val ci1 = (c1 shr b) and 1
            val ci2 = (c2 shr b) and 1
            val ci3 = (c3 shr b) and 1
            Triple(ci1, ci2, ci3)
        }
        // Traduction simple
        fun perm(t: Triple<Int,Int,Int>, trailer: Boolean): String {
            val (c1, c2, c3) = t
            // Tableau minimal lisible; pour détail complet, mapper avec datasheet
            return if (!trailer) {
                when("$c1$c2$c3") {
                    "000" -> "Data rwx (KeyA/B)"
                    "010" -> "Read (KeyA/B), Write(KeyB)"
                    "100" -> "Read (KeyA/B), no Write"
                    "110" -> "Read (KeyA/B), Decr/Inc(KeyB)"
                    "001" -> "Read(KeyB), Write(KeyB)"
                    "011" -> "Read(KeyB), no Write"
                    "101" -> "No read, write(KeyB)"
                    "111" -> "No read, no write"
                    else -> "unk"
                }
            } else {
                when("$c1$c2$c3") {
                    "000" -> "Trailer: KeyA r, Access w(KeyA/B), KeyB r"
                    "010" -> "Trailer: KeyA r, Access w(KeyB), KeyB r"
                    "100" -> "Trailer: KeyA r, Access w(KeyA), KeyB r"
                    "110" -> "Trailer: KeyA r, no Access w, KeyB r"
                    "001" -> "Trailer: KeyA r, Access w(KeyB), KeyB r/w"
                    "011" -> "Trailer: KeyA r, no Access w, KeyB r/w"
                    "101" -> "Trailer: KeyA r/w, Access w(KeyB), KeyB r/w"
                    "111" -> "Trailer: KeyA r/w, no Access w, KeyB r/w"
                    else -> "Trailer: unk"
                }
            }
        }
        val b3 = "B3: " + perm(trip[0], trailer = true)
        val b2 = "B2: " + perm(trip[1], trailer = false)
        val b1 = "B1: " + perm(trip[2], trailer = false)
        val b0 = "B0: " + perm(trip[3], trailer = false)
        return listOf(b0, b1, b2, b3).joinToString(" | ")
    }

    /** Renvoie une chaîne d’accès par bloc si trailer connu. */
    private fun perBlockAccess(tr: ClassicTrailer, blockIndexInSector: Int): String? {
        val parts = tr.accessDecoded.split(" | ")
        return when (blockIndexInSector) {
            0 -> parts.find { it.startsWith("B0:") }
            1 -> parts.find { it.startsWith("B1:") }
            2 -> parts.find { it.startsWith("B2:") }
            else -> null
        }
    }
}
