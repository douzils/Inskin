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

    suspend fun saveRead(
        details: TagDetails,
        rawDump: ByteArray?,
        source: String = "android",
        format: String = "bin"
    ) {
        val e = TagSnapshotEntity(
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
        dao.upsertSnapshot(e)
        if (rawDump != null && rawDump.isNotEmpty()) {
            dao.insertDump(
                TagDumpEntity(
                    uidHex = e.uidHex,
                    source = source,
                    format = format,
                    bytes = rawDump
                )
            )
        }
    }

    suspend fun rename(uid: String, name: String) =
        dao.rename(uid.uppercase(), name, System.currentTimeMillis())

    suspend fun setForm(uid: String, form: String?) =
        dao.setForm(uid.uppercase(), form, System.currentTimeMillis())
}
