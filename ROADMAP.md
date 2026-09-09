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
- **Audio**: player background penuh (notifikasi/lock screen), UI neumorphism, cover album otomatis, Playlist custom (tabel Room, tambah/hapus lagu), navigasi 3 halaman Playlist|Pemutar|Lirik via HorizontalPager swipe (BUKAN bottom sheet - lihat STATUS.md)
- **Excel (xlsx)**: lihat/edit sel/simpan (parser+writer sendiri)
- **Toggle Terbaru/Folder**: berlaku di semua kategori KECUALI Audio
- **FileActionSheet**: sistem clipboard/file-ops (salin/potong/hapus/ganti nama/properti) di semua konten
- **Sistem Favorit**: tombol bintang di viewer (kartu Favorit di Beranda sudah dihapus, tetap ada di menu drawer)
- **Dark theme toggle**

## Sedang Dikerjakan

Status pakai notasi [SELESAI]/[PROSES]/[BELUM] - detail lengkap ada di TODO.md, ringkasan di sini:

- hz19: investigasi bug label bulan galeri Gambar [PROSES]
- hz21: fix zoom PDF mode Scroll (5 tahap) [PROSES], verifikasi visual SettingsPanel [PROSES]
- hz25: FileActionSheet [SELESAI], integrasi ke ImageThumbnail Gambar [BELUM]
- ydiv2: Lirik Audio Checkpoint A (balik ke tag USLT, bukan .lrc) [PROSES]
- PDF page-turn: sudah membaik/nyaman, tapi masih perlu polish kecil (belum tuntas 100%)

Catatan: hz21 mengerjakan PDF (SettingsPanel + zoom, BUKAN Direktori/clipboard). hz25 memegang FileActionSheet independen.

## Belum Dikerjakan / Rencana Berikutnya

### Prioritas dekat
1. Lirik Audio (ydiv2 - Tahap C, PENDEKATAN TERBARU): sumber lirik dari tag ID3 USLT tertanam di file (BUKAN file .lrc terpisah seperti sempat direncanakan), deteksi otomatis pola waktu `[mm:ss.ss]` di dalam teks lirik ala Musicolet, plus baca judul asli lagu (frame TIT2)
2. UI PlaylistPage: thumbnail cover album + menu titik-tiga (Info lagu/Hapus/Tambah/Bagikan)
3. Integrasi FileActionSheet ke ImageThumbnail (kategori Gambar) - pola sudah disepakati hz19+hz25, tinggal eksekusi
4. Multi-select (tap-tahan pilih banyak file sekaligus) - tahap lanjutan FileActionSheet, ditunda sampai kebutuhan muncul

### Belum ada yang pegang / ditunda
- Fitur **Analisis** (menu: Semua Partisi, File Besar, Berkas Terbaru, Folder Kosong, File Redundan, File Duplikat, Keranjang Sampah) - baru tampilan kosong tanpa fungsi, scope belum ditugaskan ke siapa pun
- Ikon aplikasi baru & penomoran versi app - khusus dipegang hz11/user sendiri (terkait keystore/signing), bukan scope akun lain
- Panel pengaturan baca lanjutan (kecerahan in-app, mode halaman vertikal/horizontal/ganda), pencarian dalam dokumen PDF, highlight & catatan pribadi, lanjut baca cepat ke file terakhir, pptx viewer, formula aktif di xlsx - semua masih di tahap ide, belum ada yang mulai

## Referensi Desain

- App "One Read": text selection/highlight/copy, konversi file, menu file lengkap
- Google PDF Viewer: acuan perilaku zoom yang benar
- Google Files: pola toggle Terbaru/Folder, gaya pill button
- Samsung Music (versi lama): acuan redesain Audio Player
- Musicolet: acuan fitur lirik (auto-deteksi timestamp dari tag USLT, editor lirik manual)
