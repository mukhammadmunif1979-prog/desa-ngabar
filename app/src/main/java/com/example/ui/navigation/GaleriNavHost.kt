package com.example.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ImportPhotoSheet
import com.example.ui.components.rememberMediaPickerController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MonthGalleryScreen
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.screens.VillageFolderScreen
import com.example.ui.viewmodel.GaleriViewModel
import com.example.ui.viewmodel.MonthGalleryUiState
import com.example.ui.viewmodel.PhotoDetailUiState
import com.example.ui.viewmodel.VillageFolderUiState
import java.util.Calendar

object GaleriRoutes {
    const val DASHBOARD = "dashboard"
    const val VILLAGE_FOLDER = "village/{villageId}"
    const val MONTH_GALLERY = "month/{villageId}/{year}/{month}"
    const val PHOTO_DETAIL = "photo/{photoId}"

    fun villageFolder(villageId: Long) = "village/$villageId"
    fun monthGallery(villageId: Long, year: Int, month: Int) = "month/$villageId/$year/$month"
    fun photoDetail(photoId: Long) = "photo/$photoId"
}

@Composable
fun GaleriNavHost(viewModel: GaleriViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardUiState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val allVillages by viewModel.allVillages.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val mediaPicker = rememberMediaPickerController(
        createCameraDestination = viewModel::createCameraDestination,
        onGalleryPicked = { uris, villageId, year, month ->
            viewModel.startGalleryImport(uris, villageId, year, month)
        },
        onCameraCaptured = { tempFile, previewUri, villageId, year, month ->
            viewModel.startCameraImport(tempFile, previewUri, villageId, year, month)
        },
        onCameraFailedOrCancelled = { tempFile ->
            tempFile?.let { if (it.exists()) it.delete() }
        }
    )

    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) + 1 }

    NavHost(
        navController = navController,
        startDestination = GaleriRoutes.DASHBOARD
    ) {
        composable(GaleriRoutes.DASHBOARD) {
            DashboardScreen(
                uiState = dashboardUiState,
                availableYears = viewModel.availableYears,
                snackbarHostState = snackbarHostState,
                onSelectYear = viewModel::selectYear,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onVillageClick = { village ->
                    navController.navigate(GaleriRoutes.villageFolder(village.id))
                },
                onQuickGalleryClick = { defaultVillageId ->
                    mediaPicker.launchGallery(defaultVillageId, selectedYear, currentMonth)
                },
                onQuickCameraClick = { defaultVillageId ->
                    mediaPicker.launchCamera(defaultVillageId, selectedYear, currentMonth)
                },
                onAddCustomVillage = viewModel::addCustomVillage,
                onDeleteCustomVillage = viewModel::deleteCustomVillage,
                onPhotoClick = { photoId ->
                    navController.navigate(GaleriRoutes.photoDetail(photoId))
                }
            )
        }

        composable(
            route = GaleriRoutes.VILLAGE_FOLDER,
            arguments = listOf(navArgument("villageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val villageId = backStackEntry.arguments?.getLong("villageId") ?: 1L
            val villageUiState by remember(villageId) {
                viewModel.observeVillageFolder(villageId)
            }.collectAsStateWithLifecycle(initialValue = VillageFolderUiState.Loading)

            VillageFolderScreen(
                uiState = villageUiState,
                availableYears = viewModel.availableYears,
                snackbarHostState = snackbarHostState,
                onBackClick = { navController.popBackStack() },
                onSelectYear = viewModel::selectYear,
                onMonthClick = { vId, yr, month ->
                    navController.navigate(GaleriRoutes.monthGallery(vId, yr, month.number))
                },
                onQuickAddGalleryForMonth = { vId, yr, month ->
                    mediaPicker.launchGallery(vId, yr, month.number)
                },
                onQuickAddCameraForMonth = { vId, yr, month ->
                    mediaPicker.launchCamera(vId, yr, month.number)
                }
            )
        }

        composable(
            route = GaleriRoutes.MONTH_GALLERY,
            arguments = listOf(
                navArgument("villageId") { type = NavType.LongType },
                navArgument("year") { type = NavType.IntType },
                navArgument("month") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val villageId = backStackEntry.arguments?.getLong("villageId") ?: 1L
            val year = backStackEntry.arguments?.getInt("year") ?: selectedYear
            val month = backStackEntry.arguments?.getInt("month") ?: 1

            val monthUiState by remember(villageId, year, month) {
                viewModel.observeMonthGallery(villageId, year, month)
            }.collectAsStateWithLifecycle(initialValue = MonthGalleryUiState.Loading)

            MonthGalleryScreen(
                uiState = monthUiState,
                snackbarHostState = snackbarHostState,
                onBackClick = { navController.popBackStack() },
                onPickGalleryClick = { vId, yr, m ->
                    mediaPicker.launchGallery(vId, yr, m)
                },
                onTakeCameraClick = { vId, yr, m ->
                    mediaPicker.launchCamera(vId, yr, m)
                },
                onPhotoClick = { photoId ->
                    navController.navigate(GaleriRoutes.photoDetail(photoId))
                }
            )
        }

        composable(
            route = GaleriRoutes.PHOTO_DETAIL,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            val detailUiState by remember(photoId) {
                viewModel.observePhotoDetail(photoId)
            }.collectAsStateWithLifecycle(initialValue = PhotoDetailUiState.Loading)

            PhotoDetailScreen(
                uiState = detailUiState,
                availableYears = viewModel.availableYears,
                snackbarHostState = snackbarHostState,
                getShareableUri = viewModel::getShareableUri,
                onBackClick = { navController.popBackStack() },
                onUpdateCaption = viewModel::updatePhotoCaption,
                onMovePhoto = viewModel::movePhoto,
                onDeletePhoto = { id ->
                    viewModel.deletePhoto(id) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }

    pendingImport?.let { pending ->
        ImportPhotoSheet(
            pendingImport = pending,
            villages = allVillages,
            availableYears = viewModel.availableYears,
            isSaving = isSaving,
            onDismiss = viewModel::cancelPendingImport,
            onConfirmSave = viewModel::confirmImport
        )
    }
}
