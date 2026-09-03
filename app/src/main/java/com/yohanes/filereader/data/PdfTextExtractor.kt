package com.yohanes.filereader.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
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

    fun extractMainImage(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        ensureInit(context)
        var document: PDDocument? = null
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            document = inputStream.use { stream -> PDDocument.load(stream) }
            val doc = document ?: return null
            if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return null
            val page = doc.getPage(pageIndex)
            val resources = page.resources ?: return null
            var mainImage: Bitmap? = null
            var mainArea = 0L
            for (name in resources.xObjectNames) {
                val xObject = resources.getXObject(name)
                if (xObject is PDImageXObject) {
                    val bmp = xObject.image
                    val area = bmp.width.toLong() * bmp.height.toLong()
                    if (area > mainArea) {
                        mainArea = area
                        mainImage = bmp
                    }
                }
            }
            mainImage
        } catch (e: Exception) {
            null
        } finally {
            document?.close()
        }
    }

    fun extractAllImages(context: Context, uri: Uri, pageIndex: Int): List<Bitmap> {
        ensureInit(context)
        var document: PDDocument? = null
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            document = inputStream.use { stream -> PDDocument.load(stream) }
            val doc = document ?: return emptyList()
            if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return emptyList()
            val page = doc.getPage(pageIndex)
            val resources = page.resources ?: return emptyList()
            val images = mutableListOf<Pair<Long, Bitmap>>()
            for (name in resources.xObjectNames) {
                val xObject = resources.getXObject(name)
                if (xObject is PDImageXObject) {
                    val bmp = xObject.image
                    val area = bmp.width.toLong() * bmp.height.toLong()
                    images.add(area to bmp)
                }
            }
            images.sortedByDescending { it.first }.map { it.second }
        } catch (e: Exception) {
            emptyList()
        } finally {
            document?.close()
        }
    }
}
