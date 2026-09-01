package com.yohanes.filereader.data

import android.os.Environment
import java.io.File

object FileScanner {
    private val SUPPORTED_EXTENSIONS = setOf(
        "pdf", "docx", "xlsx", "txt", "json", "html", "js", "css",
        "jpg", "jpeg", "png", "webp", "gif"
    )

    fun scanAll(): List<FileEntity> {
        val root = Environment.getExternalStorageDirectory()
        val results = mutableListOf<FileEntity>()
        scanDir(root, results)
        return results
    }

    private fun scanDir(dir: File, results: MutableList<FileEntity>) {
        val entries = dir.listFiles() ?: return
        for (entry in entries) {
            if (entry.isDirectory) {
                if (!entry.name.startsWith(".")) {
                    scanDir(entry, results)
                }
            } else {
                val ext = entry.extension.lowercase()
                if (ext in SUPPORTED_EXTENSIONS) {
                    results.add(
                        FileEntity(
                            path = entry.absolutePath,
                            name = entry.name,
                            extension = ext,
                            sizeBytes = entry.length(),
                            lastModified = entry.lastModified()
                        )
                    )
                }
            }
        }
    }
}
