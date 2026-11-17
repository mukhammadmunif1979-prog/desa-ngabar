package com.example.ui.viewmodel

import android.net.Uri
import com.example.data.local.PhotoEntryEntity
import com.example.data.local.VillageEntity
import com.example.data.model.IndonesianMonth
import java.io.File

data class VillageDashboardItem(
    val village: VillageEntity,
    val totalPhotosInYear: Int,
    val filledMonthsCount: Int,
    val latestPhotoPath: String?
)

data class MonthFolderSummary(
    val month: IndonesianMonth,
    val photoCount: Int,
    val coverPhotoPath: String?
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val selectedYear: Int,
        val searchQuery: String,
        val villages: List<VillageDashboardItem>,
        val totalPhotosInYear: Int,
        val activeVillagesCount: Int,
        val recentPhotos: List<PhotoEntryEntity>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

sealed interface VillageFolderUiState {
    data object Loading : VillageFolderUiState
    data class Success(
        val village: VillageEntity,
        val selectedYear: Int,
        val monthSummaries: List<MonthFolderSummary>,
        val totalPhotos: Int,
        val filledMonths: Int
    ) : VillageFolderUiState
    data class Error(val message: String) : VillageFolderUiState
}

sealed interface MonthGalleryUiState {
    data object Loading : MonthGalleryUiState
    data class Success(
        val village: VillageEntity,
        val year: Int,
        val month: IndonesianMonth,
        val relativeFolderPath: String,
        val photos: List<PhotoEntryEntity>
    ) : MonthGalleryUiState
    data class Error(val message: String) : MonthGalleryUiState
}

sealed interface PhotoDetailUiState {
    data object Loading : PhotoDetailUiState
    data class Success(
        val photo: PhotoEntryEntity,
        val village: VillageEntity,
        val month: IndonesianMonth,
        val relativeFolderPath: String,
        val allVillages: List<VillageEntity>
    ) : PhotoDetailUiState
    data class Error(val message: String) : PhotoDetailUiState
}

data class PendingPhotoImport(
    val uris: List<Uri> = emptyList(),
    val cameraTempFile: File? = null,
    val cameraPreviewUri: Uri? = null,
    val initialVillageId: Long,
    val initialYear: Int,
    val initialMonth: Int
)
