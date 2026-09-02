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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.OcrStore
import com.yohanes.filereader.data.PdfTextExtractor
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

private const val RENDER_SCALE = 2f
private const val OCR_TEXT_SIZE_MIN = 12f
private const val OCR_TEXT_SIZE_MAX = 28f
private const val OCR_TEXT_SIZE_STEP = 2f

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PdfViewerScreen(uri: Uri, displayName: String) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("pdf_progress", Context.MODE_PRIVATE) }
    val ocrPrefs = remember { context.getSharedPreferences("ocr_settings", Context.MODE_PRIVATE) }

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

    var ocrModeActive by remember { mutableStateOf(false) }
    var sheetPageIndex by remember { mutableStateOf<Int?>(null) }
    var textSizeSp by remember { mutableFloatStateOf(ocrPrefs.getFloat("text_size_sp", 16f)) }
    var ujiTeksResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun changeTextSize(delta: Float) {
        textSizeSp = (textSizeSp + delta).coerceIn(OCR_TEXT_SIZE_MIN, OCR_TEXT_SIZE_MAX)
        ocrPrefs.edit().putFloat("text_size_sp", textSizeSp).apply()
    }

    LaunchedEffect(pagerState.currentPage) {
        sheetPageIndex = null
    }

    LaunchedEffect(ocrModeActive, pagerState.currentPage, pageCount) {
        if (ocrModeActive && pageCount > 0) {
            OcrStore.ensureWindow(context, uri, displayName, pageCount, pagerState.currentPage)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
            TextButton(onClick = { ocrModeActive = !ocrModeActive }) {
                Text(if (ocrModeActive) "OCR Aktif" else "OCR")
            }
            TextButton(onClick = {
                scope.launch {
                    val text = PdfTextExtractor.extractPageText(context, uri, pagerState.currentPage)
                    ujiTeksResult = text?.ifBlank { "(kosong - kemungkinan halaman hasil scan)" } ?: "(gagal ekstrak teks)"
                }
            }) {
                Text("Uji Teks")
            }
        }

        if (pageCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    ZoomablePdfPage(
                        uri = uri,
                        pageIndex = pageIndex,
                        onTap = {
                            if (ocrModeActive) {
                                sheetPageIndex = pageIndex
                            }
                        }
                    )
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

                val sheetIndex = sheetPageIndex
                if (sheetIndex != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f))
                            .pointerInput(sheetIndex) {
                                detectTapGestures(onTap = { sheetPageIndex = null })
                            }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.3f)
                            .pointerInput(sheetIndex) {
                                detectTapGestures(onTap = { })
                            },
                        tonalElevation = 4.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp
                        )
                    ) {
                        OcrTextSheet(
                            context = context,
                            displayName = displayName,
                            pageIndex = sheetIndex,
                            textSizeSp = textSizeSp,
                            onDecrease = { changeTextSize(-OCR_TEXT_SIZE_STEP) },
                            onIncrease = { changeTextSize(OCR_TEXT_SIZE_STEP) }
                        )
                    }
                }

                val ujiTeksText = ujiTeksResult
                if (ujiTeksText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                            .pointerInput(ujiTeksText) {
                                detectTapGestures(onTap = { ujiTeksResult = null })
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            ujiTeksText,
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrTextSheet(
    context: Context,
    displayName: String,
    pageIndex: Int,
    textSizeSp: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val readyKeys by OcrStore.readyKeys.collectAsState()
    val key = "$displayName|$pageIndex"
    val isReady = readyKeys.contains(key) || OcrStore.hasPageCache(context, displayName, pageIndex)

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ukuran Teks",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDecrease, modifier = Modifier.width(48.dp)) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = onIncrease, modifier = Modifier.width(48.dp)) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (isReady) {
            Text(
                OcrStore.readPageCache(context, displayName, pageIndex),
                fontSize = textSizeSp.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Sedang memproses OCR halaman ini...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomablePdfPage(uri: Uri, pageIndex: Int, onTap: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    LaunchedEffect(pageIndex) {
        bitmap = renderSinglePage(context, uri, pageIndex, RENDER_SCALE)
    }

    val bmp = bitmap
    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { coordinates -> containerSize = coordinates.size }
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
                            val currentBmp = bitmap
                            if (newZoom > 1f && currentBmp != null && containerSize.width > 0 && containerSize.height > 0) {
                                val containerW = containerSize.width.toFloat()
                                val containerH = containerSize.height.toFloat()
                                val bitmapAspect = currentBmp.width.toFloat() / currentBmp.height.toFloat()
                                val containerAspect = containerW / containerH
                                // ukuran halaman saat zoom = 1 (hasil ContentScale.Fit)
                                val fittedW: Float
                                val fittedH: Float
                                if (bitmapAspect > containerAspect) {
                                    fittedW = containerW
                                    fittedH = containerW / bitmapAspect
                                } else {
                                    fittedH = containerH
                                    fittedW = containerH * bitmapAspect
                                }
                                val scaledW = fittedW * newZoom
                                val scaledH = fittedH * newZoom
                                val maxOffsetX = ((scaledW - containerW) / 2f).coerceAtLeast(0f)
                                val maxOffsetY = ((scaledH - containerH) / 2f).coerceAtLeast(0f)
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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
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
