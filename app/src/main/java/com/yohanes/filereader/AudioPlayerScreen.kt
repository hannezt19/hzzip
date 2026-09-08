package com.yohanes.filereader

import android.content.ComponentName
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
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
import com.yohanes.filereader.data.PlaylistDao
import com.yohanes.filereader.data.PlaylistEntity
import com.yohanes.filereader.ui.SortOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val mediaItems = customPlaylistEntries.mapNotNull { entry ->
            val fe = fileByPath[entry.path] ?: return@mapNotNull null
            MediaItem.Builder()
                .setUri(Uri.fromFile(File(fe.path)))
                .setMediaId(fe.path)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(fe.name).build())
                .build()
        }
        if (mediaItems.isEmpty()) return
        controller?.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
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
                    onOpenPlaylist = { scope.launch { pagerState.animateScrollToPage(0) } },
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
                    onNext = { controller?.seekToNext() },
                    onOpenPlaylist = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
                2 -> LyricsPage(
                    currentPosition = currentPosition,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeekChange = { isUserSeeking = true; currentPosition = it.toLong() },
                    onSeekFinished = { controller?.seekTo(currentPosition); isUserSeeking = false },
                    onTogglePlay = { if (isPlaying) controller?.pause() else controller?.play() },
                    onPrev = { controller?.seekToPrevious() },
                    onNext = { controller?.seekToNext() },
                    onOpenPlaylist = { scope.launch { pagerState.animateScrollToPage(0) } }
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
    onOpenPlaylist: () -> Unit,
    isPlaying: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .softRaised(RoundedCornerShape(50), PlayerSurface)
            .padding(vertical = 12.dp)
    ) {
        IconButton(
            onClick = onOpenPlaylist,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Playlist", tint = TextDark.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
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
    onNext: () -> Unit,
    onOpenPlaylist: () -> Unit
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

        PlayerControlBar(onTogglePlay, onPrev, onNext, onOpenPlaylist, isPlaying)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LyricsPage(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenPlaylist: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Lirik akan hadir di tahap berikutnya",
                color = TextDark.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SeekBarSection(currentPosition, duration, onSeekChange, onSeekFinished)
        Spacer(Modifier.height(20.dp))
        PlayerControlBar(onTogglePlay, onPrev, onNext, onOpenPlaylist, isPlaying)
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
    onOpenPlaylist: () -> Unit,
    onPlayCustom: (Int) -> Unit,
    onPlayAll: (Int) -> Unit
) {
    var sheetTab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        TabRow(selectedTabIndex = sheetTab) {
            Tab(selected = sheetTab == 0, onClick = { sheetTab = 0 }, text = { Text("Playlist") })
            Tab(selected = sheetTab == 1, onClick = { sheetTab = 1 }, text = { Text("Semua Audio") })
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
                                val displayName = cleanTitle(fe?.name ?: entry.path.substringAfterLast("/"))
                                ListItem(
                                    headlineContent = { Text(displayName, maxLines = 1, color = TextDark) },
                                    trailingContent = {
                                        IconButton(onClick = { scope.launch { playlistDao.removeByPath(entry.path) } }) {
                                            Icon(Icons.Default.Close, contentDescription = "Hapus dari playlist", tint = TextDark.copy(alpha = 0.5f))
                                        }
                                    },
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
                                headlineContent = { Text(cleanTitle(entity.name), maxLines = 1, color = TextDark) },
                                trailingContent = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (inPlaylist) playlistDao.removeByPath(entity.path)
                                            else playlistDao.addToEnd(entity.path)
                                        }
                                    }) {
                                        Icon(
                                            if (inPlaylist) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = if (inPlaylist) "Sudah di playlist" else "Tambah ke playlist",
                                            tint = if (inPlaylist) PlayerAccentCyan else TextDark.copy(alpha = 0.5f)
                                        )
                                    }
                                },
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
        PlayerControlBar(onTogglePlay, onPrev, onNext, onOpenPlaylist, isPlaying)
        Spacer(Modifier.height(8.dp))
    }
}
