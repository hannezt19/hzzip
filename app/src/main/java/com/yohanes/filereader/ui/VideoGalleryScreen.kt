package com.yohanes.filereader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Precision
import com.yohanes.filereader.data.FileEntity
import java.io.File

private enum class VideoGalleryMode { TERBARU, FOLDER }

@Composable
fun VideoGalleryScreen(videos: List<FileEntity>, onFileClick: (FileEntity) -> Unit) {
    var mode by remember { mutableStateOf(VideoGalleryMode.TERBARU) }

    val gridItems: List<Any> = remember(videos, mode) {
        if (mode == VideoGalleryMode.TERBARU) {
            videos.sortedByDescending { it.lastModified }
        } else {
            videos
                .groupBy { File(it.path).parent ?: "/" }
                .toSortedMap()
                .flatMap { (folder, list) ->
                    listOf(folder as Any) + list.sortedByDescending { it.lastModified }
                }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 64.dp)
        ) {
            items(
                count = gridItems.size,
                key = { index ->
                    when (val item = gridItems[index]) {
                        is String -> "header_$item"
                        is FileEntity -> item.path
                        else -> index
                    }
                },
                span = { index ->
                    if (gridItems[index] is String) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                }
            ) { index ->
                when (val item = gridItems[index]) {
                    is String -> {
                        Text(
                            text = item.substringAfterLast('/'),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                    is FileEntity -> {
                        VideoThumbnail(file = item, onClick = { onFileClick(item) })
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            ModePill(
                label = "Terbaru",
                selected = mode == VideoGalleryMode.TERBARU,
                onClick = { mode = VideoGalleryMode.TERBARU }
            )
            ModePill(
                label = "Folder",
                selected = mode == VideoGalleryMode.FOLDER,
                onClick = { mode = VideoGalleryMode.FOLDER }
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
private fun VideoThumbnail(file: FileEntity, onClick: () -> Unit) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
