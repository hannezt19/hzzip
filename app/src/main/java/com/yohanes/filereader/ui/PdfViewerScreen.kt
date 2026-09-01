package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.OcrStore
import com.yohanes.filereader.data.OcrStatus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

private const val RENDER_SCALE = 2f

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PdfViewerScreen(uri: Uri, displayName: String) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("pdf_progress", Context.MODE_PRIVATE) }

    val pagerState = rememberPagerState(
        initialPage = prefs.getInt(displayName, 0),
        pageCount = { pageCount }
    )

    DisposableEffect(uri) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val renderer = pfd?.let { PdfRenderer(it) }
        pageCount = renderer?.pageCount ?: 0
        onDispose {
            renderer?.close()
            pfd?.close()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        prefs.edit().putInt(displayName, pagerState.currentPage).apply()
    }

    val favKey = uri.path ?: uri.toString()
    val favorites by FavoritesStore.favorites.collectAsState()
    val isFav = favorites.contains(favKey)

    var showTextMode by remember { mutableStateOf(false) }
    var hasOcrCache by remember(displayName) { mutableStateOf(OcrStore.hasCache(context, displayName)) }
    val ocrStatus by OcrStore.status.collectAsState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                displayName,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 4.dp)
            )
            IconButton(onClick = { FavoritesStore.toggle(favKey) }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorit",
                    tint = if (isFav) androidx.compose.ui.graphics.Color(0xFFFFC107) else androidx.compose.ui.graphics.Color.Gray
                )
            }
            if (hasOcrCache) {
                TextButton(onClick = { showTextMode = !showTextMode }) {
                    Text(if (showTextMode) "Lihat PDF" else "Lihat Teks")
                }
            } else {
                TextButton(onClick = {
                    scope.launch {
                        OcrStore.process(context, uri, displayName)
                        hasOcrCache = OcrStore.hasCache(context, displayName)
                    }
                }) {
                    Text("Proses OCR")
                }
            }
        }

        if (ocrStatus is OcrStatus.Processing) {
            val st = ocrStatus as OcrStatus.Processing
            LinearProgressIndicator(
                progress = { st.currentPage.toFloat() / st.totalPages.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Text(
                "Memproses OCR halaman ${st.currentPage}/${st.totalPages}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (showTextMode) {
            Text(
                OcrStore.readCache(context, displayName),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (pageCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    ZoomablePdfPage(uri = uri, pageIndex = pageIndex)
                }

                Text(
                    "${pagerState.currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}

@Composable
private fun ZoomablePdfPage(uri: Uri, pageIndex: Int) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pageIndex) {
        bitmap = renderSinglePage(context, uri, pageIndex, RENDER_SCALE)
    }

    val bmp = bitmap
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val isPinch = event.changes.size >= 2
                        if (isPinch || zoom > 1f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
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
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Halaman ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

private fun renderSinglePage(context: Context, uri: Uri, pageIndex: Int, scale: Float): Bitmap? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        renderer = pfd?.let { PdfRenderer(it) }
        val page = renderer?.openPage(pageIndex) ?: return null
        val width = (page.width * scale).toInt().coerceAtLeast(1)
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bitmap
    } catch (e: Exception) {
        null
    } finally {
        renderer?.close()
        pfd?.close()
    }
}
