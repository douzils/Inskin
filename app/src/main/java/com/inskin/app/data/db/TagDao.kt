package com.inskin.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Upsert
    suspend fun upsertSnapshot(e: TagSnapshotEntity)

    @Query("UPDATE tag_snapshots SET name=:name, lastEdit=:ts WHERE uidHex=:uid")
    suspend fun rename(uid: String, name: String, ts: Long)

    @Query("UPDATE tag_snapshots SET form=:form, lastEdit=:ts WHERE uidHex=:uid")
    suspend fun setForm(uid: String, form: String?, ts: Long)

    @Query("SELECT uidHex,name,form,lastSeen,lastEdit FROM tag_snapshots ORDER BY lastSeen DESC")
    fun observeHistory(): Flow<List<TagHistoryItem>>

    @Query("SELECT * FROM tag_snapshots WHERE uidHex=:uid LIMIT 1")
    fun observeSnapshot(uid: String): Flow<TagSnapshotEntity?>

    @Insert
    suspend fun insertDump(d: TagDumpEntity)

    @Query("SELECT * FROM tag_dumps WHERE uidHex=:uid ORDER BY createdAt DESC")
    fun observeDumps(uid: String): Flow<List<TagDumpEntity>>

    // Nouvelles DAO pour journal de lecture + NDEF
    @Insert
    suspend fun insertRead(read: TagReadEntity): Long

    @Insert
    suspend fun insertNdefRecords(records: List<TagNdefRecordEntity>)

    @Query("SELECT * FROM tag_reads WHERE uidHex=:uid ORDER BY readAt DESC")
    fun observeReads(uid: String): Flow<List<TagReadEntity>>

    @Query("""
        SELECT r.* FROM tag_ndef_records r
        JOIN tag_reads t ON t.id = r.readId
        WHERE t.uidHex=:uid
        ORDER BY t.readAt DESC, r.orderIndex ASC
    """)
    fun observeNdefByUid(uid: String): Flow<List<TagNdefRecordEntity>>
}
