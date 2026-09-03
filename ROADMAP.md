# Roadmap - FileReaderApp

> Update terakhir: sesi akun ke-2 sedang memperbaiki bug perpindahan halaman (page-turn) yang kurang smooth. Di sesi lain (akun ini), dibahas & dicatat rencana 3 fitur baru (TTS, highlight warna, mode gambar fullscreen) di bagian "Rencana Fitur Baru" - BELUM ada kode yang dibuat untuk ketiganya, baru tahap perencanaan. Kalau lanjut dari sini: selesaikan dulu fix bug smooth page-turn, baru pilih salah satu dari rencana fitur baru untuk mulai dikerjakan.
> Kalau file ini diupdate, tulis di baris "Update terakhir" di atas: apa yang baru selesai, biar sesi berikutnya (akun mana pun) langsung tahu titik pijaknya tanpa scroll riwayat chat.

## Tujuan Proyek
App Android pengganti beberapa app reader/editor: baca & edit banyak jenis file (PDF, xlsx, gambar, JSON/HTML/JS/TXT/CSS), dengan tampilan konsisten di berbagai perangkat & versi Android (target minSdk 28). Device utama: Motorola Moto G45 (RAM kecil, jadi semua keputusan teknis prioritaskan ringan).

## Tahap Selesai
- **Tahap 0**: Keystore signing permanen (APK release, update tanpa uninstall) ✅
- **Tahap 1**: Izin All Files Access, scan otomatis, Room DB, Beranda dasar (search/filter/list), bottom navbar (Beranda/Terakhir/Pengaturan) ✅
- **Tahap 2**: PDF Reader - swipe ala buku, pinch-zoom, back button ke Beranda, indikator halaman (pojok kanan atas) ✅
- **Tahap 3**: Image Viewer (pinch-zoom, pan, double-tap) ✅ | xlsx versi dasar (lihat/edit sel/simpan, formula ditampilkan sebagai teks belum dihitung) ✅ | pptx belum dikerjakan
- **Redesain Beranda** (Material You -> revisi ala "One Read"): kartu kategori warna beda per jenis, ikon kiri, layout rapat, kartu Direktori (info penyimpanan), kartu Favorit ✅
- **Sistem Favorit**: FavoritesStore.kt (SharedPreferences, terpisah dari DB file biar gak kereset scan ulang) + tombol bintang di semua viewer (PDF/Gambar/Excel/Teks-Kode) ✅
- **Bug kartu "Gambar" 0 file di Beranda**: diperbaiki (tambah ekstensi jpg/jpeg/png/webp/gif ke FileScanner.kt) ✅
- **Dark theme toggle** di Pengaturan (ThemeStore.kt + MaterialTheme dinamis) ✅
- **OCR untuk PDF hasil scan**: fondasi selesai dibangun - ML Kit Text Recognition v2 (on-device), diproses sekali di background (bukan realtime per halaman), deteksi otomatis PDF butuh OCR (cek text layer dulu), hasil OCR ditampilkan di mode terpisah ("Lihat sebagai teks") di PdfViewerScreen ✅

## Sedang Dikerjakan
- Fix bug: perpindahan halaman (page-turn) yang kurang smooth

## Belum Dikerjakan (dari dokumen "Rencana Pengembangan Ebook Reader")
1. Fix bug zoom PDF - konten melebihi frame saat diperbesar
2. Mode E-ink (background putih pucat/sepia, font serif, kontras tinggi, transisi halaman instan)
3. Panel pengaturan baca (kecerahan in-app, mode halaman vertikal/horizontal/ganda, tema warna latar)
4. Mode reflow teks (font besar + auto word-wrap, butuh PDF dibaca sebagai text layer)
5. Pengaturan jarak baris & margin, pilihan jenis font
6. Layar tetap nyala saat baca, kunci orientasi layar
7. Animasi ganti halaman ala membuka lembaran kertas
8. Translate teks + koreksi tata bahasa
9. Text-to-speech (lihat detail di "Rencana Fitur Baru" di bawah)
10. Pencarian dalam dokumen (highlight + indikator hasil ala Dropbox)
11. Highlight & catatan pribadi (lihat detail di "Rencana Fitur Baru" di bawah)
12. Lanjut baca cepat ke buku/file terakhir dibuka
13. pptx (PowerPoint) viewer
14. Formula aktif di xlsx (Tahap C, ditunda karena jarang dipakai)

## Rencana Fitur Baru (hasil diskusi, belum dikerjakan)

### TTS (Text-to-Speech)
- Kartu mengambang di atas teks: cuplikan kalimat yang sedang dibacakan + tombol mundur/play/maju/tutup (X)
- Panel player di bagian bawah layar: slider Volume, Pitch(Tone), Speed (masing-masing dengan tombol +/- dan reset), plus tombol play/pause/prev/next/pengaturan
- Teks yang sedang dibacakan di-highlight warna abu-abu di badan teks, sinkron dengan posisi baca

### Highlight Teks dengan Pilihan Warna
- Tap-hold pilih teks memunculkan toolbar mengambang: beberapa pilihan warna highlight (minimal 4-5 warna)
- Toolbar juga berisi tombol: Copy, Highlight, Note, Dict. (kamus), More (opsi tambahan)

### Mode Gambar Fullscreen (untuk viewer yang berisi gambar+teks campuran, misal slide/galeri)
- Gambar ditampilkan penuh layar: judul/label di atas, indikator halaman "x/y" di pojok, tombol expand
- Gambar bisa di-pinch-zoom
- Ukuran teks (untuk teks di bawah/sekitar gambar) diatur lewat modal pengaturan
- Modal pengaturan dipanggil dengan 1x tap di layar

## Referensi Desain
- App "One Read": text selection/highlight/copy, konversi file, kategori file, menu file lengkap (cetak, ganti nama, kompresi, gabung/pisah PDF, dll)
- Google PDF Viewer: jadi acuan perilaku zoom yang benar (konten terkurung rapi, beda dari bug yang kita punya sekarang)
