package com.yohanes.filereader.data

import android.content.Context
import android.net.Uri

object PdfRenderSessionCache {
    private var currentUri: String? = null
    private var currentSession: PdfRenderSession? = null

    @Synchronized
    fun getOrCreate(context: Context, uri: Uri): PdfRenderSession {
        val uriString = uri.toString()
        val existing = currentSession
        if (existing != null && currentUri == uriString) return existing
        existing?.close()
        val session = PdfRenderSession(context, uri)
        currentSession = session
        currentUri = uriString
        return session
    }

    @Synchronized
    fun closeIfMatches(uri: Uri) {
        if (currentUri == uri.toString()) {
            currentSession?.close()
            currentSession = null
            currentUri = null
        }
    }
}
