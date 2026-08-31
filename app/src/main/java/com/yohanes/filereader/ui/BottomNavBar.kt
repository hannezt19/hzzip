package com.yohanes.filereader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class AppTab {
    HOME, RECENT, SETTINGS
}

@Composable
fun BottomNavBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Beranda") },
            label = { Text("Beranda") }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.RECENT,
            onClick = { onTabSelected(AppTab.RECENT) },
            icon = { Icon(Icons.Filled.Schedule, contentDescription = "Terakhir Dibuka") },
            label = { Text("Terakhir") }
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.SETTINGS,
            onClick = { onTabSelected(AppTab.SETTINGS) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "Pengaturan") },
            label = { Text("Pengaturan") }
        )
    }
}
