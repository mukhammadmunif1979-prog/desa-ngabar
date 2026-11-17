package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.IndonesianMonth
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredPhotoFile(
    val absolutePath: String,
    val fileName: String,
    val sizeBytes: Long
)

class PhotoFileStorage(private val context: Context) {
    private val rootDir: File
        get() = File(context.filesDir, "GaleriDesa").apply { if (!exists()) mkdirs() }

    private val cameraTempDir: File
        get() = File(context.cacheDir, "camera_temp").apply { if (!exists()) mkdirs() }

    fun ensureVillageYearFolders(folderSlug: String, year: Int) {
        val yearDir = File(File(rootDir, folderSlug), year.toString())
        if (!yearDir.exists()) yearDir.mkdirs()
        IndonesianMonth.entries.forEach { month ->
            val monthDir = File(yearDir, month.folderName)
            if (!monthDir.exists()) monthDir.mkdirs()
        }
    }

    fun getRelativeDisplayPath(folderSlug: String, year: Int, monthNumber: Int): String {
        val month = IndonesianMonth.fromNumber(monthNumber)
        return "GaleriDesa/$folderSlug/$year/${month.folderName}"
    }

    fun createCameraTempUri(): Pair<Uri, File> {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val tempFile = File(cameraTempDir, "CAM_${timestamp}_${System.nanoTime() % 10000}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    fun getShareableUri(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun saveFromUri(
        sourceUri: Uri,
        folderSlug: String,
        year: Int,
        monthNumber: Int,
        sequenceIndex: Int = 0
    ): StoredPhotoFile = withContext(Dispatchers.IO) {
        val month = IndonesianMonth.fromNumber(monthNumber)
        val targetDir = File(File(File(rootDir, folderSlug), year.toString()), month.folderName)
        if (!targetDir.exists()) targetDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val suffix = if (sequenceIndex > 0) "_$sequenceIndex" else ""
        val fileName = "IMG_${month.shortName.uppercase(Locale.US)}_${timestamp}${suffix}.jpg"
        val destFile = File(targetDir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Gagal membaca file foto yang dipilih.")

        StoredPhotoFile(
            absolutePath = destFile.absolutePath,
            fileName = destFile.name,
            sizeBytes = destFile.length()
        )
    }

    suspend fun saveFromTempFile(
        tempFile: File,
        folderSlug: String,
        year: Int,
        monthNumber: Int
    ): StoredPhotoFile = withContext(Dispatchers.IO) {
        val month = IndonesianMonth.fromNumber(monthNumber)
        val targetDir = File(File(File(rootDir, folderSlug), year.toString()), month.folderName)
        if (!targetDir.exists()) targetDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "CAM_${month.shortName.uppercase(Locale.US)}_${timestamp}.jpg"
        val destFile = File(targetDir, fileName)

        tempFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile.delete()

        StoredPhotoFile(
            absolutePath = destFile.absolutePath,
            fileName = destFile.name,
            sizeBytes = destFile.length()
        )
    }

    suspend fun movePhotoFile(
        currentPath: String,
        targetFolderSlug: String,
        targetYear: Int,
        targetMonthNumber: Int
    ): StoredPhotoFile = withContext(Dispatchers.IO) {
        val sourceFile = File(currentPath)
        val month = IndonesianMonth.fromNumber(targetMonthNumber)
        val targetDir = File(File(File(rootDir, targetFolderSlug), targetYear.toString()), month.folderName)
        if (!targetDir.exists()) targetDir.mkdirs()

        val destFile = File(targetDir, sourceFile.name)
        if (sourceFile.absolutePath == destFile.absolutePath) {
            return@withContext StoredPhotoFile(
                absolutePath = sourceFile.absolutePath,
                fileName = sourceFile.name,
                sizeBytes = sourceFile.length()
            )
        }

        if (sourceFile.exists()) {
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            sourceFile.delete()
        }

        StoredPhotoFile(
            absolutePath = destFile.absolutePath,
            fileName = destFile.name,
            sizeBytes = destFile.length()
        )
    }

    suspend fun deleteFile(filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun deleteVillageDirectory(folderSlug: String) = withContext(Dispatchers.IO) {
        val dir = File(rootDir, folderSlug)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }
}
