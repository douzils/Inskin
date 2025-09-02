package com.inskin.app.data

import com.google.gson.Gson
import com.inskin.app.TagDetails
import com.inskin.app.data.db.*
import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val dao: TagDao,
    private val gson: Gson = Gson()
) {
    fun history(): Flow<List<TagHistoryItem>> = dao.observeHistory()
    fun snapshot(uid: String): Flow<TagSnapshotEntity?> = dao.observeSnapshot(uid)
    fun dumps(uid: String): Flow<List<TagDumpEntity>> = dao.observeDumps(uid)
    fun reads(uid: String): Flow<List<TagReadEntity>> = dao.observeReads(uid)
    fun ndef(uid: String): Flow<List<TagNdefRecordEntity>> = dao.observeNdefByUid(uid)

    suspend fun saveRead(
        details: TagDetails,
        rawDump: ByteArray?,
        source: String = "android",
        format: String = "bin",
        deviceModel: String? = null,
        appVersion: String? = null
    ) {
        // 1) Snapshot synthétique
        val snap = TagSnapshotEntity(
            uidHex = details.uidHex.uppercase(),
            name = "Tag",
            form = null,
            lastSeen = System.currentTimeMillis(),
            lastEdit = System.currentTimeMillis(),
            typeLabel = details.chipType,
            typeDetail = details.versionHex ?: details.memoryLayout,
            locked = details.isNdefWritable == false,
            usedBytes = details.rawReadableBytes ?: -1,
            ndefCapacity = details.ndefCapacity ?: 0,
            totalBytes = details.totalMemoryBytes ?: 0,
            detailsJson = gson.toJson(details.copy(rawDumpFirstBytesHex = null))
        )
        dao.upsertSnapshot(snap)

        // 2) Dump brut optionnel
        if (rawDump != null && rawDump.isNotEmpty()) {
            dao.insertDump(
                TagDumpEntity(
                    uidHex = snap.uidHex,
                    source = source,
                    format = format,
                    bytes = rawDump
                )
            )
        }

        // 3) Journal de lecture complet
        val read = TagReadEntity(
            uidHex = snap.uidHex,
            source = source,
            device = deviceModel,
            appVersion = appVersion,
            techsJson = safeJson(details.techList),
            atqaHex = details.atqaHex,
            sakHex = details.sakHex,
            atsHex = details.atsHex,
            hfType = details.chipType,
            ndefPresent = details.hasNdef,
            ndefWritable = details.isNdefWritable,
            ndefCapacity = details.ndefCapacity,
            totalBytes = details.totalMemoryBytes,
            rawReadableBytes = details.rawReadableBytes,
            versionHex = details.versionHex,
            memoryLayout = details.memoryLayout,
            signatureHex = details.signatureHex,
            countersJson = safeJson(details.counters),
            detailsJson = gson.toJson(details.copy(rawDumpFirstBytesHex = null))
        )
        val readId = dao.insertRead(read)

        // 4) Enregistrement des NDEF si présents
        val records = buildNdefEntities(readId, details)
        if (records.isNotEmpty()) dao.insertNdefRecords(records)
    }

    suspend fun rename(uid: String, name: String) =
        dao.rename(uid.uppercase(), name, System.currentTimeMillis())

    suspend fun setForm(uid: String, form: String?) =
        dao.setForm(uid.uppercase(), form, System.currentTimeMillis())

    // Helpers
    private fun safeJson(any: Any?): String? = any?.let { gson.toJson(it) }

    private fun buildNdefEntities(readId: Long, d: TagDetails): List<TagNdefRecordEntity> {
        val list = mutableListOf<TagNdefRecordEntity>()
        val ndef = d.ndefRecords ?: emptyList()
        ndef.forEachIndexed { idx, r ->
            // r supposé avoir: tnf, type, payloadHex, idHex, text, lang, uri, mimeType, sizeBytes
            list += TagNdefRecordEntity(
                readId = readId,
                orderIndex = idx,
                tnf = r.tnf ?: -1,
                type = r.type,
                payloadHex = r.payloadHex,
                idHex = r.idHex,
                text = r.text,
                lang = r.lang,
                uri = r.uri,
                mimeType = r.mimeType,
                sizeBytes = r.sizeBytes ?: (r.payloadHex?.length?.div(2) ?: 0)
            )
        }
        return list
    }
}
