package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
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
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.PdfTextExtractor
import com.yohanes.filereader.data.OcrStore
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import com.yohanes.filereader.data.PdfPageThumbCache
import com.yohanes.filereader.data.ReaderSettingsStore
import com.yohanes.filereader.data.ReaderSettings
import com.yohanes.filereader.data.BacaWarnaLatar
import com.yohanes.filereader.data.NavigasiMode
import com.yohanes.filereader.data.TranslateHelper
import com.yohanes.filereader.data.ModelDownloadState
import com.yohanes.filereader.data.TtsHelper
import com.yohanes.filereader.data.PageBitmapCache
import com.yohanes.filereader.data.PdfRenderSessionCache
import com.yohanes.filereader.data.PdfThumbnailCache
import java.io.File
import kotlinx.coroutines.launch

private const val RENDER_SCALE = 2f

// Pecah teks jadi per-kalimat, dipakai untuk tampilan reflow & TTS
private fun splitSentences(text: String): List<String> {
    return text
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun formatReadableText(text: String): String {
    return splitSentences(text).joinToString("\n\n")
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
        val session = PdfRenderSessionCache.getOrCreate(context, uri)
        pageCount = session.pageCount
        onDispose {
            PdfRenderSessionCache.closeIfMatches(uri)
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
    var pageGridOpen by remember { mutableStateOf(false) }
    var fullscreenImages by remember { mutableStateOf<List<Bitmap>?>(null) }
    val scope = rememberCoroutineScope()

    var ttsActive by remember { mutableStateOf(false) }
    var ttsPlaying by remember { mutableStateOf(false) }
    var ttsPanelExpanded by remember { mutableStateOf(false) }
    var ttsSentences by remember { mutableStateOf<List<String>>(emptyList()) }
    var ttsSentenceIndex by remember { mutableStateOf(0) }
    var ttsCurrentPageIndex by remember { mutableStateOf(0) }

    BackHandler(enabled = pageGridOpen) {
        pageGridOpen = false
    }
    BackHandler(enabled = !pageGridOpen && fullscreenImages != null) {
        fullscreenImages = null
    }
    BackHandler(enabled = fullscreenImages == null && ttsPanelExpanded) {
        ttsPanelExpanded = false
    }
    BackHandler(enabled = fullscreenImages == null && !ttsPanelExpanded && settingsModalOpen) {
        settingsModalOpen = false
    }
    BackHandler(enabled = fullscreenImages == null && !ttsPanelExpanded && !settingsModalOpen && modeBacaActive) {
        modeBacaActive = false
    }

    LaunchedEffect(Unit) {
        ReaderSettingsStore.ensureLoaded(context)
    }
    val readerSettings by ReaderSettingsStore.settings.collectAsState()

    LaunchedEffect(pagerState.currentPage) {
        settingsModalOpen = false
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF1C1C1E))
                .padding(horizontal = 4.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                displayName,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 4.dp)
            )
            IconButton(onClick = { FavoritesStore.toggle(favKey) }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorit",
                    tint = if (isFav) androidx.compose.ui.graphics.Color(0xFFFFC107) else androidx.compose.ui.graphics.Color.White
                )
            }
            IconButton(onClick = { pageGridOpen = true }) {
                Text(
                    "\u229E",
                    style = MaterialTheme.typography.headlineSmall,
                    color = androidx.compose.ui.graphics.Color.White
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

            suspend fun loadTtsSentences(pageIndex: Int): List<String> {
                val extracted = PdfTextExtractor.extractPageText(context, uri, pageIndex) ?: ""
                if (extracted.isBlank()) return emptyList()
                val sourceText = if (translateActive) {
                    val cacheKey = "$displayName|$pageIndex"
                    TranslateHelper.getCached(cacheKey) ?: run {
                        val lang = TranslateHelper.detectLanguage(extracted)
                        if (lang != null && lang != "id") {
                            val translator = TranslateHelper.ensureModelDownloaded(lang) {}
                            if (translator != null) {
                                val result = TranslateHelper.translate(translator, extracted)
                                TranslateHelper.cache(cacheKey, result)
                                result
                            } else extracted
                        } else extracted
                    }
                } else extracted
                return splitSentences(sourceText)
            }

            LaunchedEffect(Unit) {
                TtsHelper.onAudioFocusLost = { ttsPlaying = false }
            }

            fun speakSentence(index: Int) {
                val s = ttsSentences.getOrNull(index) ?: return
                TtsHelper.speak(s, readerSettings.ttsVolume, readerSettings.ttsPitch, readerSettings.ttsSpeed)
            }

            fun playFromPage(pageIndex: Int, sentenceIndex: Int) {
                scope.launch {
                    if (ttsCurrentPageIndex != pageIndex || ttsSentences.isEmpty()) {
                        ttsCurrentPageIndex = pageIndex
                        ttsSentences = loadTtsSentences(pageIndex)
                    }
                    if (ttsSentences.isEmpty()) {
                        ttsPlaying = false
                        return@launch
                    }
                    ttsSentenceIndex = sentenceIndex.coerceIn(0, ttsSentences.size - 1)
                    ttsPlaying = true
                    speakSentence(ttsSentenceIndex)
                }
            }

            LaunchedEffect(Unit) {
                TtsHelper.ensureInit(context) {
                    scope.launch {
                        if (!ttsPlaying) return@launch
                        val nextIndex = ttsSentenceIndex + 1
                        if (nextIndex < ttsSentences.size) {
                            ttsSentenceIndex = nextIndex
                            speakSentence(nextIndex)
                        } else if (readerSettings.navMode == NavigasiMode.SCROLL && ttsCurrentPageIndex < pageCount - 1) {
                            val nextPage = ttsCurrentPageIndex + 1
                            ttsCurrentPageIndex = nextPage
                            ttsSentences = loadTtsSentences(nextPage)
                            ttsSentenceIndex = 0
                            if (ttsSentences.isNotEmpty()) {
                                speakSentence(0)
                                scrollListState.animateScrollToItem(nextPage)
                            } else {
                                ttsPlaying = false
                            }
                        } else {
                            ttsPlaying = false
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                                fillScreen = true,
                                onExpandImage = { imgs -> fullscreenImages = imgs },
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
                    val scrollZoomableState = rememberZoomableState()
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = scrollListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .zoomable(scrollZoomableState, onClick = { settingsModalOpen = true })
                        ) {
                            items(pageCount, key = { it }) { pageIndex ->
                                if (modeBacaActive) {
                                    ReflowPage(
                                        uri = uri,
                                        displayName = displayName,
                                        pageCount = pageCount,
                                        pageIndex = pageIndex,
                                        settings = readerSettings,
                                        translateActive = translateActive,
                                        fillScreen = false,
                                        onExpandImage = { imgs -> fullscreenImages = imgs },
                                        onPrevPage = {},
                                        onNextPage = {},
                                        onTap = { settingsModalOpen = true }
                                    )
                                } else {
                                    Column {
                                        ScrollPdfPage(
                                            uri = uri,
                                            pageIndex = pageIndex,
                                            ownGestures = false
                                        )
                                        if (pageIndex < pageCount - 1) {
                                            HorizontalDivider(
                                                thickness = 1.dp,
                                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)
                                            )
                                        }
                                    }
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

                if (settingsModalOpen || ttsPanelExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    if (ttsPanelExpanded) ttsPanelExpanded = false else settingsModalOpen = false
                                })
                            }
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(0.42f)
                            .navigationBarsPadding()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { })
                            },
                        color = androidx.compose.ui.graphics.Color(0xFF2B2B2E),
                        tonalElevation = 4.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp
                        )
                    ) {
                        if (ttsPanelExpanded) {
                            VolumePanel(
                                settings = readerSettings,
                                onVolumeChange = { ReaderSettingsStore.setTtsVolume(context, it) },
                                onPitchChange = { ReaderSettingsStore.setTtsPitch(context, it) },
                                onSpeedChange = { ReaderSettingsStore.setTtsSpeed(context, it) }
                            )
                        } else {
                            SettingsPanel(
                                modeBacaActive = modeBacaActive,
                                onModeBacaChange = { modeBacaActive = it },
                                translateActive = translateActive,
                                onTranslateChange = { translateActive = it },
                                ttsActive = ttsActive,
                                onTtsActiveChange = { active ->
                                    ttsActive = active
                                    if (!active) {
                                        TtsHelper.stop()
                                        ttsPlaying = false
                                        ttsPanelExpanded = false
                                    }
                                },
                                settings = readerSettings,
                                onTextSizeChange = { ReaderSettingsStore.setTextSize(context, it) },
                                onContrastChange = { ReaderSettingsStore.setContrast(context, it) },
                                onWarnaLatarChange = { ReaderSettingsStore.setWarnaLatar(context, it) },
                                onNavModeChange = { ReaderSettingsStore.setNavMode(context, it) }
                            )
                        }
                    }
                }

                if (pageGridOpen) {
                    PageGridOverlay(
                        uri = uri,
                        pageCount = pageCount,
                        onPageSelected = { pageIndex ->
                            pageGridOpen = false
                            scope.launch {
                                if (readerSettings.navMode == NavigasiMode.SWIPE) {
                                    pagerState.scrollToPage(pageIndex)
                                } else {
                                    scrollListState.scrollToItem(pageIndex)
                                }
                            }
                        },
                        onClose = { pageGridOpen = false }
                    )
                }

                if (fullscreenImages != null) {
                    val images = fullscreenImages!!
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
                                contentDescription = "Gambar ${imgIndex + 1} diperbesar",
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
                                    detectTapGestures(onTap = { fullscreenImages = null })
                                }
                        )
                    }
                }

                if (ttsActive) {
                    val ttsBarBottomPadding = if (settingsModalOpen || ttsPanelExpanded) maxHeight * 0.42f else 0.dp
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = ttsBarBottomPadding + 20.dp)
                            .fillMaxWidth(0.85f),
                        color = androidx.compose.ui.graphics.Color.Black,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TtsPillButton(icon = "\u23EA") {
                                val newIndex = (ttsSentenceIndex - 1).coerceAtLeast(0)
                                if (ttsPlaying) {
                                    playFromPage(ttsCurrentPageIndex, newIndex)
                                } else {
                                    ttsSentenceIndex = newIndex
                                }
                            }
                            TtsPillButton(icon = if (ttsPlaying) "\u23F8" else "\u25B6") {
                                if (ttsPlaying) {
                                    TtsHelper.stop()
                                    ttsPlaying = false
                                } else {
                                    val pageIdx = if (readerSettings.navMode == NavigasiMode.SWIPE) pagerState.currentPage else scrollListState.firstVisibleItemIndex
                                    playFromPage(pageIdx, ttsSentenceIndex)
                                }
                            }
                            TtsPillButton(icon = "\u23E9") {
                                val maxIndex = (ttsSentences.size - 1).coerceAtLeast(0)
                                val newIndex = (ttsSentenceIndex + 1).coerceAtMost(maxIndex)
                                if (ttsPlaying) {
                                    playFromPage(ttsCurrentPageIndex, newIndex)
                                } else {
                                    ttsSentenceIndex = newIndex
                                }
                            }
                            TtsPillButton(icon = "\u2699") { ttsPanelExpanded = !ttsPanelExpanded }
                            TtsPillButton(icon = "\u2715") {
                                TtsHelper.stop()
                                ttsPlaying = false
                                ttsActive = false
                                ttsPanelExpanded = false
                            }
                        }
                    }

                }

            }
        }
    }
}

@Composable
private fun TtsPillButton(icon: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .background(
                if (pressed) androidx.compose.ui.graphics.Color(0xFFB2EBF2)
                else androidx.compose.ui.graphics.Color.White
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, color = androidx.compose.ui.graphics.Color.Black, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun StyledSlider(value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun VolumePanel(
    settings: ReaderSettings,
    onVolumeChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Volume", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
        StyledSlider(value = settings.ttsVolume, onValueChange = onVolumeChange, valueRange = 0f..1f)
        Text("Pitch (Nada)", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
        StyledSlider(value = settings.ttsPitch, onValueChange = onPitchChange, valueRange = 0.5f..2f)
        Text("Speed (Kecepatan)", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
        StyledSlider(value = settings.ttsSpeed, onValueChange = onSpeedChange, valueRange = 0.5f..2f)
    }
}

@Composable
private fun ModeToggleButton(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .border(
                width = if (active) 2.dp else 1.dp,
                color = androidx.compose.ui.graphics.Color(0xFF4DD0E1).copy(alpha = if (active) 1f else 0.4f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (active) androidx.compose.ui.graphics.Color(0xFF4DD0E1) else androidx.compose.ui.graphics.Color.LightGray,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SettingsPanel(
    modeBacaActive: Boolean,
    onModeBacaChange: (Boolean) -> Unit,
    translateActive: Boolean,
    onTranslateChange: (Boolean) -> Unit,
    ttsActive: Boolean,
    onTtsActiveChange: (Boolean) -> Unit,
    settings: ReaderSettings,
    onTextSizeChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onWarnaLatarChange: (BacaWarnaLatar) -> Unit,
    onNavModeChange: (NavigasiMode) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.237f), contentAlignment = Alignment.Center) {
            StyledSlider(value = settings.contrast, onValueChange = onContrastChange, valueRange = 0.5f..2f)
        }

        Row(
            Modifier.fillMaxWidth().weight(0.237f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(1.15f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f))
                        .clickable { onTextSizeChange(settings.textSizeSp - 2f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${settings.textSizeSp.toInt()}sp",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f))
                        .clickable { onTextSizeChange(settings.textSizeSp + 2f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(6.dp))
            ModeToggleButton("Baca", modeBacaActive, modifier = Modifier.weight(1f)) { onModeBacaChange(!modeBacaActive) }
            Spacer(Modifier.width(6.dp))
            ModeToggleButton("TTS", ttsActive, modifier = Modifier.weight(1f)) { onTtsActiveChange(!ttsActive) }
            Spacer(Modifier.width(6.dp))
            ModeToggleButton("ID", translateActive, modifier = Modifier.weight(1f)) { onTranslateChange(!translateActive) }
        }

        Row(
            Modifier.fillMaxWidth().weight(0.237f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigasiMode.values().forEach { mode ->
                val selected = settings.navMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .height(40.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = androidx.compose.ui.graphics.Color(0xFF4DD0E1).copy(alpha = if (selected) 1f else 0.4f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .clickable { onNavModeChange(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (mode == NavigasiMode.SWIPE) "\u2194" else "\u2195",
                        color = if (selected) androidx.compose.ui.graphics.Color(0xFF4DD0E1) else androidx.compose.ui.graphics.Color.LightGray,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().weight(0.237f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BacaWarnaLatar.values().forEach { warna ->
                val selected = settings.warnaLatar == warna
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(40.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color(warna.bg))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = androidx.compose.ui.graphics.Color(0xFF4DD0E1).copy(alpha = if (selected) 1f else 0.4f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .clickable { onWarnaLatarChange(warna) }
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
    fillScreen: Boolean,
    onExpandImage: (List<Bitmap>) -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    @Composable
    fun PageContent() {
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
                            detectTapGestures(onTap = {
                                scope.launch {
                                    val images = PdfTextExtractor.extractAllImages(context, uri, pageIndex)
                                        .takeIf { it.isNotEmpty() } ?: listOfNotNull(bitmap)
                                    onExpandImage(images)
                                }
                            })
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
                                formatReadableText(text),
                                color = textColor,
                                fontSize = settings.textSizeSp.sp,
                                lineHeight = (settings.textSizeSp * 1.6f).sp,
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
                        formatReadableText(text),
                        color = textColor,
                        fontSize = settings.textSizeSp.sp,
                        lineHeight = (settings.textSizeSp * 1.6f).sp,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
            ocrReady -> {
                val ocrText = OcrStore.readPageCache(context, displayName, pageIndex)
                Text(
                    formatReadableText(ocrText.ifBlank { "(Teks halaman ini belum tersedia)" }),
                    color = textColor,
                    fontSize = settings.textSizeSp.sp,
                    lineHeight = (settings.textSizeSp * 1.6f).sp,
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

    if (fillScreen) {
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
                PageContent()
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
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .pointerInput(pageIndex) {
                    detectTapGestures(onTap = { onTap() })
                }
        ) {
            PageContent()
        }
    }
}

@Composable
private fun ZoomableImageBox(
    bitmap: Bitmap?,
    contentDescription: String,
    onTap: () -> Unit,
    externalZoom: Float? = null,
    onExternalZoomChange: ((Float) -> Unit)? = null,
    clipOwnBounds: Boolean = true,
    externalOffsetX: Float? = null,
    externalOffsetY: Float? = null,
    onExternalOffsetChange: ((Float, Float) -> Unit)? = null,
    ownGestures: Boolean = true
) {
    var localZoom by remember { mutableFloatStateOf(1f) }
    val zoom = externalZoom ?: localZoom
    val liveZoom = rememberUpdatedState(zoom)
    val liveSetZoom = rememberUpdatedState<(Float) -> Unit> { newZoom ->
        if (externalZoom != null && onExternalZoomChange != null) onExternalZoomChange(newZoom) else localZoom = newZoom
    }
    var localOffsetX by remember { mutableFloatStateOf(0f) }
    var localOffsetY by remember { mutableFloatStateOf(0f) }
    val offsetX = externalOffsetX ?: localOffsetX
    val offsetY = externalOffsetY ?: localOffsetY
    val liveOffsetX = rememberUpdatedState(offsetX)
    val liveOffsetY = rememberUpdatedState(offsetY)
    fun setOffset(newX: Float, newY: Float) {
        if (externalOffsetX != null && onExternalOffsetChange != null) onExternalOffsetChange(newX, newY) else { localOffsetX = newX; localOffsetY = newY }
    }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { coordinates -> containerSize = coordinates.size }
            .then(
                if (clipOwnBounds && ownGestures) Modifier
                    .pointerInput(bitmap) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val isPinch = event.changes.size >= 2
                                if (isPinch || liveZoom.value > 1f) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val newZoom = (liveZoom.value * zoomChange).coerceIn(1f, 5f)
                                    liveSetZoom.value(newZoom)
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
                                        val newOffsetX = (liveOffsetX.value + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                                        val newOffsetY = (liveOffsetY.value + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        val panMasihBisaGerak = newOffsetX != liveOffsetX.value || newOffsetY != liveOffsetY.value
                                        setOffset(newOffsetX, newOffsetY)
                                        if (isPinch || panMasihBisaGerak) {
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else {
                                        setOffset(0f, 0f)
                                        if (isPinch) {
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(bitmap) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onDoubleTap = {
                                if (liveZoom.value > 1f) {
                                    liveSetZoom.value(1f)
                                    setOffset(0f, 0f)
                                } else {
                                    liveSetZoom.value(2.5f)
                                }
                            }
                        )
                    }
                else Modifier
            ),
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
private fun ScrollPdfPage(uri: Uri, pageIndex: Int, onTap: () -> Unit = {}, sharedZoom: Float? = null, onSharedZoomChange: ((Float) -> Unit)? = null, sharedOffsetX: Float? = null, sharedOffsetY: Float? = null, onSharedOffsetChange: ((Float, Float) -> Unit)? = null, ownGestures: Boolean = true) {
    val context = LocalContext.current
    var aspect by remember(pageIndex) { mutableFloatStateOf(0.7071f) }

    LaunchedEffect(pageIndex) {
        val session = PdfRenderSessionCache.getOrCreate(context, uri)
        val size = session.getPageSize(pageIndex)
        if (size != null && size.second > 0) {
            aspect = size.first.toFloat() / size.second.toFloat()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
    ) {
        ZoomablePdfPage(
            uri = uri,
            pageIndex = pageIndex,
            onTap = onTap,
            sharedZoom = sharedZoom,
            onSharedZoomChange = onSharedZoomChange,
            sharedOffsetX = sharedOffsetX,
            sharedOffsetY = sharedOffsetY,
            onSharedOffsetChange = onSharedOffsetChange,
            ownGestures = ownGestures
        )
    }
}

@Composable
private fun ZoomablePdfPage(uri: Uri, pageIndex: Int, onTap: () -> Unit, sharedZoom: Float? = null, onSharedZoomChange: ((Float) -> Unit)? = null, sharedOffsetX: Float? = null, sharedOffsetY: Float? = null, onSharedOffsetChange: ((Float, Float) -> Unit)? = null, ownGestures: Boolean = true) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        if (pageIndex == 0) {
            val path = uri.path
            if (path != null) {
                val lastModified = File(path).lastModified()
                PdfThumbnailCache.get(context, path, lastModified)?.let { cached ->
                    bitmap = cached
                }
            }
        }

        val memCached = PageBitmapCache.get(uri.toString(), pageIndex)
        if (memCached != null) {
            bitmap = memCached
            return@LaunchedEffect
        }

        val session = PdfRenderSessionCache.getOrCreate(context, uri)
        val rendered = session.renderPage(pageIndex, RENDER_SCALE)
        if (rendered != null) {
            bitmap = rendered
            PageBitmapCache.put(uri.toString(), pageIndex, rendered)

            if (pageIndex == 0) {
                val path = uri.path
                if (path != null) {
                    PdfThumbnailCache.put(context, path, File(path).lastModified(), rendered)
                }
            }
        }
    }

    ZoomableImageBox(
        bitmap = bitmap,
        contentDescription = "Halaman ${pageIndex + 1}",
        onTap = onTap,
        externalZoom = sharedZoom,
        onExternalZoomChange = onSharedZoomChange,
        clipOwnBounds = sharedZoom == null,
        externalOffsetX = sharedOffsetX,
        externalOffsetY = sharedOffsetY,
        onExternalOffsetChange = onSharedOffsetChange,
        ownGestures = ownGestures
    )
}

@Composable
private fun PageGridOverlay(
    uri: Uri,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClose() })
                }
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .navigationBarsPadding(),
            color = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
            tonalElevation = 4.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pageCount) { pageIndex ->
                    ThumbGridItem(
                        uri = uri,
                        pageIndex = pageIndex,
                        onClick = { onPageSelected(pageIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbGridItem(uri: Uri, pageIndex: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        val cached = PdfPageThumbCache.get(uri.toString(), pageIndex)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }
        val session = PdfRenderSessionCache.getOrCreate(context, uri)
        val rendered = session.renderPage(pageIndex, PdfPageThumbCache.THUMB_SCALE)
        if (rendered != null) {
            bitmap = rendered
            PdfPageThumbCache.put(uri.toString(), pageIndex, rendered)
        }
    }

    Column(
        modifier = Modifier
            .width(90.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .background(androidx.compose.ui.graphics.Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Halaman ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }
        Text(
            "${pageIndex + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.LightGray
        )
    }
}
