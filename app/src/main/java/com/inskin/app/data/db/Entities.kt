package com.inskin.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tag_snapshots")
data class TagSnapshotEntity(
    @PrimaryKey val uidHex: String,      // ex: "04B4750BC52A81"
    val name: String = "Tag",
    val form: String? = null,            // BadgeForm.name si tu l’utilises
    val lastSeen: Long = System.currentTimeMillis(),
    val lastEdit: Long = System.currentTimeMillis(),
    val typeLabel: String? = null,       // "NTAG215", "MIFARE Classic", etc.
    val typeDetail: String? = null,      // versionHex / layout
    val locked: Boolean = false,
    val usedBytes: Int = -1,
    val ndefCapacity: Int = 0,
    val totalBytes: Int = 0,
    val detailsJson: String? = null      // TagDetails sérialisé (sans blobs)
)

@Entity(
    tableName = "tag_dumps",
    indices = [Index("uidHex")],
    foreignKeys = [ForeignKey(
        entity = TagSnapshotEntity::class,
        parentColumns = ["uidHex"],
        childColumns = ["uidHex"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TagDumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uidHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "android",      // "android" | "pm3" | …
    val format: String = "bin",          // "bin" | "mfd" | "eml" | "json"
    val bytes: ByteArray                 // dump complet
)

data class TagHistoryItem(
    val uidHex: String,
    val name: String,
    val form: String?,
    val lastSeen: Long,
    val lastEdit: Long
)
