package com.yohanes.filereader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yohanes.filereader.FileType

/**
 * Editor teks sederhana: nomor baris + syntax highlighting + undo/redo + simpan.
 * History undo/redo dibatasi supaya tidak boros memori di device RAM kecil.
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
    var field by remember { mutableStateOf(TextFieldValue(initialContent)) }
    val undoStack = remember { ArrayDeque<String>() }
    val redoStack = remember { ArrayDeque<String>() }
    var lastPushed by remember { mutableStateOf(initialContent) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val isDirty = field.text != initialContent

    androidx.activity.compose.BackHandler {
        if (isDirty) showConfirmDialog = true else onExit()
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Perubahan belum disimpan") },
            text = { Text("Simpan perubahan sebelum keluar?") },
            confirmButton = {
                TextButton(onClick = {
                    onSave(field.text)
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

    fun pushUndoIfChanged(newText: String) {
        if (newText != lastPushed) {
            undoStack.addLast(lastPushed)
            if (undoStack.size > 50) undoStack.removeFirst()
            redoStack.clear()
            lastPushed = newText
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayName, maxLines = 1, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            val favKey = uri.path ?: uri.toString()
            val favorites by com.yohanes.filereader.data.FavoritesStore.favorites.collectAsState()
            val isFav = favorites.contains(favKey)
            androidx.compose.material3.IconButton(onClick = { com.yohanes.filereader.data.FavoritesStore.toggle(favKey) }) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.Star,
                    contentDescription = "Favorit",
                    tint = if (isFav) androidx.compose.ui.graphics.Color(0xFFFFC107) else androidx.compose.ui.graphics.Color.Gray
                )
            }
            Spacer(Modifier.width(4.dp))
            IconTextButton("Undo", enabled = undoStack.isNotEmpty()) {
                if (undoStack.isNotEmpty()) {
                    redoStack.addLast(field.text)
                    val prev = undoStack.removeLast()
                    lastPushed = prev
                    field = TextFieldValue(prev)
                }
            }
            Spacer(Modifier.width(4.dp))
            IconTextButton("Redo", enabled = redoStack.isNotEmpty()) {
                if (redoStack.isNotEmpty()) {
                    undoStack.addLast(field.text)
                    val next = redoStack.removeLast()
                    lastPushed = next
                    field = TextFieldValue(next)
                }
            }
            Spacer(Modifier.width(4.dp))
            Button(onClick = { onSave(field.text) }) { Text("Simpan") }
        }

        Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val lineCount = field.text.count { it == '\n' } + 1
            Column(Modifier.padding(start = 8.dp, top = 12.dp, end = 8.dp)) {
                for (i in 1..lineCount) {
                    Text(
                        text = i.toString(),
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }

            BasicTextField(
                value = field,
                onValueChange = { new ->
                    pushUndoIfChanged(field.text)
                    field = new
                },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 8.dp),
                visualTransformation = { text ->
                    androidx.compose.ui.text.input.TransformedText(
                        highlight(text.text, fileType),
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                }
            )
        }
    }
}

@Composable
private fun IconTextButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) { Text(label) }
}
