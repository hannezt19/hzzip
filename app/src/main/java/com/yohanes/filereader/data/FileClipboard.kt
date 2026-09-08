package com.yohanes.filereader.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ClipboardOp { COPY, CUT }

data class ClipboardState(
    val file: FileEntity? = null,
    val op: ClipboardOp? = null
) {
    val isEmpty: Boolean get() = file == null
}

/**
 * Singleton penyimpanan status salin/potong 1 file, dipakai bareng
 * FileActionSheet (Salin/Potong) dan DirektoriScreen (tombol Tempel).
 * Sengaja cuma simpan 1 file (belum multi-select, sesuai keputusan
 * Tugas 2 - multi-select ditunda ke tahap berikutnya).
 */
object FileClipboard {
    private val _state = MutableStateFlow(ClipboardState())
    val state: StateFlow<ClipboardState> = _state.asStateFlow()

    fun copy(file: FileEntity) {
        _state.value = ClipboardState(file, ClipboardOp.COPY)
    }

    fun cut(file: FileEntity) {
        _state.value = ClipboardState(file, ClipboardOp.CUT)
    }

    fun clear() {
        _state.value = ClipboardState()
    }
}
