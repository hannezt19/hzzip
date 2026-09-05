package com.yohanes.filereader.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanes.filereader.FileType

private data class LineEntry(val id: Long, val text: String)

private data class LineChangeOp(
    val startIndex: Int,
    val oldEntries: List<LineEntry>,
    val newEntries: List<LineEntry>
)

private class EditHistory(private val maxSize: Int = 200) {
    val undoStack = ArrayDeque<LineChangeOp>()
    val redoStack = ArrayDeque<LineChangeOp>()
    fun push(op: LineChangeOp) {
        undoStack.addLast(op)
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }
}

/**
 * Editor teks/kode berbasis LazyColumn PER BARIS - bukan satu BasicTextField
 * atau EditText raksasa untuk seluruh file. LazyColumn cuma memproses baris
 * yang benar-benar terlihat di layar, jadi file besar (puluhan ribu baris)
 * sama cepatnya dibuka dengan file kecil. Ini rombakan ke-2 dari editor ini
 * setelah pendekatan EditText native (rombakan ke-1) ternyata masih belum
 * cukup cepat untuk file sangat besar (10MB+).
 */
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
    var nextId by remember { mutableStateOf(0L) }
    fun newId(): Long { val id = nextId; nextId += 1; return id }

    val lines = remember {
        mutableStateListOf<LineEntry>().apply {
            initialContent.split("\n").forEach { add(LineEntry(newId(), it)) }
        }
    }
    val history = remember { EditHistory() }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var isDirty by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var focusRequestIndex by remember { mutableStateOf<Int?>(null) }
    var focusRequestCursor by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    val favKey = uri.path ?: uri.toString()
    val favorites by com.yohanes.filereader.data.FavoritesStore.favorites.collectAsState()
    val isFav = favorites.contains(favKey)

    fun refreshHistoryState() {
        canUndo = history.undoStack.isNotEmpty()
        canRedo = history.redoStack.isNotEmpty()
        isDirty = history.undoStack.isNotEmpty()
    }

    fun applyForward(op: LineChangeOp) {
        repeat(op.oldEntries.size) { lines.removeAt(op.startIndex) }
        lines.addAll(op.startIndex, op.newEntries)
    }

    fun applyInverse(op: LineChangeOp) {
        repeat(op.newEntries.size) { lines.removeAt(op.startIndex) }
        lines.addAll(op.startIndex, op.oldEntries)
    }

    fun commitSingleLineEdit(index: Int, oldText: String, newText: String) {
        if (oldText == newText || index !in lines.indices) return
        val id = lines[index].id
        val op = LineChangeOp(index, listOf(LineEntry(id, oldText)), listOf(LineEntry(id, newText)))
        lines[index] = LineEntry(id, newText)
        history.push(op)
        refreshHistoryState()
    }

    fun commitEnter(index: Int, before: String, after: String) {
        if (index !in lines.indices) return
        val oldEntry = lines[index]
        val newFirst = LineEntry(oldEntry.id, before)
        val newSecond = LineEntry(newId(), after)
        val op = LineChangeOp(index, listOf(oldEntry), listOf(newFirst, newSecond))
        applyForward(op)
        history.push(op)
        refreshHistoryState()
        focusRequestIndex = index + 1
        focusRequestCursor = 0
    }

    fun commitBackspaceMerge(index: Int) {
        if (index <= 0 || index !in lines.indices) return
        val prevEntry = lines[index - 1]
        val currEntry = lines[index]
        val merged = LineEntry(prevEntry.id, prevEntry.text + currEntry.text)
        val op = LineChangeOp(index - 1, listOf(prevEntry, currEntry), listOf(merged))
        applyForward(op)
        history.push(op)
        refreshHistoryState()
        focusRequestIndex = index - 1
        focusRequestCursor = prevEntry.text.length
    }

    fun undo() {
        if (history.undoStack.isEmpty()) return
        val op = history.undoStack.removeLast()
        applyInverse(op)
        history.redoStack.addLast(op)
        refreshHistoryState()
        focusRequestIndex = op.startIndex
        focusRequestCursor = op.oldEntries.lastOrNull()?.text?.length ?: 0
    }

    fun redo() {
        if (history.redoStack.isEmpty()) return
        val op = history.redoStack.removeLast()
        applyForward(op)
        history.undoStack.addLast(op)
        refreshHistoryState()
        focusRequestIndex = op.startIndex
        focusRequestCursor = op.newEntries.lastOrNull()?.text?.length ?: 0
    }

    fun currentFullText(): String = lines.joinToString("\n") { it.text }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Perubahan belum disimpan") },
            text = { Text("Simpan perubahan sebelum keluar?") },
            confirmButton = {
                TextButton(onClick = {
                    onSave(currentFullText())
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
                    tint = if (isFav) Color(0xFFFFC107) else Color.Gray
                )
            }
            Spacer(Modifier.width(4.dp))
            TextButton(enabled = canUndo, onClick = { undo() }) { Text("Undo") }
            Spacer(Modifier.width(4.dp))
            TextButton(enabled = canRedo, onClick = { redo() }) { Text("Redo") }
            Spacer(Modifier.width(4.dp))
            Button(onClick = { onSave(currentFullText()) }) { Text("Simpan") }
        }

        val digitWidth = lines.size.toString().length.coerceAtLeast(2)

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(lines, key = { _, entry -> entry.id }) { index, entry ->
                CodeLineRow(
                    entry = entry,
                    fileType = fileType,
                    numberLabel = (index + 1).toString().padStart(digitWidth),
                    requestFocus = focusRequestIndex == index,
                    requestCursor = focusRequestCursor,
                    onFocusHandled = { if (focusRequestIndex == index) focusRequestIndex = null },
                    onTextChange = { newText -> commitSingleLineEdit(index, entry.text, newText) },
                    onEnter = { before, after -> commitEnter(index, before, after) },
                    onBackspaceAtStart = { commitBackspaceMerge(index) }
                )
            }
        }
    }
}

@Composable
private fun CodeLineRow(
    entry: LineEntry,
    fileType: FileType,
    numberLabel: String,
    requestFocus: Boolean,
    requestCursor: Int,
    onFocusHandled: () -> Unit,
    onTextChange: (String) -> Unit,
    onEnter: (before: String, after: String) -> Unit,
    onBackspaceAtStart: () -> Unit
) {
    var value by remember(entry.id) { mutableStateOf(TextFieldValue(entry.text)) }
    LaunchedEffect(entry.text) {
        if (value.text != entry.text) value = TextFieldValue(entry.text)
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            value = value.copy(selection = TextRange(requestCursor.coerceIn(0, value.text.length)))
            onFocusHandled()
        }
    }
    val highlightSpans = remember(entry.text, fileType) { computeHighlightSpans(entry.text, fileType) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = numberLabel,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, end = 8.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = { new ->
                if ('\n' in new.text) {
                    val idx = new.text.indexOf('\n')
                    val before = new.text.substring(0, idx)
                    val after = new.text.substring(idx + 1)
                    value = TextFieldValue(before)
                    onEnter(before, after)
                } else {
                    value = new
                    onTextChange(new.text)
                }
            },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, end = 8.dp)
                .focusRequester(focusRequester)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        value.selection.start == 0 &&
                        value.selection.end == 0
                    ) {
                        onBackspaceAtStart()
                        true
                    } else {
                        false
                    }
                },
            visualTransformation = { text ->
                val builder = AnnotatedString.Builder(text.text)
                for (span in highlightSpans) {
                    if (span.end <= text.text.length) {
                        builder.addStyle(SpanStyle(color = Color(span.color)), span.start, span.end)
                    }
                }
                TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
            }
        )
    }
}
