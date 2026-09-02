# Roadmap - FileReaderApp

> Update terakhir: fondasi ekstraksi teks PDF untuk Mode Baca selesai (PdfTextExtractor.kt + tombol uji "Uji Teks" sementara), sortByPosition dipastikan JANGAN dipakai (lihat STATUS.md). Desain lengkap "Mode Baca" (reflow) sudah final di tahap diskusi, coding UI sungguhan belum dimulai.
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
- **Bug fix: kartu "Gambar" 0 file** - FileScanner diperbaiki, sekarang deteksi 22218 file ✅
- **Bug fix: zoom PDF melebihi frame** ✅
- **Bug fix: pan tidak berfungsi setelah zoom PDF** (root cause: closure basi baca state bitmap) ✅
- **OCR (implementasi pertama)**: aktif via tombol manual, tapi ternyata BUKAN yang dimaksud user sejak awal - lihat STATUS.md untuk penjelasan & desain final "Mode Baca"
- **Fondasi ekstraksi teks PDF untuk Mode Baca**: minSdk→34, PdfBox-Android 2.0.27.0, PdfTextExtractor.kt, tombol uji "Uji Teks" (sementara) ✅

## Belum Dikerjakan (urutan prioritas terbaru)
1. **Bangun UI "Mode Baca" sungguhan** (reflow: gambar+teks ditata rapi, dark background, sesuai desain final di STATUS.md) - gantikan tombol "Uji Teks" sementara
2. Tombol expand/fullscreen per gambar di Mode Baca (untuk gambar dengan >1 gambar tertanam per halaman - bisa geser lihat gambar lain)
3. Pilihan navigasi Scroll/Swipe (global, berlaku PDF normal maupun Mode Baca)
4. Kontrol Kontras & pilihan Warna Latar di panel Mode Baca
5. Translate (ML Kit Translate on-device, offline, ke Bahasa Indonesia, ganti teks asli)
6. Text-to-speech (TextToSpeech Android bawaan, offline, auto-scroll per paragraf)
7. Mode E-ink terpisah (kalau masih relevan setelah Mode Baca jadi)
8. Layar tetap nyala saat baca, kunci orientasi layar
9. Animasi ganti halaman ala membuka lembaran kertas
10. Pencarian dalam dokumen (highlight + indikator hasil ala Dropbox)
11. Highlight & catatan pribadi
12. Lanjut baca cepat ke buku/file terakhir dibuka
13. pptx (PowerPoint) viewer
14. Formula aktif di xlsx (ditunda, jarang dipakai)

## Referensi Desain
- Referensi custom user untuk Mode Baca: dark background, gambar+teks PDF ditata ulang jadi satu alur baca rapi (dibuat sendiri oleh user, bukan dari app tertentu)
- App "One Read": text selection/highlight/copy, konversi file, kategori file, menu file lengkap
- Google PDF Viewer: acuan perilaku zoom yang benar
