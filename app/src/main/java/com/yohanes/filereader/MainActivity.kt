package com.yohanes.filereader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yohanes.filereader.data.ThemeStore
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yohanes.filereader.ui.CodeEditorScreen
import com.yohanes.filereader.ui.HomeScreen
import com.yohanes.filereader.ui.HomeViewModel
import com.yohanes.filereader.ui.PdfViewerScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    // State dipegang di luar Compose supaya gampang diakses dari launcher/onNewIntent
    private var currentUri by mutableStateOf<Uri?>(null)
    private var currentType by mutableStateOf(FileType.UNKNOWN)
    private var currentName by mutableStateOf("")
    private var currentContent by mutableStateOf("")
    private var isLoadingContent by mutableStateOf(false)

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
        ThemeStore.init(this)

        // Status bar dibuat tetap gelap (ikon terang) apapun mode tema app - permintaan user
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            val isDarkMode = ThemeStore.isDarkMode.collectAsState()
            MaterialTheme(
                colorScheme = if (isDarkMode.value) darkColorScheme() else lightColorScheme()
            ) {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        AppRoot()
                        // Overlay hitam permanen di area status bar - permintaan user,
                        // supaya jam/ikon status bar selalu terlihat apapun mode tema app.
                        // Tidak menghalangi sentuhan karena tidak diberi pointerInput/clickable.
                        Box(
                            Modifier
                                .align(ComposeAlignment.TopCenter)
                                .fillMaxWidth()
                                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                                .background(androidx.compose.ui.graphics.Color.Black)
                        )
                    }
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
        currentContent = ""

        if (type != FileType.PDF && type != FileType.UNKNOWN && type != FileType.IMAGE && type != FileType.XLSX && type != FileType.VIDEO && type != FileType.AUDIO) {
            isLoadingContent = true
            lifecycleScope.launch {
                val text = withContext(Dispatchers.IO) { readText(uri) }
                currentContent = text
                isLoadingContent = false
            }
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

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    private fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionGranted by remember { mutableStateOf(com.yohanes.filereader.permission.hasStoragePermission()) }

    if (!permissionGranted) {
        PermissionRequestState(
            onRequestPermission = {
                context.startActivity(com.yohanes.filereader.permission.requestStoragePermissionIntent(context.packageName))
            },
            onRecheck = { permissionGranted = com.yohanes.filereader.permission.hasStoragePermission() }
        )
        return
    }

        val uri = currentUri
        if (uri != null) {
            androidx.activity.compose.BackHandler {
                currentUri = null
            }
        }
        if (uri == null) {
            var selectedTab by remember { mutableStateOf(com.yohanes.filereader.ui.AppTab.HOME) }
            val homeViewModel: HomeViewModel = viewModel()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val drawerScope = rememberCoroutineScope()
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.75f)) {
                        com.yohanes.filereader.ui.AppDrawerContent(
                            isDarkMode = ThemeStore.isDarkMode.collectAsState().value,
                            onToggleDarkMode = { ThemeStore.toggle() },
                            onBeranda = {
                                homeViewModel.onCategorySelected(null)
                                homeViewModel.closeDirektori()
                                selectedTab = com.yohanes.filereader.ui.AppTab.HOME
                                drawerScope.launch { drawerState.close() }
                            },
                            onTerakhir = {
                                selectedTab = com.yohanes.filereader.ui.AppTab.RECENT
                                drawerScope.launch { drawerState.close() }
                            },
                            onDirektori = {
                                homeViewModel.openDirektori()
                                selectedTab = com.yohanes.filereader.ui.AppTab.HOME
                                drawerScope.launch { drawerState.close() }
                            },
                            onFavorit = {
                                homeViewModel.onCategorySelected("Favorit")
                                selectedTab = com.yohanes.filereader.ui.AppTab.HOME
                                drawerScope.launch { drawerState.close() }
                            },
                        )
                    }
                }
            ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 48.dp)
                ) {
                    when (selectedTab) {
                        com.yohanes.filereader.ui.AppTab.HOME -> HomeScreen(
                            viewModel = homeViewModel,
                            onFileClick = { file ->
                                loadFile(android.net.Uri.fromFile(java.io.File(file.path)))
                            },
                            onPickFileManually = {
                                openDocumentLauncher.launch(
                                    arrayOf(
                                        "application/pdf", "application/json", "text/html",
                                        "text/javascript", "application/javascript", "text/plain"
                                    )
                                )
                            }
                        )
                        com.yohanes.filereader.ui.AppTab.RECENT -> com.yohanes.filereader.ui.RecentScreen(
                            onFileClick = { file ->
                                loadFile(android.net.Uri.fromFile(java.io.File(file.path)))
                            }
                        )
                    }
                }
                IconButton(
                    onClick = { drawerScope.launch { drawerState.open() } },
                    modifier = Modifier.statusBarsPadding().padding(4.dp)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
            }
            }
            return
        }

        when (currentType) {
            FileType.PDF -> PdfViewerScreen(uri = uri, displayName = currentName)
            FileType.IMAGE -> com.yohanes.filereader.ui.ImageViewerScreen(uri = uri, displayName = currentName, onExit = { currentUri = null })
            FileType.XLSX -> com.yohanes.filereader.ui.XlsxViewerScreen(uri = uri, displayName = currentName, onExit = { currentUri = null })
            FileType.VIDEO -> com.yohanes.filereader.VideoPlayerScreen(uri = uri, displayName = currentName, onExit = { currentUri = null })
            FileType.AUDIO -> AudioPlayerScreen(filePath = uri.path ?: uri.toString())
            FileType.UNKNOWN -> UnsupportedState(currentName)
            else -> {
                if (isLoadingContent) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    CodeEditorScreen(
                        uri = uri,
                        displayName = currentName,
                        fileType = currentType,
                        initialContent = currentContent,
                        onSave = { text -> writeText(uri, text) },
                        onSaveAs = { text ->
                            pendingSaveAsText = text
                            createDocumentLauncher.launch(currentName)
                        },
                        onExit = { currentUri = null }
                    )
                }
            }
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

@Composable
private fun PermissionRequestState(
    onRequestPermission: () -> Unit,
    onRecheck: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Izin Akses File Diperlukan", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Aplikasi ini butuh izin akses semua file untuk mencari dan membuka dokumen di penyimpanan HP kamu.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Izinkan Akses Semua File") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRecheck) { Text("Sudah Izinkan? Cek Lagi") }
    }
}
