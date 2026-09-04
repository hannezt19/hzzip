package com.yohanes.filereader.data

import android.graphics.Bitmap
import android.util.LruCache

object PageBitmapCache {
    private val maxCacheBytes = (Runtime.getRuntime().maxMemory() / 8).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private fun key(uriString: String, pageIndex: Int) = "$uriString|$pageIndex"

    fun get(uriString: String, pageIndex: Int): Bitmap? {
        return cache.get(key(uriString, pageIndex))
    }

    fun put(uriString: String, pageIndex: Int, bitmap: Bitmap) {
        cache.put(key(uriString, pageIndex), bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}
