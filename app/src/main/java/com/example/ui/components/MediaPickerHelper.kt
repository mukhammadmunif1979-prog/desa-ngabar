package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import java.io.File

class MediaPickerController(
    val launchGallery: (villageId: Long, year: Int, month: Int) -> Unit,
    val launchCamera: (villageId: Long, year: Int, month: Int) -> Unit
)

@Composable
fun rememberMediaPickerController(
    createCameraDestination: () -> Pair<Uri, File>,
    onGalleryPicked: (uris: List<Uri>, villageId: Long, year: Int, month: Int) -> Unit,
    onCameraCaptured: (tempFile: File, previewUri: Uri, villageId: Long, year: Int, month: Int) -> Unit,
    onCameraFailedOrCancelled: (File?) -> Unit
): MediaPickerController {
    val context = LocalContext.current

    var targetVillageId by remember { mutableStateOf(1L) }
    var targetYear by remember { mutableStateOf(2026) }
    var targetMonth by remember { mutableStateOf(1) }

    var pendingTempFile by remember { mutableStateOf<File?>(null) }
    var pendingTempUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onGalleryPicked(uris, targetVillageId, targetYear, targetMonth)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingTempFile
        val uri = pendingTempUri
        pendingTempFile = null
        pendingTempUri = null
        if (success && file != null && uri != null && file.exists() && file.length() > 0L) {
            onCameraCaptured(file, uri, targetVillageId, targetYear, targetMonth)
        } else {
            onCameraFailedOrCancelled(file)
        }
    }

    fun startCameraCapture() {
        val (uri, file) = createCameraDestination()
        pendingTempUri = uri
        pendingTempFile = file
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCameraCapture()
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Izin Kamera Diperlukan") },
            text = {
                Text("Untuk mengambil foto langsung menggunakan kamera, izinkan akses kamera di pengaturan perangkat atau gunakan tombol Pilih Galeri.")
            },
            confirmButton = {
                Button(
                    onClick = { showPermissionDialog = false },
                    modifier = Modifier.testTag("camera_permission_ok_button")
                ) {
                    Text("Mengerti")
                }
            }
        )
    }

    return remember(context) {
        MediaPickerController(
            launchGallery = { villageId, year, month ->
                targetVillageId = villageId
                targetYear = year
                targetMonth = month
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            launchCamera = { villageId, year, month ->
                targetVillageId = villageId
                targetYear = year
                targetMonth = month
                if (hasCameraPermission(context)) {
                    startCameraCapture()
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }
}

private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
