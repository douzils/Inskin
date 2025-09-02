package com.inskin.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TagSnapshotEntity::class,
        TagDumpEntity::class,
        TagReadEntity::class,
        TagNdefRecordEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDb : RoomDatabase() {
    abstract fun tagDao(): TagDao
}
