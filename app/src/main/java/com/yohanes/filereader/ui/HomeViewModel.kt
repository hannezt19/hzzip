package com.yohanes.filereader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yohanes.filereader.data.AppDatabase
import com.yohanes.filereader.data.FileEntity
import com.yohanes.filereader.data.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.fileDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedExtension = MutableStateFlow<String?>(null)
    val selectedExtension: StateFlow<String?> = _selectedExtension

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    val files: StateFlow<List<FileEntity>> = combine(
        dao.getAll(),
        _searchQuery,
        _selectedExtension
    ) { all, query, ext ->
        all.filter { file ->
            (query.isBlank() || file.name.contains(query, ignoreCase = true)) &&
                (ext == null || file.extension == ext)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshScan()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onExtensionFilterChange(ext: String?) {
        _selectedExtension.value = ext
    }

    fun refreshScan() {
        viewModelScope.launch {
            _isScanning.value = true
            val results = withContext(Dispatchers.IO) {
                FileScanner.scanAll()
            }
            withContext(Dispatchers.IO) {
                dao.clearAll()
                dao.insertAll(results)
            }
            _isScanning.value = false
        }
    }
}
