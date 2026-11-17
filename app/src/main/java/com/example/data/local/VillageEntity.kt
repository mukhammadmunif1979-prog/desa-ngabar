package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "villages")
data class VillageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val folderSlug: String,
    val colorIndex: Int,
    val sortOrder: Int,
    val isDefault: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
