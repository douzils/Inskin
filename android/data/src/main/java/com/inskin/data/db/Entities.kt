package com.inskin.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val createdAt: Long = System.currentTimeMillis()
)
