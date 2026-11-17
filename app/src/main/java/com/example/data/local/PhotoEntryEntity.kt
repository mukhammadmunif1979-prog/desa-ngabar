package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_entries",
    foreignKeys = [
        ForeignKey(
            entity = VillageEntity::class,
            parentColumns = ["id"],
            childColumns = ["villageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["villageId"]),
        Index(value = ["villageId", "year", "month"])
    ]
)
data class PhotoEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val villageId: Long,
    val year: Int,
    val month: Int,
    val filePath: String,
    val fileName: String,
    val caption: String,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
