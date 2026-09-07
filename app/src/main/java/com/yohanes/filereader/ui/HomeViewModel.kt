package com.yohanes.filereader.ui

import android.app.Application
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.yohanes.filereader.data.AppDatabase
import com.yohanes.filereader.data.FileEntity
import com.yohanes.filereader.data.FavoritesStore
import com.yohanes.filereader.data.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOption { NAME_AZ, DATE_NEWEST, SIZE_LARGEST }

val CATEGORY_LIST = listOf("PDF", "Gambar", "Excel", "Video", "Audio", "Teks/Kode", "Favorit")

sealed class GalleryItem {
    data class Header(val label: String) : GalleryItem()
    data class Photo(val file: FileEntity) : GalleryItem()
}

private fun monthLabelOf(epochMillis: Long): String {
    val date = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("id", "ID")))
        .replaceFirstChar { it.uppercase() }
}

data class StorageInfo(val totalBytes: Long, val usedBytes: Long, val freeBytes: Long)

fun getStorageInfo(): StorageInfo {
    return try {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        StorageInfo(total, total - free, free)
    } catch (e: Exception) {
        StorageInfo(0, 0, 0)
    }
}

fun categoryOf(extension: String): String {
    return when (extension.lowercase()) {
        "pdf" -> "PDF"
        "jpg", "jpeg", "png", "webp", "gif" -> "Gambar"
        "xlsx" -> "Excel"
        "mp4", "mkv", "webm", "3gp", "avi", "mov" -> "Video"
        "mp3", "wav", "m4a", "ogg", "flac", "aac" -> "Audio"
        else -> "Teks/Kode"
    }
}

fun categoryEmojiExtra(category: String): String? {
    return if (category == "Favorit") "\u2B50" else null
}

fun categoryEmoji(category: String): String {
    return when (category) {
        "PDF" -> "\uD83D\uDCC4"
        "Gambar" -> "\uD83D\uDDBC\uFE0F"
        "Excel" -> "\uD83D\uDCCA"
        "Video" -> "\uD83C\uDFA5"
        "Audio" -> "\uD83C\uDFB5"
        "Favorit" -> "\u2B50"
        else -> "\uD83D\uDCDD"
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.fileDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    val storageInfo: StorageInfo = getStorageInfo()

    val categoryCounts: StateFlow<Map<String, Int>> = combine(
        dao.getAll(),
        FavoritesStore.favorites
    ) { all, favs ->
        val base = all.groupingBy { categoryOf(it.extension) }.eachCount().toMutableMap()
        base["Favorit"] = all.count { favs.contains(it.path) }
        base
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val files: StateFlow<List<FileEntity>> = combine(
        dao.getAll(),
        _searchQuery,
        _selectedCategory,
        _sortOption,
        FavoritesStore.favorites
    ) { all, query, category, sort, favs ->
        val filtered = all.filter { file ->
            val matchesCategory = when (category) {
                null -> true
                "Favorit" -> favs.contains(file.path)
                else -> categoryOf(file.extension) == category
            }
            (query.isBlank() || file.name.contains(query, ignoreCase = true)) && matchesCategory
        }
        when (sort) {
            SortOption.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
            SortOption.DATE_NEWEST -> filtered.sortedByDescending { it.lastModified }
            SortOption.SIZE_LARGEST -> filtered.sortedByDescending { it.sizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imagesPaged: Flow<PagingData<GalleryItem>> = Pager(
        config = PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false)
    ) {
        dao.getImagesPaged()
    }.flow
        .map { pagingData ->
            pagingData
                .map { GalleryItem.Photo(it) as GalleryItem }
                .insertSeparators { before, after ->
                    val afterPhoto = after as? GalleryItem.Photo ?: return@insertSeparators null
                    val afterLabel = monthLabelOf(afterPhoto.file.lastModified)
                    val beforeLabel = (before as? GalleryItem.Photo)?.let { monthLabelOf(it.file.lastModified) }
                    if (beforeLabel != afterLabel) GalleryItem.Header(afterLabel) else null
                }
        }
        .cachedIn(viewModelScope)

    val videos: StateFlow<List<FileEntity>> = dao.getVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val scanPrefs = application.getSharedPreferences("home_scan_prefs", android.content.Context.MODE_PRIVATE)

    init {
        FavoritesStore.init(application)
        val lastScan = scanPrefs.getLong(KEY_LAST_SCAN, 0L)
        val elapsed = System.currentTimeMillis() - lastScan
        if (elapsed > SCAN_INTERVAL_MS) {
            refreshScan()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
        _searchQuery.value = ""
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    fun refreshScan() {
        viewModelScope.launch {
            _isScanning.value = true
            val results = withContext(Dispatchers.IO) {
                FileScanner.scanAll()
            }
            withContext(Dispatchers.IO) {
                dao.syncAll(results)
            }
            scanPrefs.edit().putLong(KEY_LAST_SCAN, System.currentTimeMillis()).apply()
            _isScanning.value = false
        }
    }

    companion object {
        private const val KEY_LAST_SCAN = "last_scan_timestamp"
        private const val SCAN_INTERVAL_MS = 10 * 60 * 1000L
    }
}
