package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PhotoFileStorage
import com.example.data.local.VillageEntity
import com.example.data.model.IndonesianMonth
import com.example.data.repository.GaleriRepository
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GaleriViewModel(
    private val repository: GaleriRepository
) : ViewModel() {

    private val currentCalendarYear = Calendar.getInstance().get(Calendar.YEAR)
    val availableYears: List<Int> = ((currentCalendarYear - 2)..(currentCalendarYear + 1)).toList()

    private val _selectedYear = MutableStateFlow(currentCalendarYear)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pendingImport = MutableStateFlow<PendingPhotoImport?>(null)
    val pendingImport: StateFlow<PendingPhotoImport?> = _pendingImport.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>()
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val allVillages: StateFlow<List<VillageEntity>> = repository.villages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        repository.villages,
        _selectedYear.flatMapLatest { year -> repository.observePhotosByYear(year) },
        _selectedYear,
        _searchQuery
    ) { villages, photosInYear, year, query ->
        val trimmedQuery = query.trim().lowercase()
        val filteredVillages = if (trimmedQuery.isEmpty()) {
            villages
        } else {
            villages.filter { village ->
                val matchesName = village.name.lowercase().contains(trimmedQuery)
                val matchesCaption = photosInYear.any {
                    it.villageId == village.id && it.caption.lowercase().contains(trimmedQuery)
                }
                matchesName || matchesCaption
            }
        }

        val items = filteredVillages.map { village ->
            val villagePhotos = photosInYear.filter { it.villageId == village.id }
            val filledMonths = villagePhotos.map { it.month }.distinct().size
            VillageDashboardItem(
                village = village,
                totalPhotosInYear = villagePhotos.size,
                filledMonthsCount = filledMonths,
                latestPhotoPath = villagePhotos.firstOrNull()?.filePath
            )
        }

        val activeVillages = villages.count { v -> photosInYear.any { it.villageId == v.id } }

        DashboardUiState.Success(
            selectedYear = year,
            searchQuery = query,
            villages = items,
            totalPhotosInYear = photosInYear.size,
            activeVillagesCount = activeVillages,
            recentPhotos = photosInYear.take(6)
        ) as DashboardUiState
    }
        .catch { emit(DashboardUiState.Error(it.message ?: "Gagal memuat dashboard")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureDefaultVillagesSeeded()
        }
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun observeVillageFolder(villageId: Long): Flow<VillageFolderUiState> {
        return combine(
            repository.observeVillage(villageId),
            _selectedYear.flatMapLatest { year ->
                repository.observePhotosForVillageYear(villageId, year).map { year to it }
            }
        ) { village, (year, photos) ->
            if (village == null) {
                VillageFolderUiState.Error("Folder desa tidak ditemukan")
            } else {
                val monthSummaries = IndonesianMonth.entries.map { month ->
                    val monthPhotos = photos.filter { it.month == month.number }
                    MonthFolderSummary(
                        month = month,
                        photoCount = monthPhotos.size,
                        coverPhotoPath = monthPhotos.firstOrNull()?.filePath
                    )
                }
                VillageFolderUiState.Success(
                    village = village,
                    selectedYear = year,
                    monthSummaries = monthSummaries,
                    totalPhotos = photos.size,
                    filledMonths = monthSummaries.count { it.photoCount > 0 }
                )
            }
        }.catch { emit(VillageFolderUiState.Error(it.message ?: "Gagal memuat folder desa")) }
    }

    fun observeMonthGallery(villageId: Long, year: Int, monthNumber: Int): Flow<MonthGalleryUiState> {
        val month = IndonesianMonth.fromNumber(monthNumber)
        return combine(
            repository.observeVillage(villageId),
            repository.observePhotosForMonth(villageId, year, monthNumber)
        ) { village, photos ->
            if (village == null) {
                MonthGalleryUiState.Error("Folder desa tidak ditemukan")
            } else {
                MonthGalleryUiState.Success(
                    village = village,
                    year = year,
                    month = month,
                    relativeFolderPath = repository.fileStorage.getRelativeDisplayPath(
                        village.folderSlug,
                        year,
                        monthNumber
                    ),
                    photos = photos
                )
            }
        }.catch { emit(MonthGalleryUiState.Error(it.message ?: "Gagal memuat foto bulan")) }
    }

    fun observePhotoDetail(photoId: Long): Flow<PhotoDetailUiState> {
        return combine(
            repository.observePhoto(photoId),
            repository.villages
        ) { photo, villages ->
            if (photo == null) {
                PhotoDetailUiState.Error("Foto tidak ditemukan atau telah dihapus")
            } else {
                val village = villages.firstOrNull { it.id == photo.villageId }
                if (village == null) {
                    PhotoDetailUiState.Error("Folder desa tidak ditemukan")
                } else {
                    PhotoDetailUiState.Success(
                        photo = photo,
                        village = village,
                        month = IndonesianMonth.fromNumber(photo.month),
                        relativeFolderPath = repository.fileStorage.getRelativeDisplayPath(
                            village.folderSlug,
                            photo.year,
                            photo.month
                        ),
                        allVillages = villages
                    )
                }
            }
        }.catch { emit(PhotoDetailUiState.Error(it.message ?: "Gagal memuat detail foto")) }
    }

    fun createCameraDestination(): Pair<Uri, File> = repository.fileStorage.createCameraTempUri()

    fun getShareableUri(filePath: String): Uri? = repository.fileStorage.getShareableUri(filePath)

    fun startGalleryImport(
        uris: List<Uri>,
        villageId: Long,
        year: Int = _selectedYear.value,
        month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    ) {
        if (uris.isEmpty()) return
        _pendingImport.value = PendingPhotoImport(
            uris = uris,
            initialVillageId = villageId,
            initialYear = year,
            initialMonth = month
        )
    }

    fun startCameraImport(
        tempFile: File,
        previewUri: Uri,
        villageId: Long,
        year: Int = _selectedYear.value,
        month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    ) {
        _pendingImport.value = PendingPhotoImport(
            cameraTempFile = tempFile,
            cameraPreviewUri = previewUri,
            initialVillageId = villageId,
            initialYear = year,
            initialMonth = month
        )
    }

    fun cancelPendingImport() {
        val current = _pendingImport.value
        current?.cameraTempFile?.let { file ->
            if (file.exists()) file.delete()
        }
        _pendingImport.value = null
    }

    fun confirmImport(
        villageId: Long,
        year: Int,
        month: Int,
        caption: String
    ) {
        val pending = _pendingImport.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val villageName = allVillages.value.firstOrNull { it.id == villageId }?.name ?: "Desa"
                val monthName = IndonesianMonth.fromNumber(month).displayName
                if (pending.cameraTempFile != null) {
                    repository.saveCapturedPhoto(
                        tempFile = pending.cameraTempFile,
                        villageId = villageId,
                        year = year,
                        month = month,
                        caption = caption
                    )
                    _snackbarMessages.emit("Foto kamera disimpan ke $villageName • $monthName $year")
                } else if (pending.uris.isNotEmpty()) {
                    val count = repository.importPhotosFromUris(
                        uris = pending.uris,
                        villageId = villageId,
                        year = year,
                        month = month,
                        caption = caption
                    )
                    _snackbarMessages.emit("$count foto disimpan ke $villageName • $monthName $year")
                }
                _pendingImport.value = null
            } catch (e: Exception) {
                _snackbarMessages.emit("Gagal menyimpan foto: ${e.message ?: "Terjadi kesalahan"}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun addCustomVillage(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val created = repository.addCustomVillage(trimmed)
                _snackbarMessages.emit("Folder desa \"${created.name}\" berhasil dibuat")
            } catch (e: Exception) {
                _snackbarMessages.emit("Gagal menambah folder desa")
            }
        }
    }

    fun deleteCustomVillage(village: VillageEntity) {
        viewModelScope.launch {
            try {
                repository.deleteVillage(village)
                _snackbarMessages.emit("Folder \"${village.name}\" dihapus")
            } catch (e: Exception) {
                _snackbarMessages.emit("Gagal menghapus folder desa")
            }
        }
    }

    fun updatePhotoCaption(photoId: Long, newCaption: String) {
        viewModelScope.launch {
            try {
                repository.updateCaption(photoId, newCaption)
                _snackbarMessages.emit("Keterangan foto diperbarui")
            } catch (e: Exception) {
                _snackbarMessages.emit("Gagal memperbarui keterangan")
            }
        }
    }

    fun movePhoto(photoId: Long, targetVillageId: Long, targetYear: Int, targetMonth: Int) {
        viewModelScope.launch {
            try {
                repository.movePhoto(photoId, targetVillageId, targetYear, targetMonth)
                val targetName = allVillages.value.firstOrNull { it.id == targetVillageId }?.name ?: "Desa"
                val monthName = IndonesianMonth.fromNumber(targetMonth).displayName
                _snackbarMessages.emit("Foto dipindahkan ke $targetName • $monthName $targetYear")
            } catch (e: Exception) {
                _snackbarMessages.emit("Gagal memindahkan foto")
            }
        }
    }

    fun deletePhoto(photoId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePhoto(photoId)
                _snackbarMessages.emit("Foto berhasil dihapus")
                onDeleted()
            } catch (e: Exception) {
                _snackbarMessages.emit("Gagal menghapus foto")
            }
        }
    }
}

class GaleriViewModelFactory(
    private val repository: GaleriRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GaleriViewModel::class.java)) {
            return GaleriViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
