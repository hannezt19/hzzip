package com.yohanes.filereader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yohanes.filereader.data.FileEntity
import java.io.File

private val CATEGORIES = listOf(
    null to "Semua",
    "pdf" to "PDF",
    "docx" to "Word",
    "xlsx" to "Excel",
    "txt" to "TXT",
    "json" to "JSON",
    "html" to "HTML",
    "js" to "JS",
    "css" to "CSS"
)

@Composable
fun HomeScreen(
    onFileClick: (FileEntity) -> Unit,
    onPickFileManually: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel()
    val query by viewModel.searchQuery.collectAsState()
    val selectedExt by viewModel.selectedExtension.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val files by viewModel.files.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cari file...") },
                singleLine = true
            )
            IconButton(onClick = { viewModel.refreshScan() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CATEGORIES) { (ext, label) ->
                FilterChip(
                    selected = selectedExt == ext,
                    onClick = { viewModel.onExtensionFilterChange(ext) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (isScanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (files.isEmpty() && !isScanning) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Tidak ada file ditemukan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Coba refresh, atau buka file lewat File Manager (\"Buka dengan\" → File Reader).",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPickFileManually) { Text("Pilih File Manual") }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(files) { file ->
                    FileRow(file = file, onClick = { onFileClick(file) })
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: FileEntity, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                "${file.extension.uppercase()} · ${formatSize(file.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}
