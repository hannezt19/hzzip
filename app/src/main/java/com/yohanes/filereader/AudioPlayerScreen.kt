package com.yohanes.filereader

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.FileEntity
import com.yohanes.filereader.ui.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val PlayerBg = Color(0xFF121212)
private val PlayerAccent = Color(0xFF4DD0E1)

private val albumArtCache = android.util.LruCache<String, Bitmap>(20)
private val noArtCache = mutableSetOf<String>()

private suspend fun loadAlbumArt(path: String): Bitmap? {
    albumArtCache.get(path)?.let { return it }
    if (noArtCache.contains(path)) return null
    return withContext(Dispatchers.IO) {
        val bmp = try {
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
        if (bmp != null) albumArtCache.put(path, bmp) else noArtCache.add(path)
        bmp
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun AudioPlayerScreen(filePath: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    val favorites by FavoritesStore.favorites.collectAsState()
    val isFav = favorites.contains(currentMediaId)

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

    Column(
        Modifier
            .fillMaxSize()
            .background(PlayerBg)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Playlist (${playlist.size} lagu)",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Text("Urutkan", color = PlayerAccent)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text("A-Z") }, onClick = {
                        sortOption = SortOption.NAME_AZ
                        showSortMenu = false
                        scope.launch { loadAndPlay(sortOption) }
                    })
                    DropdownMenuItem(text = { Text("Terbaru - Terlama") }, onClick = {
                        sortOption = SortOption.DATE_NEWEST
                        showSortMenu = false
                        scope.launch { loadAndPlay(sortOption) }
                    })
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            val art = albumArt
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = "Sampul album",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("\u266A", fontSize = 72.sp, color = Color.White.copy(alpha = 0.3f))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            currentTitle.ifBlank { "Memuat..." },
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        IconButton(
            onClick = { if (currentMediaId.isNotBlank()) FavoritesStore.toggle(currentMediaId) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Favorit",
                tint = if (isFav) Color(0xFFFFC107) else Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(Modifier.height(12.dp))

        val safeDuration = duration.coerceAtLeast(1L)
        Slider(
            value = currentPosition.coerceIn(0L, safeDuration).toFloat(),
            onValueChange = {
                isUserSeeking = true
                currentPosition = it.toLong()
            },
            onValueChangeFinished = {
                controller?.seekTo(currentPosition)
                isUserSeeking = false
            },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = PlayerAccent,
                activeTrackColor = PlayerAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { controller?.seekToPrevious() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(20.dp))
            IconButton(
                onClick = { if (isPlaying) controller?.pause() else controller?.play() },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PlayerAccent)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.width(20.dp))
            IconButton(onClick = { controller?.seekToNext() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(playlist) { index, entity ->
                val isCurrent = entity.path == currentMediaId
                Text(
                    entity.name,
                    style = if (isCurrent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) PlayerAccent else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            controller?.seekTo(index, 0L)
                            controller?.play()
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}
