package com.yohanes.filereader

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.yohanes.filereader.ui.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val PlayerBg = Color(0xFF2A2A2E)
private val PlayerSurface = Color(0xFF303035)
private val PlayerAccent = Color(0xFFD6D6D6)

/**
 * Efek "timbul lembut" ala neumorphism, versi sederhana yang aman dibangun
 * (shadow standar Compose + garis tepi gradasi, BUKAN blur ganda manual via
 * Canvas native yang lebih rawan gagal render di sebagian device/versi
 * Android). Dipakai untuk cover album, tombol kontrol, dan bar pil.
 */
private fun Modifier.softRaised(shape: Shape, baseColor: Color): Modifier = this
    .shadow(elevation = 10.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black, clip = false)
    .background(baseColor, shape)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.25f)
            )
        ),
        shape = shape
    )

private data class TrackMeta(val art: Bitmap?, val artist: String?)

private val trackMetaCache = mutableMapOf<String, TrackMeta>()

private suspend fun loadTrackMeta(path: String): TrackMeta {
    trackMetaCache[path]?.let { return it }
    return withContext(Dispatchers.IO) {
        val meta = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val artBytes = retriever.embeddedPicture
            retriever.release()
            val art = artBytes?.let { bytes ->
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
                var inSampleSize = 1
                val targetPx = 480
                while (boundsOptions.outWidth / inSampleSize > targetPx) inSampleSize *= 2
                val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            }
            TrackMeta(art, artist)
        } catch (e: Exception) {
            TrackMeta(null, null)
        }
        trackMetaCache[path] = meta
        meta
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
    var trackMeta by remember { mutableStateOf(TrackMeta(null, null)) }

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
        trackMeta = if (currentMediaId.isNotBlank()) loadTrackMeta(currentMediaId) else TrackMeta(null, null)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(PlayerBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            Modifier
                .size(280.dp)
                .softRaised(CircleShape, PlayerSurface),
            contentAlignment = Alignment.Center
        ) {
            val art = trackMeta.art
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = "Sampul album",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("\u266A", fontSize = 64.sp, color = Color.White.copy(alpha = 0.25f))
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            currentTitle.ifBlank { "Memuat..." },
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        trackMeta.artist?.let { artist ->
            Spacer(Modifier.height(4.dp))
            Text(
                artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }

        Spacer(Modifier.weight(1f))

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
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(28.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .softRaised(RoundedCornerShape(50), PlayerSurface)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                Toast.makeText(context, "Playlist segera hadir", Toast.LENGTH_SHORT).show()
            }) {
                Text("\u2630", fontSize = 22.sp, color = Color.White.copy(alpha = 0.7f))
            }

            IconButton(
                onClick = { controller?.seekToPrevious() },
                modifier = Modifier.size(52.dp).softRaised(CircleShape, PlayerSurface)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = Color.White, modifier = Modifier.size(26.dp))
            }

            IconButton(
                onClick = { if (isPlaying) controller?.pause() else controller?.play() },
                modifier = Modifier.size(70.dp).softRaised(CircleShape, PlayerAccent)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = { controller?.seekToNext() },
                modifier = Modifier.size(52.dp).softRaised(CircleShape, PlayerSurface)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
