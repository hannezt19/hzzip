package com.yohanes.filereader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Precision
import com.yohanes.filereader.data.FileEntity
import java.io.File

private data class FolderGroup(val path: String, val videos: List<FileEntity>) {
    val label: String get() = path.substringAfterLast('/')
}

@Composable
fun VideoGalleryScreen(
    videos: List<FileEntity>,
    mode: VideoGalleryMode,
    onModeChange: (VideoGalleryMode) -> Unit,
    selectedFolderPath: String?,
    onFolderSelected: (String?) -> Unit,
    onFileClick: (FileEntity) -> Unit,
    onFileLongClick: (FileEntity) -> Unit
) {
    val folderGroups = remember(videos) {
        videos
            .groupBy { File(it.path).parent ?: "/" }
            .map { (path, list) -> FolderGroup(path, list.sortedByDescending { it.lastModified }) }
            .sortedBy { it.label.lowercase() }
    }

    val selectedFolder = remember(folderGroups, selectedFolderPath) {
        folderGroups.find { it.path == selectedFolderPath }
    }

    BackHandler(enabled = selectedFolderPath != null) {
        onFolderSelected(null)
    }

    if (mode == VideoGalleryMode.FOLDER && selectedFolder != null) {
        val folder = selectedFolder
        Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(4.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onFolderSelected(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                    Text(folder.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(folder.videos, key = { it.path }) { file ->
                        VideoThumbnail(file = file, onClick = { onFileClick(file) }, onLongClick = { onFileLongClick(file) })
                    }
                }
            }
        }
        return
    }

    val flatVideos = remember(videos) { videos.sortedByDescending { it.lastModified } }

    Box(Modifier.fillMaxSize()) {
        if (mode == VideoGalleryMode.TERBARU) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 64.dp)
            ) {
                items(flatVideos, key = { it.path }) { file ->
                    VideoThumbnail(file = file, onClick = { onFileClick(file) }, onLongClick = { onFileLongClick(file) })
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 64.dp)
            ) {
                items(folderGroups, key = { it.path }) { folder ->
                    FolderThumbnail(folder = folder, onClick = { onFolderSelected(folder.path) })
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            ModePill(
                label = "Terbaru",
                selected = mode == VideoGalleryMode.TERBARU,
                onClick = { onModeChange(VideoGalleryMode.TERBARU) }
            )
            ModePill(
                label = "Folder",
                selected = mode == VideoGalleryMode.FOLDER,
                onClick = { onModeChange(VideoGalleryMode.FOLDER) }
            )
        }
    }
}

@Composable
private fun ModePill(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun FolderThumbnail(folder: FolderGroup, onClick: () -> Unit) {
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
            val cover = folder.videos.firstOrNull()
            if (cover != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(cover.path))
                        .decoderFactory(VideoFrameDecoder.Factory())
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
                    "${folder.videos.size}",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoThumbnail(file: FileEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(file.path))
                .decoderFactory(VideoFrameDecoder.Factory())
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
            },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                }
            }
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
