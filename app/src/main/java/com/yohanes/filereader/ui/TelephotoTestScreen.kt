package com.yohanes.filereader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

// LAYAR UJI COBA SEMENTARA - untuk tes apakah Telephoto bisa
// menyatukan zoom di atas banyak item LazyColumn sekaligus.
// Belum menyentuh PdfViewerScreen sama sekali.
@Composable
fun TelephotoTestScreen() {
    val zoomableState = rememberZoomableState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .zoomable(zoomableState)
    ) {
        items(20) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(if (index % 2 == 0) Color(0xFFDDDDDD) else Color(0xFFAAAAAA)),
                contentAlignment = Alignment.Center
            ) {
                Text("Halaman ${index + 1}", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
