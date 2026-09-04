package com.yohanes.filereader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PdfRenderSession(context: Context, uri: Uri) {
    private val mutex = Mutex()
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    val pageCount: Int

    init {
        pfd = context.applicationContext.contentResolver.openFileDescriptor(uri, "r")
        renderer = pfd?.let { PdfRenderer(it) }
        pageCount = renderer?.pageCount ?: 0
    }

    suspend fun renderPage(pageIndex: Int, scale: Float): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val r = renderer ?: return@withLock null
            if (pageIndex < 0 || pageIndex >= r.pageCount) return@withLock null
            try {
                val page = r.openPage(pageIndex)
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    fun close() {
        renderer?.close()
        pfd?.close()
        renderer = null
        pfd = null
    }
}
