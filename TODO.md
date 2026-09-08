# TODO - FileReaderApp

> Dikelola oleh hz11 (koordinator). Update terakhir: 8 Sept 2026 - pembagian kerja diperjelas ulang setelah koreksi scope hz21 (bukan Direktori, tapi PDF SettingsPanel) dan alih kepemilikan bug label-bulan ke hz19.
> File ini isinya tugas AKTIF tiap akun + keputusan yang masih menunggu user. Rencana besar/jangka panjang ada di ROADMAP.md.

## 🔴 URGENT - Build Rusak (prioritas di atas semua tugas lain)

Build GitHub Actions GAGAL sejak push [hz11] 8 Sept (dokumentasi only, bukan penyebabnya - cuma kebetulan jadi push yang memicu build berikutnya): `AudioPlayerScreen.kt:241` dan `:271` - `Unresolved reference: clickable`. File ini wilayah **ydiv2**.

Dugaan awal: import `androidx.compose.foundation.clickable` kelewat. TAPI ini BARU DUGAAN, belum diverifikasi - perlu dicek dulu isi file & konteks baris 241/271 sebelum dipatch (jangan asumsi cuma soal import, bisa saja penyebab lain). Kemungkinan besar terjadi karena sesi ydiv2 kehabisan token di tengah kerja Tahap A (redesain UI), bukan kelalaian.

**Ini memblokir build `main` untuk SEMUA akun** sampai diperbaiki - begitu ydiv2 aktif lagi, ini didahulukan sebelum lanjut Tahap B (playlist).

## Pembagian Kerja Aktif

### hz19 - Kategori Gambar
- [PROSES] Investigasi ulang bug label bulan hilang di mode Terbaru galeri Gambar (dugaan: logika insertSeparators/Paging3 di ImageGalleryScreen.kt)
- [BELUM] Koordinasi ImageThumbnail dengan hz25 untuk integrasi FileActionSheet
- Rencana berikutnya: Fast Scroller gaya Google Photos -> Grouping galeri final -> Deteksi foto duplikat

### hz21 - PDF SettingsPanel / Mode Baca
- [PROSES] Verifikasi visual final SettingsPanel (Warna Latar/Kontras/Mode Baca/TTS/ID di PdfViewerScreen.kt) - perlu build+tes di HP
- Catatan: hamburger Beranda SUDAH diputuskan user cukup seperti sekarang - item ini SELESAI/ditutup

### hz25 - Video / Toggle Terbaru-Folder / FileActionSheet
- [SELESAI] Tugas 1: toggle Terbaru/Folder untuk PDF/Excel/Teks-Kode/Favorit + perluasan ke Gambar
- [BELUM] Tugas 2: sistem FileActionSheet - urutan: FileDao (deleteByPath/renamePath) -> FileClipboard.kt -> FileActionSheet.kt -> sambung FileRow & VideoThumbnail -> tombol Tempel di DirektoriScreen. ImageThumbnail ditunda sampai re-koordinasi hz19.
- [BELUM] Tugas baru: hapus kartu "Favorit" dari grid Beranda (HomeScreen.kt/HomeViewModel.kt CATEGORY_LIST) - user KONFIRMASI FINAL

### ydiv2 - PDF TTS / Audio Player
- [SELESAI] PDF TTS: tombol close bar + kontrol notifikasi/lock screen
- [SELESAI, menunggu konfirmasi tes terakhir] Audio Player Tahap A: redesain UI neumorphism
- [PROSES] Audio Player Tahap B: playlist custom (sudah cek AppDatabase.kt/FileDao.kt, belum tulis PlaylistEntity/DAO)
- [BELUM] Audio Player Tahap C: lirik dari tag ID3 USLT

## Menunggu Keputusan User
- (kosong saat ini - semua blocker per 8 Sept sudah dijawab)

## Belum Ada yang Pegang
- Fitur Analisis (Semua Partisi/File Besar/Berkas Terbaru/Folder Kosong/File Redundan/File Duplikat/Keranjang Sampah) - baru tampilan kosong tanpa fungsi
- Ikon aplikasi baru & penomoran versi app - khusus dipegang hz11/user sendiri
