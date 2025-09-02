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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

data class SimpleTag(
    val uidHex: String,
    val name: String = "Tag",
    val used: Int = 0,
    val total: Int = 0,
    val locked: Boolean = false,
    val typeLabel: String = "NFC",
    val typeDetail: String? = null
)

data class SavedTag(
    val uidHex: String,
    val savedAt: Long,
    val name: String = "Tag",
    val form: String? = null
)
class NfcViewModel(app: Application) : AndroidViewModel(app) {
    val readingNow = mutableStateOf(false)
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
        val db = Room.databaseBuilder(getApplication(), AppDb::class.java, "inskin.db")
            .fallbackToDestructiveMigration()   // simple et efficace
            .build()

        TagRepository(db.tagDao())
    }


    init {
        viewModelScope.launch(Dispatchers.IO) {
            repo.history().collect { list ->
                withContext(Dispatchers.Main) {
                    history.clear()
                    history.addAll(
                        list.map { SavedTag(it.uidHex, it.lastSeen, it.name, it.form) }
                    )
                }
            }
        }
    }

// ajoute dans la classe NfcViewModel

    // NfcViewModel.kt
    suspend fun getFormFor(uid: String): String? =
        repo.getSnapshot(uid.uppercase())?.form

    fun renameTag(uid: String, name: String) {
        val u = uid.uppercase()
        upsertHistory(u, name)
        viewModelScope.launch(Dispatchers.IO) { repo.rename(u, name) }
    }

    fun setTagForm(uid: String, formKey: String?) {
        val u = uid.uppercase()
        viewModelScope.launch(Dispatchers.IO) { repo.setForm(u, formKey) }
    }


    fun openFromHistory(uid: String) {
        val u = uid.uppercase()
        viewModelScope.launch(Dispatchers.IO) {
            // lecture ponctuelle (pas de collect infini)
            val snap = repo.getSnapshot(u)
            val det = repo.loadLatestDetails(u)

            withContext(Dispatchers.Main) {
                lastDetails.value = det
                lastTag.value = SimpleTag(
                    uidHex = u,
                    name = snap?.name ?: "Tag",
                    used = det?.rawReadableBytes ?: snap?.usedBytes ?: 0,
                    total = det?.totalMemoryBytes ?: snap?.totalBytes ?: 0,
                    locked = snap?.locked ?: false,
                    typeLabel = det?.chipType ?: snap?.typeLabel ?: "NFC",
                    typeDetail = det?.versionHex ?: det?.memoryLayout ?: (snap?.typeDetail ?: "")
                )
                liveLogs.add("Ouverture depuis l’historique")
            }
        }
    }

    fun startWaiting() {
        readingNow.value = false
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
        readingNow.value = false   // ouverture offline
        val u = uidHex.uppercase()

        // 1) cache mémoire
        detailsCache[u]?.let { cached ->
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

        // 2) DB: charger dernier TagRead + snapshot pour l’en-tête
        viewModelScope.launch(Dispatchers.IO) {
            val det = repo.loadLatestDetails(u)         // <-- JSON complet TagDetails
            val snap = repo.getSnapshot(u)              // <-- nom/type/flags
            val used = det?.rawReadableBytes
                ?: det?.classicSectors?.sumOf { s -> s.blocks.sumOf { b -> b.dataHex?.length?.div(2) ?: 0 } }
                ?: snap?.usedBytes ?: 0
            val total = det?.totalMemoryBytes ?: snap?.totalBytes ?: used
            val typeLabel = det?.chipType ?: snap?.typeLabel ?: "NFC"
            val typeDetail = det?.versionHex ?: det?.memoryLayout ?: snap?.typeDetail ?: ""
            val displayName = snap?.name ?: det?.chipType ?: "Tag"

            withContext(Dispatchers.Main) {
                lastDetails.value = det
                if (det != null) detailsCache[u] = det
                lastTag.value = SimpleTag(
                    uidHex = u,
                    name = displayName,
                    used = used,
                    total = total,
                    locked = snap?.locked ?: false,
                    typeLabel = typeLabel,
                    typeDetail = typeDetail
                )
                liveLogs.add("Ouverture depuis l’historique (complet): $u")
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
        InspectorUtils.emitLog = { msg -> viewModelScope.launch { liveLogs.add(msg) } }

        val keysRepo = KeysRepository(getApplication())
        InspectorUtils.extraKeysProvider = { keysRepo.loadAllFromAssets() }
        val count = keysRepo.loadAllFromAssets().size
        viewModelScope.launch { liveLogs.add("Clés chargées: $count") }

        InspectorUtils.onKeyLearned = { uid: String, sec: Int, type: String, key: String ->
            viewModelScope.launch { liveLogs.add("Clé apprise $type S$sec = $key") }
        }

        val uidHex = (tag.id ?: ByteArray(0)).toHex()
        lastTag.value = SimpleTag(uidHex = uidHex, typeLabel = "Lecture…")
        readingNow.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val channel = com.inskin.app.tags.Channel.Android(tag)
                val result = NfcRfidInspectorRouter.firstSupporting(channel).read(channel)
                val d = result.details
                val prev = repo.getSnapshot(d.uidHex)            // <— récupère nom/icône existants
                val idNum = idFor(d.uidHex)
                val typeLabel = d.chipType ?: "NFC Tag"
                val displayName = prev?.name ?: "$typeLabel #$idNum"

                withContext(Dispatchers.Main) {
                    lastDetails.value = d
                    detailsCache[d.uidHex.uppercase()] = d

                    val usedBytes = d.rawReadableBytes
                        ?: d.classicSectors?.sumOf { s -> s.blocks.sumOf { b -> b.dataHex?.length?.div(2) ?: 0 } }
                        ?: 0

                    lastTag.value = SimpleTag(
                        uidHex = d.uidHex,
                        typeLabel = typeLabel,
                        typeDetail = d.versionHex ?: d.memoryLayout ?: "",
                        name = displayName,
                        used = usedBytes,
                        total = d.totalMemoryBytes ?: usedBytes,
                        locked = false
                    )

                    upsertHistory(d.uidHex, displayName)
                }

                // persistences
                val dump = bestEffortDump(d)
                repo.saveRead(details = d, rawDump = dump, source = "android", format = "bin")
                repo.rename(d.uidHex, displayName)               // garde le nom
            } finally {
                withContext(Dispatchers.Main) { readingNow.value = false }
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
