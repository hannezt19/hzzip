# TODO - FileReaderApp

> Dikelola oleh hz11 (koordinator). Update terakhir: 8 Sept 2026 - cross-check ke kode & LAPORAN tiap akun via Termux (bukan cuma riwayat chat), beberapa item ternyata sudah tuntas dan dihapus, Tahap C ydiv2 ditulis ulang (ganti pendekatan).
> File ini isinya tugas AKTIF tiap akun + keputusan yang masih menunggu user. Rencana besar ada di ROADMAP.md.
> PENTING: semua tugas di bawah dikerjakan di BRANCH masing-masing akun (bukan langsung main) - lihat CONVENTIONS.md.

## 🔴 URGENT - Build Rusak (prioritas di atas semua tugas lain)

Build GitHub Actions GAGAL: `AudioPlayerScreen.kt:241` dan `:271` - `Unresolved reference: clickable`. File ini wilayah **ydiv2**. Dugaan: import `androidx.compose.foundation.clickable` kelewat - BELUM diverifikasi, cek dulu sebelum patch. Memblokir build `main` untuk SEMUA akun - didahulukan begitu ydiv2 aktif.

## Pembagian Kerja Aktif

### hz19 - Kategori Gambar
- [PROSES] Investigasi bug label bulan hilang di mode Terbaru galeri Gambar. 2 dugaan sudah dicoba dan TERBUKTI SALAH: (1) soal `File.lastModified()` vs EXIF, (2) soal `contentType`/daur-ulang slot Compose di `items()` grid - JANGAN ulangi dua ini, perlu sudut analisis baru
- [BELUM] Sambungkan `onLongClick`/`combinedClickable` di `ImageThumbnail` (sudah disepakati polanya dengan hz25) - siap terima patch dari hz25 begitu Tugas 2 mulai ke bagian Gambar

### hz21 - PDF SettingsPanel / Mode Baca
- [PROSES] Verifikasi visual final SettingsPanel (Warna Latar/Kontras/Mode Baca/TTS/ID di PdfViewerScreen.kt) - perlu build+tes di HP

### hz25 - Video / Toggle Terbaru-Folder / FileActionSheet
- [BELUM] Tugas 2: sistem FileActionSheet - urutan: FileDao (deleteByPath/renamePath) -> FileClipboard.kt -> FileActionSheet.kt -> sambung FileRow & VideoThumbnail -> tombol Tempel di DirektoriScreen. Pola integrasi ke ImageThumbnail (Gambar) sudah disepakati dengan hz19 (combinedClickable + callback onFileLongClick), siap eksekusi begitu sampai ke bagian itu

### ydiv2 - PDF TTS / Audio Player
- [PROSES] Tahap B: playlist custom (sudah cek AppDatabase.kt/FileDao.kt, belum tulis PlaylistEntity/DAO)
- [BELUM] Tugas kecil sebelum lanjut Tahap C: hapus tombol ☰ dari PlayerControlBar (redundant, navigasi sudah lewat swipe)
- [BELUM] Tahap C - Lirik Audio: PENDEKATAN DIGANTI TOTAL dari rencana awal (bukan lagi baca tag ID3 USLT) - sekarang pakai file `.lrc` (format `[mm:ss.ms] teks`) disimpan di penyimpanan PRIVAT khusus app (bukan folder publik, dikunci pakai path lagu), dibersihkan otomatis menumpang `FileDao.syncAll()`. Rencana sub-tahap: C2 (penyimpanan+parser+tampilan dasar), C3 (highlight+auto-scroll sinkron), C4 (editor mode Sederhana), C5 (editor mode Disinkronkan - tap-waktu sambil dengar lagu)

## Menunggu Keputusan User
- (kosong saat ini)

## Belum Ada yang Pegang
- Fitur Analisis (Semua Partisi/File Besar/Berkas Terbaru/Folder Kosong/File Redundan/File Duplikat/Keranjang Sampah) - baru tampilan kosong tanpa fungsi
- Ikon aplikasi baru & penomoran versi app - khusus dipegang hz11/user sendiri
