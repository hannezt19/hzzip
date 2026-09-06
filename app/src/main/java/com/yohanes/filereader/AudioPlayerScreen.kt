package com.yohanes.filereader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO(ydiv2): Implementasi player pakai ExoPlayer/Media3
// Dependency yang perlu ditambahkan ke build.gradle.kts:
//   implementation("androidx.media3:media3-exoplayer:1.4.1")
//   implementation("androidx.media3:media3-session:1.4.1")
@Composable
fun AudioPlayerScreen(filePath: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Audio Player - belum diimplementasikan\n$filePath",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
