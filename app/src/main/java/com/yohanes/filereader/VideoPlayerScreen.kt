package com.yohanes.filereader

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.yohanes.filereader.data.AppDatabase
import com.yohanes.filereader.data.FileEntity
import java.io.File

private enum class VideoSortMode { TERBARU, FOLDER }

/**
 * Pemutar video pakai ExoPlayer/Media3.
 * Fitur: play/pause, seek bar, fullscreen, loading indicator, swipe ganti video
 * (fling kiri/kanan), toggle urutan Terbaru/Folder.
 * Belum ada: playlist manual/autoplay, kontrol notification, subtitle - menunggu keputusan user.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(uri: Uri, displayName: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    var isFullscreen by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPath by remember { mutableStateOf(uri.path ?: uri.toString()) }
    var currentName by remember { mutableStateOf(displayName) }
    var mode by remember { mutableStateOf(VideoSortMode.TERBARU) }

    val dao = remember { AppDatabase.getInstance(context).fileDao() }
    val allVideos by dao.getVideos().collectAsState(initial = emptyList<FileEntity>())

    val displayList by remember(allVideos, mode, currentPath) {
        derivedStateOf {
            when (mode) {
                VideoSortMode.TERBARU -> allVideos
                VideoSortMode.FOLDER -> {
                    val folder = File(currentPath).parent
                    allVideos.filter { File(it.path).parent == folder }
                }
            }
        }
    }

    val currentIndex by remember(displayList, currentPath) {
        derivedStateOf {
            val idx = displayList.indexOfFirst { it.path == currentPath }
            if (idx == -1) 0 else idx
        }
    }

    fun playAt(index: Int) {
        if (displayList.isEmpty()) return
        val safe = index.coerceIn(0, displayList.size - 1)
        val entity = displayList[safe]
        if (entity.path == currentPath) return
        currentPath = entity.path
        currentName = entity.name
        isBuffering = true
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(entity.path))))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    val currentIndexState = rememberUpdatedState(currentIndex)
    val displayListState = rememberUpdatedState(displayList)

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(enabled = true) {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (kotlin.math.abs(velocityX) < kotlin.math.abs(velocityY)) return false
                        val threshold = 800f
                        if (velocityX < -threshold) {
                            playAt(currentIndexState.value + 1)
                            return true
                        } else if (velocityX > threshold) {
                            playAt(currentIndexState.value - 1)
                            return true
                        }
                        return false
                    }
                })

                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setFullscreenButtonClickListener { isFullActive ->
                        isFullscreen = isFullActive
                        activity?.requestedOrientation = if (isFullActive) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                    setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)
                        false
                    }
                }
            }
        )

        if (isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = currentName,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        AnimatedVisibility(
            visible = !isFullscreen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                SortChip(
                    label = "Terbaru",
                    icon = Icons.Filled.Schedule,
                    selected = mode == VideoSortMode.TERBARU,
                    onClick = { mode = VideoSortMode.TERBARU }
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
                SortChip(
                    label = "Folder",
                    icon = Icons.Filled.Folder,
                    selected = mode == VideoSortMode.FOLDER,
                    onClick = { mode = VideoSortMode.FOLDER }
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .then(Modifier),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
