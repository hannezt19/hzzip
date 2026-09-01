# CONVENTIONS - FileReaderApp

> File ini berisi aturan main tetap proyek ini. Jarang berubah. Kalau ada aturan baru yang disepakati, tambahkan di sini.

## Peta File Penting
- `MainActivity.kt` - pusat navigasi (bottom navbar, buka file, tentukan viewer sesuai FileType)
- `FileType.kt` - deteksi jenis file (dari nama file & MIME type)
- `HomeViewModel.kt` + `HomeScreen.kt` - Beranda (kartu kategori, search, storage info, favorit)
- `FavoritesStore.kt` - penyimpanan status favorit (terpisah dari Room DB)
- `data/Xlsx*.kt` - parser & writer xlsx buatan sendiri
- `ui/*ViewerScreen.kt` - satu file per jenis viewer (PdfViewerScreen, ImageViewerScreen, XlsxViewerScreen, CodeEditorScreen)

## Alur Kerja Standar
1. Cek dulu struktur kode terkait sebelum bikin patch (`grep`/`sed -n`/`cat -n`) - jangan menebak isi file
2. Tulis patch pakai `python3` heredoc dengan `old`/`new` string, cetak jumlah berhasil
3. Cek hasil patch (harus sesuai jumlah yang diharapkan) sebelum lanjut
4. Kalau bikin file baru, cek `wc -l` dan `tail -5` untuk pastikan tidak terpotong
5. `git add` -> `git commit -m "..."` -> `git push`
6. Cek hasil build di GitHub Actions - kalau gagal, baca error log, perbaiki, ulangi dari langkah 2
7. Setelah build sukses & APK diinstal di HP, baru lanjut ke tugas berikutnya
8. Update ROADMAP.md/TODO.md/STATUS.md kalau ada progress atau keputusan baru

## Repo & Environment
- Repo: https://github.com/hannezt19/hzzip (nama folder lokal: FileReaderApp)
- Dikerjakan sepenuhnya dari HP via Termux, tanpa Android Studio/laptop
- Build APK release lewat GitHub Actions, sudah pakai keystore signing permanen (update APK tidak perlu uninstall)
- Target device utama: Motorola Moto G45 (RAM kecil) - semua keputusan teknis prioritaskan ringan/hemat resource

## Bahasa & Gaya Komunikasi
- Semua nama fitur, teks UI, dan komentar dalam Bahasa Indonesia
- Diskusi konsep/desain dulu sebelum mulai coding, terutama untuk perubahan besar
