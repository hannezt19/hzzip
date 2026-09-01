package com.yohanes.filereader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeStore {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"
    private var prefs: android.content.SharedPreferences? = null
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _isDarkMode.value = p.getBoolean(KEY_DARK_MODE, false)
        }
    }

    fun toggle() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, newValue)?.apply()
    }
}
