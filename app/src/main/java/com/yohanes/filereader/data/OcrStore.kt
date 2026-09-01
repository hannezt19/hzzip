package com.yohanes.filereader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

private const val OCR_RENDER_SCALE = 1.5f
private const val OCR_PREFETCH_WINDOW = 10

object OcrStore {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private val processingKeys = mutableSetOf<String>()

    private val _readyKeys = MutableStateFlow<Set<String>>(emptySet())
    val readyKeys: StateFlow<Set<String>> = _readyKeys

    private fun keyOf(displayName: String, pageIndex: Int) = "$displayName|$pageIndex"

    private fun cacheDir(context: Context, displayName: String): File {
        val dir = File(context.filesDir, "ocr_cache/$displayName")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun pageCacheFile(context: Context, displayName: String, pageIndex: Int): File {
        return File(cacheDir(context, displayName), "page_$pageIndex.txt")
    }

    fun hasPageCache(context: Context, displayName: String, pageIndex: Int): Boolean {
        return pageCacheFile(context, displayName, pageIndex).exists()
    }

    fun readPageCache(context: Context, displayName: String, pageIndex: Int): String {
        val file = pageCacheFile(context, displayName, pageIndex)
        return if (file.exists()) file.readText() else ""
    }

    private fun renderPageForOcr(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            renderer = pfd?.let { PdfRenderer(it) }
            val page = renderer?.openPage(pageIndex) ?: return null
            val width = (page.width * OCR_RENDER_SCALE).toInt().coerceAtLeast(1)
            val height = (page.height * OCR_RENDER_SCALE).toInt().coerceAtLeast(1)
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

    private suspend fun processPage(context: Context, uri: Uri, displayName: String, pageIndex: Int) {
        val key = keyOf(displayName, pageIndex)
        if (hasPageCache(context, displayName, pageIndex)) {
            if (!_readyKeys.value.contains(key)) {
                _readyKeys.value = _readyKeys.value + key
            }
            return
        }
        if (processingKeys.contains(key)) return
        processingKeys.add(key)
        try {
            val bitmap = renderPageForOcr(context, uri, pageIndex)
            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                pageCacheFile(context, displayName, pageIndex).writeText(result.text)
                bitmap.recycle()
                _readyKeys.value = _readyKeys.value + key
            }
        } catch (e: Exception) {
            // dilewati, halaman ini akan dicoba lagi saat window OCR mencakupnya lagi
        } finally {
            processingKeys.remove(key)
        }
    }

    fun ensureWindow(context: Context, uri: Uri, displayName: String, pageCount: Int, currentPage: Int) {
        if (pageCount <= 0) return
        val targets = if (pageCount <= OCR_PREFETCH_WINDOW) {
            0 until pageCount
        } else {
            currentPage until (currentPage + OCR_PREFETCH_WINDOW).coerceAtMost(pageCount)
        }
        scope.launch {
            for (p in targets) {
                processPage(context, uri, displayName, p)
            }
        }
    }
}
