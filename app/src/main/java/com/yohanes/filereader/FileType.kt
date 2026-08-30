package com.yohanes.filereader

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

enum class FileType {
    PDF, JSON, HTML, JS, TEXT, UNKNOWN
}

object FileTypeDetector {

    /**
     * Banyak file manager mengirim mime type yang kurang akurat (mis. text/plain
     * untuk semua file teks). Jadi kita cek dulu dari nama file (ekstensi),
     * baru fallback ke mime type kalau nama file tidak jelas.
     */
    fun detect(resolver: ContentResolver, uri: Uri): FileType {
        val name = queryDisplayName(resolver, uri)?.lowercase() ?: ""

        when {
            name.endsWith(".pdf") -> return FileType.PDF
            name.endsWith(".json") -> return FileType.JSON
            name.endsWith(".html") || name.endsWith(".htm") -> return FileType.HTML
            name.endsWith(".js") || name.endsWith(".mjs") -> return FileType.JS
            name.endsWith(".txt") || name.endsWith(".css") || name.endsWith(".xml") -> return FileType.TEXT
        }

        return when (resolver.getType(uri)) {
            "application/pdf" -> FileType.PDF
            "application/json", "text/json" -> FileType.JSON
            "text/html" -> FileType.HTML
            "text/javascript", "application/javascript", "application/x-javascript" -> FileType.JS
            "text/plain" -> FileType.TEXT
            else -> FileType.UNKNOWN
        }
    }

    fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        var result: String? = null
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) result = it.getString(idx)
            }
        }
        return result
    }
}
