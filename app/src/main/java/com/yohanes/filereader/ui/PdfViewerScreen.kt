package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val RENDER_SCALE = 2f

@Composable
fun PdfViewerScreen(uri: Uri, displayName: String) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    val prefs = remember { context.getSharedPreferences("pdf_progress", Context.MODE_PRIVATE) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = prefs.getInt(displayName, 0)
    )

    LaunchedEffect(listState.firstVisibleItemIndex) {
        prefs.edit().putInt(displayName, listState.firstVisibleItemIndex).apply()
    }

    DisposableEffect(uri) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val renderer = pfd?.let { PdfRenderer(it) }
        pageCount = renderer?.pageCount ?: 0
        onDispose {
            renderer?.close()
            pfd?.close()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayName, maxLines = 1, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        }
        Text(
            "Cubit untuk memperbesar - ketuk 2x untuk reset",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        if (pageCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(pageCount) { index ->
                    PdfPage(uri = uri, pageIndex = index)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PdfPage(uri: Uri, pageIndex: Int) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    var zoom by remember(pageIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(pageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(pageIndex) { mutableFloatStateOf(0f) }

    LaunchedEffect(pageIndex) {
        bitmap = renderSinglePage(context, uri, pageIndex, RENDER_SCALE)
    }

    val bmp = bitmap
    if (bmp != null) {
        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(pageIndex) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        val newZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                        zoom = newZoom
                        if (zoom > 1f) {
                            val maxOffset = (zoom - 1f) * 600f
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(pageIndex) {
                    detectTapGestures(onDoubleTap = {
                        zoom = 1f
                        offsetX = 0f
                        offsetY = 0f
                    })
                }
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Halaman ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        }
    } else {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
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
