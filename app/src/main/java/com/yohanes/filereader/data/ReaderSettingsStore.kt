package com.yohanes.filereader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class BacaWarnaLatar(val bg: Long, val teks: Long, val label: String) {
    GELAP(0xFF121212, 0xFFFFFFFF, "Gelap"),
    SEPIA(0xFFF4ECD8, 0xFF3B2F2F, "Sepia"),
    TERANG(0xFFFFFFFF, 0xFF000000, "Terang"),
    ABU(0xFF2B2B2B, 0xFFE0E0E0, "Abu-abu")
}

enum class NavigasiMode(val label: String) {
    SWIPE("Swipe"),
    SCROLL("Scroll")
}

data class ReaderSettings(
    val textSizeSp: Float = 16f,
    val contrast: Float = 1f,
    val warnaLatar: BacaWarnaLatar = BacaWarnaLatar.GELAP,
    val navMode: NavigasiMode = NavigasiMode.SWIPE,
    val ttsVolume: Float = 1f,
    val ttsPitch: Float = 1f,
    val ttsSpeed: Float = 1f
)

object ReaderSettingsStore {
    private const val PREF_NAME = "reader_settings"
    private const val KEY_TEXT_SIZE = "text_size_sp"
    private const val KEY_CONTRAST = "contrast"
    private const val KEY_WARNA_LATAR = "warna_latar"
    private const val KEY_NAV_MODE = "nav_mode"
    private const val KEY_TTS_VOLUME = "tts_volume"
    private const val KEY_TTS_PITCH = "tts_pitch"
    private const val KEY_TTS_SPEED = "tts_speed"

    private val _settings = MutableStateFlow(ReaderSettings())
    val settings: StateFlow<ReaderSettings> = _settings

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val textSize = prefs.getFloat(KEY_TEXT_SIZE, 16f)
            val contrast = prefs.getFloat(KEY_CONTRAST, 1f)
            val warnaName = prefs.getString(KEY_WARNA_LATAR, BacaWarnaLatar.GELAP.name)
            val warna = try {
                BacaWarnaLatar.valueOf(warnaName ?: BacaWarnaLatar.GELAP.name)
            } catch (e: Exception) {
                BacaWarnaLatar.GELAP
            }
            val navName = prefs.getString(KEY_NAV_MODE, NavigasiMode.SWIPE.name)
            val nav = try {
                NavigasiMode.valueOf(navName ?: NavigasiMode.SWIPE.name)
            } catch (e: Exception) {
                NavigasiMode.SWIPE
            }
            val ttsVolume = prefs.getFloat(KEY_TTS_VOLUME, 1f)
            val ttsPitch = prefs.getFloat(KEY_TTS_PITCH, 1f)
            val ttsSpeed = prefs.getFloat(KEY_TTS_SPEED, 1f)
            _settings.value = ReaderSettings(textSize, contrast, warna, nav, ttsVolume, ttsPitch, ttsSpeed)
            loaded = true
        }
    }

    fun setTextSize(context: Context, sizeSp: Float) {
        val clamped = sizeSp.coerceIn(12f, 28f)
        _settings.value = _settings.value.copy(textSizeSp = clamped)
        persist(context)
    }

    fun setContrast(context: Context, value: Float) {
        val clamped = value.coerceIn(0.5f, 2f)
        _settings.value = _settings.value.copy(contrast = clamped)
        persist(context)
    }

    fun setWarnaLatar(context: Context, warna: BacaWarnaLatar) {
        _settings.value = _settings.value.copy(warnaLatar = warna)
        persist(context)
    }

    fun setNavMode(context: Context, mode: NavigasiMode) {
        _settings.value = _settings.value.copy(navMode = mode)
        persist(context)
    }

    fun setTtsVolume(context: Context, value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _settings.value = _settings.value.copy(ttsVolume = clamped)
        persist(context)
    }

    fun setTtsPitch(context: Context, value: Float) {
        val clamped = value.coerceIn(0.5f, 2f)
        _settings.value = _settings.value.copy(ttsPitch = clamped)
        persist(context)
    }

    fun setTtsSpeed(context: Context, value: Float) {
        val clamped = value.coerceIn(0.5f, 2f)
        _settings.value = _settings.value.copy(ttsSpeed = clamped)
        persist(context)
    }

    private fun persist(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val s = _settings.value
        prefs.edit()
            .putFloat(KEY_TEXT_SIZE, s.textSizeSp)
            .putFloat(KEY_CONTRAST, s.contrast)
            .putString(KEY_WARNA_LATAR, s.warnaLatar.name)
            .putString(KEY_NAV_MODE, s.navMode.name)
            .putFloat(KEY_TTS_VOLUME, s.ttsVolume)
            .putFloat(KEY_TTS_PITCH, s.ttsPitch)
            .putFloat(KEY_TTS_SPEED, s.ttsSpeed)
            .apply()
    }
}
