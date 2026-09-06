package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.FileEntity
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
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
            modifier = Modifier.fillMaxSize(),
            beyondBoundsPageCount = 1
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
        bmp = withContext(Dispatchers.IO) { loadBitmapForPager(context, uri) }
        if (bmp == null) loadFailed = true
    }

    var zoom by remember(uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(uri) { mutableFloatStateOf(0f) }
    var containerSize by remember(uri) { mutableStateOf(IntSize.Zero) }

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
                        .onSizeChanged { containerSize = it }
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(uri) {
                            // Hanya tangkap gesture kalau pinch 2 jari (mulai zoom) atau
                            // gambar sudah dalam kondisi zoom (untuk pan). Kalau cuma
                            // 1 jari geser & belum zoom, JANGAN konsumsi - biarkan
                            // HorizontalPager di atasnya yang proses swipe ganti foto.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val isMultiTouch = event.changes.size > 1
                                    if (isMultiTouch || zoom > 1f) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val newZoom = (zoom * zoomChange).coerceIn(1f, 5f)
                                        zoom = newZoom
                                        if (newZoom > 1f) {
                                            val maxOffsetX = (containerSize.width * (newZoom - 1f)) / 2f
                                            val maxOffsetY = (containerSize.height * (newZoom - 1f)) / 2f
                                            offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
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
        val metrics = context.resources.displayMetrics
        // Batasi maksimal ~2x ukuran layar - cukup tajam untuk zoom sampai 5x
        // tanpa decode resolusi asli yang bisa berukuran raksasa
        val maxDimension = maxOf(metrics.widthPixels, metrics.heightPixels) * 2

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        var inSampleSize = 1
        while (boundsOptions.outWidth / inSampleSize >= maxDimension ||
            boundsOptions.outHeight / inSampleSize >= maxDimension
        ) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: Exception) {
        null
    }
}
