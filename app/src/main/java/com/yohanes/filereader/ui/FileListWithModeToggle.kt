package com.yohanes.filereader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.data.FileEntity
import java.io.File

private sealed class ListItem {
    data class Header(val label: String) : ListItem()
    data class Row(val file: FileEntity) : ListItem()
}

/**
 * Komponen list dengan toggle Terbaru/Folder, dipakai bareng untuk kategori
 * PDF, Excel, Teks/Kode, dan Favorit (Audio dikecualikan - beda konsep playlist).
 *
 * Mode Terbaru: list flat diurutkan lastModified descending.
 * Mode Folder: list dikelompokkan per folder dengan header nama folder,
 * TIDAK 2 tingkat seperti Video (cukup 1 layar dengan section header),
 * karena ini list biasa bukan grid thumbnail.
 */
@Composable
fun FileListWithModeToggle(
    files: List<FileEntity>,
    mode: VideoGalleryMode,
    onModeChange: (VideoGalleryMode) -> Unit,
    onFileClick: (FileEntity) -> Unit
) {
    val items = remember(files, mode) {
        if (mode == VideoGalleryMode.TERBARU) {
            files.sortedByDescending { it.lastModified }.map { ListItem.Row(it) }
        } else {
            files
                .groupBy { File(it.path).parent ?: "/" }
                .toSortedMap(compareBy { it.substringAfterLast('/').lowercase() })
                .flatMap { (folderPath, group) ->
                    val label = folderPath.substringAfterLast('/')
                    listOf(ListItem.Header(label)) +
                        group.sortedByDescending { it.lastModified }.map { ListItem.Row(it) }
                }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 72.dp)
        ) {
            items(items) { item ->
                when (item) {
                    is ListItem.Header -> {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    is ListItem.Row -> {
                        FileRowPublic(file = item.file, onClick = { onFileClick(item.file) })
                        Divider()
                    }
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
            ModeTogglePill(
                label = "Terbaru",
                selected = mode == VideoGalleryMode.TERBARU,
                onClick = { onModeChange(VideoGalleryMode.TERBARU) }
            )
            ModeTogglePill(
                label = "Folder",
                selected = mode == VideoGalleryMode.FOLDER,
                onClick = { onModeChange(VideoGalleryMode.FOLDER) }
            )
        }
    }
}

@Composable
private fun ModeTogglePill(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun FileRowPublic(file: FileEntity, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyLarge)
            androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
            Text(
                "${file.extension.uppercase()} - ${formatSizePublic(file.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSizePublic(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}
