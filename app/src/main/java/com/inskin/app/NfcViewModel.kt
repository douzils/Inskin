package com.inskin.app

import android.app.Application
import android.content.Context
import android.nfc.Tag
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.*
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inskin.app.ui.screens.EntryKind
import com.inskin.app.ui.screens.WriteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import android.os.SystemClock
import android.util.Log

/* --------- Modèles --------- */
data class SimpleTag(
    val typeLabel: String,
    val typeDetail: String?,
    val uidHex: String,
    val name: String,
    val used: Int,
    val total: Int,
    val locked: Boolean
)

/** Snapshot d’historique persisté */
data class SavedTag(
    val uidHex: String,
    val name: String,
    val typeLabel: String,
    val typeDetail: String?,
    val total: Int,
    val used: Int,
    val locked: Boolean,
    val savedAt: Long,
    val favorite: Boolean = false
)

class NfcViewModel(app: Application) : AndroidViewModel(app) {

    /* ---------- UI states ---------- */
    val isWaiting = mutableStateOf(true)
    val lastTag = mutableStateOf<SimpleTag?>(null)
    val lastDetails = mutableStateOf<TagDetails?>(null)

    // Déverrouillage (NTAG/Ultralight uniquement)
    val showAuthDialog = mutableStateOf(false)
    val authBusy = mutableStateOf(false)
    val authError = mutableStateOf<String?>(null)
    val canAskUnlock = mutableStateOf(false)

    // Mot de passe dévoilé (si succès)
    val disclosedPwdHex = mutableStateOf<String?>(null)

    // Historique observable
    val history = mutableStateOf<List<SavedTag>>(emptyList())

    /* ---------- internals ---------- */
    private val prefs = app.getSharedPreferences("nfc_prefs", Context.MODE_PRIVATE)
    private var lastAndroidTag: Tag? = null
    private var lastTagSeenAt: Long = 0L
    private var pendingWrite: Pair<List<WriteEntry>, (Boolean) -> Unit>? = null

    init {
        history.value = loadHistory()
    }

    /* ===================== API ===================== */

    fun startWaiting() {
        isWaiting.value = true
        lastTag.value = null
        lastDetails.value = null
        disclosedPwdHex.value = null
    }

    fun askUnlock() {
        authError.value = null
        showAuthDialog.value = true
    }

    /** Charger un tag depuis l’historique (comme s’il venait d’être scanné). */
    fun selectFromHistory(uid: String) {
        val s = history.value.firstOrNull { it.uidHex.equals(uid, true) } ?: return
        isWaiting.value = false
        lastDetails.value = null
        lastTag.value = SimpleTag(
            typeLabel = s.typeLabel,
            typeDetail = s.typeDetail,
            uidHex = s.uidHex,
            name = s.name,
            used = s.used,
            total = s.total,
            locked = s.locked
        )
    }

    /**
     * Déverrouillage NTAG/Ultralight (PWD_AUTH 0x1B).
     */
    fun submitNtagPassword(pwdHex: String, remember: Boolean) {
        val tag = lastAndroidTag ?: return
        val pwd = parsePwd(pwdHex) ?: run {
            authError.value = "Mot de passe invalide (4 octets HEX)"
            return
        }
        authBusy.value = true
        authError.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val a = NfcA.get(tag) ?: return@runCatching false
                a.connect()
                val resp = a.transceive(byteArrayOf(0x1B.toByte()) + pwd)
                a.close()
                resp != null && resp.size >= 2
            }.getOrElse { false }

            withContext(Dispatchers.Main) {
                if (!ok) {
                    authBusy.value = false
                    authError.value = "Échec de l'authentification"
                    return@withContext
                }
                val uid = getUid(tag)
                val hexUpper = pwdHex.uppercase()
                if (remember) savePwd(uid, hexUpper)
                disclosedPwdHex.value = hexUpper
                authBusy.value = false
                showAuthDialog.value = false
                updateTag(tag, forceNameReuse = true)
            }
        }
    }

    /** Appelé quand un Tag est détecté. */
    fun updateTag(tag: Tag, forceNameReuse: Boolean = false) {
        isWaiting.value = false
        lastAndroidTag = tag
        lastTagSeenAt = SystemClock.elapsedRealtime()
        disclosedPwdHex.value = null
        lastDetails.value = null

        val uidHex = getUid(tag)
        val typeLabel = guessTypeLabel(tag)
        val typeDetail: String? = null
        val isClassic = (MifareClassic.get(tag) != null)

        // Nom persistant (par UID) ou génération Type #n
        val name = prefs.getString("name_$uidHex", null) ?: run {
            val count = prefs.getInt("count_$typeLabel", 0) + 1
            prefs.edit { putInt("count_$typeLabel", count) }
            "$typeLabel #$count"
        }

        // Déverrouillage proposé seulement pour NTAG/Ultralight
        canAskUnlock.value =
            !isClassic && (MifareUltralight.get(tag) != null || NfcA.get(tag) != null)

        // 👇👇👇  AJOUT IMPORTANT : publier un snapshot immédiat pour déclencher la pop-up
        val earlySnap = SimpleTag(
            typeLabel = typeLabel,
            typeDetail = typeDetail,
            uidHex = uidHex,
            name = if (forceNameReuse) (prefs.getString("name_$uidHex", name) ?: name) else name,
            used = 0,           // inconnus pour l’instant
            total = 0,
            locked = false
        )
        lastTag.value = earlySnap
        Log.d("ScanFlow", "early lastTag posted -> $uidHex")
        viewModelScope.launch(Dispatchers.IO) {
            // Tentative auto si mot de passe mémorisé (NTAG)
            val saved = loadPwd(uidHex)
            if (!isClassic && !saved.isNullOrBlank()) {
                runCatching {
                    val a = NfcA.get(tag)
                    if (a != null) {
                        a.connect()
                        parsePwd(saved)?.let { pwd -> a.transceive(byteArrayOf(0x1B) + pwd) }
                        a.close()
                        withContext(Dispatchers.Main) { disclosedPwdHex.value = saved }
                    }
                }
            }

            // Lecture rapide NDEF
            var usedNdefApi = 0
            var totalNdefApi = 0
            var lockedNdef = false
            runCatching {
                val ndef = Ndef.get(tag)
                ndef?.connect()
                val msg = ndef?.cachedNdefMessage ?: ndef?.ndefMessage
                usedNdefApi = msg?.toByteArray()?.size ?: 0
                totalNdefApi = ndef?.maxSize ?: 0
                lockedNdef = (ndef?.isWritable == false)
                runCatching { ndef?.close() }
            }

            // Détails riches
            val det = NfcInspector.inspect(tag)

            // Classic
            val classicUsage = if (isClassic) readClassicUsage(tag) else null
            val classicReadable = (classicUsage != null) || quickClassicAuth(tag)

            val usedFinal = when {
                classicUsage?.used != null -> classicUsage.used
                det.usedBytes != null -> det.usedBytes
                isClassic && !classicReadable -> -1
                else -> usedNdefApi
            }

            val totalFinal = when {
                classicUsage != null -> classicUsage.capacity
                det.ndefCapacity != null -> det.ndefCapacity
                else -> totalNdefApi
            }

            val lockedFinal =
                if (isClassic) !classicReadable
                else det.isNdefWritable?.let { !it } ?: lockedNdef

            withContext(Dispatchers.Main) {
                lastDetails.value = det
                val snap = SimpleTag(
                    typeLabel = typeLabel,
                    typeDetail = typeDetail,
                    uidHex = uidHex,
                    name = if (forceNameReuse) (prefs.getString("name_$uidHex", name)
                        ?: name) else name,
                    used = usedFinal,
                    total = totalFinal,
                    locked = lockedFinal
                )
                lastTag.value = snap
                saveInHistory(snap)   // <-- enregistre à chaque scan
                tryWriteNow(tag)      // tentative d’écriture si en attente
            }
        }
    }

    /* ===================== Write queue ===================== */

    fun queueWrite(
        entries: List<WriteEntry>,
        result: (Boolean) -> Unit
    ) {
        pendingWrite = entries to result
        val tag = lastAndroidTag
        val fresh = tag != null && (SystemClock.elapsedRealtime() - lastTagSeenAt) < 2_000L
        if (fresh) tryWriteNow(tag)
    }

    private fun tryWriteNow(tag: Tag) {
        val pair = pendingWrite ?: return
        val (entries, cb) = pair

        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val msg = toNdefMessage(entries)

                // 1) NDEF présent
                Ndef.get(tag)?.let { ndef ->
                    ndef.connect()
                    val fits = msg.toByteArray().size <= ndef.maxSize
                    val writable = ndef.isWritable
                    val success = fits && writable && runCatching { ndef.writeNdefMessage(msg) }.isSuccess
                    runCatching { ndef.close() }
                    success
                } ?: run {
                    // 2) Formatage NDEF
                    NdefFormatable.get(tag)?.let { fmt ->
                        fmt.connect()
                        val success = runCatching { fmt.format(msg) }.isSuccess
                        runCatching { fmt.close() }
                        success
                    } ?: false
                }
            }.getOrDefault(false)

            withContext(Dispatchers.Main) {
                cb(ok)
                if (ok) updateTag(tag, forceNameReuse = true)
                pendingWrite = null
            }
        }
    }

    /* ===================== Helpers ===================== */

    private fun getUid(tag: Tag): String = (tag.id ?: ByteArray(0)).toHex()

    private fun guessTypeLabel(tag: Tag): String =
        when {
            MifareClassic.get(tag) != null -> {
                val size = MifareClassic.get(tag)?.size ?: 0
                if (size <= 1024) "MIFARE Classic 1K" else "MIFARE Classic 4K"
            }
            MifareUltralight.get(tag)?.type == MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
            NfcA.get(tag) != null -> "NTAG/Ultralight"
            IsoDep.get(tag) != null -> "ISO-DEP"
            NfcV.get(tag) != null -> "NfcV"
            NfcF.get(tag) != null -> "NfcF"
            NfcB.get(tag) != null -> "NfcB"
            else -> "NFC Tag"
        }

    /** "A1B2C3D4" -> 0xA1 0xB2 0xC3 0xD4 */
    private fun parsePwd(input: String): ByteArray? {
        val s = input.trim().replace(" ", "").uppercase()
        if (s.length != 8) return null
        if (!s.all { it.isDigit() || it in 'A'..'F' }) return null
        return ByteArray(4) { i -> s.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    private fun savePwd(uidHex: String, pwdHex: String) {
        prefs.edit { putString("pwd_$uidHex", pwdHex) }
    }

    fun loadPwd(uidHex: String): String? = prefs.getString("pwd_$uidHex", null)

    /* ---------- MIFARE Classic ---------- */

    private val commonKeys: List<ByteArray> = listOf(
        byteArrayOf(0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte()),
        byteArrayOf(0xA0.toByte(),0xA1.toByte(),0xA2.toByte(),0xA3.toByte(),0xA4.toByte(),0xA5.toByte()),
        byteArrayOf(0xD3.toByte(),0xF7.toByte(),0xD3.toByte(),0xF7.toByte(),0xD3.toByte(),0xF7.toByte()),
        byteArrayOf(0,0,0,0,0,0),
        byteArrayOf(0xB0.toByte(),0xB1.toByte(),0xB2.toByte(),0xB3.toByte(),0xB4.toByte(),0xB5.toByte()),
        byteArrayOf(0x4D,0x3A,0x99.toByte(),0xC3.toByte(),0x51,0xDD.toByte()),
        byteArrayOf(0x1A,0x2B,0x3C,0x4D,0x5E,0x6F)
    )

    /** Auth rapide (quelques secteurs) avec clés courantes. */
    private fun quickClassicAuth(tag: Tag): Boolean = runCatching {
        val mc = MifareClassic.get(tag) ?: return false
        mc.connect()
        mc.use {
            val sectorsToTry = listOf(0, 1).filter { it < mc.sectorCount }
            for (s in sectorsToTry) {
                val ok = commonKeys.any { k ->
                    mc.authenticateSectorWithKeyA(s, k) || mc.authenticateSectorWithKeyB(s, k)
                }
                if (ok) return true
            }
        }
        false
    }.getOrDefault(false)

    private data class ClassicUsage(val capacity: Int, val used: Int?)

    private fun readClassicUsage(tag: Tag): ClassicUsage? = runCatching {
        val mc = MifareClassic.get(tag) ?: return null
        mc.connect()
        mc.use {
            val madKeyA = byteArrayOf(0xA0.toByte(),0xA1.toByte(),0xA2.toByte(),0xA3.toByte(),0xA4.toByte(),0xA5.toByte())
            val defaultKey = byteArrayOf(0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte(),0xFF.toByte())

            // MAD directory (secteur 0 , blocs 1-2)
            val dir: ByteArray? = runCatching {
                if (mc.authenticateSectorWithKeyA(0, madKeyA) || mc.authenticateSectorWithKeyA(0, defaultKey)) {
                    mc.readBlock(1) + mc.readBlock(2)
                } else null
            }.getOrNull()

            val usedSectorsFromMad = mutableSetOf<Int>()
            if (dir != null) {
                for (s in 1 until mc.sectorCount.coerceAtMost(16)) {
                    val i = (s - 1) * 2
                    if (i + 1 < dir.size) {
                        val a = dir[i].toInt() and 0xFF
                        val b = dir[i + 1].toInt() and 0xFF
                        if (((a shl 8) or b) != 0x0000) usedSectorsFromMad += s
                    }
                }
            }

            val keys = listOf(
                defaultKey, madKeyA,
                byteArrayOf(0xD3.toByte(),0xF7.toByte(),0xD3.toByte(),0xF7.toByte(),0xD3.toByte(),0xF7.toByte()),
                byteArrayOf(0,0,0,0,0,0),
                byteArrayOf(0xB0.toByte(),0xB1.toByte(),0xB2.toByte(),0xB3.toByte(),0xB4.toByte(),0xB5.toByte())
            )

            val sectorsToRead = if (usedSectorsFromMad.isNotEmpty()) usedSectorsFromMad.toList()
            else (1 until mc.sectorCount).toList()

            val stream = ArrayList<Byte>()
            var capacityBytes = 0
            var usedHeuristic = 0

            fun isEmptyBlock(b: ByteArray): Boolean {
                val all0 = b.all { it == 0x00.toByte() }
                val allFF = b.all { it == 0xFF.toByte() }
                return all0 || allFF
            }

            for (s in sectorsToRead) {
                val authed = keys.any { k ->
                    mc.authenticateSectorWithKeyA(s, k) || mc.authenticateSectorWithKeyB(s, k)
                }
                if (!authed) continue

                val first = mc.sectorToBlock(s)
                val count = mc.getBlockCountInSector(s)
                val dataBlocks = (count - 1).coerceAtLeast(0)
                capacityBytes += dataBlocks * MifareClassic.BLOCK_SIZE

                for (b in 0 until dataBlocks) {
                    val block = mc.readBlock(first + b)
                    stream.addAll(block.toList())
                    if (!isEmptyBlock(block)) usedHeuristic += block.size
                }
            }

            if (capacityBytes == 0) return null

            fun scanNdefLen(buf: List<Byte>): Int? {
                var i = 0
                val n = buf.size
                while (i < n) {
                    when (buf[i].toInt() and 0xFF) {
                        0x00 -> i++
                        0xFE -> return null
                        0x03 -> {
                            if (i + 1 >= n) return null
                            val len = buf[i + 1].toInt() and 0xFF
                            return if (len == 0xFF) {
                                if (i + 3 >= n) null
                                else (((buf[i + 2].toInt() and 0xFF) shl 8) or (buf[i + 3].toInt() and 0xFF))
                            } else len
                        }
                        else -> {
                            if (i + 1 >= n) return null
                            val len = (buf[i + 1].toInt() and 0xFF)
                            i += if (len == 0xFF) {
                                if (i + 3 >= n) return null
                                4 + (((buf[i + 2].toInt() and 0xFF) shl 8) or (buf[i + 3].toInt() and 0xFF))
                            } else 2 + len
                        }
                    }
                }
                return null
            }

            val usedNdef = scanNdefLen(stream)?.coerceAtMost(capacityBytes)
            val used = (usedNdef ?: if (usedSectorsFromMad.isNotEmpty()) {
                usedSectorsFromMad.sumOf {
                    val blks = mc.getBlockCountInSector(it)
                    (blks - 1).coerceAtLeast(0) * MifareClassic.BLOCK_SIZE
                }
            } else usedHeuristic).coerceAtMost(capacityBytes)

            ClassicUsage(capacity = capacityBytes, used = used)
        }
    }.getOrNull()

    /* ---------- Historique : persistance JSON ---------- */

    /** Construit un NDEF à partir de la liste UI. */
    private fun toNdefMessage(entries: List<WriteEntry>): NdefMessage {
        val recs: List<NdefRecord> = entries.mapNotNull { e ->
            when (e.kind) {
                EntryKind.URL -> e.value.takeIf { it.isNotBlank() }?.let {
                    NdefRecord.createUri(it.toUri())
                }
                EntryKind.TEXT -> e.value.takeIf { it.isNotBlank() }?.let {
                    NdefRecord.createTextRecord(null, it)
                }
                else -> null
            }
        }
        return NdefMessage(recs.toTypedArray())
    }

    private fun saveInHistory(tag: SimpleTag) {
        val now = System.currentTimeMillis()
        val list = loadHistory().toMutableList()
        val existingIdx = list.indexOfFirst { it.uidHex.equals(tag.uidHex, true) }
        val existingFav = list.getOrNull(existingIdx)?.favorite ?: false
        val entry = SavedTag(
            uidHex = tag.uidHex,
            name = tag.name,
            typeLabel = tag.typeLabel,
            typeDetail = tag.typeDetail,
            total = tag.total,
            used = tag.used,
            locked = tag.locked,
            savedAt = now,
            favorite = existingFav
        )
        if (existingIdx >= 0) list[existingIdx] = entry else list.add(0, entry)
        persistHistory(list)
        history.value = list
    }

    fun toggleFavorite(uidHex: String, favorite: Boolean) {
        val list = loadHistory().toMutableList()
        val i = list.indexOfFirst { it.uidHex.equals(uidHex, true) }
        if (i >= 0) {
            val s = list[i]
            list[i] = s.copy(favorite = favorite)
            persistHistory(list)
            history.value = list
        }
    }

    private fun loadHistory(): List<SavedTag> {
        val s = prefs.getString("history_json", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(s)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val td = o.optString("typeDetail").takeIf { it.isNotEmpty() }
                SavedTag(
                    uidHex = o.optString("uid"),
                    name = o.optString("name"),
                    typeLabel = o.optString("type"),
                    typeDetail = td,
                    total = o.optInt("total", 0),
                    used = o.optInt("used", -1),
                    locked = o.optBoolean("locked", false),
                    savedAt = o.optLong("savedAt", 0L),
                    favorite = o.optBoolean("favorite", false)
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun persistHistory(list: List<SavedTag>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("uid", s.uidHex)
                    .put("name", s.name)
                    .put("type", s.typeLabel)
                    .put("typeDetail", s.typeDetail ?: JSONObject.NULL)
                    .put("total", s.total)
                    .put("used", s.used)
                    .put("locked", s.locked)
                    .put("savedAt", s.savedAt)
                    .put("favorite", s.favorite)
            )
        }
        prefs.edit { putString("history_json", arr.toString()) }
    }
}
