# Roadmap - FileReaderApp

> Update terakhir: bug Gambar 0 file, dark theme, bug zoom PDF, dan bug pan PDF semua sudah beres. Target device diubah jadi minSdk 34 (Moto G45 saja). Rancangan detail Mode Baca + Translate + TTS sudah disepakati (lihat STATUS.md), masih tahap diskusi belum mulai coding.

## Tujuan Proyek
App Android pengganti beberapa app reader/editor: baca dan edit banyak jenis file (PDF, xlsx, gambar, JSON/HTML/JS/TXT/CSS), dengan tampilan konsisten. Device utama dan minimum: Motorola Moto G45 (minSdk 34, RAM kecil, jadi semua keputusan teknis prioritaskan ringan).

## Tahap Selesai
- Tahap 0: Keystore signing permanen (APK release, update tanpa uninstall)
- Tahap 1: Izin All Files Access, scan otomatis, Room DB, Beranda dasar (search/filter/list), bottom navbar (Beranda/Terakhir/Pengaturan)
- Tahap 2: PDF Reader - swipe ala buku, pinch-zoom, back button ke Beranda, indikator halaman
- Tahap 3: Image Viewer (pinch-zoom, pan, double-tap), xlsx versi dasar (lihat/edit sel/simpan)
- Redesain Beranda ala "One Read": kartu kategori warna beda per jenis, kartu Direktori, kartu Favorit
- Sistem Favorit: FavoritesStore.kt (SharedPreferences terpisah dari DB file) plus tombol bintang di semua viewer
- Bug Gambar 0 file di Beranda: diperbaiki
- Dark theme toggle: sudah ada di Pengaturan
- Bug zoom PDF (konten melebihi frame): diperbaiki
- Bug pan PDF setelah zoom (tidak bisa digeser): diperbaiki, root cause closure basi baca state bitmap
- OCR per-halaman dengan prefetch progresif: selesai dibuild, TAPI akan digantikan sepenuhnya oleh fitur Mode Baca (lihat bagian rencana di bawah)
- Target device diubah jadi hanya Motorola Moto G45, minSdk dinaikkan ke 34, targetSdk 35

## Rencana Fitur: Mode Baca, Translate, TTS
Status: tahap DISKUSI, detail lengkap sudah disepakati dan dicatat di STATUS.md, belum mulai coding.

Ringkasan: tap layar membuka modal pengaturan berisi saklar Mode Baca (tata ulang gambar dan teks PDF jadi tampilan rapi, sistem otomatis pilih teks asli atau OCR di belakang layar), kontrol Kontras, Warna Latar, Ukuran Teks, pilihan navigasi Scroll atau Swipe, saklar Translate (on-device ke Bahasa Indonesia), dan TTS (baca teks yang tampil, auto-scroll per paragraf, kontrol di Pengaturan).

## Belum Dikerjakan (urutan prioritas)
1. Mode Baca, Translate, TTS (detail di STATUS.md dan bagian di atas)
2. Mode E-ink (background putih pucat/sepia, font serif, kontras tinggi, transisi halaman instan)
3. Pengaturan jarak baris dan margin, pilihan jenis font
4. Layar tetap nyala saat baca, kunci orientasi layar
5. Animasi ganti halaman ala membuka lembaran kertas
6. Pencarian dalam dokumen (highlight dan indikator hasil ala Dropbox)
7. Highlight dan catatan pribadi
8. Lanjut baca cepat ke buku/file terakhir dibuka
9. pptx (PowerPoint) viewer
10. Formula aktif di xlsx (ditunda karena jarang dipakai)

## Referensi Desain
- App "One Read": text selection/highlight/copy, konversi file, kategori file, menu file lengkap
- Google PDF Viewer: acuan perilaku zoom yang benar
- Referensi custom milik user untuk tampilan Mode Baca (dark background, gambar dan teks ditata rapi) - dibuat sendiri, bukan dari aplikasi tertentu
