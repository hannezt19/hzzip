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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.PdfTextExtractor
import com.yohanes.filereader.data.OcrStore
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.lazy.*
import com.yohanes.filereader.data.ReaderSettingsStore
import com.yohanes.filereader.data.ReaderSettings
import com.yohanes.filereader.data.BacaWarnaLatar
import com.yohanes.filereader.data.NavigasiMode
import com.yohanes.filereader.data.TranslateHelper
import com.yohanes.filereader.data.ModelDownloadState
import kotlinx.coroutines.launch

private const val RENDER_SCALE = 2f

// Pecah teks jadi per-kalimat & beri jeda antar kalimat, biar lebih mudah dibaca
private fun formatReadableText(text: String): String {
    return text
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

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

    var modeBacaActive by remember { mutableStateOf(false) }
    var translateActive by remember { mutableStateOf(false) }
    var settingsModalOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ReaderSettingsStore.ensureLoaded(context)
    }
    val readerSettings by ReaderSettingsStore.settings.collectAsState()

    LaunchedEffect(pagerState.currentPage) {
        settingsModalOpen = false
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
        }

        if (pageCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val scrollListState = rememberLazyListState()
            LaunchedEffect(readerSettings.navMode, scrollListState.firstVisibleItemIndex) {
                if (readerSettings.navMode == NavigasiMode.SCROLL) {
                    prefs.edit().putInt(displayName, scrollListState.firstVisibleItemIndex).apply()
                    settingsModalOpen = false
                }
            }
            val currentPageNumber = if (readerSettings.navMode == NavigasiMode.SWIPE) {
                pagerState.currentPage + 1
            } else {
                scrollListState.firstVisibleItemIndex + 1
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (readerSettings.navMode == NavigasiMode.SWIPE) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        if (modeBacaActive) {
                            ReflowPage(
                                uri = uri,
                                displayName = displayName,
                                pageCount = pageCount,
                                pageIndex = pageIndex,
                                settings = readerSettings,
                                translateActive = translateActive,
                                onPrevPage = {
                                    scope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                                    }
                                },
                                onNextPage = {
                                    scope.launch {
                                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pageCount - 1))
                                    }
                                },
                                onTap = { settingsModalOpen = true }
                            )
                        } else {
                            ZoomablePdfPage(
                                uri = uri,
                                pageIndex = pageIndex,
                                onTap = { settingsModalOpen = true }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pageCount) { pageIndex ->
                            Box(modifier = Modifier.fillMaxWidth().fillParentMaxHeight()) {
                                if (modeBacaActive) {
                                    ReflowPage(
                                        uri = uri,
                                        displayName = displayName,
                                        pageCount = pageCount,
                                        pageIndex = pageIndex,
                                        settings = readerSettings,
                                        translateActive = translateActive,
                                        onPrevPage = {},
                                        onNextPage = {},
                                        onTap = { settingsModalOpen = true }
                                    )
                                } else {
                                    ZoomablePdfPage(
                                        uri = uri,
                                        pageIndex = pageIndex,
                                        onTap = { settingsModalOpen = true }
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    "$currentPageNumber / $pageCount",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = androidx.compose.ui.graphics.Color.White
                )

                if (settingsModalOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { settingsModalOpen = false })
                            }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .navigationBarsPadding()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { })
                            },
                        tonalElevation = 4.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp
                        )
                    ) {
                        SettingsPanel(
                            modeBacaActive = modeBacaActive,
                            onModeBacaChange = { modeBacaActive = it },
                            translateActive = translateActive,
                            onTranslateChange = { translateActive = it },
                            settings = readerSettings,
                            onTextSizeChange = { ReaderSettingsStore.setTextSize(context, it) },
                            onContrastChange = { ReaderSettingsStore.setContrast(context, it) },
                            onWarnaLatarChange = { ReaderSettingsStore.setWarnaLatar(context, it) },
                            onNavModeChange = { ReaderSettingsStore.setNavMode(context, it) }
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun SettingsPanel(
    modeBacaActive: Boolean,
    onModeBacaChange: (Boolean) -> Unit,
    translateActive: Boolean,
    onTranslateChange: (Boolean) -> Unit,
    settings: ReaderSettings,
    onTextSizeChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onWarnaLatarChange: (BacaWarnaLatar) -> Unit,
    onNavModeChange: (NavigasiMode) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = { onTextSizeChange(settings.textSizeSp - 2f) }) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "${settings.textSizeSp.toInt()} sp",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = { onTextSizeChange(settings.textSizeSp + 2f) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Switch(
                checked = modeBacaActive,
                onCheckedChange = onModeBacaChange
            )
        }

        Slider(
            value = settings.contrast,
            onValueChange = onContrastChange,
            valueRange = 0.5f..2f,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp)
        )

        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavigasiMode.values().forEach { mode ->
                val selected = settings.navMode == mode
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onNavModeChange(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (mode == NavigasiMode.SWIPE) "\u2194" else "\u2195",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BacaWarnaLatar.values().forEach { warna ->
                val selected = settings.warnaLatar == warna
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color(warna.bg))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.4f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .clickable { onWarnaLatarChange(warna) }
                )
            }
        }

        if (modeBacaActive) {
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ID",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = translateActive,
                    onCheckedChange = onTranslateChange
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ReflowPage(
    uri: Uri,
    displayName: String,
    pageCount: Int,
    pageIndex: Int,
    settings: ReaderSettings,
    translateActive: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var imageLoadDone by remember(pageIndex) { mutableStateOf(false) }
    var extractedText by remember(pageIndex) { mutableStateOf<String?>(null) }
    val readyKeys by OcrStore.readyKeys.collectAsState()

    var translatedText by remember(pageIndex, displayName) { mutableStateOf<String?>(null) }
    var downloadState by remember(pageIndex) { mutableStateOf<ModelDownloadState>(ModelDownloadState.Idle) }

    LaunchedEffect(pageIndex) {
        bitmap = PdfTextExtractor.extractMainImage(context, uri, pageIndex)
        imageLoadDone = true
        val text = PdfTextExtractor.extractPageText(context, uri, pageIndex)
        extractedText = text
        if (text.isNullOrBlank()) {
            OcrStore.ensureWindow(context, uri, displayName, pageCount, pageIndex)
        }
    }

    LaunchedEffect(extractedText, translateActive) {
        val srcText = extractedText
        if (translateActive && !srcText.isNullOrBlank()) {
            val cacheKey = "$displayName|$pageIndex"
            val cached = TranslateHelper.getCached(cacheKey)
            if (cached != null) {
                translatedText = cached
            } else {
                val lang = TranslateHelper.detectLanguage(srcText)
                if (lang != null && lang != "id") {
                    val translator = TranslateHelper.ensureModelDownloaded(lang) { state -> downloadState = state }
                    if (translator != null) {
                        val result = TranslateHelper.translate(translator, srcText)
                        TranslateHelper.cache(cacheKey, result)
                        translatedText = result
                    }
                } else {
                    translatedText = srcText
                }
            }
        }
    }

    val ocrKey = "$displayName|$pageIndex"
    val ocrReady = readyKeys.contains(ocrKey) || OcrStore.hasPageCache(context, displayName, pageIndex)

    val bgColor = androidx.compose.ui.graphics.Color(settings.warnaLatar.bg)
    val textColor = androidx.compose.ui.graphics.Color(settings.warnaLatar.teks)
    val contrastMatrix = remember(settings.contrast) {
        val c = settings.contrast
        val translate = (1f - c) / 2f * 255f
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    var fullscreenImage by remember(pageIndex) { mutableStateOf(false) }
    var fullscreenImages by remember(pageIndex) { mutableStateOf<List<Bitmap>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .verticalScroll(rememberScrollState())
                .pointerInput(pageIndex) {
                    detectTapGestures(onTap = { onTap() })
                }
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Halaman ${pageIndex + 1}",
                        colorFilter = ColorFilter.colorMatrix(contrastMatrix),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "\u2922",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { fullscreenImage = true })
                            }
                    )
                }
            } else if (!imageLoadDone) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            val text = extractedText
            when {
                text == null -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Memuat teks halaman...",
                                color = textColor,
                                fontSize = settings.textSizeSp.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                text.isNotBlank() -> {
                    if (translateActive) {
                        when {
                            downloadState is ModelDownloadState.Downloading -> {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Mengunduh model bahasa...",
                                            color = textColor,
                                            fontSize = settings.textSizeSp.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            translatedText != null -> {
                                Text(
                                    formatReadableText(translatedText ?: ""),
                                    color = textColor,
                                    fontSize = settings.textSizeSp.sp,
                                    lineHeight = (settings.textSizeSp * 1.6f).sp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            }
                            downloadState is ModelDownloadState.Error -> {
                                Text(
                                    text,
                                    color = textColor,
                                    fontSize = settings.textSizeSp.sp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                )
                            }
                            else -> {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Menerjemahkan...",
                                            color = textColor,
                                            fontSize = settings.textSizeSp.sp,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text,
                            color = textColor,
                            fontSize = settings.textSizeSp.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }
                ocrReady -> {
                    val ocrText = OcrStore.readPageCache(context, displayName, pageIndex)
                    Text(
                        ocrText.ifBlank { "(Teks halaman ini belum tersedia)" },
                        color = textColor,
                        fontSize = settings.textSizeSp.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
                else -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Memproses OCR halaman ini...",
                                color = textColor,
                                fontSize = settings.textSizeSp.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (settings.navMode == NavigasiMode.SWIPE) {
            if (pageIndex > 0) {
                Text(
                    "\u2190",
                    color = textColor,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onPrevPage() })
                        }
                )
            }
            if (pageIndex < pageCount - 1) {
                Text(
                    "\u2192",
                    color = textColor,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onNextPage() })
                        }
                )
            }
        }

        if (fullscreenImage && bitmap != null) {
            LaunchedEffect(pageIndex, fullscreenImage) {
                if (fullscreenImage && fullscreenImages == null) {
                    fullscreenImages = PdfTextExtractor.extractAllImages(context, uri, pageIndex)
                }
            }
            val images = fullscreenImages?.takeIf { it.isNotEmpty() } ?: listOf(bitmap)
            val fsPagerState = rememberPagerState(pageCount = { images.size })
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
            ) {
                HorizontalPager(
                    state = fsPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { imgIndex ->
                    ZoomableImageBox(
                        bitmap = images[imgIndex],
                        contentDescription = "Halaman ${pageIndex + 1} gambar ${imgIndex + 1} diperbesar",
                        onTap = {}
                    )
                }
                Text(
                    "\u2715",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { fullscreenImage = false })
                        }
                )
            }
        }
    }
}

@Composable
private fun ZoomableImageBox(
    bitmap: Bitmap?,
    contentDescription: String,
    onTap: () -> Unit
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { coordinates -> containerSize = coordinates.size }
            .pointerInput(bitmap) {
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
            .pointerInput(bitmap) {
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
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
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

@Composable
private fun ZoomablePdfPage(uri: Uri, pageIndex: Int, onTap: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        bitmap = renderSinglePage(context, uri, pageIndex, RENDER_SCALE)
    }

    ZoomableImageBox(
        bitmap = bitmap,
        contentDescription = "Halaman ${pageIndex + 1}",
        onTap = onTap
    )
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
