package com.yohanes.filereader

import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.yohanes.filereader.data.AppDatabase
import com.yohanes.filereader.data.FileEntity
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Pemutar video pakai ExoPlayer/Media3. Kontrol custom minimal: play/pause + seek bar tebal ala Google.
 *
 * Gesture:
 * - Tap sekali -> tampil/sembunyikan kontrol
 * - Swipe cepat lalu lepas (fling) -> ganti video (urutan terbaru)
 * - Tahan sebentar lalu geser dikit (lambat) -> percepat 2x selama ditahan
 *
 * Fullscreen mengikuti rotasi sistem. Toggle Terbaru/Folder sengaja dihapus dari sini,
 * rencananya dipindah ke halaman grid Video (kerja bareng hz11) supaya player tetap bersih.
 * Belum ada: playlist manual/autoplay, kontrol notification, subtitle - menunggu keputusan user.
 */
@Composable
fun VideoPlayerScreen(uri: Uri, displayName: String, onExit: () -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPath by remember { mutableStateOf(uri.path ?: uri.toString()) }
    var currentName by remember { mutableStateOf(displayName) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(1L) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    val dao = remember { AppDatabase.getInstance(context).fileDao() }
    val allVideos by dao.getVideos().collectAsState(initial = emptyList<FileEntity>())

    val currentIndex by remember(allVideos, currentPath) {
        derivedStateOf {
            val idx = allVideos.indexOfFirst { it.path == currentPath }
            if (idx == -1) 0 else idx
        }
    }

    fun playAt(index: Int) {
        if (allVideos.isEmpty()) return
        val safe = index.coerceIn(0, allVideos.size - 1)
        val entity = allVideos[safe]
        if (entity.path == currentPath) return
        currentPath = entity.path
        currentName = entity.name
        isBuffering = true
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(entity.path))))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    val currentIndexState = rememberUpdatedState(currentIndex)

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(1L)
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isDraggingSlider) {
                positionMs = exoPlayer.currentPosition
            }
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    BackHandler(enabled = true) { onExit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop
                var downX = 0f
                var downY = 0f
                var downTime = 0L
                var isHolding = false

                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        controlsVisible = !controlsVisible
                        return true
                    }
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (isHolding) return false
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
                    useController = false
                    setOnTouchListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.x
                                downY = event.y
                                downTime = event.eventTime
                                isHolding = false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = event.x - downX
                                val dy = event.y - downY
                                val elapsed = event.eventTime - downTime
                                if (!isHolding &&
                                    elapsed > 180 &&
                                    kotlin.math.abs(dx) > touchSlop &&
                                    kotlin.math.abs(dx) > kotlin.math.abs(dy)
                                ) {
                                    isHolding = true
                                    exoPlayer.playbackParameters = PlaybackParameters(2f)
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                if (isHolding) {
                                    exoPlayer.playbackParameters = PlaybackParameters.DEFAULT
                                    isHolding = false
                                }
                            }
                        }
                        gestureDetector.onTouchEvent(event)
                        true
                    }
                }
            }
        )

        if (isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Surface(
                modifier = Modifier.padding(12.dp),
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
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            CustomSeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                isDragging = isDraggingSlider,
                onDragStart = { isDraggingSlider = true },
                onDrag = { positionMs = it },
                onDragEnd = {
                    exoPlayer.seekTo(positionMs)
                    isDraggingSlider = false
                }
            )
        }
    }
}

@Composable
private fun CustomSeekBar(
    positionMs: Long,
    durationMs: Long,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit
) {
    var widthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isDragging) {
            Text(
                text = formatTime(positionMs) + " / " + formatTime(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 6.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onGloballyPositioned { widthPx = it.size.width.toFloat() }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            onDragStart()
                            if (widthPx > 0) {
                                val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                                onDrag((frac * durationMs).toLong())
                            }
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onHorizontalDrag = { change, _ ->
                            if (widthPx > 0) {
                                val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                                onDrag((frac * durationMs).toLong())
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .align(Alignment.CenterStart)
                    .background(Color.White, RoundedCornerShape(3.dp))
            )
            val thumbOffsetDp = with(density) { (widthPx * fraction).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = (thumbOffsetDp - 8.dp).let { if (it < 0.dp) 0.dp else it })
                    .size(16.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
