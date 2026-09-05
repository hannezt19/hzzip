package com.yohanes.filereader.ui

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
