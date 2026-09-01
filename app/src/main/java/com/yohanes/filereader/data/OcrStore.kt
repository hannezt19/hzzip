package com.yohanes.filereader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.io.File

private const val OCR_RENDER_SCALE = 1.5f

sealed class OcrStatus {
    object Idle : OcrStatus()
    data class Processing(val currentPage: Int, val totalPages: Int) : OcrStatus()
    object Done : OcrStatus()
    data class Error(val message: String) : OcrStatus()
}

object OcrStore {
    private val _status = MutableStateFlow<OcrStatus>(OcrStatus.Idle)
    val status: StateFlow<OcrStatus> = _status

    private fun cacheFile(context: Context, displayName: String): File {
        val dir = File(context.filesDir, "ocr_cache")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$displayName.txt")
    }

    fun hasCache(context: Context, displayName: String): Boolean {
        return cacheFile(context, displayName).exists()
    }

    fun readCache(context: Context, displayName: String): String {
        val file = cacheFile(context, displayName)
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

    suspend fun process(context: Context, uri: Uri, displayName: String) {
        _status.value = OcrStatus.Idle
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pageCount = try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            renderer = pfd?.let { PdfRenderer(it) }
            renderer?.pageCount ?: 0
        } catch (e: Exception) {
            0
        } finally {
            renderer?.close()
            pfd?.close()
        }

        if (pageCount == 0) {
            _status.value = OcrStatus.Error("Gagal membaca jumlah halaman PDF")
            return
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val fullText = StringBuilder()

        try {
            for (i in 0 until pageCount) {
                _status.value = OcrStatus.Processing(currentPage = i + 1, totalPages = pageCount)
                val bitmap = renderPageForOcr(context, uri, i)
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val result = recognizer.process(image).await()
                    fullText.append(result.text)
                    fullText.append("\n\n--- Halaman ${i + 1} ---\n\n")
                    bitmap.recycle()
                }
            }
            cacheFile(context, displayName).writeText(fullText.toString())
            _status.value = OcrStatus.Done
        } catch (e: Exception) {
            _status.value = OcrStatus.Error(e.message ?: "OCR gagal")
        }
    }
}
