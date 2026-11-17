package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.VillageEntity
import com.example.data.model.IndonesianMonth
import com.example.ui.viewmodel.PendingPhotoImport

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportPhotoSheet(
    pendingImport: PendingPhotoImport,
    villages: List<VillageEntity>,
    availableYears: List<Int>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirmSave: (villageId: Long, year: Int, month: Int, caption: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedVillageId by remember(pendingImport) {
        mutableLongStateOf(
            villages.firstOrNull { it.id == pendingImport.initialVillageId }?.id
                ?: villages.firstOrNull()?.id
                ?: pendingImport.initialVillageId
        )
    }
    var selectedYear by remember(pendingImport) { mutableIntStateOf(pendingImport.initialYear) }
    var selectedMonth by remember(pendingImport) { mutableIntStateOf(pendingImport.initialMonth) }
    var caption by remember(pendingImport) { mutableStateOf("") }

    val previewUris = remember(pendingImport) {
        if (pendingImport.cameraPreviewUri != null) {
            listOf(pendingImport.cameraPreviewUri)
        } else {
            pendingImport.uris
        }
    }

    val selectedVillage = villages.firstOrNull { it.id == selectedVillageId }
    val monthObj = IndonesianMonth.fromNumber(selectedMonth)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Simpan ${previewUris.size} Foto",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih folder desa dan bulan tujuan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier.testTag("close_import_sheet_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                }
            }

            // Thumbnail preview strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                previewUris.forEachIndexed { idx, uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Pratinjau foto ${idx + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(14.dp)
                            )
                    )
                }
            }

            // Target folder preview chip
            if (selectedVillage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Folder: ${selectedVillage.name} / $selectedYear / ${monthObj.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Village selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih Folder Desa",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    villages.forEach { village ->
                        val selected = village.id == selectedVillageId
                        FilterChip(
                            selected = selected,
                            onClick = { selectedVillageId = village.id },
                            label = { Text(village.name) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("import_village_chip_${village.folderSlug}")
                        )
                    }
                }
            }

            // Year selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tahun",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableYears.forEach { yr ->
                        FilterChip(
                            selected = yr == selectedYear,
                            onClick = { selectedYear = yr },
                            label = { Text(yr.toString()) },
                            modifier = Modifier.testTag("import_year_chip_$yr")
                        )
                    }
                }
            }

            // Month selection (Januari - Desember)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bulan (Januari – Desember)",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IndonesianMonth.entries.forEach { m ->
                        val selected = m.number == selectedMonth
                        FilterChip(
                            selected = selected,
                            onClick = { selectedMonth = m.number },
                            label = { Text(m.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("import_month_chip_${m.number}")
                        )
                    }
                }
            }

            // Caption input
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Keterangan / Nama Kegiatan (Opsional)") },
                placeholder = { Text("Misal: Kerja bakti, Posyandu, Perbaikan jalan…") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_caption_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_import_button")
                ) {
                    Text("Batal")
                }
                Button(
                    onClick = {
                        onConfirmSave(selectedVillageId, selectedYear, selectedMonth, caption)
                    },
                    enabled = !isSaving && villages.isNotEmpty(),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("confirm_save_photo_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan ke Folder")
                    }
                }
            }
        }
    }
}
