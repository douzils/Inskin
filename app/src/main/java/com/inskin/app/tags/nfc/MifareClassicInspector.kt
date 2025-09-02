// tags/nfc/MifareClassicInspector.kt
package com.inskin.app.tags.nfc

import android.nfc.tech.MifareClassic
import com.inskin.app.TagDetails
import com.inskin.app.ClassicSectorInfo
import com.inskin.app.ClassicBlockInfo
import com.inskin.app.ClassicTrailer
import com.inskin.app.tags.TagInspector
import com.inskin.app.tags.Channel
import com.inskin.app.tags.WriteOp
import com.inskin.app.tags.WriteReport
import com.inskin.app.tags.InspectorUtils

object MifareClassicInspector : TagInspector {

    override fun supports(channel: Channel): Boolean =
        channel is Channel.Android && MifareClassic.get(channel.tag) != null

    override fun readRaw(channel: Channel): ByteArray? = null

    override fun decode(channel: Channel, raw: ByteArray?): TagDetails {
        val tag = (channel as Channel.Android).tag
        val mc = MifareClassic.get(tag)!!
        val uid = (tag.id ?: ByteArray(0))
        val uidHex = uid.toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }.distinct()
        val chip = when (mc.size) { 1024 -> "MIFARE Classic 1K"; 4096 -> "MIFARE Classic 4K"; else -> "MIFARE Classic" }
        val layout = "${mc.sectorCount} secteurs • ${mc.blockCount} blocs • 16B/bloc (${mc.size}B)"

        InspectorUtils.emitLog?.invoke("MIFARE Classic: UID=$uidHex, $chip, $layout")

        val (sectors, apps) = dumpWithDictionary(
            mc,
            (channel as Channel.Android).tag.id ?: ByteArray(0),
            uidHex
        )

        val readable = sectors.count { s -> s.blocks.any { it.dataHex != null } }
        val opened = sectors.count { it.usedKeyHex != null }

        return TagDetails(
            uidHex = uidHex,
            techList = techs,
            chipType = chip,
            totalMemoryBytes = mc.size,
            memoryLayout = layout,
            classicSectors = sectors,
            classicReadableSectors = readable,
            classicDiscoveredKeys = opened,
            applications = apps
        )
    }

    override fun write(channel: Channel, ops: List<WriteOp>): WriteReport {
        val tag =
            (channel as? Channel.Android)?.tag ?: return WriteReport(false, "Canal Android requis")
        val mc = MifareClassic.get(tag) ?: return WriteReport(false, "MIFARE Classic requis")
        return runCatching {
            mc.connect()
            for (op in ops) {
                when (op) {
                    is WriteOp.ClassicBlock -> {
                        require(op.data.size == 16) { "Bloc Classic = 16 octets" }
                        mc.writeBlock(op.block, op.data)
                        InspectorUtils.emitLog?.invoke("Écrit bloc ${op.block} : ${op.data.toHex(16)}")
                    }

                    else -> return WriteReport(false, "Non supporté: ${op::class.simpleName}")
                }
            }
            mc.close()
            WriteReport(true, "Écriture Classic OK")
        }.getOrElse { e ->
            runCatching { mc.close() }
            WriteReport(false, e.message ?: "Échec écriture Classic")
        }
    }

    // ---------- internals ----------
// --- Logs trailer
    private fun logTrailer(sector: Int, raw: ByteArray, decoded: String) {
        InspectorUtils.emitLog?.invoke("Trailer S$sector: ${raw.toHex(16)} | $decoded")
    }

    // ========== Bloc: clés de base et bague ==========
    private val BASE_KEY_HEX = listOf(
        "FFFFFFFFFFFF", "A0A1A2A3A4A5", "D3F7D3F7D3F7", "000000000000", "B0B1B2B3B4B5",
        "4D3A99C351DD", "1A2B3C4D5E6F", "AABBCCDDEEFF", "1234567890AB", "010203040506",
        "001122334455", "999999999999", "AA55AA55AA55", "A1B2C3D4E5F6", "2C7E151628AE",
        "5A5A5A5A5A5A", "6A6A6A6A6A6A", "000001020304", "5F5F5F5F5F5F", "1A982C7E459A",
        "A0478CC39091", "FFFFFFFFFFF0", "F0F1F2F3F4F5", "CAFEBABE0000"
        ,"A0B0C0D0E0F0","FFFFFFFFFFFF","A1A2A3A4A5A6","B1B2B3B4B5B6","4B791EBEDE71",
        "00000A4B5C6D","AABBCCDDEEFF","D3F7D3F7D3F7","A0A1A2A3A4A5","1A982C7E459A",
        "714C5C886E97","8FD0A4F256E9","F9E98A3C5D2E","A9B8C7D6E5F4","FFFFFFFFFFF0",
        "000000000001","000000000002","00000000000A","5F078D4C90FF","D0D1D2D3D4D5"

    )

    private fun baseKeys(): MutableList<ByteArray> = BASE_KEY_HEX.map { hex6(it) }.toMutableList()

    private fun allKeys(uid: ByteArray?): MutableList<ByteArray> {
        val mct = InspectorUtils.extraKeysProvider?.invoke().orElse(emptyList())
        val ring = (derivedKeysFromUid(uid) + baseKeys() + mct)
            .distinctBy { it.joinToString("") { b -> "%02X".format(b) } }
            .toMutableList()
        return ring
    }
    private fun <T> T?.orElse(fallback: T) = this ?: fallback


    // ========== Bloc: TLV + NDEF ==========
// --- TLV + NDEF
    private fun extractTlv(whole: ByteArray, tlvType: Int = 0x03): ByteArray? {
        var i = 0
        while (i < whole.size) {
            val t = whole[i].toInt() and 0xFF
            if (t == 0x00) {
                i += 1; continue
            }
            if (t == 0xFE) break
            if (i + 1 >= whole.size) break
            val l = whole[i + 1].toInt() and 0xFF
            val len: Int
            var header = 2
            if (l == 0xFF) {
                if (i + 3 >= whole.size) break
                len = ((whole[i + 2].toInt() and 0xFF) shl 8) or (whole[i + 3].toInt() and 0xFF)
                header = 4
            } else len = l
            val start = i + header
            val end = start + len
            if (end > whole.size) break
            if (t == tlvType) return whole.copyOfRange(start, end)
            i = end
        }
        return null
    }

    private fun rebuildNdefPayload(
        sectors: List<ClassicSectorInfo>,
        madMap: Map<Int, Int>
    ): ByteArray? {
        val data = ArrayList<Byte>()
        for ((sector, aid) in madMap) {
            if (!isNdefAid(aid)) continue
            val sec = sectors.getOrNull(sector) ?: continue
            sec.blocks.forEach { b ->
                if (b.indexInSector == (sec.blockCount - 1)) return@forEach
                val raw = b.dataHex?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
                if (raw != null) data.addAll(raw.toList())
            }
        }
        if (data.isEmpty()) return null
        return extractTlv(data.toByteArray(), 0x03)
    }

    private fun parseNdefMessage(ndef: ByteArray): List<String> {
        val out = ArrayList<String>()
        var i = 0
        while (i < ndef.size) {
            val hdr = ndef[i].toInt() and 0xFF; i += 1
            if (i >= ndef.size) break
            val typeLen = ndef[i].toInt() and 0xFF; i += 1
            val sr = (hdr and 0x10) != 0
            val il = (hdr and 0x08) != 0
            val payloadLen: Int = if (sr) {
                val v = ndef[i].toInt() and 0xFF; i += 1; v
            } else {
                if (i + 3 >= ndef.size) break
                val v = ((ndef[i].toInt() and 0xFF) shl 24) or
                        ((ndef[i + 1].toInt() and 0xFF) shl 16) or
                        ((ndef[i + 2].toInt() and 0xFF) shl 8) or
                        (ndef[i + 3].toInt() and 0xFF)
                i += 4; v
            }
            val idLen = if (il) {
                val v = ndef[i].toInt() and 0xFF; i += 1; v
            } else 0
            if (i + typeLen + idLen + payloadLen > ndef.size) break
            val type = ndef.copyOfRange(i, i + typeLen); i += typeLen
            val id = if (il) ndef.copyOfRange(i, i + idLen).also { i += idLen } else ByteArray(0)
            val payload = ndef.copyOfRange(i, i + payloadLen); i += payloadLen

            val t = String(type)
            val summary = when (t) {
                "U" -> {
                    val prefixes = arrayOf(
                        "", "http://www.", "https://www.", "http://", "https://",
                        "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
                        "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
                        "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
                        "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
                        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
                        "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:",
                        "urn:nfc:"
                    )
                    if (payload.isNotEmpty()) {
                        val code = payload[0].toInt() and 0xFF
                        val uri = (prefixes.getOrNull(code) ?: "") + String(
                            payload.copyOfRange(
                                1,
                                payload.size
                            )
                        )
                        "URI: $uri"
                    } else "URI: (vide)"
                }

                "T" -> {
                    if (payload.isNotEmpty()) {
                        val langLen = payload[0].toInt() and 0x3F
                        val text = payload.copyOfRange(1 + langLen, payload.size)
                        "TEXT: ${String(text)}"
                    } else "TEXT: (vide)"
                }

                else -> "RTD:$t len=${payload.size} id=${id.toHex()}"
            }
            out += summary
        }
        return out
    }

    // ========== Bloc: priorisation des clés par secteur ==========
// --- Priorisation clés / lecture / logs
    private fun prioritizedKeysForSector(sector: Int, keyRing: List<ByteArray>, madMap: Map<Int,Int>): List<ByteArray> {
        val pri = ArrayList<ByteArray>()
        if (madMap[sector]?.let { isNdefAid(it) } == true) {
            // Met en tête les plus classiques + 20 premières du dictionnaire MCT
            pri += listOf("A0A1A2A3A4A5","FFFFFFFFFFFF","000000000000").map { hex6(it) }
            pri += keyRing.take(20)
            pri += keyRing.drop(20)
        } else {
            pri += keyRing
        }
        return pri.distinctBy { it.joinToString("") { b -> "%02X".format(b) } }
    }
    private fun hex6(hex: String): ByteArray =
        ByteArray(6) { i -> ((Character.digit(hex[i*2],16) shl 4) or Character.digit(hex[i*2+1],16)).toByte() }

    // --- dumpWithDictionary: version complète avec madMap hors du try
    private fun dumpWithDictionary(
        mc: MifareClassic,
        uid: ByteArray,
        uidHex: String
    ): Pair<List<ClassicSectorInfo>, List<String>> {
        val out = ArrayList<ClassicSectorInfo>()
        val apps = ArrayList<String>()
        val keyRing = allKeys(uid)
        var madMap: Map<Int, Int> = emptyMap()

        if (!runCatching { mc.connect() }.isSuccess) {
            InspectorUtils.emitLog?.invoke("Connexion échouée.")
            return emptyList<ClassicSectorInfo>() to emptyList()
        }
        mc.timeout = 1000
        InspectorUtils.emitLog?.invoke("Connecté. Secteurs=${mc.sectorCount}, blocs=${mc.blockCount}")

        try {
            madMap = readMad(mc)
            if (madMap.isNotEmpty()) {
                madMap.forEach { (s, aid) ->
                    apps += "Secteur $s → AID 0x${aid.toString(16).uppercase().padStart(4,'0')} ${aidName(aid)}"
                }
                InspectorUtils.emitLog?.invoke("MAD détecté (${madMap.size} entrées).")
            }

            val learned = HashSet<String>()

            repeat(3) { pass ->                       // 2→3 passes
                for (s in 0 until mc.sectorCount) {
                    if (out.size > s && out[s].usedKeyHex != null && pass == 0) continue

                    var usedKey: ByteArray? = null
                    var usedType: String? = null

                    val tryList = prioritizedKeysForSector(s, keyRing, madMap)
                    for (k in tryList) {
                        if (mc.authenticateSectorWithKeyA(s, k)) { usedKey = k; usedType = "KeyA"; break }
                        if (mc.authenticateSectorWithKeyB(s, k)) { usedKey = k; usedType = "KeyB"; break }
                    }

                    val first = mc.sectorToBlock(s)
                    val count = mc.getBlockCountInSector(s)
                    val blocks = ArrayList<ClassicBlockInfo>()
                    var trailer: ClassicTrailer? = null

                    if (usedKey != null) {
                        val keyHex = usedKey.toHex(12)
                        if (learned.add(keyHex)) keyRing.add(0, usedKey)

                        val (blk, tr) = readSectorRobust(mc, s, uidHex, usedKey, usedType!!, keyRing)
                        blocks += blk
                        trailer = tr

                        // Propagation globale de clé (pas seulement voisins)
                        val kbHex = trailer?.keyB
                        if (!kbHex.isNullOrBlank() && kbHex != "FFFFFFFFFFFF" && kbHex != "000000000000") {
                            val kb = hex6(kbHex)
                            floodAllLockedWithKey(mc, kb, out, uidHex)
                        }
                    } else {
                        if (pass == 0) InspectorUtils.emitLog?.invoke("Sec $s auth KO")
                        repeat(count) { i ->
                            val abs = first + i
                            blocks += ClassicBlockInfo(s, i, abs, null, null, null, null, null)
                        }
                    }

                    val secInfo = ClassicSectorInfo(
                        sector = s, usedKeyType = usedType, usedKeyHex = usedKey?.toHex(),
                        blockCount = count, blocks = blocks, trailer = trailer
                    )
                    if (out.size > s) out[s] = secInfo else out += secInfo
                }
            }
        } finally {
            runCatching { mc.close() }
            InspectorUtils.emitLog?.invoke("Déconnexion.")
        }

        if (madMap.values.any { isNdefAid(it) }) {
            val ndefBytes = rebuildNdefPayload(out, madMap)
            if (ndefBytes != null) {
                val recs = parseNdefMessage(ndefBytes)
                if (recs.isNotEmpty()) {
                    recs.forEach { apps += "NDEF: $it" }
                    InspectorUtils.emitLog?.invoke("NDEF détecté (${recs.size} enregistrements)")
                }
            }
        }
        return out to apps
    }

    private fun floodAllLockedWithKey(
        mc: MifareClassic,
        key: ByteArray,
        out: MutableList<ClassicSectorInfo>,
        uidHex: String
    ) {
        for (s in 0 until mc.sectorCount) {
            if (out.size > s && out[s].usedKeyHex != null) continue
            val authedA = mc.authenticateSectorWithKeyA(s, key)
            val authedB = if (!authedA) mc.authenticateSectorWithKeyB(s, key) else false
            if (!authedA && !authedB) continue
            val usedType = if (authedA) "KeyA" else "KeyB"

            val first = mc.sectorToBlock(s)
            val count = mc.getBlockCountInSector(s)
            val blocks = ArrayList<ClassicBlockInfo>()
            var trailer: ClassicTrailer? = null

            for (i in 0 until count) {
                val abs = first + i
                val raw = runCatching { mc.readBlock(abs) }.getOrNull()
                if (i == count - 1) {
                    raw?.let {
                        val tr = parseTrailer(it); trailer = tr
                        logTrailer(s, it, tr.accessDecoded)
                    }
                } else {
                    val vb = raw?.let { detectValueBlock(it) }
                    blocks += ClassicBlockInfo(s, i, abs, raw?.toHex(), vb?.first, vb?.second, vb?.third, null)
                }
            }
            val secInfo = ClassicSectorInfo(s, usedType, key.toHex(), count, blocks, trailer)
            if (out.size > s) out[s] = secInfo else out += secInfo
            InspectorUtils.onKeyLearned?.invoke(uidHex, s, usedType, key.toHex(12))
        }
    }

    private fun derivedKeysFromUid(uid: ByteArray?): List<ByteArray> {
        if (uid == null || uid.isEmpty()) return emptyList()
        fun hexToBytes12(hex: String): ByteArray =
            ByteArray(6) { i -> ((Character.digit(hex[i*2],16) shl 4) or Character.digit(hex[i*2+1],16)).toByte() }

        val h = uid.joinToString("") { "%02X".format(it) }
        val first6 = (h + "000000000000").take(12)
        val rev6 = first6.chunked(2).reversed().joinToString("")
        val inv6 = first6.chunked(2).joinToString("") { "%02X".format((it.toInt(16) xor 0xFF)) }
        // variantes courantes vues sur terrain
        val repeat4 = ByteArray(6) { idx -> uid[idx % minOf(uid.size,4)] }.joinToString("") { "%02X".format(it) }

        return listOf(first6, rev6, inv6, repeat4)
            .distinct()
            .map(::hexToBytes12)
    }

    // ========== Bloc: MAD v1/v2 lecture et parsing ==========
// --- MAD v1/v2
    private fun readMad(mc: MifareClassic): Map<Int, Int> {
        val out = LinkedHashMap<Int, Int>()
        val keyA = hex6("A0A1A2A3A4A5")
        val ffff = hex6("FFFFFFFFFFFF")

        fun authA(sector: Int): Boolean =
            mc.authenticateSectorWithKeyA(sector, keyA) || mc.authenticateSectorWithKeyA(
                sector,
                ffff
            )

        // v1: secteur 0, blocs 1..2
        if (authA(0)) {
            val b1 = runCatching { mc.readBlock(1) }.getOrNull()
            val b2 = runCatching { mc.readBlock(2) }.getOrNull()
            if (b1 != null && b2 != null) {
                val bytes = b1 + b2
                parseMadDirectory(
                    bytes,
                    startSector = 1,
                    maxEntries = minOf(16, mc.sectorCount - 1)
                )
                    .forEach { (sec, aid) -> out[sec] = aid }
            }
        }
        // v2: secteur 16, blocs 0..2
        if (mc.sectorCount > 16 && authA(16)) {
            val base = mc.sectorToBlock(16)
            val d0 = runCatching { mc.readBlock(base + 0) }.getOrNull()
            val d1 = runCatching { mc.readBlock(base + 1) }.getOrNull()
            val d2 = runCatching { mc.readBlock(base + 2) }.getOrNull()
            if (d0 != null && d1 != null && d2 != null) {
                val bytes = d0 + d1 + d2
                val upperMax = minOf(39, mc.sectorCount - 1)
                parseMadDirectory(bytes, startSector = 17, maxEntries = (upperMax - 17 + 1))
                    .forEach { (sec, aid) -> out[sec] = aid }
            }
        }
        return out
    }

    private fun parseMadDirectory(
        dir: ByteArray,
        startSector: Int,
        maxEntries: Int
    ): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        var sector = startSector
        var i = 0
        while (i + 2 < dir.size && out.size < maxEntries) {
            val aid = ((dir[i].toInt() and 0xFF) shl 8) or (dir[i + 1].toInt() and 0xFF)
            // dir[i+2] = CRC ignorée
            if (aid != 0x0000) out += sector to aid
            sector += 1
            i += 3
        }
        return out
    }

    private fun isNdefAid(aid: Int): Boolean = aid == 0x03E1 || aid == 0xE103
    private fun aidName(aid: Int): String = when (aid) {
        0x03E1, 0xE103 -> "NDEF"
        0x1003 -> "MIFARE Application Dir"
        else -> "App 0x${aid.toString(16).uppercase()}"
    }


// ========== Bloc: Value Blocks ==========
    /** Retourne Triple(isValue, value?, addr?) si plausible, sinon null. */
// --- Value blocks
    private fun detectValueBlock(data16: ByteArray): Triple<Boolean, Int?, Int?>? {
        if (data16.size != 16) return null
        val v0 = java.nio.ByteBuffer.wrap(data16, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        val v1 = java.nio.ByteBuffer.wrap(data16, 8, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        val nv = data16.copyOfRange(4, 8).map { it.toInt() and 0xFF }
        val invV0 = byteArrayOf(
            (v0 and 0xFF).inv().toByte(),
            ((v0 shr 8) and 0xFF).inv().toByte(),
            ((v0 shr 16) and 0xFF).inv().toByte(),
            ((v0 shr 24) and 0xFF).inv().toByte()
        ).map { it.toInt() and 0xFF }

        val addr = data16[12].toInt() and 0xFF
        val naddr = data16[13].toInt() and 0xFF
        val addr2 = data16[14].toInt() and 0xFF
        val naddr2 = data16[15].toInt() and 0xFF

        val valueOk = (nv == invV0) && (v0 == v1)
        val addrOk = (addr == addr2) && (naddr == naddr2) && (addr == (naddr xor 0xFF))
        return if (valueOk && addrOk) Triple(true, v0, addr) else null
    }

    // ========== Bloc: Trailer + Access Bits ==========
// --- Trailer + droits
    private fun parseTrailer(trailer16: ByteArray): ClassicTrailer {
        require(trailer16.size == 16)
        val keyA = trailer16.copyOfRange(0, 6).toHex(12)
        val access = trailer16.copyOfRange(6, 10)
        val keyB = trailer16.copyOfRange(10, 16).toHex(12)
        val decoded = decodeAccessBitsStrict(access)
        return ClassicTrailer(
            keyA = keyA,
            keyB = keyB,
            accessBitsHex = access.toHex(),   // nom de champ courant
            accessDecoded = decoded
        )
    }

    private fun perBlockAccess(trailer: ClassicTrailer, indexInSector: Int): String {
        val parts = trailer.accessDecoded.split(" | ")
        return parts.getOrNull(indexInSector) ?: trailer.accessDecoded
    }


    private fun ByteArray.toHex(group: Int = 0): String {
        val s = joinToString("") { "%02X".format(it) }
        if (group <= 0) return s
        return s.chunked(group).joinToString(" ")
    }
    // Réalise une lecture complète d’un secteur avec une clé donnée.
// Si beaucoup d’échecs de lecture et qu’on récupère l’autre clé dans le trailer,
// on ré-authentifie avec l’autre clé et on relit le secteur.
// --- Lecture robuste d’un secteur + ré-auth éventuelle avec l’autre clé
    private fun readSectorRobust(
        mc: MifareClassic,
        sector: Int,
        uidHex: String,
        firstKey: ByteArray,
        firstType: String,
        keyRing: MutableList<ByteArray>
    ): Pair<List<ClassicBlockInfo>, ClassicTrailer?> {

        val firstBlock = mc.sectorToBlock(sector)
        val count = mc.getBlockCountInSector(sector)

        fun tryReadWith(key: ByteArray, type: String): Pair<List<ClassicBlockInfo>, ClassicTrailer?> {
            val blocks = ArrayList<ClassicBlockInfo>()
            var trailer: ClassicTrailer? = null
            var readFails = 0

            val authed = if (type == "KeyA") mc.authenticateSectorWithKeyA(sector, key)
            else mc.authenticateSectorWithKeyB(sector, key)
            if (!authed) return emptyList<ClassicBlockInfo>() to null

            for (i in 0 until count) {
                val abs = firstBlock + i
                val raw = runCatching { mc.readBlock(abs) }.getOrNull()
                if (i == count - 1) {
                    raw?.let {
                        val tr = parseTrailer(it)
                        trailer = tr
                        logTrailer(sector, it, tr.accessDecoded)
                        val kb = tr.keyB
                        if (!kb.isNullOrBlank() && kb != "FFFFFFFFFFFF" && kb != "000000000000") {
                            val kbBytes = hex6(kb)
                            val kbHex = kbBytes.toHex(12)
                            if (keyRing.none { it.toHex(12) == kbHex }) keyRing.add(0, kbBytes)
                            InspectorUtils.onKeyLearned?.invoke(uidHex, sector, "KeyB(trailer)", kbHex)
                        }
                    }
                } else {
                    if (raw == null) readFails++
                    val vb = raw?.let { detectValueBlock(it) }
                    val access = trailer?.let { perBlockAccess(it, i) }
                    blocks += ClassicBlockInfo(sector, i, abs, raw?.toHex(), vb?.first, vb?.second, vb?.third, access)
                }
            }

            // Si beaucoup d'échecs, tente le **même** octet de clé avec l'autre type (A<->B).
            if (readFails > (count - 1) / 2) {
                val other = if (type == "KeyA") "KeyB" else "KeyA"
                val again = tryReadWith(key, other)
                if (again.first.isNotEmpty()) return again
                // Sinon, si KeyB du trailer lisible, déjà géré plus bas.
                trailer?.keyB?.let { kbHex ->
                    if (type == "KeyA") {
                        val kb = hex6(kbHex)
                        val res = tryReadWith(kb, "KeyB")
                        if (res.first.isNotEmpty()) return res
                    }
                }
            }
            return blocks to trailer
        }

        return tryReadWith(firstKey, firstType)
    }

    // Tente une clé fraichement apprise sur les secteurs voisins encore fermés.
// --- Propagation de clé vers les secteurs voisins encore fermés
    private fun floodNeighborsWithKey(
        mc: MifareClassic,
        key: ByteArray,
        out: MutableList<ClassicSectorInfo>,
        uidHex: String
    ) {
        for (s in 0 until mc.sectorCount) {
            if (out.size > s && out[s].usedKeyHex != null) continue
            val authedA = mc.authenticateSectorWithKeyA(s, key)
            val authedB = if (!authedA) mc.authenticateSectorWithKeyB(s, key) else false
            if (!authedA && !authedB) continue
            val usedType = if (authedA) "KeyA" else "KeyB"

            val first = mc.sectorToBlock(s)
            val count = mc.getBlockCountInSector(s)
            val blocks = ArrayList<ClassicBlockInfo>()
            var trailer: ClassicTrailer? = null

            for (i in 0 until count) {
                val abs = first + i
                val raw = runCatching { mc.readBlock(abs) }.getOrNull()
                if (i == count - 1) {
                    raw?.let {
                        val tr = parseTrailer(it)
                        trailer = tr
                        logTrailer(s, it, tr.accessDecoded)
                    }
                } else {
                    val vb = raw?.let { detectValueBlock(it) }
                    blocks += ClassicBlockInfo(s, i, abs, raw?.toHex(), vb?.first, vb?.second, vb?.third, null)
                }
            }

            val secInfo = ClassicSectorInfo(
                sector = s, usedKeyType = usedType, usedKeyHex = key.toHex(),
                blockCount = count, blocks = blocks, trailer = trailer
            )
            if (out.size > s) out[s] = secInfo else out += secInfo
            InspectorUtils.onKeyLearned?.invoke(uidHex, s, usedType, key.toHex(12))
        }
    }
    // ---- Décode accès C1/C2/C3 avec vérification stricte + droits humains (NXP Table 7/8)
    private data class BlockRights(
        val read: String, val write: String, val inc: String, val dtr: String // dtr = dec/transfer/restore
    )
    private data class TrailerRights(
        val keyARead: String, val keyAWrite: String,
        val accessBitsRead: String, val accessBitsWrite: String,
        val keyBRead: String, val keyBWrite: String,
        val keyBUsableForAuth: Boolean
    )

    /** Retourne "B0:... | B1:... | B2:... | T:..." et contrôle des compléments. */
// ---- Décode accès C1/C2/C3 (4 octets: bytes 6..9 du trailer)
    private fun decodeAccessBitsStrict(abcd: ByteArray): String {
        require(abcd.size == 4)
        val (ok, c) = parseAccessTriplets(abcd)
        val rightsB0 = dataBlockRights(c[0])
        val rightsB1 = dataBlockRights(c[1])
        val rightsB2 = dataBlockRights(c[2])
        val tr = trailerRights(c[3])
        val flag = if (ok) "OK" else "INVALID"
        return buildString {
            append("[$flag] ")
            append("B0(R=${rightsB0.read},W=${rightsB0.write},I=${rightsB0.inc},DTR=${rightsB0.dtr}) | ")
            append("B1(R=${rightsB1.read},W=${rightsB1.write},I=${rightsB1.inc},DTR=${rightsB1.dtr}) | ")
            append("B2(R=${rightsB2.read},W=${rightsB2.write},I=${rightsB2.inc},DTR=${rightsB2.dtr}) | ")
            append("T(KaR=${tr.keyARead},KaW=${tr.keyAWrite},ABR=${tr.accessBitsRead},ABW=${tr.accessBitsWrite},KbR=${tr.keyBRead},KbW=${tr.keyBWrite},KbAuth=${tr.keyBUsableForAuth})")
        }
    }


    /** Parse C1/C2/C3 non-inversés pour blocs 0..3 + vérifie les compléments (Fig.10). */
    private fun parseAccessTriplets(abcd: ByteArray): Pair<Boolean, IntArray> {
        // Byte6: C2[3..0] (bits7..4), C1[3..0] (bits3..0)
        // Byte7: C1[3..0] (bits7..4), C3[3..0] (bits3..0)
        // Byte8: C3[3..0] (bits7..4), C2[3..0] (bits3..0)
        // Byte9: user data (mêmes droits que bytes 6..8)
        val b6 = abcd[0].toInt() and 0xFF
        val b7 = abcd[1].toInt() and 0xFF
        val b8 = abcd[2].toInt() and 0xFF
        // non-inverted
        val c1 = ((b7 ushr 4) and 0x0F)
        val c2 = (b6 ushr 4) and 0x0F
        val c3 = (b8 ushr 4) and 0x0F
        // inverted stored parts
        val c1n = b6 and 0x0F
        val c2n = b8 and 0x0F
        val c3n = b7 and 0x0F
        val ok = (c1 xor c1n) == 0x0F && (c2 xor c2n) == 0x0F && (c3 xor c3n) == 0x0F
        val trip = IntArray(4) { i -> // 0..2 data, 3 trailer
            val b = 1 shl i
            ((if ((c1 and b) != 0) 1 else 0) shl 2) or
                    ((if ((c2 and b) != 0) 1 else 0) shl 1) or
                    ( if ((c3 and b) != 0) 1 else 0)
        }
        return ok to trip
    }

    /** Table 8 (NXP MF1S50YYX): droits data block par (C1C2C3). */
    private fun dataBlockRights(code: Int): BlockRights = when (code) {
        0b000 -> BlockRights("A|B","A|B","A|B","A|B") // transport
        0b010 -> BlockRights("A|B","never","never","never")
        0b100 -> BlockRights("A|B","B","never","never")
        0b110 -> BlockRights("A|B","B","B","A|B")     // value
        0b001 -> BlockRights("A|B","never","never","A|B") // value (no inc)
        0b011 -> BlockRights("B","B","never","never")
        0b101 -> BlockRights("B","never","never","never")
        0b111 -> BlockRights("never","never","never","never")
        else -> BlockRights("unknown","unknown","unknown","unknown")
    }

    /** Table 7 (NXP MF1S50YYX): droits sector trailer par (C1C2C3). */
    private fun trailerRights(code: Int): TrailerRights = when (code) {
        0b000 -> TrailerRights("never","A","A","never","A","A", true) // Kb readable => utilisable (NXP note: oui)
        0b010 -> TrailerRights("never","never","A","never","A","never", true)
        0b100 -> TrailerRights("never","B","A|B","never","never","B", false)
        0b110 -> TrailerRights("never","never","A|B","never","never","never", false)
        0b001 -> TrailerRights("never","A","A","A","A","A", true)     // transport
        0b011 -> TrailerRights("never","B","A|B","B","never","B", false)
        0b101 -> TrailerRights("never","never","A|B","B","never","never", false)
        0b111 -> TrailerRights("never","never","A|B","never","never","never", false)
        else -> TrailerRights("unk","unk","unk","unk","unk","unk", false)
    }


    // ---- Parser simple de fichiers MCT: lignes "ffffffffffff", commentaires ;#//, hex 12 chars
    private fun parseMctKeys(text: String): List<ByteArray> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(";") && !it.startsWith("//") }
            .mapNotNull { line ->
                val hex = line.replace(" ", "").uppercase()
                if (hex.length == 12 && hex.all { c -> c in "0123456789ABCDEF" }) hex6(hex) else null
            }
            .toList()

    // ---- Parser "sector:keyA,keyB" optionnel (non standard MCT mais utile)
    private fun parseSectorKeyMap(text: String): Map<Int, Pair<ByteArray?, ByteArray?>> =
        buildMap {
            text.lineSequence().forEach { ln ->
                val line = ln.substringBefore('#').substringBefore(';').substringBefore("//").trim()
                if (line.isEmpty()) return@forEach
                val parts = line.split(':')
                if (parts.size != 2) return@forEach
                val s = parts[0].trim().toIntOrNull() ?: return@forEach
                val ks = parts[1].split(',').map { it.trim().uppercase() }
                val ka = ks.getOrNull(0)?.takeIf { it.length==12 }?.let { hex6(it) }
                val kb = ks.getOrNull(1)?.takeIf { it.length==12 }?.let { hex6(it) }
                put(s, ka to kb)
            }
        }
    // ---- Décodage GPB de MAD (AN10787 §2.4)
    private fun decodeMadGpb(byte9: Int): String {
        val adv = (byte9 and 0x03)
        val ma  = ((byte9 shr 6) and 0x01) == 1
        val da  = ((byte9 shr 7) and 0x01) == 1
        val ver = when (adv) { 0x01 -> "MAD1"; 0x02 -> "MAD2"; else -> "RFU" }
        val multi = if (ma) "multi-app" else "mono-app"
        val hasMad = if (da) "MAD present" else "MAD absent"
        return "$ver • $multi • $hasMad"
    }
    // ---- BCC pour UID 4B (datasheet §8.6.1)
    private fun bcc(uid4: ByteArray): Byte {
        require(uid4.size == 4)
        return (uid4[0].toInt() xor uid4[1].toInt() xor uid4[2].toInt() xor 0x88).toByte()
    }

}