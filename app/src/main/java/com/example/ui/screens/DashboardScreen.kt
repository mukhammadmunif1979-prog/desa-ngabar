package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.PhotoEntryEntity
import com.example.data.local.VillageEntity
import com.example.data.model.IndonesianMonth
import com.example.ui.theme.VillageAccentColors
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.VillageDashboardItem
import java.io.File

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    availableYears: List<Int>,
    snackbarHostState: SnackbarHostState,
    onSelectYear: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onVillageClick: (VillageEntity) -> Unit,
    onQuickGalleryClick: (defaultVillageId: Long) -> Unit,
    onQuickCameraClick: (defaultVillageId: Long) -> Unit,
    onAddCustomVillage: (String) -> Unit,
    onDeleteCustomVillage: (VillageEntity) -> Unit,
    onPhotoClick: (Long) -> Unit
) {
    var showAddVillageDialog by remember { mutableStateOf(false) }
    var villagePendingDelete by remember { mutableStateOf<VillageEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState) {
                is DashboardUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DashboardUiState.Error -> {
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

                is DashboardUiState.Success -> {
                    val firstVillageId = uiState.villages.firstOrNull()?.village?.id ?: 1L
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 158.dp),
                        modifier = Modifier
                            .widthIn(max = 920.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 28.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Hero Banner
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            DashboardHeroBanner(
                                selectedYear = uiState.selectedYear,
                                availableYears = availableYears,
                                totalPhotos = uiState.totalPhotosInYear,
                                activeVillages = uiState.activeVillagesCount,
                                totalVillages = uiState.villages.size,
                                onSelectYear = onSelectYear
                            )
                        }

                        // Quick Photo Input Actions
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            QuickInputActionBar(
                                onGalleryClick = { onQuickGalleryClick(firstVillageId) },
                                onCameraClick = { onQuickCameraClick(firstVillageId) }
                            )
                        }

                        // Search Bar
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text("Cari folder desa atau keterangan foto…")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Cari"
                                    )
                                },
                                trailingIcon = {
                                    AnimatedVisibility(
                                        visible = uiState.searchQuery.isNotEmpty(),
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        IconButton(
                                            onClick = { onSearchQueryChange("") },
                                            modifier = Modifier.testTag("clear_search_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Hapus pencarian"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_search_input")
                            )
                        }

                        // Section Header: Folder Wilayah Desa
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Folder Wilayah Desa",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ketuk tombol desa untuk membuka folder Januari – Desember",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { showAddVillageDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("add_village_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreateNewFolder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah")
                                }
                            }
                        }

                        // Empty search result
                        if (uiState.villages.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderOpen,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Folder desa tidak ditemukan",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Coba kata kunci lain atau tambah folder desa baru.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            // Village Folder Dashboard Buttons
                            items(
                                items = uiState.villages,
                                key = { it.village.id }
                            ) { item ->
                                VillageFolderButtonCard(
                                    item = item,
                                    onClick = { onVillageClick(item.village) },
                                    onDeleteClick = if (!item.village.isDefault) {
                                        { villagePendingDelete = item.village }
                                    } else null
                                )
                            }
                        }

                        // Recent Photos Section
                        if (uiState.recentPhotos.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Foto Terbaru (${uiState.selectedYear})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    RecentPhotosStrip(
                                        photos = uiState.recentPhotos,
                                        villages = uiState.villages.map { it.village },
                                        onPhotoClick = onPhotoClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddVillageDialog) {
        AddVillageDialog(
            onDismiss = { showAddVillageDialog = false },
            onConfirm = { name ->
                onAddCustomVillage(name)
                showAddVillageDialog = false
            }
        )
    }

    villagePendingDelete?.let { village ->
        AlertDialog(
            onDismissRequest = { villagePendingDelete = null },
            title = { Text("Hapus Folder Desa?") },
            text = {
                Text("Folder \"${village.name}\" beserta seluruh foto di dalamnya akan dihapus permanen dari perangkat.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomVillage(village)
                        villagePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_village_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { villagePendingDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun DashboardHeroBanner(
    selectedYear: Int,
    availableYears: List<Int>,
    totalPhotos: Int,
    activeVillages: Int,
    totalVillages: Int,
    onSelectYear: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_village_banner),
                contentDescription = "Ilustrasi pedesaan Galeri Desa",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC09291F),
                                Color(0xAA0C3629),
                                Color(0xEA08221A)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Galeri Desa",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Arsip Foto Januari – Desember",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD9EDE4)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x33FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0x55FFFFFF)
                        )
                    ) {
                        Text(
                            text = "$totalPhotos Foto",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$activeVillages dari $totalVillages folder desa terisi di tahun $selectedYear",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE9F4EE)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableYears.forEach { yr ->
                            val selected = yr == selectedYear
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectYear(yr) },
                                label = {
                                    Text(
                                        text = yr.toString(),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0x28FFFFFF),
                                    labelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = Color(0x55FFFFFF),
                                    selectedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.testTag("dashboard_year_chip_$yr")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickInputActionBar(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onGalleryClick,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .testTag("dashboard_quick_gallery_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Input dari Galeri",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(
            onClick = onCameraClick,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .testTag("dashboard_quick_camera_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Kamera",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VillageFolderButtonCard(
    item: VillageDashboardItem,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    val accent = VillageAccentColors[item.village.colorIndex % VillageAccentColors.size]
    val progress = item.filledMonthsCount / 12f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("village_button_${item.village.folderSlug}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Folder-tab visual top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.latestPhotoPath != null) {
                        AsyncImage(
                            model = File(item.latestPhotoPath),
                            contentDescription = "Foto terbaru ${item.village.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = "Folder ${item.village.name}",
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${item.totalPhotosInYear} foto",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (onDeleteClick != null) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.testTag("delete_village_${item.village.folderSlug}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Hapus folder ${item.village.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.village.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Jan – Des",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.filledMonthsCount}/12 bln",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.filledMonthsCount > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = accent,
                        trackColor = accent.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentPhotosStrip(
    photos: List<PhotoEntryEntity>,
    villages: List<VillageEntity>,
    onPhotoClick: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = photos, key = { it.id }) { photo ->
            val villageName = villages.firstOrNull { it.id == photo.villageId }?.name ?: ""
            val monthName = IndonesianMonth.fromNumber(photo.month).shortName
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(132.dp)
                    .clickable { onPhotoClick(photo.id) }
                    .testTag("recent_photo_card_${photo.id}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = photo.caption.ifEmpty { "Foto $villageName $monthName" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xBF000000))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = villageName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$monthName ${photo.year}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0ECE6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddVillageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Folder Desa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Folder baru akan otomatis memiliki subfolder bulan Januari hingga Desember.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Desa / Dusun") },
                    placeholder = { Text("Misal: Dusun Wetan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_village_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_add_village_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buat Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
