package com.yohanes.filereader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(isDarkMode: Boolean, onToggleDarkMode: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Tema gelap", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(12.dp))
            Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Mode baca, kontras, dan pengaturan lain akan muncul di sini.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
