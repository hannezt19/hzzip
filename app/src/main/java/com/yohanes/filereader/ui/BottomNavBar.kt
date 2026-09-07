package com.yohanes.filereader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class AppTab {
    HOME, RECENT
}

@Composable
fun AppDrawerContent(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onBeranda: () -> Unit,
    onTerakhir: () -> Unit,
    onDirektori: () -> Unit,
    onFavorit: () -> Unit
) {
    var pengaturanExpanded by remember { mutableStateOf(false) }

    NavigationDrawerItem(
        label = { Text("Beranda") },
        selected = false,
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
        onClick = onBeranda,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    NavigationDrawerItem(
        label = { Text("Terakhir") },
        selected = false,
        icon = { Icon(Icons.Filled.List, contentDescription = null) },
        onClick = onTerakhir,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    NavigationDrawerItem(
        label = { Text("Direktori") },
        selected = false,
        icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
        onClick = onDirektori,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    NavigationDrawerItem(
        label = { Text("Favorit") },
        selected = false,
        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
        onClick = onFavorit,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    NavigationDrawerItem(
        label = { Text("Pengaturan") },
        selected = false,
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
        onClick = { pengaturanExpanded = !pengaturanExpanded },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    AnimatedVisibility(visible = pengaturanExpanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tema gelap", modifier = Modifier.padding(end = 12.dp))
            Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
        }
    }
}
