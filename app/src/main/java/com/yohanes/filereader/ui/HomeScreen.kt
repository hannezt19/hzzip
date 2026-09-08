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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.data.FileEntity

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFileClick: (FileEntity) -> Unit,
    onPickFileManually: () -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var showAnalisis by remember { mutableStateOf(false) }

    BackHandler(enabled = showAnalisis) {
        showAnalisis = false
    }
    BackHandler(enabled = !showAnalisis && selectedCategory != null) {
        viewModel.onCategorySelected(null)
    }

    val showDirektori by viewModel.showDirektori.collectAsState()

    when {
        showDirektori -> DirektoriScreen(
            onFileClick = onFileClick,
            onBack = { viewModel.closeDirektori() }
        )
        showAnalisis -> AnalisisScreen(onBack = { showAnalisis = false })
        selectedCategory != null -> CategoryDetailScreen(
            viewModel = viewModel,
            category = selectedCategory!!,
            onBack = { viewModel.onCategorySelected(null) },
            onFileClick = onFileClick
        )
        else -> CategoryHomeScreen(
            viewModel = viewModel,
            onCategoryClick = { viewModel.onCategorySelected(it) },
            onFileClick = onFileClick,
            onPickFileManually = onPickFileManually,
            onAnalisisClick = { showAnalisis = true },
            onDirektoriClick = { viewModel.openDirektori() }
        )
    }
}

@Composable
private fun CategoryHomeScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit,
    onFileClick: (FileEntity) -> Unit,
    onPickFileManually: () -> Unit,
    onAnalisisClick: () -> Unit,
    onDirektoriClick: () -> Unit
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
                    Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StorageCard(viewModel.storageInfo, modifier = Modifier.weight(1f), onClick = onDirektoriClick)
                        AnalisisCard(modifier = Modifier.weight(1f), onClick = onAnalisisClick)
                    }
                    CATEGORY_LIST.chunked(3).forEach { rowItems ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowItems.forEach { cat ->
                                CategoryCircleCard(cat, counts[cat] ?: 0) { onCategoryClick(cat) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCard(info: StorageInfo, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val usedFraction = if (info.totalBytes > 0) info.usedBytes.toFloat() / info.totalBytes.toFloat() else 0f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83D\uDCC1", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Direktori", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatSize(info.usedBytes)} terpakai dari ${formatSize(info.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = usedFraction,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text("${formatSize(info.freeBytes)} tersisa", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun categoryContainerColor(name: String): androidx.compose.ui.graphics.Color {
    return when (name) {
        "PDF" -> MaterialTheme.colorScheme.errorContainer
        "Gambar" -> MaterialTheme.colorScheme.tertiaryContainer
        "Excel" -> MaterialTheme.colorScheme.primaryContainer
        "Favorit" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
}

@Composable
private fun categoryContentColor(name: String): androidx.compose.ui.graphics.Color {
    return when (name) {
        "PDF" -> MaterialTheme.colorScheme.onErrorContainer
        "Gambar" -> MaterialTheme.colorScheme.onTertiaryContainer
        "Excel" -> MaterialTheme.colorScheme.onPrimaryContainer
        "Favorit" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
}

@Composable
private fun DirektoriScreen(onFileClick: (FileEntity) -> Unit, onBack: () -> Unit) {
    val rootPath = android.os.Environment.getExternalStorageDirectory().path
    var currentDir by remember { mutableStateOf(java.io.File(rootPath)) }

    BackHandler(enabled = true) {
        val parent = currentDir.parentFile
        if (currentDir.path != rootPath && parent != null) {
            currentDir = parent
        } else {
            onBack()
        }
    }

    val entries = remember(currentDir) {
        (currentDir.listFiles()?.toList() ?: emptyList())
            .sortedWith(compareByDescending<java.io.File> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val parent = currentDir.parentFile
                if (currentDir.path != rootPath && parent != null) currentDir = parent else onBack()
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Direktori", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        val relative = currentDir.path.removePrefix(rootPath).trim('/')
        val segments = if (relative.isBlank()) listOf("Internal") else listOf("Internal") + relative.split("/")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, seg ->
                Text(
                    seg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val target = if (index == 0) rootPath
                            else rootPath + "/" + segments.drop(1).take(index).joinToString("/")
                        currentDir = java.io.File(target)
                    }
                )
                if (index != segments.lastIndex) {
                    Text(" > ", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Folder kosong")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    if (entry.isDirectory) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { currentDir = entry }
                                .padding(16.dp, 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("\uD83D\uDCC1", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${entry.listFiles()?.size ?: 0} item",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        FileRow(
                            file = FileEntity(
                                path = entry.absolutePath,
                                name = entry.name,
                                extension = entry.extension.lowercase(),
                                sizeBytes = entry.length(),
                                lastModified = entry.lastModified()
                            ),
                            onClick = {
                                onFileClick(
                                    FileEntity(
                                        path = entry.absolutePath,
                                        name = entry.name,
                                        extension = entry.extension.lowercase(),
                                        sizeBytes = entry.length(),
                                        lastModified = entry.lastModified()
                                    )
                                )
                            }
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

private val ANALISIS_MENU = listOf(
    "Semua Partisi",
    "File Besar",
    "Berkas Terbaru",
    "Folder Kosong",
    "File Redundan",
    "File Duplikat",
    "Keranjang Sampah"
)

@Composable
private fun AnalisisCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Analisis",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("Analisis", style = MaterialTheme.typography.titleMedium)
            Text(
                "File lain untuk dibersihkan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalisisScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Analisis", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(ANALISIS_MENU) { menu ->
                Row(
                    Modifier.fillMaxWidth().padding(16.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(menu, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
                Divider()
            }
        }
    }
}

@Composable
private fun categorySolidColor(name: String): androidx.compose.ui.graphics.Color {
    return when (name) {
        "PDF" -> androidx.compose.ui.graphics.Color(0xFFE53935)
        "Excel" -> androidx.compose.ui.graphics.Color(0xFF43A047)
        "Gambar" -> androidx.compose.ui.graphics.Color(0xFF5C6BC0)
        "Video" -> androidx.compose.ui.graphics.Color(0xFFFB8C00)
        "Audio" -> androidx.compose.ui.graphics.Color(0xFFD81B60)
        "Favorit" -> androidx.compose.ui.graphics.Color(0xFFFFB300)
        else -> androidx.compose.ui.graphics.Color(0xFF616161)
    }
}

private fun categoryIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "PDF" -> Icons.Filled.PictureAsPdf
        "Excel" -> Icons.Filled.TableChart
        "Gambar" -> Icons.Filled.ImageIcon
        "Video" -> Icons.Filled.Movie
        "Audio" -> Icons.Filled.MusicNote
        "Favorit" -> Icons.Filled.Star
        else -> Icons.Filled.Description
    }
}

@Composable
private fun CategoryCircleCard(name: String, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(categorySolidColor(name)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIcon(name),
                contentDescription = name,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(
            "$count file",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryCard(name: String, count: Int, onClick: () -> Unit) {
    val contentColor = categoryContentColor(name)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = categoryContainerColor(name),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(categoryEmoji(name), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = contentColor)
                Text("$count file", style = MaterialTheme.typography.bodySmall, color = contentColor)
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
    val files by viewModel.files.collectAsState()

    Column(Modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada file di kategori ini")
            }
        } else if (category == "Gambar") {
            val images by viewModel.images.collectAsState()
            val imageMode by viewModel.imageGalleryMode.collectAsState()
            val selectedImageFolder by viewModel.selectedImageFolderPath.collectAsState()
            ImageGalleryScreen(
                imagesFlow = viewModel.imagesPaged,
                images = images,
                mode = imageMode,
                onModeChange = { viewModel.setImageGalleryMode(it) },
                selectedFolderPath = selectedImageFolder,
                onFolderSelected = { viewModel.selectImageFolder(it) },
                onFileClick = onFileClick
            )
        } else if (category == "Video") {
            val videos by viewModel.videos.collectAsState()
            val videoMode by viewModel.videoGalleryMode.collectAsState()
            val selectedVideoFolder by viewModel.selectedVideoFolderPath.collectAsState()
            VideoGalleryScreen(
                videos = videos,
                mode = videoMode,
                onModeChange = { viewModel.setVideoGalleryMode(it) },
                selectedFolderPath = selectedVideoFolder,
                onFolderSelected = { viewModel.selectVideoFolder(it) },
                onFileClick = onFileClick
            )
        } else {
            val categoryModes by viewModel.categoryGalleryMode.collectAsState()
            val mode = categoryModes[category] ?: VideoGalleryMode.TERBARU
            FileListWithModeToggle(
                files = files,
                mode = mode,
                onModeChange = { viewModel.setCategoryGalleryMode(category, it) },
                onFileClick = onFileClick
            )
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
