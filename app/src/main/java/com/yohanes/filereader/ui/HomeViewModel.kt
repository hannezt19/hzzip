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

// "Favorit" sengaja tidak dimasukkan di sini - sudah ada menu Favorit terpisah di drawer
// (lihat onFavorit di MainActivity.kt), jadi kartu di grid Beranda dihilangkan supaya tidak dobel.
val CATEGORY_LIST = listOf("PDF", "Gambar", "Excel", "Video", "Audio", "Teks/Kode")

enum class VideoGalleryMode { TERBARU, FOLDER }

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

    // Versi ringan (bukan paging) khusus untuk pengelompokan folder di mode Folder Gambar.
    // Mode Terbaru Gambar tetap pakai imagesPaged (Paging3) yang sudah ada, tidak diubah.
    val images: StateFlow<List<FileEntity>> = dao.getImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _imageGalleryMode = MutableStateFlow(VideoGalleryMode.TERBARU)
    val imageGalleryMode: StateFlow<VideoGalleryMode> = _imageGalleryMode
    fun setImageGalleryMode(mode: VideoGalleryMode) {
        _imageGalleryMode.value = mode
        if (mode == VideoGalleryMode.TERBARU) _selectedImageFolderPath.value = null
    }

    private val _selectedImageFolderPath = MutableStateFlow<String?>(null)
    val selectedImageFolderPath: StateFlow<String?> = _selectedImageFolderPath
    fun selectImageFolder(path: String?) { _selectedImageFolderPath.value = path }

    // Terbaru/Folder - dinaikkan ke sini (bukan remember lokal di VideoGalleryScreen)
    // supaya tidak ter-reset saat composable grid dilepas total ketika video diputar.
    private val _videoGalleryMode = MutableStateFlow(VideoGalleryMode.TERBARU)
    val videoGalleryMode: StateFlow<VideoGalleryMode> = _videoGalleryMode
    fun setVideoGalleryMode(mode: VideoGalleryMode) {
        _videoGalleryMode.value = mode
        if (mode == VideoGalleryMode.TERBARU) _selectedVideoFolderPath.value = null
    }

    private val _selectedVideoFolderPath = MutableStateFlow<String?>(null)
    val selectedVideoFolderPath: StateFlow<String?> = _selectedVideoFolderPath
    fun selectVideoFolder(path: String?) { _selectedVideoFolderPath.value = path }

    // Toggle Terbaru/Folder untuk kategori list biasa (PDF, Excel, Teks/Kode, Favorit).
    // Disimpan per-kategori dalam Map, dinaikkan ke sini (bukan remember lokal) supaya
    // tidak ter-reset saat composable dilepas total ketika viewer file dibuka.
    private val _categoryGalleryMode = MutableStateFlow<Map<String, VideoGalleryMode>>(emptyMap())
    val categoryGalleryMode: StateFlow<Map<String, VideoGalleryMode>> = _categoryGalleryMode
    fun setCategoryGalleryMode(category: String, mode: VideoGalleryMode) {
        _categoryGalleryMode.value = _categoryGalleryMode.value + (category to mode)
    }

    private val _selectedCategoryFolderPath = MutableStateFlow<Map<String, String?>>(emptyMap())
    val selectedCategoryFolderPath: StateFlow<Map<String, String?>> = _selectedCategoryFolderPath
    fun selectCategoryFolder(category: String, path: String?) {
        _selectedCategoryFolderPath.value = _selectedCategoryFolderPath.value + (category to path)
    }

    // File mana yang lagi dibuka menu titik-tiganya (FileActionSheet). null = tidak ada yang kebuka.
    private val _actionSheetFile = MutableStateFlow<FileEntity?>(null)
    val actionSheetFile: StateFlow<FileEntity?> = _actionSheetFile
    fun openActionSheet(file: FileEntity) { _actionSheetFile.value = file }
    fun closeActionSheet() { _actionSheetFile.value = null }

    // Penanda "ada perubahan file dari luar Room" (hapus/rename/tempel manual di Direktori),
    // dipakai DirektoriScreen untuk tahu kapan perlu baca ulang java.io.File.listFiles().
    private val _fileOpsTick = MutableStateFlow(0)
    val fileOpsTick: StateFlow<Int> = _fileOpsTick
    fun notifyFileOpsChanged() { _fileOpsTick.value++ }

    // Hapus file fisik dari storage, baru hapus datanya dari database kalau berhasil.
    fun deleteFile(file: FileEntity) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                val ok = java.io.File(file.path).delete()
                if (ok) dao.deleteByPath(file.path)
                ok
            }
            if (deleted) notifyFileOpsChanged()
        }
    }

    // Ganti nama file fisik (tetap di folder yang sama), baru selaraskan database kalau berhasil.
    fun renameFile(file: FileEntity, newName: String) {
        viewModelScope.launch {
            val renamed = withContext(Dispatchers.IO) {
                val oldFile = java.io.File(file.path)
                val newFile = java.io.File(oldFile.parentFile, newName)
                val ok = oldFile.renameTo(newFile)
                if (ok) dao.renamePath(file.path, newFile.absolutePath, newName)
                ok
            }
            if (renamed) notifyFileOpsChanged()
        }
    }

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

    // yhs13: ambil file pertama pada kategori tertentu (dipakai kartu Audio di Beranda
    // supaya bisa langsung buka pemutar tanpa lewat daftar file kategori dulu)
    fun getFirstFileInCategory(category: String): FileEntity? {
        return files.value.firstOrNull { categoryOf(it.extension) == category }
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    private val _showDirektori = MutableStateFlow(false)
    val showDirektori: StateFlow<Boolean> = _showDirektori
    fun openDirektori() { _showDirektori.value = true }
    fun closeDirektori() { _showDirektori.value = false }

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
