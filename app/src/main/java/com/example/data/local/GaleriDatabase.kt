package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VillageEntity::class, PhotoEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GaleriDatabase : RoomDatabase() {
    abstract fun galeriDao(): GaleriDao

    companion object {
        @Volatile
        private var INSTANCE: GaleriDatabase? = null

        val DEFAULT_VILLAGES = listOf(
            VillageEntity(name = "Ngabar Selatan", folderSlug = "ngabar_selatan", colorIndex = 0, sortOrder = 1, isDefault = true),
            VillageEntity(name = "Ngabar Timur", folderSlug = "ngabar_timur", colorIndex = 1, sortOrder = 2, isDefault = true),
            VillageEntity(name = "Ngabar Krajan", folderSlug = "ngabar_krajan", colorIndex = 2, sortOrder = 3, isDefault = true),
            VillageEntity(name = "Sukodani Krajan", folderSlug = "sukodani_krajan", colorIndex = 3, sortOrder = 4, isDefault = true),
            VillageEntity(name = "Karang Nongko", folderSlug = "karang_nongko", colorIndex = 4, sortOrder = 5, isDefault = true),
            VillageEntity(name = "Sukodani Barat", folderSlug = "sukodani_barat", colorIndex = 5, sortOrder = 6, isDefault = true),
            VillageEntity(name = "Sukodani Utara", folderSlug = "sukodani_utara", colorIndex = 6, sortOrder = 7, isDefault = true)
        )

        fun getInstance(context: Context): GaleriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GaleriDatabase::class.java,
                    "galeri_desa.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
