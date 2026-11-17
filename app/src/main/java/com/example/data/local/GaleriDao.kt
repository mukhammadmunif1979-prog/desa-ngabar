package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GaleriDao {
    @Query("SELECT * FROM villages ORDER BY sortOrder ASC, id ASC")
    fun observeAllVillages(): Flow<List<VillageEntity>>

    @Query("SELECT * FROM villages ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllVillagesOnce(): List<VillageEntity>

    @Query("SELECT * FROM villages WHERE id = :villageId LIMIT 1")
    fun observeVillageById(villageId: Long): Flow<VillageEntity?>

    @Query("SELECT * FROM villages WHERE id = :villageId LIMIT 1")
    suspend fun getVillageByIdOnce(villageId: Long): VillageEntity?

    @Query("SELECT COUNT(*) FROM villages")
    suspend fun countVillages(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillage(village: VillageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillages(villages: List<VillageEntity>)

    @Query("DELETE FROM villages WHERE id = :villageId")
    suspend fun deleteVillageById(villageId: Long)

    @Query("SELECT * FROM photo_entries WHERE year = :year ORDER BY createdAt DESC")
    fun observePhotosByYear(year: Int): Flow<List<PhotoEntryEntity>>

    @Query("SELECT * FROM photo_entries ORDER BY createdAt DESC")
    fun observeAllPhotos(): Flow<List<PhotoEntryEntity>>

    @Query("SELECT * FROM photo_entries WHERE villageId = :villageId AND year = :year ORDER BY month ASC, createdAt DESC")
    fun observePhotosForVillageYear(villageId: Long, year: Int): Flow<List<PhotoEntryEntity>>

    @Query("SELECT * FROM photo_entries WHERE villageId = :villageId AND year = :year AND month = :month ORDER BY createdAt DESC")
    fun observePhotosForMonth(villageId: Long, year: Int, month: Int): Flow<List<PhotoEntryEntity>>

    @Query("SELECT * FROM photo_entries WHERE id = :photoId LIMIT 1")
    fun observePhotoById(photoId: Long): Flow<PhotoEntryEntity?>

    @Query("SELECT * FROM photo_entries WHERE id = :photoId LIMIT 1")
    suspend fun getPhotoByIdOnce(photoId: Long): PhotoEntryEntity?

    @Query("SELECT * FROM photo_entries WHERE villageId = :villageId")
    suspend fun getPhotosForVillageOnce(villageId: Long): List<PhotoEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntryEntity): Long

    @Update
    suspend fun updatePhoto(photo: PhotoEntryEntity)

    @Query("DELETE FROM photo_entries WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)
}
