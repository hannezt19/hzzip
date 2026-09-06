package com.yohanes.filereader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object PdfThumbnailCache {
    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, "pdf_thumbs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun keyFor(path: String, lastModified: Long): String {
        val raw = "$path|$lastModified"
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun get(context: Context, path: String, lastModified: Long): Bitmap? {
        return try {
            val file = File(cacheDir(context), keyFor(path, lastModified) + ".png")
            if (!file.exists()) null else BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    fun put(context: Context, path: String, lastModified: Long, bitmap: Bitmap) {
        try {
            val file = File(cacheDir(context), keyFor(path, lastModified) + ".png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (e: Exception) {
            // gagal simpan cache bukan error fatal, abaikan saja
        }
    }
}
