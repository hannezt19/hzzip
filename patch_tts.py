import sys

def replace_once(path, old, new):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    count = content.count(old)
    if count != 1:
        print(f"ERROR di {path}: expected 1 occurrence, found {count}")
        print("---OLD (awal 200 char)---")
        print(old[:200])
        sys.exit(1)
    content = content.replace(old, new, 1)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"OK: {path} dipatch")

gradle_path = "app/build.gradle.kts"
replace_once(
    gradle_path,
    '    implementation("io.coil-kt:coil-compose:2.6.0")\n',
    '    implementation("io.coil-kt:coil-compose:2.6.0")\n    implementation("androidx.media:media:1.7.0")\n'
)

manifest_path = "app/src/main/AndroidManifest.xml"
replace_once(
    manifest_path,
    '    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"\n        tools:ignore="ScopedStorage" />\n',
    '    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"\n        tools:ignore="ScopedStorage" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />\n    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n'
)
replace_once(
    manifest_path,
    '        </activity>\n\n    </application>',
    '        </activity>\n\n        <service\n            android:name=".service.TtsPlaybackService"\n            android:foregroundServiceType="mediaPlayback"\n            android:exported="false" />\n\n    </application>'
)

pdf_path = "app/src/main/java/com/yohanes/filereader/ui/PdfViewerScreen.kt"

replace_once(
    pdf_path,
    'import android.content.Context\n',
    'import android.content.Context\n'
    'import android.content.pm.PackageManager\n'
    'import android.os.Build\n'
    'import androidx.activity.compose.rememberLauncherForActivityResult\n'
    'import androidx.activity.result.contract.ActivityResultContracts\n'
    'import androidx.core.content.ContextCompat\n'
    'import com.yohanes.filereader.service.TtsPlaybackBridge\n'
    'import com.yohanes.filereader.service.TtsPlaybackService\n'
)

old_anchor = '''            fun playFromPage(pageIndex: Int, sentenceIndex: Int) {
                scope.launch {
                    if (ttsCurrentPageIndex != pageIndex || ttsSentences.isEmpty()) {
                        ttsCurrentPageIndex = pageIndex
                        ttsSentences = loadTtsSentences(pageIndex)
                    }
                    if (ttsSentences.isEmpty()) {
                        ttsPlaying = false
                        return@launch
                    }
                    ttsSentenceIndex = sentenceIndex.coerceIn(0, ttsSentences.size - 1)
                    ttsPlaying = true
                    speakSentence(ttsSentenceIndex)
                }
            }

            LaunchedEffect(Unit) {
                TtsHelper.ensureInit(context) {'''

new_anchor = '''            fun playFromPage(pageIndex: Int, sentenceIndex: Int) {
                scope.launch {
                    if (ttsCurrentPageIndex != pageIndex || ttsSentences.isEmpty()) {
                        ttsCurrentPageIndex = pageIndex
                        ttsSentences = loadTtsSentences(pageIndex)
                    }
                    if (ttsSentences.isEmpty()) {
                        ttsPlaying = false
                        return@launch
                    }
                    ttsSentenceIndex = sentenceIndex.coerceIn(0, ttsSentences.size - 1)
                    ttsPlaying = true
                    speakSentence(ttsSentenceIndex)
                }
            }

            fun ttsSkipPrev() {
                val newIndex = (ttsSentenceIndex - 1).coerceAtLeast(0)
                if (ttsPlaying) {
                    playFromPage(ttsCurrentPageIndex, newIndex)
                } else {
                    ttsSentenceIndex = newIndex
                }
            }

            fun ttsSkipNext() {
                val maxIndex = (ttsSentences.size - 1).coerceAtLeast(0)
                val newIndex = (ttsSentenceIndex + 1).coerceAtMost(maxIndex)
                if (ttsPlaying) {
                    playFromPage(ttsCurrentPageIndex, newIndex)
                } else {
                    ttsSentenceIndex = newIndex
                }
            }

            fun ttsTogglePlayPause() {
                if (ttsPlaying) {
                    TtsHelper.stop()
                    ttsPlaying = false
                } else {
                    val pageIdx = if (readerSettings.navMode == NavigasiMode.SWIPE) pagerState.currentPage else scrollListState.firstVisibleItemIndex
                    playFromPage(pageIdx, ttsSentenceIndex)
                }
            }

            fun ttsStopAndClose() {
                TtsHelper.stop()
                ttsPlaying = false
                ttsActive = false
                ttsPanelExpanded = false
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
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
            }

            LaunchedEffect(ttsPlaying, ttsActive) {
                if (ttsActive) {
                    TtsPlaybackBridge.updateState(ttsPlaying, displayName)
                }
            }

            LaunchedEffect(Unit) {
                TtsHelper.ensureInit(context) {'''

replace_once(pdf_path, old_anchor, new_anchor)

old_buttons = '''                            TtsPillButton(icon = "\\u23EA") {
                                val newIndex = (ttsSentenceIndex - 1).coerceAtLeast(0)
                                if (ttsPlaying) {
                                    playFromPage(ttsCurrentPageIndex, newIndex)
                                } else {
                                    ttsSentenceIndex = newIndex
                                }
                            }
                            TtsPillButton(icon = if (ttsPlaying) "\\u23F8" else "\\u25B6") {
                                if (ttsPlaying) {
                                    TtsHelper.stop()
                                    ttsPlaying = false
                                } else {
                                    val pageIdx = if (readerSettings.navMode == NavigasiMode.SWIPE) pagerState.currentPage else scrollListState.firstVisibleItemIndex
                                    playFromPage(pageIdx, ttsSentenceIndex)
                                }
                            }
                            TtsPillButton(icon = "\\u23E9") {
                                val maxIndex = (ttsSentences.size - 1).coerceAtLeast(0)
                                val newIndex = (ttsSentenceIndex + 1).coerceAtMost(maxIndex)
                                if (ttsPlaying) {
                                    playFromPage(ttsCurrentPageIndex, newIndex)
                                } else {
                                    ttsSentenceIndex = newIndex
                                }
                            }
                            TtsPillButton(icon = "\\u2699") { ttsPanelExpanded = !ttsPanelExpanded }
                            TtsPillButton(icon = "\\u2715") {
                                TtsHelper.stop()
                                ttsPlaying = false
                                ttsActive = false
                                ttsPanelExpanded = false
                            }'''

new_buttons = '''                            TtsPillButton(icon = "\\u23EA") { ttsSkipPrev() }
                            TtsPillButton(icon = if (ttsPlaying) "\\u23F8" else "\\u25B6") { ttsTogglePlayPause() }
                            TtsPillButton(icon = "\\u23E9") { ttsSkipNext() }
                            TtsPillButton(icon = "\\u2699") { ttsPanelExpanded = !ttsPanelExpanded }
                            TtsPillButton(icon = "\\u2715") { ttsStopAndClose() }'''

replace_once(pdf_path, old_buttons, new_buttons)

print("SEMUA PATCH BERHASIL")
