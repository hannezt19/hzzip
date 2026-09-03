# Roadmap - FileReaderApp

> Update terakhir: Mode Baca (reflow) SELESAI total - kontrol Kontras/Warna Latar/Ukuran Teks, navigasi Scroll/Swipe (global), tombol expand gambar fullscreen, dan Panel Pengaturan dirombak sesuai desain user (tanpa teks judul, kontrol Ukuran Teks+Mode Baca satu baris, Warna Latar & Navigasi jadi kotak ikon/swatch). Semua sudah dites & jalan di HP. Berikutnya: Translate & TTS.
> Kalau file ini diupdate, tulis di baris "Update terakhir" di atas: apa yang baru selesai, biar sesi berikutnya (akun mana pun) langsung tahu titik pijaknya tanpa scroll riwayat chat.

## Tujuan Proyek
App Android pengganti beberapa app reader/editor: baca & edit banyak jenis file (PDF, xlsx, gambar, JSON/HTML/JS/TXT/CSS), dengan tampilan konsisten di berbagai perangkat & versi Android. Device target: **HANYA Motorola Moto G45** (RAM kecil, jadi semua keputusan teknis prioritaskan ringan). minSdk 34, targetSdk/compileSdk 35.

## Tahap Selesai
- **Tahap 0**: Keystore signing permanen (APK release, update tanpa uninstall) ✅
- **Tahap 1**: Izin All Files Access, scan otomatis, Room DB, Beranda dasar (search/filter/list), bottom navbar (Beranda/Terakhir/Pengaturan) ✅
- **Tahap 2**: PDF Reader - swipe ala buku, pinch-zoom, back button ke Beranda, indikator halaman ✅
- **Tahap 3**: Image Viewer (pinch-zoom, pan, double-tap) ✅ | xlsx versi dasar (lihat/edit sel/simpan, formula belum dihitung) ✅ | pptx belum dikerjakan
- **Redesain Beranda**: kartu kategori warna beda per jenis, ikon kiri, kartu Direktori (info penyimpanan), kartu Favorit ✅
- **Sistem Favorit**: FavoritesStore.kt + tombol bintang di semua viewer (PDF/Gambar/Excel/Teks-Kode) ✅
- **Dark theme toggle** (Pengaturan) ✅
- **Bug fix**: kartu "Gambar" 0 file, zoom PDF melebihi frame, pan setelah zoom - semua sudah diperbaiki ✅
- **Mode Baca (reflow) - LENGKAP**:
  - Fondasi ekstraksi teks PDF (PdfBox-Android, PdfTextExtractor.kt) ✅
  - UI reflow (ReflowPage): gambar+teks halaman ditata rapi, dark background, fallback OCR untuk PDF hasil scan ✅
  - Kontrol Kontras Gambar (ColorMatrix), Warna Latar (4 pilihan: Gelap/Sepia/Terang/Abu-abu), Ukuran Teks (-/+, permanen) - via ReaderSettingsStore.kt ✅
  - Navigasi Scroll/Swipe - pengaturan GLOBAL berlaku PDF normal maupun Mode Baca (HorizontalPager vs LazyColumn) ✅
  - Tombol expand/fullscreen gambar di Mode Baca (pakai ulang ZoomableImageBox, sama seperti zoom PDF normal) ✅
  - Panel Pengaturan dirombak sesuai desain user: SEMUA teks judul dihapus, kontrol Ukuran Teks & saklar Mode Baca satu baris, Navigasi Halaman jadi 2 kotak ikon panah (↔/↕), Warna Latar jadi 4 kotak swatch warna dengan cincin saat terpilih ✅

## Belum Dikerjakan (urutan prioritas terbaru)
1. **Translate**: ML Kit Translate on-device (offline), tujuan tetap Bahasa Indonesia, ganti teks asli (bukan berdampingan) - saklar tambahan di panel, aktif kalau Mode Baca nyala
2. **Text-to-speech (TTS)**: TextToSpeech Android bawaan (offline), baca versi terjemahan/teks asli, kontrol Play/Jeda+kecepatan di Pengaturan, auto-lanjut halaman ikut mode Scroll/Swipe, auto-scroll per paragraf, tanpa highlight kata
3. Mode E-ink terpisah (kalau masih relevan setelah Translate/TTS jadi)
4. Layar tetap nyala saat baca, kunci orientasi layar
5. Animasi ganti halaman ala membuka lembaran kertas
6. Pencarian dalam dokumen (highlight + indikator hasil ala Dropbox)
7. Highlight & catatan pribadi
8. Lanjut baca cepat ke buku/file terakhir dibuka
9. pptx (PowerPoint) viewer
10. Formula aktif di xlsx (ditunda, jarang dipakai)

## Referensi Desain
- Referensi custom user untuk Mode Baca: dark background, gambar+teks PDF ditata ulang jadi satu alur baca rapi (dibuat sendiri oleh user, bukan dari app tertentu)
- App "One Read": text selection/highlight/copy, konversi file, kategori file, menu file lengkap
- Google PDF Viewer: acuan perilaku zoom yang benar
- Panel Pengaturan: tanpa teks judul sama sekali, kontrol berbentuk ikon/swatch/kotak (desain diajukan langsung oleh user lewat sketsa)
