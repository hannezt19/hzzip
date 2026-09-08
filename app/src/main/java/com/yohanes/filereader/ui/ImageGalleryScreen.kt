package com.yohanes.filereader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.yohanes.filereader.data.FileEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

private data class ImageFolderGroup(val path: String, val photos: List<FileEntity>) {
    val label: String get() = path.substringAfterLast('/')
}

@Composable
fun ImageGalleryScreen(
    imagesFlow: Flow<PagingData<GalleryItem>>,
    images: List<FileEntity>,
    mode: VideoGalleryMode,
    onModeChange: (VideoGalleryMode) -> Unit,
    selectedFolderPath: String?,
    onFolderSelected: (String?) -> Unit,
    onFileClick: (FileEntity) -> Unit
) {
    var pagerIndex by remember { mutableStateOf<Int?>(null) }
    var pagerPhotos by remember { mutableStateOf<List<FileEntity>>(emptyList()) }

    if (pagerIndex != null) {
        ImagePagerScreen(
            files = pagerPhotos,
            initialIndex = pagerIndex!!,
            onExit = { pagerIndex = null }
        )
        return
    }

    BackHandler(enabled = mode == VideoGalleryMode.FOLDER && selectedFolderPath != null) {
        onFolderSelected(null)
    }

    // Mode Folder - pengelompokan ringan (cuma daftar nama file, bukan buka semua gambar
    // sekaligus), ditampilkan 2 tingkat seperti Video: daftar folder -> isi folder.
    if (mode == VideoGalleryMode.FOLDER) {
        val folderGroups = remember(images) {
            images
                .groupBy { File(it.path).parent ?: "/" }
                .map { (path, list) -> ImageFolderGroup(path, list.sortedByDescending { it.lastModified }) }
                .sortedBy { it.label.lowercase() }
        }
        val selectedFolder = remember(folderGroups, selectedFolderPath) {
            folderGroups.find { it.path == selectedFolderPath }
        }

        if (selectedFolder != null) {
            Box(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(4.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onFolderSelected(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                        Text(selectedFolder.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(selectedFolder.photos, key = { it.path }) { file ->
                            ImageThumbnail(
                                file = file,
                                onClick = {
                                    val idx = selectedFolder.photos.indexOfFirst { it.path == file.path }
                                    if (idx >= 0) {
                                        pagerPhotos = selectedFolder.photos
                                        pagerIndex = idx
                                    }
                                }
                            )
                        }
                    }
                }
            }
            return
        }

        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 64.dp)
            ) {
                items(folderGroups, key = { it.path }) { folder ->
                    ImageFolderThumbnail(folder = folder, onClick = { onFolderSelected(folder.path) })
                }
            }
            ModeToggleRow(mode = mode, onModeChange = onModeChange)
        }
        return
    }

    // Mode Terbaru - tetap pakai sistem paging yang sudah ada (ringan untuk koleksi besar).
    val pagingItems = imagesFlow.collectAsLazyPagingItems()

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 64.dp)
        ) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { item ->
                    when (item) {
                        is GalleryItem.Header -> "header_${item.label}"
                        is GalleryItem.Photo -> item.file.path
                    }
                },
                span = { index ->
                    when (pagingItems.peek(index)) {
                        is GalleryItem.Header -> GridItemSpan(maxLineSpan)
                        else -> GridItemSpan(1)
                    }
                }
            ) { index ->
                when (val item = pagingItems[index]) {
                    is GalleryItem.Header -> {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    is GalleryItem.Photo -> {
                        ImageThumbnail(
                            file = item.file,
                            onClick = {
                                val photoList = pagingItems.itemSnapshotList.items
                                    .filterIsInstance<GalleryItem.Photo>()
                                    .map { it.file }
                                val clickedIndex = photoList.indexOfFirst { it.path == item.file.path }
                                if (clickedIndex >= 0) {
                                    pagerPhotos = photoList
                                    pagerIndex = clickedIndex
                                }
                            }
                        )
                    }
                    null -> {}
                }
            }
        }
        ModeToggleRow(mode = mode, onModeChange = onModeChange)
    }
}

@Composable
private fun ModeToggleRow(mode: VideoGalleryMode, onModeChange: (VideoGalleryMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            ImageModePill(
                label = "Terbaru",
                selected = mode == VideoGalleryMode.TERBARU,
                onClick = { onModeChange(VideoGalleryMode.TERBARU) }
            )
            ImageModePill(
                label = "Folder",
                selected = mode == VideoGalleryMode.FOLDER,
                onClick = { onModeChange(VideoGalleryMode.FOLDER) }
            )
        }
    }
}

@Composable
private fun ImageModePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ImageFolderThumbnail(folder: ImageFolderGroup, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(2.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val cover = folder.photos.firstOrNull()
            if (cover != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(cover.path))
                        .crossfade(true)
                        .precision(Precision.INEXACT)
                        .build(),
                    contentDescription = folder.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "${folder.photos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        Text(
            text = folder.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
        )
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
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(file.path))
                .crossfade(true)
                .precision(Precision.INEXACT)
                .build(),
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
        )
    }
}
