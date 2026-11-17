package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.IndonesianMonth
import com.example.ui.theme.VillageAccentColors
import com.example.ui.viewmodel.MonthFolderSummary
import com.example.ui.viewmodel.VillageFolderUiState
import java.io.File

private enum class MonthFilter(val label: String) {
    ALL("Semua (12)"),
    FILLED("Terisi"),
    EMPTY("Belum Terisi")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageFolderScreen(
    uiState: VillageFolderUiState,
    availableYears: List<Int>,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSelectYear: (Int) -> Unit,
    onMonthClick: (villageId: Long, year: Int, month: IndonesianMonth) -> Unit,
    onQuickAddGalleryForMonth: (villageId: Long, year: Int, month: IndonesianMonth) -> Unit,
    onQuickAddCameraForMonth: (villageId: Long, year: Int, month: IndonesianMonth) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(MonthFilter.ALL) }
    var monthForSourcePicker by remember { mutableStateOf<IndonesianMonth?>(null) }

    val villageName = (uiState as? VillageFolderUiState.Success)?.village?.name ?: "Folder Desa"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = villageName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState is VillageFolderUiState.Success) {
                            Text(
                                text = "Folder: GaleriDesa/${uiState.village.folderSlug}/${uiState.selectedYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("village_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali ke Dashboard"
                        )
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
                is VillageFolderUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is VillageFolderUiState.Error -> {
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

                is VillageFolderUiState.Success -> {
                    val accent = VillageAccentColors[uiState.village.colorIndex % VillageAccentColors.size]
                    val displayedMonths = when (selectedFilter) {
                        MonthFilter.ALL -> uiState.monthSummaries
                        MonthFilter.FILLED -> uiState.monthSummaries.filter { it.photoCount > 0 }
                        MonthFilter.EMPTY -> uiState.monthSummaries.filter { it.photoCount == 0 }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        modifier = Modifier
                            .widthIn(max = 920.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 28.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Year & Summary Header Card
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = accent.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    accent.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(accent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Arsip 12 Bulan (${uiState.selectedYear})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${uiState.filledMonths}/12 bulan terisi • ${uiState.totalPhotos} foto",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Year Switcher
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        availableYears.forEach { yr ->
                                            FilterChip(
                                                selected = yr == uiState.selectedYear,
                                                onClick = { onSelectYear(yr) },
                                                label = { Text(yr.toString()) },
                                                modifier = Modifier.testTag("village_year_chip_$yr")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Filter Chips
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MonthFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter.label) },
                                        modifier = Modifier.testTag("month_filter_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Month Folder Cards (Januari - Desember)
                        items(
                            items = displayedMonths,
                            key = { it.month.number }
                        ) { summary ->
                            MonthFolderCard(
                                summary = summary,
                                accentColor = accent,
                                onOpenMonth = {
                                    onMonthClick(uiState.village.id, uiState.selectedYear, summary.month)
                                },
                                onQuickAdd = {
                                    monthForSourcePicker = summary.month
                                }
                            )
                        }
                    }

                    monthForSourcePicker?.let { pickedMonth ->
                        ModalBottomSheet(
                            onDismissRequest = { monthForSourcePicker = null }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Input Foto • ${pickedMonth.displayName} ${uiState.selectedYear}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Folder tujuan: ${uiState.village.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            monthForSourcePicker = null
                                            onQuickAddGalleryForMonth(
                                                uiState.village.id,
                                                uiState.selectedYear,
                                                pickedMonth
                                            )
                                        }
                                        .testTag("sheet_pick_gallery")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Column {
                                            Text(
                                                text = "Pilih dari Galeri",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Pilih satu atau beberapa foto sekaligus",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            monthForSourcePicker = null
                                            onQuickAddCameraForMonth(
                                                uiState.village.id,
                                                uiState.selectedYear,
                                                pickedMonth
                                            )
                                        }
                                        .testTag("sheet_take_camera")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Column {
                                            Text(
                                                text = "Ambil Foto dengan Kamera",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "Potret langsung dan simpan ke folder ${pickedMonth.displayName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthFolderCard(
    summary: MonthFolderSummary,
    accentColor: Color,
    onOpenMonth: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val hasPhotos = summary.photoCount > 0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasPhotos) 2.dp else 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (hasPhotos) accentColor.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenMonth)
            .testTag("month_card_${summary.month.number}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(
                        if (hasPhotos) MaterialTheme.colorScheme.surfaceVariant
                        else accentColor.copy(alpha = 0.08f)
                    )
            ) {
                if (summary.coverPhotoPath != null) {
                    AsyncImage(
                        model = File(summary.coverPhotoPath),
                        contentDescription = "Sampul bulan ${summary.month.displayName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x55000000), Color.Transparent, Color(0x99000000))
                                )
                            )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                    )
                }

                // Month number badge top-left
                Surface(
                    shape = CircleShape,
                    color = if (hasPhotos) Color(0xCC0D221B) else accentColor.copy(alpha = 0.16f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = summary.month.formattedNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPhotos) Color.White else accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Quick add button top-right
                FilledIconButton(
                    onClick = onQuickAdd,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasPhotos) Color(0xCCFFFFFF) else accentColor,
                        contentColor = if (hasPhotos) accentColor else Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(40.dp)
                        .testTag("quick_add_month_${summary.month.number}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Tambah foto bulan ${summary.month.displayName}",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.month.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasPhotos) "${summary.photoCount} foto tersimpan" else "Belum ada foto",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasPhotos) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
