# FileReaderApp

Aplikasi Android pengganti beberapa app reader/editor file: baca & edit PDF, gambar, xlsx, dan file teks/kode (JSON/HTML/JS/CSS/TXT/dll) dalam satu app.

Dikerjakan sepenuhnya dari HP lewat Termux (tanpa laptop/Android Studio). Build APK otomatis lewat GitHub Actions.

## Dokumentasi Proyek

Sebelum melanjutkan pengerjaan (dari sesi/akun manapun), baca dulu 4 file ini secara berurutan:

1. CONVENTIONS.md - aturan kerja tetap: peta file, alur kerja, environment
2. STATUS.md - keputusan desain penting + catatan teknis/jebakan yang sudah ditemukan
3. ROADMAP.md - semua fitur & prioritas jangka panjang
4. TODO.md - tugas paling mendesak (maks 5 poin)

## Target Device

- Perangkat: Motorola Moto G45
- minSdk: 34 (Android 14)
- targetSdk: 35

## Arsitektur

- Jetpack Compose (native Kotlin) - dipilih daripada Capacitor/hybrid demi performa di device RAM kecil
- minSdk 34, tidak perlu dukung device Android lama
- Build dan signing APK release otomatis lewat GitHub Actions (keystore signing permanen, update tanpa perlu uninstall)

## Fitur yang Sudah Selesai

- Beranda: scan otomatis file di penyimpanan, kartu kategori (PDF/Gambar/Excel/Teks-Kode/Direktori/Favorit), pencarian, filter urutan
- Bottom navigation: Beranda, Terakhir, Pengaturan
- PDF Viewer: swipe antar halaman ala buku, pinch-zoom, pan, dark theme
- Image Viewer: pinch-zoom, pan, double-tap zoom
- Excel (xlsx) Viewer: lihat/edit sel, simpan (parser dan writer buatan sendiri, bukan Apache POI)
- Text/Code Editor: buka dan edit file teks/kode, konfirmasi simpan sebelum keluar
- Sistem Favorit: tombol bintang di semua viewer, tersimpan terpisah dari database scan
- OCR per-halaman dengan prefetch progresif (akan digantikan fitur Mode Baca, lihat ROADMAP.md)

## Fitur dalam Rencana

Lihat ROADMAP.md untuk daftar lengkap. Yang sedang didiskusikan detailnya: Mode Baca (tata ulang tampilan gambar dan teks PDF jadi lebih nyaman dibaca), Translate (on-device, ke Bahasa Indonesia), dan Text-to-Speech.

## Cara Kerja Pengembangan

Semua dikerjakan dari HP via Termux: edit file pakai heredoc untuk file baru atau besar, atau python3 untuk patch kecil, lalu git add, git commit, git push. Setelah push, cek hasil build di tab Actions repo GitHub, baru unduh dan instal APK manual di HP setelah build sukses.

Detail lengkap aturan kerja ada di CONVENTIONS.md.
