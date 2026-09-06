package com.yohanes.filereader

import android.content.ComponentName
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yohanes.filereader.data.AppDatabase
import com.yohanes.filereader.data.FileEntity
import com.yohanes.filereader.ui.SortOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

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

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                currentTitle.ifBlank { "Memuat..." },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Text("Urutkan")
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

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { controller?.seekToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya")
            }
            IconButton(onClick = {
                if (isPlaying) controller?.pause() else controller?.play()
            }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar"
                )
            }
            IconButton(onClick = { controller?.seekToNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya")
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Playlist (${playlist.size} lagu)", style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(playlist) { index, entity ->
                val isCurrent = entity.path == currentMediaId
                Text(
                    entity.name,
                    style = if (isCurrent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
