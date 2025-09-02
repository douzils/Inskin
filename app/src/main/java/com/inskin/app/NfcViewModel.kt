package com.inskin.app

import android.app.Application
import android.nfc.Tag
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.inskin.app.data.TagRepository
import com.inskin.app.data.db.AppDb
import com.inskin.app.tags.InspectorUtils
import com.inskin.app.tags.NfcRfidInspectorRouter
import com.inskin.app.tags.nfc.KeysRepository
import com.inskin.app.usb.ProxmarkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SimpleTag(
    val uidHex: String,
    val name: String = "Tag",
    val used: Int = 0,
    val total: Int = 0,
    val locked: Boolean = false,
    val typeLabel: String = "NFC",
    val typeDetail: String? = null
)

data class SavedTag(val uidHex: String, val savedAt: Long, val name: String = "Tag")

class NfcViewModel(app: Application) : AndroidViewModel(app) {

    // UI state
    val history = mutableStateListOf<SavedTag>()
    val lastTag = mutableStateOf<SimpleTag?>(null)
    val lastDetails = mutableStateOf<TagDetails?>(null)

    val showAuthDialog = mutableStateOf(false)
    val authBusy = mutableStateOf(false)
    val authError = mutableStateOf<String?>(null)
    val canAskUnlock = mutableStateOf(false)

    val phoneSignalLevel = mutableStateOf(0)
    val liveLogs = mutableStateListOf<String>()
    val pm3Status = mutableStateOf(ProxmarkStatus.NotPresent)

    // caches
    private val detailsCache = HashMap<String, TagDetails>() // key = UID uppercase
    private val uidToId = LinkedHashMap<String, Int>()
    private var nextId = 1
    private fun idFor(uidHex: String): Int = uidToId.getOrPut(uidHex.uppercase()) { nextId++ }

    // DB repo
    private val repo: TagRepository by lazy {
        val db = Room.databaseBuilder(getApplication(), AppDb::class.java, "inskin.db").build()
        TagRepository(db.tagDao())
    }

    init {
        // Charger l’historique persistant
        viewModelScope.launch(Dispatchers.IO) {
            repo.history().collect { list ->
                withContext(Dispatchers.Main) {
                    history.clear()
                    history.addAll(list.map { SavedTag(it.uidHex, it.lastSeen, it.name) })
                }
            }
        }
    }

    fun startWaiting() {
        lastTag.value = null
        lastDetails.value = null
        canAskUnlock.value = false
        authBusy.value = false
        showAuthDialog.value = false
        liveLogs.clear()
        liveLogs.add("En attente d’un tag…")
    }

    private fun upsertHistory(uidHex: String, name: String) {
        val u = uidHex.uppercase()
        val i = history.indexOfFirst { it.uidHex.equals(u, true) }
        val now = System.currentTimeMillis()
        if (i >= 0) history[i] = history[i].copy(savedAt = now, name = name)
        else history.add(0, SavedTag(u, now, name))
    }

    fun selectFromHistory(uidHex: String) {
        val u = uidHex.uppercase()
        val cached = detailsCache[u]
        if (cached != null) {
            val used = cached.rawReadableBytes
                ?: cached.classicSectors?.sumOf { s -> s.blocks.sumOf { b -> b.dataHex?.length?.div(2) ?: 0 } }
                ?: 0
            val type = cached.chipType ?: "NFC Tag"
            val name = history.firstOrNull { it.uidHex.equals(u, true) }?.name ?: type
            lastDetails.value = cached
            lastTag.value = SimpleTag(
                uidHex = cached.uidHex,
                typeLabel = type,
                typeDetail = cached.versionHex ?: cached.memoryLayout ?: "",
                name = name,
                used = used,
                total = cached.totalMemoryBytes ?: used,
                locked = false
            )
            liveLogs.add("Ouverture depuis l’historique (cache): $u")
            return
        }

        // Fallback: snapshot DB
        viewModelScope.launch(Dispatchers.IO) {
            repo.snapshot(u).collect { snap ->
                if (snap != null) {
                    withContext(Dispatchers.Main) {
                        lastDetails.value = null
                        lastTag.value = SimpleTag(
                            uidHex = snap.uidHex,
                            typeLabel = snap.typeLabel ?: "NFC",
                            typeDetail = snap.typeDetail ?: "",
                            name = snap.name,
                            used = snap.usedBytes,
                            total = snap.totalBytes,
                            locked = snap.locked
                        )
                        liveLogs.add("Ouverture depuis l’historique (DB): $u")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val name = history.firstOrNull { it.uidHex.equals(u, true) }?.name ?: "Tag"
                        lastDetails.value = null
                        lastTag.value = SimpleTag(uidHex = u, typeLabel = "Historique", name = name)
                        liveLogs.add("Historique: pas de snapshot DB pour $u")
                    }
                }
            }
        }
    }

    fun askUnlock() { showAuthDialog.value = true }

    fun submitNtagPassword(pwdHex: String, remember: Boolean) {
        authBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    authError.value = null
                    showAuthDialog.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { authError.value = e.message }
            } finally {
                withContext(Dispatchers.Main) { authBusy.value = false }
            }
        }
    }

    fun updateTag(tag: Tag) {
        // hooks logs inspecteurs
        InspectorUtils.emitLog = { msg -> viewModelScope.launch { liveLogs.add(msg) } }

        val keysRepo = KeysRepository(getApplication())
        InspectorUtils.extraKeysProvider = { keysRepo.loadAllFromAssets() }
        val count = keysRepo.loadAllFromAssets().size
        viewModelScope.launch { liveLogs.add("Clés chargées: $count") }

        InspectorUtils.onKeyLearned = { uid: String, sec: Int, type: String, key: String ->
            viewModelScope.launch { liveLogs.add("Clé apprise $type S$sec = $key") }
        }

        // overlay immédiat
        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        lastTag.value = SimpleTag(uidHex = uidHex, typeLabel = "Lecture…")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val channel = com.inskin.app.tags.Channel.Android(tag)
                val result = NfcRfidInspectorRouter.firstSupporting(channel).read(channel)

                withContext(Dispatchers.Main) {
                    val d = result.details
                    lastDetails.value = d

                    val key = d.uidHex.uppercase()
                    detailsCache[key] = d

                    val usedBytes = d.rawReadableBytes
                        ?: d.classicSectors?.sumOf { s -> s.blocks.sumOf { b -> b.dataHex?.length?.div(2) ?: 0 } }
                        ?: 0

                    val idNum = idFor(d.uidHex)
                    val typeLabel = d.chipType ?: "NFC Tag"
                    val typeDetail = d.versionHex ?: d.memoryLayout ?: ""
                    val displayName = "$typeLabel #$idNum"

                    lastTag.value = SimpleTag(
                        uidHex = d.uidHex,
                        typeLabel = typeLabel,
                        typeDetail = typeDetail,
                        name = displayName,
                        used = usedBytes,
                        total = d.totalMemoryBytes ?: usedBytes,
                        locked = false
                    )

                    // Historique UI + persistance du nom
                    upsertHistory(d.uidHex, displayName)
                    viewModelScope.launch(Dispatchers.IO) { repo.rename(d.uidHex, displayName) }

                    // Persist snapshot + dump
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val dump = bestEffortDump(d)
                            repo.saveRead(details = d, rawDump = dump, source = "android", format = "bin")
                        } catch (_: Exception) { }
                    }

                    liveLogs.add("Lecture terminée: ${d.chipType}")
                    canAskUnlock.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { liveLogs.add("Erreur: ${e.message}") }
            } finally {
                InspectorUtils.emitLog = null
                InspectorUtils.onKeyLearned = null
            }
        }
    }

    private fun bestEffortDump(details: TagDetails): ByteArray {
        val head = details.rawDumpFirstBytesHex.orEmpty()
            .chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()

        val classic = details.classicSectors.orEmpty()
            .flatMap { it.blocks }
            .mapNotNull { it.dataHex }
            .flatMap { hex -> hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() } }
            .toByteArray()

        return head + classic
    }
}
