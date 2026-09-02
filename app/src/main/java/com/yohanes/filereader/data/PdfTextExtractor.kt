package com.yohanes.filereader.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfTextExtractor {
    @Volatile
    private var initialized = false

    private fun ensureInit(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                PDFBoxResourceLoader.init(context.applicationContext)
                initialized = true
            }
        }
    }

    fun extractPageText(context: Context, uri: Uri, pageIndex: Int): String? {
        ensureInit(context)
        var document: PDDocument? = null
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            document = inputStream.use { stream -> PDDocument.load(stream) }
            val doc = document ?: return null
            if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return null
            val stripper = PDFTextStripper()
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(doc).trim()
        } catch (e: Exception) {
            null
        } finally {
            document?.close()
        }
    }
}
