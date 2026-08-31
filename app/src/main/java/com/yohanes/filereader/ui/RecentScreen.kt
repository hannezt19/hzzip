package com.yohanes.filereader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yohanes.filereader.data.FileEntity

@Composable
fun RecentScreen(onFileClick: (FileEntity) -> Unit) {
    val viewModel: HomeViewModel = viewModel()
    val files by viewModel.files.collectAsState()
    val recentFiles = files.take(20)

    if (recentFiles.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Belum ada riwayat file", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(recentFiles) { file ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 12.dp)
                        .clickable { onFileClick(file) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            file.extension.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Divider()
            }
        }
    }
}
