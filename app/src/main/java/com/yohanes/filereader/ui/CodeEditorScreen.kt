package com.yohanes.filereader.ui

import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.ScrollView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yohanes.filereader.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class EditOp(val start: Int, val removed: String, val inserted: String)

private class EditHistory(private val maxSize: Int = 200) {
    val undoStack = ArrayDeque<EditOp>()
    val redoStack = ArrayDeque<EditOp>()

    fun push(op: EditOp) {
        undoStack.addLast(op)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }
}

@Composable
fun CodeEditorScreen(
    uri: android.net.Uri,
    displayName: String,
    fileType: FileType,
    initialContent: String,
    onSave: (String) -> Unit,
    onSaveAs: (String) -> Unit,
    onExit: () -> Unit
) {
    val editTextRef = remember { mutableStateOf<LineNumberEditText?>(null) }
    val history = remember { EditHistory() }
    var isDirty by remember { mutableStateOf(false) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var applyingHistory by remember { mutableStateOf(false) }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val favKey = uri.path ?: uri.toString()
    val favorites by com.yohanes.filereader.data.FavoritesStore.favorites.collectAsState()
    val isFav = favorites.contains(favKey)

    fun refreshHistoryState() {
        canUndo = history.undoStack.isNotEmpty()
        canRedo = history.redoStack.isNotEmpty()
        isDirty = history.undoStack.isNotEmpty()
    }

    fun rehighlightRange(editText: LineNumberEditText, changeStart: Int, changeEndHint: Int) {
        val editable = editText.text as? Editable ?: return
        val layout = editText.layout ?: return
        val safeStart = changeStart.coerceIn(0, editable.length)
        val safeEndHint = changeEndHint.coerceIn(0, editable.length)
        val startLine = layout.getLineForOffset(safeStart)
        val endLine = layout.getLineForOffset(safeEndHint).coerceAtMost((layout.lineCount - 1).coerceAtLeast(0))
        val from = layout.getLineStart(startLine).coerceIn(0, editable.length)
        val to = layout.getLineEnd(endLine).coerceIn(from, editable.length)

        editable.getSpans(from, to, ForegroundColorSpan::class.java).forEach { editable.removeSpan(it) }
        val lineText = editable.substring(from, to)
        val spans = computeHighlightSpans(lineText, fileType, offset = from)
        for (span in spans) {
            editable.setSpan(
                ForegroundColorSpan(span.color),
                span.start, span.end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun applyOpInverse(editText: LineNumberEditText, op: EditOp) {
        applyingHistory = true
        val editable = editText.text as Editable
        editable.replace(op.start, op.start + op.inserted.length, op.removed)
        editText.setSelection((op.start + op.removed.length).coerceIn(0, editable.length))
        applyingHistory = false
        rehighlightRange(editText, op.start, op.start + op.removed.length)
    }

    fun applyOpForward(editText: LineNumberEditText, op: EditOp) {
        applyingHistory = true
        val editable = editText.text as Editable
        editable.replace(op.start, op.start + op.removed.length, op.inserted)
        editText.setSelection((op.start + op.inserted.length).coerceIn(0, editable.length))
        applyingHistory = false
        rehighlightRange(editText, op.start, op.start + op.inserted.length)
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Perubahan belum disimpan") },
            text = { Text("Simpan perubahan sebelum keluar?") },
            confirmButton = {
                TextButton(onClick = {
                    editTextRef.value?.let { onSave(it.text.toString()) }
                    showConfirmDialog = false
                    onExit()
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onExit()
                }) { Text("Buang Perubahan") }
            }
        )
    }

    androidx.activity.compose.BackHandler {
        if (isDirty) showConfirmDialog = true else onExit()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayName, maxLines = 1, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { com.yohanes.filereader.data.FavoritesStore.toggle(favKey) }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorit",
                    tint = if (isFav) androidx.compose.ui.graphics.Color(0xFFFFC107) else androidx.compose.ui.graphics.Color.Gray
                )
            }
            Spacer(Modifier.width(4.dp))
            TextButton(enabled = canUndo, onClick = {
                val editText = editTextRef.value ?: return@TextButton
                if (history.undoStack.isNotEmpty()) {
                    val op = history.undoStack.removeLast()
                    applyOpInverse(editText, op)
                    history.redoStack.addLast(op)
                    refreshHistoryState()
                }
            }) { Text("Undo") }
            Spacer(Modifier.width(4.dp))
            TextButton(enabled = canRedo, onClick = {
                val editText = editTextRef.value ?: return@TextButton
                if (history.redoStack.isNotEmpty()) {
                    val op = history.redoStack.removeLast()
                    applyOpForward(editText, op)
                    history.undoStack.addLast(op)
                    refreshHistoryState()
                }
            }) { Text("Redo") }
            Spacer(Modifier.width(4.dp))
            Button(onClick = {
                editTextRef.value?.let { onSave(it.text.toString()) }
            }) { Text("Simpan") }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val editText = LineNumberEditText(ctx).apply {
                    setText(initialContent)
                    textSize = 14f
                    setTypeface(android.graphics.Typeface.MONOSPACE)
                    setTextColor(onSurfaceColor)
                }
                editTextRef.value = editText

                editText.addTextChangedListener(object : TextWatcher {
                    var editStart = 0
                    var editRemoved = ""
                    var editInserted = ""

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        if (applyingHistory) return
                        editStart = start
                        editRemoved = s?.subSequence(start, start + count)?.toString() ?: ""
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (applyingHistory) return
                        editInserted = s?.subSequence(start, start + count)?.toString() ?: ""
                    }

                    override fun afterTextChanged(s: Editable?) {
                        if (applyingHistory || s == null) return
                        if (editRemoved.isEmpty() && editInserted.isEmpty()) return
                        history.push(EditOp(editStart, editRemoved, editInserted))
                        refreshHistoryState()
                        rehighlightRange(editText, editStart, editStart + editInserted.length)
                    }
                })

                ScrollView(ctx).apply {
                    isFillViewport = true
                    addView(editText)
                }
            },
            update = { scrollView ->
                (scrollView.getChildAt(0) as? LineNumberEditText)?.setTextColor(onSurfaceColor)
            }
        )
    }

    LaunchedEffect(editTextRef.value) {
        val editText = editTextRef.value ?: return@LaunchedEffect
        val spans = withContext(Dispatchers.Default) {
            computeHighlightSpans(initialContent, fileType)
        }
        val editable = editText.text as? Editable ?: return@LaunchedEffect
        for (span in spans) {
            if (span.end <= editable.length) {
                editable.setSpan(
                    ForegroundColorSpan(span.color),
                    span.start, span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
