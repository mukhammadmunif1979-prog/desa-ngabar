package com.example.data.repository

import android.net.Uri
import com.example.data.local.GaleriDao
import com.example.data.local.GaleriDatabase
import com.example.data.local.PhotoEntryEntity
import com.example.data.local.PhotoFileStorage
import com.example.data.local.VillageEntity
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GaleriRepository(
    private val dao: GaleriDao,
    val fileStorage: PhotoFileStorage
) {
    val villages: Flow<List<VillageEntity>> = dao.observeAllVillages()
    val allPhotos: Flow<List<PhotoEntryEntity>> = dao.observeAllPhotos()

    suspend fun ensureDefaultVillagesSeeded() = withContext(Dispatchers.IO) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (dao.countVillages() == 0) {
            dao.insertVillages(GaleriDatabase.DEFAULT_VILLAGES)
        }
        dao.getAllVillagesOnce().forEach { village ->
            fileStorage.ensureVillageYearFolders(village.folderSlug, currentYear)
        }
    }

    fun observeVillage(villageId: Long): Flow<VillageEntity?> = dao.observeVillageById(villageId)

    fun observePhotosByYear(year: Int): Flow<List<PhotoEntryEntity>> = dao.observePhotosByYear(year)

    fun observePhotosForVillageYear(villageId: Long, year: Int): Flow<List<PhotoEntryEntity>> =
        dao.observePhotosForVillageYear(villageId, year)

    fun observePhotosForMonth(villageId: Long, year: Int, month: Int): Flow<List<PhotoEntryEntity>> =
        dao.observePhotosForMonth(villageId, year, month)

    fun observePhoto(photoId: Long): Flow<PhotoEntryEntity?> = dao.observePhotoById(photoId)

    suspend fun addCustomVillage(name: String): VillageEntity = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        val existing = dao.getAllVillagesOnce()
        val baseSlug = trimmed.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "desa_${System.currentTimeMillis()}" }
        val uniqueSlug = if (existing.any { it.folderSlug == baseSlug }) {
            "${baseSlug}_${System.currentTimeMillis() % 1000}"
        } else {
            baseSlug
        }
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val colorIdx = existing.size % 8
        val newEntity = VillageEntity(
            name = trimmed,
            folderSlug = uniqueSlug,
            colorIndex = colorIdx,
            sortOrder = nextOrder,
            isDefault = false
        )
        val id = dao.insertVillage(newEntity)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        fileStorage.ensureVillageYearFolders(uniqueSlug, currentYear)
        newEntity.copy(id = id)
    }

    suspend fun deleteVillage(village: VillageEntity) = withContext(Dispatchers.IO) {
        val photos = dao.getPhotosForVillageOnce(village.id)
        photos.forEach { fileStorage.deleteFile(it.filePath) }
        fileStorage.deleteVillageDirectory(village.folderSlug)
        dao.deleteVillageById(village.id)
    }

    suspend fun importPhotosFromUris(
        uris: List<Uri>,
        villageId: Long,
        year: Int,
        month: Int,
        caption: String
    ): Int = withContext(Dispatchers.IO) {
        val village = dao.getVillageByIdOnce(villageId) ?: return@withContext 0
        fileStorage.ensureVillageYearFolders(village.folderSlug, year)
        var savedCount = 0
        uris.forEachIndexed { index, uri ->
            val stored = fileStorage.saveFromUri(
                sourceUri = uri,
                folderSlug = village.folderSlug,
                year = year,
                monthNumber = month,
                sequenceIndex = index + 1
            )
            dao.insertPhoto(
                PhotoEntryEntity(
                    villageId = villageId,
                    year = year,
                    month = month,
                    filePath = stored.absolutePath,
                    fileName = stored.fileName,
                    caption = caption.trim(),
                    fileSizeBytes = stored.sizeBytes
                )
            )
            savedCount++
        }
        savedCount
    }

    suspend fun saveCapturedPhoto(
        tempFile: File,
        villageId: Long,
        year: Int,
        month: Int,
        caption: String
    ): Long = withContext(Dispatchers.IO) {
        val village = dao.getVillageByIdOnce(villageId) ?: return@withContext -1L
        fileStorage.ensureVillageYearFolders(village.folderSlug, year)
        val stored = fileStorage.saveFromTempFile(
            tempFile = tempFile,
            folderSlug = village.folderSlug,
            year = year,
            monthNumber = month
        )
        dao.insertPhoto(
            PhotoEntryEntity(
                villageId = villageId,
                year = year,
                month = month,
                filePath = stored.absolutePath,
                fileName = stored.fileName,
                caption = caption.trim(),
                fileSizeBytes = stored.sizeBytes
            )
        )
    }

    suspend fun updateCaption(photoId: Long, newCaption: String) = withContext(Dispatchers.IO) {
        val existing = dao.getPhotoByIdOnce(photoId) ?: return@withContext
        dao.updatePhoto(existing.copy(caption = newCaption.trim()))
    }

    suspend fun movePhoto(
        photoId: Long,
        targetVillageId: Long,
        targetYear: Int,
        targetMonth: Int
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getPhotoByIdOnce(photoId) ?: return@withContext
        val targetVillage = dao.getVillageByIdOnce(targetVillageId) ?: return@withContext
        fileStorage.ensureVillageYearFolders(targetVillage.folderSlug, targetYear)
        val moved = fileStorage.movePhotoFile(
            currentPath = existing.filePath,
            targetFolderSlug = targetVillage.folderSlug,
            targetYear = targetYear,
            targetMonthNumber = targetMonth
        )
        dao.updatePhoto(
            existing.copy(
                villageId = targetVillageId,
                year = targetYear,
                month = targetMonth,
                filePath = moved.absolutePath,
                fileName = moved.fileName,
                fileSizeBytes = moved.sizeBytes
            )
        )
    }

    suspend fun deletePhoto(photoId: Long) = withContext(Dispatchers.IO) {
        val existing = dao.getPhotoByIdOnce(photoId) ?: return@withContext
        fileStorage.deleteFile(existing.filePath)
        dao.deletePhotoById(photoId)
    }
}
