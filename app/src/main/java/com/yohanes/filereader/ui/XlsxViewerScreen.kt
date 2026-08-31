package com.yohanes.filereader.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.data.XlsxCell
import com.yohanes.filereader.data.XlsxParser
import com.yohanes.filereader.data.XlsxSheet
import com.yohanes.filereader.data.XlsxWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XlsxViewerScreen(uri: Uri, displayName: String, onExit: () -> Unit) {
    val context = LocalContext.current

    var sheet by remember { mutableStateOf<XlsxSheet?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        val result = XlsxParser.parse(context, uri)
        if (result == null) {
            loadFailed = true
        } else {
            sheet = result
        }
    }

    fun tryExit() {
        if (hasChanges) showConfirmDialog = true else onExit()
    }

    BackHandler { tryExit() }

    fun save() {
        val current = sheet ?: return
        isSaving = true
        val ok = XlsxWriter.save(context, uri, current)
        isSaving = false
        if (ok) {
            hasChanges = false
            Toast.makeText(context, "Tersimpan", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { tryExit() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    TextButton(onClick = { save() }) {
                        Text("Simpan")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loadFailed -> Text(
                    "Gagal membaca file xlsx",
                    modifier = Modifier.padding(16.dp)
                )
                sheet == null -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                isSaving -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                else -> {
                    val currentSheet = sheet!!
                    val horizontalScroll = rememberScrollState()
                    val colCount = currentSheet.rows.maxOfOrNull { it.size } ?: 0

                    Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(currentSheet.rows.size) { rowIdx ->
                                val row = currentSheet.rows[rowIdx]
                                Row {
                                    for (colIdx in 0 until colCount) {
                                        val cell = row.getOrNull(colIdx) ?: XlsxCell()
                                        var text by remember(rowIdx, colIdx, currentSheet) {
                                            mutableStateOf(cell.formula?.let { "=$it" } ?: cell.value)
                                        }
                                        TextField(
                                            value = text,
                                            onValueChange = { newValue ->
                                                text = newValue
                                                while (row.size <= colIdx) row.add(XlsxCell())
                                                val target = row[colIdx]
                                                if (newValue.startsWith("=")) {
                                                    target.formula = newValue.removePrefix("=")
                                                    target.value = ""
                                                    target.isNumeric = false
                                                } else {
                                                    target.formula = null
                                                    target.value = newValue
                                                    target.isNumeric = newValue.toDoubleOrNull() != null
                                                }
                                                hasChanges = true
                                            },
                                            modifier = Modifier
                                                .width(110.dp)
                                                .border(0.5.dp, Color.Gray)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Perubahan belum disimpan") },
            text = { Text("Simpan perubahan sebelum keluar?") },
            confirmButton = {
                TextButton(onClick = {
                    save()
                    showConfirmDialog = false
                    onExit()
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onExit()
                }) { Text("Batal") }
            }
        )
    }
}
