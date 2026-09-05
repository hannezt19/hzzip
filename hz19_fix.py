# hz19. Fase 1 galeri gambar: grid thumbnail Coil dikelompokkan per tanggal

# 1) Tambah dependency Coil
path1 = 'app/build.gradle.kts'
content1 = open(path1).read()
old1 = '    implementation("com.tom-roush:pdfbox-android:2.0.27.0")\n}'
new1 = '    implementation("com.tom-roush:pdfbox-android:2.0.27.0")\n    implementation("io.coil-kt:coil-compose:2.6.0")\n}'
assert old1 in content1, "hz19: pola build.gradle.kts tidak ditemukan, cek manual"
open(path1, 'w').write(content1.replace(old1, new1))
print("OK: build.gradle.kts")

# 2) HomeScreen.kt: percabangan kategori Gambar pakai ImageGalleryScreen
path2 = 'app/src/main/java/com/yohanes/filereader/ui/HomeScreen.kt'
content2 = open(path2).read()
old2 = """        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada file di kategori ini")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(files) { file ->
                    FileRow(file = file, onClick = { onFileClick(file) })
                    Divider()
                }
            }
        }"""
new2 = """        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada file di kategori ini")
            }
        } else if (category == "Gambar") {
            ImageGalleryScreen(files = files, onFileClick = onFileClick)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(files) { file ->
                    FileRow(file = file, onClick = { onFileClick(file) })
                    Divider()
                }
            }
        }"""
assert old2 in content2, "hz19: pola HomeScreen.kt tidak ditemukan, cek manual"
open(path2, 'w').write(content2.replace(old2, new2))
print("OK: HomeScreen.kt")

# 3) File baru ImageGalleryScreen.kt
path3 = 'app/src/main/java/com/yohanes/filereader/ui/ImageGalleryScreen.kt'
content3 = '''package com.yohanes.filereader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yohanes.filereader.data.FileEntity
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ImageGalleryScreen(
    files: List<FileEntity>,
    onFileClick: (FileEntity) -> Unit
) {
    val grouped = remember(files) { groupByDate(files) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
    ) {
        grouped.forEach { (label, filesInGroup) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            items(filesInGroup, key = { it.path }) { file ->
                ImageThumbnail(file = file, onClick = { onFileClick(file) })
            }
        }
    }
}

@Composable
private fun ImageThumbnail(file: FileEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(file.path),
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun groupByDate(files: List<FileEntity>): List<Pair<String, List<FileEntity>>> {
    val today = LocalDate.now()
    val result = LinkedHashMap<String, MutableList<FileEntity>>()

    for (file in files) {
        val date = Instant.ofEpochMilli(file.lastModified).atZone(ZoneId.systemDefault()).toLocalDate()
        val label = when {
            date.isEqual(today) -> "Hari ini"
            date.isEqual(today.minusDays(1)) -> "Kemarin"
            else -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID")))
                .replaceFirstChar { it.uppercase() }
        }
        result.getOrPut(label) { mutableListOf() }.add(file)
    }
    return result.map { it.key to it.value }
}
'''
import os
assert not os.path.exists(path3), "hz19: file ImageGalleryScreen.kt sudah ada, cek manual sebelum timpa"
open(path3, 'w').write(content3)
print("OK: ImageGalleryScreen.kt (baru)")

print("\\nSemua perubahan Fase 1 berhasil diterapkan.")
