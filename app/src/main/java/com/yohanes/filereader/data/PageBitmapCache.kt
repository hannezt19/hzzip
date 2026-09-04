package com.yohanes.filereader.data

import android.graphics.Bitmap
import android.util.LruCache

object PageBitmapCache {
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && oldValue != newValue) {
                oldValue.recycle()
            }
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
