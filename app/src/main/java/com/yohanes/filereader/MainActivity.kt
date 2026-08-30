package com.yohanes.filereader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.ui.CodeEditorScreen
import com.yohanes.filereader.ui.PdfViewerScreen
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    // State dipegang di luar Compose supaya gampang diakses dari launcher/onNewIntent
    private var currentUri by mutableStateOf<Uri?>(null)
    private var currentType by mutableStateOf(FileType.UNKNOWN)
    private var currentName by mutableStateOf("")
    private var currentContent by mutableStateOf("")

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { loadFile(it) } }

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            writeText(it, pendingSaveAsText)
            Toast.makeText(this, "Tersimpan sebagai file baru", Toast.LENGTH_SHORT).show()
        }
    }
    private var pendingSaveAsText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        loadFile(uri)
    }

    private fun loadFile(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Beberapa file manager tidak mengizinkan izin persist — tidak fatal,
            // baca file tetap bisa jalan selama sesi ini.
        }

        val name = FileTypeDetector.queryDisplayName(contentResolver, uri) ?: "file"
        val type = FileTypeDetector.detect(contentResolver, uri)
        currentUri = uri
        currentName = name
        currentType = type

        if (type != FileType.PDF && type != FileType.UNKNOWN) {
            currentContent = readText(uri)
        }
    }

    private fun readText(uri: Uri): String {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            } ?: ""
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membaca file: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        }
    }

    private fun writeText(uri: Uri, text: String) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(text.toByteArray())
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa menimpa file langsung. Gunakan 'Simpan sebagai'.", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun AppRoot() {
        val uri = currentUri
        if (uri == null) {
            EmptyState(onPickFile = {
                openDocumentLauncher.launch(
                    arrayOf(
                        "application/pdf", "application/json", "text/html",
                        "text/javascript", "application/javascript", "text/plain"
                    )
                )
            })
            return
        }

        when (currentType) {
            FileType.PDF -> PdfViewerScreen(uri = uri, displayName = currentName)
            FileType.UNKNOWN -> UnsupportedState(currentName)
            else -> CodeEditorScreen(
                displayName = currentName,
                fileType = currentType,
                initialContent = currentContent,
                onSave = { text -> writeText(uri, text) },
                onSaveAs = { text ->
                    pendingSaveAsText = text
                    createDocumentLauncher.launch(currentName)
                }
            )
        }
    }
}

@Composable
private fun EmptyState(onPickFile: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Belum ada file dibuka", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Buka file lewat File Manager (pilih \"Buka dengan\" → File Reader), atau pilih file di bawah ini.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onPickFile) { Text("Pilih File") }
    }
}

@Composable
private fun UnsupportedState(name: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Jenis file belum didukung", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}
