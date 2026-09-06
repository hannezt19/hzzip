package com.yohanes.filereader

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

enum class FileType {
    PDF, JSON, HTML, JS, TEXT, IMAGE, XLSX, VIDEO, AUDIO, UNKNOWN
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
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif") -> return FileType.IMAGE
            name.endsWith(".xlsx") -> return FileType.XLSX
            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".3gp") || name.endsWith(".avi") || name.endsWith(".mov") -> return FileType.VIDEO
            name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".flac") || name.endsWith(".aac") -> return FileType.AUDIO
        }

        val mime = resolver.getType(uri)
        if (mime?.startsWith("image/") == true) return FileType.IMAGE
        if (mime?.startsWith("video/") == true) return FileType.VIDEO
        if (mime?.startsWith("audio/") == true) return FileType.AUDIO
        if (mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") return FileType.XLSX
        return when (mime) {
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
