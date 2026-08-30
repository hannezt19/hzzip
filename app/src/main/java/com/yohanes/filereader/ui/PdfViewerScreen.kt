package com.yohanes.filereader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Render PDF SATU HALAMAN dalam satu waktu (lazy), bukan semua halaman
 * sekaligus ke memori — penting untuk device RAM kecil (mis. Redmi C2 3GB).
 * Skala render bisa diperbesar/perkecil lewat tombol zoom.
 */
@Composable
fun PdfViewerScreen(uri: Uri, displayName: String) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1.5f) }
    val prefs = remember { context.getSharedPreferences("pdf_progress", Context.MODE_PRIVATE) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = prefs.getInt(displayName, 0)
    )

    // Simpan posisi terakhir setiap kali halaman berubah
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
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayName, maxLines = 1, style = MaterialTheme.typography.titleSmall)
            Row {
                IconButtonText("A-") { if (scale > 0.75f) scale -= 0.25f }
                Spacer(Modifier.width(8.dp))
                IconButtonText("A+") { if (scale < 3f) scale += 0.25f }
            }
        }

        if (pageCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(pageCount) { index ->
                    PdfPage(uri = uri, pageIndex = index, scale = scale)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun IconButtonText(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Text(label) }
}

/**
 * Membuka file descriptor sendiri per halaman supaya bitmap halaman
 * sebelumnya bisa di-garbage-collect saat sudah di-scroll lewat.
 */
@Composable
private fun PdfPage(uri: Uri, pageIndex: Int, scale: Float) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex, scale) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex, scale) {
        bitmap = renderSinglePage(context, uri, pageIndex, scale)
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Halaman ${pageIndex + 1}",
            modifier = Modifier.fillMaxWidth()
        )
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
