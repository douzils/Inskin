package com.inskin.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    /* --- snapshots --- */
    @Upsert
    suspend fun upsertSnapshot(e: TagSnapshotEntity)

    @Query("UPDATE tag_snapshots SET name=:name, lastEdit=:ts WHERE uidHex=:uid")
    suspend fun rename(uid: String, name: String, ts: Long)

    @Query("UPDATE tag_snapshots SET form=:form, lastEdit=:ts WHERE uidHex=:uid")
    suspend fun setForm(uid: String, form: String?, ts: Long)

    /** Historique simple (1 ligne/UID) — correspond à TagHistoryItem top-level. */
    @Query("""
        SELECT uidHex, name, form, lastSeen, lastEdit
        FROM tag_snapshots
        ORDER BY lastSeen DESC
    """)
    fun observeHistory(): Flow<List<TagHistoryItem>>

    /** Variante groupée (si plusieurs lignes existent) — champs alignés avec TagHistoryItem. */
    @Query("""
        SELECT
            s.uidHex                             AS uidHex,
            MAX(s.lastSeen)                      AS lastSeen,
            MAX(s.lastEdit)                      AS lastEdit,
            (SELECT name FROM tag_snapshots
               WHERE uidHex = s.uidHex
               ORDER BY lastSeen DESC LIMIT 1)   AS name,
            (SELECT form FROM tag_snapshots
               WHERE uidHex = s.uidHex
               ORDER BY lastSeen DESC LIMIT 1)   AS form
        FROM tag_snapshots s
        GROUP BY s.uidHex
        ORDER BY lastSeen DESC
    """)
    fun history(): Flow<List<TagHistoryItem>>

    @Query("SELECT * FROM tag_snapshots WHERE uidHex=:uid LIMIT 1")
    fun observeSnapshot(uid: String): Flow<TagSnapshotEntity?>

    @Query("SELECT * FROM tag_snapshots WHERE uidHex=:uid LIMIT 1")
    suspend fun getSnapshot(uid: String): TagSnapshotEntity?

    /* --- dumps --- */
    @Insert
    suspend fun insertDump(d: TagDumpEntity)

    @Query("SELECT * FROM tag_dumps WHERE uidHex=:uid ORDER BY createdAt DESC")
    fun observeDumps(uid: String): Flow<List<TagDumpEntity>>

    /* --- reads & NDEF --- */
    @Insert
    suspend fun insertRead(read: TagReadEntity): Long

    @Insert
    suspend fun insertNdefRecords(records: List<TagNdefRecordEntity>)

    @Query("SELECT * FROM tag_reads WHERE uidHex=:uid ORDER BY readAt DESC LIMIT 1")
    suspend fun latestRead(uid: String): TagReadEntity?

    @Query("""
        SELECT r.* FROM tag_ndef_records r
        JOIN tag_reads t ON t.id = r.readId
        WHERE t.uidHex=:uid
        ORDER BY t.readAt DESC, r.orderIndex ASC
    """)
    fun observeNdefByUid(uid: String): Flow<List<TagNdefRecordEntity>>
}
