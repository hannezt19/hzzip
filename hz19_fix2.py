# hz19. Fase 2 galeri gambar: swipe geser antar foto (HorizontalPager)

# 1) File baru: ImagePagerScreen.kt
path1 = 'app/src/main/java/com/yohanes/filereader/ui/ImagePagerScreen.kt'
content1 = '''package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.FileEntity
import java.io.File

@Composable
fun ImagePagerScreen(
    files: List<FileEntity>,
    initialIndex: Int,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, (files.size - 1).coerceAtLeast(0))) {
        files.size
    }
    var zoomedIn by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        zoomedIn = false
    }

    val currentFile = files.getOrNull(pagerState.currentPage)
    val favKey = currentFile?.path
    val favorites by FavoritesStore.favorites.collectAsState()
    val isFav = favKey != null && favorites.contains(favKey)

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomedIn,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val file = files[page]
            ZoomableImage(
                uri = Uri.fromFile(File(file.path)),
                displayName = file.name,
                onZoomChanged = { zoom ->
                    if (page == pagerState.currentPage) zoomedIn = zoom > 1f
                }
            )
        }

        IconButton(
            onClick = { favKey?.let { FavoritesStore.toggle(it) } },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favorit",
                tint = if (isFav) Color(0xFFFFC107) else Color.White
            )
        }
    }
}

@Composable
private fun ZoomableImage(uri: Uri, displayName: String, onZoomChanged: (Float) -> Unit) {
    val context = LocalContext.current
    var bmp by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadFailed by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        bmp = loadBitmapForPager(context, uri)
        if (bmp == null) loadFailed = true
    }

    var zoom by remember(uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(uri) { mutableFloatStateOf(0f) }

    LaunchedEffect(zoom) { onZoomChanged(zoom) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            loadFailed -> Text("Gagal memuat gambar: $displayName")
            bmp == null -> CircularProgressIndicator()
            else -> {
                val safeBmp = bmp!!
                Image(
                    bitmap = safeBmp.asImageBitmap(),
                    contentDescription = displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(uri) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                val newZoom = (zoom * zoomChange).coerceIn(1f, 5f)
                                zoom = newZoom
                                if (newZoom > 1f) {
                                    val maxOffset = (newZoom - 1f) * 800f
                                    offsetX = (offsetX + panChange.x).coerceIn(-maxOffset, maxOffset)
                                    offsetY = (offsetY + panChange.y).coerceIn(-maxOffset, maxOffset)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .pointerInput(uri) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoom > 1f) {
                                        zoom = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        zoom = 2.5f
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

private fun loadBitmapForPager(context: Context, uri: Uri): android.graphics.Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}
'''
import os
assert not os.path.exists(path1), "hz19: ImagePagerScreen.kt sudah ada, cek manual"
open(path1, 'w').write(content1)
print("OK: ImagePagerScreen.kt (baru)")

# 2) Timpa ImageGalleryScreen.kt dengan versi yang sudah terhubung ke pager
path2 = 'app/src/main/java/com/yohanes/filereader/ui/ImageGalleryScreen.kt'
content2 = '''package com.yohanes.filereader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yohanes.filereader.data.FileEntity
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ImageGalleryScreen(
    files: List<FileEntity>,
    onFileClick: (FileEntity) -> Unit
) {
    val grouped = remember(files) { groupByDate(files) }
    val flatFiles = remember(grouped) { grouped.flatMap { it.second } }
    var pagerIndex by remember { mutableStateOf<Int?>(null) }

    if (pagerIndex != null) {
        ImagePagerScreen(
            files = flatFiles,
            initialIndex = pagerIndex!!,
            onExit = { pagerIndex = null }
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
    ) {
        grouped.forEach { (label, filesInGroup) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            items(filesInGroup, key = { it.path }) { file ->
                ImageThumbnail(
                    file = file,
                    onClick = { pagerIndex = flatFiles.indexOf(file) }
                )
            }
        }
    }
}

@Composable
private fun ImageThumbnail(file: FileEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(file.path),
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun groupByDate(files: List<FileEntity>): List<Pair<String, List<FileEntity>>> {
    val today = LocalDate.now()
    val result = LinkedHashMap<String, MutableList<FileEntity>>()

    for (file in files) {
        val date = Instant.ofEpochMilli(file.lastModified).atZone(ZoneId.systemDefault()).toLocalDate()
        val label = when {
            date.isEqual(today) -> "Hari ini"
            date.isEqual(today.minusDays(1)) -> "Kemarin"
            else -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID")))
                .replaceFirstChar { it.uppercase() }
        }
        result.getOrPut(label) { mutableListOf() }.add(file)
    }
    return result.map { it.key to it.value }
}
'''
assert os.path.exists(path2), "hz19: ImageGalleryScreen.kt tidak ditemukan, jalankan hz19_fix.py (Fase 1) dulu"
open(path2, 'w').write(content2)
print("OK: ImageGalleryScreen.kt (ditimpa, terhubung ke pager)")

print("\\nSemua perubahan Fase 2 (swipe) berhasil diterapkan.")
