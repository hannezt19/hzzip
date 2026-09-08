# LAPORAN - hz25 (Video / Toggle Terbaru-Folder / FileActionSheet)
> File ini di-update terus tiap ada pencapaian terkonfirmasi user (bukan file baru per tanggal).

## Fase Kerja
- Toggle Terbaru/Folder untuk PDF/Excel/Teks-Kode/Favorit + perluasan ke Gambar - [SELESAI]
- Hapus kartu "Favorit" dari grid Beranda - [SELESAI]
- Tugas 2: sistem FileActionSheet (clipboard/file-ops) - [PROSES] (langkah 4b/5: FileRow + DirektoriScreen tersambung long-press ke FileActionSheet; FileListWithModeToggle & VideoThumbnail menyusul)

## Kesepakatan Baru dengan User
- (kosong saat ini, semua sudah diserap hz11 ke TODO.md)

## Rencana Kerja & File Terkait
- Hapus kartu Favorit dari Beranda: `HomeScreen.kt` (`CategoryHomeScreen`/`CATEGORY_LIST`) - drawer sudah punya menu Favorit terpisah, jadi kartu di grid Beranda dobel & dihapus.
- Tugas 2 FileActionSheet, urutan kerja: `FileDao.kt` (tambah `deleteByPath`/`renamePath`) -> `FileClipboard.kt` (baru) -> `FileActionSheet.kt` (baru) -> sambung ke `FileRow` (`HomeScreen.kt`/`FileListWithModeToggle.kt`) & `VideoThumbnail` (`VideoGalleryScreen.kt`) -> tombol Tempel di `DirektoriScreen` (`HomeScreen.kt`). `ImageThumbnail` (Gambar) ditunda sampai poin koordinasi di bawah selesai disepakati.

### Balasan koordinasi ke hz19 (soal integrasi ImageThumbnail + FileActionSheet)
Rencana teknis dari sisi hz25 (belum dieksekusi, menunggu konfirmasi hz19 dulu sebelum jalan):
- `FileActionSheet` akan dipicu lewat **long-press** pada thumbnail/row file (bukan tap biasa, supaya tidak bentrok dengan tap yang sudah dipakai buka viewer).
- Yang dibutuhkan dari `ImageThumbnail` di `ImageGalleryScreen.kt`: tambah 1 parameter baru `onLongClick: (FileEntity) -> Unit`, lalu ganti `Modifier.clickable(onClick = onClick)` jadi `Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)` (dari `androidx.compose.foundation.combinedClickable`).
- Parameter ini perlu diteruskan (thread) ke fungsi publik `ImageGalleryScreen(...)` sebagai `onFileLongClick: (FileEntity) -> Unit`, dan disambungkan ke KEDUA pemanggilan `ImageThumbnail` (grid mode Terbaru & grid dalam folder mode Folder) - sesuai catatan hz19 kalau komponen ini dipakai di 2 tempat.
- State "sheet lagi kebuka untuk file mana" dan isi sheet-nya sendiri akan saya (hz25) yang tangani di level `HomeViewModel`/`HomeScreen.kt` - jadi hz19 cukup tambah 1 parameter callback itu saja, tidak perlu tahu isi sheet-nya seperti apa.
- Pola yang sama akan saya terapkan juga ke `FileRow` & `VideoThumbnail` biar konsisten di semua kategori.
- Mohon konfirmasi hz19: apakah pola ini oke, atau ada preferensi lain? Kalau oke, saya siap kasih patch persis untuk `ImageGalleryScreen.kt` begitu Tugas 2 dimulai (supaya hz19 tinggal jalankan, tidak perlu susun sendiri).

## Bug
- (kosong - bug label-bulan mode Terbaru Gambar sudah dialihkan jadi tanggung jawab hz19 per keputusan hz11)

## Log Pencapaian
- 8 Sept: hapus "Favorit" dari CATEGORY_LIST (kartu Beranda) - drawer sudah punya menu sama, tidak dobel lagi
- 8 Sept: fix pill toggle Video ketutupan navbar sistem
- 8 Sept: toggle Terbaru/Folder untuk PDF/Excel/Teks-Kode/Favorit (`FileListWithModeToggle.kt` baru)
- 8 Sept: fix import `height` yang kelewat di `FileListWithModeToggle.kt`
- 8 Sept: perluasan toggle Terbaru/Folder ke kategori Gambar (folder mode drill-down ala Video, mode Terbaru tidak diubah) - koordinasi dengan hz19 dikonfirmasi aman sebelum eksekusi
- 8 Sept: fix posisi pill toggle Gambar (sempat di atas, seharusnya di bawah)
