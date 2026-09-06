package com.yohanes.filereader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
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

@Composable
fun ImageGalleryScreen(
    imagesFlow: Flow<PagingData<GalleryItem>>,
    onFileClick: (FileEntity) -> Unit
) {
    val pagingItems = imagesFlow.collectAsLazyPagingItems()
    var pagerIndex by remember { mutableStateOf<Int?>(null) }

    if (pagerIndex != null) {
        val photoList = pagingItems.itemSnapshotList.items
            .filterIsInstance<GalleryItem.Photo>()
            .map { it.file }
        ImagePagerScreen(
            files = photoList,
            initialIndex = pagerIndex!!,
            onExit = { pagerIndex = null }
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
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
                            if (clickedIndex >= 0) pagerIndex = clickedIndex
                        }
                    )
                }
                null -> {}
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
