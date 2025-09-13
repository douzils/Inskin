package com.inskin.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
  @Insert suspend fun insert(item: Item): Long
  @Query("SELECT * FROM items ORDER BY createdAt DESC")
  fun observeAll(): Flow<List<Item>>
}
