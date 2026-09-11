package com.yohanes.filereader.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ClipboardOp { COPY, CUT }

data class ClipboardState(
    val files: List<FileEntity> = emptyList(),
    val op: ClipboardOp? = null
) {
    val isEmpty: Boolean get() = files.isEmpty()
}

/**
 * Singleton penyimpanan status salin/potong, dipakai bareng FileActionSheet
 * (1 file, dari long-press biasa) dan mode multi-select (banyak file sekaligus).
 * Fungsi copy/cut lama (1 file) dipertahankan sebagai overload untuk kompatibilitas
 * pemanggil yang sudah ada.
 */
object FileClipboard {
    private val _state = MutableStateFlow(ClipboardState())
    val state: StateFlow<ClipboardState> = _state.asStateFlow()

    fun copy(file: FileEntity) { copy(listOf(file)) }
    fun cut(file: FileEntity) { cut(listOf(file)) }

    fun copy(files: List<FileEntity>) {
        _state.value = ClipboardState(files, ClipboardOp.COPY)
    }

    fun cut(files: List<FileEntity>) {
        _state.value = ClipboardState(files, ClipboardOp.CUT)
    }

    fun clear() {
        _state.value = ClipboardState()
    }
}
