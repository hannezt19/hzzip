package com.yohanes.filereader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FavoritesStore {
    private const val PREFS_NAME = "favorites"
    private const val KEY_PATHS = "paths"
    private var prefs: android.content.SharedPreferences? = null
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _favorites.value = p.getStringSet(KEY_PATHS, emptySet()) ?: emptySet()
        }
    }

    fun isFavorite(path: String): Boolean = _favorites.value.contains(path)

    fun toggle(path: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _favorites.value = current
        prefs?.edit()?.putStringSet(KEY_PATHS, current)?.apply()
    }
}
