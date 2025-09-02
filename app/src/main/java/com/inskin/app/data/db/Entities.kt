package com.inskin.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tag_snapshots")
data class TagSnapshotEntity(
    @PrimaryKey val uidHex: String,
    val name: String = "Tag",
    val form: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val lastEdit: Long = System.currentTimeMillis(),
    val typeLabel: String? = null,
    val typeDetail: String? = null,
    val locked: Boolean = false,
    val usedBytes: Int = -1,
    val ndefCapacity: Int = 0,
    val totalBytes: Int = 0,
    val detailsJson: String? = null
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
    val source: String = "android",
    val format: String = "bin",
    val bytes: ByteArray
)

/** Nouvelle table: chaque scan = un enregistrement complet */
@Entity(
    tableName = "tag_reads",
    indices = [Index("uidHex"), Index("readAt")],
    foreignKeys = [ForeignKey(
        entity = TagSnapshotEntity::class,
        parentColumns = ["uidHex"],
        childColumns = ["uidHex"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TagReadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uidHex: String,
    val readAt: Long = System.currentTimeMillis(),
    val source: String = "android",   // ex: android, pm3
    val device: String? = null,       // Build.MODEL / pm3 serial
    val appVersion: String? = null,   // versionName
    // Paramètres bas niveau connus (si disponibles)
    val techsJson: String? = null,    // liste des techs Android
    val atqaHex: String? = null,
    val sakHex: String? = null,
    val atsHex: String? = null,
    val hfType: String? = null,       // "NTAG215", "MIFARE Classic 1K", etc.
    val ndefPresent: Boolean? = null,
    val ndefWritable: Boolean? = null,
    val ndefCapacity: Int? = null,
    val totalBytes: Int? = null,
    val rawReadableBytes: Int? = null,
    val versionHex: String? = null,
    val memoryLayout: String? = null,
    val signatureHex: String? = null, // Ultralight/NTAG signature si lue
    val countersJson: String? = null, // compteurs (UL-C/NTAG21x) si lus
    val detailsJson: String? = null   // TagDetails complet (sans blobs)
)

/** NDEF: enregistre chaque record décodé */
@Entity(
    tableName = "tag_ndef_records",
    indices = [Index("readId")],
    foreignKeys = [ForeignKey(
        entity = TagReadEntity::class,
        parentColumns = ["id"],
        childColumns = ["readId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TagNdefRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val readId: Long,
    val orderIndex: Int,
    val tnf: Int,
    val type: String?,
    val payloadHex: String?,   // hexdump brut
    val idHex: String?,
    // Champs dérivés courants
    val text: String?,
    val lang: String?,
    val uri: String?,
    val mimeType: String?,
    val sizeBytes: Int
)

data class TagHistoryItem(
    val uidHex: String,
    val name: String,
    val form: String?,
    val lastSeen: Long,
    val lastEdit: Long
)
