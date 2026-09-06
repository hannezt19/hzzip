import sys

def replace_once(path, old, new):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    count = content.count(old)
    if count != 1:
        print(f"ERROR di {path}: expected 1 occurrence, found {count}")
        print("---OLD (awal 300 char)---")
        print(old[:300])
        sys.exit(1)
    content = content.replace(old, new, 1)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"OK: {path} dipatch")

pdf_path = "app/src/main/java/com/yohanes/filereader/ui/PdfViewerScreen.kt"

old_effect = '''            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(ttsActive) {
                if (ttsActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    ContextCompat.startForegroundService(
                        context, android.content.Intent(context, TtsPlaybackService::class.java)
                    )
                    TtsPlaybackBridge.onPlayPause = { ttsTogglePlayPause() }
                    TtsPlaybackBridge.onSkipNext = { ttsSkipNext() }
                    TtsPlaybackBridge.onSkipPrev = { ttsSkipPrev() }
                    TtsPlaybackBridge.onStop = { ttsStopAndClose() }
                } else {
                    TtsPlaybackBridge.onPlayPause = null
                    TtsPlaybackBridge.onSkipNext = null
                    TtsPlaybackBridge.onSkipPrev = null
                    TtsPlaybackBridge.onStop = null
                    context.stopService(android.content.Intent(context, TtsPlaybackService::class.java))
                }
            }'''

new_effect = '''            LaunchedEffect(ttsActive) {
                if (ttsActive) {
                    try {
                        ContextCompat.startForegroundService(
                            context, android.content.Intent(context, TtsPlaybackService::class.java)
                        )
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context, "TTS notif error: ${e.message}", android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    TtsPlaybackBridge.onPlayPause = { ttsTogglePlayPause() }
                    TtsPlaybackBridge.onSkipNext = { ttsSkipNext() }
                    TtsPlaybackBridge.onSkipPrev = { ttsSkipPrev() }
                    TtsPlaybackBridge.onStop = { ttsStopAndClose() }
                } else {
                    TtsPlaybackBridge.onPlayPause = null
                    TtsPlaybackBridge.onSkipNext = null
                    TtsPlaybackBridge.onSkipPrev = null
                    TtsPlaybackBridge.onStop = null
                    context.stopService(android.content.Intent(context, TtsPlaybackService::class.java))
                }
            }'''

replace_once(pdf_path, old_effect, new_effect)

main_path = "app/src/main/java/com/yohanes/filereader/MainActivity.kt"

replace_once(
    main_path,
    'import android.os.Bundle\n',
    'import android.os.Bundle\n'
    'import android.content.pm.PackageManager\n'
    'import android.os.Build\n'
    'import androidx.core.content.ContextCompat\n'
)

old_launcher_block = '''    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            writeText(it, pendingSaveAsText)
            Toast.makeText(this, "Tersimpan sebagai file baru", Toast.LENGTH_SHORT).show()
        }
    }
    private var pendingSaveAsText: String = ""'''

new_launcher_block = '''    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            writeText(it, pendingSaveAsText)
            Toast.makeText(this, "Tersimpan sebagai file baru", Toast.LENGTH_SHORT).show()
        }
    }
    private var pendingSaveAsText: String = ""

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }'''

replace_once(main_path, old_launcher_block, new_launcher_block)

old_oncreate = '''        handleIncomingIntent(intent)
        ThemeStore.init(this)

        setContent {'''

new_oncreate = '''        handleIncomingIntent(intent)
        ThemeStore.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {'''

replace_once(main_path, old_oncreate, new_oncreate)

print("SEMUA PATCH BERHASIL")
