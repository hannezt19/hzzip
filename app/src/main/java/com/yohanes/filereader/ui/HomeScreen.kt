package com.yohanes.filereader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yohanes.filereader.data.FileEntity

@Composable
fun HomeScreen(
    onFileClick: (FileEntity) -> Unit,
    onPickFileManually: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    BackHandler(enabled = selectedCategory != null) {
        viewModel.onCategorySelected(null)
    }

    if (selectedCategory != null) {
        CategoryDetailScreen(
            viewModel = viewModel,
            category = selectedCategory!!,
            onBack = { viewModel.onCategorySelected(null) },
            onFileClick = onFileClick
        )
    } else {
        CategoryHomeScreen(
            viewModel = viewModel,
            onCategoryClick = { viewModel.onCategorySelected(it) },
            onFileClick = onFileClick,
            onPickFileManually = onPickFileManually
        )
    }
}

@Composable
private fun CategoryHomeScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit,
    onFileClick: (FileEntity) -> Unit,
    onPickFileManually: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val counts by viewModel.categoryCounts.collectAsState()
    val searchResults by viewModel.files.collectAsState()

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
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.refreshScan() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        if (isScanning) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        when {
            query.isNotBlank() -> {
                if (searchResults.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada file ditemukan")
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(searchResults) { file ->
                            FileRow(file = file, onClick = { onFileClick(file) })
                            Divider()
                        }
                    }
                }
            }
            counts.values.sum() == 0 && !isScanning -> {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Tidak ada file ditemukan", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Coba refresh, atau buka file lewat File Manager (\"Buka dengan\" -> File Reader).",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onPickFileManually) { Text("Pilih File Manual") }
                }
            }
            else -> {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategoryCard(CATEGORY_LIST[0], counts[CATEGORY_LIST[0]] ?: 0, Modifier.weight(1f)) { onCategoryClick(CATEGORY_LIST[0]) }
                        CategoryCard(CATEGORY_LIST[1], counts[CATEGORY_LIST[1]] ?: 0, Modifier.weight(1f)) { onCategoryClick(CATEGORY_LIST[1]) }
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategoryCard(CATEGORY_LIST[2], counts[CATEGORY_LIST[2]] ?: 0, Modifier.weight(1f)) { onCategoryClick(CATEGORY_LIST[2]) }
                        CategoryCard(CATEGORY_LIST[3], counts[CATEGORY_LIST[3]] ?: 0, Modifier.weight(1f)) { onCategoryClick(CATEGORY_LIST[3]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(name: String, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(categoryEmoji(name), style = MaterialTheme.typography.headlineLarge)
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$count file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailScreen(
    viewModel: HomeViewModel,
    category: String,
    onBack: () -> Unit,
    onFileClick: (FileEntity) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val files by viewModel.files.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text(category, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        Row(
            Modifier.fillMaxWidth().padding(16.dp, 4.dp, 16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cari di $category...") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Text("Urutkan")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text("A-Z") }, onClick = {
                        viewModel.onSortOptionChange(SortOption.NAME_AZ)
                        showSortMenu = false
                    })
                    DropdownMenuItem(text = { Text("Terbaru - Terlama") }, onClick = {
                        viewModel.onSortOptionChange(SortOption.DATE_NEWEST)
                        showSortMenu = false
                    })
                    DropdownMenuItem(text = { Text("Terbesar - Terkecil") }, onClick = {
                        viewModel.onSortOptionChange(SortOption.SIZE_LARGEST)
                        showSortMenu = false
                    })
                }
            }
        }

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada file di kategori ini")
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
                "${file.extension.uppercase()} - ${formatSize(file.sizeBytes)}",
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
