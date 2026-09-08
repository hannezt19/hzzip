# Roadmap - FileReaderApp

> Update terakhir (8 Sept, oleh hz11/koordinator): dokumen ini ditulis ulang total setelah drift besar dari versi 3 Sept - banyak fitur yang tercatat "belum dikerjakan" ternyata sudah selesai (TTS, Mode Baca, dll), dan sebaliknya. Kalau file ini diupdate lagi: tulis di baris ini apa yang baru berubah, biar sesi berikutnya (akun mana pun) langsung tahu titik pijaknya.

## Tujuan Proyek

App Android pengganti beberapa app reader/editor: baca & edit banyak jenis file (PDF, xlsx, gambar, video, audio, JSON/HTML/JS/TXT/CSS), dengan tampilan konsisten. Device utama: Motorola Moto G45 (RAM kecil, jadi semua keputusan teknis prioritaskan ringan). minSdk 34.

## Tahap Selesai (ringkas - detail teknis ada di STATUS.md)

- **Fondasi**: Keystore signing permanen, izin All Files Access, scan otomatis (sekarang incremental, bukan replace-total), Room DB, Beranda (kartu kategori bulat, search, storage info)
- **Navigasi app**: drawer/hamburger 3/4 layar menggantikan bottom nav bar lama, Pengaturan jadi accordion di drawer, layar kategori full-screen tanpa TopAppBar
- **PDF**: swipe ala buku, pinch-zoom (bug frame fix), OCR (ML Kit v2), mode baca 3-tingkat, page grid navigasi cepat, Text-to-Speech lengkap (translate ID + kontrol notifikasi/lock screen)
- **Gambar**: viewer (pinch-zoom/pan/double-tap), galeri Paging3 per-bulan, toggle Terbaru/Folder
- **Video**: player ExoPlayer/Media3 gesture lengkap, galeri grid dengan toggle Terbaru/Folder (drill-down 2 tingkat)
- **Audio**: player background penuh (notifikasi/lock screen), UI neumorphism dengan cover album otomatis
- **Excel (xlsx)**: lihat/edit sel/simpan (parser+writer sendiri)
- **Toggle Terbaru/Folder**: berlaku di semua kategori KECUALI Audio
- **Sistem Favorit**: tombol bintang di semua viewer (FavoritesStore terpisah dari DB scan)
- **Dark theme toggle**

## Sedang Dikerjakan

- **hz25 - Tugas 2**: sistem clipboard/file-ops terpadu (`FileActionSheet`) - salin/potong/hapus/ganti nama/bagikan/properti file, dipanggil dari titik tiga di semua konten (list & grid thumbnail). Menggabungkan scope yang sebelumnya dipegang hz21 (multi-select Direktori). Urutan kerja: FileDao dulu (deleteByPath/renamePath) -> FileClipboard.kt -> FileActionSheet.kt -> sambung ke FileRow & VideoThumbnail -> tombol Tempel di DirektoriScreen. ImageThumbnail (Gambar) ditunda sampai re-koordinasi dengan hz19.
- **PDF page-turn**: sudah membaik/nyaman, tapi masih perlu perbaikan/polish kecil (belum tuntas 100%)

## Belum Dikerjakan / Rencana Berikutnya

### Prioritas dekat
1. Playlist Audio (ydiv2 - Tahap B): tabel Room baru (path+urutan lagu), panel 2 tab "Playlist"/"Semua Audio", direncanakan sebagai bottom sheet dari bawah (gaya Spotify), BUKAN drawer dari samping
2. Lirik Audio dari tag ID3 USLT (ydiv2 - Tahap C), diakses gestur geser di layar pemutar
3. Multi-select (tap-tahan pilih banyak file sekaligus) - tahap lanjutan dari FileActionSheet, ditunda sampai single-file actions stabil

### Belum ada yang pegang / ditunda
- Fitur **Analisis** (menu: Semua Partisi, File Besar, Berkas Terbaru, Folder Kosong, File Redundan, File Duplikat, Keranjang Sampah) - baru tampilan kosong tanpa fungsi, scope belum ditugaskan ke siapa pun
- Keputusan hapus/tidak kartu "Favorit" dari grid Beranda (dobel dengan menu Favorit di drawer) - masih pending konfirmasi final user
- Bug lama: label bulan hilang di mode Terbaru galeri Gambar (cuma bulan berjalan yang ada headernya) - pernah dicoba diperbaiki, belum tuntas
- Ikon aplikasi baru & penomoran versi app - khusus dipegang hz11/user sendiri (terkait keystore/signing), bukan scope akun lain
- Panel pengaturan baca lanjutan (kecerahan in-app, mode halaman vertikal/horizontal/ganda), pencarian dalam dokumen PDF, highlight & catatan pribadi, lanjut baca cepat ke file terakhir, pptx viewer, formula aktif di xlsx - semua masih di tahap ide, belum ada yang mulai

## Referensi Desain

- App "One Read": text selection/highlight/copy, konversi file, menu file lengkap
- Google PDF Viewer: acuan perilaku zoom yang benar
- Google Files: pola toggle Terbaru/Folder, gaya pill button
- Samsung Music (versi lama): acuan redesain Audio Player
- Spotify: acuan rencana playlist Audio (bottom sheet dari bawah)
