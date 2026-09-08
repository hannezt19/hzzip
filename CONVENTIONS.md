# CONVENTIONS - FileReaderApp

> File ini berisi aturan main tetap proyek ini. Jarang berubah. Kalau ada aturan baru yang disepakati, tambahkan di sini.

## Peta File Penting
- `MainActivity.kt` - pusat navigasi (drawer/hamburger via ModalNavigationDrawer, buka file, tentukan viewer sesuai FileType)
- `FileType.kt` - deteksi jenis file (dari nama file & MIME type)
- `ui/BottomNavBar.kt` - isi drawer (`AppDrawerContent`, enum `AppTab` cuma HOME+RECENT - Pengaturan jadi accordion di sini, bukan tab)
- `HomeViewModel.kt` + `ui/HomeScreen.kt` - Beranda (kartu kategori, search, storage info, favorit) + state navigasi kategori/toggle Terbaru-Folder per kategori
- `FavoritesStore.kt` - penyimpanan status favorit (terpisah dari Room DB)
- `data/FileDao.kt` - termasuk `syncAll()` (scan incremental diff-by-path)
- `data/Xlsx*.kt` - parser & writer xlsx buatan sendiri
- `VideoPlayerScreen.kt` / `ui/VideoGalleryScreen.kt` - player Video (ExoPlayer/Media3) & galeri grid Video
- `AudioPlayerScreen.kt` / `AudioPlayerService.kt` - player Audio (MediaSessionService) & UI-nya
- `ui/ImageGalleryScreen.kt` / `ui/ImagePagerScreen.kt` - galeri & viewer Gambar
- `ui/FileListWithModeToggle.kt` - komponen list+toggle Terbaru/Folder dipakai bareng kategori PDF/Excel/Teks-Kode/Favorit
- `ui/*ViewerScreen.kt` lain - satu file per jenis viewer (PdfViewerScreen, XlsxViewerScreen, CodeEditorScreen)

## Alur Kerja Standar
1. Cek dulu struktur kode terkait sebelum bikin patch (`grep`/`sed -n`/`cat -n`) - jangan menebak isi file
2. Tulis patch pakai `python3` heredoc dengan `old`/`new` string, cetak jumlah berhasil
3. Cek hasil patch (harus sesuai jumlah yang diharapkan) sebelum lanjut
4. Kalau bikin file baru, cek `wc -l` dan `tail -5` untuk pastikan tidak terpotong
5. `git add` -> `git commit -m "..."` -> `git push`
6. Cek hasil build di GitHub Actions - kalau gagal, baca error log, perbaiki, ulangi dari langkah 2
7. Setelah build sukses & APK diinstal di HP, baru lanjut ke tugas berikutnya
8. Update ROADMAP.md/TODO.md/STATUS.md kalau ada progress atau keputusan baru

## Laporan Progress Antar-Akun

**Wajib**: setiap akun yang mengerjakan repo ini (hz11, hz19, hz21, hz25, ydiv2, dst) membuat file `LAPORAN-[nama-akun]-[tanggal].md` di root repo dan commit langsung ke repo (bukan cuma dikirim lewat chat masing-masing), setiap kali sebuah rencana kerja/tugas dikonfirmasi selesai oleh user (bukan cuma selesai nulis kode - harus sudah dikonfirmasi build sukses & dites di HP).

Isi minimal laporan:
- Apa yang selesai (per tugas/fase)
- Commit terkait (pesan commit, kalau perlu hash lewat `git log`)
- Keputusan desain yang diambil, terutama kalau menyimpang dari perintah awal
- Pertanyaan/blocker yang perlu dikonfirmasi
- Rencana selanjutnya

Tujuan: semua akun bisa tahu progress & pembagian kerja akun lain langsung dari repo, tanpa perlu buka chat akun lain satu-satu.

## Repo & Environment
- Repo: https://github.com/hannezt19/hzzip (nama folder lokal: FileReaderApp)
- Dikerjakan sepenuhnya dari HP via Termux, tanpa Android Studio/laptop
- Build APK release lewat GitHub Actions, sudah pakai keystore signing permanen (update APK tidak perlu uninstall)
- Target device utama: Motorola Moto G45 (RAM kecil) - semua keputusan teknis prioritaskan ringan/hemat resource

## Bahasa & Gaya Komunikasi
- Semua nama fitur, teks UI, dan komentar dalam Bahasa Indonesia
- Diskusi konsep/desain dulu sebelum mulai coding, terutama untuk perubahan besar
