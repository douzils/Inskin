package com.inskin.app.data

import com.google.gson.Gson
import com.inskin.app.TagDetails
import com.inskin.app.data.db.*
import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao, private val gson: Gson = Gson()) {

    fun history(): Flow<List<TagHistoryItem>> = dao.observeHistory()
    fun snapshot(uid: String): Flow<TagSnapshotEntity?> = dao.observeSnapshot(uid)
    fun ndef(uid: String): Flow<List<TagNdefRecordEntity>> = dao.observeNdefByUid(uid)

    suspend fun getSnapshot(uid: String) = dao.getSnapshot(uid.uppercase())

    suspend fun loadLatestDetails(uid: String): TagDetails? {
        val r = dao.latestRead(uid.uppercase()) ?: return null
        return runCatching { gson.fromJson(r.detailsJson, TagDetails::class.java) }.getOrNull()
    }

    suspend fun saveRead(
        details: TagDetails,
        rawDump: ByteArray?,
        source: String = "android",
        format: String = "bin",
        deviceModel: String? = null,
        appVersion: String? = null
    ) {
        val uid = details.uidHex.uppercase()
        val prev = dao.getSnapshot(uid)
        val now = System.currentTimeMillis()

        // NEW: used effectif
        val usedEffective = details.rawReadableBytes
            ?: details.usedBytes
            ?: details.classicSectors?.sumOf { s -> s.blocks.sumOf { b -> b.dataHex?.length?.div(2) ?: 0 } }

        // 1) Snapshot
        val snap = TagSnapshotEntity(
            uidHex = uid,
            name = prev?.name ?: "Tag",
            form = prev?.form,
            lastSeen = now,
            lastEdit = prev?.lastEdit ?: now,
            typeLabel = details.chipType,
            typeDetail = details.versionHex ?: details.memoryLayout,
            locked = details.isNdefWritable == false,
            usedBytes = usedEffective ?: -1,   // <-- corrigé
            ndefCapacity = details.ndefCapacity ?: 0,
            totalBytes = details.totalMemoryBytes ?: 0,
            detailsJson = gson.toJson(details.copy(rawDumpFirstBytesHex = null))
        )
        dao.upsertSnapshot(snap)

        // 2) Dump brut
        if (rawDump != null && rawDump.isNotEmpty()) {
            dao.insertDump(TagDumpEntity(uidHex = uid, source = source, format = format, bytes = rawDump))
        }

        // 3) Lecture complète
        val readId = dao.insertRead(
            TagReadEntity(
                uidHex = uid,
                source = source,
                device = deviceModel,
                appVersion = appVersion,
                techsJson = gson.toJson(details.techList),
                atqaHex = details.atqaHex,
                sakHex = details.sakHex,
                atsHex = details.atsHex,
                hfType = details.chipType,
                ndefPresent = !details.ndefRecords.isNullOrEmpty(),
                ndefWritable = details.isNdefWritable,
                ndefCapacity = details.ndefCapacity,
                totalBytes = details.totalMemoryBytes,
                rawReadableBytes = usedEffective,   // <-- corrigé
                versionHex = details.versionHex,
                memoryLayout = details.memoryLayout,
                signatureHex = details.eccSignatureHex,
                countersJson = details.countersHex,
                detailsJson = gson.toJson(details.copy(rawDumpFirstBytesHex = null))
            )
        )

        // 4) NDEF
        val records = (details.ndefRecords ?: emptyList()).mapIndexed { idx, r ->
            TagNdefRecordEntity(
                readId = readId,
                orderIndex = idx,
                tnf = r.tnf ?: -1,
                type = r.type ?: r.mimeType,
                payloadHex = r.payloadHex,
                idHex = r.idHex,
                text = r.text,
                lang = r.lang,
                uri = r.uri,
                mimeType = r.mimeType,
                sizeBytes = r.sizeBytes ?: (r.payloadHex?.length?.div(2) ?: 0)
            )
        }
        if (records.isNotEmpty()) dao.insertNdefRecords(records)
    }

    suspend fun rename(uid: String, name: String) =
        dao.rename(uid.uppercase(), name, System.currentTimeMillis())

    suspend fun setForm(uid: String, form: String?) =
        dao.setForm(uid.uppercase(), form, System.currentTimeMillis())
}
