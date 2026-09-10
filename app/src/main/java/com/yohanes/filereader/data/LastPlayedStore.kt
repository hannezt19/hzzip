package com.yohanes.filereader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// yhs13: simpan path lagu terakhir diputar, supaya AudioPlayerScreen bisa
// lanjut dari situ (bukan mulai dari file pertama lagi) saat dibuka ulang.
object LastPlayedStore {
    private const val PREFS_NAME = "last_played"
    private const val KEY_PATH = "path"
    private var prefs: android.content.SharedPreferences? = null
    private val _lastPlayedPath = MutableStateFlow<String?>(null)
    val lastPlayedPath: StateFlow<String?> = _lastPlayedPath

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _lastPlayedPath.value = p.getString(KEY_PATH, null)
        }
    }

    fun setLastPlayed(path: String) {
        _lastPlayedPath.value = path
        prefs?.edit()?.putString(KEY_PATH, path)?.apply()
    }
}
