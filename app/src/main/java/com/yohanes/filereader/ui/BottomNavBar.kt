package com.yohanes.filereader.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class AppTab {
    HOME, RECENT, SETTINGS
}

@Composable
fun AppDrawerContent(
    onBeranda: () -> Unit,
    onTerakhir: () -> Unit,
    onDirektori: () -> Unit,
    onFavorit: () -> Unit,
    onPengaturan: () -> Unit
) {
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
        onClick = onPengaturan,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
