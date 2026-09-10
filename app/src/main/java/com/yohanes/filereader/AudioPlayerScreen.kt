package com.yohanes.filereader

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yohanes.filereader.data.AppDatabase
import com.yohanes.filereader.data.FileEntity
import com.yohanes.filereader.data.Id3UsltReader
import com.yohanes.filereader.data.LyricLine
import com.yohanes.filereader.data.LyricsStore
import com.yohanes.filereader.data.PlaylistDao
import com.yohanes.filereader.data.PlaylistEntity
import com.yohanes.filereader.ui.SortOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import java.io.File

private val PlayerBg = Color(0xFFEEEEF2)
private val PlayerSurface = Color(0xFFEEEEF2)
private val PlayerAccentCyan = Color(0xFF4DD0E1)
private val TextDark = Color(0xFF2B2B2E)

private fun Modifier.softRaised(shape: Shape, baseColor: Color): Modifier = this
    .shadow(elevation = 10.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black, clip = false)
    .background(baseColor, shape)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.12f)
            )
        ),
        shape = shape
    )

private val albumArtCache = mutableMapOf<String, Bitmap?>()

private suspend fun loadAlbumArt(path: String): Bitmap? {
    if (albumArtCache.containsKey(path)) return albumArtCache[path]
    return withContext(Dispatchers.IO) {
        val art = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val artBytes = retriever.embeddedPicture
            retriever.release()
            artBytes?.let { bytes ->
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
                var inSampleSize = 1
                val targetPx = 480
                while (boundsOptions.outWidth / inSampleSize > targetPx) inSampleSize *= 2
                val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
        albumArtCache[path] = art
        art
    }
}

/** Bersihkan nama file jadi judul yang enak dibaca: buang ekstensi, underscore jadi spasi. */
private fun cleanTitle(rawName: String): String {
    val withoutExt = rawName.substringBeforeLast(".")
    return withoutExt.replace("_", " ").trim()
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioPlayerScreen(filePath: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlistDao = remember { AppDatabase.getInstance(context).playlistDao() }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_NEWEST) }
    var playlist by remember { mutableStateOf<List<FileEntity>>(emptyList()) }
    var currentMediaId by remember { mutableStateOf(filePath) }
    var currentTitle by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }

    val customPlaylistFlow = remember(playlistDao) { playlistDao.getAll() }
    val customPlaylistEntries by customPlaylistFlow.collectAsState(initial = emptyList())
    val fileByPath = remember(playlist) { playlist.associateBy { it.path } }
    val customPlaylistPaths = remember(customPlaylistEntries) { customPlaylistEntries.map { it.path }.toSet() }

    suspend fun loadAndPlay(option: SortOption) {
        val dao = AppDatabase.getInstance(context).fileDao()
        val all = dao.getAudios().first()
        val sorted = when (option) {
            SortOption.NAME_AZ -> all.sortedBy { it.name.lowercase() }
            SortOption.DATE_NEWEST -> all.sortedByDescending { it.lastModified }
            SortOption.SIZE_LARGEST -> all.sortedByDescending { it.sizeBytes }
        }
        playlist = sorted
        val startIndex = sorted.indexOfFirst { it.path == filePath }.coerceAtLeast(0)
        val mediaItems = sorted.map { entity ->
            MediaItem.Builder()
                .setUri(Uri.fromFile(File(entity.path)))
                .setMediaId(entity.path)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(entity.name).build())
                .build()
        }
        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
    }

    fun playFromCustomPlaylist(startIndex: Int) {
        // Filter dulu entry yang file-nya sudah tidak ketemu, BARU hitung index -
        // supaya startIndex tetap sinkron dengan mediaItems (jangan geser gara-gara ada yang dibuang).
        val validEntries = customPlaylistEntries.filter { fileByPath.containsKey(it.path) }
        val mediaItems = validEntries.map { entry ->
            val fe = fileByPath.getValue(entry.path)
            MediaItem.Builder()
                .setUri(Uri.fromFile(File(fe.path)))
                .setMediaId(fe.path)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(fe.name).build())
                .build()
        }
        if (mediaItems.isEmpty()) return
        val targetEntry = customPlaylistEntries.getOrNull(startIndex)
        val adjustedIndex = validEntries.indexOf(targetEntry).takeIf { it >= 0 } ?: 0
        controller?.setMediaItems(mediaItems, adjustedIndex.coerceIn(0, mediaItems.size - 1), 0L)
        controller?.prepare()
        controller?.play()
    }

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlayerService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            val c = controllerFuture.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaId = mediaItem?.mediaId ?: ""
                    currentTitle = mediaItem?.mediaMetadata?.title?.toString() ?: ""
                }
            })
            scope.launch { loadAndPlay(sortOption) }
        }, MoreExecutors.directExecutor())

        onDispose {
            controller?.release()
        }
    }

    LaunchedEffect(controller) {
        while (true) {
            controller?.let { c ->
                if (!isUserSeeking) {
                    currentPosition = c.currentPosition.coerceAtLeast(0L)
                }
                duration = c.duration.takeIf { it > 0 } ?: 0L
            }
            delay(500)
        }
    }

    LaunchedEffect(currentMediaId) {
        albumArt = if (currentMediaId.isNotBlank()) loadAlbumArt(currentMediaId) else null
    }

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    Column(
        Modifier
            .fillMaxSize()
            .background(PlayerBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> PlaylistPage(
                    playlist = playlist,
                    customPlaylistEntries = customPlaylistEntries,
                    customPlaylistPaths = customPlaylistPaths,
                    fileByPath = fileByPath,
                    playlistDao = playlistDao,
                    scope = scope,
                    isPlaying = isPlaying,
                    onTogglePlay = { if (isPlaying) controller?.pause() else controller?.play() },
                    onPrev = { controller?.seekToPrevious() },
                    onNext = { controller?.seekToNext() },
                    onPlayCustom = { idx -> playFromCustomPlaylist(idx) },
                    onPlayAll = { idx -> controller?.seekTo(idx, 0L); controller?.play() }
                )
                1 -> PlayerPage(
                    albumArt = albumArt,
                    title = cleanTitle(currentTitle.ifBlank { "Memuat..." }),
                    currentPosition = currentPosition,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeekChange = { isUserSeeking = true; currentPosition = it.toLong() },
                    onSeekFinished = { controller?.seekTo(currentPosition); isUserSeeking = false },
                    onTogglePlay = { if (isPlaying) controller?.pause() else controller?.play() },
                    onPrev = { controller?.seekToPrevious() },
                    onNext = { controller?.seekToNext() }
                )
                2 -> LyricsPage(
                    songPath = currentMediaId,
                    currentPosition = currentPosition,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeekChange = { isUserSeeking = true; currentPosition = it.toLong() },
                    onSeekFinished = { controller?.seekTo(currentPosition); isUserSeeking = false },
                    onTogglePlay = { if (isPlaying) controller?.pause() else controller?.play() },
                    onPrev = { controller?.seekToPrevious() },
                    onNext = { controller?.seekToNext() }
                )
            }
        }
    }
}

@Composable
private fun PlayerControlBar(
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    isPlaying: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .softRaised(RoundedCornerShape(50), PlayerSurface)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(52.dp).softRaised(CircleShape, PlayerSurface)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = TextDark, modifier = Modifier.size(26.dp))
            }
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(70.dp)
                    .softRaised(CircleShape, PlayerSurface)
                    .border(1.5.dp, PlayerAccentCyan.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = PlayerAccentCyan,
                    modifier = Modifier.size(34.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(52.dp).softRaised(CircleShape, PlayerSurface)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = TextDark, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun SeekBarSection(
    currentPosition: Long,
    duration: Long,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    val safeDuration = duration.coerceAtLeast(1L)
    Slider(
        value = currentPosition.coerceIn(0L, safeDuration).toFloat(),
        onValueChange = onSeekChange,
        onValueChangeFinished = onSeekFinished,
        valueRange = 0f..safeDuration.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = PlayerAccentCyan,
            activeTrackColor = PlayerAccentCyan,
            inactiveTrackColor = TextDark.copy(alpha = 0.15f)
        )
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatDuration(currentPosition), style = MaterialTheme.typography.bodySmall, color = TextDark.copy(alpha = 0.5f))
        Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = TextDark.copy(alpha = 0.5f))
    }
}

@Composable
private fun PlayerPage(
    albumArt: Bitmap?,
    title: String,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            Modifier.size(280.dp).softRaised(CircleShape, PlayerSurface),
            contentAlignment = Alignment.Center
        ) {
            if (albumArt != null) {
                Image(
                    bitmap = albumArt.asImageBitmap(),
                    contentDescription = "Sampul album",
                    modifier = Modifier.fillMaxSize().padding(6.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("\u266A", fontSize = 64.sp, color = TextDark.copy(alpha = 0.25f))
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextDark,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.weight(1f))

        SeekBarSection(currentPosition, duration, onSeekChange, onSeekFinished)

        Spacer(Modifier.height(40.dp))

        PlayerControlBar(onTogglePlay, onPrev, onNext, isPlaying)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LyricsPage(
    songPath: String,
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var lyricLines by remember { mutableStateOf<List<LyricLine>?>(null) }

    var isSynced by remember { mutableStateOf(true) }

    LaunchedEffect(songPath) {
        val result = if (songPath.isNotBlank()) {
            withContext(Dispatchers.IO) { LyricsStore.loadForSong(songPath) }
        } else null
        lyricLines = result?.lines
        isSynced = result?.synced ?: true
    }

    val lines = lyricLines
    val listState = rememberLazyListState()
    val activeIndex = remember(lines, currentPosition, isSynced) {
        if (isSynced) lines?.indexOfLast { it.timeMs <= currentPosition } ?: -1 else -1
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 3).coerceAtLeast(0))
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (lines.isNullOrEmpty()) {
                Text("Lirik tidak tersedia", color = TextDark.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (!isSynced) {
                        Text(
                            "Lirik dari tag lagu (tidak tersinkron)",
                            color = TextDark.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = if (isSynced) 160.dp else 8.dp)
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val isActive = isSynced && index == activeIndex
                            Text(
                                line.text.ifBlank { "\u266A" },
                                color = if (isActive) PlayerAccentCyan else TextDark.copy(alpha = 0.7f),
                                style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        SeekBarSection(currentPosition, duration, onSeekChange, onSeekFinished)
        Spacer(Modifier.height(20.dp))
        PlayerControlBar(onTogglePlay, onPrev, onNext, isPlaying)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlaylistPage(
    playlist: List<FileEntity>,
    customPlaylistEntries: List<PlaylistEntity>,
    customPlaylistPaths: Set<String>,
    fileByPath: Map<String, FileEntity>,
    playlistDao: PlaylistDao,
    scope: CoroutineScope,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlayCustom: (Int) -> Unit,
    onPlayAll: (Int) -> Unit
) {
    var sheetTab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        TabRow(
            selectedTabIndex = sheetTab,
            containerColor = PlayerSurface,
            contentColor = TextDark
        ) {
            Tab(
                selected = sheetTab == 0,
                onClick = { sheetTab = 0 },
                text = { Text("Playlist", color = TextDark) },
                selectedContentColor = TextDark,
                unselectedContentColor = TextDark.copy(alpha = 0.5f)
            )
            Tab(
                selected = sheetTab == 1,
                onClick = { sheetTab = 1 },
                text = { Text("Semua Audio", color = TextDark) },
                selectedContentColor = TextDark,
                unselectedContentColor = TextDark.copy(alpha = 0.5f)
            )
        }

        Box(Modifier.weight(1f)) {
            when (sheetTab) {
                0 -> {
                    if (customPlaylistEntries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Playlist masih kosong. Tambah dari tab \"Semua Audio\".", color = TextDark.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(customPlaylistEntries, key = { it.path }) { entry: PlaylistEntity ->
                                val fe = fileByPath[entry.path]
                                val fallbackName = fe?.name ?: entry.path.substringAfterLast("/")
                                ListItem(
                                    leadingContent = { SongThumbnail(entry.path) },
                                    headlineContent = { SongRowTitle(entry.path, fallbackName) },
                                    trailingContent = {
                                        SongOptionsMenu(
                                            path = entry.path,
                                            name = fallbackName,
                                            sizeBytes = fe?.sizeBytes ?: 0L,
                                            inPlaylist = true,
                                            onToggleInPlaylist = { scope.launch { playlistDao.removeByPath(entry.path) } }
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = PlayerBg, headlineColor = TextDark),
                                    modifier = Modifier.clickable {
                                        onPlayCustom(customPlaylistEntries.indexOf(entry))
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(playlist, key = { it.path }) { entity: FileEntity ->
                            val inPlaylist = customPlaylistPaths.contains(entity.path)
                            ListItem(
                                leadingContent = { SongThumbnail(entity.path) },
                                headlineContent = { SongRowTitle(entity.path, entity.name) },
                                trailingContent = {
                                    SongOptionsMenu(
                                        path = entity.path,
                                        name = entity.name,
                                        sizeBytes = entity.sizeBytes,
                                        inPlaylist = inPlaylist,
                                        onToggleInPlaylist = {
                                            scope.launch {
                                                if (inPlaylist) playlistDao.removeByPath(entity.path)
                                                else playlistDao.addToEnd(entity.path)
                                            }
                                        }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = PlayerBg, headlineColor = TextDark),
                                modifier = Modifier.clickable {
                                    onPlayAll(playlist.indexOf(entity))
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        PlayerControlBar(onTogglePlay, onPrev, onNext, isPlaying)
        Spacer(Modifier.height(8.dp))
    }
}

private val titleCache = mutableMapOf<String, String>()

private suspend fun resolveTitle(path: String, fallbackName: String): String {
    titleCache[path]?.let { return it }
    val resolved = withContext(Dispatchers.IO) { Id3UsltReader.readTitle(path) }
        ?.takeIf { it.isNotBlank() }
        ?: cleanTitle(fallbackName)
    titleCache[path] = resolved
    return resolved
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}

@Composable
private fun SongThumbnail(path: String) {
    val art by produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = loadAlbumArt(path)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PlayerAccentCyan.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(
                bitmap = art!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = PlayerAccentCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SongRowTitle(path: String, fallbackName: String) {
    val title by produceState(initialValue = cleanTitle(fallbackName), key1 = path) {
        value = resolveTitle(path, fallbackName)
    }
    Text(title, maxLines = 1, color = TextDark)
}

@Composable
private fun SongOptionsMenu(
    path: String,
    name: String,
    sizeBytes: Long,
    inPlaylist: Boolean,
    onToggleInPlaylist: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Opsi lagu", tint = TextDark.copy(alpha = 0.5f))
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(PlayerSurface)
        ) {
            DropdownMenuItem(
                text = { Text("Info lagu", color = TextDark) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = { menuExpanded = false; showInfoDialog = true }
            )
            DropdownMenuItem(
                text = { Text(if (inPlaylist) "Hapus dari Playlist" else "Tambahkan ke Playlist", color = TextDark) },
                leadingIcon = {
                    Icon(if (inPlaylist) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                },
                onClick = { menuExpanded = false; onToggleInPlaylist() }
            )
            DropdownMenuItem(
                text = { Text("Bagikan", color = TextDark) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    try {
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(path))
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Bagikan lagu"))
                    } catch (e: Exception) {
                    }
                }
            )
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = PlayerSurface,
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Tutup", color = TextDark) }
            },
            title = { Text("Info Lagu", color = TextDark) },
            text = {
                Column {
                    Text("Nama file: " + name, color = TextDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Ukuran: " + formatFileSize(sizeBytes), color = TextDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Path: " + path, color = TextDark)
                }
            }
        )
    }
}
