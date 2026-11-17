package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.VillageEntity
import com.example.data.model.IndonesianMonth
import com.example.ui.viewmodel.PhotoDetailUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoDetailScreen(
    uiState: PhotoDetailUiState,
    availableYears: List<Int>,
    snackbarHostState: SnackbarHostState,
    getShareableUri: (String) -> Uri?,
    onBackClick: () -> Unit,
    onUpdateCaption: (Long, String) -> Unit,
    onMovePhoto: (photoId: Long, targetVillageId: Long, targetYear: Int, targetMonth: Int) -> Unit,
    onDeletePhoto: (Long) -> Unit
) {
    val context = LocalContext.current
    var showEditCaptionDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Detail Foto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (uiState is PhotoDetailUiState.Success) {
                        IconButton(
                            onClick = {
                                val shareUri = getShareableUri(uiState.photo.filePath)
                                if (shareUri != null) {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${uiState.village.name} - ${uiState.month.displayName} ${uiState.photo.year}"
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Bagikan Foto Desa")
                                    )
                                }
                            },
                            modifier = Modifier.testTag("share_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Bagikan foto"
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("delete_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Hapus foto",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState) {
                is PhotoDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is PhotoDetailUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is PhotoDetailUiState.Success -> {
                    val photo = uiState.photo
                    val dateText = remember(photo.createdAt) {
                        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
                            .format(Date(photo.createdAt))
                    }
                    val fileSizeText = remember(photo.fileSizeBytes) {
                        val kb = photo.fileSizeBytes / 1024.0
                        if (kb >= 1024.0) String.format(Locale.US, "%.2f MB", kb / 1024.0)
                        else String.format(Locale.US, "%.0f KB", kb)
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Photo Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF101815)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(photo.filePath),
                                contentDescription = photo.caption.ifBlank { photo.fileName },
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Metadata Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${uiState.village.name} • ${uiState.month.displayName} ${photo.year}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = photo.caption.ifBlank { "Belum ada keterangan kegiatan" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (photo.caption.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Tanggal Simpan: $dateText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Nama File: ${photo.fileName} ($fileSizeText)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Lokasi Folder: ${uiState.relativeFolderPath}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEditCaptionDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("edit_caption_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ubah Keterangan")
                            }

                            Button(
                                onClick = { showMoveDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("move_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DriveFileMove,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pindah Folder")
                            }
                        }
                    }

                    if (showEditCaptionDialog) {
                        var editedCaption by remember(photo.caption) { mutableStateOf(photo.caption) }
                        AlertDialog(
                            onDismissRequest = { showEditCaptionDialog = false },
                            title = { Text("Ubah Keterangan Foto") },
                            text = {
                                OutlinedTextField(
                                    value = editedCaption,
                                    onValueChange = { editedCaption = it },
                                    label = { Text("Keterangan Kegiatan") },
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("edit_caption_input")
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onUpdateCaption(photo.id, editedCaption)
                                        showEditCaptionDialog = false
                                    },
                                    modifier = Modifier.testTag("save_caption_button")
                                ) {
                                    Text("Simpan")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditCaptionDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }

                    if (showMoveDialog) {
                        MovePhotoDialog(
                            currentVillageId = photo.villageId,
                            currentYear = photo.year,
                            currentMonth = photo.month,
                            villages = uiState.allVillages,
                            availableYears = availableYears,
                            onDismiss = { showMoveDialog = false },
                            onConfirmMove = { targetVillageId, targetYear, targetMonth ->
                                onMovePhoto(photo.id, targetVillageId, targetYear, targetMonth)
                                showMoveDialog = false
                            }
                        )
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Hapus Foto Ini?") },
                            text = { Text("Foto akan dihapus permanen dari folder ${uiState.village.name} (${uiState.month.displayName} ${photo.year}).") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteDialog = false
                                        onDeletePhoto(photo.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("confirm_delete_photo_button")
                                ) {
                                    Text("Hapus")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Batal")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovePhotoDialog(
    currentVillageId: Long,
    currentYear: Int,
    currentMonth: Int,
    villages: List<VillageEntity>,
    availableYears: List<Int>,
    onDismiss: () -> Unit,
    onConfirmMove: (villageId: Long, year: Int, month: Int) -> Unit
) {
    var targetVillageId by remember { mutableLongStateOf(currentVillageId) }
    var targetYear by remember { mutableIntStateOf(currentYear) }
    var targetMonth by remember { mutableIntStateOf(currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindahkan ke Folder Lain") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Desa Tujuan", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    villages.forEach { village ->
                        FilterChip(
                            selected = village.id == targetVillageId,
                            onClick = { targetVillageId = village.id },
                            label = { Text(village.name) }
                        )
                    }
                }

                Text("Tahun", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableYears.forEach { yr ->
                        FilterChip(
                            selected = yr == targetYear,
                            onClick = { targetYear = yr },
                            label = { Text(yr.toString()) }
                        )
                    }
                }

                Text("Bulan", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IndonesianMonth.entries.forEach { m ->
                        FilterChip(
                            selected = m.number == targetMonth,
                            onClick = { targetMonth = m.number },
                            label = { Text(m.shortName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmMove(targetVillageId, targetYear, targetMonth) },
                modifier = Modifier.testTag("confirm_move_photo_button")
            ) {
                Text("Pindahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
