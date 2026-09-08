package com.yohanes.filereader.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.yohanes.filereader.data.FileEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menu titik-tiga per file: Properti, Salin, Potong, Ganti Nama, Bagikan, Hapus.
 * Dipicu lewat long-press (bukan tap biasa) supaya tidak bentrok dengan tap
 * yang sudah dipakai buka viewer file.
 *
 * File fisik (hapus/rename) TIDAK diurus di sini - komponen ini cuma menangkap
 * niat & konfirmasi user, eksekusi sebenarnya (java.io.File + update FileDao)
 * dilakukan pemanggil (HomeViewModel) lewat callback onDeleteConfirmed/onRenameConfirmed,
 * supaya kerja IO/coroutine tetap terpusat di ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionSheet(
    file: FileEntity,
    onDismiss: () -> Unit,
    onCopy: (FileEntity) -> Unit,
    onCut: (FileEntity) -> Unit,
    onDeleteConfirmed: (FileEntity) -> Unit,
    onRenameConfirmed: (FileEntity, String) -> Unit
) {
    val context = LocalContext.current
    var showProperties by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                file.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Divider()
            ActionItem(Icons.Filled.Info, "Properti") { showProperties = true }
            ActionItem(Icons.Filled.ContentCopy, "Salin") {
                onCopy(file)
                onDismiss()
            }
            ActionItem(Icons.Filled.ContentCut, "Potong") {
                onCut(file)
                onDismiss()
            }
            ActionItem(Icons.Filled.DriveFileRenameOutline, "Ganti Nama") { showRenameDialog = true }
            ActionItem(Icons.Filled.Share, "Bagikan") {
                shareFile(context, file)
                onDismiss()
            }
            ActionItem(Icons.Filled.Delete, "Hapus") { showDeleteConfirm = true }
        }
    }

    if (showProperties) {
        FilePropertiesDialog(file = file, onDismiss = { showProperties = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus file?") },
            text = { Text("\"${file.name}\" akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteConfirmed(file)
                    onDismiss()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(file.name) }
        val invalidChars = remember { "[/\\\\:*?\"<>|]".toRegex() }
        val isValid = newName.isNotBlank() && !invalidChars.containsMatchIn(newName)
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ganti Nama") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        isError = !isValid && newName.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isValid && newName.isNotEmpty()) {
                        Text(
                            "Nama tidak boleh kosong atau mengandung karakter / \\ : * ? \" < > |",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = isValid,
                    onClick = {
                        showRenameDialog = false
                        onRenameConfirmed(file, newName)
                        onDismiss()
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FilePropertiesDialog(file: FileEntity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properti") },
        text = {
            Column {
                PropertyRow("Nama", file.name)
                PropertyRow("Lokasi", file.path)
                PropertyRow("Ukuran", formatSizeProperties(file.sizeBytes))
                PropertyRow("Diubah", formatDateProperties(file.lastModified))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatSizeProperties(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}

private fun formatDateProperties(epochMillis: Long): String {
    val sdf = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(epochMillis))
}

private fun shareFile(context: Context, file: FileEntity) {
    val fileObj = File(file.path)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileObj)
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan ${file.name}"))
}
