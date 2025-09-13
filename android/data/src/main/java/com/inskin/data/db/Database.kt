package com.inskin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Item::class], version = 1, exportSchema = true)
abstract class InskinDatabase : RoomDatabase() {
  abstract fun items(): ItemDao

  companion object {
    @Volatile private var INSTANCE: InskinDatabase? = null
    fun get(context: Context): InskinDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          InskinDatabase::class.java,
          "inskin.db"
        ).build().also { INSTANCE = it }
      }
  }
}
