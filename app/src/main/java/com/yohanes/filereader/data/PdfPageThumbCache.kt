package com.yohanes.filereader.data

import android.graphics.Bitmap
import android.util.LruCache

object PdfPageThumbCache {
    const val THUMB_SCALE = 0.25f

    private val maxCacheBytes = (Runtime.getRuntime().maxMemory() / 16).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private fun key(uriString: String, pageIndex: Int) = "$uriString|thumb|$pageIndex"

    fun get(uriString: String, pageIndex: Int): Bitmap? {
        return cache.get(key(uriString, pageIndex))
    }

    fun put(uriString: String, pageIndex: Int, bitmap: Bitmap) {
        cache.put(key(uriString, pageIndex), bitmap)
    }
}
